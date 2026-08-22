# Play Console store listing

Copy for the Google Play listing, in the two locales the app itself ships —
`en-US` as the default listing and `th-TH` alongside it. Play counts these in
**characters, not bytes**, so the Thai fits the same 80/4000 limits the English
does; the counts under each block are the real ones.

> ⚠ **The Thai here is a first pass and wants a native reader before it is
> published**, for the same reason `values-th/strings.xml` does. It is written
> in the app's own vocabulary — จุดเช็คอิน, บูธชมรม, กำหนดการ — so a rewrite
> should keep those words or change them in both places.

Everything below is stated to be true of 0.2.3. The two things most likely to
go stale are the fair's date and the Android floor; both are checked in
`RELEASE_NOTES.md` and `app/build.gradle.kts` respectively.

---

## App name

Play allows 30 characters. The app's own `app_name` is longer than that in both
locales, so the listing name is a trim rather than a copy of it.

| Locale | Name | Characters |
|---|---|---|
| `en-US` | `SU Club Fair — MFU` | 18 |
| `th-TH` | `Club Fair องค์การนักศึกษา` | 25 |

---

## Short description — `en-US`

*Limit 80 characters.*

```
Scan a QR at every club booth, collect checkpoints, unlock your MFU333 code.
```

*76 characters.*

## Short description — `th-TH`

```
สแกน QR ที่ทุกบูธชมรม เก็บจุดเช็คอินให้ครบ แล้วปลดล็อกรหัส MFU333
```

*65 characters.*

---

## Full description — `en-US`

*Limit 4000 characters.*

```
The Mae Fah Luang University Student Union Club Fair, in your pocket.

Walk the fair floor, scan the QR code on each club's booth sign, and watch your checkpoints add up. Collect enough of them and MFU333 — the Student Union's prize for walking the whole fair — unlocks on your phone.

WHAT YOU CAN DO

• Browse every club at the fair. Search by name, or explore the floor zone by zone.
• Scan a booth. Point the camera at the QR code on the booth sign. The code changes every 30 seconds, so turning up is the only way to collect it.
• Watch your checkpoints add up on the Home tab. Progress is saved as you go and holds up on a patchy signal.
• Unlock MFU333. The scan that crosses the threshold tells you on the spot, and the revealed code goes to the Student Union desk to claim your prize.
• Follow the programme — what is on and when, with whatever is running right now marked as running.
• Read announcements posted by the organisers during the fair.
• Carry your student pass: your name, your student ID, and a QR code for staff to scan.
• Thai and English throughout, switchable in Settings.

FOR CLUBS AND ORGANISERS

Booth accounts show their own rotating QR code on the home screen, with the display kept awake. Staff and admin accounts scan a student's pass rather than a booth code, and admins can bring up every booth's live code from a single device.

WHAT YOU NEED

• An MFU student account. Sign in with Google, or with an email address and a password.
• A camera, for scanning. Everything else works without one — the club directory, the programme, the announcements and your pass.
• Android 7.0 or newer.

THE FAIR

22 August 2026, 16:00 – 21:30, Indoor Stadium, Mae Fah Luang University.

Made by the Student Union of Mae Fah Luang University.
```

*1763 characters.*

## Full description — `th-TH`

```
แอปงาน Club Fair ขององค์การนักศึกษา มหาวิทยาลัยแม่ฟ้าหลวง

เดินชมบูธชมรมในงาน สแกน QR ที่ป้ายของแต่ละบูธ แล้วดูจุดเช็คอินของคุณเพิ่มขึ้นเรื่อย ๆ เก็บให้ครบตามเกณฑ์แล้ว MFU333 รางวัลจากองค์การนักศึกษาสำหรับคนที่เดินชมงานครบ จะปลดล็อกบนเครื่องของคุณ

สิ่งที่ทำได้ในแอป

• ดูรายชื่อชมรมทั้งหมดในงาน ค้นหาด้วยชื่อ หรือไล่ดูทีละโซนของผังงาน
• สแกนบูธ เล็งกล้องไปที่ QR code บนป้ายของบูธ รหัสเปลี่ยนทุก 30 วินาที การมาที่บูธจริงจึงเป็นทางเดียวที่จะเก็บได้
• ดูจุดเช็คอินที่เก็บได้จากหน้าแรก ความคืบหน้าถูกบันทึกให้ตลอดทาง และยังใช้ได้แม้สัญญาณไม่ดี
• ปลดล็อก MFU333 การสแกนครั้งที่ทำให้ครบเกณฑ์จะบอกคุณทันที แล้วนำรหัสที่ปรากฏไปแสดงที่จุดองค์การนักศึกษาเพื่อรับรางวัล
• ดูกำหนดการของงานว่ามีอะไรเมื่อไร พร้อมป้ายกำกับรายการที่กำลังดำเนินอยู่
• อ่านประกาศจากผู้จัดงานระหว่างงาน
• พกบัตรนักศึกษาไว้ในแอป ทั้งชื่อ รหัสนักศึกษา และ QR ให้เจ้าหน้าที่สแกน
• ใช้ได้ทั้งภาษาไทยและภาษาอังกฤษ สลับภาษาได้ในหน้าตั้งค่า

สำหรับชมรมและผู้จัดงาน

บัญชีเจ้าของบูธจะเห็น QR ของบูธตัวเองบนหน้าแรก อัปเดตอัตโนมัติและหน้าจอไม่ดับ บัญชีเจ้าหน้าที่และผู้ดูแลจะสแกนบัตรนักศึกษาแทนการสแกนบูธ และผู้ดูแลเปิดดู QR ของทุกบูธได้จากเครื่องเดียว

สิ่งที่ต้องมี

• บัญชีนักศึกษา มฟล. เข้าสู่ระบบด้วย Google หรือด้วยอีเมลและรหัสผ่าน
• กล้อง สำหรับสแกน ส่วนที่เหลือใช้ได้โดยไม่ต้องมีกล้อง ทั้งรายชื่อชมรม กำหนดการ ประกาศ และบัตรนักศึกษา
• Android 7.0 ขึ้นไป

รายละเอียดงาน

22 สิงหาคม 2569 เวลา 16:00 – 21:30 อาคารกีฬาในร่ม มหาวิทยาลัยแม่ฟ้าหลวง

จัดทำโดยองค์การนักศึกษา มหาวิทยาลัยแม่ฟ้าหลวง
```

*1454 characters.*

---

## App access — what the review team is told

Play Console → **App content → App access**. Choose **"All or some functionality
is restricted"**: everything past the welcome screen needs a signed-in student,
and a reviewer who cannot get in files the app as broken.

**Access instruction name**

```
Student account — required for every screen except the welcome page
```

**Username / Password**

A demo participant account on the production server. It does not exist yet —
create one before submitting, and keep it alive for as long as the listing is
live, because Play re-reviews on every update.

```
Username: <demo student email>
Password: <demo password>
```

**Any other instructions**

The field is short — this is written to fit **499 characters**, so it will go in
whatever the Console's cap turns out to be. Everything a reviewer has to be told
is here; everything a reviewer would only find interesting is not.

```
Everything past the welcome page needs a signed-in student; use the account above.

Sign in: Get Started, then "I already have an account", then email and password. That reaches every screen — clubs, programme, announcements, MFU333, the pass.

Scanner: the round button on the bottom bar; needs camera access. Booth QR codes rotate every 30 seconds, so a saved image will not scan. Open <live booth code URL> on a second screen and point the camera at it.

Thai and English, switchable in Settings.
```

*499 characters, 84 words.*

⚠ The `<live booth code URL>` placeholder is the one piece of this that has to
be built or decided before submitting. The rotation is deliberate —
it is what stops a student collecting a booth they never walked to — but it also
means a reviewer cannot scan anything from a static image. Three ways out, in
order of how little work they are:

1. Point them at a booth account's home screen, opened in a second install, which
   shows that booth's live rotating QR.
2. Serve one page from su-server that renders a live code for a demo booth, and
   put its URL in the instructions.
3. Say in the instructions that the scanner cannot be exercised outside the
   event, and let the reviewer approve the rest. This is normally accepted, but
   it leaves the app's headline feature untested and is the weakest of the three.

Whichever you pick, **do not hand the reviewer an admin account as a shortcut.**
An admin's scanner reads a student's pass rather than a booth code — the screen
does a different job — so an admin account would show the reviewer a feature the
listing does not describe.


## Graphics

| Asset | Play's requirement | Where it is |
|---|---|---|
| App icon | 512 × 512 PNG, 32-bit, no transparency | `art/play/icon-512.png` |
| Feature graphic | 1024 × 500 PNG or JPEG | `art/play/feature-graphic-en.png`, `-th.png` |
| Phone screenshots | 2–8, 320–3840px, side ratio at most 2:1 | `art/play/screenshots/en/`, `/th/` — nine each, JPEG q92 |

The icon is the same composition the launcher draws, rendered from
`drawable/ic_launcher_legacy.xml` — see the note in that file about why the
unmasked form is the adaptive one scaled 1.5x rather than the two layers
stacked. The feature graphic reuses that mark beside the app's own dot-matrix
wordmark, lifted from the welcome screen rather than re-set: `Bitcount Prop
Single` is a variable font pinned to `wght` 300 and `CRSV` 0 in `Type.kt`, and
nothing outside the app renders that instance.

### The screenshots

`make screenshots` regenerates them. They come out of `StoreScreenshots` in
`androidTest` — the real composables, rendered by the real Compose runtime on an
emulator at 1080 × 1920, in both languages. Nine per locale, numbered in the
order they should be uploaded:

| | Screen | Shows |
|---|---|---|
| 1 | Home | the countdown, the checkpoint grid, the tiles and the nav bar |
| 2 | Booths | the three zones of the fair floor |
| 3 | Scan | the viewfinder and a recorded checkpoint |
| 4 | MFU333 | the locked prize code and what unlocks it |
| 5 | Programme | the running order, with the current item badged |
| 6 | Student pass | the QR a booth scans |
| 7 | Announcements | the channel, with reactions |
| 8 | Profile | the account, the pass shortcut and the details |
| 9 | Welcome | the signed-out screen, date and venue |

Two things to know before uploading them:

- **The student is a fixture.** `PreviewStudent` carries a name, an email and a
  student id that read as real ones, so `StoreScreenshots` substitutes its own
  demo student. Do not point the generator at a live account to get "better"
  screenshots — these frames are public.
- **Screenshot 3 is the weak one.** The emulator's camera renders its built-in
  virtual scene — a living room with a checkerboard test card — so the
  viewfinder is showing furniture rather than a booth sign. The app's UI in that
  frame is real and correct; only what the lens sees is wrong. The fair is on
  22 August: one screen recording made at a real booth would replace it with
  something no emulator can produce.

