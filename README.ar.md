<div align="center">

<img src="docs/assets/banner.svg" alt="SincronIA: منبّهات حقيقية لتقويم Google ومهام Google. صُنع بمساعدة Claude." width="100%">

**تطبيق تقويم لنظام Android مع منبّهات حقيقية، يتزامن في الاتجاهين مع تقويم Google ومهام Google.**

[![الترخيص: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-1565C0)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-0277BD)

🌐 [English](README.md) · [中文](README.zh-CN.md) · [हिन्दी](README.hi.md) · [Español](README.es.md) · **العربية** · [Português](README.pt-BR.md)

</div>

---

<div dir="rtl">

من السهل أن تفوتك تذكيرات تقويم Google: صوت إشعار، واهتزاز، ثم ينتهي الأمر. يحوّل SincronIA أحداثك ومهامك إلى **منبّهات توقظك فعلًا**: بملء الشاشة فوق شاشة القفل، وترنّ حتى في الوضع الصامت ووضع "عدم الإزعاج"، مع زرّي "غفوة" و"إيقاف". كل ما تنشئه أو تعدّله في التطبيق يعود إلى Google، وكل ما تغيّره في Google يظهر في التطبيق.

> **ملاحظة:** واجهة التطبيق حاليًا باللغة البرتغالية البرازيلية. ترجمتها مساهمة مرحّب بها!

## الميزات

- شاشة **اليوم**: تحية، ومهام متأخرة، وعناصر طوال اليوم، وخط زمني مع علامة "الآن"، والمهام المنجزة اليوم.
- **التقويم** (شريط أيام الأسبوع + قائمة اليوم) و**المهام** (كل قوائمك في مهام Google، مع المهام الفرعية).
- **إنشاء وتعديل وحذف** الأحداث والمهام، مع مزامنة في الاتجاهين مع Google.
- **منبّهات حقيقية**: ملء الشاشة عندما يكون الهاتف مقفلًا، وإشعار مع غفوة/إيقاف أثناء استخدام الهاتف، وصوت حتى في الوضع الصامت؛ وتبقى المنبّهات بعد إعادة التشغيل أو تحديث التطبيق أو تغيير المنطقة الزمنية.
- إيقاف منبّه **سلسلة متكررة كاملة** دفعة واحدة.
- **يعمل دون اتصال أولًا**: تنتظر التغييرات في قائمة انتظار وتُرفع تلقائيًا عند عودة الإنترنت.
- الإعدادات: منبّه افتراضي للأحداث الجديدة، ومدة الغفوة، وصوت المنبّه، وتذكير يومي بالمهام بلا وقت، وفترة المزامنة، واختصارات الأذونات، وتصدير تشخيص دون بيانات شخصية.

## الخصوصية

- **لا يوجد خادم.** يتحدث التطبيق مباشرة مع واجهات برمجة تقويم Google ومهام Google؛ لا يوجد خادم لـ SincronIA.
- نطاقان فقط: `calendar.events` و`tasks`. لا API key ولا client secret.
- تبقى بياناتك على هاتفك (Room + DataStore). النسخ الاحتياطي في Android معطّل، ولا تحتوي السجلات أبدًا على رموز الدخول أو البريد الإلكتروني أو عناوين الأحداث.
- "قطع الاتصال" يلغي الوصول، ويلغي المنبّهات، ويحذف البيانات المحلية.

## قريبًا على Google Play

> 🚀 نخطط لإطلاق SincronIA على Google Play مجانًا للجميع. وحتى ذلك الحين، يمكنك بناؤه بنفسك (انظر أدناه).

## طريقة الاستخدام

1. افتح التطبيق و**سجّل الدخول بحساب Google**، مع السماح بالوصول إلى التقويم والمهام.
2. اسمح **بالإشعارات** و**المنبّهات بملء الشاشة** عندما يطلبها التطبيق. وإذا نقص شيء، يظهر شريط في شاشة **اليوم** مع اختصار.
3. تظهر أحداثك ومهامك في **اليوم** و**التقويم** و**المهام**. اسحب شاشة اليوم إلى الأسفل للمزامنة فورًا.
4. اضغط **+** لإنشاء حدث أو مهمة، واختر متى يرنّ المنبّه: عند البداية أو قبلها ببضع دقائق (وللمهام، وقت محدد).
5. عندما يرنّ المنبّه، اضغط **غفوة** أو **إيقاف**. وفي **الإعدادات** تغيّر المنبّه الافتراضي ومدة الغفوة والصوت وفترة المزامنة.

> 💡 حتى لا تتأخر المنبّهات والمزامنة أبدًا، اجعل استخدام البطارية للتطبيق **غير مقيّد** في إعدادات الهاتف.

## البناء بنفسك

المتطلبات: Android Studio (أو JDK 17+) وAndroid SDK (compileSdk 37). يعمل التطبيق على Android 8.0 (API 26) أو أحدث.

</div>

```bash
git clone https://github.com/GustavoHRX/sincronia.git
cd sincronia
./gradlew assembleDebug test lint
```

<div dir="rtl">

- **وضع العرض التجريبي (دون حساب Google):** ثبّت نسخة debug واضغط **"Entrar em modo demonstração"**. سيمتلئ التطبيق بأحداث ومهام نموذجية.
- **تسجيل الدخول بحساب Google:** أنشئ عملاء OAuth خاصين بك في Google Cloud (عميل *Android* ببصمة SHA-1 الخاصة بنسخة debug لديك، وعميل *Web*)، وفعّل واجهتي Calendar وTasks، وضع معرّف عميل Web في `local.properties`:

</div>

```properties
GOOGLE_WEB_CLIENT_ID=123456789-abc.apps.googleusercontent.com
```

<div dir="rtl">

الدليل خطوة بخطوة في القسم 3.2 من [`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md). احصل على بصمة SHA-1 عبر `./gradlew signingReport`.

- **نسخة الإصدار (release):** أنشئ ملف `key.properties` في جذر المشروع يشير إلى keystore الخاص بك (`storeFile`، `storePassword`، `keyAlias`، `keyPassword`). بدونه، ينتج `assembleRelease` ملف APK غير موقّع.

> لا ترفع أبدًا `local.properties` أو `key.properties` أو ملفات keystore أو `google-services.json` أو `client_secret*.json`. ملف `.gitignore` يحجبها بالفعل.

## التقنيات

</div>

Kotlin · Jetpack Compose + Material 3 · Hilt · Room · DataStore · WorkManager · Retrofit + OkHttp + kotlinx.serialization · Credential Manager + `AuthorizationClient` · `AlarmManager.setAlarmClock()`

```
app/src/main/java/com/sincronia/app/
├── alarm/       alarm receivers, foreground service, full screen, notifications
├── core/        auth, database (Room), network (Google APIs), alarm planning, logging
├── data/        repositories, sync (queue + download), settings, demo data
├── navigation/  navigation
└── ui/          Today, Agenda, Tasks, editor, settings, onboarding, permissions
```

<div dir="rtl">

## التوثيق (بالبرتغالية)

| الملف | ما هو |
|---|---|
| [`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md) | المواصفات الكاملة: البنية، وقواعد المزامنة، والمنبّهات |
| [`docs/DECISOES.md`](docs/DECISOES.md) | القرارات والانحرافات عن المواصفات، مع التاريخ والسبب |

## المساهمة

نرحّب بالـ issues وطلبات الدمج (pull requests). راجع [`CONTRIBUTING.md`](CONTRIBUTING.md).

## الترخيص

[MIT](LICENSE) © GustavoHRX

</div>
