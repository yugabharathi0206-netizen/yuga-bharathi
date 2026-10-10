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
    LOCAL_APPTS: "homeo_patient_appts_cache"
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

    // Render slots
    slotsGrid.innerHTML = DEFAULT_SLOTS.map(slot => {
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
      "#profile": "viewProfile"
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

  // ================= EVENT LISTENERS & SETUP =================
  document.addEventListener("DOMContentLoaded", () => {
    // 1. Initialize Firebase
    if (window.initFirebase) {
      window.initFirebase();
    }

    // 2. Initialize Session
    initAuthSession();

    // 3. Setup Hash Route Listener
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
  });

})();
