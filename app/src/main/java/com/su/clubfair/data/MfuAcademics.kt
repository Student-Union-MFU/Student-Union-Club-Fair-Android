package com.su.clubfair.data

import com.su.clubfair.BuildConfig

/**
 * What an MFU student id says about the student, and what it doesn't.
 *
 * ## The number
 *
 * Ten digits, `6831503029`:
 *
 * ```
 *   68   3    15    03    029
 *   ├─   ├─   ├──   ├──   ├───
 *   │    │    │     │     └─ running number within the programme
 *   │    │    │     └─ programme within the school
 *   │    │    └─ school
 *   │    └─ level: 3 is a bachelor's degree
 *   └─ year of entry, Buddhist era (2568 → entered 2025)
 * ```
 *
 * Digits 4–7 together are the programme code (`1503`); its first half is the
 * school. That split is what [schoolOf] relies on, and it is the same one the
 * WBW registration site has been using in production —
 * `components/register/mfu-data.ts`, read there as `sid.slice(3, 5)`.
 *
 * ## Why the major is not derived
 *
 * The last two digits of the programme code do identify the major, so in
 * principle `1503` names one course. In practice nobody has a current table:
 * the only published mapping is a 2013 blog post, which predates MFU folding
 * the School of Information Technology into Applied Digital Technology, lists
 * that school under both `13xx` and `15xx`, and does not contain `1503` at all.
 * Guessing from it would put a wrong major on real accounts, silently, and the
 * student would have no reason to look at a field the form had already filled.
 *
 * So the school is derived and the major is *narrowed*: [majorsOf] returns the
 * handful of courses that school actually teaches, and the student picks. If the
 * registrar's table ever turns up, [MajorByProgrammeCode] below is the one place
 * it goes — nothing else has to change.
 */
object MfuStudentId {

    /** Ten digits, nothing else. Google gives us the local part of an MFU
     *  address, and every account on that domain is a student id. */
    private val Form = Regex("""^\d{10}$""")

    fun isValid(raw: String): Boolean = Form.matches(raw.trim())

    /**
     * The intakes this fair is for, as the first digits of an id — `69` is 2569.
     *
     * From `BuildConfig.INTAKE_PREFIXES`, which mirrors su-server's
     * `CLUBFAIR_INTAKE_PREFIXES`. Two copies of one rule, for the usual reason
     * the password policy has two: the client's saves a round trip and puts the
     * error under the field, the server's is the boundary. They are kept in step
     * by both reading a setting rather than a literal.
     */
    val EligibleIntakes: List<String> =
        BuildConfig.INTAKE_PREFIXES.trim()
            .takeIf { it.isNotEmpty() && it != "*" }
            ?.split(',')
            ?.mapNotNull { part -> part.trim().takeIf { it.isNotEmpty() } }
            .orEmpty()

    /**
     * Whether this id may open a **new** account.
     *
     * Only ever asked about sign-up. An account that already exists signs in
     * whatever its year — see `eligibleIntake` in `clubfair_auth_service.go`,
     * which is the copy that decides — because the alternative locks out staff,
     * admins and every student who registered before the rule existed.
     *
     * An empty [EligibleIntakes] means the rule is switched off, not that
     * nothing qualifies. That distinction is why this is not a `startsWith` at
     * the call site.
     */
    fun isEligibleIntake(raw: String): Boolean {
        if (EligibleIntakes.isEmpty()) return true
        val id = raw.trim()
        return isValid(id) && EligibleIntakes.any { id.startsWith(it) }
    }

    /** The rule as the form states it: "69", or "69 or 70". */
    val eligibleIntakeLabel: String get() = EligibleIntakes.joinToString(" / ")

    /**
     * The programme code — digits 4–7, `1503` — or null if this is not an id.
     *
     * Worth storing even though nothing decodes it yet: it is the part of the
     * number that carries the major, and keeping it means a future mapping can
     * be applied to accounts that already exist.
     */
    fun programmeCodeOf(raw: String): String? =
        raw.trim().takeIf { isValid(it) }?.substring(3, 7)

    /** The school, or null when the id is malformed or its code is unknown. */
    fun schoolOf(raw: String): String? =
        programmeCodeOf(raw)?.take(2)?.let(SchoolByCode::get)

    /**
     * The majors to offer. The school's own list when we know the school,
     * otherwise every major MFU teaches — an unknown code must not leave the
     * student with an empty dropdown and no way forward.
     */
    fun majorsOf(raw: String): List<String> {
        val school = schoolOf(raw) ?: return AllMajors
        return MajorsBySchool[school] ?: AllMajors
    }

    /**
     * Digits 4–5 → school.
     *
     * Ported verbatim from `su-wbw-website/components/register/mfu-data.ts`,
     * which is what the WBW registration form has been matching against. These
     * are MFU's codes, unrelated to the `school_id` column in su-server's
     * `school` table — that one is a database key and is numbered differently.
     */
    private val SchoolByCode = mapOf(
        "10" to "School of Liberal Arts",
        "11" to "School of Science",
        "12" to "School of Management",
        "14" to "School of Agro-Industry",
        "15" to "School of Applied Digital Technology",
        "16" to "School of Law",
        "17" to "School of Cosmetic Science",
        "18" to "School of Health Science",
        "19" to "School of Nursing",
        "21" to "School of Medicine",
        "22" to "School of Dentistry",
        "23" to "School of Social Innovation",
        "24" to "School of Sinology",
        "25" to "School of Integrative Medicine",
    )

    /**
     * The full programme code → major. **Empty on purpose.**
     *
     * This is the hand-off point described above. Fill it from the registrar's
     * current course list — `"1503" to "Software Engineering"` and so on — and
     * [suggestedMajorOf] starts pre-selecting the dropdown. Leaving it empty
     * costs one tap; filling it with stale data costs a wrong major on a real
     * account, so it stays empty until the codes come from MFU itself.
     */
    private val MajorByProgrammeCode = emptyMap<String, String>()

    /**
     * The major to pre-select, if the code is one we know for certain.
     *
     * Always null today. The screen treats null as "ask", which is the same
     * thing it does for an unrecognised school, so nothing needs to change here
     * when [MajorByProgrammeCode] is filled in.
     */
    fun suggestedMajorOf(raw: String): String? =
        programmeCodeOf(raw)?.let(MajorByProgrammeCode::get)

    /**
     * Undergraduate majors, by school.
     *
     * Ported from the same file as [SchoolByCode]. Only the schools that teach a
     * bachelor's degree appear — Anti-Aging and Regenerative Medicine is
     * graduate-only, which is why no `1xxx` code points at it.
     */
    val MajorsBySchool: Map<String, List<String>> = mapOf(
        "School of Agro-Industry" to listOf(
            "Innovative Food Science and Technology",
            "Agri-Food Logistics",
        ),
        "School of Applied Digital Technology" to listOf(
            "Digital and Communication Engineering",
            "Computer Engineering",
            "Digital Technology for Business Innovation",
            "Software Engineering",
            "Multimedia Technology and Animation",
        ),
        "School of Cosmetic Science" to listOf(
            "Cosmetic Science",
            "Beauty Technology",
        ),
        "School of Dentistry" to listOf("Doctor of Dental Surgery"),
        "School of Health Science" to listOf(
            "Public Health",
            "Sports and Health Science",
            "Environmental Health",
            "Occupational Health and Safety",
        ),
        "School of Integrative Medicine" to listOf(
            "Applied Thai Traditional Medicine",
            "Physical Therapy",
            "Traditional Chinese Medicine",
        ),
        "School of Law" to listOf(
            "Laws",
            "Business Law and Chinese Communication",
        ),
        "School of Liberal Arts" to listOf(
            "English",
            "Thai Language and Culture for Foreigners",
        ),
        "School of Management" to listOf(
            "Accounting",
            "Business Administration",
            "Tourism Business and Events",
            "Hospitality Business Management",
            "Logistics and Supply Chain Management",
            "Aviation Business Management",
            "Economics",
        ),
        "School of Medicine" to listOf("Medicine"),
        "School of Nursing" to listOf(
            "Practical Nursing",
            "Nursing Science",
        ),
        "School of Science" to listOf(
            "Applied Chemistry",
            "Biosciences / Biological Science",
            "Materials Engineering",
        ),
        "School of Sinology" to listOf(
            "Business Chinese",
            "Chinese Language and Culture",
            "Chinese Studies",
            "Teaching Chinese Language",
        ),
        "School of Social Innovation" to listOf("International Development"),
    )

    /** Every school, for the picker a student falls back to. */
    val Schools: List<String> = MajorsBySchool.keys.sorted()

    private val AllMajors: List<String> =
        MajorsBySchool.values.flatten().distinct().sorted()
}
