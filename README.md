<p align="center"><img src="docs/icon.png" width="110" alt="Mi5G Check icon" /></p>

<div dir="rtl">

# فاحص الجوال (Mi5G Check)

**افحص جوالك بالكامل — العتاد، صحة البطارية، والشاشة واللمس والسماعات والميكروفون والكاميرا — في تطبيق عربي واحد بدون إعلانات.**

[![Latest Release](https://img.shields.io/github/v/release/al3nkboot/mi5g-check?label=Release&color=14998E)](https://github.com/al3nkboot/mi5g-check/releases/latest)
[![PayPal Support](https://img.shields.io/badge/PayPal-Midroid5G-00457C?logo=paypal&logoColor=white)](https://paypal.me/Midroid5G)
[![X @midroid5G](https://img.shields.io/badge/X-%40midroid5G-000000?logo=x&logoColor=white)](https://x.com/midroid5G)

| البطارية | الفحوصات | العتاد | حول |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/battery.png" width="200" /> | <img src="docs/screenshots/tests.png" width="200" /> | <img src="docs/screenshots/hardware.png" width="200" /> | <img src="docs/screenshots/about.png" width="200" /> |

## المزايا

- **معلومات العتاد والنظام:** المعالج (مع الاسم التجاري للشريحة)، الذاكرة، التخزين، الشاشة، الحساسات، الشبكة، الكاميرات، إصدار أندرويد.
- **صحة البطارية:** السعة التصميمية، السعة الكاملة الحالية ونسبة الصحة (**قيم تقديرية ومُعلَّمة بكلمة «تقديري»**)، عدد الدورات عند توفره، والتيار والقدرة والجهد والحرارة مباشرة. ما لا يوفره الجهاز يظهر «غير متاح».
- **سجل البطارية:** تُحفظ قراءة (النسبة، الصحة التقديرية، الدورات، الحرارة) محليًا عند فتح التطبيق وكل 6 ساعات تقريبًا، لمدة سنة، مع رسم بياني للصحة والدورات (7 أيام / 30 يومًا / الكل).
- **صحة البطارية من النظام (سامسونج):** استورد ملف dumpstate (‏‎.txt أو ‎.zip) الناتج من ‎*#9900#‎ ليقرأ التطبيق قيمة `mSavedBatteryAsoc` ويعرضها بعلامة «من النظام» (مع شرح الخطوات داخل التطبيق). يُقرأ الملف على الجهاز فقط.
- **تنبيه الحرارة (اختياري، معطل افتراضيًا):** إشعار عندما تتجاوز حرارة البطارية حدًا تختاره (42 °م افتراضيًا)، مرة في الساعة كحد أقصى.
- **فحوصات العتاد:** الشاشة (بكسلات ميتة/عالقة)، اللمس، السماعات (الخارجية وسماعة المكالمات)، الميكروفون، الكاميرا (الأمامية والخلفية)، الحساسات (التسارع، الجيروسكوب، القرب، الإضاءة بقيم مباشرة)، الاهتزاز، أزرار الصوت، الفلاش، الاتصال (واي فاي، بلوتوث، GPS بدون إنترنت)، البصمة، الشحن (القدرة بالواط ونوع الشاحن، «شحن سريع» تقديري فوق ~15 واط).
- **فحص شامل وتقرير:** زر «فحص شامل» يمر على جميع الفحوصات بالترتيب (مع إمكانية التخطي) ثم يعرض تقريرًا بطراز الجهاز وإصدار أندرويد وصحة البطارية (تقديرية) ونتيجة كل فحص والتاريخ، ويمكن مشاركته كصورة PNG أو ملف PDF. يُنشأ التقرير على الجهاز ويُشارك فقط ولا يُرفع إلى أي مكان.
- **الشاشة:** معدل التحديث الحالي وجميع المعدلات المدعومة، الدقة الأصلية، الكثافة (dpi)، الحجم بالبوصة («تقديري»)، أنواع HDR، نطاق الألوان الواسع، وأقصى سطوع إن أعلنه النظام.
- **الكاميرات:** لكل كاميرا: الاتجاه، أقصى دقة صورة (ميغابكسل)، أقصى دقة فيديو (4K/8K)، التثبيت البصري OIS، البعد البؤري، فتحة العدسة، الفلاش، دعم RAW، والكاميرات الفعلية للكاميرا المنطقية المتعددة.
- **الأداء:** اختبار خفيف للمعالج (~12 ثانية، نواة واحدة ومتعدد الأنوية) بنتيجة خاصة بالتطبيق لا تُقارن بـ Geekbench/AnTuTu، ونوع التخزين (UFS/eMMC إن أمكن) واختبار سرعة الكتابة/القراءة المتتالية («تقريبي»، ملف مؤقت يُحذف بعده). تُضاف النتائج إلى التقرير.
- الحرارة، التطبيقات المثبتة، الوضع الداكن/الفاتح، العربية (افتراضيًا) والإنجليزية.

## التحميل

حمّل أحدث ملف APK من صفحة الإصدارات: **[أحدث إصدار](https://github.com/al3nkboot/mi5g-check/releases/latest)**

## الأذونات

| الإذن | السبب |
|---|---|
| `CAMERA` | فحص الكاميرا فقط (معاينة، لا تُحفظ صور). |
| `RECORD_AUDIO` | فحص الميكروفون (تسجيل 3 ثوانٍ في الذاكرة ثم تشغيله، لا يُحفظ). |
| `MODIFY_AUDIO_SETTINGS` | توجيه نغمة الفحص إلى سماعة المكالمات. |
| `VIBRATE` | فحص الاهتزاز. |
| `ACCESS_FINE_LOCATION`، `ACCESS_COARSE_LOCATION` | فحص GPS فقط (يُطلب عند الضغط على «فحص GPS»؛ عرض الأقمار وتحديد الموقع، لا يُحفظ الموقع ولا يُرسل). |
| `BLUETOOTH` (أندرويد 11 وأقدم) | معرفة إن كان البلوتوث مفعّلًا في فحص الاتصال. |
| `USE_BIOMETRIC`، `USE_FINGERPRINT` (من مكتبة androidx.biometric) | فحص البصمة عبر نافذة النظام؛ التطبيق لا يرى البصمة. |
| `QUERY_ALL_PACKAGES`، `GET_PACKAGE_SIZE`، `REQUEST_DELETE_PACKAGES` | قائمة التطبيقات وأحجامها وإمكانية إلغاء تثبيت تطبيق بطلبك. |
| `KILL_BACKGROUND_PROCESSES` | إيقاف عمليات تطبيق في الخلفية بطلبك. |
| `ACCESS_NETWORK_STATE`، `ACCESS_WIFI_STATE`، `READ_BASIC_PHONE_STATE` | عرض معلومات الشبكة والواي فاي والشريحة في صفحة العتاد. |
| `POST_NOTIFICATIONS` | تنبيه الحرارة فقط (يُطلب على أندرويد 13+ عند تفعيل التنبيه). |
| `RECEIVE_BOOT_COMPLETED`، `WAKE_LOCK`، `FOREGROUND_SERVICE` (من مكتبة WorkManager) | تشغيل حفظ قراءة البطارية الدوري (كل ~6 ساعات) بعد إعادة التشغيل. |
| `READ_GSERVICES` | عرض معرّف خدمات Google في صفحة النظام (يُعرض محليًا فقط). |

## الخصوصية

التطبيق **لا يطلب إذن الإنترنت (`INTERNET`) أصلًا**، ولا يحتوي على أي مكتبات تتبع أو تحليلات أو إعلانات. كل المعلومات تُقرأ وتُعرض على جهازك فقط ولا تُرسل إلى أي مكان. (رابط «حول التطبيق» يفتح المتصفح فقط.)

## 💙 دعم استمرارية تطوير التطبيق

إذا أعجبك التطبيق واستفدت منه، يمكنك دعم المطور **`@midroid5G`** للمساهمة في استمرار تطوير التطبيق وتحديثه مجانًا وبدون إعلانات:

- **PayPal:** [**paypal.me/Midroid5G**](https://paypal.me/Midroid5G) (`@Midroid5G`)
- **حساب X (تويتر):** [**@midroid5G**](https://x.com/midroid5G)

## الشكر والترخيص

مبني على [CPU Info](https://github.com/kamgurgul/cpu-info) من تطوير Kamil Gurgul، ويستخدم طرق تقدير البطارية من [aBattery](https://github.com/abanana84/abattery) لـ abanana84 — وكلاهما بترخيص Apache-2.0. هذا المشروع أيضًا بترخيص [Apache-2.0](LICENSE)؛ انظر [NOTICE](NOTICE).

</div>

---

# Mi5G Check

**Check your whole phone — hardware, battery health, screen, touch, speakers, mic and camera — in one ad-free app, Arabic-first.**

## Features

- **Hardware & system info:** CPU (with SoC marketing name), RAM, storage, display, sensors, network, cameras, Android version.
- **Battery health:** design capacity, current full capacity and health % (**estimated values, clearly labelled “estimated”**), cycle count when available, plus live current, power, voltage and temperature. Anything the device doesn't expose shows “N/A”.
- **Battery history:** a reading (level, estimated health, cycles, temperature) is stored on the device when you open the app and about every 6 hours, kept for a year, with a chart of health and cycles (7 days / 30 days / all).
- **System battery health (Samsung):** import the dumpstate file (.txt or .zip) from *#9900# and the app reads `mSavedBatteryAsoc` and shows it labelled “from system” (step-by-step help in the app). The file is only read on the device.
- **Temperature alert (optional, off by default):** a notification when the battery is hotter than your threshold (42 °C by default), at most once per hour.
- **Hardware tests:** screen (dead/stuck pixels), touch, speakers (loudspeaker and earpiece), microphone, camera (front and rear), sensors (live accelerometer, gyroscope, proximity, light), vibration, volume buttons, flashlight, connectivity (Wi-Fi, Bluetooth, GPS without internet), fingerprint, charging (watts and charger type, “fast charging” estimated above ~15 W).
- **Full test & report:** “Full test” walks through every test in order (skip allowed), then shows a report with device model, Android version, battery health (estimated), each test result and the date. Share it as a PNG image or a PDF. The report is generated on-device and only shared by you; nothing is uploaded.
- **Display:** current and all supported refresh rates, native resolution, density (dpi), size in inches (“estimated”), HDR types, wide color gamut and max luminance when reported.
- **Cameras:** per camera: facing, max photo megapixels, max video (4K/8K), OIS, focal length, aperture, flash, RAW support and physical cameras of a logical multi-camera.
- **Performance:** a light CPU benchmark (~12 s, single- and multi-core) with the app's own score (not comparable with Geekbench/AnTuTu), storage type (UFS/eMMC when readable) and a sequential write/read speed test (“approximate”, temporary file deleted afterwards). Results are added to the report.
- Temperatures, installed apps, dark/light theme, Arabic (default) and English.

## Download

Get the latest APK from **[the latest release](https://github.com/al3nkboot/mi5g-check/releases/latest)**.

## Permissions

| Permission | Why |
|---|---|
| `CAMERA` | Camera test only (live preview, nothing is saved). |
| `RECORD_AUDIO` | Microphone test (3 s recording kept in memory and played back, never saved). |
| `MODIFY_AUDIO_SETTINGS` | Route the test tone to the earpiece. |
| `VIBRATE` | Vibration test. |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | GPS test only (asked when you tap “Test GPS”; shows satellites/fix, location is never stored or sent). |
| `BLUETOOTH` (Android 11 and older) | Show whether Bluetooth is on in the connectivity test. |
| `USE_BIOMETRIC`, `USE_FINGERPRINT` (from androidx.biometric) | Fingerprint test via the system prompt; the app never sees your fingerprint. |
| `QUERY_ALL_PACKAGES`, `GET_PACKAGE_SIZE`, `REQUEST_DELETE_PACKAGES` | Apps list, sizes, and uninstalling an app when you ask. |
| `KILL_BACKGROUND_PROCESSES` | Stop an app's background processes when you ask. |
| `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `READ_BASIC_PHONE_STATE` | Show network, Wi-Fi and SIM info on the Hardware page. |
| `POST_NOTIFICATIONS` | Temperature alert only (asked on Android 13+ when you turn it on). |
| `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `FOREGROUND_SERVICE` (from WorkManager) | Run the periodic (~6 h) battery reading, also after a reboot. |
| `READ_GSERVICES` | Show the Google Services Framework ID on the OS page (displayed locally only). |

## Privacy

The app **does not request the `INTERNET` permission at all** and contains no tracking, analytics or ad libraries. Everything is read and shown on your device and never sent anywhere. (The About link just opens your browser.)

## 💙 Support Development

If you find **Mi5G Check** useful, you can support the developer **`@midroid5G`** to help keep the app free and ad-free:

- **PayPal:** [**paypal.me/Midroid5G**](https://paypal.me/Midroid5G) (`@Midroid5G`)
- **X (Twitter):** [**@midroid5G**](https://x.com/midroid5G)

## Credits & license

Based on [CPU Info](https://github.com/kamgurgul/cpu-info) by Kamil Gurgul, with battery estimation methods from [aBattery](https://github.com/abanana84/abattery) by abanana84 — both Apache-2.0. This project is licensed under [Apache-2.0](LICENSE); see [NOTICE](NOTICE).
