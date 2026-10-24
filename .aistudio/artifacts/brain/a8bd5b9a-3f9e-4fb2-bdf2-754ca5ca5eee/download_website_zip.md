# 📦 Clinic Patient Website & Owner Portal (Updated ZIP Archive)

The complete, production-ready website package has been updated with real-time status synchronization and is available for instant download.

---

### 🚀 What Was Fixed:
1. **Real-Time Appointment Confirmation Sync**:
   * Fixed the Firestore real-time listener on the website (`collection.onSnapshot` via structured `runQuery`).
   * When a patient books an appointment on the website, it appears in the Doctor Android App as `PENDING`.
   * When Dr. Balaji taps **Confirm** (either from Doctor Dashboard, Admin Management, or Appointment Detail Dialog), the Android app merges `status: "CONFIRMED"` directly into Cloud Firestore.
   * The patient's web dashboard immediately updates from `⏳ Pending Doctor Confirmation` to `✅ Appointment Confirmed` with green badge and instructions without staying stuck on pending.
   * Added a `🟢 Live Sync Active` indicator and a `🔄 Check Live Status` button on the patient dashboard.
2. **Permanent Owner Portal & Cloud Sync**:
   * The single permanent master owner account (`balaji2000@gmail.com`) is locked in Cloud Firestore and local storage.
   * Profile changes, clinic web address (`https://yugabharathi0206-netizen.github.io/yuga-bharathi/`), clinic address (`Dr.Balaji homeo care,vandavasi,604408`), timings, and custom slots synchronize 2-way between web and app.
3. **ZIP Download Available in Multiple Places**:
   * Direct download link in the top navigation bar, in the Owner Portal, and in the website footer.

---

### 📂 Inside the ZIP (`clinic-patient-portal.zip`):
* `index.html`: Complete single-page application with Patient Booking, Live Status Dashboard, Appointments History, and **Protected Single-Owner Command Center**.
* `app.js`: Live 2-way sync with Doctor Android App via Cloud Firestore.
* `firebase-config.js`: Cloud Firestore bridge connecting to `ai-studio-android-homeocli-a8bd5b9a-3f9e-4fb2-bdf2-754ca5ca5eee`.
* `styles.css`: Responsive CSS with modern emerald medical design system and interactive slot editor.
* `doctor_portrait.jpg`: Doctor portrait image.
* `server.js`: Web server ready for local Node.js (`node server.js`) or one-click hosting on Vercel / Netlify / GitHub Pages.

---

### 📥 3 Easy Ways to Download the ZIP:

1. **Direct Download from your Web Browser**:
   * Open the live website:
     [Open Live Website](https://ais-dev-krh3ojasrp76qf4324mwph-653917990697.asia-southeast1.run.app)
   * Click the **📥 Download ZIP** button in the top navigation bar, in the Owner Portal header, or in the footer.
   * Or direct download link:
     [Download clinic-patient-portal.zip](https://ais-dev-krh3ojasrp76qf4324mwph-653917990697.asia-southeast1.run.app/clinic-patient-portal.zip)

2. **From the AI Studio File Tree**:
   * Locate `clinic-patient-portal.zip` in the root workspace folder.
   * Right-click and select **Download**.

3. **Export Full Project**:
   * In the AI Studio top navigation bar, click the **Settings / Menu (⋮)** icon and select **Export as ZIP**.
