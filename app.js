/**
 * SP CLINIC / HOMEO AI CLASSICAL CLINIC
 * SECURE OWNER DASHBOARD & CLIENT APPOINTMENT BOOKING PORTAL
 */

// Master Clinic Default Data
const DEFAULT_CLINIC_DATA = {
  clinicName: "SP Clinic",
  clinicSubtitle: "HOMEo AI Classical Clinic • Your health, our care.",
  doctorName: "Dr. Balaji",
  doctorQualification: "BHMS, MD (Homeopathy)",
  doctorSpeciality: "Classical Constitutional Homeopathy",
  doctorBio: "Dr. Balaji specializes in individualized Classical Constitutional Homeopathy, combining deep repertorisation with gentle holistic remedies to treat chronic and acute ailments safely with zero side effects.",
  phone: "+91 98765 43210",
  cleanPhone: "919876543210",
  address: "SP Clinic, 74 Gandhi Road, Near Central Park, Health Complex, Chennai - 600001",
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
  OWNER_AUTH: "sp_clinic_owner_auth",       // Salted cryptographic hash of Owner credentials
  OWNER_SESSION: "sp_clinic_owner_session", // Active owner session token
  CLINIC_DATA: "sp_clinic_master_data"      // Master clinic settings and profile
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
      const initialSeed = [
        {
          id: "HM-2026-1001",
          patientName: "Karthik Subramanian",
          patientPhone: "9876543210",
          patientAge: "34",
          patientGender: "Male",
          consultationType: "IN_CLINIC",
          date: new Date().toISOString().split("T")[0],
          timeSlot: "10:30 AM - 11:00 AM",
          symptoms: "Allergic rhinitis and morning sneezing",
          status: "CONFIRMED",
          createdAt: new Date().toISOString()
        }
      ];
      localStorage.setItem(STORAGE_KEYS.APPOINTMENTS, JSON.stringify(initialSeed));
      return initialSeed;
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
    checkAdminRoute();

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
  if (hash === "#admin" || search.get("admin") === "true") {
    openOwnerPortal();
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
    .filter(a => a.date === selectedDate && a.status !== "CANCELLED")
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

  const newAppointment = {
    id: appointmentId,
    patientName,
    patientPhone,
    patientAge,
    patientGender,
    consultationType,
    date,
    timeSlot,
    symptoms: symptoms || "General Homeopathy Consultation",
    status: "PENDING",
    createdAt: new Date().toISOString()
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
    if (modalTitle) modalTitle.innerHTML = `<i class="fa-solid fa-lock"></i> Owner Login — Enter Password`;
    if (modalSubtitle) modalSubtitle.textContent = `${CLINIC_DATA.clinicName} Protected Management`;
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

  const tabs = ["appts", "slots", "profile", "clinic", "security"];
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
        ${a.status === "PENDING" ? `
          <button class="btn btn-primary btn-sm" style="background:#059669; border-color:#059669; font-weight:700;" onclick="confirmAndNotifyPatient('${a.id}')">
            <i class="fa-solid fa-check"></i> Approve &amp; WhatsApp
          </button>
        ` : ""}
        ${a.status === "CONFIRMED" ? `
          <button class="btn btn-outline btn-sm" onclick="confirmAndNotifyPatient('${a.id}')" title="Resend WhatsApp Confirmation">
            <i class="fa-brands fa-whatsapp"></i> WhatsApp Status
          </button>
        ` : ""}
        <select onchange="changeAppointmentStatusFromSelect('${a.id}', this.value)" class="form-control form-control-sm" style="width:auto; display:inline-block; font-size:12px;">
          <option value="PENDING" ${a.status === "PENDING" ? "selected" : ""}>Pending</option>
          <option value="CONFIRMED" ${a.status === "CONFIRMED" ? "selected" : ""}>Confirmed</option>
          <option value="COMPLETED" ${a.status === "COMPLETED" ? "selected" : ""}>Completed</option>
          <option value="CANCELLED" ${a.status === "CANCELLED" ? "selected" : ""}>Cancelled</option>
        </select>
        ${a.status !== "CANCELLED" ? `
          <button class="btn btn-secondary btn-sm" style="color:#dc2626;" onclick="rejectAppointment('${a.id}')">
            <i class="fa-solid fa-ban"></i> Reject
          </button>
        ` : ""}
        <button class="btn btn-outline btn-sm" style="color:#94a3b8;" onclick="deleteAppointmentRecord('${a.id}')" title="Delete Booking Record">
          <i class="fa-solid fa-trash"></i>
        </button>
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
  if (confirm("Are you sure you want to reject/cancel this appointment?")) {
    updateAppointmentStatus(id, "CANCELLED");
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
