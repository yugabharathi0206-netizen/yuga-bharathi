package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ContactUtils {

    const val NOT_CONFIGURED_PHONE_MSG = "Clinic phone number not configured"
    const val NOT_CONFIGURED_WHATSAPP_MSG = "WhatsApp number not configured"
    const val NOT_CONFIGURED_EMAIL_MSG = "Clinic email not configured"
    const val NOT_CONFIGURED_ADDRESS_MSG = "Clinic address not configured"

    /**
     * Requirement:
     * The Clinic Phone Number entered in the Clinic Profile must be the ONLY source for the "Call Clinic" button.
     * When user taps "Call Clinic" -> open the phone dialer using the exact Clinic Phone Number from the profile.
     * If the Clinic Phone Number has not been configured, show:
     * "Clinic phone number not configured" instead of opening a random number.
     * Works on mobile Android browsers and normal web browsers / devices.
     */
    fun callClinic(context: Context, phoneNumber: String?) {
        val clean = phoneNumber?.trim() ?: ""
        if (clean.isBlank()) {
            Toast.makeText(context, NOT_CONFIGURED_PHONE_MSG, Toast.LENGTH_LONG).show()
            return
        }
        try {
            val dialNumber = clean.replace(Regex("[^0-9+]"), "")
            val dialUri = Uri.parse("tel:$dialNumber")
            val intent = Intent(Intent.ACTION_DIAL, dialUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to launch dialer for $clean", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Requirement:
     * WhatsApp -> use the WhatsApp Number from the profile.
     */
    fun openWhatsApp(context: Context, whatsAppNumber: String?, message: String = "Hello, I would like to enquire with HOMEo AI Clinic") {
        val clean = whatsAppNumber?.replace(Regex("[^0-9]"), "") ?: ""
        if (clean.isBlank()) {
            Toast.makeText(context, NOT_CONFIGURED_WHATSAPP_MSG, Toast.LENGTH_LONG).show()
            return
        }
        try {
            val encodedMsg = Uri.encode(message)
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$clean&text=$encodedMsg")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to launch WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Email contact
     */
    fun sendEmail(context: Context, emailAddress: String?, subject: String = "Clinic Consultation Enquiry") {
        val clean = emailAddress?.trim() ?: ""
        if (clean.isBlank()) {
            Toast.makeText(context, NOT_CONFIGURED_EMAIL_MSG, Toast.LENGTH_LONG).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$clean")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to launch email client", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Directions
     */
    fun openDirections(context: Context, address: String?) {
        val clean = address?.trim() ?: ""
        if (clean.isBlank()) {
            Toast.makeText(context, NOT_CONFIGURED_ADDRESS_MSG, Toast.LENGTH_LONG).show()
            return
        }
        try {
            val mapIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=" + Uri.encode(clean))
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to launch maps", Toast.LENGTH_SHORT).show()
        }
    }
}
