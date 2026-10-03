package com.example.ui.voice

import androidx.compose.runtime.*
import com.example.data.model.AppointmentType
import com.example.data.model.DoctorEntity
import com.example.data.model.PaymentMethod
import com.example.data.repository.ClinicRepository
import com.example.data.repository.SlotInfo
import com.example.localization.AppLanguage
import com.example.localization.StringsLocalization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class VoiceBookingStep {
    IDLE,
    SELECTING_DOCTOR,
    SELECTING_SLOT,
    CONFIRMATION_PROMPT,
    COMPLETED
}

data class VoiceMessage(
    val sender: String, // "AI" or "USER"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class VoiceAssistantManager(
    private val repository: ClinicRepository,
    private val scope: CoroutineScope
) {
    var isListening by mutableStateOf(false)
    var bookingStep by mutableStateOf(VoiceBookingStep.IDLE)
    var conversationHistory = mutableStateListOf<VoiceMessage>()

    var selectedDoctor by mutableStateOf<DoctorEntity?>(null)
    var selectedDate by mutableStateOf<String>("")
    var selectedSlot by mutableStateOf<String>("")
    var availableSlotsForSelection = mutableStateListOf<SlotInfo>()
    var proposedBookingPending by mutableStateOf(false)

    init {
        // Welcome message
        addAiMessage("Vanakkam & Welcome to HOMEo AI. You can speak in English, தமிழ், or Tanglish. How can I assist you today?")
    }

    fun addAiMessage(msg: String) {
        conversationHistory.add(VoiceMessage("AI", msg))
    }

    fun addUserMessage(msg: String) {
        conversationHistory.add(VoiceMessage("USER", msg))
    }

    fun resetConversation() {
        conversationHistory.clear()
        bookingStep = VoiceBookingStep.IDLE
        selectedDoctor = null
        selectedSlot = ""
        proposedBookingPending = false
        addAiMessage("HOMEo AI is ready. You can ask for appointments today, tomorrow, or doctor consultations in Tamil, Tanglish, or English.")
    }

    fun processVoiceInput(input: String, lang: AppLanguage, onOpenConfirmation: (DoctorEntity, String, String) -> Unit) {
        val cleanInput = input.trim()
        if (cleanInput.isEmpty()) return

        addUserMessage(cleanInput)
        val lower = cleanInput.lowercase(Locale.ROOT)

        scope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())

            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            val tomorrowStr = sdf.format(cal.time)

            when (bookingStep) {
                VoiceBookingStep.IDLE -> {
                    // Detect desire to book
                    val wantsBooking = lower.contains("appointment") || lower.contains("book") ||
                            lower.contains("doctor") || lower.contains("venum") ||
                            lower.contains("முன்பதிவு") || lower.contains("வேண்டும்")

                    // Check date intent
                    val isTomorrow = lower.contains("tomorrow") || lower.contains("naalaikku") ||
                            lower.contains("நாளை") || lower.contains("nalaikku")
                    val isToday = lower.contains("today") || lower.contains("innaikku") ||
                            lower.contains("inru") || lower.contains("இன்று") || (!isTomorrow)

                    selectedDate = if (isTomorrow) tomorrowStr else todayStr

                    // Check doctor name in prompt
                    var matchedDoctor: DoctorEntity? = null
                    if (lower.contains("rajesh") || lower.contains("kumar") || lower.contains("ராஜேஷ்")) {
                        matchedDoctor = repository.getDoctor("doc_rajesh")
                    } else if (lower.contains("priya") || lower.contains("ananth") || lower.contains("பிரியா")) {
                        matchedDoctor = repository.getDoctor("doc_priya")
                    }

                    if (matchedDoctor != null) {
                        selectedDoctor = matchedDoctor
                        advanceToSlotSelection(matchedDoctor, selectedDate, lower)
                    } else {
                        bookingStep = VoiceBookingStep.SELECTING_DOCTOR
                        val reply = when (lang) {
                            AppLanguage.TAMIL -> "நிச்சயமாக. எந்த மருத்துவரை பார்க்க விரும்புகிறீர்கள்? டாக்டர் ராஜேஷ் குமார் அல்லது டாக்டர் பிரியா ஆனந்த்?"
                            AppLanguage.TANGLISH -> "Sure! Entha doctor-ku book pannanum? Dr. Rajesh Kumar or Dr. Priya Ananth?"
                            AppLanguage.ENGLISH -> "Sure! Which doctor would you like to consult? Dr. Rajesh Kumar or Dr. Priya Ananth?"
                        }
                        addAiMessage(reply)
                    }
                }

                VoiceBookingStep.SELECTING_DOCTOR -> {
                    var doctor: DoctorEntity? = null
                    if (lower.contains("rajesh") || lower.contains("kumar") || lower.contains("ராஜேஷ்") || lower.contains("first") || lower.contains("1")) {
                        doctor = repository.getDoctor("doc_rajesh")
                    } else if (lower.contains("priya") || lower.contains("ananth") || lower.contains("பிரியா") || lower.contains("second") || lower.contains("2")) {
                        doctor = repository.getDoctor("doc_priya")
                    }

                    if (doctor != null) {
                        selectedDoctor = doctor
                        advanceToSlotSelection(doctor, selectedDate.ifEmpty { todayStr }, lower)
                    } else {
                        addAiMessage("Please choose between Dr. Rajesh Kumar (Senior Classical Homeopath) or Dr. Priya Ananth.")
                    }
                }

                VoiceBookingStep.SELECTING_SLOT -> {
                    val doc = selectedDoctor
                    if (doc == null) {
                        bookingStep = VoiceBookingStep.IDLE
                        return@launch
                    }

                    // Find if user mentioned time like "5", "5:00", "5:30", "10", "11", "evening", "morning"
                    val available = availableSlotsForSelection.filter { it.isAvailable }
                    val matchedSlot = available.firstOrNull { slot ->
                        val slotDigits = slot.timeSlot.replace(":", "").replace(" ", "").lowercase(Locale.ROOT)
                        val queryDigits = lower.replace(":", "").replace(" ", "")
                        queryDigits.contains(slot.timeSlot.lowercase(Locale.ROOT)) ||
                                (lower.contains("5") && slot.timeSlot.startsWith("05")) ||
                                (lower.contains("5:30") && slot.timeSlot.contains("05:30")) ||
                                (lower.contains("10") && slot.timeSlot.startsWith("10")) ||
                                (lower.contains("11") && slot.timeSlot.startsWith("11")) ||
                                (lower.contains("first") && slot == available.firstOrNull())
                    } ?: available.firstOrNull()

                    if (matchedSlot != null) {
                        selectedSlot = matchedSlot.timeSlot
                        bookingStep = VoiceBookingStep.CONFIRMATION_PROMPT
                        proposedBookingPending = true

                        val dateLabel = if (selectedDate == todayStr) "Today" else "Tomorrow ($selectedDate)"
                        val reply = when (lang) {
                            AppLanguage.TAMIL -> "${doc.name} உடன் $dateLabel ${matchedSlot.timeSlot} மணிக்கு முன்பதிவை உறுதி செய்யவா? (உறுதி செய்ய 'Yes' அல்லது 'சரி' என கூறவும்)"
                            AppLanguage.TANGLISH -> "${doc.name} kitta $dateLabel ${matchedSlot.timeSlot}-ku confirm pannava? Say 'Yes' or 'Confirm pannu'."
                            AppLanguage.ENGLISH -> "Should I confirm booking with ${doc.name} on $dateLabel at ${matchedSlot.timeSlot}? Please say 'Yes' to confirm."
                        }
                        addAiMessage(reply)
                    } else {
                        addAiMessage("Could not find that slot. Please choose an available time slot.")
                    }
                }

                VoiceBookingStep.CONFIRMATION_PROMPT -> {
                    val isConfirmed = lower.contains("yes") || lower.contains("confirm") ||
                            lower.contains("pannu") || lower.contains("சரி") ||
                            lower.contains("aama") || lower.contains("ok") || lower.contains("okay")

                    val doc = selectedDoctor
                    if (isConfirmed && doc != null && selectedSlot.isNotEmpty()) {
                        bookingStep = VoiceBookingStep.COMPLETED
                        proposedBookingPending = false
                        addAiMessage("Opening booking confirmation screen for ${doc.name} at $selectedSlot. Please verify your patient details.")
                        withContext(Dispatchers.Main) {
                            onOpenConfirmation(doc, selectedDate, selectedSlot)
                        }
                    } else if (lower.contains("no") || lower.contains("cancel") || lower.contains("vendaam")) {
                        bookingStep = VoiceBookingStep.IDLE
                        proposedBookingPending = false
                        addAiMessage("Booking cancelled. Let me know if you need another time or doctor.")
                    } else {
                        addAiMessage("Please say 'Yes' or 'Confirm' to finalize your appointment.")
                    }
                }

                VoiceBookingStep.COMPLETED -> {
                    addAiMessage("Your appointment request was forwarded. Would you like to check token status or book another visit?")
                    bookingStep = VoiceBookingStep.IDLE
                }
            }
        }
    }

    private suspend fun advanceToSlotSelection(doctor: DoctorEntity, date: String, inputContext: String) {
        val slots = repository.getAvailableSlots(doctor, date)
        availableSlotsForSelection.clear()
        availableSlotsForSelection.addAll(slots)

        val available = slots.filter { it.isAvailable }
        val eveningPreferred = inputContext.contains("evening") || inputContext.contains("maalai") || inputContext.contains("ஈவ்னிங்")

        val targetSlots = if (eveningPreferred) {
            available.filter { it.timeSlot.contains("PM") }
        } else {
            available
        }

        val slotSummary = targetSlots.take(3).joinToString(", ") { it.timeSlot }
        bookingStep = VoiceBookingStep.SELECTING_SLOT

        val reply = if (targetSlots.isNotEmpty()) {
            "Available slots for ${doctor.name} are $slotSummary. Which slot would you prefer?"
        } else {
            "No free slots found on this day for ${doctor.name}. Would you like to try another date?"
        }
        addAiMessage(reply)
    }
}
