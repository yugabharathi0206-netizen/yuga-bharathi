/**
 * Dr. Balaji Homeo Care - Firebase Backend Configuration
 * Shared real-time cloud database between Patient Website and Doctor Android App
 */
const FIREBASE_CONFIG = {
  apiKey: "AIzaSyAKyeBOsKoLXy6xCgQoTImsl0u5oVikekU",
  authDomain: "gen-lang-client-0498394744.firebaseapp.com",
  projectId: "gen-lang-client-0498394744",
  storageBucket: "gen-lang-client-0498394744.firebasestorage.app",
  messagingSenderId: "135294159728",
  appId: "1:135294159728:android:30f7854afcf68acaad9db5",
  databaseId: "ai-studio-android-homeocli-a8bd5b9a-3f9e-4fb2-bdf2-754ca5ca5eee"
};

let db = null;
let isFirebaseConnected = false;

function initFirebase() {
  if (typeof firebase !== "undefined") {
    try {
      if (!firebase.apps.length) {
        firebase.initializeApp(FIREBASE_CONFIG);
      }
      db = firebase.app().firestore(FIREBASE_CONFIG.databaseId || undefined);
      isFirebaseConnected = true;
      console.log("✓ Connected to Shared Clinic Firestore Database (" + FIREBASE_CONFIG.databaseId + ")");
      return db;
    } catch (e) {
      console.warn("Firebase initialization note:", e);
      return null;
    }
  }
  return null;
}

// Global accessor
if (typeof window !== "undefined") {
  window.FIREBASE_CONFIG = FIREBASE_CONFIG;
  window.initFirebase = initFirebase;
}
