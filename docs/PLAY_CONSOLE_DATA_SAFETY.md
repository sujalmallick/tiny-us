# Google Play Console — Data Safety Form Guide for Tiny Us

Use this checklist to complete the **Data Safety** questionnaire directly inside Google Play Console (**Policy > Data safety**).

---

## Section 1: Data Collection and Security Overview

| Play Console Question | Exact Answer | Rationale / Explanation |
| :--- | :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **Yes** | Even though Tiny Us itself collects zero data, integrating Google Play Billing causes Google Play Services to collect/process transaction data on Google's behalf. Under Google's guidelines, third-party libraries integrated into the app must be disclosed. |
| **Is all of the user data collected by your app encrypted in transit?** | **Yes** | All purchase communications between the device and Google Play servers occur over encrypted HTTPS protocols handled by Google Play Services. |
| **Do you provide a way for users to request that their data be deleted?** | **Yes** | Users can delete all locally stored data at any time by clearing application data in Android settings or uninstalling the app. |

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
- **User payment info (credit card, bank account):** **No**  
  *(Processed by Google Play directly in its own system dialogs; Tiny Us code never touches card details).*
- **Purchase history:** **Yes (Collected by Google Play Billing SDK)**
  - *Is this data shared with third parties?* **No** (Shared with Google as service provider).
  - *Is this data processed ephemerally?* **No**
  - *Is this data required or optional?* **Optional** (Users can use the entire core app without making purchases; only collected if the user chooses to buy an optional pack).
  - *Why is this data collected?* **App functionality / Account management** (to deliver digital purchases and enable purchase restoration across devices).

### 4. Health and Fitness
- **No** (None collected).

### 5. Messages
- **Emails / SMS / Other in-app messages:** **No**  
  *(Love notes written in the app are stored purely in local device files/SharedPreferences and never leave the device).*

### 6. Photos and Videos
- **Photos / Videos:** **No**  
  *(Polaroids generated in-game are saved to local private app storage or exported to device media gallery upon explicit user request. They are never transmitted off the phone).*

### 7. Audio Files
- **Voice or sound recordings:** **No**  
  *(Ambient audio and synth music are generated procedurally on-device using AudioTrack PCM waveforms. No microphone input or audio collection exists).*

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
- **Crash logs / Diagnostics:** **No**  
  *(Unless an optional mail-based feedback intent is triggered by the user via their own email client, no crash reporting SDK like Firebase Crashlytics is embedded).*

### 14. Device or Other Identifiers
- **Device or other identifiers:** **Yes (Collected by Google Play Billing SDK)**
  - *Is this data collected or shared?* **Collected by Google Play Services**
  - *Is this data optional?* **Optional** (Only accessed during purchase transactions).
  - *Why is this data collected?* **Fraud prevention, security, and compliance / App functionality**.

---

## Section 3: Summary for Play Console Submission

When prompted to review your answers before submitting:
- **Data Collected:** Purchase history, Device identifiers (both exclusively for Google Play in-app purchase functionality).
- **Data Shared:** None (no third-party advertising or analytics data sharing).
- **Tracking:** No data is used to track users across apps or websites.
- **Security:** Encrypted in transit (HTTPS).
- **Data Deletion:** Full deletion supported locally via device app settings or app uninstall.
