/**
 * HOMEO CLINIC PRO - CLIENT & ADMIN WEB APPLICATION LOGIC
 */

// Clinic Configuration
const CLINIC_INFO = {
  name: "HOMEo AI Classical Clinic",
  doctor: "Dr. Balaji",
  qualification: "BHMS, MD (Homeopathy)",
  phone: "+919876543210",
  cleanPhone: "919876543210",
  fee: 500
};

// Storage Keys
const STORAGE_KEYS = {
  APPOINTMENTS: "homeo_clinic_appointments",
  ADMIN_PIN: "homeo_clinic_admin_pin",
  ADMIN_SESSION: "homeo_clinic_admin_logged_in",
  PROFILE: "homeo_clinic_profile"
};

// Default Sample Seed Data if first time opening
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

// Current latest booked appointment
let currentBooking = null;

// ================= INITIALIZATION =================
if (typeof document !== "undefined") {
  document.addEventListener("DOMContentLoaded", () => {
    loadClinicProfile();
    initDateLimits();
    checkAdminSession();

    // Close modals on clicking overlay backdrop
    document.addEventListener("click", (e) => {
      if (e.target.classList && e.target.classList.contains("modal-overlay")) {
        e.target.classList.remove("active");
        document.body.style.overflow = "";
      }
    });

    // Close modals on pressing Escape
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        document.querySelectorAll(".modal-overlay.active").forEach(m => m.classList.remove("active"));
        document.body.style.overflow = "";
      }
    });
  });
}

// Setup Date constraints (Today up to 30 days ahead)
function initDateLimits() {
  const dateInput = document.getElementById("bookDate");
  if (!dateInput) return;

  const today = new Date();
  const yyyy = today.getFullYear();
  const mm = String(today.getMonth() + 1).padStart(2, "0");
  const dd = String(today.getDate()).padStart(2, "0");
  const minDate = `${yyyy}-${mm}-${dd}`;

  const max = new Date();
  max.setDate(today.getDate() + 30);
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

// ================= BOOKING MODAL =================
function openBookingModal() {
  const modal = document.getElementById("bookingModal");
  if (modal) {
    modal.classList.add("active");
    document.body.style.overflow = "hidden";
  }
}

function closeBookingModal() {
  const modal = document.getElementById("bookingModal");
  if (modal) {
    modal.classList.remove("active");
    document.body.style.overflow = "";
  }
}

// Slot Management
function loadAvailableSlots() {
  // Dynamically update available slots if needed
  const selectedDate = document.getElementById("bookDate").value;
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

// ================= BOOKING SUBMISSION =================
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

  const rawPhone = phoneInput.value.replace(/\D/g, "");
  const patientPhone = rawPhone.length >= 10 ? rawPhone.slice(-10) : rawPhone;
  const patientAge = ageInput.value.trim() || "30";
  const patientGender = genderInput.value;
  const date = dateInput.value;
  const timeSlot = timeInput.value;
  const symptoms = symptomsInput.value.trim();
  const consultationType = consultTypeInput ? consultTypeInput.value : "IN_CLINIC";

  // Validate
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
  if (!timeSlot) {
    alert("Please select a convenient time slot.");
    timeInput.focus();
    return;
  }

  // Generate Unique ID
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
    symptoms: symptoms || "Homeopathy Consultation",
    status: "PENDING",
    createdAt: new Date().toISOString()
  };

  // Save to persistent storage
  const appointments = getStoredAppointments();
  appointments.unshift(newAppointment);
  saveAppointments(appointments);

  // Sync to Firebase if configured
  if (window.IS_FIREBASE_ENABLED && window.db) {
    try {
      window.db.collection("appointments").doc(appointmentId).set(newAppointment);
    } catch (e) {
      console.warn("Firebase sync error", e);
    }
  }

  currentBooking = newAppointment;

  // Reset form
  document.getElementById("bookingForm").reset();
  initDateLimits();
  closeBookingModal();

  // Show Confirmation Modal
  showConfirmation(newAppointment);
}

const APP_CLOUD_URL = "https://ais-pre-krh3ojasrp76qf4324mwph-653917990697.asia-southeast1.run.app";
const ANDROID_PACKAGE = "com.aistudio.homeoai.clnxrt";

function openAndroidAppDirectly() {
  const isAndroid = /Android/i.test(navigator.userAgent);
  if (isAndroid) {
    const intentUri = `intent:#Intent;package=${ANDROID_PACKAGE};S.browser_fallback_url=${encodeURIComponent(APP_CLOUD_URL)};end`;
    window.location.href = intentUri;
    setTimeout(() => {
      if (document.hidden || document.webkitHidden) return;
      window.open(APP_CLOUD_URL, "_blank");
    }, 1500);
  } else {
    window.open(APP_CLOUD_URL, "_blank");
  }
}

function openBookingInAndroidApp() {
  if (!currentBooking) {
    openAndroidAppDirectly();
    return;
  }
  const query = `id=${encodeURIComponent(currentBooking.id)}&name=${encodeURIComponent(currentBooking.patientName)}&phone=${encodeURIComponent(currentBooking.patientPhone)}&age=${encodeURIComponent(currentBooking.patientAge || 30)}&gender=${encodeURIComponent(currentBooking.patientGender || "Male")}&date=${encodeURIComponent(currentBooking.date)}&time=${encodeURIComponent(currentBooking.timeSlot)}&mode=${encodeURIComponent(currentBooking.consultationType)}&symptoms=${encodeURIComponent(currentBooking.symptoms || "")}`;
  
  const isAndroid = /Android/i.test(navigator.userAgent);
  if (isAndroid) {
    const intentUri = `intent://appointment?${query}#Intent;scheme=homeoclinic;package=${ANDROID_PACKAGE};S.browser_fallback_url=${encodeURIComponent(APP_CLOUD_URL)};end`;
    window.location.href = intentUri;
    setTimeout(() => {
      if (document.hidden || document.webkitHidden) return;
      window.open(APP_CLOUD_URL, "_blank");
    }, 1500);
  } else {
    window.open(APP_CLOUD_URL, "_blank");
  }
}

// ================= CONFIRMATION MODAL =================
function showConfirmation(appt) {
  document.getElementById("confirmApptId").textContent = appt.id;
  const smallId = document.getElementById("confirmApptIdSmall");
  if (smallId) smallId.textContent = appt.id;
  document.getElementById("confirmPatientName").textContent = appt.patientName;
  document.getElementById("confirmPatientPhone").textContent = appt.patientPhone;
  document.getElementById("confirmDateTime").textContent = `${appt.date} at ${appt.timeSlot}`;
  document.getElementById("confirmMode").textContent =
    appt.consultationType === "IN_CLINIC" ? "In-Clinic Visit (Chennai)" : "Online Video Consultation";

  // Dynamic Deep Link for QR Code & direct App Import
  const query = `id=${encodeURIComponent(appt.id)}&name=${encodeURIComponent(appt.patientName)}&phone=${encodeURIComponent(appt.patientPhone)}&age=${encodeURIComponent(appt.patientAge || 30)}&gender=${encodeURIComponent(appt.patientGender || "Male")}&date=${encodeURIComponent(appt.date)}&time=${encodeURIComponent(appt.timeSlot)}&mode=${encodeURIComponent(appt.consultationType)}&symptoms=${encodeURIComponent(appt.symptoms || "")}`;
  const deepLink = `homeoclinic://appointment?${query}`;

  const qrImg = document.getElementById("confirmApptQr");
  if (qrImg) {
    qrImg.src = `https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=${encodeURIComponent(deepLink)}`;
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
  const query = `id=${encodeURIComponent(currentBooking.id)}&name=${encodeURIComponent(currentBooking.patientName)}&phone=${encodeURIComponent(currentBooking.patientPhone)}&age=${encodeURIComponent(currentBooking.patientAge || 30)}&gender=${encodeURIComponent(currentBooking.patientGender || "Male")}&date=${encodeURIComponent(currentBooking.date)}&time=${encodeURIComponent(currentBooking.timeSlot)}&mode=${encodeURIComponent(currentBooking.consultationType)}&symptoms=${encodeURIComponent(currentBooking.symptoms || "")}`;
  const deepLink = `homeoclinic://appointment?${query}`;

  const msg = `*HOMEo AI Classical Clinic - Appointment Ticket*%0A%0A` +
    `*Doctor Name:* Dr. Balaji, BHMS, MD (Homeopathy)%0A` +
    `*Patient Name:* ${encodeURIComponent(currentBooking.patientName)}%0A` +
    `*Token ID:* ${encodeURIComponent(currentBooking.id)}%0A` +
    `*Date:* ${encodeURIComponent(currentBooking.date)}%0A` +
    `*Slot:* ${encodeURIComponent(currentBooking.timeSlot)}%0A` +
    `*Mode:* ${currentBooking.consultationType === "IN_CLINIC" ? "In-Clinic Visit (Chennai)" : "Online Video Consultation"}%0A` +
    `*Fee:* ₹500%0A%0A` +
    `*📲 Open in HOMEo AI App:*%0A${encodeURIComponent(deepLink)}%0A%0A` +
    `_Please confirm my consultation. Thank you!_`;

  window.open(`https://wa.me/${CLINIC_INFO.cleanPhone}?text=${msg}`, "_blank");
}

// ================= OWNER / ADMIN PORTAL =================
function openAdminModal() {
  const modal = document.getElementById("adminModal");
  if (modal) {
    modal.classList.add("active");
    document.body.style.overflow = "hidden";
    checkAdminSession();
  }
}

function closeAdminModal() {
  const modal = document.getElementById("adminModal");
  if (modal) {
    modal.classList.remove("active");
    document.body.style.overflow = "";
  }
}

function checkAdminSession() {
  const isLoggedIn = sessionStorage.getItem(STORAGE_KEYS.ADMIN_SESSION) === "true";
  const loginForm = document.getElementById("adminLoginForm");
  const dashView = document.getElementById("adminDashboardView");

  if (isLoggedIn) {
    if (loginForm) loginForm.style.display = "none";
    if (dashView) {
      dashView.style.display = "block";
      renderAdminDashboard();
    }
  } else {
    if (loginForm) loginForm.style.display = "block";
    if (dashView) dashView.style.display = "none";
  }
}

function handleAdminLogin() {
  const emailInput = document.getElementById("adminEmail").value.trim().toLowerCase();
  const passwordInput = document.getElementById("adminPassword").value.trim();

  const savedPin = localStorage.getItem(STORAGE_KEYS.ADMIN_PIN) || "admin123";

  // Check credentials
  if (
    (emailInput === "" || emailInput.includes("yugabharathi") || emailInput.includes("admin")) &&
    (passwordInput === savedPin || passwordInput === "admin123")
  ) {
    sessionStorage.setItem(STORAGE_KEYS.ADMIN_SESSION, "true");
    document.getElementById("adminPassword").value = "";
    checkAdminSession();
  } else {
    alert("Invalid owner password. Default PIN is 'admin123'.");
  }
}

function handleAdminLogout() {
  sessionStorage.removeItem(STORAGE_KEYS.ADMIN_SESSION);
  checkAdminSession();
}

function renderAdminDashboard() {
  const appointments = getStoredAppointments();

  // Top stats
  const total = appointments.length;
  const pending = appointments.filter(a => a.status === "PENDING").length;
  const confirmed = appointments.filter(a => a.status === "CONFIRMED").length;

  document.getElementById("statTotalAppointments").textContent = total;
  document.getElementById("statPendingAppointments").textContent = pending;
  document.getElementById("statConfirmedAppointments").textContent = confirmed;

  filterAdminAppointments();
}

function filterAdminAppointments() {
  const query = (document.getElementById("adminSearchInput")?.value || "").toLowerCase().trim();
  const statusFilter = document.getElementById("adminStatusFilter")?.value || "ALL";

  const appointments = getStoredAppointments();
  const filtered = appointments.filter(a => {
    const matchesQuery = a.patientName.toLowerCase().includes(query) || a.patientPhone.includes(query) || a.id.toLowerCase().includes(query);
    const matchesStatus = statusFilter === "ALL" || a.status === statusFilter;
    return matchesQuery && matchesStatus;
  });

  const listContainer = document.getElementById("adminAppointmentsList");
  if (!listContainer) return;

  if (filtered.length === 0) {
    listContainer.innerHTML = `<div style="text-align:center; padding:20px; color:#64748b; font-size:14px;">No appointments found.</div>`;
    return;
  }

  listContainer.innerHTML = filtered.map(a => `
    <div class="admin-appt-card status-${a.status.toLowerCase()}">
      <div class="admin-appt-card-top">
        <div>
          <span class="admin-appt-id">${a.id}</span>
          <h4 style="font-size:15px; margin:2px 0;">${a.patientName} (${a.patientAge}y, ${a.patientGender})</h4>
          <span style="font-size:13px; color:#0d9488; font-weight:600;"><i class="fa-solid fa-phone"></i> ${a.patientPhone}</span>
        </div>
        <span class="badge-${a.status === "CONFIRMED" ? "success" : a.status === "PENDING" ? "warning" : "secondary"}" style="font-size:11px; padding:3px 8px; border-radius:4px; font-weight:700; background:#f1f5f9;">
          ${a.status}
        </span>
      </div>
      <div style="font-size:13px; color:#475569; margin:4px 0;">
        <i class="fa-regular fa-calendar"></i> ${a.date} &bull; <i class="fa-regular fa-clock"></i> ${a.timeSlot} &bull; 
        <strong>${a.consultationType === "IN_CLINIC" ? "In-Clinic" : "Online Video"}</strong>
      </div>
      <div style="font-size:12px; color:#64748b; background:#f8fafc; padding:6px 10px; border-radius:6px; margin-top:6px;">
        <strong>Symptoms:</strong> ${a.symptoms || "General Checkup"}
      </div>
      <div class="admin-appt-actions">
        ${a.status === "PENDING" ? `
          <button class="btn btn-primary btn-sm" style="background:#059669; border-color:#059669; font-weight:700;" onclick="confirmAndNotifyPatient('${a.id}')">
            <i class="fa-solid fa-check-double"></i> ✓ Confirm & WhatsApp Patient
          </button>
        ` : ""}
        ${a.status === "CONFIRMED" ? `
          <button class="btn btn-outline btn-sm" onclick="confirmAndNotifyPatient('${a.id}')" title="Resend WhatsApp Confirmation">
            <i class="fa-brands fa-whatsapp"></i> Resend WhatsApp
          </button>
        ` : ""}
        ${a.status !== "COMPLETED" && a.status === "CONFIRMED" ? `
          <button class="btn btn-outline btn-sm" onclick="updateAppointmentStatus('${a.id}', 'COMPLETED')">
            <i class="fa-solid fa-circle-check"></i> Mark Consulted
          </button>
        ` : ""}
        ${a.status !== "CANCELLED" ? `
          <button class="btn btn-secondary btn-sm" style="color:#dc2626;" onclick="updateAppointmentStatus('${a.id}', 'CANCELLED')">
            <i class="fa-solid fa-ban"></i> Reject / Cancel
          </button>
        ` : ""}
        <a href="https://wa.me/91${a.patientPhone}?text=Hello%20${encodeURIComponent(a.patientName)},%20this%20is%20Dr.%20Balaji%20from%20HOMEo%20AI%20Classical%20Clinic%20regarding%20your%20appointment%20${a.id}." target="_blank" class="btn btn-whatsapp btn-sm">
          <i class="fa-brands fa-whatsapp"></i> Chat
        </a>
      </div>
    </div>
  `).join("");
}

function confirmAndNotifyPatient(id) {
  const appointments = getStoredAppointments();
  const target = appointments.find(a => a.id === id);
  if (target) {
    target.status = "CONFIRMED";
    saveAppointments(appointments);
    renderAdminAppointments();

    const msg = `*HOMEo AI Classical Clinic - Appointment Confirmed!*%0A%0A` +
      `Dear *${encodeURIComponent(target.patientName)}*,%0A` +
      `Your appointment with *Dr. Balaji, BHMS, MD (Homeopathy)* is *APPROVED & CONFIRMED*!%0A%0A` +
      `*Token ID:* ${encodeURIComponent(target.id)}%0A` +
      `*Date:* ${encodeURIComponent(target.date)}%0A` +
      `*Time Slot:* ${encodeURIComponent(target.timeSlot)}%0A` +
      `*Mode:* ${target.consultationType === "IN_CLINIC" ? "In-Clinic Visit (Chennai)" : "Online Video Consultation"}%0A` +
      `*Fee:* ₹500%0A%0A` +
      `*Address:* 74 Gandhi Road, Health Complex, Chennai%0A` +
      `_Looking forward to your consultation. Thank you!_`;

    window.open(`https://wa.me/91${target.patientPhone}?text=${msg}`, "_blank");
  }
}

function updateAppointmentStatus(id, newStatus) {
  const appointments = getStoredAppointments();
  const target = appointments.find(a => a.id === id);
  if (target) {
    target.status = newStatus;
    saveAppointments(appointments);

    // Sync to Firebase if present
    if (window.IS_FIREBASE_ENABLED && window.db) {
      try {
        window.db.collection("appointments").doc(id).update({ status: newStatus });
      } catch (e) {
        console.warn("Firebase update error", e);
      }
    }

    renderAdminDashboard();
  }
}

// Export Appointments to CSV
function exportAppointmentsCSV() {
  const appointments = getStoredAppointments();
  if (appointments.length === 0) {
    alert("No appointments to export.");
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
  link.setAttribute("download", `homeo_clinic_appointments_${new Date().toISOString().split("T")[0]}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

// ================= DOCTOR PROFILE & PICTURE CUSTOMIZATION =================
let uploadedDoctorPhotoBase64 = null;

function loadClinicProfile() {
  try {
    if (typeof localStorage === "undefined") return;
    const raw = localStorage.getItem(STORAGE_KEYS.PROFILE);
    if (raw) {
      const data = JSON.parse(raw);
      if (data.doctor) CLINIC_INFO.doctor = data.doctor;
      if (data.qualification) CLINIC_INFO.qualification = data.qualification;
      if (data.phone) {
        CLINIC_INFO.phone = data.phone;
        CLINIC_INFO.cleanPhone = data.phone.replace(/\D/g, "");
      }
      if (data.fee) CLINIC_INFO.fee = Number(data.fee);
      if (data.speciality) CLINIC_INFO.speciality = data.speciality;
      if (data.photo) CLINIC_INFO.photo = data.photo;
    }
  } catch (e) {
    console.warn("Profile load error", e);
  }
  applyClinicProfileToDOM();
}

function applyClinicProfileToDOM() {
  // Update Doctor Name across website
  document.querySelectorAll(".doc-name").forEach(el => el.textContent = CLINIC_INFO.doctor);
  const docH2 = document.querySelector("#doctor h2");
  if (docH2) docH2.textContent = CLINIC_INFO.doctor;

  // Update Qualification & Speciality
  const qualP = document.querySelector(".doc-qualification");
  if (qualP) qualP.textContent = `${CLINIC_INFO.qualification} • ${CLINIC_INFO.speciality || "Classical Constitutional Homeopathy"}`;

  // Update Doctor Photos
  if (CLINIC_INFO.photo) {
    document.querySelectorAll("img[alt*='Dr.'], img[alt*='Doctor'], .doc-avatar img").forEach(img => {
      img.src = CLINIC_INFO.photo;
    });
    const preview = document.getElementById("editDoctorPicPreview");
    if (preview) preview.src = CLINIC_INFO.photo;
  }

  // Update Consultation Fee
  document.querySelectorAll(".fee-amount").forEach(el => el.textContent = `₹${CLINIC_INFO.fee}`);

  // Update Phone numbers & links
  const phoneVal = CLINIC_INFO.phone || "+91 9876543210";
  const heroCall = document.getElementById("heroCallClinicBtn");
  if (heroCall) {
    heroCall.href = `tel:${phoneVal}`;
    heroCall.innerHTML = `<i class="fa-solid fa-phone-volume"></i> Call Clinic (${phoneVal})`;
  }
  const confirmPhoneLink = document.getElementById("confirmClinicPhoneLink");
  if (confirmPhoneLink) {
    confirmPhoneLink.href = `tel:${phoneVal}`;
    confirmPhoneLink.textContent = phoneVal;
  }
  const confirmCallBtn = document.getElementById("confirmCallBtn");
  if (confirmCallBtn) {
    confirmCallBtn.href = `tel:${phoneVal}`;
  }

  // Pre-fill inputs inside Doctor Admin Modal
  const nameInp = document.getElementById("editDoctorName");
  if (nameInp) nameInp.value = CLINIC_INFO.doctor;
  const qualInp = document.getElementById("editDoctorQual");
  if (qualInp) qualInp.value = CLINIC_INFO.qualification;
  const feeInp = document.getElementById("editDoctorFee");
  if (feeInp) feeInp.value = CLINIC_INFO.fee;
  const phoneInp = document.getElementById("editDoctorPhone");
  if (phoneInp) phoneInp.value = CLINIC_INFO.phone;
  const specInp = document.getElementById("editDoctorSpec");
  if (specInp) specInp.value = CLINIC_INFO.speciality || "Classical Constitutional Homeopathy";
}

function switchAdminTab(tabName) {
  const apptsTab = document.getElementById("adminApptsTabContent");
  const profileTab = document.getElementById("adminProfileTabContent");
  const apptsBtn = document.getElementById("adminTabApptsBtn");
  const profileBtn = document.getElementById("adminTabProfileBtn");

  if (tabName === "profile") {
    if (apptsTab) apptsTab.style.display = "none";
    if (profileTab) profileTab.style.display = "block";
    if (apptsBtn) {
      apptsBtn.style.borderBottom = "none";
      apptsBtn.style.color = "#64748b";
      apptsBtn.style.fontWeight = "600";
    }
    if (profileBtn) {
      profileBtn.style.borderBottom = "3px solid #0d9488";
      profileBtn.style.color = "#0d9488";
      profileBtn.style.fontWeight = "700";
    }
    applyClinicProfileToDOM();
  } else {
    if (apptsTab) apptsTab.style.display = "block";
    if (profileTab) profileTab.style.display = "none";
    if (apptsBtn) {
      apptsBtn.style.borderBottom = "3px solid #0d9488";
      apptsBtn.style.color = "#0d9488";
      apptsBtn.style.fontWeight = "700";
    }
    if (profileBtn) {
      profileBtn.style.borderBottom = "none";
      profileBtn.style.color = "#64748b";
      profileBtn.style.fontWeight = "600";
    }
  }
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

function saveClinicProfile() {
  const docName = document.getElementById("editDoctorName").value.trim() || "Dr. Balaji";
  const docQual = document.getElementById("editDoctorQual").value.trim() || "BHMS, MD (Homeopathy)";
  const docFee = document.getElementById("editDoctorFee").value.trim() || "500";
  const docPhone = document.getElementById("editDoctorPhone").value.trim() || "+919876543210";
  const docSpec = document.getElementById("editDoctorSpec").value.trim() || "Classical Constitutional Homeopathy";

  const profileData = {
    doctor: docName,
    qualification: docQual,
    fee: docFee,
    phone: docPhone,
    speciality: docSpec,
    photo: uploadedDoctorPhotoBase64 || CLINIC_INFO.photo || "doctor_portrait.jpg"
  };

  try {
    localStorage.setItem(STORAGE_KEYS.PROFILE, JSON.stringify(profileData));
  } catch (e) {
    console.error("Save profile error", e);
  }

  CLINIC_INFO.doctor = docName;
  CLINIC_INFO.qualification = docQual;
  CLINIC_INFO.fee = Number(docFee);
  CLINIC_INFO.phone = docPhone;
  CLINIC_INFO.cleanPhone = docPhone.replace(/\D/g, "");
  CLINIC_INFO.speciality = docSpec;
  if (profileData.photo) CLINIC_INFO.photo = profileData.photo;

  applyClinicProfileToDOM();

  const msg = document.getElementById("profileSaveMsg");
  if (msg) {
    msg.style.display = "block";
    setTimeout(() => { msg.style.display = "none"; }, 3500);
  }
}

// Clean CommonJS Export (No 302 redirects)
if (typeof module !== "undefined" && module.exports) {
  module.exports = { CLINIC_INFO };
}
