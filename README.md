# Honiko — تطبيق أندرويد تي في

تطبيق تلفاز حقيقي مبني بـ **Java + Android Leanback + Media3 (ExoPlayer)** لواجهة خادم الفيديوهات المحلي
`local-youtube` (الافتراضي: `http://192.168.1.8:8090`).

التطبيق ليس غلاف WebView: الواجهة أصلية، التنقّل بالريموت، والبطاقات تعرض الصور المصغّرة والمدة
وشريط التقدّم، مع حفظ واستئناف مواضع المشاهدة من الخادم.

## المزايا

- صفوف Leanback أصلية: «متابعة المشاهدة» + صف لكل مجلد/سلسلة.
- صور مصغّرة من `/api/thumb/{id}` مع تخزين مؤقت.
- تشغيل عبر ExoPlayer مع استئناف من آخر موضع محفوظ.
- حفظ التقدّم كل 5 ثوانٍ وعند الإيقاف/الخروج عبر `POST /api/progress/{id}`.
- شاشة إعدادات لتغيير عنوان الخادم ورمز API الاختياري.
- واجهة عربية RTL وثيم داكن مطابق لواجهة الويب (`#0B0B0C` / `#E5484D`).
- معالجة أخطاء التشغيل: إذا لم يدعم الجهاز ترميز AV1 يظهر خيار فتح الفيديو في مشغّل خارجي مثل VLC.
- Banner و أيقونة تكيّفية خاصة بأندرويد تي في.

## البناء عبر GitHub Actions

الملف: `.github/workflows/build.yml`

- يعمل عند الدفع إلى `main` أو يدوياً من تبويب Actions.
- يبني `assembleDebug` على `ubuntu-latest` مع JDK 21.
- يرفع الـ APK كـ Artifact باسم **honiko-apk**.

## البناء محلياً

المتطلبات: JDK 21+ و Android SDK يحتوي `platforms;android-37.0` و `build-tools;37.0.0`.

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew assembleDebug
```

الناتج:

```
app/build/outputs/apk/debug/app-debug.apk
```

> التوقيع: يستخدم البناء مفتاح debug ثابتاً موجوداً في `app/honiko-debug.keystore` (كلمة المرور `android`).
> هذا يضمن أن كل نسخ APK من GitHub Actions تحمل نفس التوقيع، فتقبل التلفازات التحديث فوق النسخة السابقة.
> المفتاح للاختبار والتثبيت الجانبي فقط، وليس للنشر على Google Play.

## التثبيت على التلفاز

1. حمّل الـ APK من Artifact في GitHub Actions (أو من البناء المحلي).
2. ثبّته عبر ADB:

```bash
adb connect 192.168.1.x:5555
adb install -r app-debug.apk
```

أو انسخه إلى التلفاز واستخدم تطبيق Downloader / مدير ملفات لفتحه.

## التحكم بالريموت

| الزر | الوظيفة |
| --- | --- |
| أسهم | التنقّل بين الصفوف والبطاقات |
| وسط / Enter | تشغيل الفيديو أو إيقافه مؤقتاً |
| يمين / يسار داخل المشغّل | تقديم/إرجاع 10 ثوانٍ |
| زر الرجوع | الخروج من المشغّل أو من التطبيق |
| قائمة الإعدادات | تغيير عنوان الخادم أو رمز API |

## إعدادات الخادم

افتراضياً يتصل التطبيق بـ `http://192.168.1.8:8090`. إذا تغيّر عنوان البيسي من الراوتر:

1. افتح **الإعدادات** من الشريط العلوي.
2. أدخل العنوان مثل `192.168.1.8:8090` (يمكن إدخال `http://` أو بدونها).
3. استخدم **اختبار الاتصال** ثم **حفظ**.

إذا فعّلت حماية الخادم بـ `--token`، أضف الرمز في حقل «رمز API». يضيفه التطبيق إلى كل طلبات
الـ API والصور والبث تلقائياً.

## واجهات الخادم المستخدمة

| المسار | الاستخدام |
| --- | --- |
| `GET /api/health` | حالة الخادم وعدد الفيديوهات |
| `GET /api/videos?limit=5000&sort=series` | كل الفيديوهات مرتبة |
| `GET /api/folders` | المجلدات/السلاسل |
| `GET /api/progress?limit=50` | متابعة المشاهدة |
| `GET /api/stream/{id}` | بث الفيديو (يدعم Range) |
| `GET /api/thumb/{id}?w=480` | الصور المصغّرة |
| `POST /api/progress/{id}` | حفظ `{position, duration}` بالثواني |

## ملاحظة حول AV1

ملفات الخادم حالياً **AV1 + Opus داخل WebM**. دعم AV1 يعتمد على جهاز/نسخة أندرويد التلفاز.
إذا فشل التشغيل يعرض التطبيق رسالة واضحة مع زر **فتح في مشغل خارجي** (مثل VLC) بدلاً من التجمد.

## بنية المشروع

```
app/src/main/java/com/mishachan/honiko/
├── MainActivity.java          # الشاشة الرئيسية + الشريط العلوي + التحميل/الأخطاء
├── SettingsActivity.java      # عنوان الخادم ورمز API
├── PlayerActivity.java        # ExoPlayer + أزرار الريموت + حفظ التقدّم
├── data/
│   ├── ApiClient.java         # OkHttp + JSON
│   ├── Library.java           # تجميع الفيديوهات/المجلدات/التقدّم
│   ├── Video.java
│   ├── FolderInfo.java
│   ├── ProgressEntry.java
│   ├── CardItem.java
│   └── Prefs.java
└── ui/
    ├── RowsFragment.java      # صفوف Leanback
    ├── VideoCardPresenter.java
    └── Ui.java
```

## الرفع إلى GitHub

```bash
git remote add origin https://github.com/Misha-Chan/Honiko.git
git push -u origin main
```

سيطلب GitHub اسم المستخدم وكلمة المرور (استخدم Personal Access Token مكان كلمة المرور).
