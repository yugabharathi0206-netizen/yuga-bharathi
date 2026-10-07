/**
 * HOMEo AI Classical Clinic - Firebase Cloud Sync Configuration
 */
if (typeof window !== "undefined") {
  window.FIREBASE_CONFIG = {
    apiKey: "AIzaSyAKyeBOsKoLXy6xCgQoTImsl0u5oVikekU",
    authDomain: "gen-lang-client-0498394744.firebaseapp.com",
    projectId: "gen-lang-client-0498394744",
    storageBucket: "gen-lang-client-0498394744.firebasestorage.app",
    messagingSenderId: "135294159728",
    appId: "1:135294159728:android:30f7854afcf68acaad9db5",
    databaseId: "ai-studio-android-homeocli-a8bd5b9a-3f9e-4fb2-bdf2-754ca5ca5eee"
  };

  window.IS_FIREBASE_ENABLED = true;

  try {
    if (typeof firebase !== "undefined") {
      if (!firebase.apps.length) {
        firebase.initializeApp(window.FIREBASE_CONFIG);
      }
      // Initialize Firestore on the named database
      window.db = firebase.app().firestore(window.FIREBASE_CONFIG.databaseId || undefined);
      console.log("✓ Firebase Cloud Sync Active on gen-lang-client-0498394744");
    }
  } catch (err) {
    console.warn("Firebase initialization notice:", err);
  }
} else if (typeof module !== "undefined" && module.exports) {
  module.exports = {};
}
