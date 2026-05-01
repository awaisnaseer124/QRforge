package com.qrforge.app.core.qr

enum class QRType(val label: String) {
    URL("URL"),
    WIFI("Wi-Fi"),
    VCARD("Contact"),
    TEXT("Text"),
    EMAIL("Email"),
    PHONE("Phone"),
    SMS("SMS")
}

data class QRField(
    val key: String,
    val label: String,
    val hint: String,
    val isPassword: Boolean = false
)

fun getFieldsForType(type: QRType): List<QRField> = when (type) {
    QRType.URL   -> listOf(QRField("url", "Website URL", "https://example.com"))
    QRType.WIFI  -> listOf(
        QRField("ssid",     "Network Name",              "MyWiFi"),
        QRField("password", "Password",                  "••••••••", isPassword = true),
        QRField("security", "Security (WPA/WEP/nopass)", "WPA")
    )
    QRType.VCARD -> listOf(
        QRField("name",  "Full Name", "John Smith"),
        QRField("phone", "Phone",     "+1 234 567 8900"),
        QRField("email", "Email",     "john@example.com"),
        QRField("org",   "Company",   "Acme Inc")
    )
    QRType.TEXT  -> listOf(QRField("text",    "Text Content", "Enter any text..."))
    QRType.EMAIL -> listOf(
        QRField("to",      "To Email", "someone@example.com"),
        QRField("subject", "Subject",  "Hello!"),
        QRField("body",    "Message",  "Your message here...")
    )
    QRType.PHONE -> listOf(QRField("phone",   "Phone Number", "+1 234 567 8900"))
    QRType.SMS   -> listOf(
        QRField("phone",   "Phone Number", "+1 234 567 8900"),
        QRField("message", "Message",      "Your message here...")
    )
}

fun buildQRContent(type: QRType, fields: Map<String, String>): String = when (type) {
    QRType.URL -> {
        val url = fields["url"].orEmpty().trim()
        if (url.startsWith("http://") || url.startsWith("https://")) url
        else if (url.isBlank()) ""
        else "https://$url"
    }
    QRType.WIFI -> {
        val ssid     = fields["ssid"].orEmpty()
        val password = fields["password"].orEmpty()
        val security = fields["security"].orEmpty().ifBlank { "WPA" }
        if (ssid.isBlank()) "" else "WIFI:T:$security;S:$ssid;P:$password;;"
    }
    QRType.VCARD -> {
        val name  = fields["name"].orEmpty()
        val phone = fields["phone"].orEmpty()
        val email = fields["email"].orEmpty()
        val org   = fields["org"].orEmpty()
        if (name.isBlank()) ""
        else "BEGIN:VCARD\nVERSION:3.0\nFN:$name\nTEL:$phone\nEMAIL:$email\nORG:$org\nEND:VCARD"
    }
    QRType.TEXT  -> fields["text"].orEmpty()
    QRType.EMAIL -> {
        val to      = fields["to"].orEmpty()
        val subject = fields["subject"].orEmpty()
        val body    = fields["body"].orEmpty()
        if (to.isBlank()) "" else "mailto:$to?subject=$subject&body=$body"
    }
    QRType.PHONE -> {
        val phone = fields["phone"].orEmpty()
        if (phone.isBlank()) "" else "tel:$phone"
    }
    QRType.SMS -> {
        val phone   = fields["phone"].orEmpty()
        val message = fields["message"].orEmpty()
        if (phone.isBlank()) "" else "sms:$phone?body=$message"
    }
}