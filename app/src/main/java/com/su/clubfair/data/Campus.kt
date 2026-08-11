package com.su.clubfair.data

/**
 * Mae Fah Luang's schools, for the sign-up dropdown.
 *
 * The list itself now lives in [MfuStudentId], because that is where it has to
 * agree with something: the same names are what a student id's school code
 * decodes to, and two lists that must match are one list.
 *
 * They also match `su-wbw-website/components/register/mfu-data.ts`, hence the
 * "School of …" prefix that was not on the names here before. A student is one
 * person across the two apps and su-server stores what either sends, so the two
 * writing the same school under different names is a difference with no meaning
 * that any later report would have to undo.
 *
 * The warning that came with this list still stands: **check it against the
 * registrar before release.** Schools are renamed, split and merged more often
 * than an app is updated, and a student whose school is missing cannot finish
 * signing up. Both dropdowns that use it fall back to the full list rather than
 * to nothing, which softens that but does not fix it.
 */
object Campus {
    val Schools: List<String> get() = MfuStudentId.Schools
}
