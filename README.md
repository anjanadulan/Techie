# 📱 TechFix - Mobile & Computer Repair Platform

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Java](https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Gradle](https://img.shields.io/badge/Build-Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)

**TechFix** is a modern Android mobile application designed for computer and mobile phone repair services in Sri Lanka, supporting branch operations in **Colombo** and **Galle**.

---

## 📸 Key Features & UI Screens

### 1. 🎬 Splash & Loading Screen (`MainActivity`)
- Dark Charcoal Slate background (`#1A1A2E`) with Electric Purple (`#7209B7`) accent overlays.
- **TechFix** branding logo frame, title, subtitle (*"Repair & Service Platform"*), and progress spinner.
- Navigation bar providing instant access to **Sign in** and **Sign up**.

### 2. 🔑 Sign In Portal (`Signin`)
- **"Welcome back"** sheet container.
- Outlined input fields for **Email** and **Password** with password visibility toggle.
- Validates credentials against the local SQLite user database.
- Creates a persistent local session after a successful login.

### 3. 📝 Sign Up Portal (`Signup`)
- **"Get Started"** onboarding layout.
- Inputs for **Full Name**, **Email**, **Password**, and password confirmation.
- Creates unique local user accounts with salted PBKDF2 password hashes.
- Direct navigation back to Sign In.

### 4. 👤 Local Account Session (`AccountActivity`)
- Displays the signed-in customer name and email.
- Restores the signed-in session when the app is reopened.
- Provides a logout action that clears the local session.

---

## 🏗️ Architecture & Features

- **UI Layout System:** Constructed using structured `LinearLayout`s, custom drawables, and Material 3 components for optimal responsiveness following the **Tech-Precision Hybrid** color palette.
- **Locations & GPS:** Branch distance calculation between Colombo & Galle locations using `FusedLocationProviderClient` and Google Maps API.
- **Camera & Image Integration:** Attachment support for faulty device photos and repaired device proof images using `CameraX`.
- **Offline Storage:** Native SQLite stores user accounts, branches, technicians, device categories, repair services, branch spare-part inventory, appointments, payments, and repair-history events.
- **Remote Data & Web Services:** REST API / Firebase integration for real-time status tracking and appointment updates.

---

## 🎨 Design Theme: Tech-Precision Hybrid

- **Primary Background (60%):** `#1A1A2E` (Charcoal Slate)
- **Secondary / Structure (30%):** `#E0E1DD` (Platinum Silver) & `#22223B` (Container Cards)
- **Accent Highlight (10%):** `#7209B7` (Electric Purple)
- **Status Indicators:**
  - 🟢 **Success Green:** `#4CAF50` (*Ready for Pickup / Payment Successful*)
  - 🟢 **Subtle Green:** `#2E4F4F` (*Deep Sage*)
  - ⚠️ **Warning Amber:** `#FFB703` (*In Progress*)

---

## 🚀 Getting Started

### Prerequisites
* [Android Studio](https://developer.android.com/studio) (Ladybug or newer)
* JDK 11 or JDK 17
* Android Device or Emulator running Android 7.0+

### Installation & Run

1. **Clone the Repository:**
   ```bash
   git clone <repository-url> TechFix
   cd TechFix
   ```

2. **Open in Android Studio:**
   * File -> Open -> Select the `TechFix` project directory.

3. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on Emulator / Device:**
   * Select your device in Android Studio and press **Shift + F10** or click **Run app**.

---

*Created with ❤️ for TechFix Repair Services.*
