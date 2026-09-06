package com.evgeniich.flashread.ui.settings

data class LegalSection(
    val heading: String,
    val body: String,
)

data class LegalDocument(
    val title: String,
    val lastUpdated: String,
    val sections: List<LegalSection>,
)

object LegalDocuments {
    private const val LAST_UPDATED = "September 6, 2026"
    private const val DEVELOPER_NAME = "Yevhen Chmutov"
    private const val CONTACT_EMAIL = "yevhen.chmutov.support@gmail.com"

    val privacyPolicy = LegalDocument(
        title = "Privacy Policy",
        lastUpdated = LAST_UPDATED,
        sections = listOf(
            LegalSection(
                heading = "Overview",
                body = "FlashReader is a reading and speed-reading app published by $DEVELOPER_NAME. This policy explains what information the app stores and how it is used. FlashReader is designed to keep your materials on your device.",
            ),
            LegalSection(
                heading = "Information stored on your device",
                body = "FlashReader stores the books and files you import, books you write in the app, extracted text, titles, optional cover images, reading progress, reader preferences, and speed-reading settings. This information stays in app storage on your device.",
            ),
            LegalSection(
                heading = "Analytics",
                body = "FlashReader uses Google Firebase Analytics to record usage events, such as importing a book, opening the reader, starting a speed-reading session, and changing settings. These events do not include the titles or text of your books. Google may collect the device Advertising ID as part of Firebase Analytics.",
            ),
            LegalSection(
                heading = "Crash reports",
                body = "FlashReader uses Google Firebase Crashlytics to collect crash and error reports so we can fix stability problems. These reports include stack traces, device and app version information, and related diagnostic data. They do not include the titles or text of your books. Collection is subject to your consent choices where Google’s consent tools apply.",
            ),
            LegalSection(
                heading = "Information we do not collect",
                body = "FlashReader does not create an account, and it does not collect location data from GPS. The app does not upload your imported books, books you write, or reading history to our servers.",
            ),
            LegalSection(
                heading = "How information is used",
                body = "Local data is used only to show your library, remember where you left off, and apply the reading preferences you choose. Books you write stay on the device so you can keep editing them.",
            ),
            LegalSection(
                heading = "Sharing",
                body = "We do not sell or share your library or reading data. The operating system may include FlashReader data in device backups you choose to make.",
            ),
            LegalSection(
                heading = "Retention and your choices",
                body = "Your materials remain on the device until you delete them from the library or uninstall the app. Uninstalling FlashReader removes the data stored by the app, except copies that may remain in an operating-system backup. Google retains Firebase Analytics data for 14 months and Firebase Crashlytics crash data for 90 days. Uninstalling the app does not guarantee that Google will delete information already processed by Firebase Analytics, Firebase Crashlytics, AdMob, UMP, or Google Play Billing.",
            ),
            LegalSection(
                heading = "Children",
                body = "FlashReader is not directed at children under 13. We do not knowingly collect personal information from children.",
            ),
            LegalSection(
                heading = "Changes",
                body = "If this policy changes, we will update the date at the top of this screen. Continued use of FlashReader after an update means you accept the revised policy.",
            ),
            LegalSection(
                heading = "Contact",
                body = "For privacy questions, contact $DEVELOPER_NAME at $CONTACT_EMAIL.",
            ),
        ),
    )

    val termsAndConditions = LegalDocument(
        title = "Terms & Conditions",
        lastUpdated = LAST_UPDATED,
        sections = listOf(
            LegalSection(
                heading = "Agreement",
                body = "By using FlashReader, you agree to these terms. If you do not agree, do not use the app.",
            ),
            LegalSection(
                heading = "The app",
                body = "FlashReader lets you import reading materials, write your own books, read them on your device, and practice speed-reading. Features may change as the app is updated.",
            ),
            LegalSection(
                heading = "Your content",
                body = "You are responsible for the files, titles, and books you write. Only import or create materials you have the right to use. FlashReader does not claim ownership of your imported books, written books, or saved materials.",
            ),
            LegalSection(
                heading = "Acceptable use",
                body = "Use FlashReader only for lawful purposes. Do not use the app to store or distribute content that you are not allowed to copy, or to interfere with the app or other users.",
            ),
            LegalSection(
                heading = "Intellectual property",
                body = "FlashReader, including its name, logo, and software, is owned by $DEVELOPER_NAME and is licensed to you for personal use. These terms do not transfer any ownership rights to you.",
            ),
            LegalSection(
                heading = "Disclaimer",
                body = "FlashReader is provided \"as is\" and \"as available\". Reading progress, imports, and playback may fail because of file format limits, device storage, or other issues outside our control. We do not warrant that the app will be uninterrupted or error-free.",
            ),
            LegalSection(
                heading = "Limitation of liability",
                body = "To the fullest extent permitted by law, the developer is not liable for any indirect, incidental, or consequential damages, or for loss of data, arising from your use of FlashReader. Some jurisdictions do not allow certain limitations, so parts of this section may not apply to you.",
            ),
            LegalSection(
                heading = "Changes",
                body = "We may update these terms from time to time. The date at the top of this screen shows the latest version. Continued use after an update means you accept the revised terms.",
            ),
            LegalSection(
                heading = "Governing law",
                body = "These terms are governed by the laws of Poland and the courts of Poland, without regard to conflict-of-law rules, except where mandatory consumer-protection laws in your country of residence apply and cannot be waived.",
            ),
            LegalSection(
                heading = "Contact",
                body = "For questions about these terms, contact $DEVELOPER_NAME at $CONTACT_EMAIL.",
            ),
        ),
    )
}
