package com.example.data.repository

import com.example.data.dao.ClinicDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SlotInfo(
    val timeSlot: String,
    val isAvailable: Boolean
)

data class LiveQueueStatus(
    val yourToken: Int?,
    val nowServing: Int?,
    val patientsAhead: Int,
    val estimatedWaitMinutes: Int?,
    val currentStatus: QueueStatus?
)

class ClinicRepository(private val dao: ClinicDao) {

    // Authentication Session State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser = _currentUser.asStateFlow()

    // Public / Shared Flows
    val clinic: Flow<ClinicEntity?> = dao.getClinic()
    val allDoctors: Flow<List<DoctorEntity>> = dao.getAllDoctors()
    val allServices: Flow<List<ServiceEntity>> = dao.getAllServices()
    val allPatients: Flow<List<PatientEntity>> = dao.getAllPatients()
    val allMedicines: Flow<List<MedicineEntity>> = dao.getAllMedicines()
    val allMessages: Flow<List<MessageEntity>> = dao.getAllMessages()

    // Admin-Only Flows
    val allAppointments: Flow<List<AppointmentEntity>> = dao.getAllAppointments()
    val adminNotifications: Flow<List<NotificationEntity>> = dao.getNotificationsForAdmin()
    val allPayments: Flow<List<PaymentEntity>> = dao.getAllPayments()
    val allFollowUps: Flow<List<FollowUpEntity>> = dao.getAllFollowUps()
    val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()
    val allAdmins: Flow<List<UserEntity>> = dao.getAllAdmins()

    // Owner Account System
    suspend fun hasOwner(): Boolean = dao.countOwners() > 0

    suspend fun setupOwnerAccount(
        fullName: String,
        email: String,
        phone: String,
        password: String
    ): Result<UserEntity> {
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        val cleanName = fullName.trim()

        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your name."))
        }
        if (cleanEmail.isBlank() && cleanPhone.isBlank()) {
            return Result.failure(IllegalArgumentException("Please provide an email or mobile phone number."))
        }
        if (password.length < 4) {
            return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
        }

        val existing = dao.getUserByEmailOrPhone(cleanEmail.ifBlank { cleanPhone })
        if (existing != null) {
            return Result.failure(IllegalArgumentException("An account already exists with this contact information."))
        }

        val ownerId = "usr_owner_main"
        val ownerUser = UserEntity(
            id = ownerId,
            email = cleanEmail,
            phone = cleanPhone,
            passwordHash = password,
            fullName = cleanName,
            role = UserRole.OWNER,
            isApproved = true,
            approvalStatus = "APPROVED",
            canManageDoctors = true,
            canManageAdmins = true,
            canManageAppointments = true,
            canManagePatients = true,
            canManageQueue = true,
            canManageMedicines = true,
            canManageBilling = true,
            canViewReports = true,
            canManageSettings = true,
            isActive = true
        )
        dao.insertUser(ownerUser)
        _currentUser.value = ownerUser

        dao.insertAuditLog(
            AuditLogEntity(
                adminId = ownerId,
                action = "OWNER_ACCOUNT_INITIALIZED",
                affectedRecordId = ownerId,
                details = "Clinic Owner account established by $cleanName."
            )
        )
        return Result.success(ownerUser)
    }

    // Admin Access Management (Only by Owner)
    suspend fun addAdminRequest(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        canManageAppointments: Boolean = true,
        canManagePatients: Boolean = true,
        canManageQueue: Boolean = true,
        canManageMedicines: Boolean = true,
        canManageBilling: Boolean = true,
        canViewReports: Boolean = true,
        canManageSettings: Boolean = false,
        ownerId: String
    ): Result<UserEntity> {
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()
        val cleanName = fullName.trim()

        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter the admin's full name."))
        }
        val existing = dao.getUserByEmailOrPhone(cleanEmail.ifBlank { cleanPhone })
        if (existing != null) {
            return Result.failure(IllegalArgumentException("An account already exists with this email or mobile number."))
        }

        val adminId = "usr_admin_" + (1000..9999).random()
        val newAdmin = UserEntity(
            id = adminId,
            email = cleanEmail,
            phone = cleanPhone,
            passwordHash = password.ifBlank { "admin123" },
            fullName = cleanName,
            role = UserRole.ADMIN,
            isApproved = false, // Must be approved by Owner!
            approvalStatus = "PENDING",
            canManageDoctors = false, // Owner only!
            canManageAdmins = false,  // Owner only!
            canManageAppointments = canManageAppointments,
            canManagePatients = canManagePatients,
            canManageQueue = canManageQueue,
            canManageMedicines = canManageMedicines,
            canManageBilling = canManageBilling,
            canViewReports = canViewReports,
            canManageSettings = canManageSettings,
            isActive = true,
            invitedBy = ownerId
        )
        dao.insertUser(newAdmin)
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = ownerId,
                action = "ADMIN_ACCESS_REQUESTED",
                affectedRecordId = adminId,
                details = "Admin access requested for $cleanName. Pending Owner approval."
            )
        )
        return Result.success(newAdmin)
    }

    suspend fun approveAdmin(adminId: String, ownerId: String): Result<Unit> {
        val admin = dao.getUserById(adminId) ?: return Result.failure(IllegalArgumentException("Admin not found"))
        val updated = admin.copy(
            isApproved = true,
            approvalStatus = "APPROVED"
        )
        dao.updateUser(updated)
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = ownerId,
                action = "ADMIN_APPROVED",
                affectedRecordId = adminId,
                details = "Owner approved admin access for ${admin.fullName}."
            )
        )
        return Result.success(Unit)
    }

    suspend fun rejectAdmin(adminId: String, ownerId: String): Result<Unit> {
        val admin = dao.getUserById(adminId) ?: return Result.failure(IllegalArgumentException("Admin not found"))
        val updated = admin.copy(
            isApproved = false,
            approvalStatus = "REJECTED"
        )
        dao.updateUser(updated)
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = ownerId,
                action = "ADMIN_REJECTED",
                affectedRecordId = adminId,
                details = "Owner rejected admin access for ${admin.fullName}."
            )
        )
        return Result.success(Unit)
    }

    suspend fun toggleAdminActive(adminId: String, isActive: Boolean, ownerId: String): Result<Unit> {
        val admin = dao.getUserById(adminId) ?: return Result.failure(IllegalArgumentException("Admin not found"))
        if (admin.role == UserRole.OWNER) {
            return Result.failure(IllegalStateException("Cannot disable the Clinic Owner account."))
        }
        val updated = admin.copy(isActive = isActive)
        dao.updateUser(updated)
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = ownerId,
                action = if (isActive) "ADMIN_ENABLED" else "ADMIN_DISABLED",
                affectedRecordId = adminId,
                details = "Owner set active state to $isActive for ${admin.fullName}."
            )
        )
        return Result.success(Unit)
    }

    suspend fun deleteAdmin(adminId: String, ownerId: String): Result<Unit> {
        val admin = dao.getUserById(adminId) ?: return Result.failure(IllegalArgumentException("Admin not found"))
        if (admin.role == UserRole.OWNER) {
            return Result.failure(IllegalStateException("Cannot remove the Clinic Owner account."))
        }
        dao.deleteUser(admin)
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = ownerId,
                action = "ADMIN_REMOVED",
                affectedRecordId = adminId,
                details = "Owner deleted admin account ${admin.fullName}."
            )
        )
        return Result.success(Unit)
    }

    suspend fun updateAdminPermissions(
        adminId: String,
        canManageAppointments: Boolean,
        canManagePatients: Boolean,
        canManageQueue: Boolean,
        canManageMedicines: Boolean,
        canManageBilling: Boolean,
        canViewReports: Boolean,
        canManageSettings: Boolean,
        ownerId: String
    ): Result<Unit> {
        val admin = dao.getUserById(adminId) ?: return Result.failure(IllegalArgumentException("Admin not found"))
        if (admin.role == UserRole.OWNER) {
            return Result.failure(IllegalStateException("Cannot modify Owner permissions."))
        }
        val updated = admin.copy(
            canManageAppointments = canManageAppointments,
            canManagePatients = canManagePatients,
            canManageQueue = canManageQueue,
            canManageMedicines = canManageMedicines,
            canManageBilling = canManageBilling,
            canViewReports = canViewReports,
            canManageSettings = canManageSettings
        )
        dao.updateUser(updated)
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = ownerId,
                action = "PERMISSIONS_UPDATED",
                affectedRecordId = adminId,
                details = "Owner updated permissions for ${admin.fullName}."
            )
        )
        return Result.success(Unit)
    }

    // Owner Doctor Profile Management
    suspend fun saveOwnerDoctorProfile(doctor: DoctorEntity, ownerId: String): Result<DoctorEntity> {
        dao.insertDoctor(doctor)
        // Sync clinic info with doctor profile
        val currentClinic = dao.getClinic()
        // Update audit log
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = ownerId,
                action = "DOCTOR_PROFILE_SAVED",
                affectedRecordId = doctor.id,
                details = "Doctor profile created/updated for ${doctor.name}."
            )
        )
        return Result.success(doctor)
    }

    suspend fun deleteDoctorProfile(doctorId: String, ownerId: String): Result<Unit> {
        dao.deleteDoctorById(doctorId)
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = ownerId,
                action = "DOCTOR_PROFILE_DELETED",
                affectedRecordId = doctorId,
                details = "Doctor profile deleted with id $doctorId."
            )
        )
        return Result.success(Unit)
    }

    suspend fun getFirstDoctor(): DoctorEntity? = dao.getFirstDoctor()

    // Client-Specific Flows (Enforces strict access control)
    fun getClientAppointments(clientId: String, phone: String): Flow<List<AppointmentEntity>> {
        return dao.getAppointmentsForClient(clientId, phone)
    }

    fun getClientNotifications(clientId: String, phone: String): Flow<List<NotificationEntity>> {
        return dao.getNotificationsForClient(clientId, phone)
    }

    fun getClientPayments(clientId: String): Flow<List<PaymentEntity>> {
        return dao.getPaymentsForClient(clientId)
    }

    fun getClientFollowUps(patientId: String, phone: String): Flow<List<FollowUpEntity>> {
        return dao.getFollowUpsForPatient(patientId, phone)
    }

    fun getUserSessions(userId: String): Flow<List<SessionEntity>> {
        return dao.getSessionsForUser(userId)
    }

    suspend fun deactivateAllSessions(userId: String) {
        dao.deactivateAllSessionsForUser(userId)
        // Record audit log
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = userId,
                action = "LOGOUT_ALL_DEVICES",
                affectedRecordId = userId,
                details = "User requested termination of all active sessions."
            )
        )
    }

    fun getAppointmentsForDate(date: String): Flow<List<AppointmentEntity>> =
        dao.getAppointmentsForDate(date)

    fun getQueueForDate(date: String): Flow<List<QueueEntryEntity>> =
        dao.getQueueForDate(date)

    // Authentication Operations
    suspend fun login(identifier: String, passwordOrOtp: String): Result<UserEntity> {
        val cleanIdentifier = identifier.trim()
        val user = dao.getUserByEmailOrPhone(cleanIdentifier)
            ?: return Result.failure(IllegalArgumentException("Account not found with this email or mobile number."))

        if (user.passwordHash == passwordOrOtp) {
            // Check admin status
            if (user.role == UserRole.ADMIN) {
                if (!user.isActive) {
                    return Result.failure(IllegalStateException("Your admin account has been disabled by the clinic Owner."))
                }
                if (!user.isApproved || user.approvalStatus != "APPROVED") {
                    return Result.failure(IllegalStateException("Your admin access request is pending approval from the clinic Owner."))
                }
            }

            _currentUser.value = user
            // Log session
            dao.insertSession(
                SessionEntity(
                    id = "sess_" + (1000..9999).random(),
                    userId = user.id,
                    deviceName = "Android Device (${user.role.name} Session)"
                )
            )
            dao.insertAuditLog(
                AuditLogEntity(
                    adminId = user.id,
                    action = "LOGIN_SUCCESS",
                    affectedRecordId = user.id,
                    details = "User ${user.fullName} (${user.role.name}) logged in successfully."
                )
            )
            return Result.success(user)
        }
        return Result.failure(IllegalArgumentException("Invalid password. Please check your credentials."))
    }

    suspend fun registerClient(
        fullName: String,
        phone: String,
        email: String,
        passwordOrOtp: String,
        dob: String,
        gender: String,
        address: String = "",
        emergencyContact: String = ""
    ): Result<UserEntity> {
        val existing = dao.getUserByEmailOrPhone(email.ifBlank { phone })
        if (existing != null) {
            return Result.failure(IllegalArgumentException("An account already exists with this phone or email."))
        }

        val userId = "usr_client_" + (1000..9999).random()
        val user = UserEntity(
            id = userId,
            email = email,
            phone = phone,
            passwordHash = passwordOrOtp.ifBlank { "1234" },
            fullName = fullName,
            role = UserRole.CLIENT,
            dateOfBirth = dob,
            gender = gender,
            address = address,
            emergencyContact = emergencyContact
        )
        dao.insertUser(user)

        val patient = PatientEntity(
            id = "pat_" + (1000..9999).random(),
            userId = userId,
            fullName = fullName,
            phone = phone,
            email = email,
            dateOfBirth = dob,
            gender = gender,
            address = address,
            emergencyContact = emergencyContact
        )
        dao.insertOrUpdatePatient(patient)

        _currentUser.value = user
        return Result.success(user)
    }

    fun logout() {
        _currentUser.value = null
    }

    // Direct access & profile updates
    suspend fun getDoctor(id: String): DoctorEntity? = dao.getDoctorById(id)
    suspend fun saveClinic(clinic: ClinicEntity) = dao.insertOrUpdateClinic(clinic)
    suspend fun saveDoctor(doctor: DoctorEntity) = dao.insertDoctor(doctor)
    suspend fun savePatient(patient: PatientEntity) = dao.insertOrUpdatePatient(patient)
    suspend fun updateUser(user: UserEntity) {
        dao.updateUser(user)
        if (_currentUser.value?.id == user.id) {
            _currentUser.value = user
        }
    }
    suspend fun saveService(service: ServiceEntity) = dao.insertService(service)
    suspend fun deleteService(service: ServiceEntity) = dao.deleteService(service)
    suspend fun updateMedicine(medicine: MedicineEntity) = dao.updateMedicine(medicine)
    suspend fun addMedicine(medicine: MedicineEntity) = dao.insertMedicine(medicine)

    // Dynamic Slot Generation
    suspend fun getAvailableSlots(doctor: DoctorEntity, dateStr: String): List<SlotInfo> {
        val booked = dao.getBookedSlotsForDoctorAndDate(doctor.id, dateStr).toSet()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = try { sdf.parse(dateStr) } catch (_: Exception) { Date() } ?: Date()
        val cal = Calendar.getInstance().apply { time = date }
        val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Sun"
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Mon"
        }

        if (!doctor.availableDays.contains(dayName, ignoreCase = true)) {
            return emptyList()
        }

        val step = doctor.slotDurationMinutes.coerceAtLeast(15)
        val allGeneratedSlots = mutableListOf<String>()

        generateSlotsForRange(doctor.morningStart, doctor.morningEnd, step, allGeneratedSlots)
        generateSlotsForRange(doctor.eveningStart, doctor.eveningEnd, step, allGeneratedSlots)

        return allGeneratedSlots.map { slot ->
            SlotInfo(
                timeSlot = slot,
                isAvailable = !booked.contains(slot)
            )
        }
    }

    private fun generateSlotsForRange(
        startTime: String,
        endTime: String,
        stepMinutes: Int,
        resultList: MutableList<String>
    ) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val displayFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        try {
            val start = timeFormat.parse(startTime) ?: return
            val end = timeFormat.parse(endTime) ?: return

            val calendar = Calendar.getInstance().apply { time = start }
            val endCalendar = Calendar.getInstance().apply { time = end }

            while (calendar.timeInMillis < endCalendar.timeInMillis) {
                resultList.add(displayFormat.format(calendar.time).uppercase(Locale.getDefault()))
                calendar.add(Calendar.MINUTE, stepMinutes)
            }
        } catch (_: Exception) {
            // fallback
        }
    }

    // Atomic Booking Flow
    suspend fun bookAppointment(
        clientId: String,
        doctorId: String,
        doctorName: String,
        patientName: String,
        patientPhone: String,
        patientEmail: String = "",
        patientAge: Int = 30,
        patientGender: String = "Male",
        reason: String,
        symptomsDescription: String = "",
        uploadedDocumentUrl: String = "",
        date: String,
        timeSlot: String,
        type: AppointmentType,
        consultationFee: Double,
        paymentMethod: PaymentMethod
    ): Result<AppointmentEntity> {
        val aptId = "HM-2026-" + (10000..99999).random()
        val meetingLink = if (type == AppointmentType.ONLINE) "https://meet.homeoai.clinic/${aptId.lowercase(Locale.ROOT)}" else ""
        val paymentStatus = if (paymentMethod == PaymentMethod.ONLINE) PaymentStatus.PAID else PaymentStatus.PENDING

        val appointment = AppointmentEntity(
            id = aptId,
            clientId = clientId.ifBlank { "usr_client_guest" },
            doctorId = doctorId,
            doctorName = doctorName,
            patientId = "pat_" + (1000..9999).random(),
            patientName = patientName,
            patientPhone = patientPhone,
            patientEmail = patientEmail,
            patientAge = patientAge,
            patientGender = patientGender,
            reasonForVisit = reason,
            symptomsDescription = symptomsDescription,
            uploadedDocumentUrl = uploadedDocumentUrl,
            appointmentDate = date,
            timeSlot = timeSlot,
            status = AppointmentStatus.PENDING,
            type = type,
            meetingLink = meetingLink,
            consultationFee = consultationFee,
            paymentStatus = paymentStatus,
            paymentMethod = paymentMethod
        )

        val success = dao.bookAppointmentAtomic(appointment)
        return if (success) {
            // 1. Notify Admin: "🔔 New appointment request from [Name]"
            dao.insertNotification(
                NotificationEntity(
                    recipientRole = UserRole.ADMIN,
                    recipientId = "ADMIN",
                    title = "🔔 New appointment request from $patientName.",
                    message = "$patientName requested an appointment for $date at $timeSlot (Reason: $reason • Booking ID: $aptId).",
                    type = "NEW_BOOKING",
                    appointmentId = aptId
                )
            )

            // 2. Notify Client
            dao.insertNotification(
                NotificationEntity(
                    recipientRole = UserRole.CLIENT,
                    recipientId = clientId,
                    recipientPhone = patientPhone,
                    title = "Appointment request submitted successfully.",
                    message = "Your appointment ($aptId) with $doctorName on $date at $timeSlot is pending doctor confirmation.",
                    type = "BOOKED",
                    appointmentId = aptId
                )
            )

            // 3. Record Pending Payment
            dao.insertPayment(
                PaymentEntity(
                    id = "PAY-" + (10000..99999).random(),
                    appointmentId = aptId,
                    clientId = clientId,
                    patientName = patientName,
                    totalFee = consultationFee,
                    paidAmount = if (paymentMethod == PaymentMethod.ONLINE) consultationFee else 0.0,
                    pendingAmount = if (paymentMethod == PaymentMethod.ONLINE) 0.0 else consultationFee,
                    status = if (paymentMethod == PaymentMethod.ONLINE) PaymentStatus.PAID else PaymentStatus.PENDING,
                    paymentDate = date,
                    paymentMethod = paymentMethod
                )
            )

            Result.success(appointment)
        } else {
            Result.failure(IllegalStateException("No available appointments for this date and time. Please select another slot."))
        }
    }

    // Admin Action: Confirm Appointment
    suspend fun confirmAppointmentByAdmin(appointmentId: String, adminNotes: String = "") {
        val apt = dao.getAppointmentById(appointmentId) ?: return
        val updated = apt.copy(
            status = AppointmentStatus.CONFIRMED,
            adminNotes = adminNotes.ifBlank { apt.adminNotes },
            updatedAt = System.currentTimeMillis()
        )
        dao.updateAppointment(updated)

        // Send Client Notification
        dao.insertNotification(
            NotificationEntity(
                recipientRole = UserRole.CLIENT,
                recipientId = apt.clientId,
                recipientPhone = apt.patientPhone,
                title = "✅ Your appointment has been confirmed.",
                message = "Your appointment ($appointmentId) with ${apt.doctorName} on ${apt.appointmentDate} at ${apt.timeSlot} is now confirmed.",
                type = "CONFIRMED",
                appointmentId = appointmentId
            )
        )

        // Audit Log
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = _currentUser.value?.id ?: "ADMIN",
                action = "APPOINTMENT_CONFIRMED",
                affectedRecordId = appointmentId,
                details = "Confirmed appointment for ${apt.patientName}."
            )
        )
    }

    // Admin Action: Cancel / Reject Appointment
    suspend fun cancelAppointmentByAdmin(appointmentId: String, reason: String = "") {
        val apt = dao.getAppointmentById(appointmentId) ?: return
        val updated = apt.copy(
            status = AppointmentStatus.CANCELLED,
            adminNotes = "Cancelled by clinic: $reason",
            updatedAt = System.currentTimeMillis()
        )
        dao.updateAppointment(updated)

        // Send Client Notification
        dao.insertNotification(
            NotificationEntity(
                recipientRole = UserRole.CLIENT,
                recipientId = apt.clientId,
                recipientPhone = apt.patientPhone,
                title = "❌ Your appointment has been cancelled.",
                message = "Your appointment ($appointmentId) on ${apt.appointmentDate} has been cancelled. Reason: $reason",
                type = "CANCELLED",
                appointmentId = appointmentId
            )
        )

        // Audit Log
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = _currentUser.value?.id ?: "ADMIN",
                action = "APPOINTMENT_CANCELLED",
                affectedRecordId = appointmentId,
                details = "Cancelled appointment. Reason: $reason"
            )
        )
    }

    suspend fun cancelAppointment(appointmentId: String): Boolean {
        cancelAppointmentByAdmin(appointmentId, "Cancelled upon client request")
        return true
    }

    suspend fun updateAppointmentStatus(appointmentId: String, newStatus: AppointmentStatus) {
        val apt = dao.getAppointmentById(appointmentId) ?: return
        dao.updateAppointment(apt.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
    }

    // Admin / Client Action: Reschedule Appointment
    suspend fun rescheduleAppointment(
        oldAppointmentId: String,
        newDoctorId: String,
        newDate: String,
        newSlot: String,
        notes: String = ""
    ): Result<Boolean> {
        val success = dao.rescheduleAppointmentAtomic(oldAppointmentId, newDoctorId, newDate, newSlot)
        if (success) {
            val oldApt = dao.getAppointmentById(oldAppointmentId)
            if (oldApt != null) {
                // Client Notification
                dao.insertNotification(
                    NotificationEntity(
                        recipientRole = UserRole.CLIENT,
                        recipientId = oldApt.clientId,
                        recipientPhone = oldApt.patientPhone,
                        title = "📅 Your appointment has been rescheduled.",
                        message = "Your appointment has been moved to $newDate at $newSlot.",
                        type = "RESCHEDULED",
                        appointmentId = oldAppointmentId
                    )
                )

                // Audit Log
                dao.insertAuditLog(
                    AuditLogEntity(
                        adminId = _currentUser.value?.id ?: "ADMIN",
                        action = "APPOINTMENT_RESCHEDULED",
                        affectedRecordId = oldAppointmentId,
                        details = "Rescheduled to $newDate at $newSlot."
                    )
                )
            }
            return Result.success(true)
        }
        return Result.failure(IllegalStateException("Selected slot is no longer available."))
    }

    // Admin Action: Mark Completed
    suspend fun markAppointmentCompleted(appointmentId: String, notes: String = "") {
        val apt = dao.getAppointmentById(appointmentId) ?: return
        dao.updateAppointment(
            apt.copy(
                status = AppointmentStatus.COMPLETED,
                adminNotes = notes.ifBlank { apt.adminNotes },
                updatedAt = System.currentTimeMillis()
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = _currentUser.value?.id ?: "ADMIN",
                action = "APPOINTMENT_COMPLETED",
                affectedRecordId = appointmentId,
                details = "Consultation completed for ${apt.patientName}."
            )
        )
    }

    // Patient Check-In
    suspend fun checkInPatient(appointmentId: String, todayDate: String): Int {
        val token = dao.checkInPatientAtomic(appointmentId, todayDate)
        if (token > 0) {
            val apt = dao.getAppointmentById(appointmentId)
            if (apt != null) {
                dao.insertNotification(
                    NotificationEntity(
                        recipientRole = UserRole.CLIENT,
                        recipientId = apt.clientId,
                        recipientPhone = apt.patientPhone,
                        title = "Checked In: Token #$token",
                        message = "You have checked in at reception. Your token is #$token.",
                        type = "QUEUE",
                        appointmentId = appointmentId
                    )
                )
            }
        }
        return token
    }

    // Follow-ups
    suspend fun createFollowUp(
        patientId: String,
        patientName: String,
        patientPhone: String,
        doctorId: String,
        doctorName: String,
        date: String,
        notes: String
    ) {
        val fu = FollowUpEntity(
            patientId = patientId,
            patientName = patientName,
            patientPhone = patientPhone,
            doctorId = doctorId,
            doctorName = doctorName,
            followUpDate = date,
            notes = notes
        )
        dao.insertFollowUp(fu)
        dao.insertNotification(
            NotificationEntity(
                recipientRole = UserRole.ADMIN,
                recipientId = "ADMIN",
                title = "Follow-up reminder set: $patientName",
                message = "Scheduled follow-up for $patientName on $date.",
                type = "FOLLOW_UP"
            )
        )
    }

    // Payments
    suspend fun recordPayment(
        appointmentId: String,
        clientId: String,
        patientName: String,
        total: Double,
        paid: Double,
        method: PaymentMethod
    ) {
        val pending = (total - paid).coerceAtLeast(0.0)
        val status = when {
            pending == 0.0 -> PaymentStatus.PAID
            paid > 0.0 -> PaymentStatus.PARTIAL
            else -> PaymentStatus.PENDING
        }
        val p = PaymentEntity(
            id = "PAY-" + (10000..99999).random(),
            appointmentId = appointmentId,
            clientId = clientId,
            patientName = patientName,
            totalFee = total,
            paidAmount = paid,
            pendingAmount = pending,
            status = status,
            paymentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            paymentMethod = method
        )
        dao.insertPayment(p)
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = _currentUser.value?.id ?: "ADMIN",
                action = "PAYMENT_RECORDED",
                affectedRecordId = appointmentId,
                details = "Recorded payment: ₹$paid ($status)"
            )
        )
    }

    // Submit Enquiry / Message
    suspend fun submitEnquiry(
        clientId: String,
        name: String,
        phone: String,
        email: String,
        subject: String,
        message: String
    ) {
        val msg = MessageEntity(
            clientId = clientId,
            clientName = name,
            clientPhone = phone,
            clientEmail = email,
            subject = subject,
            message = message
        )
        dao.insertMessage(msg)

        dao.insertNotification(
            NotificationEntity(
                recipientRole = UserRole.ADMIN,
                recipientId = "ADMIN",
                title = "New patient enquiry received",
                message = "From $name ($phone): $subject",
                type = "ENQUIRY"
            )
        )
    }

    // Administrative AI Assistant Engine (Queries authorized clinic records)
    fun answerAdminClinicQuery(
        prompt: String,
        appointments: List<AppointmentEntity>,
        patients: List<PatientEntity>,
        followUps: List<FollowUpEntity>
    ): String {
        val lower = prompt.lowercase(Locale.ROOT)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val tomorrow = sdf.format(cal.time)

        return when {
            lower.contains("tomorrow") || lower.contains("naalaikku") || lower.contains("நாளை") -> {
                val list = appointments.filter { it.appointmentDate == tomorrow && it.status != AppointmentStatus.CANCELLED }
                if (list.isEmpty()) {
                    "You have no appointments scheduled for tomorrow ($tomorrow)."
                } else {
                    val names = list.joinToString("\n") { "• ${it.patientName} at ${it.timeSlot} (${it.status.name}) - ${it.reasonForVisit}" }
                    "You have ${list.size} appointment(s) tomorrow:\n$names"
                }
            }
            lower.contains("pending") || lower.contains("request") -> {
                val pending = appointments.filter { it.status == AppointmentStatus.PENDING }
                if (pending.isEmpty()) {
                    "There are no pending booking requests right now. All appointments are processed."
                } else {
                    val details = pending.joinToString("\n") { "• ${it.patientName} (${it.appointmentDate} at ${it.timeSlot}) - Booking ID: ${it.id}" }
                    "There are ${pending.size} pending booking request(s):\n$details"
                }
            }
            lower.contains("follow") || lower.contains("follow-up") -> {
                val pendingFollowUps = followUps.filter { it.status == "PENDING" }
                if (pendingFollowUps.isEmpty()) {
                    "No pending patient follow-ups scheduled for this period."
                } else {
                    val list = pendingFollowUps.take(5).joinToString("\n") { "• ${it.patientName} on ${it.followUpDate}: ${it.notes}" }
                    "Upcoming patient follow-ups:\n$list"
                }
            }
            lower.contains("week") -> {
                val active = appointments.filter { it.status != AppointmentStatus.CANCELLED }
                "You have ${active.size} total active appointment(s) scheduled for this period."
            }
            lower.contains("patient") || lower.contains("total") -> {
                "The clinic has ${patients.size} registered patient(s) in the database."
            }
            else -> {
                // Search for a specific patient name in prompt
                val matched = patients.firstOrNull { lower.contains(it.fullName.lowercase(Locale.ROOT)) }
                if (matched != null) {
                    val patientApts = appointments.filter { it.patientPhone == matched.phone || it.patientId == matched.id }
                    "Found record for ${matched.fullName} (Age ${matched.age}, Phone: ${matched.phone}).\nTotal Consultations: ${patientApts.size}.\nRecent visit: ${patientApts.firstOrNull()?.appointmentDate ?: "None"} (${patientApts.firstOrNull()?.reasonForVisit ?: ""})."
                } else {
                    "I am your Clinic Administrative Assistant. You can ask me:\n• 'Who is coming tomorrow?'\n• 'Show today's pending appointments'\n• 'How many appointments do I have this week?'\n• 'Who needs a follow-up?'\n• 'Show Arun Kumar's appointment history'"
                }
            }
        }
    }

    // Case History & Prescriptions
    suspend fun saveCaseHistory(caseHistory: CaseHistoryEntity) = dao.insertCaseHistory(caseHistory)
    suspend fun savePrescription(prescription: PrescriptionEntity, items: List<PrescriptionItemEntity>) {
        dao.insertPrescription(prescription)
        dao.insertPrescriptionItems(items)
        val apt = dao.getAppointmentById(prescription.appointmentId)
        if (apt != null) {
            dao.updateAppointment(apt.copy(status = AppointmentStatus.COMPLETED, updatedAt = System.currentTimeMillis()))
        }
    }

    // Direct Web Appointment Synchronization
    suspend fun importWebBooking(
        id: String,
        patientName: String,
        patientPhone: String,
        patientAge: Int,
        patientGender: String,
        date: String,
        timeSlot: String,
        consultationType: String,
        symptoms: String,
        initialStatus: AppointmentStatus = AppointmentStatus.PENDING
    ): Result<AppointmentEntity> {
        val existing = dao.getAppointmentById(id)
        if (existing != null) {
            return Result.success(existing)
        }

        val doctor = dao.getFirstDoctor()
        val doctorId = doctor?.id ?: "doc_main"
        val doctorName = doctor?.name ?: "Dr. Balaji"

        // Find or create patient record
        var patient = dao.getPatientByPhone(patientPhone)
        if (patient == null) {
            patient = PatientEntity(
                id = "patient_${System.currentTimeMillis()}",
                fullName = patientName,
                phone = patientPhone,
                age = patientAge,
                gender = patientGender,
                currentConcerns = symptoms
            )
            dao.insertOrUpdatePatient(patient)
        }

        val maxToken = dao.getMaxTokenForDate(date) ?: 0
        val assignedToken = maxToken + 1

        val type = if (consultationType.contains("ONLINE", ignoreCase = true)) {
            AppointmentType.ONLINE
        } else {
            AppointmentType.IN_CLINIC
        }

        val newAppt = AppointmentEntity(
            id = id,
            clientId = patient.userId.ifBlank { "web_patient" },
            doctorId = doctorId,
            doctorName = doctorName,
            patientId = patient.id,
            patientName = patientName,
            patientPhone = patientPhone,
            patientAge = patientAge,
            patientGender = patientGender,
            reasonForVisit = symptoms.ifBlank { "Homeopathy Consultation" },
            symptomsDescription = symptoms,
            appointmentDate = date,
            timeSlot = timeSlot,
            status = initialStatus,
            type = type,
            consultationFee = doctor?.consultationFee ?: 500.0,
            tokenNumber = assignedToken,
            notes = "Booked via Web Portal"
        )

        dao.insertAppointment(newAppt)

        // Generate in-app Admin Notification
        dao.insertNotification(
            NotificationEntity(
                recipientRole = UserRole.ADMIN,
                recipientId = "ADMIN",
                title = "New Web Booking: $patientName",
                message = "$patientName booked $timeSlot on $date. Needs Confirmation.",
                type = "NEW_BOOKING"
            )
        )

        return Result.success(newAppt)
    }

    // Real-Time Firebase Firestore Cloud Sync for Web Bookings
    fun startFirestoreRealtimeSync(context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope) {
        try {
            val dbId = context.getString(com.example.R.string.firestore_database_id)
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance(dbId)
            firestore.collection("appointments")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.w("FirestoreSync", "Live sync notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            for (doc in snapshot.documents) {
                                val id = doc.getString("id") ?: doc.id
                                val name = doc.getString("patientName") ?: continue
                                val phone = doc.getString("patientPhone") ?: ""
                                val age = doc.getString("patientAge")?.toIntOrNull() ?: 30
                                val gender = doc.getString("patientGender") ?: "Male"
                                val date = doc.getString("date") ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                val time = doc.getString("timeSlot") ?: "10:00 AM"
                                val mode = doc.getString("consultationType") ?: "IN_CLINIC"
                                val symptoms = doc.getString("symptoms") ?: "Web Booking"
                                val statusStr = doc.getString("status") ?: "PENDING"

                                val existing = dao.getAppointmentById(id)
                                if (existing == null) {
                                    importWebBooking(
                                        id = id,
                                        patientName = name,
                                        patientPhone = phone,
                                        patientAge = age,
                                        patientGender = gender,
                                        date = date,
                                        timeSlot = time,
                                        consultationType = mode,
                                        symptoms = symptoms,
                                        initialStatus = if (statusStr.equals("CONFIRMED", ignoreCase = true)) AppointmentStatus.CONFIRMED else AppointmentStatus.PENDING
                                    )
                                } else if (statusStr.equals("CONFIRMED", ignoreCase = true) && existing.status != AppointmentStatus.CONFIRMED) {
                                    dao.updateAppointment(existing.copy(status = AppointmentStatus.CONFIRMED))
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreSync", "Failed to start Firestore sync", e)
        }
    }

    suspend fun syncAppointmentConfirmationToCloud(context: android.content.Context, appointmentId: String) {
        try {
            val dbId = context.getString(com.example.R.string.firestore_database_id)
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance(dbId)
            firestore.collection("appointments").document(appointmentId)
                .update("status", "CONFIRMED")
        } catch (e: Exception) {
            android.util.Log.w("FirestoreSync", "Cloud status update notice: ${e.message}")
        }
    }
}
