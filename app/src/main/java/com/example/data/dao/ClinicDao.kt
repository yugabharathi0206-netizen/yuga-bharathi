package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicDao {

    // Users & Authentication
    @Query("SELECT * FROM users WHERE email = :identifier OR phone = :identifier LIMIT 1")
    suspend fun getUserByEmailOrPhone(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'CLIENT'")
    fun getAllClientUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = 'OWNER' LIMIT 1")
    suspend fun getOwnerUser(): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'OWNER' LIMIT 1")
    fun getOwnerUserFlow(): Flow<UserEntity?>

    @Query("SELECT COUNT(*) FROM users WHERE role = 'OWNER'")
    suspend fun countOwners(): Int

    @Query("SELECT * FROM users WHERE role = 'ADMIN' ORDER BY createdAt DESC")
    fun getAllAdmins(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    // Clinic Info
    @Query("SELECT * FROM clinics WHERE id = 'clinic_main' LIMIT 1")
    fun getClinic(): Flow<ClinicEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateClinic(clinic: ClinicEntity)

    // Doctors
    @Query("SELECT * FROM doctors")
    fun getAllDoctors(): Flow<List<DoctorEntity>>

    @Query("SELECT * FROM doctors WHERE id = :id LIMIT 1")
    suspend fun getDoctorById(id: String): DoctorEntity?

    @Query("SELECT * FROM doctors LIMIT 1")
    suspend fun getFirstDoctor(): DoctorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: DoctorEntity)

    @Update
    suspend fun updateDoctor(doctor: DoctorEntity)

    @Delete
    suspend fun deleteDoctor(doctor: DoctorEntity)

    @Query("DELETE FROM doctors WHERE id = :id")
    suspend fun deleteDoctorById(id: String)

    // Services
    @Query("SELECT * FROM services")
    fun getAllServices(): Flow<List<ServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceEntity)

    @Delete
    suspend fun deleteService(service: ServiceEntity)

    // Patients
    @Query("SELECT * FROM patients WHERE phone = :phone LIMIT 1")
    suspend fun getPatientByPhone(phone: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE id = :id LIMIT 1")
    suspend fun getPatientById(id: String): PatientEntity?

    @Query("SELECT * FROM patients WHERE userId = :userId LIMIT 1")
    suspend fun getPatientByUserId(userId: String): PatientEntity?

    @Query("SELECT * FROM patients ORDER BY fullName ASC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePatient(patient: PatientEntity)

    // Appointments - Admin (All) & Client (Own only!)
    @Query("SELECT * FROM appointments ORDER BY appointmentDate DESC, timeSlot ASC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE clientId = :clientId OR patientPhone = :phone ORDER BY appointmentDate DESC, timeSlot ASC")
    fun getAppointmentsForClient(clientId: String, phone: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE appointmentDate = :date ORDER BY timeSlot ASC")
    fun getAppointmentsForDate(date: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :appointmentId LIMIT 1")
    suspend fun getAppointmentById(appointmentId: String): AppointmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    // Real Double-Booking Prevention Query
    @Query("""
        SELECT COUNT(*) FROM appointments 
        WHERE doctorId = :doctorId 
        AND appointmentDate = :date 
        AND timeSlot = :slot 
        AND status NOT IN ('CANCELLED', 'RESCHEDULED')
    """)
    suspend fun countBookedAppointments(doctorId: String, date: String, slot: String): Int

    @Query("""
        SELECT timeSlot FROM appointments 
        WHERE doctorId = :doctorId 
        AND appointmentDate = :date 
        AND status NOT IN ('CANCELLED', 'RESCHEDULED')
    """)
    suspend fun getBookedSlotsForDoctorAndDate(doctorId: String, date: String): List<String>

    // Queue
    @Query("SELECT * FROM queue_entries WHERE queueDate = :date ORDER BY tokenNumber ASC")
    fun getQueueForDate(date: String): Flow<List<QueueEntryEntity>>

    @Query("SELECT MAX(tokenNumber) FROM queue_entries WHERE queueDate = :date")
    suspend fun getMaxTokenForDate(date: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueEntry(entry: QueueEntryEntity): Long

    @Update
    suspend fun updateQueueEntry(entry: QueueEntryEntity)

    // Case History & Prescriptions
    @Query("SELECT * FROM case_history WHERE patientId = :patientId ORDER BY id DESC")
    fun getCaseHistoryForPatient(patientId: String): Flow<List<CaseHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaseHistory(caseHistory: CaseHistoryEntity)

    @Query("SELECT * FROM prescriptions WHERE appointmentId = :appointmentId LIMIT 1")
    suspend fun getPrescriptionForAppointment(appointmentId: String): PrescriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity)

    @Query("SELECT * FROM prescription_items WHERE prescriptionId = :prescriptionId")
    suspend fun getItemsForPrescription(prescriptionId: String): List<PrescriptionItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescriptionItems(items: List<PrescriptionItemEntity>)

    // Medicines Inventory
    @Query("SELECT * FROM medicines ORDER BY remedyName ASC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity)

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    // Notifications
    @Query("SELECT * FROM notifications WHERE recipientRole IN ('ADMIN', 'OWNER') OR recipientId IN ('ADMIN', 'OWNER') ORDER BY timestamp DESC")
    fun getNotificationsForAdmin(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE recipientId = :clientId OR recipientPhone = :phone ORDER BY timestamp DESC")
    fun getNotificationsForClient(clientId: String, phone: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    // Enquiries / Messages
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    // Payments
    @Query("SELECT * FROM payments ORDER BY id DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY id DESC")
    fun getPaymentsForClient(clientId: String): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    // Follow-ups
    @Query("SELECT * FROM follow_ups ORDER BY followUpDate ASC")
    fun getAllFollowUps(): Flow<List<FollowUpEntity>>

    @Query("SELECT * FROM follow_ups WHERE patientId = :patientId OR patientPhone = :phone ORDER BY followUpDate ASC")
    fun getFollowUpsForPatient(patientId: String, phone: String): Flow<List<FollowUpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUp(followUp: FollowUpEntity)

    @Update
    suspend fun updateFollowUp(followUp: FollowUpEntity)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // Sessions
    @Query("SELECT * FROM user_sessions WHERE userId = :userId ORDER BY loginTime DESC")
    fun getSessionsForUser(userId: String): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("UPDATE user_sessions SET isActive = 0 WHERE userId = :userId")
    suspend fun deactivateAllSessionsForUser(userId: String)

    // Settings
    @Query("SELECT settingValue FROM clinic_settings WHERE settingKey = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: ClinicSettingsEntity)

    // Atomic Booking Transaction: Ensures zero double booking!
    @Transaction
    suspend fun bookAppointmentAtomic(appointment: AppointmentEntity): Boolean {
        val existingCount = countBookedAppointments(
            doctorId = appointment.doctorId,
            date = appointment.appointmentDate,
            slot = appointment.timeSlot
        )
        if (existingCount > 0) {
            return false // Slot is already taken!
        }
        insertAppointment(appointment)
        return true
    }

    // Atomic Reschedule Transaction
    @Transaction
    suspend fun rescheduleAppointmentAtomic(
        oldAppointmentId: String,
        newDoctorId: String,
        newDate: String,
        newSlot: String
    ): Boolean {
        val existingCount = countBookedAppointments(
            doctorId = newDoctorId,
            date = newDate,
            slot = newSlot
        )
        if (existingCount > 0) {
            return false
        }
        val old = getAppointmentById(oldAppointmentId) ?: return false
        val updatedOld = old.copy(
            status = AppointmentStatus.RESCHEDULED,
            updatedAt = System.currentTimeMillis()
        )
        updateAppointment(updatedOld)

        val newApt = old.copy(
            id = "HM-2026-" + (10000..99999).random(),
            doctorId = newDoctorId,
            appointmentDate = newDate,
            timeSlot = newSlot,
            status = AppointmentStatus.CONFIRMED,
            tokenNumber = null,
            queueStatus = null,
            updatedAt = System.currentTimeMillis()
        )
        insertAppointment(newApt)
        return true
    }

    // Check-in & Assign Token Transaction
    @Transaction
    suspend fun checkInPatientAtomic(appointmentId: String, todayDate: String): Int {
        val appointment = getAppointmentById(appointmentId) ?: return -1
        val maxToken = getMaxTokenForDate(todayDate) ?: 0
        val assignedToken = maxToken + 1

        val updatedApt = appointment.copy(
            status = AppointmentStatus.WAITING,
            tokenNumber = assignedToken,
            queueStatus = QueueStatus.WAITING,
            updatedAt = System.currentTimeMillis()
        )
        updateAppointment(updatedApt)

        val queueEntry = QueueEntryEntity(
            appointmentId = appointmentId,
            doctorId = appointment.doctorId,
            tokenNumber = assignedToken,
            patientName = appointment.patientName,
            patientPhone = appointment.patientPhone,
            checkInTime = System.currentTimeMillis(),
            status = QueueStatus.WAITING,
            queueDate = todayDate
        )
        insertQueueEntry(queueEntry)
        return assignedToken
    }
}
