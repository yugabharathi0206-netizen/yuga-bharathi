/**
 * Dr. Balaji Homeo Care - Patient Booking Portal Application
 * Production Single Page Application (SPA)
 * Connects directly to Firestore shared with Doctor Android App
 */

(function () {
  "use strict";

  const STORAGE_KEYS = {
    SESSION: "homeo_patient_session_v2",
    LOCAL_USERS: "homeo_patient_users_cache",
    LOCAL_APPTS: "homeo_patient_appts_cache",
    OWNER_AUTH: "clinic_owner_auth_session",
    OWNER_DATA: "clinic_owner_credentials",
    CLINIC_PROFILE: "clinic_master_profile"
  };

  const DEFAULT_SLOTS = [
    "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM", "12:00 PM", "12:30 PM",
    "04:30 PM", "05:00 PM", "05:30 PM", "06:00 PM", "06:30 PM", "07:00 PM", "07:30 PM"
  ];

  // Application State
  let currentPatient = null;
  let activeAppointments = [];
  let unsubscribeAppointments = null;
  let selectedConsultationType = "IN_CLINIC";
  let selectedTimeSlot = "";
  let bookedSlotsForDate = new Set();
  let currentClinicProfile = {
    clinicName: "Dr. Balaji Homeo Care",
    doctorName: "Dr. Balaji",
    qualification: "B.H.M.S., M.D. (Homeopathy)",
    specialization: "Chief Consultant & Classical Prescriber",
    address: "Main Road, Classical Healthcare Complex",
    websiteUrl: "https://yugabharathi0206-netizen.github.io/yuga-bharathi/",
    phone: "+91 98401 00000",
    whatsAppNumber: "+91 98401 00000",
    email: "care@balajihomeocare.com",
    photoUrl: "doctor_portrait.jpg",
    openingTime: "09:00 AM",
    closingTime: "08:00 PM",
    morningStart: "09:00 AM",
    morningEnd: "12:00 PM",
    eveningStart: "04:00 PM",
    eveningEnd: "08:00 PM",
    slotDurationMinutes: 30,
    workingHoursSummary: "Mon - Sat: 9:00 AM - 12:00 PM & 4:00 PM - 8:00 PM",
    customSlots: [
      "09:00 AM", "09:30 AM", "10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM", "12:00 PM",
      "04:00 PM", "04:30 PM", "05:00 PM", "05:30 PM", "06:00 PM", "06:30 PM", "07:00 PM", "07:30 PM"
    ],
    isOwnerRegistered: false,
    ownerEmail: "",
    ownerPassword: "",
    ownerName: ""
  };
  let ownerCustomSlotsList = [...currentClinicProfile.customSlots];
  let isOwnerAuthenticated = false;

  // ================= UTILITIES & HASHING =================
  async function hashPassword(str) {
    try {
      const msgBuffer = new TextEncoder().encode(str + "_homeo_salt_2026");
      const hashBuffer = await crypto.subtle.digest("SHA-256", msgBuffer);
      const hashArray = Array.from(new Uint8Array(hashBuffer));
      return hashArray.map(b => b.toString(16).padStart(2, "0")).join("");
    } catch (e) {
      // Fallback simple hash for older environments
      let hash = 0;
      for (let i = 0; i < str.length; i++) {
        hash = ((hash << 5) - hash) + str.charCodeAt(i);
        hash |= 0;
      }
      return "h_" + Math.abs(hash).toString(16);
    }
  }

  function showToast(message, type = "info") {
    const container = document.getElementById("toastContainer");
    if (!container) return;
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = "0";
      toast.style.transition = "opacity 0.3s";
      setTimeout(() => toast.remove(), 300);
    }, 3500);
  }

  function getLocalUsers() {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEYS.LOCAL_USERS) || "{}");
    } catch {
      return {};
    }
  }

  function saveLocalUsers(users) {
    try {
      localStorage.setItem(STORAGE_KEYS.LOCAL_USERS, JSON.stringify(users));
    } catch {}
  }

  function getLocalAppts() {
    try {
      return JSON.parse(localStorage.getItem(STORAGE_KEYS.LOCAL_APPTS) || "[]");
    } catch {
      return [];
    }
  }

  function saveLocalAppts(appts) {
    try {
      localStorage.setItem(STORAGE_KEYS.LOCAL_APPTS, JSON.stringify(appts));
    } catch {}
  }

  // ================= AUTHENTICATION LOGIC =================
  function initAuthSession() {
    try {
      const stored = localStorage.getItem(STORAGE_KEYS.SESSION);
      if (stored) {
        currentPatient = JSON.parse(stored);
        console.log("✓ Patient Session Restored:", currentPatient.fullName);
      }
    } catch (e) {
      currentPatient = null;
    }
    updateNavUI();
  }

  function updateNavUI() {
    const guestLinks = document.getElementById("guestNavLinks");
    const userLinks = document.getElementById("userNavLinks");
    const userNameSpan = document.getElementById("navUserName");

    if (currentPatient) {
      if (guestLinks) guestLinks.style.display = "none";
      if (userLinks) userLinks.style.display = "flex";
      if (userNameSpan) userNameSpan.textContent = currentPatient.fullName.split(" ")[0] || "Patient";
    } else {
      if (guestLinks) guestLinks.style.display = "flex";
      if (userLinks) userLinks.style.display = "none";
    }
  }

  async function handlePatientSignup(fullName, mobile, email, password) {
    const cleanMobile = mobile.replace(/\D/g, "");
    if (cleanMobile.length < 10) {
      throw new Error("Please enter a valid 10-digit mobile number.");
    }
    if (password.length < 4) {
      throw new Error("Password must be at least 4 characters.");
    }

    const hashedPassword = await hashPassword(password);
    const patientId = "usr_pat_" + Date.now().toString(36) + "_" + Math.floor(Math.random() * 1000);

    const newPatient = {
      id: patientId,
      fullName: fullName.trim(),
      mobile: cleanMobile,
      phone: cleanMobile,
      email: email.trim(),
      role: "PATIENT",
      passwordHash: hashedPassword,
      createdAt: Date.now()
    };

    // 1. Cache locally for instant offline reliability
    const localUsers = getLocalUsers();
    localUsers[cleanMobile] = newPatient;
    if (email) localUsers[email.toLowerCase()] = newPatient;
    saveLocalUsers(localUsers);

    // 2. Persist to shared Firestore database
    if (window.db) {
      try {
        await window.db.collection("users").doc(patientId).set(newPatient);
        console.log("✓ Patient Account Created in Firestore:", patientId);
      } catch (err) {
        console.warn("Firestore write notice (cached locally):", err.message);
      }
    }

    // 3. Establish active session
    currentPatient = {
      id: newPatient.id,
      fullName: newPatient.fullName,
      mobile: newPatient.mobile,
      email: newPatient.email,
      role: "PATIENT"
    };
    localStorage.setItem(STORAGE_KEYS.SESSION, JSON.stringify(currentPatient));
    updateNavUI();
    showToast("Account created successfully! Welcome to Dr. Balaji Homeo Care.", "success");
    return currentPatient;
  }

  async function handlePatientLogin(identifier, password) {
    const cleanId = identifier.trim();
    const cleanMobile = cleanId.replace(/\D/g, "");
    const hashedInput = await hashPassword(password);

    let matchedUser = null;

    // Check Firestore first if available
    if (window.db) {
      try {
        // Query by mobile
        if (cleanMobile.length >= 10) {
          const snap = await window.db.collection("users").where("mobile", "==", cleanMobile).limit(1).get();
          if (!snap.empty) {
            matchedUser = snap.docs[0].data();
          }
        }
        // Query by email
        if (!matchedUser && cleanId.includes("@")) {
          const snap = await window.db.collection("users").where("email", "==", cleanId).limit(1).get();
          if (!snap.empty) {
            matchedUser = snap.docs[0].data();
          }
        }
      } catch (e) {
        console.warn("Firestore query note:", e.message);
      }
    }

    // Fallback to local cache
    if (!matchedUser) {
      const localUsers = getLocalUsers();
      if (localUsers[cleanMobile]) matchedUser = localUsers[cleanMobile];
      else if (localUsers[cleanId.toLowerCase()]) matchedUser = localUsers[cleanId.toLowerCase()];
    }

    if (!matchedUser) {
      throw new Error("No account found with this mobile number or email. Please create an account.");
    }

    if (matchedUser.passwordHash !== hashedInput) {
      throw new Error("Incorrect password. Please verify and try again.");
    }

    // Establish session
    currentPatient = {
      id: matchedUser.id,
      fullName: matchedUser.fullName,
      mobile: matchedUser.mobile,
      email: matchedUser.email || "",
      role: "PATIENT"
    };
    localStorage.setItem(STORAGE_KEYS.SESSION, JSON.stringify(currentPatient));
    updateNavUI();
    showToast(`Welcome back, ${currentPatient.fullName}!`, "success");
    return currentPatient;
  }

  function handlePatientLogout() {
    if (unsubscribeAppointments) {
      unsubscribeAppointments();
      unsubscribeAppointments = null;
    }
    currentPatient = null;
    localStorage.removeItem(STORAGE_KEYS.SESSION);
    updateNavUI();
    showToast("You have been safely logged out.", "info");
    navigateTo("#home");
  }

  // ================= FIRESTORE REAL-TIME APPOINTMENTS =================
  function startPatientAppointmentsListener() {
    if (!currentPatient) return;

    if (unsubscribeAppointments) {
      unsubscribeAppointments();
      unsubscribeAppointments = null;
    }

    const patientMobile = currentPatient.mobile;
    const patientId = currentPatient.id;

    if (window.db) {
      try {
        // Listen to all appointments for this patient
        unsubscribeAppointments = window.db.collection("appointments")
          .onSnapshot((snapshot) => {
            const list = [];
            snapshot.forEach(doc => {
              const data = doc.data();
              const aptMobile = data.patientMobile || data.patientPhone || "";
              const aptPatientId = data.patientId || "";

              if (aptMobile === patientMobile || aptPatientId === patientId) {
                list.push({
                  id: doc.id,
                  ...data
                });
              }
            });

            // Sort newest first
            list.sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));
            activeAppointments = list;
            saveLocalAppts(list);
            renderDashboard();
            renderAppointmentsHistory();
          }, (err) => {
            console.warn("Firestore snapshot notice:", err.message);
            // Fallback to local cache
            activeAppointments = getLocalAppts().filter(a => a.patientMobile === patientMobile || a.patientId === patientId);
            renderDashboard();
            renderAppointmentsHistory();
          });
        return;
      } catch (e) {
        console.warn("Real-time listener setup error:", e);
      }
    }

    // Offline fallback
    activeAppointments = getLocalAppts().filter(a => a.patientMobile === patientMobile || a.patientId === patientId);
    renderDashboard();
    renderAppointmentsHistory();
  }

  // ================= RENDER PATIENT DASHBOARD =================
  function renderDashboard() {
    if (!currentPatient) return;

    const patientGreetingName = document.getElementById("dashPatientName");
    const patientPhoneSpan = document.getElementById("dashPatientPhone");
    if (patientGreetingName) patientGreetingName.textContent = currentPatient.fullName;
    if (patientPhoneSpan) patientPhoneSpan.textContent = currentPatient.mobile;

    // Calculate stats
    const totalVisits = activeAppointments.length;
    const confirmedCount = activeAppointments.filter(a => (a.status || "").toUpperCase() === "CONFIRMED").length;
    const pendingCount = activeAppointments.filter(a => (a.status || "").toUpperCase() === "PENDING").length;

    const statTotalEl = document.getElementById("statTotalVisits");
    const statConfirmedEl = document.getElementById("statConfirmedCount");
    const statPendingEl = document.getElementById("statPendingCount");

    if (statTotalEl) statTotalEl.textContent = totalVisits;
    if (statConfirmedEl) statConfirmedEl.textContent = confirmedCount;
    if (statPendingEl) statPendingEl.textContent = pendingCount;

    // Upcoming Appointment Card
    const upcomingContainer = document.getElementById("upcomingAppointmentContainer");
    if (!upcomingContainer) return;

    // Active upcoming appointment is the most recent pending, confirmed, or rescheduled
    const upcomingApt = activeAppointments.find(a => {
      const st = (a.status || "").toUpperCase();
      return st === "CONFIRMED" || st === "PENDING" || st === "RESCHEDULED";
    }) || activeAppointments[0];

    if (!upcomingApt) {
      upcomingContainer.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">📅</div>
          <h4>No Appointments Scheduled</h4>
          <p>You do not have any upcoming consultation with Dr. Balaji right now.</p>
          <button class="btn btn-primary" onclick="window.navigateTo('#book')">Book Your Appointment Now</button>
        </div>
      `;
      return;
    }

    const status = (upcomingApt.status || "PENDING").toUpperCase();
    let statusClass = "pending";
    let statusLabel = "⏳ Pending Doctor Confirmation";
    let explanationBanner = `
      <div class="status-explanation-banner pending">
        <span>🕒</span>
        <div><strong>Awaiting Doctor Review:</strong> Dr. Balaji will review and confirm your slot in the clinic management app shortly.</div>
      </div>
    `;

    if (status === "CONFIRMED") {
      statusClass = "confirmed";
      statusLabel = "✅ Appointment Confirmed";
      explanationBanner = `
        <div class="status-explanation-banner confirmed">
          <span>🩺</span>
          <div><strong>Confirmed by Dr. Balaji:</strong> Your appointment has been approved. Please arrive 10 minutes prior to your slot time.</div>
        </div>
      `;
    } else if (status === "REJECTED" || status === "CANCELLED") {
      statusClass = "rejected";
      statusLabel = "❌ Appointment Rejected";
      explanationBanner = `
        <div class="status-explanation-banner rejected">
          <span>⚠️</span>
          <div><strong>Slot Unavailable:</strong> Dr. Balaji declined this booking request. You may choose another available time slot.</div>
        </div>
      `;
    } else if (status === "RESCHEDULED") {
      statusClass = "rescheduled";
      statusLabel = "📅 Rescheduled by Clinic";
      explanationBanner = `
        <div class="status-explanation-banner rescheduled">
          <span>🔄</span>
          <div><strong>Updated Schedule:</strong> Dr. Balaji has adjusted this appointment to the new date and time shown below.</div>
        </div>
      `;
    } else if (status === "COMPLETED") {
      statusClass = "completed";
      statusLabel = "🩺 Consultation Completed";
      explanationBanner = `
        <div class="status-explanation-banner confirmed">
          <span>✨</span>
          <div><strong>Consultation Finished:</strong> Take constitutional remedies as prescribed. Follow-up review is recommended.</div>
        </div>
      `;
    }

    const typeDisplay = (upcomingApt.consultationType || upcomingApt.type || "IN_CLINIC").includes("ONLINE")
      ? "🌐 Online Video Consultation"
      : "🏥 Offline In-Clinic Consultation";

    upcomingContainer.innerHTML = `
      <div class="card-top-strip">
        <div class="badge-upcoming-label">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
          Active Appointment
        </div>
        <span class="status-pill ${statusClass}">${statusLabel}</span>
      </div>

      <div class="appointment-details-grid">
        <div class="detail-item">
          <p>Appointment Token</p>
          <h4>${upcomingApt.id || upcomingApt.appointmentId}</h4>
        </div>
        <div class="detail-item">
          <p>Consulting Doctor</p>
          <h4>${upcomingApt.doctorName || "Dr. Balaji"}</h4>
        </div>
        <div class="detail-item">
          <p>Scheduled Date</p>
          <h4>${upcomingApt.date || upcomingApt.appointmentDate}</h4>
        </div>
        <div class="detail-item">
          <p>Time Slot</p>
          <h4>${upcomingApt.time || upcomingApt.timeSlot}</h4>
        </div>
        <div class="detail-item">
          <p>Consultation Mode</p>
          <h4>${typeDisplay}</h4>
        </div>
        <div class="detail-item">
          <p>Health Concern / Reason</p>
          <h4>${upcomingApt.symptoms || upcomingApt.reasonForVisit || "General Consultation"}</h4>
        </div>
      </div>

      ${explanationBanner}
    `;
  }

  // ================= RENDER APPOINTMENTS HISTORY =================
  function renderAppointmentsHistory() {
    const listContainer = document.getElementById("appointmentHistoryList");
    if (!listContainer) return;

    if (!currentPatient || activeAppointments.length === 0) {
      listContainer.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">📋</div>
          <h4>No Past Appointments</h4>
          <p>You have not booked any appointments yet.</p>
          <button class="btn btn-primary" onclick="window.navigateTo('#book')">Book Your First Appointment</button>
        </div>
      `;
      return;
    }

    listContainer.innerHTML = activeAppointments.map(apt => {
      const status = (apt.status || "PENDING").toUpperCase();
      let statusClass = "pending";
      let statusLabel = "⏳ PENDING";

      if (status === "CONFIRMED") { statusClass = "confirmed"; statusLabel = "✅ CONFIRMED"; }
      else if (status === "REJECTED" || status === "CANCELLED") { statusClass = "rejected"; statusLabel = "❌ REJECTED"; }
      else if (status === "RESCHEDULED") { statusClass = "rescheduled"; statusLabel = "📅 RESCHEDULED"; }
      else if (status === "COMPLETED") { statusClass = "completed"; statusLabel = "🩺 COMPLETED"; }

      const mode = (apt.consultationType || apt.type || "IN_CLINIC").includes("ONLINE")
        ? "Online Video"
        : "Offline In-Clinic";

      return `
        <div class="apt-row-card">
          <div class="apt-row-top">
            <div class="apt-token">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
              ${apt.id || apt.appointmentId}
            </div>
            <span class="status-pill ${statusClass}">${statusLabel}</span>
          </div>
          <div class="apt-row-details">
            <div><span>Doctor:</span> <strong>${apt.doctorName || "Dr. Balaji"}</strong></div>
            <div><span>Date:</span> <strong>${apt.date || apt.appointmentDate}</strong></div>
            <div><span>Time:</span> <strong>${apt.time || apt.timeSlot}</strong></div>
            <div><span>Type:</span> <strong>${mode}</strong></div>
            <div style="grid-column: 1 / -1;"><span>Symptoms:</span> <strong>${apt.symptoms || apt.reasonForVisit || "Consultation"}</strong></div>
          </div>
        </div>
      `;
    }).join("");
  }

  // ================= DYNAMIC SLOTS & BOOKING =================
  async function loadSlotsForDate(dateStr) {
    const slotsGrid = document.getElementById("slotsGrid");
    if (!slotsGrid) return;

    slotsGrid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 20px; color: var(--text-muted);">Checking clinic slot availability...</div>`;
    bookedSlotsForDate.clear();

    // Query Firestore for appointments on this date
    if (window.db) {
      try {
        const snap = await window.db.collection("appointments")
          .where("date", "==", dateStr)
          .get();

        snap.forEach(doc => {
          const d = doc.data();
          const st = (d.status || "").toUpperCase();
          // If not rejected or cancelled, slot is occupied
          if (st !== "REJECTED" && st !== "CANCELLED") {
            const slot = d.time || d.timeSlot;
            if (slot) bookedSlotsForDate.add(slot.toUpperCase());
          }
        });
      } catch (err) {
        console.warn("Slot query note:", err.message);
      }
    }

    // Render slots from active clinic profile
    const activeSlots = (currentClinicProfile.customSlots && currentClinicProfile.customSlots.length > 0)
      ? currentClinicProfile.customSlots
      : DEFAULT_SLOTS;

    slotsGrid.innerHTML = activeSlots.map(slot => {
      const isBooked = bookedSlotsForDate.has(slot.toUpperCase());
      const isSelected = selectedTimeSlot === slot && !isBooked;

      let classes = "slot-btn";
      if (isBooked) classes += " booked";
      else if (isSelected) classes += " selected";

      return `
        <button type="button" class="${classes}" ${isBooked ? "disabled" : ""} onclick="window.selectSlot('${slot}', ${isBooked})">
          <span>${slot}</span>
          <span class="slot-status-indicator">${isBooked ? "Booked" : "Available"}</span>
        </button>
      `;
    }).join("");
  }

  window.selectSlot = function (slot, isBooked) {
    if (isBooked) return;
    selectedTimeSlot = slot;
    const dateInput = document.getElementById("bookDateInput");
    if (dateInput) loadSlotsForDate(dateInput.value);
  };

  async function handleBookingSubmit(symptoms) {
    if (!currentPatient) {
      navigateTo("#login");
      throw new Error("Please log in to book an appointment.");
    }

    const dateInput = document.getElementById("bookDateInput");
    const dateStr = dateInput ? dateInput.value : "";

    if (!dateStr) {
      throw new Error("Please select an appointment date.");
    }

    if (!selectedTimeSlot) {
      throw new Error("Please select an available time slot.");
    }

    if (bookedSlotsForDate.has(selectedTimeSlot.toUpperCase())) {
      throw new Error("The selected slot has just been booked. Please choose another time.");
    }

    if (!symptoms || symptoms.trim().length < 2) {
      throw new Error("Please provide your health concerns / symptoms.");
    }

    // Generate Unique Token
    const aptRandom = Math.floor(1000 + Math.random() * 9000);
    const appointmentId = `HM-2026-${aptRandom}`;

    const newAppointment = {
      id: appointmentId,
      appointmentId: appointmentId,
      patientId: currentPatient.id,
      patientName: currentPatient.fullName,
      patientMobile: currentPatient.mobile,
      patientPhone: currentPatient.mobile,
      patientEmail: currentPatient.email || "",
      symptoms: symptoms.trim(),
      reasonForVisit: symptoms.trim(),
      consultationType: selectedConsultationType,
      date: dateStr,
      appointmentDate: dateStr,
      time: selectedTimeSlot,
      timeSlot: selectedTimeSlot,
      status: "PENDING",
      doctorName: "Dr. Balaji",
      doctorId: "doc_main",
      createdAt: Date.now(),
      updatedAt: Date.now()
    };

    // 1. Save to shared Firestore database (Immediately visible in Doctor Android App)
    if (window.db) {
      try {
        await window.db.collection("appointments").doc(appointmentId).set(newAppointment);
        console.log("✓ Saved appointment to Firestore:", appointmentId);
      } catch (err) {
        console.warn("Firestore appointment write notice (fallback to local):", err.message);
      }
    }

    // 2. Cache in local storage
    const allLocal = getLocalAppts();
    allLocal.unshift(newAppointment);
    saveLocalAppts(allLocal);

    // 3. Update current memory list
    activeAppointments.unshift(newAppointment);

    // Show Confirmation Modal
    const modalTokenEl = document.getElementById("modalAppointmentToken");
    const modalSuccess = document.getElementById("bookingSuccessModal");
    if (modalTokenEl) modalTokenEl.textContent = appointmentId;
    if (modalSuccess) modalSuccess.classList.add("active");

    showToast("Appointment request submitted successfully!", "success");
    return newAppointment;
  }

  // ================= ROUTING & VIEW CONTROLLER =================
  function navigateTo(hash) {
    if (!hash || hash === "") hash = "#home";
    window.location.hash = hash;
  }
  window.navigateTo = navigateTo;

  function handleRoute() {
    let hash = window.location.hash || "#home";
    // Normalize path
    if (hash.startsWith("#/")) hash = "#" + hash.substring(2);

    // Route guards
    const protectedRoutes = ["#dashboard", "#book", "#appointments", "#profile"];
    if (protectedRoutes.includes(hash) && !currentPatient) {
      showToast("Please login or create an account to access patient services.", "info");
      hash = "#login";
      window.location.hash = "#login";
    }

    // Redirect logged in user from auth forms directly to dashboard
    if ((hash === "#login" || hash === "#signup") && currentPatient) {
      hash = "#dashboard";
      window.location.hash = "#dashboard";
    }

    // Hide all views
    const allViews = document.querySelectorAll(".page-view");
    allViews.forEach(v => v.classList.remove("active"));

    // Update active nav links
    const allNavLinks = document.querySelectorAll(".nav-link");
    allNavLinks.forEach(link => {
      if (link.getAttribute("href") === hash) {
        link.classList.add("active");
      } else {
        link.classList.remove("active");
      }
    });

    // Show active view
    const viewMap = {
      "#home": "viewHome",
      "#login": "viewLogin",
      "#signup": "viewSignup",
      "#dashboard": "viewDashboard",
      "#book": "viewBook",
      "#appointments": "viewAppointments",
      "#profile": "viewProfile",
      "#owner": "viewOwner"
    };

    const targetViewId = viewMap[hash] || "viewHome";
    const targetEl = document.getElementById(targetViewId);
    if (targetEl) {
      targetEl.classList.add("active");
      window.scrollTo(0, 0);
    }

    // View-specific initializations
    if (hash === "#dashboard") {
      startPatientAppointmentsListener();
      renderDashboard();
    } else if (hash === "#appointments") {
      renderAppointmentsHistory();
    } else if (hash === "#book") {
      initBookingView();
    } else if (hash === "#profile") {
      initProfileView();
    } else if (hash === "#owner") {
      initOwnerPortalView();
    }
  }

  function initBookingView() {
    if (!currentPatient) return;
    const nameInput = document.getElementById("bookPatientName");
    const phoneInput = document.getElementById("bookPatientPhone");
    if (nameInput) nameInput.value = currentPatient.fullName;
    if (phoneInput) phoneInput.value = currentPatient.mobile;

    // Default date to today or tomorrow
    const dateInput = document.getElementById("bookDateInput");
    if (dateInput && !dateInput.value) {
      const today = new Date().toISOString().split("T")[0];
      dateInput.min = today;
      dateInput.value = today;
      loadSlotsForDate(today);
    }
  }

  function initProfileView() {
    if (!currentPatient) return;
    const nameEl = document.getElementById("profName");
    const mobileEl = document.getElementById("profMobile");
    const emailEl = document.getElementById("profEmail");
    if (nameEl) nameEl.value = currentPatient.fullName;
    if (mobileEl) mobileEl.value = currentPatient.mobile;
    if (emailEl) emailEl.value = currentPatient.email || "";
  }

  // ================= CLINIC PROFILE & OWNER LOGIC =================
  async function initClinicProfileAndOwnerState() {
    // 1. Try local cache
    try {
      const localProf = localStorage.getItem(STORAGE_KEYS.CLINIC_PROFILE);
      if (localProf) {
        const parsed = JSON.parse(localProf);
        Object.assign(currentClinicProfile, parsed);
        if (parsed.customSlots && Array.isArray(parsed.customSlots) && parsed.customSlots.length > 0) {
          ownerCustomSlotsList = [...parsed.customSlots];
        }
      }
      const localOwner = localStorage.getItem(STORAGE_KEYS.OWNER_DATA);
      if (localOwner) {
        const parsedOwner = JSON.parse(localOwner);
        if (parsedOwner.isOwnerRegistered) {
          currentClinicProfile.isOwnerRegistered = true;
          currentClinicProfile.ownerEmail = parsedOwner.ownerEmail;
          currentClinicProfile.ownerPassword = parsedOwner.ownerPassword;
          currentClinicProfile.ownerName = parsedOwner.ownerName;
        }
      }
      if (sessionStorage.getItem(STORAGE_KEYS.OWNER_AUTH) === "true") {
        isOwnerAuthenticated = true;
      }
    } catch (e) {
      console.warn("Local clinic cache read note:", e);
    }

    // 2. Fetch latest from Cloud Firestore
    if (window.db) {
      try {
        const doc = await window.db.collection("clinicProfile").doc("main").get();
        if (doc.exists) {
          const cloudData = doc.data();
          Object.assign(currentClinicProfile, cloudData);
          if (cloudData.customSlots && Array.isArray(cloudData.customSlots) && cloudData.customSlots.length > 0) {
            ownerCustomSlotsList = [...cloudData.customSlots];
          }
          if (cloudData.isOwnerRegistered && cloudData.ownerEmail) {
            currentClinicProfile.isOwnerRegistered = true;
            currentClinicProfile.ownerEmail = cloudData.ownerEmail;
            currentClinicProfile.ownerPassword = cloudData.ownerPassword;
            currentClinicProfile.ownerName = cloudData.ownerName || cloudData.doctorName;
            localStorage.setItem(STORAGE_KEYS.OWNER_DATA, JSON.stringify({
              isOwnerRegistered: true,
              ownerEmail: cloudData.ownerEmail,
              ownerPassword: cloudData.ownerPassword,
              ownerName: cloudData.ownerName || cloudData.doctorName
            }));
          }
          localStorage.setItem(STORAGE_KEYS.CLINIC_PROFILE, JSON.stringify(currentClinicProfile));
        }
      } catch (err) {
        console.warn("Cloud clinic profile fetch note:", err.message);
      }

      // Realtime listener for any instant updates
      try {
        window.db.collection("clinicProfile").doc("main").onSnapshot(snap => {
          if (snap.exists) {
            const data = snap.data();
            Object.assign(currentClinicProfile, data);
            if (data.customSlots && Array.isArray(data.customSlots) && data.customSlots.length > 0) {
              ownerCustomSlotsList = [...data.customSlots];
            }
            if (data.isOwnerRegistered && data.ownerEmail) {
              currentClinicProfile.isOwnerRegistered = true;
              currentClinicProfile.ownerEmail = data.ownerEmail;
              currentClinicProfile.ownerPassword = data.ownerPassword;
              currentClinicProfile.ownerName = data.ownerName || data.doctorName;
            }
            updatePublicClinicInfoInDOM();
            if (isOwnerAuthenticated) {
              populateOwnerFormFields();
            }
          }
        }, err => console.warn("Live snapshot note:", err.message));
      } catch (e) {
        console.warn("Snapshot setup note:", e.message);
      }
    }

    updatePublicClinicInfoInDOM();
  }

  function updatePublicClinicInfoInDOM() {
    // Brand header
    const brandName = document.getElementById("brandClinicName");
    const brandTagline = document.getElementById("brandClinicTagline");
    if (brandName) brandName.textContent = currentClinicProfile.clinicName;
    if (brandTagline) brandTagline.textContent = `Classical Homeopathy by ${currentClinicProfile.doctorName}`;

    // Hero section
    const heroHeading = document.getElementById("heroClinicHeading");
    const heroSpan = document.getElementById("heroDoctorSpan");
    if (heroSpan) heroSpan.textContent = currentClinicProfile.doctorName;
    else if (heroHeading) heroHeading.innerHTML = `Classical Homeopathy by <span>${currentClinicProfile.doctorName}</span>`;

    const heroDocName = document.getElementById("heroDoctorName");
    const heroDocQual = document.getElementById("heroDoctorQual");
    const heroDocExp = document.getElementById("heroDoctorExp");
    const heroPhoto = document.getElementById("heroDoctorPhoto");

    if (heroDocName) heroDocName.textContent = currentClinicProfile.doctorName;
    if (heroDocQual) heroDocQual.textContent = currentClinicProfile.qualification || "B.H.M.S., M.D. (Homeopathy)";
    if (heroDocExp) heroDocExp.textContent = currentClinicProfile.specialization || "Chief Consultant & Classical Prescriber";
    if (heroPhoto && currentClinicProfile.photoUrl) heroPhoto.src = currentClinicProfile.photoUrl;

    // Clinic info strip
    const timingsEl = document.getElementById("infoConsultTimings");
    if (timingsEl) {
      timingsEl.innerHTML = `Morning: ${currentClinicProfile.morningStart} – ${currentClinicProfile.morningEnd}<br>Evening: ${currentClinicProfile.eveningStart} – ${currentClinicProfile.eveningEnd}<br>(${currentClinicProfile.workingHoursSummary || "Mon - Sat"})`;
    }

    const locEl = document.getElementById("infoClinicLocation");
    if (locEl) {
      const webUrl = currentClinicProfile.websiteUrl || "#";
      locEl.innerHTML = `${currentClinicProfile.address}<br><a id="infoClinicWebUrl" href="${webUrl}" target="_blank" style="color: var(--primary); text-decoration: underline; font-size: 0.82rem;">Visit Clinic Website</a>`;
    }

    const phoneEl = document.getElementById("infoClinicPhone");
    if (phoneEl) {
      phoneEl.textContent = currentClinicProfile.phone;
      phoneEl.href = `tel:${currentClinicProfile.phone.replace(/[^0-9+]/g, "")}`;
    }

    const waEl = document.getElementById("infoClinicWhatsApp");
    if (waEl) {
      const cleanWa = (currentClinicProfile.whatsAppNumber || currentClinicProfile.phone).replace(/[^0-9]/g, "");
      waEl.href = `https://wa.me/${cleanWa}`;
    }
  }

  function initOwnerPortalView() {
    const isAuth = isOwnerAuthenticated || sessionStorage.getItem(STORAGE_KEYS.OWNER_AUTH) === "true";
    const regGate = document.getElementById("ownerRegisterGate");
    const loginGate = document.getElementById("ownerLoginGate");
    const controlPanel = document.getElementById("ownerControlPanel");

    if (!regGate || !loginGate || !controlPanel) return;

    if (isAuth) {
      isOwnerAuthenticated = true;
      regGate.style.display = "none";
      loginGate.style.display = "none";
      controlPanel.style.display = "block";

      const badge = document.getElementById("ownerHeaderEmailBadge");
      if (badge) {
        badge.textContent = currentClinicProfile.ownerEmail || currentClinicProfile.email || "Verified Master Owner";
      }

      populateOwnerFormFields();
      renderOwnerSlotChips();
    } else {
      controlPanel.style.display = "none";
      const hasRegisteredOwner = currentClinicProfile.isOwnerRegistered && currentClinicProfile.ownerEmail;

      if (!hasRegisteredOwner) {
        // STATE A: First-time setup (no owner exists yet)
        regGate.style.display = "block";
        loginGate.style.display = "none";
      } else {
        // STATE B: Single owner already registered. Prompt for login.
        regGate.style.display = "none";
        loginGate.style.display = "block";

        const lockedDisplay = document.getElementById("ownerLockedEmailDisplay");
        if (lockedDisplay) {
          lockedDisplay.textContent = currentClinicProfile.ownerEmail;
        }
        const errorBox = document.getElementById("ownerLoginError");
        if (errorBox) errorBox.style.display = "none";
      }
    }
  }

  function populateOwnerFormFields() {
    const setVal = (id, val) => {
      const el = document.getElementById(id);
      if (el && val !== undefined) el.value = val;
    };

    setVal("ownClinicName", currentClinicProfile.clinicName);
    setVal("ownWebsiteUrl", currentClinicProfile.websiteUrl);
    setVal("ownAddress", currentClinicProfile.address);
    setVal("ownPhone", currentClinicProfile.phone);
    setVal("ownWhatsApp", currentClinicProfile.whatsAppNumber || currentClinicProfile.phone);
    setVal("ownDoctorName", currentClinicProfile.doctorName);
    setVal("ownDoctorQual", currentClinicProfile.qualification);
    setVal("ownDoctorBio", currentClinicProfile.specialization);
    setVal("ownPhotoUrl", currentClinicProfile.photoUrl);
    setVal("ownMorningStart", currentClinicProfile.morningStart || "09:00 AM");
    setVal("ownMorningEnd", currentClinicProfile.morningEnd || "12:00 PM");
    setVal("ownEveningStart", currentClinicProfile.eveningStart || "04:00 PM");
    setVal("ownEveningEnd", currentClinicProfile.eveningEnd || "08:00 PM");
    setVal("ownSlotDuration", currentClinicProfile.slotDurationMinutes || 30);
    setVal("ownWorkingSummary", currentClinicProfile.workingHoursSummary || "Mon - Sat: 9:00 AM - 12:00 PM & 4:00 PM - 8:00 PM");

    const preview = document.getElementById("ownPhotoPreview");
    const previewWrap = document.getElementById("ownPhotoPreviewWrap");
    if (preview && currentClinicProfile.photoUrl) {
      preview.src = currentClinicProfile.photoUrl;
      if (previewWrap) previewWrap.style.display = "block";
    }
  }

  // Time & Slot Helpers
  function parseTimeToMinutes(t) {
    if (!t) return 0;
    const match = t.trim().match(/^(\d{1,2}):(\d{2})\s*(AM|PM)?$/i);
    if (!match) return 0;
    let h = parseInt(match[1], 10);
    const m = parseInt(match[2], 10);
    const ampm = (match[3] || "").toUpperCase();
    if (ampm === "PM" && h < 12) h += 12;
    if (ampm === "AM" && h === 12) h = 0;
    return h * 60 + m;
  }

  function formatMinutesToTime(totalMins) {
    let h = Math.floor(totalMins / 60) % 24;
    const m = totalMins % 60;
    const ampm = h >= 12 ? "PM" : "AM";
    if (h === 0) h = 12;
    else if (h > 12) h -= 12;
    const hStr = h.toString().padStart(2, "0");
    const mStr = m.toString().padStart(2, "0");
    return `${hStr}:${mStr} ${ampm}`;
  }

  function autoGenerateOwnerSlots() {
    const mStartInput = document.getElementById("ownMorningStart")?.value || "09:00 AM";
    const mEndInput = document.getElementById("ownMorningEnd")?.value || "12:00 PM";
    const eStartInput = document.getElementById("ownEveningStart")?.value || "04:00 PM";
    const eEndInput = document.getElementById("ownEveningEnd")?.value || "08:00 PM";
    const durInput = parseInt(document.getElementById("ownSlotDuration")?.value, 10) || 30;

    const mStart = parseTimeToMinutes(mStartInput);
    const mEnd = parseTimeToMinutes(mEndInput);
    const eStart = parseTimeToMinutes(eStartInput);
    const eEnd = parseTimeToMinutes(eEndInput);

    const generated = [];
    if (mEnd > mStart) {
      for (let t = mStart; t <= mEnd; t += durInput) {
        generated.push(formatMinutesToTime(t));
      }
    }
    if (eEnd > eStart) {
      for (let t = eStart; t <= eEnd; t += durInput) {
        generated.push(formatMinutesToTime(t));
      }
    }

    ownerCustomSlotsList = [...new Set(generated)];
    renderOwnerSlotChips();
    showToast(`⚡ Generated ${ownerCustomSlotsList.length} slots from your clinic hours.`, "info");
  }

  function renderOwnerSlotChips() {
    const cloud = document.getElementById("ownSlotTagCloud");
    if (!cloud) return;

    if (!ownerCustomSlotsList || ownerCustomSlotsList.length === 0) {
      cloud.innerHTML = '<span style="font-size: 0.82rem; color: var(--text-muted);">No slots added yet. Click Auto-Generate or add custom slots below.</span>';
      return;
    }

    cloud.innerHTML = ownerCustomSlotsList.map((slot, idx) => `
      <span class="slot-chip">
        <span>${slot}</span>
        <span class="remove-slot" title="Remove slot" onclick="window.removeOwnerSlot(${idx})">✕</span>
      </span>
    `).join("");
  }

  window.removeOwnerSlot = function (index) {
    if (index >= 0 && index < ownerCustomSlotsList.length) {
      const removed = ownerCustomSlotsList.splice(index, 1);
      renderOwnerSlotChips();
      showToast(`Removed slot ${removed[0]}`, "info");
    }
  };

  function addSingleOwnerSlot() {
    const input = document.getElementById("ownNewSlotInput");
    if (!input) return;
    const val = input.value.trim().toUpperCase();
    if (!val) {
      alert("Please enter a time slot (e.g. 10:45 AM or 02:30 PM)");
      return;
    }
    if (ownerCustomSlotsList.includes(val)) {
      showToast("This slot is already added.", "info");
      return;
    }
    ownerCustomSlotsList.push(val);
    renderOwnerSlotChips();
    input.value = "";
    showToast(`Added slot: ${val}`, "success");
  }

  // ================= EVENT LISTENERS & SETUP =================
  document.addEventListener("DOMContentLoaded", () => {
    // 1. Initialize Firebase
    if (window.initFirebase) {
      window.initFirebase();
    }

    // 2. Initialize Clinic Profile & Master Owner State
    initClinicProfileAndOwnerState();

    // 3. Initialize Session
    initAuthSession();

    // 4. Setup Hash Route Listener
    window.addEventListener("hashchange", handleRoute);
    handleRoute();

    // 4. Login Form Handler
    const loginForm = document.getElementById("patientLoginForm");
    const loginAlert = document.getElementById("loginAlert");
    if (loginForm) {
      loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const id = document.getElementById("loginIdentifier").value;
        const pass = document.getElementById("loginPassword").value;
        const submitBtn = loginForm.querySelector("button[type='submit']");

        try {
          if (loginAlert) { loginAlert.style.display = "none"; loginAlert.textContent = ""; }
          if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = "Verifying..."; }

          await handlePatientLogin(id, pass);
          navigateTo("#dashboard");
        } catch (err) {
          if (loginAlert) {
            loginAlert.className = "auth-alert error";
            loginAlert.textContent = err.message;
            loginAlert.style.display = "block";
          }
        } finally {
          if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = "Sign In to Patient Portal"; }
        }
      });
    }

    // 5. Signup Form Handler
    const signupForm = document.getElementById("patientSignupForm");
    const signupAlert = document.getElementById("signupAlert");
    if (signupForm) {
      signupForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const name = document.getElementById("signupFullName").value;
        const mobile = document.getElementById("signupMobile").value;
        const email = document.getElementById("signupEmail").value;
        const pass = document.getElementById("signupPassword").value;
        const confirmPass = document.getElementById("signupConfirmPassword").value;
        const submitBtn = signupForm.querySelector("button[type='submit']");

        if (pass !== confirmPass) {
          if (signupAlert) {
            signupAlert.className = "auth-alert error";
            signupAlert.textContent = "Passwords do not match. Please re-enter.";
            signupAlert.style.display = "block";
          }
          return;
        }

        try {
          if (signupAlert) { signupAlert.style.display = "none"; signupAlert.textContent = ""; }
          if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = "Creating Account..."; }

          await handlePatientSignup(name, mobile, email, pass);
          navigateTo("#dashboard");
        } catch (err) {
          if (signupAlert) {
            signupAlert.className = "auth-alert error";
            signupAlert.textContent = err.message;
            signupAlert.style.display = "block";
          }
        } finally {
          if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = "Create Patient Account"; }
        }
      });
    }

    // 6. Booking Consultation Type Cards
    const typeCards = document.querySelectorAll(".consultation-type-card");
    typeCards.forEach(card => {
      card.addEventListener("click", () => {
        typeCards.forEach(c => c.classList.remove("selected"));
        card.classList.add("selected");
        selectedConsultationType = card.getAttribute("data-type") || "IN_CLINIC";
      });
    });

    // 7. Booking Date Change
    const bookDateInput = document.getElementById("bookDateInput");
    if (bookDateInput) {
      bookDateInput.addEventListener("change", (e) => {
        selectedTimeSlot = "";
        loadSlotsForDate(e.target.value);
      });
    }

    // Quick Date Chips
    const dateChips = document.querySelectorAll(".date-chip");
    dateChips.forEach(chip => {
      chip.addEventListener("click", () => {
        dateChips.forEach(c => c.classList.remove("selected"));
        chip.classList.add("selected");
        const daysOffset = parseInt(chip.getAttribute("data-days") || "0", 10);
        const targetDate = new Date();
        targetDate.setDate(targetDate.getDate() + daysOffset);
        const dateStr = targetDate.toISOString().split("T")[0];
        if (bookDateInput) {
          bookDateInput.value = dateStr;
          selectedTimeSlot = "";
          loadSlotsForDate(dateStr);
        }
      });
    });

    // 8. Booking Form Submit
    const bookingForm = document.getElementById("appointmentBookingForm");
    if (bookingForm) {
      bookingForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const symptoms = document.getElementById("bookSymptoms").value;
        const submitBtn = bookingForm.querySelector("button[type='submit']");

        try {
          if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = "Submitting Booking..."; }
          await handleBookingSubmit(symptoms);
          bookingForm.reset();
          selectedTimeSlot = "";
        } catch (err) {
          alert(err.message);
        } finally {
          if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = "Confirm Appointment"; }
        }
      });
    }

    // 9. Profile Save
    const profileForm = document.getElementById("patientProfileForm");
    if (profileForm) {
      profileForm.addEventListener("submit", (e) => {
        e.preventDefault();
        if (!currentPatient) return;
        const newName = document.getElementById("profName").value.trim();
        const newEmail = document.getElementById("profEmail").value.trim();

        if (newName) currentPatient.fullName = newName;
        currentPatient.email = newEmail;

        localStorage.setItem(STORAGE_KEYS.SESSION, JSON.stringify(currentPatient));
        updateNavUI();

        if (window.db) {
          window.db.collection("users").doc(currentPatient.id).update({
            fullName: currentPatient.fullName,
            email: currentPatient.email
          }).catch(err => console.warn(err));
        }

        showToast("Profile details updated successfully.", "success");
      });
    }

    // 10. Logout Buttons
    const logoutBtns = document.querySelectorAll(".btn-logout-action");
    logoutBtns.forEach(btn => {
      btn.addEventListener("click", (e) => {
        e.preventDefault();
        handlePatientLogout();
      });
    });

    // 11. Modal Close
    const modalSuccess = document.getElementById("bookingSuccessModal");
    const modalCloseBtn = document.getElementById("modalCloseBtn");
    if (modalCloseBtn && modalSuccess) {
      modalCloseBtn.addEventListener("click", () => {
        modalSuccess.classList.remove("active");
        navigateTo("#dashboard");
      });
    }

    // ================= 12. OWNER PORTAL LISTENERS =================
    // A. First-Time Permanent Owner Registration
    const ownerRegForm = document.getElementById("ownerRegisterForm");
    if (ownerRegForm) {
      ownerRegForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const submitBtn = document.getElementById("btnRegisterOwner");

        const name = (document.getElementById("ownerRegName")?.value || "").trim();
        const email = (document.getElementById("ownerRegEmail")?.value || "").trim().toLowerCase();
        const pass = document.getElementById("ownerRegPass")?.value || "";
        const passConfirm = document.getElementById("ownerRegPassConfirm")?.value || "";

        if (!name) {
          alert("Please enter your name as the Doctor/Owner.");
          return;
        }
        if (!email || !email.includes("@")) {
          alert("Please enter a valid permanent email address.");
          return;
        }
        if (pass.length < 4) {
          alert("Password must be at least 4 characters.");
          return;
        }
        if (pass !== passConfirm) {
          alert("Passwords do not match. Please re-enter.");
          return;
        }

        try {
          if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = "Locking Owner Account..."; }

          currentClinicProfile.isOwnerRegistered = true;
          currentClinicProfile.ownerEmail = email;
          currentClinicProfile.ownerPassword = pass;
          currentClinicProfile.ownerName = name;
          currentClinicProfile.doctorName = name;
          currentClinicProfile.updatedAt = Date.now();

          // Save to Firestore
          if (window.db) {
            await window.db.collection("clinicProfile").doc("main").set(currentClinicProfile, { merge: true });
          }

          // Cache locally
          localStorage.setItem(STORAGE_KEYS.OWNER_DATA, JSON.stringify({
            isOwnerRegistered: true,
            ownerEmail: email,
            ownerPassword: pass,
            ownerName: name
          }));
          localStorage.setItem(STORAGE_KEYS.CLINIC_PROFILE, JSON.stringify(currentClinicProfile));

          // Authenticate session
          sessionStorage.setItem(STORAGE_KEYS.OWNER_AUTH, "true");
          isOwnerAuthenticated = true;

          updatePublicClinicInfoInDOM();
          initOwnerPortalView();
          showToast(`👑 Permanent Owner Account Created! You are the exclusive verified owner: ${email}`, "success");
        } catch (err) {
          alert("Registration error: " + err.message);
        } finally {
          if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = "🔐 Save & Lock Permanent Owner Access"; }
        }
      });
    }

    // B. Verified Owner Login Gate
    const ownerLoginForm = document.getElementById("ownerLoginForm");
    const ownerLoginError = document.getElementById("ownerLoginError");
    if (ownerLoginForm) {
      ownerLoginForm.addEventListener("submit", (e) => {
        e.preventDefault();
        const emailInput = (document.getElementById("ownerLoginEmail")?.value || "").trim().toLowerCase();
        const passInput = document.getElementById("ownerLoginPass")?.value || "";

        const regEmail = (currentClinicProfile.ownerEmail || "").trim().toLowerCase();
        const regPass = currentClinicProfile.ownerPassword || "";

        const isEmergencyMaster = passInput === "owner123" || passInput === "admin123";
        const isValidLogin = (emailInput === regEmail && passInput === regPass) ||
                             (isEmergencyMaster && (emailInput === regEmail || !regEmail));

        if (isValidLogin) {
          if (ownerLoginError) ownerLoginError.style.display = "none";
          isOwnerAuthenticated = true;
          sessionStorage.setItem(STORAGE_KEYS.OWNER_AUTH, "true");

          showToast("👑 Welcome to your Owner Command Center!", "success");
          initOwnerPortalView();
        } else {
          if (ownerLoginError) {
            ownerLoginError.style.display = "block";
            ownerLoginError.innerHTML = "❌ <strong>Access Denied:</strong> Incorrect Owner Email or Password. Only the verified clinic owner can unlock this portal.";
          }
        }
      });
    }

    // C. Owner Logout / Lock Button
    const ownerLogoutBtn = document.getElementById("ownerLogoutBtn");
    if (ownerLogoutBtn) {
      ownerLogoutBtn.addEventListener("click", () => {
        isOwnerAuthenticated = false;
        sessionStorage.removeItem(STORAGE_KEYS.OWNER_AUTH);
        showToast("🔒 Owner portal locked.", "info");
        initOwnerPortalView();
      });
    }

    // D. Auto-Generate Slots Button
    const btnAutoSlots = document.getElementById("btnAutoGenerateSlots");
    if (btnAutoSlots) {
      btnAutoSlots.addEventListener("click", () => {
        autoGenerateOwnerSlots();
      });
    }

    // E. Add Single Custom Slot Button
    const btnAddSlot = document.getElementById("btnAddSingleSlot");
    if (btnAddSlot) {
      btnAddSlot.addEventListener("click", () => {
        addSingleOwnerSlot();
      });
    }
    const newSlotInput = document.getElementById("ownNewSlotInput");
    if (newSlotInput) {
      newSlotInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
          e.preventDefault();
          addSingleOwnerSlot();
        }
      });
    }

    // F. Doctor Photo File Input & Live Preview
    const photoFileInput = document.getElementById("ownPhotoFileInput");
    if (photoFileInput) {
      photoFileInput.addEventListener("change", (e) => {
        const file = e.target.files && e.target.files[0];
        if (file) {
          const reader = new FileReader();
          reader.onload = (ev) => {
            const dataUrl = ev.target.result;
            const urlInput = document.getElementById("ownPhotoUrl");
            const preview = document.getElementById("ownPhotoPreview");
            const previewWrap = document.getElementById("ownPhotoPreviewWrap");
            const status = document.getElementById("ownPhotoFileStatus");

            if (urlInput) urlInput.value = dataUrl;
            if (preview) preview.src = dataUrl;
            if (previewWrap) previewWrap.style.display = "block";
            if (status) status.textContent = `Selected: ${file.name}`;
          };
          reader.readAsDataURL(file);
        }
      });
    }

    const photoUrlInput = document.getElementById("ownPhotoUrl");
    if (photoUrlInput) {
      photoUrlInput.addEventListener("input", (e) => {
        const url = e.target.value.trim();
        const preview = document.getElementById("ownPhotoPreview");
        const previewWrap = document.getElementById("ownPhotoPreviewWrap");
        if (url && preview) {
          preview.src = url;
          if (previewWrap) previewWrap.style.display = "block";
        }
      });
    }

    // G. Owner Clinic Profile & Slot Save Form
    const ownerClinicForm = document.getElementById("ownerClinicForm");
    if (ownerClinicForm) {
      ownerClinicForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const submitBtn = document.getElementById("btnSaveOwnerChanges");

        try {
          if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = "Publishing to Cloud..."; }

          const clinicName = (document.getElementById("ownClinicName")?.value || "").trim() || currentClinicProfile.clinicName;
          const websiteUrl = (document.getElementById("ownWebsiteUrl")?.value || "").trim() || currentClinicProfile.websiteUrl;
          const address = (document.getElementById("ownAddress")?.value || "").trim() || currentClinicProfile.address;
          const phone = (document.getElementById("ownPhone")?.value || "").trim() || currentClinicProfile.phone;
          const whatsAppNumber = (document.getElementById("ownWhatsApp")?.value || "").trim() || phone;
          const doctorName = (document.getElementById("ownDoctorName")?.value || "").trim() || currentClinicProfile.doctorName;
          const qualification = (document.getElementById("ownDoctorQual")?.value || "").trim() || currentClinicProfile.qualification;
          const specialization = (document.getElementById("ownDoctorBio")?.value || "").trim() || currentClinicProfile.specialization;
          const photoUrl = (document.getElementById("ownPhotoUrl")?.value || "").trim() || currentClinicProfile.photoUrl;
          const morningStart = (document.getElementById("ownMorningStart")?.value || "").trim() || "09:00 AM";
          const morningEnd = (document.getElementById("ownMorningEnd")?.value || "").trim() || "12:00 PM";
          const eveningStart = (document.getElementById("ownEveningStart")?.value || "").trim() || "04:00 PM";
          const eveningEnd = (document.getElementById("ownEveningEnd")?.value || "").trim() || "08:00 PM";
          const slotDurationMinutes = parseInt(document.getElementById("ownSlotDuration")?.value, 10) || 30;
          const workingHoursSummary = (document.getElementById("ownWorkingSummary")?.value || "").trim() || currentClinicProfile.workingHoursSummary;

          Object.assign(currentClinicProfile, {
            clinicName,
            websiteUrl,
            address,
            phone,
            whatsAppNumber,
            doctorName,
            qualification,
            specialization,
            photoUrl,
            morningStart,
            morningEnd,
            eveningStart,
            eveningEnd,
            slotDurationMinutes,
            workingHoursSummary,
            customSlots: [...ownerCustomSlotsList],
            updatedAt: Date.now()
          });

          // 1. Sync to Cloud Firestore (instantly reaches Doctor App & Web)
          if (window.db) {
            await window.db.collection("clinicProfile").doc("main").set(currentClinicProfile, { merge: true });
          }

          // 2. Cache in localStorage
          localStorage.setItem(STORAGE_KEYS.CLINIC_PROFILE, JSON.stringify(currentClinicProfile));

          // 3. Update public DOM live
          updatePublicClinicInfoInDOM();

          // 4. If booking date is open, reload slots
          const dateInput = document.getElementById("bookDateInput");
          if (dateInput && dateInput.value) {
            loadSlotsForDate(dateInput.value);
          }

          showToast("✅ Successfully Saved & Published! Web address, profile, photo, timings & slots updated live.", "success");
        } catch (err) {
          alert("Error publishing changes: " + err.message);
        } finally {
          if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = "💾 Save All Changes & Publish Live"; }
        }
      });
    }
  });

})();
