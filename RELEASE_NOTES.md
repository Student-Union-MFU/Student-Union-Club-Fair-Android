แอป **Club Fair องค์การนักศึกษา** สำหรับ Android — เดินชมบูธชมรม สแกน QR ที่แต่ละบูธ แล้วสะสมแต้มแลกของรางวัล

The **Student Union Club Fair** app for Android — walk the club booths, scan the QR at each one, collect stamps toward prizes.

---

## มีอะไรใหม่ในเวอร์ชัน 0.2.2 / What's new in 0.2.2

- **วันและเวลาจริงของงาน** เสาร์ที่ 22 สิงหาคม 2569 เวลา 16:00 – 21:30 ที่อาคารกีฬาในร่ม มหาวิทยาลัยแม่ฟ้าหลวง หน้าแรกและบัตรนักศึกษาแสดงวันที่จริงแล้ว ไม่ใช่วันที่ตัวอย่างอีกต่อไป
  *The real date, time and venue* — 22 August 2026, 16:00 – 21:30, Indoor
  Stadium, Mae Fah Luang University. The welcome screen and the student pass now
  print it; the pass had been carrying a placeholder "26–27 Jul" since before the
  fair moved.
- **กำหนดการมาจากเซิร์ฟเวอร์** ถ้าเวลางานเปลี่ยน แอปจะรู้เองโดยไม่ต้องอัปเดตแอป
  *The schedule comes from the server*, so a change to the fair's hours or venue
  reaches every phone without a new release.
- **MFU333 คืออะไร** เพิ่มคำอธิบายในหน้าของรางวัล บอกครบสามขั้น — สแกน เก็บให้ครบ แล้วนำรหัสไปแลก
  *"What is MFU333?"* — the prizes screen now explains itself in three steps:
  scan at each booth, collect your checkpoints, show the unlocked code at the
  Student Union desk.
- **ชื่อยาวไม่ถูกตัดอีกต่อไป** คำทักทายบนหน้าแรกจะย่อขนาดตัวอักษรแทนการตัดชื่อทิ้ง
  *A long name is no longer truncated.* The greeting on Home shrinks its type
  instead of ending a student's own name in an ellipsis.
- **หน้าแรกก่อนเข้าสู่ระบบ** จัดวางใหม่ให้วันเวลาและสถานที่อ่านง่ายขึ้น
  *A cleaner welcome screen*, with the date, time and venue set to read at a
  glance.

### สำหรับเจ้าหน้าที่และผู้ดูแล / For staff and organisers

- **แท็บ "รหัส QR"** สำหรับผู้ดูแล เปิดดู QR ของทุกบูธได้จากเครื่องเดียว รหัสเปลี่ยนทุก 30 วินาทีเหมือนที่บูธ
  *A "Codes" tab for admin accounts* — every booth's live QR from one device,
  rotating every 30 seconds just as it does at the booth.
- **สแกนบัตรนักศึกษา** บัญชีผู้ดูแลสแกนบัตรของนักศึกษาเพื่อดูจำนวนบูธที่เก็บได้และสถานะบัญชี
  *Admin accounts scan a student's pass* rather than a booth code, and see how
  many booths that student has collected.

---

## เวอร์ชัน 0.2.1 / In 0.2.1

- **หน้าจอบูธสำหรับเจ้าของบูธ** บัญชีเจ้าของบูธจะเห็น QR ของบูธตัวเองบนหน้าแรก อัปเดตอัตโนมัติและหน้าจอไม่ดับ
  *A booth display for booth accounts* — the booth's own rotating QR on the home
  screen, refreshed automatically with the screen kept awake.
- **บัญชีเจ้าหน้าที่** ซ่อนการสะสมแต้มและการสแกน เพราะเป็นของผู้เข้าร่วมงาน
  *Staff accounts* no longer see checkpoints or the scanner; both belong to
  participants.
- **แสดงบทบาทของบัญชี** ในหน้าโปรไฟล์
  *Your account role* is shown on the profile page.
- **ขอสิทธิ์น้อยลง** เอาสิทธิ์ลายนิ้วมือที่ไลบรารีของ Google ใส่มาออก แอปไม่เคยใช้
  *Two fewer permissions.* The fingerprint permissions that Google's sign-in
  library declared on the app's behalf are gone; nothing here ever used them.

## ดาวน์โหลด / Download

**[su-clubfair.apk](https://github.com/Student-Union-MFU/Student-Union-Club-Fair-Android/releases/latest/download/su-clubfair.apk)** · 3.6 MB · Android 7.0 ขึ้นไป / Android 7.0 or newer

ถ้าเคยติดตั้งเวอร์ชันก่อนหน้าไว้ ติดตั้งทับได้เลย ไม่ต้องถอนออกก่อน และแต้มที่เก็บไว้ยังอยู่ครบ
*Installing over an earlier version works — no need to uninstall first, and your
checkpoints are safe on the server either way.*

---

## วิธีติดตั้ง (ภาษาไทย)

แอปนี้ไม่ได้อยู่บน Play Store จึงต้องติดตั้งจากไฟล์เอง ระบบ Android จะขึ้นคำเตือนระหว่างทาง **เป็นเรื่องปกติ** ทำตามขั้นตอนนี้ได้เลย

1. กดลิงก์ **su-clubfair.apk** ด้านบนเพื่อดาวน์โหลด
2. เปิดไฟล์ที่ดาวน์โหลดมา (จากแถบแจ้งเตือน หรือในแอป "ไฟล์" → โฟลเดอร์ Download)
3. ถ้าขึ้นข้อความว่า **ไม่ได้รับอนุญาตให้ติดตั้งแอปที่ไม่รู้จัก** ให้กด **ตั้งค่า** แล้วเปิดสวิตช์ **อนุญาตจากแหล่งนี้** จากนั้นกดย้อนกลับ
4. ถ้า Play Protect ขึ้นว่า **บล็อกแอปที่ไม่ปลอดภัย** หรือ **ไม่รู้จักผู้พัฒนาแอปนี้** ให้กด **รายละเอียดเพิ่มเติม** แล้วกด **ติดตั้งต่อไป**
5. กด **ติดตั้ง** แล้วรอสักครู่

> คำเตือนเหล่านี้ขึ้นเพราะแอปไม่ได้มาจาก Play Store ไม่ได้แปลว่าแอปมีปัญหา ถ้าอยากมั่นใจ ตรวจสอบค่า SHA-256 ท้ายหน้านี้ได้

### การเข้าสู่ระบบ

ใช้อีเมล **@lamduan.mfu.ac.th** ของมหาวิทยาลัย (เข้าด้วย Google ได้เลย) หรือสมัครด้วยรหัสนักศึกษาและรหัสผ่าน เปิดให้เฉพาะรหัสรุ่น 66–69

### สิ่งที่ต้องอนุญาต

- **กล้อง** — ใช้สแกน QR ที่บูธเท่านั้น ถ้าไม่อนุญาต ส่วนอื่นของแอปยังใช้ได้ตามปกติ

---

## How to install (English)

This app is not on the Play Store, so it installs from a file. Android will show
warnings along the way — **this is expected**. Here's the whole path:

1. Tap **su-clubfair.apk** above to download it.
2. Open the downloaded file, from your notification shade or via **Files → Download**.
3. If Android says **"not allowed to install unknown apps"**, tap **Settings**,
   turn on **Allow from this source**, then go back.
4. If Play Protect says **"unsafe app blocked"** or **"app from an unknown developer"**,
   tap **More details**, then **Install anyway**.
5. Tap **Install**.

> These warnings appear because the app didn't come from the Play Store, not
> because anything is wrong with it. If you want to be certain, check the SHA-256
> below against the file you downloaded.

### Signing in

Use your university **@lamduan.mfu.ac.th** address — Google sign-in works
directly — or register with your student ID and a password. Open to intakes
66–69.

### Permissions

- **Camera** — only to scan the QR code at a booth. Decline it and the rest of
  the app still works normally.

---

## หมายเหตุ / Notes

- **รายการของรางวัลยังไม่สรุป** ขั้นรางวัลที่เห็นในแอปเป็นข้อมูลตัวอย่างระหว่างรอองค์การนักศึกษายืนยัน แต่จำนวนบูธที่ต้องเก็บเป็นข้อมูลจริง
  *The prize list isn't final. The tiers shown in the app are placeholders while
  the Student Union confirms them; the booth targets are real.*
- แอปรองรับทั้งภาษาไทยและอังกฤษ เปลี่ยนได้ในหน้าตั้งค่า
  *Thai and English are both supported; switch in Settings.*
- แอปจะไม่อัปเดตเอง ถ้ามีเวอร์ชันใหม่จะประกาศที่หน้านี้
  *The app does not update itself. New versions are announced on this page.*

## ตรวจสอบไฟล์ / Verify the download

```
SHA-256  493709d6894af85eceea2e6c7e8d793f9f6375fdb22365ff7eadce758cfdcb1b
```

```bash
sha256sum su-clubfair.apk        # Linux
shasum -a 256 su-clubfair.apk    # macOS
```

Signed by `CN=Thuta Naing, OU=Student Union, O=Mae Fah Luang University` —
certificate SHA-1 `73:41:DF:57:B2:B9:79:C4:AE:F3:48:BF:17:A0:72:4A:7F:35:B2:4A`.

## ติดปัญหา / Problems

ติดต่อองค์การนักศึกษา — studentunion.developer@gmail.com
*Contact the Student Union at the address above.*
