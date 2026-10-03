/**
 * HOMEo Clinic Pro - Cloud Database Configuration
 * 
 * If you want all appointments to sync in real time across different phones, tablets,
 * and computers, you can plug in a free Firebase Firestore project here.
 * 
 * HOW TO GET FREE FIREBASE CREDENTIALS (Takes 2 minutes):
 * 1. Go to https://console.firebase.google.com/
 * 2. Click "Add project" -> Name it "homeo-clinic"
 * 3. Go to "Firestore Database" -> Click "Create Database" (Start in test mode)
 * 4. Go to Project Settings -> Under "Your apps", click the Web (</>) icon
 * 5. Copy the firebaseConfig object and paste it below:
 */

window.FIREBASE_CONFIG = {
  apiKey: "",
  authDomain: "",
  projectId: "",
  storageBucket: "",
  messagingSenderId: "",
  appId: ""
};

// Check if Firebase is enabled
window.IS_FIREBASE_ENABLED = Boolean(window.FIREBASE_CONFIG && window.FIREBASE_CONFIG.projectId);
