package com.su.clubfair.data

/**
 * Mae Fah Luang's schools, for the sign-up dropdown.
 *
 * **Check this against the registrar before release.** It is written from the
 * university's public school list and schools are renamed, split and merged more
 * often than an app is updated — a student whose school is missing cannot finish
 * signing up, which is the worst possible way for a stale list to fail.
 *
 * Major is deliberately *not* a list. Programmes change every intake and a
 * dropdown that is one year out of date forces a student to pick something they
 * are not on; a text field is right until this comes from the registrar's own
 * data rather than from a constant in an app.
 */
object Campus {

    val Schools = listOf(
        "Applied Digital Technology",
        "Agro-Industry",
        "Cosmetic Science",
        "Dentistry",
        "Health Science",
        "Integrative Medicine",
        "Law",
        "Liberal Arts",
        "Management",
        "Medicine",
        "Nursing",
        "Science",
        "Sinology",
        "Social Innovation",
    )
}
