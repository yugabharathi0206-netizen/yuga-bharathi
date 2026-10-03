package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ClinicRepository
import com.example.localization.AppLanguage
import com.example.localization.StringsLocalization
import com.example.util.ContactUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ClinicRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = database.clinicDao()
        repository = ClinicRepository(dao)

        runBlocking {
            AppDatabase.populateInitialData(dao)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("HOMEo AI", appName)
    }

    @Test
    fun testMultilingualLocalization() {
        // English
        assertEquals("Book Appointment", StringsLocalization.get("book_appointment", AppLanguage.ENGLISH))
        // Tamil
        assertEquals("முன்பதிவு செய்யவும்", StringsLocalization.get("book_appointment", AppLanguage.TAMIL))
        // Tanglish
        assertEquals("Appointment Book Pannu", StringsLocalization.get("book_appointment", AppLanguage.TANGLISH))

        // Confirm booking strings
        assertEquals("CONFIRM BOOKING", StringsLocalization.get("confirm_booking_button", AppLanguage.ENGLISH))
        assertEquals("முன்பதிவை உறுதி செய்", StringsLocalization.get("confirm_booking_button", AppLanguage.TAMIL))
        assertEquals("CONFIRM BOOKING", StringsLocalization.get("confirm_booking_button", AppLanguage.TANGLISH))
    }

    @Test
    fun testCleanDatabaseHasNoDemoData() = runBlocking {
        // Requirement: No random doctor, no fake patient, no fake appointments, no demo admin/owner
        assertFalse("No owner should exist initially", repository.hasOwner())
        val doctors = repository.allDoctors.first()
        assertTrue("Doctors list must start clean and empty", doctors.isEmpty())
        val appointments = repository.allAppointments.first()
        assertTrue("Appointments must start clean and empty", appointments.isEmpty())
        val patients = repository.allPatients.first()
        assertTrue("Patients must start clean and empty", patients.isEmpty())
        val admins = repository.allAdmins.first()
        assertTrue("Admins list must start empty", admins.isEmpty())

        // Requirement: Central Clinic Phone must be empty initially, never hardcoded
        val clinic = repository.clinic.first()
        assertNotNull(clinic)
        assertTrue("Clinic phone must be empty by default (no hardcoded demo numbers)", clinic!!.phone.isBlank())
        assertTrue("Clinic WhatsApp must be empty by default", clinic.whatsAppNumber.isBlank())
    }

    @Test
    fun testOwnerSetupAndDoctorProfileCreation() = runBlocking {
        // 1. Owner sets up private master account
        val ownerResult = repository.setupOwnerAccount(
            fullName = "Dr. S. Yugabharathi",
            email = "owner@homeoai.clinic",
            phone = "+91 98401 99999",
            password = "MasterOwnerPass@2026"
        )
        assertTrue("Owner setup should succeed", ownerResult.isSuccess)
        val owner = ownerResult.getOrThrow()
        assertEquals(UserRole.OWNER, owner.role)
        assertTrue(owner.isApproved)
        assertTrue(owner.canManageDoctors)
        assertTrue(owner.canManageAdmins)
        assertTrue(repository.hasOwner())

        // 2. Owner logs in with their private credentials
        val loginResult = repository.login("owner@homeoai.clinic", "MasterOwnerPass@2026")
        assertTrue("Owner login should succeed", loginResult.isSuccess)
        assertEquals(UserRole.OWNER, loginResult.getOrThrow().role)

        // 3. Owner creates their real Doctor profile with all requested fields
        val doctorProfile = DoctorEntity(
            id = "doc_owner_primary",
            name = "Dr. S. Yugabharathi",
            qualification = "BHMS, MD (Homeopathy)",
            specialization = "Classical Constitutional Homeopathy & Chronic Care",
            experienceYears = 14,
            registrationNumber = "TN-HOM-88741",
            bio = "Dedicated Classical Homeopath treating deep-seated chronic, autoimmune, and pediatric disorders.",
            consultationFee = 600.0,
            morningStart = "09:30",
            morningEnd = "13:00",
            eveningStart = "16:30",
            eveningEnd = "20:30",
            availableDays = "Mon,Tue,Wed,Thu,Fri,Sat",
            slotDurationMinutes = 30,
            phone = "+91 98401 99999",
            whatsAppNumber = "+91 98401 99999",
            clinicName = "HOMEo AI Classical Clinic",
            clinicAddress = "12 Main Boulevard, Anna Nagar, Chennai - 600040",
            languages = "Tamil, Tanglish, English",
            services = "Constitutional Case Taking, Pediatric Homeopathy, Chronic Allergy Relief, Skin Disorders",
            socialMediaLinks = "https://instagram.com/homeoai.clinic",
            photoUrl = ""
        )
        val saveDoctorResult = repository.saveOwnerDoctorProfile(doctorProfile, owner.id)
        assertTrue("Doctor profile save should succeed", saveDoctorResult.isSuccess)

        // 4. Verify doctor is now visible in clinic
        val doctorsList = repository.allDoctors.first()
        assertEquals(1, doctorsList.size)
        val savedDoc = doctorsList.first()
        assertEquals("Dr. S. Yugabharathi", savedDoc.name)
        assertEquals("Tamil, Tanglish, English", savedDoc.languages)
        assertEquals(600.0, savedDoc.consultationFee, 0.01)
    }

    @Test
    fun testAdminInvitationApprovalAndSecurityEnforcement() = runBlocking {
        // Setup Owner
        val owner = repository.setupOwnerAccount(
            fullName = "Clinic Master Owner",
            email = "owner@clinic.com",
            phone = "+91 90000 11111",
            password = "ownerSecurePass"
        ).getOrThrow()

        // 1. Owner requests to add an Admin
        val adminRequest = repository.addAdminRequest(
            fullName = "Assistant Manager Priya",
            email = "priya@clinic.com",
            phone = "+91 98888 22222",
            password = "adminInitialPassword",
            canManageAppointments = true,
            canManagePatients = true,
            canManageQueue = true,
            canManageMedicines = false,
            canManageBilling = false,
            canViewReports = false,
            canManageSettings = false,
            ownerId = owner.id
        )
        assertTrue("Admin request creation should succeed", adminRequest.isSuccess)
        val pendingAdmin = adminRequest.getOrThrow()
        assertEquals(UserRole.ADMIN, pendingAdmin.role)
        assertFalse("New admin must NOT be approved automatically", pendingAdmin.isApproved)
        assertEquals("PENDING", pendingAdmin.approvalStatus)

        // 2. Unapproved admin attempts login -> MUST FAIL
        val earlyLogin = repository.login("priya@clinic.com", "adminInitialPassword")
        assertFalse("Unapproved admin login must be rejected", earlyLogin.isSuccess)
        assertTrue(earlyLogin.exceptionOrNull()?.message?.contains("pending approval") == true)

        // 3. Owner approves admin access
        val approveResult = repository.approveAdmin(pendingAdmin.id, owner.id)
        assertTrue("Owner approval should succeed", approveResult.isSuccess)

        // 4. Approved admin now logs in successfully
        val approvedLogin = repository.login("priya@clinic.com", "adminInitialPassword")
        assertTrue("Approved admin should log in", approvedLogin.isSuccess)
        val activeAdmin = approvedLogin.getOrThrow()
        assertEquals(UserRole.ADMIN, activeAdmin.role)
        assertTrue(activeAdmin.isApproved)
        assertTrue(activeAdmin.canManageAppointments)
        assertFalse(activeAdmin.canManageMedicines)

        // 5. Security: Admin cannot remove the Owner
        val adminTriesRemoveOwner = repository.deleteAdmin(owner.id, activeAdmin.id)
        assertFalse("Admin cannot remove Owner account", adminTriesRemoveOwner.isSuccess)

        // 6. Security: Admin cannot alter Owner permissions
        val adminTriesEditOwner = repository.updateAdminPermissions(
            adminId = owner.id,
            canManageAppointments = false,
            canManagePatients = false,
            canManageQueue = false,
            canManageMedicines = false,
            canManageBilling = false,
            canViewReports = false,
            canManageSettings = false,
            ownerId = activeAdmin.id
        )
        assertFalse("Owner permissions cannot be altered", adminTriesEditOwner.isSuccess)

        // 7. Owner toggles admin active state (disable)
        repository.toggleAdminActive(activeAdmin.id, false, owner.id)
        val disabledLogin = repository.login("priya@clinic.com", "adminInitialPassword")
        assertFalse("Disabled admin login must be blocked", disabledLogin.isSuccess)
        assertTrue(disabledLogin.exceptionOrNull()?.message?.contains("disabled") == true)

        // 8. Owner permanently removes admin
        val deleteAdminResult = repository.deleteAdmin(activeAdmin.id, owner.id)
        assertTrue("Owner can remove admin", deleteAdminResult.isSuccess)
        val finalAdmins = repository.allAdmins.first()
        assertTrue("Admin list should be empty after removal", finalAdmins.isEmpty())
    }

    @Test
    fun testPatientBookingWorkflowWithRealDoctor() = runBlocking {
        // 1. Owner sets up doctor
        val owner = repository.setupOwnerAccount(
            fullName = "Dr. Owner",
            email = "dr.owner@clinic.com",
            phone = "+91 97777 66666",
            password = "drOwnerPass123"
        ).getOrThrow()

        val doctor = repository.saveOwnerDoctorProfile(
            DoctorEntity(
                id = "doc_main",
                name = "Dr. Owner",
                qualification = "BHMS",
                specialization = "Classical Homeopathy",
                experienceYears = 10,
                registrationNumber = "REG-5555",
                bio = "Experienced doctor",
                consultationFee = 500.0,
                morningStart = "10:00",
                morningEnd = "13:00",
                eveningStart = "17:00",
                eveningEnd = "20:00",
                availableDays = "Mon,Tue,Wed,Thu,Fri,Sat",
                slotDurationMinutes = 30,
                phone = "+91 97777 66666",
                whatsAppNumber = "+91 97777 66666",
                clinicName = "HOMEo AI",
                clinicAddress = "Chennai",
                languages = "Tamil, English",
                services = "Allergy, Migraine",
                socialMediaLinks = "",
                photoUrl = ""
            ),
            owner.id
        ).getOrThrow()

        // 2. Patient registers
        val patientUser = repository.registerClient(
            fullName = "Ananya Sundaram",
            phone = "+91 94444 33333",
            email = "ananya@gmail.com",
            passwordOrOtp = "patientSecretPass",
            dob = "1996-05-12",
            gender = "Female",
            address = "T. Nagar, Chennai"
        ).getOrThrow()

        assertEquals(UserRole.CLIENT, patientUser.role)

        // 3. Patient books appointment
        val booking = repository.bookAppointment(
            clientId = patientUser.id,
            doctorId = doctor.id,
            doctorName = doctor.name,
            patientName = "Ananya Sundaram",
            patientPhone = patientUser.phone,
            patientEmail = patientUser.email,
            patientAge = 30,
            patientGender = "Female",
            reason = "Chronic Skin Allergy",
            symptomsDescription = "Red patches on arms for 2 months",
            uploadedDocumentUrl = "",
            date = "2026-10-15",
            timeSlot = "10:30 AM",
            type = AppointmentType.IN_CLINIC,
            consultationFee = 500.0,
            paymentMethod = PaymentMethod.PAY_AT_CLINIC
        ).getOrThrow()

        assertEquals(AppointmentStatus.PENDING, booking.status)

        // 4. Admin confirms appointment
        repository.confirmAppointmentByAdmin(booking.id, "Please come 10 minutes early")

        val appointments = repository.allAppointments.first()
        val confirmedApt = appointments.find { it.id == booking.id }
        assertNotNull(confirmedApt)
        assertEquals(AppointmentStatus.CONFIRMED, confirmedApt!!.status)

        // 5. Patient data isolation
        val clientNotifs = repository.getClientNotifications(patientUser.id, patientUser.phone).first()
        assertTrue("Patient should receive confirmation notification", clientNotifs.any { it.appointmentId == booking.id })

        val clientAppointments = repository.getClientAppointments(patientUser.id, patientUser.phone).first()
        assertEquals(1, clientAppointments.size)
        assertEquals(booking.id, clientAppointments.first().id)

        // Another patient should see 0 appointments
        val otherClientApts = repository.getClientAppointments("usr_other", "+91 91111 22222").first()
        assertTrue("Other client should see 0 appointments", otherClientApts.isEmpty())
    }

    @Test
    fun testCentralClinicPhoneManagementAndDynamicUpdate() = runBlocking {
        // 1. Initial state: phone is empty
        val initialClinic = repository.clinic.first()
        assertNotNull(initialClinic)
        assertEquals("", initialClinic!!.phone)

        // 2. Owner configures Clinic Profile in Admin Settings
        val configured = initialClinic.copy(
            name = "My Classical Homeopathy Clinic",
            doctorName = "Dr. S. Yugabharathi",
            phone = "+91 98888 12345",
            whatsAppNumber = "+91 98888 54321",
            email = "care@myclinic.com",
            address = "77 Grand Avenue, Chennai",
            openingTime = "08:30 AM",
            closingTime = "09:00 PM",
            showPhoneNumberOnPublicPage = true
        )
        repository.saveClinic(configured)

        // 3. Dynamic emission check via Room Flow
        val savedClinic = repository.clinic.first()
        assertNotNull(savedClinic)
        assertEquals("+91 98888 12345", savedClinic!!.phone)
        assertEquals("+91 98888 54321", savedClinic.whatsAppNumber)
        assertEquals("Dr. S. Yugabharathi", savedClinic.doctorName)
        assertEquals("77 Grand Avenue, Chennai", savedClinic.address)
        assertEquals("08:30 AM", savedClinic.openingTime)
        assertEquals("09:00 PM", savedClinic.closingTime)
        assertTrue(savedClinic.showPhoneNumberOnPublicPage)

        // 4. Dynamic change: Owner updates phone number again
        val updatedAgain = savedClinic.copy(phone = "+91 97777 00000")
        repository.saveClinic(updatedAgain)

        val latestClinic = repository.clinic.first()
        assertNotNull(latestClinic)
        assertEquals("+91 97777 00000", latestClinic!!.phone)
    }

    @Test
    fun testContactUtilsSafetyWhenUnconfigured() {
        // When unconfigured, helper must safely notify without crashes or invalid numbers
        ContactUtils.callClinic(context, "")
        ContactUtils.callClinic(context, null)
        ContactUtils.openWhatsApp(context, "")
        ContactUtils.openWhatsApp(context, null)
        ContactUtils.sendEmail(context, "")
        ContactUtils.sendEmail(context, null)
        ContactUtils.openDirections(context, "")
        ContactUtils.openDirections(context, null)
    }
}
