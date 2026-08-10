package com.su.clubfair.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.su.clubfair.ClubFairApplication
import com.su.clubfair.data.AuthOutcome
import com.su.clubfair.data.EmailAddress
import com.su.clubfair.data.FairRepository
import com.su.clubfair.data.MfuEmail
import com.su.clubfair.data.PasswordPolicy
import com.su.clubfair.data.PasswordProblem
import com.su.clubfair.data.PersonName
import com.su.clubfair.data.PhoneNumber
import com.su.clubfair.data.net.GoogleSignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Why one field is not acceptable.
 *
 * An enum rather than a ready-made string: the ViewModel has no `Context` and
 * should not be formatting user-facing copy, and a Thai translation of "at least
 * 8 characters" is not its business. `AuthComponents` maps these to string
 * resources at draw time.
 *
 * These checks are the client's copy of rules su-server enforces again. That is
 * deliberate duplication — the client's job is to save a round trip and put the
 * error next to the field, the server's is to be the boundary — and the two are
 * kept in step by both being stated in one place each: here, and
 * `clubfair_auth_service.go`.
 */
enum class FieldError {
    Required,
    BadPhone,
    BadEmail,
    NotMfuEmail,
    PasswordTooShort,
    PasswordNeedsLetter,
    PasswordNeedsDigit,
}

/**
 * Why a submit failed.
 *
 * [Rejected] carries su-server's own message, which is already Thai and written
 * for a student — better than anything the app could substitute, since only the
 * server knows whether it was the number, the password or a flagged account.
 * [Offline] is separate because it is the one failure the student can act on by
 * moving, and because sign-in is the single thing this app cannot queue.
 */
sealed interface FormError {
    data class Rejected(val message: String?) : FormError
    data object Offline : FormError
}

data class LoginForm(
    val phone: String = "",
    val password: String = "",
    /**
     * Errors are held per field but only *shown* once a field has been left or the
     * form submitted. Validating as someone types tells them their password is too
     * short when they have entered one character of it — true, useless, and it
     * reads as the form shouting.
     */
    val showErrors: Boolean = false,
    val formError: FormError? = null,
    val submitting: Boolean = false,
) {
    val phoneError: FieldError? = when {
        phone.isBlank() -> FieldError.Required
        !PhoneNumber.isValid(phone) -> FieldError.BadPhone
        else -> null
    }

    val passwordError: FieldError? = if (password.isBlank()) FieldError.Required else null

    val isValid: Boolean = phoneError == null && passwordError == null
}

/**
 * Sign-up.
 *
 * The student id field is gone. su-server derives it from the MFU email's local
 * part — `6831503029@lamduan.mfu.ac.th` — so asking for it separately invited a
 * mismatch between two things that are the same number. The email is asked for
 * instead, which is one field rather than two and cannot disagree with itself.
 */
data class RegisterForm(
    val firstName: String = "",
    val surname: String = "",
    val email: String = "",
    val phone: String = "",
    val school: String = "",
    val major: String = "",
    val password: String = "",
    val showErrors: Boolean = false,
    val formError: FormError? = null,
    val submitting: Boolean = false,
) {
    val firstNameError = FieldError.Required.takeUnless { PersonName.isValid(firstName) }
    val surnameError = FieldError.Required.takeUnless { PersonName.isValid(surname) }

    val emailError: FieldError? = when {
        email.isBlank() -> FieldError.Required
        !EmailAddress.isValid(email) -> FieldError.BadEmail
        // Checked here as well as on the server so the student is told before
        // filling in six more fields, not after submitting them.
        !MfuEmail.isMfuAddress(email) -> FieldError.NotMfuEmail
        else -> null
    }

    val phoneError: FieldError? = when {
        phone.isBlank() -> FieldError.Required
        !PhoneNumber.isValid(phone) -> FieldError.BadPhone
        else -> null
    }

    val schoolError = FieldError.Required.takeIf { school.isBlank() }
    val majorError = FieldError.Required.takeIf { major.isBlank() }

    val passwordError: FieldError? = when (PasswordPolicy.check(password)) {
        PasswordProblem.Ok -> null
        PasswordProblem.TooShort ->
            if (password.isEmpty()) FieldError.Required else FieldError.PasswordTooShort

        PasswordProblem.NeedsLetter -> FieldError.PasswordNeedsLetter
        PasswordProblem.NeedsDigit -> FieldError.PasswordNeedsDigit
    }

    /** The student id the server will derive, shown so it is not a surprise. */
    val derivedStudentId: String? = MfuEmail.studentIdFrom(email)

    val isValid: Boolean = listOf(
        firstNameError, surnameError, emailError,
        phoneError, schoolError, majorError, passwordError,
    ).all { it == null }
}

/**
 * The signed-out half of the app: two forms and what happens when they are sent.
 *
 * Separate from `FairViewModel` because it has a different lifetime and a
 * different job — this one is finished the moment a session exists, and holding a
 * half-typed password alongside the fair's booth list would keep it in memory for
 * the rest of the session.
 *
 * Neither form navigates on success. Writing the session changes
 * `SessionStatus`, and the gate in `MainActivity` swaps the whole tree; a screen
 * that also navigated itself would be a second source of truth for one fact.
 */
class AuthViewModel(private val repository: FairRepository) : ViewModel() {

    /**
     * Whether the Google button can do anything.
     *
     * False until a Web OAuth client id is built into the app. Surfaced so the UI
     * can disable the button rather than open a credential sheet that cannot
     * succeed — a control that fails silently is worse than one visibly not ready.
     */
    val googleAvailable: Boolean = GoogleSignIn.isConfigured

    private val _login = MutableStateFlow(LoginForm())
    val login: StateFlow<LoginForm> = _login.asStateFlow()

    private val _register = MutableStateFlow(RegisterForm())
    val register: StateFlow<RegisterForm> = _register.asStateFlow()

    fun onLoginPhone(value: String) =
        _login.update { it.copy(phone = value, formError = null) }

    fun onLoginPassword(value: String) =
        _login.update { it.copy(password = value, formError = null) }

    fun onRegisterField(update: RegisterForm.() -> RegisterForm) =
        _register.update { it.update().copy(formError = null) }

    fun submitLogin() {
        val form = _login.value
        if (!form.isValid) {
            _login.update { it.copy(showErrors = true) }
            return
        }
        if (form.submitting) return

        _login.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            when (val outcome = repository.signInWithPassword(form.phone, form.password)) {
                AuthOutcome.Success -> _login.value = LoginForm()

                AuthOutcome.Offline ->
                    _login.update { it.copy(submitting = false, formError = FormError.Offline) }

                is AuthOutcome.Rejected -> _login.update {
                    // Clear the password rather than leave a wrong one in the box
                    // for the student to hunt through and edit.
                    it.copy(
                        submitting = false,
                        password = "",
                        formError = FormError.Rejected(outcome.message),
                    )
                }
            }
        }
    }

    fun submitRegister() {
        val form = _register.value
        if (!form.isValid) {
            _register.update { it.copy(showErrors = true) }
            return
        }
        if (form.submitting) return

        _register.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            val outcome = repository.register(
                firstName = form.firstName.trim(),
                surname = form.surname.trim(),
                email = form.email.trim(),
                phone = form.phone,
                school = form.school.trim().ifBlank { null },
                major = form.major.trim().ifBlank { null },
                password = form.password,
            )
            when (outcome) {
                AuthOutcome.Success -> _register.value = RegisterForm()

                AuthOutcome.Offline ->
                    _register.update { it.copy(submitting = false, formError = FormError.Offline) }

                is AuthOutcome.Rejected -> _register.update {
                    it.copy(submitting = false, formError = FormError.Rejected(outcome.message))
                }
            }
        }
    }

    /**
     * Signs in with Google, once there is a client id to do it with.
     *
     * [idToken] comes from `GoogleSignIn.requestIdToken`, which needs an Activity
     * context — so the screen fetches it and hands the raw token here. The app
     * never inspects it: su-server verifies the signature, the audience,
     * `email_verified` and the MFU domain, and a client-side check would prove
     * nothing anyway.
     */
    fun submitGoogle(idToken: String) {
        _login.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            when (val outcome = repository.signInWithGoogle(idToken)) {
                AuthOutcome.Success -> _login.value = LoginForm()

                AuthOutcome.Offline ->
                    _login.update { it.copy(submitting = false, formError = FormError.Offline) }

                is AuthOutcome.Rejected -> _login.update {
                    it.copy(submitting = false, formError = FormError.Rejected(outcome.message))
                }
            }
        }
    }

    /** Reports a credential-sheet problem on the form, without a server round trip. */
    fun onGoogleFailed(message: String?) {
        _login.update { it.copy(submitting = false, formError = FormError.Rejected(message)) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as ClubFairApplication
                AuthViewModel(app.repository)
            }
        }
    }
}
