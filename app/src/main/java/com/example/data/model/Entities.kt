package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    OWNER,
    ADMIN,
    CLIENT
}

enum class AppointmentStatus {
    PENDING,
    CONFIRMED,
    RESCHEDULED,
    COMPLETED,
    CANCELLED,
    NO_SHOW,
    WAITING,
    IN_CONSULTATION,
    AVAILABLE,
    BOOKED
}

enum class AppointmentType {
    IN_CLINIC,
    ONLINE
}

enum class PaymentStatus {
    PAID,
    PARTIAL,
    PENDING,
    FAILED
}

enum class PaymentMethod {
    PAY_AT_CLINIC,
    ONLINE,
    UPI,
    CASH,
    CARD
}

enum class QueueStatus {
    WAITING,
    IN_CONSULTATION,
    COMPLETED,
    SKIPPED
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // e.g. "usr_owner_01", "usr_admin_01", "usr_client_01"
    val email: String,
    val phone: String,
    val passwordHash: String, // Stored securely
    val fullName: String,
    val role: UserRole,
    val dateOfBirth: String = "",
    val gender: String = "Male",
    val address: String = "",
    val emergencyContact: String = "",
    val twoFactorEnabled: Boolean = false,
    // Admin access and permission controls (Controlled by Owner)
    val isApproved: Boolean = true, // Owner & Client default true; new Admin pending approval
    val approvalStatus: String = "APPROVED", // "PENDING", "APPROVED", "REJECTED"
    val canManageDoctors: Boolean = false, // Owner only by default
    val canManageAdmins: Boolean = false,  // Owner only
    val canManageAppointments: Boolean = true,
    val canManagePatients: Boolean = true,
    val canManageQueue: Boolean = true,
    val canManageMedicines: Boolean = true,
    val canManageBilling: Boolean = true,
    val canViewReports: Boolean = true,
    val canManageSettings: Boolean = false,
    val isActive: Boolean = true,
    val invitedBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "clinics")
data class ClinicEntity(
    @PrimaryKey val id: String = "clinic_main",
    val name: String = "HOMEo AI Classical Clinic",
    val doctorName: String = "",
    val ownerName: String = "",
    val phone: String = "", // Central Clinic Phone Number (Empty until configured by Owner)
    val whatsAppNumber: String = "", // WhatsApp Number
    val email: String = "",
    val address: String = "",
    val openingTime: String = "09:00 AM",
    val closingTime: String = "08:00 PM",
    val showPhoneNumberOnPublicPage: Boolean = true,
    val tagline: String = "Smart Clinic. Better Care.",
    val description: String = "Holistic Classical Homeopathy with precision diagnostics, constitutional case analysis, and gentle individualised healing.",
    val website: String = "",
    val workingHoursSummary: String = "Mon - Sat: 9:00 AM - 1:00 PM & 4:00 PM - 8:00 PM",
    val defaultConsultationFee: Double = 500.0,
    val facilities: String = "Digital Case Repository, Live Token Dispenser, Pure Potentised Pharmacy, Air-conditioned Waiting Lounge",
    val supportedLanguages: String = "English, Tamil, Tanglish"
)

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey val id: String = "doc_main",
    val name: String,
    val qualification: String,
    val specialization: String,
    val experienceYears: Int,
    val registrationNumber: String = "",
    val bio: String = "",
    val consultationFee: Double = 500.0,
    val morningStart: String = "09:00", // "09:00"
    val morningEnd: String = "13:00",   // "13:00"
    val breakStart: String = "13:00",   // "13:00"
    val breakEnd: String = "16:00",     // "16:00"
    val eveningStart: String = "16:00", // "16:00"
    val eveningEnd: String = "20:00",   // "20:00"
    val availableDays: String = "Mon,Tue,Wed,Thu,Fri,Sat", // "Mon,Tue,Wed,Thu,Fri,Sat"
    val slotDurationMinutes: Int = 30,
    val phone: String = "",
    val whatsAppNumber: String = "",
    val clinicName: String = "",
    val clinicAddress: String = "",
    val languages: String = "English, Tamil, Tanglish",
    val services: String = "Constitutional Homeopathy, Chronic Disease Care, Pediatric Care",
    val socialMediaLinks: String = "",
    val photoUrl: String = ""
)

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val fullName: String,
    val phone: String,
    val email: String = "",
    val dateOfBirth: String = "",
    val age: Int = 0,
    val gender: String = "",
    val address: String = "",
    val emergencyContact: String = "",
    val currentConcerns: String = "",
    val uploadedDocumentsSummary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey val id: String, // e.g. "HM-2026-00125"
    val clientId: String, // foreign reference to users.id
    val doctorId: String,
    val doctorName: String,
    val patientId: String,
    val patientName: String,
    val patientPhone: String,
    val patientEmail: String = "",
    val patientAge: Int = 30,
    val patientGender: String = "Male",
    val reasonForVisit: String,
    val symptomsDescription: String = "",
    val uploadedDocumentUrl: String = "",
    val appointmentDate: String, // "YYYY-MM-DD"
    val timeSlot: String,        // "10:30 AM"
    val status: AppointmentStatus = AppointmentStatus.PENDING,
    val type: AppointmentType = AppointmentType.IN_CLINIC,
    val meetingLink: String = "",
    val consultationFee: Double = 500.0,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val paymentMethod: PaymentMethod = PaymentMethod.PAY_AT_CLINIC,
    val tokenNumber: Int? = null,
    val queueStatus: QueueStatus? = null,
    val adminNotes: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "queue_entries")
data class QueueEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appointmentId: String,
    val doctorId: String,
    val tokenNumber: Int,
    val patientName: String,
    val patientPhone: String,
    val checkInTime: Long = System.currentTimeMillis(),
    val status: QueueStatus = QueueStatus.WAITING,
    val queueDate: String // "YYYY-MM-DD"
)

@Entity(tableName = "case_history")
data class CaseHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appointmentId: String,
    val patientId: String,
    val doctorId: String,
    val visitDate: String,
    val chiefComplaints: String,
    val totalityOfSymptoms: String,
    val mentalState: String = "",
    val physicalGenerals: String = "",
    val miasm: String = "Psora", // Psora, Sycosis, Syphilis, Tubercular
    val repertorisationSummary: String = "",
    val clinicalNotes: String = ""
)

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey val id: String, // e.g. "RX-91823"
    val appointmentId: String,
    val patientId: String,
    val doctorId: String,
    val date: String,
    val diagnosis: String,
    val dietAndRegimenAdvice: String = "Avoid coffee, raw onions, and strong aromatic substances 30 minutes before/after medicine.",
    val followUpInDays: Int = 14
)

@Entity(tableName = "prescription_items")
data class PrescriptionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prescriptionId: String,
    val medicineName: String, // e.g. "Arnica Montana"
    val potency: String,      // "30C", "200C", "1M"
    val dosageForm: String,   // "Globules No. 30", "Dilution"
    val dosageInstruction: String, // "4 pills thrice daily"
    val durationDays: Int = 7
)

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remedyName: String,
    val commonName: String,
    val availablePotencies: String, // "6C, 30C, 200C, 1M"
    val form: String, // "Globules", "Dilution", "Mother Tincture", "Trituration"
    val stockQuantity: Int,
    val unit: String = "Bottles",
    val reorderLevel: Int = 5,
    val price: Double
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val fee: Double,
    val availableOnline: Boolean = true,
    val availableInClinic: Boolean = true
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientRole: UserRole = UserRole.CLIENT, // ADMIN or CLIENT
    val recipientId: String = "", // clientId or "ADMIN"
    val recipientPhone: String = "",
    val title: String,
    val message: String,
    val type: String, // "NEW_BOOKING", "CONFIRMED", "RESCHEDULED", "CANCELLED", "COMPLETED", "REMINDER"
    val appointmentId: String? = null,
    val channel: String = "IN_APP",
    val channelStatus: String = "SENT",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: String,
    val clientName: String,
    val clientPhone: String,
    val clientEmail: String = "",
    val subject: String,
    val message: String,
    val status: String = "UNREAD", // UNREAD, REPLIED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String, // e.g. "PAY-84920"
    val appointmentId: String,
    val clientId: String,
    val patientName: String,
    val totalFee: Double,
    val paidAmount: Double,
    val pendingAmount: Double,
    val status: PaymentStatus, // PAID, PARTIAL, PENDING
    val paymentDate: String,
    val paymentMethod: PaymentMethod = PaymentMethod.PAY_AT_CLINIC
)

@Entity(tableName = "follow_ups")
data class FollowUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patientId: String,
    val patientName: String,
    val patientPhone: String,
    val doctorId: String,
    val doctorName: String,
    val followUpDate: String, // "YYYY-MM-DD"
    val notes: String = "",
    val status: String = "PENDING", // PENDING, COMPLETED
    val reminderSent: Boolean = false
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val adminId: String,
    val action: String, // "APPOINTMENT_CONFIRMED", "APPOINTMENT_CANCELLED", "PATIENT_UPDATED", "PAYMENT_RECORDED"
    val affectedRecordId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val deviceName: String,
    val ipAddress: String = "192.168.1.1",
    val loginTime: Long = System.currentTimeMillis(),
    val lastActiveTime: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "clinic_settings")
data class ClinicSettingsEntity(
    @PrimaryKey val settingKey: String,
    val settingValue: String
)
