package com.example.data.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ClinicDao
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        UserEntity::class,
        ClinicEntity::class,
        DoctorEntity::class,
        PatientEntity::class,
        AppointmentEntity::class,
        QueueEntryEntity::class,
        CaseHistoryEntity::class,
        PrescriptionEntity::class,
        PrescriptionItemEntity::class,
        MedicineEntity::class,
        ServiceEntity::class,
        NotificationEntity::class,
        MessageEntity::class,
        PaymentEntity::class,
        FollowUpEntity::class,
        AuditLogEntity::class,
        SessionEntity::class,
        ClinicSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clinicDao(): ClinicDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "homeo_clinic_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.clinicDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: ClinicDao) {
            // Clean production start:
            // NO fake doctors (e.g. Dr. Rajesh Kumar completely removed)
            // NO fake patients
            // NO fake appointments
            // NO demo users or passwords
            // Baseline Clinic Entity & Standard Services only
            val clinic = ClinicEntity(
                id = "clinic_main",
                name = "HOMEo AI Classical Clinic",
                doctorName = "",
                ownerName = "",
                phone = "", // Configured by Owner in Clinic Profile
                whatsAppNumber = "",
                email = "",
                address = "",
                openingTime = "09:00 AM",
                closingTime = "08:00 PM",
                showPhoneNumberOnPublicPage = true,
                tagline = "Smart Clinic. Better Care.",
                description = "Holistic Classical Homeopathy with precision diagnostics, constitutional case analysis, and gentle individualised healing.",
                website = "",
                workingHoursSummary = "Mon - Sat: 9:00 AM - 1:00 PM & 4:00 PM - 8:00 PM",
                defaultConsultationFee = 500.0,
                facilities = "Digital Case Repository, Live Token Dispenser, Pure Potentised Pharmacy, Air-conditioned Waiting Lounge",
                supportedLanguages = "English, Tamil, Tanglish"
            )
            dao.insertOrUpdateClinic(clinic)

            // Standard Clinical Services
            val services = listOf(
                ServiceEntity(
                    id = "srv_initial",
                    name = "Comprehensive Initial Case Taking",
                    description = "In-depth 45-minute holistic constitutional analysis, mind-body miasmatic evaluation, and customized remedy formulation.",
                    durationMinutes = 45,
                    fee = 500.0,
                    availableOnline = true,
                    availableInClinic = true
                ),
                ServiceEntity(
                    id = "srv_followup",
                    name = "Follow-up & Progression Review",
                    description = "Assessment of remedy reaction, second prescription adjustment, and symptom relief tracking.",
                    durationMinutes = 20,
                    fee = 350.0,
                    availableOnline = true,
                    availableInClinic = true
                ),
                ServiceEntity(
                    id = "srv_acute",
                    name = "Acute Illness Consultation",
                    description = "Rapid response treatment for acute cough, cold, fever, seasonal allergies, migraines, and digestive distress.",
                    durationMinutes = 15,
                    fee = 300.0,
                    availableOnline = true,
                    availableInClinic = true
                ),
                ServiceEntity(
                    id = "srv_online",
                    name = "Global Online Tele-Consultation",
                    description = "Secure high-definition video consultation with digital prescription and door-step remedy courier.",
                    durationMinutes = 30,
                    fee = 500.0,
                    availableOnline = true,
                    availableInClinic = false
                )
            )
            services.forEach { dao.insertService(it) }

            // Standard Pure Homeopathic Medicines Inventory Baseline
            val medicines = listOf(
                MedicineEntity(remedyName = "Arnica Montana", commonName = "Leopard's Bane", availablePotencies = "30C, 200C, 1M", form = "Globules", stockQuantity = 50, unit = "Bottles", price = 120.0),
                MedicineEntity(remedyName = "Nux Vomica", commonName = "Poison Nut", availablePotencies = "30C, 200C, 1M", form = "Globules", stockQuantity = 45, unit = "Bottles", price = 120.0),
                MedicineEntity(remedyName = "Lycopodium Clavatum", commonName = "Club Moss", availablePotencies = "30C, 200C, 1M", form = "Globules", stockQuantity = 30, unit = "Bottles", price = 140.0),
                MedicineEntity(remedyName = "Thuja Occidentalis", commonName = "Arbor Vitae", availablePotencies = "30C, 200C, 1M", form = "Globules", stockQuantity = 35, unit = "Bottles", price = 130.0),
                MedicineEntity(remedyName = "Rhus Toxicodendron", commonName = "Poison Ivy", availablePotencies = "30C, 200C, 1M", form = "Globules", stockQuantity = 25, unit = "Bottles", reorderLevel = 5, price = 130.0),
                MedicineEntity(remedyName = "Arsenicum Album", commonName = "White Oxide of Arsenic", availablePotencies = "30C, 200C", form = "Globules", stockQuantity = 40, unit = "Bottles", price = 125.0)
            )
            medicines.forEach { dao.insertMedicine(it) }
        }
    }
}
