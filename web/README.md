# HOMEo Clinic Pro - Public Web Application

This folder (`/web`) contains the standalone, responsive **Web Booking Portal** for HOMEo Clinic Pro.

---

## 🌟 Key Features

1. **Zero Login for Clients**: Patients on mobile Chrome, Safari, or desktop can view clinic information and book an appointment with no Google account or app installation.
2. **Online & In-Clinic Booking**: Clients enter Name, Phone, Age, Symptoms, Preferred Date, and Time Slot.
3. **Instant Token Generation**: Generates unique appointment IDs (e.g., `HM-2026-1048`) and provides direct WhatsApp ticket confirmation.
4. **Private Doctor / Admin Portal**:
   - Securely protected behind owner login / PIN (`admin123`).
   - View, filter, and search patient bookings.
   - Accept, Complete, or Cancel appointments.
   - Direct click-to-WhatsApp patient contact.
   - Export all bookings to Excel / CSV.
5. **Central Database Ready**: Works out-of-the-box with browser storage, with pre-configured support for free Firebase Firestore (`firebase-config.js`) for multi-device live sync.

---

## 🚀 How to Deploy to a Public URL in 2 Minutes (100% Free)

You do **NOT** need Google Play Console. Choose any of these instant free hosting methods:

### Method 1: Netlify Drop (Easiest - 30 Seconds, No Coding)
1. In Google AI Studio, export/download your project as a ZIP, or export the `web` folder.
2. Go to **[https://app.netlify.com/drop](https://app.netlify.com/drop)** in your browser.
3. Drag and drop the `web` folder directly into the browser box.
4. Netlify immediately generates a **live public HTTPS URL** (e.g. `https://homeo-clinic-pro.netlify.app`).
5. Share that URL with your patients. It works on every smartphone without login!

---

### Method 2: Vercel (1-Click Deployment)
1. Go to **[https://vercel.com](https://vercel.com)** and sign in with GitHub or email.
2. Click **"Add New Project"** and import your repository.
3. In the "Root Directory" setting, choose `web`.
4. Click **"Deploy"**.
5. You get a free permanent URL (e.g. `https://homeo-clinic.vercel.app`).

---

### Method 3: GitHub Pages (Free forever on GitHub)
1. Push this project to GitHub.
2. Go to your repository **Settings** → **Pages**.
3. Under **Branch**, select `main` and choose folder `/web` (or move files to `gh-pages` branch).
4. Click **Save**.
5. Your public clinic URL will be live at `https://<your-username>.github.io/<repo-name>/`.

---

### Method 4: Firebase Hosting
1. Install Firebase CLI: `npm install -g firebase-tools`
2. Run `firebase login` and `cd web`
3. Run `firebase init hosting` and select this directory
4. Run `firebase deploy`
5. Your site is live at `https://<your-project>.web.app`.
