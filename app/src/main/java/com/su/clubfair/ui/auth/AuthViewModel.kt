package com.su.clubfair.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.su.clubfair.ClubFairApplication
import com.su.clubfair.data.AuthResult
import com.su.clubfair.data.FairRepository
import com.su.clubfair.data.PasswordPolicy
import com.su.clubfair.data.PasswordProblem
import com.su.clubfair.data.PersonName
import com.su.clubfair.data.PhoneNumber
import com.su.clubfair.data.StudentId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Why one field is not acceptable.
 *
 * An enum rather than a ready-made string: the ViewModel has no `Context` and
 * should not be formatting user-facing copy, and a Thai translation of "at least
 * 8 characters" is not the ViewModel's business. `AuthComponents` maps these to
 * string resources at draw time.
 */
enum class FieldError {
    Required,
    BadPhone,
    BadStudentId,
    PasswordTooShort,
    PasswordNeedsLetter,
    PasswordNeedsDigit,
}

/** Why the form as a whole was rejected, after a submit. */
enum class FormError {
    NoAccount,
    UnknownPhone,
    WrongPassword,
}

data class LoginForm(
    val phone: String = "",
    val password: String = "",
    /**
     * Errors are held per field but only *shown* once a field has been left or
     * the form submitted — see [showErrors]. Validating as someone types tells
     * them their password is too short when they have entered one character of
     * it, which is true, useless, and reads as the form shouting.
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

data class RegisterForm(
    val firstName: String = "",
    val surname: String = "",
    val studentId: String = "",
    val phone: String = "",
    val school: String = "",
    val major: String = "",
    val password: String = "",
    val showErrors: Boolean = false,
    val submitting: Boolean = false,
) {
    val firstNameError = FieldError.Required.takeUnless { PersonName.isValid(firstName) }
    val surnameError = FieldError.Required.takeUnless { PersonName.isValid(surname) }

    val studentIdError: FieldError? = when {
        studentId.isBlank() -> FieldError.Required
        !StudentId.isValid(studentId) -> FieldError.BadStudentId
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

    val isValid: Boolean = listOf(
        firstNameError, surnameError, studentIdError,
        phoneError, schoolError, majorError, passwordError,
    ).all { it == null }
}

/**
 * The signed-out half of the app: two forms and what happens when they are sent.
 *
 * Separate from `FairViewModel` because it has a different lifetime and a
 * different job — this one is finished the moment a session exists, and holding
 * a half-typed password alongside the fair's booth list would keep it in memory
 * for the rest of the session.
 */
class AuthViewModel(private val repository: FairRepository) : ViewModel() {

    /** Whether signing up would replace an account already on this phone. */
    val hasAccount: StateFlow<Boolean> = repository.hasAccount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    private val _login = MutableStateFlow(LoginForm())
    val login: StateFlow<LoginForm> = _login.asStateFlow()

    private val _register = MutableStateFlow(RegisterForm())
    val register: StateFlow<RegisterForm> = _register.asStateFlow()

    fun onLoginPhone(value: String) =
        _login.update { it.copy(phone = value, formError = null) }

    fun onLoginPassword(value: String) =
        _login.update { it.copy(password = value, formError = null) }

    /**
     * Attempts sign-in; [onSuccess] runs only if it worked.
     *
     * A callback rather than an event the screen observes, because there is
     * exactly one consumer and the alternative — a nullable `navigateTo` field
     * on the state that the screen must remember to clear — is the shape that
     * produces double navigation on a rotation.
     */
    fun submitLogin(onSuccess: () -> Unit) {
        val form = _login.value
        if (!form.isValid) {
            _login.update { it.copy(showErrors = true) }
            return
        }
        if (form.submitting) return

        _login.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            when (repository.signIn(form.phone, form.password)) {
                AuthResult.Success -> {
                    _login.value = LoginForm()
                    onSuccess()
                }

                AuthResult.NoAccount ->
                    _login.update { it.copy(submitting = false, formError = FormError.NoAccount) }

                AuthResult.UnknownPhone ->
                    _login.update { it.copy(submitting = false, formError = FormError.UnknownPhone) }

                AuthResult.WrongPassword ->
                    _login.update {
                        // Clear the password rather than leave a wrong one in the
                        // box for the student to hunt through and edit.
                        it.copy(
                            submitting = false,
                            password = "",
                            formError = FormError.WrongPassword,
                        )
                    }
            }
        }
    }

    fun onRegisterField(update: RegisterForm.() -> RegisterForm) = _register.update(update)

    fun submitRegister(onSuccess: () -> Unit) {
        val form = _register.value
        if (!form.isValid) {
            _register.update { it.copy(showErrors = true) }
            return
        }
        if (form.submitting) return

        _register.update { it.copy(submitting = true) }
        viewModelScope.launch {
            repository.signUp(
                firstName = form.firstName,
                surname = form.surname,
                studentId = StudentId.normalise(form.studentId),
                phone = form.phone,
                // Supplied by the Google step once that is real; the profile
                // shows it as not set until then.
                email = "",
                school = form.school,
                major = form.major,
                password = form.password,
            )
            _register.value = RegisterForm()
            onSuccess()
        }
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
