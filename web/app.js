/**
 * HOMEO CLINIC PRO - CLIENT & ADMIN WEB APPLICATION LOGIC
 */

// Clinic Configuration
const CLINIC_INFO = {
  name: "HOMEo Clinic Pro",
  doctor: "Dr. Yuga Bharathi",
  qualification: "B.H.M.S, M.D (Homeopathy)",
  phone: "+919876543210",
  cleanPhone: "919876543210",
  fee: 500
};

// Storage Keys
const STORAGE_KEYS = {
  APPOINTMENTS: "homeo_clinic_appointments",
  ADMIN_PIN: "homeo_clinic_admin_pin",
  ADMIN_SESSION: "homeo_clinic_admin_logged_in"
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

  const patientName = nameInput.value.trim();
  const patientPhone = phoneInput.value.trim();
  const patientAge = ageInput.value.trim();
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
  if (!/^\d{10}$/.test(patientPhone)) {
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

// ================= CONFIRMATION MODAL =================
function showConfirmation(appt) {
  document.getElementById("confirmApptId").textContent = appt.id;
  document.getElementById("confirmPatientName").textContent = appt.patientName;
  document.getElementById("confirmPatientPhone").textContent = appt.patientPhone;
  document.getElementById("confirmDateTime").textContent = `${appt.date} at ${appt.timeSlot}`;
  document.getElementById("confirmMode").textContent =
    appt.consultationType === "IN_CLINIC" ? "In-Clinic Visit (Chennai)" : "Online Video Consultation";

  // Configure Deep Link to Android App
  const syncBtn = document.getElementById("syncAppBtn");
  if (syncBtn) {
    const deepLink = `homeoclinic://appointment?id=${encodeURIComponent(appt.id)}&name=${encodeURIComponent(appt.patientName)}&phone=${encodeURIComponent(appt.patientPhone)}&age=${encodeURIComponent(appt.patientAge || 30)}&gender=${encodeURIComponent(appt.patientGender || "Male")}&date=${encodeURIComponent(appt.date)}&time=${encodeURIComponent(appt.timeSlot)}&mode=${encodeURIComponent(appt.consultationType)}&symptoms=${encodeURIComponent(appt.symptoms || "")}`;
    syncBtn.setAttribute("href", deepLink);
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
  const msg = `*HOMEo Clinic Pro - Appointment Booking*%0A%0A` +
    `*Token ID:* ${currentBooking.id}%0A` +
    `*Patient:* ${currentBooking.patientName}%0A` +
    `*Phone:* ${currentBooking.patientPhone}%0A` +
    `*Date:* ${currentBooking.date}%0A` +
    `*Time:* ${currentBooking.timeSlot}%0A` +
    `*Type:* ${currentBooking.consultationType === "IN_CLINIC" ? "In-Clinic Visit" : "Online Video"}%0A` +
    `*Doctor:* Dr. Yuga Bharathi%0A%0A` +
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
          <button class="btn btn-primary btn-sm" onclick="updateAppointmentStatus('${a.id}', 'CONFIRMED')">
            <i class="fa-solid fa-check"></i> Accept
          </button>
        ` : ""}
        ${a.status !== "COMPLETED" ? `
          <button class="btn btn-outline btn-sm" onclick="updateAppointmentStatus('${a.id}', 'COMPLETED')">
            <i class="fa-solid fa-circle-check"></i> Complete
          </button>
        ` : ""}
        ${a.status !== "CANCELLED" ? `
          <button class="btn btn-secondary btn-sm" style="color:#dc2626;" onclick="updateAppointmentStatus('${a.id}', 'CANCELLED')">
            <i class="fa-solid fa-ban"></i> Cancel
          </button>
        ` : ""}
        <a href="https://wa.me/91${a.patientPhone}?text=Hello%20${encodeURIComponent(a.patientName)},%20this%20is%20Dr.%20Yuga%20Bharathi%20from%20HOMEo%20Clinic%20regarding%20your%20appointment%20${a.id}." target="_blank" class="btn btn-whatsapp btn-sm">
          <i class="fa-brands fa-whatsapp"></i> Chat
        </a>
      </div>
    </div>
  `).join("");
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

// Node.js / Vercel Serverless Function fallback
// Prevents FUNCTION_INVOCATION_FAILED if Vercel ever executes app.js as a serverless function
if (typeof module !== "undefined" && module.exports) {
  module.exports = (req, res) => {
    if (res && typeof res.writeHead === "function") {
      res.writeHead(302, { Location: "/" });
      res.end();
    }
  };
}
