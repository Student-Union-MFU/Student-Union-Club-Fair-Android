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
import com.su.clubfair.data.MfuStudentId
import com.su.clubfair.data.PasswordPolicy
import com.su.clubfair.data.PasswordProblem
import com.su.clubfair.data.PersonName
import com.su.clubfair.data.PhoneNumber
import com.su.clubfair.data.net.GoogleAccount
import com.su.clubfair.data.net.GoogleSignIn
import com.su.clubfair.ui.model.Student
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
    IneligibleIntake,
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

    /**
     * The Google credential sheet failed before any request was made.
     *
     * Its own case because the fallback for a null server message is "check your
     * number and password", which is actively misleading when the student never
     * typed either — they tapped a Google button and something on the device or
     * in the console configuration went wrong.
     */
    data object GoogleUnavailable : FormError

    /** No Google account on the device that Google would offer. */
    data object GoogleNoAccount : FormError

    /**
     * The session ended on its own, and the student is here because of it.
     *
     * Not a submit failure at all — nothing has been submitted yet — and that is
     * exactly why it needs its own case. It arrives before the form is touched,
     * and it is the only thing that explains why someone who was signed in this
     * morning is looking at a login screen this afternoon.
     */
    data object SessionExpired : FormError
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
        // The intake rule, applied to the id the address carries. Same reason it
        // is here: the address is the first field on the form, and being turned
        // away on submit after typing six more is the version worth avoiding.
        // Derived again rather than read off `derivedStudentId` below: property
        // initialisers run in declaration order, and this one runs first.
        !MfuStudentId.isEligibleIntake(MfuEmail.studentIdFrom(email).orEmpty()) ->
            FieldError.IneligibleIntake

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
 * The rest of sign-up, for a student Google has just introduced.
 *
 * Google establishes who they are — name, MFU address, and therefore the student
 * id — so this form only asks for what Google cannot answer. That is the same
 * split [RegisterForm] makes, minus every field the account already has.
 *
 * [school] is derived from the student id rather than asked for, and is still
 * editable: the code table can go stale, and a student who knows their own
 * school should not be stuck behind our copy of MFU's numbering. See
 * [MfuStudentId] for why the major is picked rather than derived.
 */
data class CompleteProfileForm(
    /** From the Google account, shown but not editable. */
    val firstName: String = "",
    val surname: String = "",
    val studentId: String? = null,

    val phone: String = "",
    val school: String = "",
    val major: String = "",
    val password: String = "",
    val showErrors: Boolean = false,
    val formError: FormError? = null,
    val submitting: Boolean = false,
) {
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

    /** The majors to offer: this school's, or all of them if it is unrecognised. */
    val majorOptions: List<String>
        get() = MfuStudentId.MajorsBySchool[school]
            ?: studentId?.let(MfuStudentId::majorsOf)
            ?: MfuStudentId.MajorsBySchool.values.flatten().distinct().sorted()

    /** True when the school came from the id rather than from the student. */
    val schoolWasDerived: Boolean
        get() = studentId != null && MfuStudentId.schoolOf(studentId) == school

    val isValid: Boolean = listOf(phoneError, schoolError, majorError, passwordError)
        .all { it == null }

    companion object {
        /**
         * Seeded from the account Google just created.
         *
         * The school is filled in from the student id here rather than on first
         * keystroke, so it is on screen before the student touches anything and
         * reads as something already known about them rather than as a guess the
         * form makes while they type.
         */
        fun from(student: Student) = CompleteProfileForm(
            firstName = student.firstName,
            surname = student.surname,
            studentId = student.studentId,
            phone = student.phone.orEmpty(),
            school = student.school
                ?: student.studentId?.let(MfuStudentId::schoolOf).orEmpty(),
            major = student.major
                ?: student.studentId?.let(MfuStudentId::suggestedMajorOf).orEmpty(),
        )
    }
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

    private val _completeProfile = MutableStateFlow(CompleteProfileForm())
    val completeProfile: StateFlow<CompleteProfileForm> = _completeProfile.asStateFlow()

    /**
     * Whether this screen is being shown because a session expired.
     *
     * Read by the gate in `MainActivity` to open on the login form rather than on
     * Welcome: a student who was signed in a moment ago does not need introducing
     * to the app, they need the box they type their password into.
     */
    val sessionExpired: StateFlow<Boolean> = repository.sessionExpired.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    init {
        // Says why, on the form itself. The flag is already true by the time this
        // ViewModel exists — the session ended before the signed-out flow was
        // composed — so the first collected value is the one that matters, and
        // `collect` delivers it.
        viewModelScope.launch {
            repository.sessionExpired.collect { expired ->
                if (expired) _login.update { it.copy(formError = FormError.SessionExpired) }
            }
        }
    }

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
     * Both halves of the credential come from `GoogleSignIn.requestIdToken`,
     * which needs an Activity context — so the screen fetches them and hands
     * them here. They are used for different things and that is deliberate:
     * [idToken] is the only one that proves anything, and su-server verifies its
     * signature, audience, `email_verified` and MFU domain. [account] is the
     * unverified profile riding alongside it, kept to fill in what the server has
     * no copy of, and it decides nothing.
     */
    fun submitGoogle(idToken: String, account: GoogleAccount) {
        _login.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            when (val outcome = repository.signInWithGoogle(idToken, account)) {
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
    fun onGoogleFailed(error: FormError) {
        _login.update { it.copy(submitting = false, formError = error) }
    }

    // ---- Finishing sign-up -----------------------------------------------

    fun onCompleteProfileField(update: CompleteProfileForm.() -> CompleteProfileForm) =
        _completeProfile.update { it.update().copy(formError = null) }

    /**
     * Seeds the form from the account that has just signed in.
     *
     * Called by the gate every time it draws the screen, and deliberately does
     * nothing once the student has started typing: the session refreshes in the
     * background, and re-seeding on a refresh would wipe a half-filled form.
     * [studentId] is the marker for "already seeded" because it is the one field
     * that comes from the account and is never edited.
     */
    fun seedCompleteProfile(student: Student) {
        _completeProfile.update { current ->
            if (current.studentId != null) current else CompleteProfileForm.from(student)
        }
    }

    /**
     * Sends the rest of sign-up.
     *
     * No navigation on success, for the same reason the other two forms do none:
     * the write updates the cached account, `profile_complete` flips, and the
     * gate swaps the tree on its own.
     */
    fun submitCompleteProfile() {
        val form = _completeProfile.value
        if (!form.isValid) {
            _completeProfile.update { it.copy(showErrors = true) }
            return
        }
        if (form.submitting) return

        _completeProfile.update { it.copy(submitting = true, formError = null) }
        viewModelScope.launch {
            val outcome = repository.completeSignUp(
                phone = form.phone.trim(),
                school = form.school.trim(),
                major = form.major.trim(),
                password = form.password,
            )
            when (outcome) {
                AuthOutcome.Success -> _completeProfile.value = CompleteProfileForm()

                AuthOutcome.Offline -> _completeProfile.update {
                    it.copy(submitting = false, formError = FormError.Offline)
                }

                is AuthOutcome.Rejected -> _completeProfile.update {
                    it.copy(submitting = false, formError = FormError.Rejected(outcome.message))
                }
            }
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
