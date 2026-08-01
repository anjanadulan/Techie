# 📱 TechFix - Mobile & Computer Repair Platform

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Java](https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Gradle](https://img.shields.io/badge/Build-Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)

**TechFix** is a modern Android mobile application designed for computer and mobile phone repair services in Sri Lanka, supporting branch operations in **Colombo** and **Galle**.

---

## 📸 Key Features & UI Screens

### 1. 🎬 Splash & Loading Screen (`MainActivity`)
- Light cloud-blue header with a clean white authentication card and black primary actions.
- **TechFix** branding logo frame, title, subtitle (*"Repair & Service Platform"*), and progress spinner.
- Navigation bar providing instant access to **Sign in** and **Sign up**.

### 2. 🔑 Sign In Portal (`Signin`)
- **"Welcome back"** sheet container.
- Outlined input fields for **Email** and **Password** with password visibility toggle.
- Authenticates email and password accounts with Firebase Authentication.
- Mirrors the signed-in profile into SQLite for offline appointment relationships.

### 3. 📝 Sign Up Portal (`Signup`)
- **"Get Started"** onboarding layout.
- Inputs for **Full Name**, **Email**, **Password**, and password confirmation.
- Creates Firebase accounts and a matching offline SQLite profile.
- Direct navigation back to Sign In.

### 4. 👤 Local Account Session (`AccountActivity`)
- Displays the signed-in customer name and email.
- Restores the signed-in session when the app is reopened.
- Provides a logout action that clears the local session.

---

## 🏗️ Architecture & Features

- **UI Layout System:** Constructed using structured `LinearLayout`s, custom drawables, and Material 3 components for optimal responsiveness following the **Tech-Precision Hybrid** color palette.
- **Locations & GPS:** Uses Android `LocationManager` and Haversine distance calculations to select the nearest branch that has a suitable technician and required stock.
- **Camera & Image Integration:** Managers can capture repaired devices through an in-app CameraX viewfinder or select an existing gallery image. Photos are retained locally until WorkManager uploads them to Firebase Storage, and featured samples are displayed in the customer repair gallery.
- **Offline Storage:** Native SQLite stores user accounts, branches, technicians, device categories, repair services, branch spare-part inventory, appointments, payments, and repair-history events.
- **Remote Data & Web Services:** Firebase Authentication, Cloud Firestore listeners, and WorkManager synchronize availability, appointments, payments, and repair progress.
- **Collision-safe Sync:** Customer appointments, payments, and repair-history events use persistent UUIDs across devices.
- **Customer Availability:** Customers can browse active technicians, specialties, branches, and current spare-part quantities.
- **Management:** Dedicated modules manage branches, device categories, appointments, technicians, pricing, spare parts, repair images, payments, and statuses.
- **Customer Payments:** A coursework-safe payment simulation records card, bank-transfer, or online payment receipts without collecting real banking credentials.
- **Repair Gallery:** Managers can feature repaired-device images for customers to browse.

---

## 🎨 Design Theme

- **Authentication:** Cloud blue, white, and black for calm, focused account flows.
- **Customer experience:** Warm white surfaces with blue and orange accents.
- **Management workspace:** Deep navy surfaces with cyan, green, and amber operational states.

---

## 🚀 Getting Started

### Prerequisites
* [Android Studio](https://developer.android.com/studio) (Ladybug or newer)
* JDK 11 or JDK 17
* Android Device or Emulator running Android 7.0+

### Installation & Run

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/anjanadulan/Techie.git TechFix
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
