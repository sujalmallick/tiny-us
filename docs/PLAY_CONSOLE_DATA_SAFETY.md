# Google Play Console — Data Safety Form Guide for Tiny Us

Use this checklist to complete the **Data Safety** questionnaire directly inside Google Play Console (**Policy > Data safety**).

---

## Section 1: Data Collection and Security Overview

| Play Console Question | Exact Answer | Rationale / Explanation |
| :--- | :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **No** | Tiny Us has no internet access (no `INTERNET` permission), no in-app purchases, no ads and no analytics or crash-reporting SDKs. Everything stays on the device, and Google's definition of "collected" only covers data sent off the device by the app. |

When you answer **No** here, the Play Console skips the per-type questions and the encryption and deletion questions. Section 2 is kept so you can double-check each type if Google asks.

---

## Section 2: Data Types Breakdown

Go through each category in the questionnaire and match these selections:

### 1. Location
- **Approximate location:** **No** (Not collected, not shared)
- **Precise location:** **No** (Not collected, not shared)

### 2. Personal Info
- **Name:** **No**  
  *(Note: Partner names entered in the app are stored exclusively on-device in local SharedPreferences and are never transmitted off the device. Google's Data Safety definition specifies that data stored purely locally and not sent to developer/third-party servers is considered NOT collected).*
- **Email address:** **No**
- **User IDs:** **No**
- **Address / Phone number:** **No**
- **Race, ethnicity, beliefs, sexual orientation:** **No**
- **Date of birth:** **No** *(Anniversaries/birthdays are stored strictly on-device).*

### 3. Financial Info
- **User payment info / Purchase history:** **No** (there are no in-app purchases).

### 4. Health and Fitness
- **No** (None collected).

### 5. Messages
- **Emails / SMS / Other in-app messages:** **No**  
  *(Love notes are stored in local SharedPreferences and are never sent to the developer. Android Auto Backup may include them in the user's own end-to-end encrypted device backup — see `app/src/main/res/xml/data_extraction_rules.xml`. Re-check Play's current Data safety guidance on OS-level backups before submitting.)*

### 6. Photos and Videos
- **Photos / Videos:** **No**  
  *(Polaroids generated in-game are saved to local private app storage or exported to device media gallery upon explicit user request. They are never transmitted off the phone).*

### 7. Audio Files
- **Voice or sound recordings:** **No**  
  *(The background music is bundled with the app and the sound effects are generated on the device. The app never uses the microphone.)*

### 8. Files and Docs
- **Files and docs:** **No**

### 9. Calendar
- **Calendar events:** **No**  
  *(The app's countdown and keepsake system uses an internal offline timeline, not the device's system Calendar provider).*

### 10. Contacts
- **Contacts:** **No**

### 11. App Activity
- **App interactions / In-app search / Installed apps:** **No** (Zero analytics SDKs).

### 12. Web Browsing
- **Web browsing history:** **No**

### 13. App Info and Performance
- **Crash logs / Diagnostics:** **No** (no crash-reporting SDK such as Firebase Crashlytics).

### 14. Device or Other Identifiers
- **Device or other identifiers:** **No**

---

## Section 3: Summary for Play Console Submission

When prompted to review your answers before submitting:
- **Data collected:** None.
- **Data shared:** None.
- **Tracking:** None.
- The store listing will show "No data collected" and "No data shared with third parties".

If the app ever adds internet access, purchases or an SDK that sends data, this form must be redone before that version is uploaded.
