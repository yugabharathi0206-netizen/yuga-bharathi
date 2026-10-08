/**
 * SP CLINIC / HOMEO AI CLASSICAL CLINIC
 * SECURE OWNER DASHBOARD & CLIENT APPOINTMENT BOOKING PORTAL
 */

// Master Clinic Default Data
const DEFAULT_CLINIC_DATA = {
  clinicName: "Dr. Balaji Homeo Care",
  clinicSubtitle: "HOMEo AI Classical Clinic • Your health, our care.",
  doctorName: "Dr. Balaji",
  doctorQualification: "BHMS, MD (Homeopathy)",
  doctorSpeciality: "Classical Constitutional Homeopathy",
  doctorBio: "Dr. Balaji specializes in individualized Classical Constitutional Homeopathy, combining deep repertorisation with gentle holistic remedies to treat chronic and acute ailments safely with zero side effects.",
  phone: "+91 98765 43210",
  cleanPhone: "919876543210",
  address: "Dr. Balaji Homeo Care, 74 Gandhi Road, Near Central Park, Health Complex, Chennai - 600001",
  email: "care@spclinic.com",
  consultingHours: "Morning: 09:00 AM – 01:00 PM | Evening: 05:00 PM – 09:00 PM (Mon - Sat)",
  fee: 500,
  doctorPhoto: "doctor_portrait.jpg",
  inClinicEnabled: true,
  onlineEnabled: true,
  inClinicLabel: "In-Clinic Visit",
  onlineLabel: "Online Video Consultation",
  slots: [
    "09:30 AM - 10:00 AM",
    "10:00 AM - 10:30 AM",
    "10:30 AM - 11:00 AM",
    "11:00 AM - 11:30 AM",
    "11:30 AM - 12:00 PM",
    "12:00 PM - 12:30 PM",
    "05:00 PM - 05:30 PM",
    "05:30 PM - 06:00 PM",
    "06:00 PM - 06:30 PM",
    "06:30 PM - 07:00 PM",
    "07:00 PM - 07:30 PM",
    "07:30 PM - 08:00 PM",
    "08:00 PM - 08:30 PM"
  ],
  advanceDays: 30,
  consultingDays: "Monday - Saturday (Sunday Closed)"
};

// Storage Keys
const STORAGE_KEYS = {
  APPOINTMENTS: "homeo_clinic_appointments",
  OWNER_AUTH: "sp_clinic_owner_auth",         // Salted cryptographic hash of Owner credentials
  OWNER_SESSION: "sp_clinic_owner_session",   // Active owner session token
  CLINIC_DATA: "sp_clinic_master_data",        // Master clinic settings and profile
  PATIENT_USERS: "sp_clinic_patient_users",    // Patient accounts (phone + salted password hash)
  PATIENT_SESSION: "sp_clinic_patient_session" // Active logged-in patient session
};

// Runtime Clinic State
let CLINIC_DATA = { ...DEFAULT_CLINIC_DATA };
let currentBooking = null;
let uploadedDoctorPhotoBase64 = null;

// ================= CRYPTOGRAPHIC HELPERS =================
async function hashCredential(text, salt) {
  const normalized = (text || "").trim();
  if (typeof window !== "undefined" && window.crypto && crypto.subtle) {
    try {
      const enc = new TextEncoder();
      const data = enc.encode(`${normalized}::${salt}::SP_CLINIC_AUTH_SALT_2026`);
      const hashBuffer = await crypto.subtle.digest("SHA-256", data);
      return Array.from(new Uint8Array(hashBuffer))
        .map(b => b.toString(16).padStart(2, "0"))
        .join("");
    } catch (e) {
      console.warn("Crypto subtle fallback", e);
    }
  }
  // Deterministic fallback for environments without crypto.subtle
  let hash = 5381;
  const str = `${normalized}::${salt}::SP_CLINIC`;
  for (let i = 0; i < str.length; i++) {
    hash = ((hash << 5) + hash) + str.charCodeAt(i);
    hash |= 0;
  }
  return "hash_" + Math.abs(hash).toString(16);
}

function generateSecureSalt() {
  if (typeof window !== "undefined" && window.crypto && crypto.getRandomValues) {
    const arr = new Uint8Array(16);
    crypto.getRandomValues(arr);
    return Array.from(arr).map(b => b.toString(16).padStart(2, "0")).join("");
  }
  return Math.random().toString(36).substring(2) + Date.now().toString(36);
}

// ================= OWNER AUTHENTICATION STATE =================
function getStoredOwnerAuth() {
  try {
    if (typeof localStorage === "undefined") return null;
    const raw = localStorage.getItem(STORAGE_KEYS.OWNER_AUTH);
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

function isOwnerSetupComplete() {
  const auth = getStoredOwnerAuth();
  return Boolean(auth && auth.isSetupComplete && auth.passwordHash);
}

function isOwnerAuthenticated() {
  try {
    if (typeof sessionStorage === "undefined") return false;
    const raw = sessionStorage.getItem(STORAGE_KEYS.OWNER_SESSION);
    if (!raw) return false;
    const session = JSON.parse(raw);
    if (!session || !session.token || !session.expiresAt) return false;
    if (Date.now() > session.expiresAt) {
      sessionStorage.removeItem(STORAGE_KEYS.OWNER_SESSION);
      return false;
    }
    return true;
  } catch (e) {
    return false;
  }
}

function getActiveOwnerId() {
  try {
    if (typeof sessionStorage === "undefined") return "Owner";
    const raw = sessionStorage.getItem(STORAGE_KEYS.OWNER_SESSION);
    if (!raw) return "Owner";
    const session = JSON.parse(raw);
    return session.ownerId || "Owner";
  } catch (e) {
    return "Owner";
  }
}

function requireOwnerAuth() {
  if (!isOwnerAuthenticated()) {
    console.error("Blocked unauthorized attempt to execute owner operation.");
    alert("Access Denied: Owner authorization required for this action.");
    openOwnerPortal();
    throw new Error("Unauthorized: Owner access required.");
  }
}

// ================= APPOINTMENTS DATA LAYER =================
function getStoredAppointments() {
  try {
    if (typeof localStorage === "undefined") return [];
    const raw = localStorage.getItem(STORAGE_KEYS.APPOINTMENTS);
    if (!raw) {
      return [];
    }
    return JSON.parse(raw);
  } catch (e) {
    console.error("Storage read error", e);
    return [];
  }
}

function saveAppointments(appts) {
  try {
    if (typeof localStorage === "undefined") return;
    localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(appts));
  } catch (e) {
    console.error("Storage write error", e);
  }
}

// ================= CLINIC DATA LAYER =================
function loadClinicData() {
  try {
    if (typeof localStorage === "undefined") return;
    const raw = localStorage.getItem(STORAGE_KEYS.CLINIC_DATA);
    if (raw) {
      const parsed = JSON.parse(raw);
      CLINIC_DATA = { ...DEFAULT_CLINIC_DATA, ...parsed };
    }
  } catch (e) {
    console.warn("Error loading clinic data", e);
  }
  applyClinicDataToDOM();
}

function saveClinicData() {
  requireOwnerAuth();
  try {
    if (typeof localStorage === "undefined") return;
    localStorage.setItem(STORAGE_KEYS.CLINIC_DATA, JSON.stringify(CLINIC_DATA));
  } catch (e) {
    console.error("Error saving clinic data", e);
  }
  applyClinicDataToDOM();
}

function applyClinicDataToDOM() {
  // Update Clinic Title & Subtitles
  const navTitle = document.getElementById("navClinicTitle");
  if (navTitle) navTitle.textContent = CLINIC_DATA.clinicName;

  const navSubtitle = document.getElementById("navClinicSubtitle");
  if (navSubtitle) navSubtitle.textContent = CLINIC_DATA.clinicSubtitle;

  const welcomeTitle = document.getElementById("welcomeClinicTitle");
  if (welcomeTitle) welcomeTitle.textContent = CLINIC_DATA.clinicName;

  const welcomeSubtitle = document.getElementById("welcomeClinicSubtitle");
  if (welcomeSubtitle) welcomeSubtitle.textContent = CLINIC_DATA.clinicSubtitle;

  const welcomeDesc = document.getElementById("welcomeClinicDesc");
  if (welcomeDesc) welcomeDesc.textContent = CLINIC_DATA.doctorSpeciality;

  const footerTitle = document.getElementById("footerClinicTitle");
  if (footerTitle) footerTitle.textContent = CLINIC_DATA.clinicName;

  const footerCopyright = document.getElementById("footerCopyrightClinic");
  if (footerCopyright) footerCopyright.textContent = CLINIC_DATA.clinicName;

  // Update Doctor details
  document.querySelectorAll(".doc-name").forEach(el => el.textContent = CLINIC_DATA.doctorName);
  const docH2 = document.querySelector("#doctor h2");
  if (docH2) docH2.textContent = CLINIC_DATA.doctorName;

  const qualP = document.querySelector(".doc-qualification");
  if (qualP) qualP.textContent = `${CLINIC_DATA.doctorQualification} • ${CLINIC_DATA.doctorSpeciality}`;

  const docDescP = document.querySelector(".doc-bio, #doctor .doc-info p");
  if (docDescP && CLINIC_DATA.doctorBio) docDescP.textContent = CLINIC_DATA.doctorBio;

  // Update Doctor Photos & Clinic Logo
  if (CLINIC_DATA.doctorPhoto) {
    document.querySelectorAll("img[alt*='Dr.'], img[alt*='Doctor'], .doc-avatar img").forEach(img => {
      img.src = CLINIC_DATA.doctorPhoto;
    });
    const welcomeLogo = document.getElementById("welcomeClinicLogoImg");
    if (welcomeLogo) welcomeLogo.src = CLINIC_DATA.doctorPhoto;

    const navLogo = document.getElementById("navClinicLogoImg");
    if (navLogo) navLogo.src = CLINIC_DATA.doctorPhoto;

    const preview = document.getElementById("editDoctorPicPreview");
    if (preview) preview.src = CLINIC_DATA.doctorPhoto;
  }

  // Update Consultation Fee
  document.querySelectorAll(".fee-amount").forEach(el => el.textContent = `₹${CLINIC_DATA.fee}`);

  // Update Contact Info
  const phoneDisplay = CLINIC_DATA.phone || "+91 98765 43210";
  const cleanPhone = (CLINIC_DATA.cleanPhone || phoneDisplay.replace(/\D/g, "")).slice(-10);

  const topPhoneDisplay = document.getElementById("topPhoneDisplay");
  if (topPhoneDisplay) topPhoneDisplay.textContent = phoneDisplay;

  const topPhoneLink = document.getElementById("topPhoneLink");
  if (topPhoneLink) topPhoneLink.href = `tel:+91${cleanPhone}`;

  const topWaLink = document.getElementById("topWaLink");
  if (topWaLink) topWaLink.href = `https://wa.me/91${cleanPhone}`;

  const topBarTimings = document.getElementById("topBarTimings");
  if (topBarTimings) topBarTimings.textContent = CLINIC_DATA.consultingHours;

  const heroCall = document.getElementById("heroCallClinicBtn");
  if (heroCall) {
    heroCall.href = `tel:+91${cleanPhone}`;
    heroCall.innerHTML = `<i class="fa-solid fa-phone-volume"></i> Call Clinic (${phoneDisplay})`;
  }

  const contactAddress = document.getElementById("contactAddressDisplay");
  if (contactAddress) contactAddress.textContent = CLINIC_DATA.address;

  const contactPhone = document.getElementById("contactPhoneDisplay");
  if (contactPhone) contactPhone.textContent = phoneDisplay;

  const contactEmail = document.getElementById("contactEmailDisplay");
  if (contactEmail) contactEmail.textContent = CLINIC_DATA.email;

  const contactHours = document.getElementById("contactHoursDisplay");
  if (contactHours) contactHours.textContent = CLINIC_DATA.consultingHours;

  const contactWhatsApp = document.getElementById("contactWhatsAppLink");
  if (contactWhatsApp) contactWhatsApp.href = `https://wa.me/91${cleanPhone}?text=Hello%20${encodeURIComponent(CLINIC_DATA.doctorName)},%20I%20would%20like%20to%20inquire%20about%20a%20consultation%20at%20${encodeURIComponent(CLINIC_DATA.clinicName)}`;

  // Update In-Clinic & Online Consultation mode visibility in public booking form
  const inClinicRadio = document.getElementById("radioCardInClinic");
  const onlineRadio = document.getElementById("radioCardOnline");
  const inClinicInput = document.getElementById("radioInClinicInput");
  const onlineInput = document.getElementById("radioOnlineInput");

  if (inClinicRadio && onlineRadio) {
    inClinicRadio.style.display = CLINIC_DATA.inClinicEnabled ? "flex" : "none";
    onlineRadio.style.display = CLINIC_DATA.onlineEnabled ? "flex" : "none";

    // Auto-select the active available mode
    if (!CLINIC_DATA.inClinicEnabled && CLINIC_DATA.onlineEnabled && inClinicInput && onlineInput) {
      onlineInput.checked = true;
      inClinicRadio.classList.remove("selected");
      onlineRadio.classList.add("selected");
    } else if (CLINIC_DATA.inClinicEnabled && inClinicInput) {
      inClinicInput.checked = true;
      inClinicRadio.classList.add("selected");
      onlineRadio.classList.remove("selected");
    }
  }

  // Populate slots in public booking modal
  renderSlotsInBookingForm();
}

function renderSlotsInBookingForm() {
  const timeSelect = document.getElementById("bookTime");
  if (!timeSelect) return;

  const currentVal = timeSelect.value;
  timeSelect.innerHTML = "";

  const slots = (CLINIC_DATA.slots && CLINIC_DATA.slots.length > 0)
    ? CLINIC_DATA.slots
    : DEFAULT_CLINIC_DATA.slots;

  slots.forEach((slot, idx) => {
    const opt = document.createElement("option");
    opt.value = slot;
    opt.textContent = slot;
    if (idx === 0 || slot === currentVal) {
      opt.selected = true;
    }
    timeSelect.appendChild(opt);
  });
}

// ================= INITIALIZATION & ROUTING =================
if (typeof document !== "undefined") {
  document.addEventListener("DOMContentLoaded", () => {
    loadClinicData();
    initDateLimits();
    setupRealtimeFirestoreSync();
    checkAdminRoute();
    if (window.location.hash === '#patient-portal') {
      openPatientPortal();
    }

    // Close modals on clicking backdrop
    document.addEventListener("click", (e) => {
      if (e.target.classList && e.target.classList.contains("modal-overlay")) {
        e.target.classList.remove("active");
        document.body.style.overflow = "";
      }
    });

    // Close modals on Escape
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        document.querySelectorAll(".modal-overlay.active").forEach(m => m.classList.remove("active"));
        document.body.style.overflow = "";
      }
    });
  });

  window.addEventListener("hashchange", checkAdminRoute);
}

function checkAdminRoute() {
  const hash = window.location.hash;
  const search = new URLSearchParams(window.location.search);
  const path = window.location.pathname;

  // Admin routes: #admin, /admin, /admin/login, /admin/dashboard
  if (hash === "#admin" || hash === "#admin/dashboard" || hash === "#admin/login" || search.get("admin") === "true" || path.includes("/admin")) {
    openOwnerPortal();
    return;
  }

  // Patient routes: #patient-portal, #patient/dashboard, #login, #signup, etc.
  if (hash === "#patient-portal" || hash === "#patient/dashboard" || hash === "#patient/book" || hash === "#patient/appointments" || hash === "#patient/profile" || hash === "#login" || hash === "#signup" || path.includes("/patient") || path.includes("/login") || path.includes("/signup")) {
    openPatientPortal();
    if (hash === "#signup" || path.includes("/signup")) {
      switchPatientAuthTab("signup");
    } else {
      switchPatientAuthTab("login");
    }
  }
}

// Setup Date constraints
function initDateLimits() {
  const dateInput = document.getElementById("bookDate");
  if (!dateInput) return;

  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, "0");
  const dd = String(today.getDate()).padStart(2, "0");
  const minDate = `${yyyy}-${mm}-${dd}`;

  const horizon = CLINIC_DATA.advanceDays || 30;
  const max = new Date();
  max.setDate(today.getDate() + horizon);
  const maxYear = max.getFullYear();
  const maxMonth = String(max.getMonth() + 1).padStart(2, "0");
  const maxDay = String(max.getDate()).padStart(2, "0");
  const maxDate = `${maxYear}-${maxMonth}-${maxDay}`;

  dateInput.min = minDate;
  dateInput.max = maxDate;
  dateInput.value = minDate;
}

// Mobile Menu
function toggleMobileMenu() {
  const menu = document.getElementById("mobileMenu");
  if (menu) {
    menu.classList.toggle("open");
  }
}

// Consultation Radio Selection UI
function updateRadioSelection(input) {
  document.querySelectorAll(".radio-card").forEach(card => card.classList.remove("selected"));
  const parent = input.closest(".radio-card");
  if (parent) parent.classList.add("selected");
}

// ================= PUBLIC CLIENT BOOKING MODAL =================
function openBookingModal() {
  const modal = document.getElementById("bookingModal");
  if (modal) {
    modal.classList.add("active");
    document.body.style.overflow = "hidden";
    renderSlotsInBookingForm();
    loadAvailableSlots();

    // Pre-fill patient details automatically if patient is logged in
    const patientSession = getActivePatientSession();
    if (patientSession) {
      const nameInput = document.getElementById("patientName");
      const phoneInput = document.getElementById("patientPhone");
      if (nameInput && !nameInput.value) nameInput.value = patientSession.fullName || "";
      if (phoneInput && !phoneInput.value) phoneInput.value = patientSession.phone || "";
    }
  }
}

function closeBookingModal() {
  const modal = document.getElementById("bookingModal");
  if (modal) {
    modal.classList.remove("active");
    document.body.style.overflow = "";
  }
}

function loadAvailableSlots() {
  const selectedDate = document.getElementById("bookDate")?.value;
  const timeSelect = document.getElementById("bookTime");
  if (!selectedDate || !timeSelect) return;

  const all = getStoredAppointments();
  const bookedSlots = all
    .filter(a => a.date === selectedDate && a.status !== "CANCELLED" && a.status !== "REJECTED")
    .map(a => a.timeSlot);

  Array.from(timeSelect.options).forEach(opt => {
    if (opt.value && bookedSlots.includes(opt.value)) {
      opt.textContent = `${opt.value} (Already Booked)`;
      opt.disabled = true;
    } else if (opt.value) {
      opt.textContent = opt.value;
      opt.disabled = false;
    }
  });
}

function handleBookingSubmit(event) {
  event.preventDefault();

  const nameInput = document.getElementById("patientName");
  const phoneInput = document.getElementById("patientPhone");
  const ageInput = document.getElementById("patientAge");
  const genderInput = document.getElementById("patientGender");
  const dateInput = document.getElementById("bookDate");
  const timeInput = document.getElementById("bookTime");
  const symptomsInput = document.getElementById("symptoms");
  const consultTypeInput = document.querySelector('input[name="consultationType"]:checked');

  const patientName = nameInput.value.trim();
  const rawPhone = phoneInput.value.replace(/\D/g, "");
  const patientPhone = rawPhone.length >= 10 ? rawPhone.slice(-10) : rawPhone;
  const patientAge = ageInput.value.trim() || "30";
  const patientGender = genderInput.value;
  const date = dateInput.value;
  const timeSlot = timeInput.value;
  const symptoms = symptomsInput.value.trim();
  const consultationType = consultTypeInput ? consultTypeInput.value : "IN_CLINIC";

  if (patientName.length < 2) {
    alert("Please enter a valid patient name.");
    nameInput.focus();
    return;
  }
  if (patientPhone.length !== 10) {
    alert("Please enter a valid 10-digit mobile number.");
    phoneInput.focus();
    return;
  }
  if (!timeSlot || timeSlot.includes("Already Booked")) {
    alert("Please select an available consultation slot.");
    timeInput.focus();
    return;
  }

  // Generate Unique Ticket ID
  const randomNum = Math.floor(1000 + Math.random() * 9000);
  const appointmentId = `HM-2026-${randomNum}`;

  const session = getActivePatientSession();
  const patientId = (session && session.phone) ? ("USER-" + session.phone) : ("USER-" + patientPhone);

  const newAppointment = {
    id: appointmentId,
    appointmentId,
    patientId,
    patientName,
    patientMobile: patientPhone,
    patientPhone,
    patientAge,
    patientGender,
    consultationType,
    date,
    timeSlot,
    symptoms: symptoms || "General Homeopathy Consultation",
    status: "PENDING",
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  // Save to appointment store
  const appointments = getStoredAppointments();
  appointments.unshift(newAppointment);
  saveAppointments(appointments);

  // Sync to Firebase if present
  if (typeof window !== "undefined" && window.IS_FIREBASE_ENABLED && window.db) {
    try {
      window.db.collection("appointments").doc(appointmentId).set(newAppointment);
    } catch (e) {
      console.warn("Firebase sync error", e);
    }
  }

  currentBooking = newAppointment;

  // Auto-establish patient session for the booked phone number so they can instantly view status & history
  try {
    const patientSession = {
      phone: patientPhone,
      token: generateSecureSalt(),
      expiresAt: Date.now() + 24 * 60 * 60 * 1000
    };
    sessionStorage.setItem(STORAGE_KEYS.PATIENT_SESSION, JSON.stringify(patientSession));
  } catch (err) {
    console.warn('Patient session auto-store error', err);
  }

  // Reset form & close modal
  document.getElementById("bookingForm").reset();
  initDateLimits();
  closeBookingModal();

  // Show Confirmation Modal for patient
  showConfirmation(newAppointment);
}

// ================= PUBLIC CONFIRMATION MODAL =================
function showConfirmation(appt) {
  const idEl = document.getElementById("confirmApptId");
  if (idEl) idEl.textContent = appt.id;

  const smallId = document.getElementById("confirmApptIdSmall");
  if (smallId) smallId.textContent = appt.id;

  const nameEl = document.getElementById("confirmPatientName");
  if (nameEl) nameEl.textContent = appt.patientName;

  const phoneEl = document.getElementById("confirmPatientPhone");
  if (phoneEl) phoneEl.textContent = appt.patientPhone;

  const dtEl = document.getElementById("confirmDateTime");
  if (dtEl) dtEl.textContent = `${appt.date} at ${appt.timeSlot}`;

  const modeEl = document.getElementById("confirmMode");
  if (modeEl) {
    modeEl.textContent = appt.consultationType === "IN_CLINIC"
      ? `In-Clinic Visit (${CLINIC_DATA.clinicName})`
      : "Online Video Consultation (WhatsApp)";
  }

  const cleanPhone = (CLINIC_DATA.cleanPhone || CLINIC_DATA.phone.replace(/\D/g, "")).slice(-10);

  // Dynamic deep link for QR code
  const query = `id=${encodeURIComponent(appt.id)}&name=${encodeURIComponent(appt.patientName)}&phone=${encodeURIComponent(appt.patientPhone)}&age=${encodeURIComponent(appt.patientAge || 30)}&gender=${encodeURIComponent(appt.patientGender || "Male")}&date=${encodeURIComponent(appt.date)}&time=${encodeURIComponent(appt.timeSlot)}&mode=${encodeURIComponent(appt.consultationType)}&symptoms=${encodeURIComponent(appt.symptoms || "")}`;
  const deepLink = `homeoclinic://appointment?${query}`;

  const qrImg = document.getElementById("confirmApptQr");
  if (qrImg) {
    qrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(deepLink)}`;
  }

  const confirmPhoneLink = document.getElementById("confirmClinicPhoneLink");
  if (confirmPhoneLink) {
    confirmPhoneLink.href = `tel:+91${cleanPhone}`;
    confirmPhoneLink.textContent = CLINIC_DATA.phone;
  }

  const confirmCallBtn = document.getElementById("confirmCallBtn");
  if (confirmCallBtn) {
    confirmCallBtn.href = `tel:+91${cleanPhone}`;
  }

  const modal = document.getElementById("confirmationModal");
  if (modal) {
    modal.classList.add("active");
    document.body.style.overflow = "hidden";
  }
}

function closeConfirmationModal() {
  const modal = document.getElementById("confirmationModal");
  if (modal) {
    modal.classList.remove("active");
    document.body.style.overflow = "";
  }
}

function sendWhatsAppConfirmation() {
  if (!currentBooking) return;
  const cleanPhone = (CLINIC_DATA.cleanPhone || CLINIC_DATA.phone.replace(/\D/g, "")).slice(-10);
  const query = `id=${encodeURIComponent(currentBooking.id)}&name=${encodeURIComponent(currentBooking.patientName)}&phone=${encodeURIComponent(currentBooking.patientPhone)}&age=${encodeURIComponent(currentBooking.patientAge || 30)}&gender=${encodeURIComponent(currentBooking.patientGender || "Male")}&date=${encodeURIComponent(currentBooking.date)}&time=${encodeURIComponent(currentBooking.timeSlot)}&mode=${encodeURIComponent(currentBooking.consultationType)}&symptoms=${encodeURIComponent(currentBooking.symptoms || "")}`;
  const deepLink = `homeoclinic://appointment?${query}`;

  const msg = `*${encodeURIComponent(CLINIC_DATA.clinicName)} - Appointment Request*%0A%0A` +
    `*Doctor:* ${encodeURIComponent(CLINIC_DATA.doctorName)} (${encodeURIComponent(CLINIC_DATA.doctorQualification)})%0A` +
    `*Patient Name:* ${encodeURIComponent(currentBooking.patientName)}%0A` +
    `*Token ID:* ${encodeURIComponent(currentBooking.id)}%0A` +
    `*Date:* ${encodeURIComponent(currentBooking.date)}%0A` +
    `*Time Slot:* ${encodeURIComponent(currentBooking.timeSlot)}%0A` +
    `*Mode:* ${currentBooking.consultationType === "IN_CLINIC" ? `In-Clinic Visit at ${encodeURIComponent(CLINIC_DATA.clinicName)}` : "Online Video Consultation"}%0A` +
    `*Fee:* ₹${CLINIC_DATA.fee}%0A%0A` +
    `*📲 Open in Clinic App:*%0A${encodeURIComponent(deepLink)}%0A%0A` +
    `_Kindly confirm my appointment slot. Thank you!_`;

  window.open(`https://wa.me/91${cleanPhone}?text=${msg}`, "_blank");
}

// ================= SECURE OWNER PORTAL CONTROLLER =================
function openOwnerPortal() {
  const modal = document.getElementById("adminModal");
  if (modal) {
    modal.classList.add("active");
    document.body.style.overflow = "hidden";
    updateOwnerPortalUI();
  }
}

function closeAdminModal() {
  const modal = document.getElementById("adminModal");
  if (modal) {
    modal.classList.remove("active");
    document.body.style.overflow = "";
    if (window.location.hash === "#admin") {
      history.pushState("", document.title, window.location.pathname + window.location.search);
    }
  }
}

function previewSetupPhoto(input) {
  if (input.files && input.files[0]) {
    const reader = new FileReader();
    reader.onload = function(e) {
      uploadedDoctorPhotoBase64 = e.target.result;
      const previewBox = document.getElementById("setupPhotoPreviewBox");
      const previewImg = document.getElementById("setupPhotoPreviewImg");
      if (previewBox && previewImg) {
        previewImg.src = uploadedDoctorPhotoBase64;
        previewBox.style.display = "flex";
      }
    };
    reader.readAsDataURL(input.files[0]);
  }
}

function updateOwnerPortalUI() {
  const setupView = document.getElementById("ownerSetupView");
  const loginView = document.getElementById("ownerLoginView");
  const dashView = document.getElementById("adminDashboardView");
  const modalCard = document.getElementById("adminModalCard");
  const modalTitle = document.getElementById("adminModalTitle");
  const modalSubtitle = document.getElementById("adminModalSubtitle");

  const isSetupDone = isOwnerSetupComplete();
  const isAuth = isOwnerAuthenticated();

  if (!isSetupDone) {
    // 1. First-time setup: prompt Owner to configure clinic details & secret password
    if (setupView) setupView.style.display = "block";
    if (loginView) loginView.style.display = "none";
    if (dashView) dashView.style.display = "none";
    if (modalCard) {
      modalCard.classList.remove("modal-lg");
      modalCard.classList.add("modal-sm");
    }
    if (modalTitle) modalTitle.innerHTML = `<i class="fa-solid fa-pen-to-square"></i> Set Up Your Clinic &amp; Password`;
    if (modalSubtitle) modalSubtitle.textContent = `Enter your real clinic info and create your secret password`;

    // Pre-fill fields with current clinic data if present
    const cName = document.getElementById("setupClinicName");
    if (cName && !cName.value) cName.value = CLINIC_DATA.clinicName || "";

    const dName = document.getElementById("setupDoctorName");
    if (dName && !dName.value) dName.value = CLINIC_DATA.doctorName || "";

    const cPhone = document.getElementById("setupClinicPhone");
    if (cPhone && !cPhone.value) cPhone.value = CLINIC_DATA.cleanPhone || "";

    const cAddr = document.getElementById("setupClinicAddress");
    if (cAddr && !cAddr.value) cAddr.value = CLINIC_DATA.address || "";
  } else if (!isAuth) {
    // 2. Setup is done, but not logged in: show password unlock screen
    if (setupView) setupView.style.display = "none";
    if (loginView) loginView.style.display = "block";
    if (dashView) dashView.style.display = "none";
    if (modalCard) {
      modalCard.classList.remove("modal-lg");
      modalCard.classList.add("modal-sm");
    }
    if (modalTitle) modalTitle.innerHTML = `<i class="fa-solid fa-lock"></i> Owner / Admin Login`;
    if (modalSubtitle) modalSubtitle.textContent = `Private Clinic & Doctor Administration (/admin/login)`;
  } else {
    // 3. Authenticated: show full Owner Dashboard
    if (setupView) setupView.style.display = "none";
    if (loginView) loginView.style.display = "none";
    if (dashView) dashView.style.display = "block";
    if (modalCard) {
      modalCard.classList.remove("modal-sm");
      modalCard.classList.add("modal-lg");
    }
    if (modalTitle) modalTitle.innerHTML = `<i class="fa-solid fa-hospital-user"></i> ${CLINIC_DATA.clinicName} — Owner Dashboard`;
    if (modalSubtitle) modalSubtitle.textContent = `Full Control & Website Management`;

    const activeDisplay = document.getElementById("activeOwnerDisplay");
    if (activeDisplay) activeDisplay.textContent = getActiveOwnerId();

    renderAdminDashboard();
  }
}

// 1. Initial Setup Handler: Saves real clinic details and private password
async function handleOwnerSetup(event) {
  event.preventDefault();

  const clinicNameInput = document.getElementById("setupClinicName");
  const docNameInput = document.getElementById("setupDoctorName");
  const phoneInput = document.getElementById("setupClinicPhone");
  const addrInput = document.getElementById("setupClinicAddress");
  const passInput = document.getElementById("setupOwnerPassword");
  const confirmInput = document.getElementById("setupOwnerPasswordConfirm");
  const errorDiv = document.getElementById("setupErrorMsg");

  const clinicName = (clinicNameInput ? clinicNameInput.value : "").trim();
  const doctorName = (docNameInput ? docNameInput.value : "").trim();
  const rawPhone = (phoneInput ? phoneInput.value : "").trim();
  const address = (addrInput ? addrInput.value : "").trim();
  const password = passInput ? passInput.value : "";
  const confirmPass = confirmInput ? confirmInput.value : "";

  if (!clinicName) {
    showAuthError(errorDiv, "Please enter your clinic name.");
    return;
  }
  if (!doctorName) {
    showAuthError(errorDiv, "Please enter the doctor name.");
    return;
  }
  const cleanPhone = rawPhone.replace(/\D/g, "");
  if (cleanPhone.length < 10) {
    showAuthError(errorDiv, "Please enter your valid 10-digit mobile number.");
    return;
  }
  if (!address) {
    showAuthError(errorDiv, "Please enter your clinic address.");
    return;
  }
  if (password.length < 4) {
    showAuthError(errorDiv, "Password must be at least 4 characters long.");
    return;
  }
  if (password !== confirmPass) {
    showAuthError(errorDiv, "Passwords do not match. Please re-type your password.");
    return;
  }

  // Update master clinic data with owner's real details
  CLINIC_DATA.clinicName = clinicName;
  CLINIC_DATA.doctorName = doctorName;
  CLINIC_DATA.phone = rawPhone.startsWith("+91") ? rawPhone : `+91 ${cleanPhone.slice(-10)}`;
  CLINIC_DATA.cleanPhone = cleanPhone.slice(-10);
  CLINIC_DATA.address = address;
  if (uploadedDoctorPhotoBase64) {
    CLINIC_DATA.doctorPhoto = uploadedDoctorPhotoBase64;
  }

  // Save to persistent storage
  try {
    localStorage.setItem(STORAGE_KEYS.CLINIC_DATA, JSON.stringify(CLINIC_DATA));
  } catch (e) {
    console.error("Save clinic data error", e);
  }

  // Create salted password hash
  const salt = generateSecureSalt();
  const passwordHash = await hashCredential(password, salt);

  const authRecord = {
    isSetupComplete: true,
    passwordHash,
    salt,
    createdAt: new Date().toISOString()
  };
  localStorage.setItem(STORAGE_KEYS.OWNER_AUTH, JSON.stringify(authRecord));

  // Create active session
  const session = {
    ownerId: doctorName || "Clinic Owner",
    token: generateSecureSalt(),
    expiresAt: Date.now() + 2 * 60 * 60 * 1000 // 2 hours
  };
  sessionStorage.setItem(STORAGE_KEYS.OWNER_SESSION, JSON.stringify(session));

  if (passInput) passInput.value = "";
  if (confirmInput) confirmInput.value = "";
  if (errorDiv) errorDiv.style.display = "none";

  // Immediately apply new clinic name, phone, photo across the entire site
  applyClinicDataToDOM();
  updateOwnerPortalUI();
}

// 2. Owner Login Handler: Unlocks using Owner's chosen password
async function handleOwnerLogin(event) {
  event.preventDefault();
  const passInput = document.getElementById("ownerLoginPassword");
  const errorDiv = document.getElementById("loginErrorMsg");
  const errorText = document.getElementById("loginErrorText");

  const password = passInput ? passInput.value : "";
  const auth = getStoredOwnerAuth();

  if (!auth || !auth.passwordHash) {
    updateOwnerPortalUI();
    return;
  }

  const checkPassHash = await hashCredential(password, auth.salt);

  if (checkPassHash === auth.passwordHash) {
    // Password correct
    if (errorDiv) errorDiv.style.display = "none";
    const session = {
      ownerId: CLINIC_DATA.doctorName || "Clinic Owner",
      token: generateSecureSalt(),
      expiresAt: Date.now() + 2 * 60 * 60 * 1000
    };
    sessionStorage.setItem(STORAGE_KEYS.OWNER_SESSION, JSON.stringify(session));

    if (passInput) passInput.value = "";
    updateOwnerPortalUI();
  } else {
    // Password incorrect
    if (errorDiv) {
      if (errorText) errorText.textContent = "Invalid Password. Please enter the password you created.";
      errorDiv.style.display = "flex";
    }
    if (passInput) passInput.value = "";
  }
}

// 3. Owner Logout Handler
function handleOwnerLogout() {
  sessionStorage.removeItem(STORAGE_KEYS.OWNER_SESSION);
  updateOwnerPortalUI();
}

function showAuthError(el, msg) {
  if (el) {
    el.textContent = msg;
    el.style.display = "block";
  } else {
    alert(msg);
  }
}

// ================= OWNER DASHBOARD CONTROLLERS =================
function switchAdminTab(tabName) {
  requireOwnerAuth();

  const tabs = ["appts", "patients", "slots", "profile", "clinic", "security"];
  tabs.forEach(t => {
    const content = document.getElementById(`admin${t.charAt(0).toUpperCase() + t.slice(1)}TabContent`);
    const btn = document.getElementById(`adminTab${t.charAt(0).toUpperCase() + t.slice(1)}Btn`);
    if (content) content.style.display = (t === tabName) ? "block" : "none";
    if (btn) {
      if (t === tabName) {
        btn.classList.add("active");
      } else {
        btn.classList.remove("active");
      }
    }
  });

  if (tabName === "appts") {
    renderAdminAppointments();
  } else if (tabName === "patients") {
    renderAdminPatients();
  } else if (tabName === "slots") {
    renderAdminSlots();
  } else if (tabName === "profile") {
    populateDoctorProfileTab();
  } else if (tabName === "clinic") {
    populateClinicTab();
  }
}

function renderAdminDashboard() {
  requireOwnerAuth();
  const appointments = getStoredAppointments();

  const total = appointments.length;
  const pending = appointments.filter(a => a.status === "PENDING").length;
  const confirmed = appointments.filter(a => a.status === "CONFIRMED").length;

  const totalEl = document.getElementById("statTotalAppointments");
  if (totalEl) totalEl.textContent = total;

  const pendingEl = document.getElementById("statPendingAppointments");
  if (pendingEl) pendingEl.textContent = pending;

  const confirmedEl = document.getElementById("statConfirmedAppointments");
  if (confirmedEl) confirmedEl.textContent = confirmed;

  filterAdminAppointments();
  renderAdminSlots();
  populateDoctorProfileTab();
  populateClinicTab();
}

// TAB 1: Appointments Queue
function filterAdminAppointments() {
  requireOwnerAuth();
  const query = (document.getElementById("adminSearchInput")?.value || "").toLowerCase().trim();
  const statusFilter = document.getElementById("adminStatusFilter")?.value || "ALL";

  const appointments = getStoredAppointments();
  const filtered = appointments.filter(a => {
    const matchesQuery = (a.patientName || "").toLowerCase().includes(query) ||
      (a.patientPhone || "").includes(query) ||
      (a.id || "").toLowerCase().includes(query) ||
      (a.symptoms || "").toLowerCase().includes(query);
    const matchesStatus = statusFilter === "ALL" || a.status === statusFilter;
    return matchesQuery && matchesStatus;
  });

  const listContainer = document.getElementById("adminAppointmentsList");
  if (!listContainer) return;

  if (filtered.length === 0) {
    listContainer.innerHTML = `<div style="text-align:center; padding:30px; color:#64748b; font-size:14px;">No matching appointments found.</div>`;
    return;
  }

  listContainer.innerHTML = filtered.map(a => `
    <div class="admin-appt-card status-${(a.status || "pending").toLowerCase()}">
      <div class="admin-appt-card-top">
        <div>
          <span class="admin-appt-id">${a.id}</span>
          <h4 style="font-size:15px; margin:2px 0; color:#0f172a;">${a.patientName} (${a.patientAge || 30}y, ${a.patientGender || "Male"})</h4>
          <span style="font-size:13px; color:#0d9488; font-weight:600;"><i class="fa-solid fa-phone"></i> +91 ${a.patientPhone}</span>
        </div>
        <span class="badge-${a.status === "CONFIRMED" ? "success" : a.status === "PENDING" ? "warning" : "secondary"}" style="font-size:11px; padding:3px 8px; border-radius:4px; font-weight:700;">
          ${a.status}
        </span>
      </div>
      <div style="font-size:13px; color:#475569; margin:4px 0;">
        <i class="fa-regular fa-calendar"></i> ${a.date} &bull; <i class="fa-regular fa-clock"></i> ${a.timeSlot} &bull; 
        <strong>${a.consultationType === "IN_CLINIC" ? "In-Clinic Visit" : "Online Video Consultation"}</strong>
      </div>
      <div style="font-size:12px; color:#64748b; background:#f8fafc; padding:6px 10px; border-radius:6px; margin-top:6px;">
        <strong>Symptoms:</strong> ${a.symptoms || "General Checkup"}
      </div>
      <div class="admin-appt-actions" style="flex-wrap:wrap; margin-top:10px;">
        ${a.status !== "CONFIRMED" ? `
          <button class="btn btn-primary btn-sm" style="background:#059669; border-color:#059669; font-weight:700;" onclick="confirmAndNotifyPatient('${a.id}')">
            <i class="fa-solid fa-check"></i> CONFIRM
          </button>
        ` : `
          <button class="btn btn-outline btn-sm" onclick="confirmAndNotifyPatient('${a.id}')" title="Resend WhatsApp Confirmation">
            <i class="fa-brands fa-whatsapp"></i> Confirmed (WhatsApp)
          </button>
        `}
        ${a.status !== "REJECTED" && a.status !== "CANCELLED" ? `
          <button class="btn btn-secondary btn-sm" style="color:#dc2626; font-weight:700;" onclick="rejectAppointment('${a.id}')">
            <i class="fa-solid fa-ban"></i> REJECT
          </button>
        ` : ""}
        <button class="btn btn-outline btn-sm" style="color:#d97706; font-weight:700;" onclick="openRescheduleModal('${a.id}')">
          <i class="fa-solid fa-calendar-days"></i> RESCHEDULE
        </button>
        <button class="btn btn-outline btn-sm" style="color:#0284c7; font-weight:700;" onclick="switchAdminTab('patients'); document.getElementById('adminPatientSearchInput').value = '${a.patientPhone}'; renderAdminPatients();">
          <i class="fa-solid fa-user"></i> VIEW PATIENT
        </button>
        <select onchange="changeAppointmentStatusFromSelect('${a.id}', this.value)" class="form-control form-control-sm" style="width:auto; display:inline-block; font-size:12px;">
          <option value="PENDING" ${a.status === "PENDING" ? "selected" : ""}>Pending</option>
          <option value="CONFIRMED" ${a.status === "CONFIRMED" ? "selected" : ""}>Confirmed</option>
          <option value="COMPLETED" ${a.status === "COMPLETED" ? "selected" : ""}>Completed</option>
          <option value="REJECTED" ${a.status === "REJECTED" ? "selected" : ""}>Rejected</option>
          <option value="CANCELLED" ${a.status === "CANCELLED" ? "selected" : ""}>Cancelled</option>
          <option value="RESCHEDULED" ${a.status === "RESCHEDULED" ? "selected" : ""}>Rescheduled</option>
        </select>
        <a href="https://wa.me/91${a.patientPhone}?text=Hello%20${encodeURIComponent(a.patientName)},%20this%20is%20${encodeURIComponent(CLINIC_DATA.doctorName)}%20from%20${encodeURIComponent(CLINIC_DATA.clinicName)}%20regarding%20your%20appointment%20${a.id}." target="_blank" class="btn btn-whatsapp btn-sm">
          <i class="fa-brands fa-whatsapp"></i> Chat
        </a>
      </div>
    </div>
  `).join("");
}

function renderAdminAppointments() {
  filterAdminAppointments();
}

function confirmAndNotifyPatient(id) {
  requireOwnerAuth();
  const appointments = getStoredAppointments();
  const target = appointments.find(a => a.id === id);
  if (!target) return;

  target.status = "CONFIRMED";
  saveAppointments(appointments);

  // Sync to Firebase
  if (typeof window !== "undefined" && window.IS_FIREBASE_ENABLED && window.db) {
    try {
      window.db.collection("appointments").doc(id).update({ status: "CONFIRMED" });
    } catch (e) {
      console.warn("Firebase update error", e);
    }
  }

  renderAdminDashboard();

  const cleanPhone = (CLINIC_DATA.cleanPhone || CLINIC_DATA.phone.replace(/\D/g, "")).slice(-10);
  const msg = `*${encodeURIComponent(CLINIC_DATA.clinicName)} - Appointment Confirmed!*%0A%0A` +
    `Dear *${encodeURIComponent(target.patientName)}*,%0A` +
    `Your appointment with *${encodeURIComponent(CLINIC_DATA.doctorName)}* is *APPROVED & CONFIRMED*!%0A%0A` +
    `*Token ID:* ${encodeURIComponent(target.id)}%0A` +
    `*Date:* ${encodeURIComponent(target.date)}%0A` +
    `*Time Slot:* ${encodeURIComponent(target.timeSlot)}%0A` +
    `*Mode:* ${target.consultationType === "IN_CLINIC" ? `In-Clinic Visit (${encodeURIComponent(CLINIC_DATA.address)})` : "Online Video Consultation"}%0A` +
    `*Fee:* ₹${CLINIC_DATA.fee}%0A%0A` +
    `_Looking forward to your consultation. Have questions? Call ${encodeURIComponent(CLINIC_DATA.phone)}._`;

  window.open(`https://wa.me/91${target.patientPhone}?text=${msg}`, "_blank");
}

function rejectAppointment(id) {
  requireOwnerAuth();
  if (confirm("Are you sure you want to reject this appointment?")) {
    const appointments = getStoredAppointments();
    const target = appointments.find(a => a.id === id);
    updateAppointmentStatus(id, "REJECTED");

    if (target) {
      const msg = "*" + encodeURIComponent(CLINIC_DATA.clinicName) + " - Appointment Update*%0A%0A" +
        "Dear *" + encodeURIComponent(target.patientName) + "*,%0A" +
        "Your appointment request (" + encodeURIComponent(target.id) + ") for *" + encodeURIComponent(target.date) + "* has been updated to REJECTED.%0A%0A" +
        "_Please choose another slot or call " + encodeURIComponent(CLINIC_DATA.phone) + "._";
      window.open("https://wa.me/91" + target.patientPhone + "?text=" + msg, "_blank");
    }
  }
}

function changeAppointmentStatusFromSelect(id, newStatus) {
  requireOwnerAuth();
  updateAppointmentStatus(id, newStatus);
}

function updateAppointmentStatus(id, newStatus) {
  requireOwnerAuth();
  const appointments = getStoredAppointments();
  const target = appointments.find(a => a.id === id);
  if (!target) return;

  target.status = newStatus;
  saveAppointments(appointments);

  if (typeof window !== "undefined" && window.IS_FIREBASE_ENABLED && window.db) {
    try {
      window.db.collection("appointments").doc(id).update({ status: newStatus });
    } catch (e) {
      console.warn("Firebase update error", e);
    }
  }

  renderAdminDashboard();
}

function deleteAppointmentRecord(id) {
  requireOwnerAuth();
  if (confirm(`Permanently delete appointment ${id}? This cannot be undone.`)) {
    let appointments = getStoredAppointments();
    appointments = appointments.filter(a => a.id !== id);
    saveAppointments(appointments);

    if (typeof window !== "undefined" && window.IS_FIREBASE_ENABLED && window.db) {
      try {
        window.db.collection("appointments").doc(id).delete();
      } catch (e) {
        console.warn("Firebase delete notice", e);
      }
    }

    renderAdminDashboard();
  }
}

function exportAppointmentsCSV() {
  requireOwnerAuth();
  const appointments = getStoredAppointments();
  if (appointments.length === 0) {
    alert("No appointments available to export.");
    return;
  }

  const headers = ["Appointment ID", "Patient Name", "Phone", "Age", "Gender", "Date", "Time Slot", "Mode", "Symptoms", "Status", "Booked At"];
  const rows = appointments.map(a => [
    `"${a.id}"`,
    `"${a.patientName}"`,
    `"${a.patientPhone}"`,
    `"${a.patientAge}"`,
    `"${a.patientGender}"`,
    `"${a.date}"`,
    `"${a.timeSlot}"`,
    `"${a.consultationType}"`,
    `"${(a.symptoms || "").replace(/"/g, '""')}"`,
    `"${a.status}"`,
    `"${a.createdAt}"`
  ]);

  const csvContent = "data:text/csv;charset=utf-8," + [headers.join(","), ...rows.map(e => e.join(","))].join("\n");
  const encodedUri = encodeURI(csvContent);
  const link = document.createElement("a");
  link.setAttribute("href", encodedUri);
  link.setAttribute("download", `sp_clinic_appointments_${new Date().toISOString().split("T")[0]}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

// TAB 2: Manage Slots
function renderAdminSlots() {
  const container = document.getElementById("adminSlotsListContainer");
  if (!container) return;

  const slots = CLINIC_DATA.slots || DEFAULT_CLINIC_DATA.slots;
  container.innerHTML = slots.map(slot => `
    <span class="slot-tag">
      <i class="fa-regular fa-clock" style="font-size:11px; color:#0d9488;"></i> ${slot}
      <button class="remove-slot-btn" onclick="handleRemoveSlot('${slot}')" title="Remove slot">&times;</button>
    </span>
  `).join("");

  const advanceInp = document.getElementById("editAdvanceDays");
  if (advanceInp) advanceInp.value = CLINIC_DATA.advanceDays || 30;

  const daysInp = document.getElementById("editConsultingDays");
  if (daysInp) daysInp.value = CLINIC_DATA.consultingDays || "Monday - Saturday (Sunday Closed)";
}

function handleAddSlot() {
  requireOwnerAuth();
  const input = document.getElementById("newSlotInput");
  if (!input) return;
  const val = input.value.trim();
  if (!val) {
    alert("Please enter a time slot (e.g. 04:30 PM - 05:00 PM)");
    return;
  }

  if (!CLINIC_DATA.slots) CLINIC_DATA.slots = [...DEFAULT_CLINIC_DATA.slots];
  if (CLINIC_DATA.slots.includes(val)) {
    alert("This slot already exists.");
    return;
  }

  CLINIC_DATA.slots.push(val);
  input.value = "";
  saveClinicData();
  renderAdminSlots();
}

function handleRemoveSlot(slot) {
  requireOwnerAuth();
  if (CLINIC_DATA.slots && CLINIC_DATA.slots.length <= 1) {
    alert("At least one time slot is required.");
    return;
  }
  CLINIC_DATA.slots = (CLINIC_DATA.slots || DEFAULT_CLINIC_DATA.slots).filter(s => s !== slot);
  saveClinicData();
  renderAdminSlots();
}

function resetSlotsToDefault() {
  requireOwnerAuth();
  if (confirm("Reset all time slots to standard clinic hours?")) {
    CLINIC_DATA.slots = [...DEFAULT_CLINIC_DATA.slots];
    saveClinicData();
    renderAdminSlots();
  }
}

function saveSlotsConfiguration() {
  requireOwnerAuth();
  const advanceInp = document.getElementById("editAdvanceDays");
  if (advanceInp) CLINIC_DATA.advanceDays = Number(advanceInp.value) || 30;

  const daysInp = document.getElementById("editConsultingDays");
  if (daysInp) CLINIC_DATA.consultingDays = daysInp.value.trim();

  saveClinicData();
  initDateLimits();

  const msg = document.getElementById("slotsSaveMsg");
  if (msg) {
    msg.style.display = "flex";
    setTimeout(() => { msg.style.display = "none"; }, 3500);
  }
}

// TAB 3: Doctor Profile
function populateDoctorProfileTab() {
  const nameInp = document.getElementById("editDoctorName");
  if (nameInp) nameInp.value = CLINIC_DATA.doctorName;

  const qualInp = document.getElementById("editDoctorQual");
  if (qualInp) qualInp.value = CLINIC_DATA.doctorQualification;

  const feeInp = document.getElementById("editDoctorFee");
  if (feeInp) feeInp.value = CLINIC_DATA.fee;

  const specInp = document.getElementById("editDoctorSpec");
  if (specInp) specInp.value = CLINIC_DATA.doctorSpeciality;

  const bioInp = document.getElementById("editDoctorBio");
  if (bioInp) bioInp.value = CLINIC_DATA.doctorBio;

  const preview = document.getElementById("editDoctorPicPreview");
  if (preview && CLINIC_DATA.doctorPhoto) preview.src = CLINIC_DATA.doctorPhoto;
}

function previewDoctorPhoto(input) {
  if (input.files && input.files[0]) {
    const reader = new FileReader();
    reader.onload = function(e) {
      uploadedDoctorPhotoBase64 = e.target.result;
      const preview = document.getElementById("editDoctorPicPreview");
      if (preview) preview.src = uploadedDoctorPhotoBase64;
    };
    reader.readAsDataURL(input.files[0]);
  }
}

function previewDoctorPhotoUrl(url) {
  if (url && url.trim()) {
    uploadedDoctorPhotoBase64 = url.trim();
    const preview = document.getElementById("editDoctorPicPreview");
    if (preview) preview.src = uploadedDoctorPhotoBase64;
  }
}

function saveDoctorProfile() {
  requireOwnerAuth();
  const nameInp = document.getElementById("editDoctorName");
  const qualInp = document.getElementById("editDoctorQual");
  const feeInp = document.getElementById("editDoctorFee");
  const specInp = document.getElementById("editDoctorSpec");
  const bioInp = document.getElementById("editDoctorBio");

  if (nameInp && nameInp.value.trim()) CLINIC_DATA.doctorName = nameInp.value.trim();
  if (qualInp && qualInp.value.trim()) CLINIC_DATA.doctorQualification = qualInp.value.trim();
  if (feeInp && feeInp.value.trim()) CLINIC_DATA.fee = Number(feeInp.value.trim()) || 500;
  if (specInp && specInp.value.trim()) CLINIC_DATA.doctorSpeciality = specInp.value.trim();
  if (bioInp && bioInp.value.trim()) CLINIC_DATA.doctorBio = bioInp.value.trim();

  if (uploadedDoctorPhotoBase64) {
    CLINIC_DATA.doctorPhoto = uploadedDoctorPhotoBase64;
  }

  saveClinicData();

  const msg = document.getElementById("profileSaveMsg");
  if (msg) {
    msg.style.display = "flex";
    setTimeout(() => { msg.style.display = "none"; }, 3500);
  }
}

// TAB 4: Clinic Details & Settings
function populateClinicTab() {
  const nameInp = document.getElementById("editClinicName");
  if (nameInp) nameInp.value = CLINIC_DATA.clinicName;

  const tagInp = document.getElementById("editClinicTagline");
  if (tagInp) tagInp.value = CLINIC_DATA.clinicSubtitle;

  const phoneInp = document.getElementById("editClinicPhone");
  if (phoneInp) phoneInp.value = CLINIC_DATA.phone;

  const emailInp = document.getElementById("editClinicEmail");
  if (emailInp) emailInp.value = CLINIC_DATA.email;

  const addrInp = document.getElementById("editClinicAddress");
  if (addrInp) addrInp.value = CLINIC_DATA.address;

  const hoursInp = document.getElementById("editClinicHours");
  if (hoursInp) hoursInp.value = CLINIC_DATA.consultingHours;

  const inClinicCb = document.getElementById("editInClinicEnabled");
  if (inClinicCb) inClinicCb.checked = Boolean(CLINIC_DATA.inClinicEnabled);

  const onlineCb = document.getElementById("editOnlineEnabled");
  if (onlineCb) onlineCb.checked = Boolean(CLINIC_DATA.onlineEnabled);
}

function saveClinicDetails() {
  requireOwnerAuth();
  const nameInp = document.getElementById("editClinicName");
  const tagInp = document.getElementById("editClinicTagline");
  const phoneInp = document.getElementById("editClinicPhone");
  const emailInp = document.getElementById("editClinicEmail");
  const addrInp = document.getElementById("editClinicAddress");
  const hoursInp = document.getElementById("editClinicHours");
  const inClinicCb = document.getElementById("editInClinicEnabled");
  const onlineCb = document.getElementById("editOnlineEnabled");

  if (nameInp && nameInp.value.trim()) CLINIC_DATA.clinicName = nameInp.value.trim();
  if (tagInp && tagInp.value.trim()) CLINIC_DATA.clinicSubtitle = tagInp.value.trim();
  if (phoneInp && phoneInp.value.trim()) {
    CLINIC_DATA.phone = phoneInp.value.trim();
    CLINIC_DATA.cleanPhone = phoneInp.value.replace(/\D/g, "");
  }
  if (emailInp && emailInp.value.trim()) CLINIC_DATA.email = emailInp.value.trim();
  if (addrInp && addrInp.value.trim()) CLINIC_DATA.address = addrInp.value.trim();
  if (hoursInp && hoursInp.value.trim()) CLINIC_DATA.consultingHours = hoursInp.value.trim();

  if (inClinicCb) CLINIC_DATA.inClinicEnabled = inClinicCb.checked;
  if (onlineCb) CLINIC_DATA.onlineEnabled = onlineCb.checked;

  saveClinicData();

  const msg = document.getElementById("clinicSaveMsg");
  if (msg) {
    msg.style.display = "flex";
    setTimeout(() => { msg.style.display = "none"; }, 3500);
  }
}

// TAB 5: Security & Credentials Management
async function handleChangeOwnerId(event) {
  event.preventDefault();
  requireOwnerAuth();

  const passInput = document.getElementById("changeIdCurrentPassword");
  const newIdInput = document.getElementById("newOwnerIdInput");
  const msgDiv = document.getElementById("changeIdMsg");

  const currentPass = passInput ? passInput.value : "";
  const newId = (newIdInput ? newIdInput.value : "").trim();

  if (newId.length < 3) {
    displayFeedback(msgDiv, "New Owner ID must be at least 3 characters.", false);
    return;
  }

  const auth = getStoredOwnerAuth();
  if (!auth) return;

  const testPassHash = await hashCredential(currentPass, auth.salt);
  if (testPassHash !== auth.passwordHash) {
    displayFeedback(msgDiv, "Incorrect current password.", false);
    return;
  }

  // Update Owner ID
  auth.ownerIdHash = await hashCredential(newId.toLowerCase(), auth.salt);
  localStorage.setItem(STORAGE_KEYS.OWNER_AUTH, JSON.stringify(auth));

  // Update current session
  const session = JSON.parse(sessionStorage.getItem(STORAGE_KEYS.OWNER_SESSION) || "{}");
  session.ownerId = newId;
  sessionStorage.setItem(STORAGE_KEYS.OWNER_SESSION, JSON.stringify(session));

  if (passInput) passInput.value = "";
  if (newIdInput) newIdInput.value = "";

  const activeDisplay = document.getElementById("activeOwnerDisplay");
  if (activeDisplay) activeDisplay.textContent = newId;

  displayFeedback(msgDiv, "✓ Owner ID successfully updated!", true);
}

async function handleChangePassword(event) {
  event.preventDefault();
  requireOwnerAuth();

  const currentPassInput = document.getElementById("currentPasswordInput");
  const newPassInput = document.getElementById("newPasswordInput");
  const confirmPassInput = document.getElementById("confirmNewPasswordInput");
  const msgDiv = document.getElementById("changePassMsg");

  const currentPass = currentPassInput ? currentPassInput.value : "";
  const newPass = newPassInput ? newPassInput.value : "";
  const confirmPass = confirmPassInput ? confirmPassInput.value : "";

  if (newPass.length < 6) {
    displayFeedback(msgDiv, "New password must be at least 6 characters.", false);
    return;
  }
  if (newPass !== confirmPass) {
    displayFeedback(msgDiv, "New passwords do not match.", false);
    return;
  }

  const auth = getStoredOwnerAuth();
  if (!auth) return;

  const testPassHash = await hashCredential(currentPass, auth.salt);
  if (testPassHash !== auth.passwordHash) {
    displayFeedback(msgDiv, "Incorrect current password.", false);
    return;
  }

  // Re-salt and update password
  const newSalt = generateSecureSalt();
  const currentOwnerId = getActiveOwnerId();
  auth.salt = newSalt;
  auth.ownerIdHash = await hashCredential(currentOwnerId.toLowerCase(), newSalt);
  auth.passwordHash = await hashCredential(newPass, newSalt);

  localStorage.setItem(STORAGE_KEYS.OWNER_AUTH, JSON.stringify(auth));

  if (currentPassInput) currentPassInput.value = "";
  if (newPassInput) newPassInput.value = "";
  if (confirmPassInput) confirmPassInput.value = "";

  displayFeedback(msgDiv, "✓ Master Password successfully updated!", true);
}

function displayFeedback(el, text, isSuccess) {
  if (!el) return;
  el.className = isSuccess ? "auth-alert auth-alert-success" : "auth-alert auth-alert-error";
  el.textContent = text;
  el.style.display = "flex";
  setTimeout(() => { el.style.display = "none"; }, 4000);
}

// Module export for Node.js / testing environments
if (typeof module !== "undefined" && module.exports) {
  module.exports = {
    DEFAULT_CLINIC_DATA,
    STORAGE_KEYS,
    getStoredAppointments,
    saveAppointments,
    loadClinicData,
    hashCredential
  };
}


// ================= REAL-TIME FIRESTORE SYNCHRONIZATION =================
let firestoreUnsubscribe = null;

function setupRealtimeFirestoreSync() {
  if (typeof window === "undefined" || !window.IS_FIREBASE_ENABLED || !window.db) {
    return;
  }
  try {
    if (firestoreUnsubscribe) {
      firestoreUnsubscribe();
    }
    firestoreUnsubscribe = window.db.collection("appointments").onSnapshot((snapshot) => {
      const remoteAppts = [];
      snapshot.forEach(doc => {
        remoteAppts.push(doc.data());
      });
      if (remoteAppts.length > 0) {
        // Merge with local storage ensuring newest data
        const local = getStoredAppointments();
        const map = new Map();
        local.forEach(a => map.set(a.id, a));
        remoteAppts.forEach(a => map.set(a.id, { ...(map.get(a.id) || {}), ...a }));
        const merged = Array.from(map.values()).sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
        saveAppointments(merged);

        // Update active UI
        if (isOwnerAuthenticated()) {
          renderAdminDashboard();
        }
        const patientSession = getActivePatientSession();
        if (patientSession) {
          renderPatientDashboardData(patientSession.phone);
        }
      }
    }, (err) => {
      console.warn("Firestore snapshot notice:", err);
    });
  } catch (err) {
    console.warn("Realtime Firestore listener setup failed:", err);
  }
}

// ================= PATIENT AUTHENTICATION & SECURE PORTAL =================

function getStoredPatientUsers() {
  try {
    if (typeof localStorage === "undefined") return {};
    const raw = localStorage.getItem(STORAGE_KEYS.PATIENT_USERS);
    return raw ? JSON.parse(raw) : {};
  } catch (e) {
    return {};
  }
}

function savePatientUsers(users) {
  try {
    if (typeof localStorage === "undefined") return;
    localStorage.setItem(STORAGE_KEYS.PATIENT_USERS, JSON.stringify(users));
  } catch (e) {
    console.error("Error saving patient users", e);
  }
}

function getActivePatientSession() {
  try {
    if (typeof sessionStorage === "undefined") return null;
    const raw = sessionStorage.getItem(STORAGE_KEYS.PATIENT_SESSION);
    if (!raw) return null;
    const session = JSON.parse(raw);
    if (!session || !session.phone || !session.expiresAt) return null;
    if (Date.now() > session.expiresAt) {
      sessionStorage.removeItem(STORAGE_KEYS.PATIENT_SESSION);
      return null;
    }
    return session;
  } catch (e) {
    return null;
  }
}

function openPatientPortal() {
  const modal = document.getElementById("patientModal");
  if (modal) {
    modal.classList.add("active");
    document.body.style.overflow = "hidden";
    updatePatientPortalUI();
  }
}

function closePatientModal() {
  const modal = document.getElementById("patientModal");
  if (modal) {
    modal.classList.remove("active");
    document.body.style.overflow = "";
    if (window.location.hash.startsWith("#patient")) {
      history.pushState("", document.title, window.location.pathname + window.location.search);
    }
  }
}

function openPatientPortalFromConfirmation() {
  closeConfirmationModal();
  openPatientPortal();
}

function switchPatientAuthTab(tab) {
  const loginBtn = document.getElementById("tabPatientLoginBtn");
  const signupBtn = document.getElementById("tabPatientSignupBtn");
  const loginForm = document.getElementById("patientLoginForm");
  const signupForm = document.getElementById("patientSignupForm");
  const alertBox = document.getElementById("patientAuthAlertMsg");
  if (alertBox) alertBox.style.display = "none";

  if (tab === "signup") {
    if (loginBtn) loginBtn.classList.remove("active");
    if (signupBtn) signupBtn.classList.add("active");
    if (loginForm) loginForm.style.display = "none";
    if (signupForm) signupForm.style.display = "block";
  } else {
    if (loginBtn) loginBtn.classList.add("active");
    if (signupBtn) signupBtn.classList.remove("active");
    if (loginForm) loginForm.style.display = "block";
    if (signupForm) signupForm.style.display = "none";
  }
}

function updatePatientPortalUI() {
  const session = getActivePatientSession();
  const authView = document.getElementById("patientAuthView");
  const dashView = document.getElementById("patientDashboardView");

  if (!session) {
    if (authView) authView.style.display = "block";
    if (dashView) dashView.style.display = "none";
  } else {
    if (authView) authView.style.display = "none";
    if (dashView) dashView.style.display = "block";
    renderPatientDashboardData(session.phone);
  }
}

// 1. Patient Sign Up Handler
async function handlePatientSignupSubmit(event) {
  event.preventDefault();
  const nameInput = document.getElementById("signupPatientName");
  const phoneInput = document.getElementById("signupPatientPhone");
  const emailInput = document.getElementById("signupPatientEmail");
  const passInput = document.getElementById("signupPatientPassword");
  const confirmPassInput = document.getElementById("signupPatientConfirmPassword");
  const alertBox = document.getElementById("patientAuthAlertMsg");

  const fullName = (nameInput ? nameInput.value : "").trim();
  const rawPhone = (phoneInput ? phoneInput.value : "").replace(/\D/g, "");
  const phone = rawPhone.length >= 10 ? rawPhone.slice(-10) : rawPhone;
  const email = (emailInput ? emailInput.value : "").trim();
  const password = passInput ? passInput.value : "";
  const confirmPassword = confirmPassInput ? confirmPassInput.value : "";

  if (fullName.length < 2) {
    displayFeedback(alertBox, "Please enter your full name.", false);
    return;
  }
  if (phone.length !== 10) {
    displayFeedback(alertBox, "Please enter a valid 10-digit mobile number.", false);
    return;
  }
  if (password.length < 4) {
    displayFeedback(alertBox, "Password must be at least 4 characters long.", false);
    return;
  }
  if (password !== confirmPassword) {
    displayFeedback(alertBox, "Passwords do not match. Please re-enter.", false);
    return;
  }

  const users = getStoredPatientUsers();
  if (users[phone]) {
    displayFeedback(alertBox, "An account with this mobile number already exists. Please login.", false);
    switchPatientAuthTab("login");
    const loginPhone = document.getElementById("loginPatientPhone");
    if (loginPhone) loginPhone.value = phone;
    return;
  }

  // Create salted hash
  const salt = generateSecureSalt();
  const passwordHash = await hashCredential(password, salt);

  const newUser = {
    id: `USER-${phone}`,
    fullName,
    mobile: phone,
    email: email || "",
    role: "PATIENT",
    salt,
    passwordHash,
    createdAt: new Date().toISOString()
  };

  users[phone] = newUser;
  savePatientUsers(users);

  // Sync user profile to Firestore
  if (typeof window !== "undefined" && window.IS_FIREBASE_ENABLED && window.db) {
    try {
      window.db.collection("users").doc(newUser.id).set({
        id: newUser.id,
        fullName: newUser.fullName,
        mobile: newUser.mobile,
        email: newUser.email,
        role: "PATIENT",
        createdAt: newUser.createdAt
      });
    } catch (e) {
      console.warn("Firestore user sync error", e);
    }
  }

  // Establish patient session
  const session = {
    phone,
    fullName,
    email: email || "",
    token: generateSecureSalt(),
    expiresAt: Date.now() + 24 * 60 * 60 * 1000
  };
  sessionStorage.setItem(STORAGE_KEYS.PATIENT_SESSION, JSON.stringify(session));

  if (nameInput) nameInput.value = "";
  if (phoneInput) phoneInput.value = "";
  if (emailInput) emailInput.value = "";
  if (passInput) passInput.value = "";
  if (confirmPassInput) confirmPassInput.value = "";
  if (alertBox) alertBox.style.display = "none";

  updatePatientPortalUI();
}

// 2. Returning Patient Login Handler
async function handlePatientLoginSubmit(event) {
  event.preventDefault();
  const phoneInput = document.getElementById("loginPatientPhone");
  const passInput = document.getElementById("loginPatientPassword");
  const alertBox = document.getElementById("patientAuthAlertMsg");

  const rawPhone = (phoneInput ? phoneInput.value : "").replace(/\D/g, "");
  const phone = rawPhone.length >= 10 ? rawPhone.slice(-10) : rawPhone;
  const password = passInput ? passInput.value : "";

  if (phone.length !== 10) {
    displayFeedback(alertBox, "Please enter your valid 10-digit mobile number.", false);
    return;
  }
  if (!password) {
    displayFeedback(alertBox, "Please enter your password.", false);
    return;
  }

  const users = getStoredPatientUsers();
  const user = users[phone];

  if (!user) {
    displayFeedback(alertBox, "No patient account found for this mobile. Please click Sign Up to register.", false);
    return;
  }

  const testHash = await hashCredential(password, user.salt);
  if (testHash !== user.passwordHash) {
    displayFeedback(alertBox, "Incorrect password. Please verify and try again.", false);
    return;
  }

  // Password confirmed! Create active session
  const session = {
    phone,
    fullName: user.fullName || "Patient",
    email: user.email || "",
    token: generateSecureSalt(),
    expiresAt: Date.now() + 24 * 60 * 60 * 1000
  };
  sessionStorage.setItem(STORAGE_KEYS.PATIENT_SESSION, JSON.stringify(session));

  if (passInput) passInput.value = "";
  if (alertBox) alertBox.style.display = "none";

  // Directly open Patient Dashboard!
  updatePatientPortalUI();
}

// 3. Patient Logout Handler
function handlePatientLogout() {
  sessionStorage.removeItem(STORAGE_KEYS.PATIENT_SESSION);
  updatePatientPortalUI();
}

// 4. Patient Sub-Tabs Switching
function switchPatientDashSubTab(subTab) {
  const tabs = ["upcoming", "history", "profile"];
  tabs.forEach(t => {
    const btn = document.getElementById(`pill${t.charAt(0).toUpperCase() + t.slice(1)}Btn`);
    const content = document.getElementById(`subTab${t.charAt(0).toUpperCase() + t.slice(1)}Content`);
    if (btn) {
      if (t === subTab) btn.classList.add("active");
      else btn.classList.remove("active");
    }
    if (content) {
      content.style.display = (t === subTab) ? "block" : "none";
    }
  });

  const session = getActivePatientSession();
  if (session && subTab === "profile") {
    const nameInp = document.getElementById("editProfileName");
    const phoneInp = document.getElementById("editProfilePhone");
    const emailInp = document.getElementById("editProfileEmail");
    const users = getStoredPatientUsers();
    const user = users[session.phone] || {};
    if (nameInp) nameInp.value = session.fullName || user.fullName || "";
    if (phoneInp) phoneInp.value = `+91 ${session.phone}`;
    if (emailInp) emailInp.value = session.email || user.email || "";
  }
}

// 5. Open Booking Pre-filled from Patient Dashboard
function openBookingFromPatientDashboard() {
  const session = getActivePatientSession();
  closePatientModal();
  openBookingModal();

  if (session) {
    const nameInp = document.getElementById("patientName");
    const phoneInp = document.getElementById("patientPhone");
    if (nameInp && !nameInp.value) nameInp.value = session.fullName || "";
    if (phoneInp) phoneInp.value = session.phone || "";
  }
}

// 6. Patient Profile Update
function handlePatientProfileUpdate(event) {
  event.preventDefault();
  const session = getActivePatientSession();
  if (!session) return;

  const nameInp = document.getElementById("editProfileName");
  const emailInp = document.getElementById("editProfileEmail");
  const msgBox = document.getElementById("patientProfileFeedbackMsg");

  const newName = (nameInp ? nameInp.value : "").trim();
  const newEmail = (emailInp ? emailInp.value : "").trim();

  if (newName.length < 2) {
    alert("Please enter a valid full name.");
    return;
  }

  const users = getStoredPatientUsers();
  if (users[session.phone]) {
    users[session.phone].fullName = newName;
    users[session.phone].email = newEmail;
    savePatientUsers(users);
  }

  session.fullName = newName;
  session.email = newEmail;
  sessionStorage.setItem(STORAGE_KEYS.PATIENT_SESSION, JSON.stringify(session));

  // Sync to Firestore
  if (typeof window !== "undefined" && window.IS_FIREBASE_ENABLED && window.db) {
    try {
      window.db.collection("users").doc(`USER-${session.phone}`).update({
        fullName: newName,
        email: newEmail
      });
    } catch (e) {
      console.warn("Firestore profile update error", e);
    }
  }

  renderPatientDashboardData(session.phone);
  displayFeedback(msgBox, "✓ Profile updated successfully!", true);
}

// 7. Render Patient Dashboard Data (Upcoming & History)
function renderPatientDashboardData(phone) {
  const session = getActivePatientSession();
  const welcomeName = document.getElementById("patientWelcomeName");
  const phoneBadge = document.getElementById("patientPhoneDisplayBadge");
  const users = getStoredPatientUsers();
  const user = users[phone] || {};

  const displayName = session?.fullName || user.fullName || "Patient";
  if (welcomeName) welcomeName.textContent = `${displayName} 👋`;
  if (phoneBadge) phoneBadge.innerHTML = `<i class="fa-solid fa-phone"></i> +91 ${phone}`;

  const all = getStoredAppointments();
  // Filter strictly for this patient (Data Separation)
  const patientBookings = all.filter(a => {
    const rawA = (a.patientPhone || "").replace(/\D/g, "");
    const cleanA = rawA.length >= 10 ? rawA.slice(-10) : rawA;
    return cleanA === phone;
  });

  const countBadge = document.getElementById("patientHistoryCountBadge");
  if (countBadge) countBadge.textContent = `${patientBookings.length} Record${patientBookings.length === 1 ? "" : "s"}`;

  // UPCOMING APPOINTMENT LOGIC
  const upcomingContainer = document.getElementById("patientUpcomingContainer");
  if (upcomingContainer) {
    // Find earliest confirmed or pending appointment that is today or in future
    const todayStr = new Date().toISOString().split("T")[0];
    const upcomingList = patientBookings.filter(a => a.status === "CONFIRMED" || a.status === "PENDING" || a.status === "RESCHEDULED");

    if (upcomingList.length === 0) {
      upcomingContainer.innerHTML = `
        <div style="text-align:center; padding:28px 16px; background:#fff; border:1px dashed var(--border); border-radius:12px; margin-bottom:14px;">
          <i class="fa-solid fa-calendar-check" style="font-size:32px; color:#94a3b8; margin-bottom:8px; display:block;"></i>
          <h4 style="margin:0 0 4px; font-size:15px; color:#334155;">No Upcoming Appointments</h4>
          <p style="margin:0 0 14px; font-size:12px; color:#64748b;">Ready to consult with Dr. Balaji? Book your slot in seconds.</p>
          <button class="btn btn-primary btn-sm" onclick="openBookingFromPatientDashboard()" style="background:#0d9488; border-color:#0d9488; font-weight:700;">
            <i class="fa-solid fa-calendar-plus"></i> Book Consultation Now
          </button>
        </div>
      `;
    } else {
      const topUpcoming = upcomingList[0];
      const isConfirmed = topUpcoming.status === "CONFIRMED";
      const isRescheduled = topUpcoming.status === "RESCHEDULED";
      const isPending = topUpcoming.status === "PENDING";

      const badgeColor = isConfirmed ? "#16a34a" : (isRescheduled ? "#d97706" : "#b45309");
      const badgeBg = isConfirmed ? "#dcfce7" : (isRescheduled ? "#fef3c7" : "#fffbeb");
      const badgeBorder = isConfirmed ? "#86efac" : (isRescheduled ? "#fde68a" : "#fde68a");
      const statusIcon = isConfirmed ? "fa-solid fa-circle-check" : "fa-solid fa-clock";

      upcomingContainer.innerHTML = `
        <div class="patient-upcoming-card">
          <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:10px;">
            <div>
              <span class="patient-upcoming-badge" style="background:${badgeColor};">
                <i class="${statusIcon}"></i> ${topUpcoming.status}
              </span>
              <span style="font-size:11px; color:#64748b; margin-left:8px; font-family:monospace; font-weight:700;">${topUpcoming.id}</span>
            </div>
            <span style="font-size:12px; color:#0d9488; font-weight:700;">
              <i class="fa-solid fa-indian-rupee-sign"></i> ₹${CLINIC_DATA.fee}
            </span>
          </div>

          <div style="background:${badgeBg}; border:1px solid ${badgeBorder}; border-radius:10px; padding:12px; margin-bottom:12px;">
            <strong style="color:${badgeColor}; font-size:13px; display:block; margin-bottom:4px;">
              ${isConfirmed ? "✅ Appointment Confirmed by Doctor" : (isRescheduled ? "📅 Appointment Rescheduled to New Time" : "⏳ Pending Doctor Confirmation")}
            </strong>
            <p style="margin:0; font-size:12px; color:#334155;">
              ${isConfirmed ? `Dr. ${CLINIC_DATA.doctorName} has approved your booking. Please visit or join on time.` : (isRescheduled ? `Dr. ${CLINIC_DATA.doctorName} has updated your appointment slot.` : `Request received. Dr. ${CLINIC_DATA.doctorName} will review and confirm shortly.`)}
            </p>
          </div>

          <div class="patient-history-details">
            <div><i class="fa-solid fa-user-doctor"></i> <span><strong>Doctor:</strong> ${CLINIC_DATA.doctorName}</span></div>
            <div><i class="fa-regular fa-calendar"></i> <span><strong>Date:</strong> ${topUpcoming.date}</span></div>
            <div><i class="fa-regular fa-clock"></i> <span><strong>Time:</strong> ${topUpcoming.timeSlot}</span></div>
            <div><i class="fa-solid fa-stethoscope"></i> <span><strong>Type:</strong> ${topUpcoming.consultationType === "IN_CLINIC" ? `In-Clinic (${CLINIC_DATA.clinicName})` : "Online Video Consultation"}</span></div>
          </div>

          <div style="display:flex; gap:8px; margin-top:12px; border-top:1px dashed #bbf7d0; padding-top:10px;">
            <button class="btn btn-outline btn-sm" style="flex:1; font-size:12px;" onclick="viewBookingTicket('${topUpcoming.id}')">
              <i class="fa-solid fa-ticket"></i> View Ticket / QR
            </button>
            <a href="https://wa.me/91${(CLINIC_DATA.cleanPhone || CLINIC_DATA.phone.replace(/\\D/g, '')).slice(-10)}?text=Hello%20Dr.%20${encodeURIComponent(CLINIC_DATA.doctorName)},%20I%20am%20inquiring%20about%20my%20appointment%20${topUpcoming.id}" target="_blank" class="btn btn-whatsapp btn-sm" style="flex:1; font-size:12px; display:inline-flex; align-items:center; justify-content:center; gap:6px;">
              <i class="fa-brands fa-whatsapp"></i> Chat Clinic
            </a>
          </div>
        </div>
      `;
    }
  }

  // COMPLETE APPOINTMENT HISTORY LIST
  const historyListContainer = document.getElementById("patientAppointmentsList");
  if (historyListContainer) {
    if (patientBookings.length === 0) {
      historyListContainer.innerHTML = `
        <div style="text-align:center; padding:30px; color:#64748b; font-size:13px; background:#fff; border-radius:10px; border:1px solid var(--border);">
          No consultation history found for this account yet.
        </div>
      `;
    } else {
      historyListContainer.innerHTML = patientBookings.map(a => {
        const isConfirmed = a.status === "CONFIRMED";
        const isPending = a.status === "PENDING";
        const isRejected = a.status === "REJECTED";
        const isCompleted = a.status === "COMPLETED";
        const isRescheduled = a.status === "RESCHEDULED";

        const badgeClass = isConfirmed ? "patient-status-confirmed" : (isPending ? "patient-status-pending" : (isRejected ? "patient-status-cancelled" : (isCompleted ? "patient-status-completed" : "patient-status-pending")));
        const statusIcon = isConfirmed ? "fa-solid fa-circle-check" : (isPending ? "fa-solid fa-clock" : (isRejected ? "fa-solid fa-ban" : (isCompleted ? "fa-solid fa-check-double" : "fa-solid fa-calendar-alt")));

        const doctorStatusBanner = isConfirmed 
          ? `✅ Appointment Confirmed by Doctor` 
          : (isPending 
            ? `⏳ Pending Confirmation: Under Review` 
            : (isRejected 
              ? `❌ Appointment Rejected: Please choose another slot` 
              : (isRescheduled 
                ? `📅 Rescheduled to New Time by Doctor` 
                : `Status: ${a.status}`)));

        const bannerBg = isConfirmed ? "#f0fdf4" : (isPending ? "#fffbeb" : (isRejected ? "#fef2f2" : "#f0f9ff"));
        const bannerBorder = isConfirmed ? "#bbf7d0" : (isPending ? "#fde68a" : (isRejected ? "#fecaca" : "#bae6fd"));
        const bannerColor = isConfirmed ? "#15803d" : (isPending ? "#b45309" : (isRejected ? "#b91c1c" : "#0369a1"));

        return `
          <div class="patient-history-card">
            <div class="patient-history-header">
              <div>
                <span class="patient-history-id">${a.id}</span>
                <span style="font-size:11px; color:#64748b; margin-left:8px;">${new Date(a.createdAt || Date.now()).toLocaleDateString()}</span>
              </div>
              <span class="patient-status-badge ${badgeClass}">
                <i class="${statusIcon}"></i> ${a.status}
              </span>
            </div>

            <div style="background:${bannerBg}; border:1px solid ${bannerBorder}; border-radius:8px; padding:8px 12px; margin-bottom:10px; font-size:12px; color:${bannerColor}; font-weight:700;">
              ${doctorStatusBanner}
            </div>

            <div class="patient-history-details">
              <div><i class="fa-regular fa-calendar"></i> <span><strong>Date:</strong> ${a.date}</span></div>
              <div><i class="fa-regular fa-clock"></i> <span><strong>Time:</strong> ${a.timeSlot}</span></div>
              <div><i class="fa-solid fa-user-doctor"></i> <span><strong>Doctor:</strong> ${CLINIC_DATA.doctorName}</span></div>
              <div><i class="fa-solid fa-stethoscope"></i> <span><strong>Type:</strong> ${a.consultationType === "IN_CLINIC" ? "Offline Consultation" : "Online Consultation"}</span></div>
              <div><i class="fa-solid fa-notes-medical"></i> <span><strong>Reason:</strong> ${a.symptoms || "General"}</span></div>
              <div><i class="fa-solid fa-indian-rupee-sign"></i> <span><strong>Fee:</strong> ₹${CLINIC_DATA.fee}</span></div>
            </div>

            <div style="display:flex; gap:8px; margin-top:8px; border-top:1px dashed #f1f5f9; padding-top:8px;">
              <button class="btn btn-outline btn-sm" style="flex:1; font-size:12px;" onclick="viewBookingTicket('${a.id}')">
                <i class="fa-solid fa-ticket"></i> View Ticket
              </button>
              <a href="https://wa.me/91${(CLINIC_DATA.cleanPhone || CLINIC_DATA.phone.replace(/\\D/g, '')).slice(-10)}?text=Hello%20Dr.%20${encodeURIComponent(CLINIC_DATA.doctorName)},%20inquiring%20about%20my%20token%20${a.id}" target="_blank" class="btn btn-whatsapp btn-sm" style="flex:1; font-size:12px; display:inline-flex; align-items:center; justify-content:center; gap:6px;">
                <i class="fa-brands fa-whatsapp"></i> Chat Clinic
              </a>
            </div>
          </div>
        `;
      }).join("");
    }
  }
}

// 8. View ticket from patient list
function viewBookingTicket(id) {
  const all = getStoredAppointments();
  const found = all.find(a => a.id === id);
  if (!found) return;
  closePatientModal();
  currentBooking = found;
  showConfirmation(found);
}

// ================= ADMIN PATIENTS DIRECTORY CONTROLLER =================

function renderAdminPatients() {
  requireOwnerAuth();
  const container = document.getElementById("adminPatientsListContainer");
  if (!container) return;

  const search = (document.getElementById("adminPatientSearchInput")?.value || "").toLowerCase().trim();
  const allAppointments = getStoredAppointments();
  const storedUsers = getStoredPatientUsers();

  // Aggregate patients by phone number
  const patientMap = new Map();

  // From registered accounts
  Object.values(storedUsers).forEach(u => {
    patientMap.set(u.mobile, {
      id: u.id || `USER-${u.mobile}`,
      fullName: u.fullName,
      mobile: u.mobile,
      email: u.email || "",
      appointments: []
    });
  });

  // From appointments
  allAppointments.forEach(a => {
    const rawP = (a.patientPhone || "").replace(/\D/g, "");
    const cleanP = rawP.length >= 10 ? rawP.slice(-10) : rawP;
    if (!cleanP) return;

    if (!patientMap.has(cleanP)) {
      patientMap.set(cleanP, {
        id: `USER-${cleanP}`,
        fullName: a.patientName || "Patient",
        mobile: cleanP,
        email: "",
        appointments: []
      });
    }
    const patientObj = patientMap.get(cleanP);
    if (!patientObj.fullName && a.patientName) patientObj.fullName = a.patientName;
    patientObj.appointments.push(a);
  });

  const patientList = Array.from(patientMap.values());
  const filtered = patientList.filter(p => {
    const matchesSearch = p.fullName.toLowerCase().includes(search) ||
      p.mobile.includes(search) ||
      p.email.toLowerCase().includes(search) ||
      p.appointments.some(a => a.id.toLowerCase().includes(search));
    return matchesSearch;
  });

  if (filtered.length === 0) {
    container.innerHTML = `<div style="text-align:center; padding:30px; color:#64748b;">No matching patients found.</div>`;
    return;
  }

  container.innerHTML = filtered.map(p => {
    const sortedAppts = p.appointments.sort((x, y) => new Date(y.createdAt || 0) - new Date(x.createdAt || 0));
    const upcoming = sortedAppts.filter(a => a.status === "CONFIRMED" || a.status === "PENDING" || a.status === "RESCHEDULED");
    const completed = sortedAppts.filter(a => a.status === "COMPLETED");
    const rejected = sortedAppts.filter(a => a.status === "REJECTED" || a.status === "CANCELLED");

    return `
      <div class="patient-history-card" style="border-left:4px solid #0d9488;">
        <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:10px; flex-wrap:wrap; gap:8px;">
          <div>
            <h4 style="margin:0; font-size:16px; color:#0f172a;">${p.fullName}</h4>
            <span style="font-size:13px; color:#0d9488; font-weight:700;"><i class="fa-solid fa-phone"></i> +91 ${p.mobile}</span>
            ${p.email ? `<span style="font-size:12px; color:#64748b; margin-left:10px;"><i class="fa-solid fa-envelope"></i> ${p.email}</span>` : ""}
          </div>
          <div style="display:flex; gap:6px;">
            <span class="badge-pro" style="background:#0284c7; font-size:11px;">Total: ${sortedAppts.length} Bookings</span>
            <span class="badge-success" style="font-size:11px;">${upcoming.length} Active</span>
          </div>
        </div>

        <!-- Appointment History Breakdown -->
        <div style="margin-top:10px;">
          <strong style="font-size:12px; color:#334155; text-transform:uppercase; letter-spacing:0.5px; display:block; margin-bottom:6px;">
            <i class="fa-solid fa-notes-medical"></i> Complete Consultation History:
          </strong>
          ${sortedAppts.length === 0 ? `<p style="font-size:12px; color:#94a3b8; margin:0;">No bookings submitted yet.</p>` : `
            <div style="display:flex; flex-direction:column; gap:6px;">
              ${sortedAppts.map(a => `
                <div style="display:flex; justify-content:space-between; align-items:center; background:#f8fafc; border:1px solid #e2e8f0; border-radius:6px; padding:6px 10px; font-size:12px;">
                  <div>
                    <strong style="color:#0f172a;">${a.id}</strong> &bull; 
                    <span>${a.date} at ${a.timeSlot}</span> &bull; 
                    <span style="color:#64748b;">${a.consultationType === "IN_CLINIC" ? "In-Clinic" : "Online Video"}</span> &bull;
                    <span style="color:#0f766e;">${a.symptoms || "General"}</span>
                  </div>
                  <div>
                    <span class="badge-${a.status === "CONFIRMED" ? "success" : (a.status === "PENDING" ? "warning" : "secondary")}" style="font-size:10px; padding:2px 6px; border-radius:4px; font-weight:700;">
                      ${a.status}
                    </span>
                  </div>
                </div>
              `).join("")}
            </div>
          `}
        </div>

        <div style="display:flex; gap:8px; margin-top:12px; border-top:1px dashed var(--border); padding-top:8px;">
          <a href="https://wa.me/91${p.mobile}?text=Hello%20${encodeURIComponent(p.fullName)},%20this%20is%20Dr.%20${encodeURIComponent(CLINIC_DATA.doctorName)}%20from%20${encodeURIComponent(CLINIC_DATA.clinicName)}." target="_blank" class="btn btn-whatsapp btn-sm" style="font-size:12px; display:inline-flex; align-items:center; gap:6px;">
            <i class="fa-brands fa-whatsapp"></i> Chat Patient
          </a>
          <a href="tel:+91${p.mobile}" class="btn btn-outline btn-sm" style="font-size:12px; color:#0d9488; border-color:#0d9488;">
            <i class="fa-solid fa-phone"></i> Call Patient
          </a>
        </div>
      </div>
    `;
  }).join("");
}

// ================= ADMIN RESCHEDULE MODAL CONTROLLER =================

function openRescheduleModal(apptId) {
  requireOwnerAuth();
  const all = getStoredAppointments();
  const appt = all.find(a => a.id === apptId);
  if (!appt) return;

  const modal = document.getElementById("rescheduleModal");
  const subEl = document.getElementById("rescheduleApptIdSubtitle");
  const idInput = document.getElementById("rescheduleTargetId");
  const nameInput = document.getElementById("reschedulePatientName");
  const dateInput = document.getElementById("rescheduleDate");
  const slotSelect = document.getElementById("rescheduleTimeSlot");

  if (subEl) subEl.textContent = `Token: ${appt.id}`;
  if (idInput) idInput.value = appt.id;
  if (nameInput) nameInput.value = appt.patientName;
  if (dateInput) {
    const today = new Date().toISOString().split("T")[0];
    dateInput.min = today;
    dateInput.value = appt.date || today;
  }

  if (slotSelect) {
    slotSelect.innerHTML = "";
    const slots = CLINIC_DATA.slots || DEFAULT_CLINIC_DATA.slots;
    slots.forEach(s => {
      const opt = document.createElement("option");
      opt.value = s;
      opt.textContent = s;
      if (s === appt.timeSlot) opt.selected = true;
      slotSelect.appendChild(opt);
    });
  }

  if (modal) {
    modal.classList.add("active");
  }
}

function closeRescheduleModal() {
  const modal = document.getElementById("rescheduleModal");
  if (modal) modal.classList.remove("active");
}

function handleRescheduleSubmit(event) {
  event.preventDefault();
  requireOwnerAuth();

  const idInput = document.getElementById("rescheduleTargetId");
  const dateInput = document.getElementById("rescheduleDate");
  const slotSelect = document.getElementById("rescheduleTimeSlot");

  const apptId = idInput?.value;
  const newDate = dateInput?.value;
  const newSlot = slotSelect?.value;

  if (!apptId || !newDate || !newSlot) return;

  const appointments = getStoredAppointments();
  const target = appointments.find(a => a.id === apptId);
  if (!target) return;

  target.date = newDate;
  target.timeSlot = newSlot;
  target.status = "RESCHEDULED";
  target.updatedAt = new Date().toISOString();

  saveAppointments(appointments);

  // Sync to Firestore
  if (typeof window !== "undefined" && window.IS_FIREBASE_ENABLED && window.db) {
    try {
      window.db.collection("appointments").doc(apptId).update({
        date: newDate,
        timeSlot: newSlot,
        status: "RESCHEDULED",
        updatedAt: target.updatedAt
      });
    } catch (e) {
      console.warn("Firestore update error", e);
    }
  }

  closeRescheduleModal();
  renderAdminDashboard();

  // Send WhatsApp update notification to patient
  const cleanPhone = (CLINIC_DATA.cleanPhone || CLINIC_DATA.phone.replace(/\D/g, "")).slice(-10);
  const msg = `*${encodeURIComponent(CLINIC_DATA.clinicName)} - Appointment Rescheduled*%0A%0A` +
    `Dear *${encodeURIComponent(target.patientName)}*,%0A` +
    `Your appointment with *${encodeURIComponent(CLINIC_DATA.doctorName)}* has been rescheduled to:%0A%0A` +
    `*New Date:* ${encodeURIComponent(newDate)}%0A` +
    `*New Time:* ${encodeURIComponent(newSlot)}%0A` +
    `*Token ID:* ${encodeURIComponent(target.id)}%0A%0A` +
    `_Please contact ${encodeURIComponent(CLINIC_DATA.phone)} if you require further adjustments._`;
  window.open(`https://wa.me/91${target.patientPhone}?text=${msg}`, "_blank");
}

