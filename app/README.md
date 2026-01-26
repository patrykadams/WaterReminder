# WaterReminder v1.1 💧

**WaterReminder** is a smart hydration tracking application built with **Jetpack Compose** and **Kotlin**. Unlike standard reminder apps, it uses a "Smart Pacing" algorithm to dynamically adjust notification intervals based on your hydration progress and the time remaining until you sleep.

## 🚀 Key Features

### 🧠 Smart Pacing Algorithm
The app eliminates static reminders. The interval is recalculated every time you drink water.
* **Formula:** `Interval = Minutes Until Bedtime / (Remaining Water / Portion Size)`
* **Dynamic Adjustment:** As the day progresses, reminders speed up or slow down to ensure you meet your goal comfortably by bedtime.
* **Constraints:** Intervals are kept between **30 and 180 minutes** to prevent spam or long silences.
* **Morning Logic:** If it's night or the interval is zero, the alarm automatically reschedules for your waking hour.

### 📊 Progress Tracking & Statistics
* **Real-time Visualization:** An animated water droplet fills up as you drink, using custom Canvas drawing in Compose.
* **7-Day History:** A bar chart visualizes your hydration consistency over the last week.
* **Streak Counter:** Tracks consecutive days you've met your daily goal.

### 🛠️ Customization
* **Personalized Goals:** Auto-calculate daily goals based on **Gender**, **Weight**, and **Activity Level** (None, Low, Medium, High).
* **Sleep Schedule:** Define your wake-up and sleep times to optimize the reminder window.
* **Quick Actions:** Add standard portions (e.g., 250ml) or custom amounts directly from the main screen or notifications.

## 🏗️ Technical Stack

* **Language:** Kotlin
* **UI:** Jetpack Compose (Material 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Persistence:** Room Database (SQLite) with Coroutines & Flow.
* **Background Tasks:** `AlarmManager` with `setExactAndAllowWhileIdle` for reliable timing.
* **Asynchronicity:** Kotlin Coroutines & StateFlow.

## 📂 Project Structure

* **`AlarmScheduler.kt`**: Contains the core math for the dynamic interval and manages system alarms.
* **`WaterViewModel.kt`**: Handles business logic, state management, and database operations. It ensures data integrity (e.g., preventing duplicate daily records).
* **`WaterScreen.kt`**: The main UI entry point containing the animated droplet, charts, and settings dialogs.
* **`WaterDatabase.kt` / `WaterDao.kt`**: Room database definitions.

## ⚙️ Installation

1.  Clone the repository.
2.  Open in **Android Studio**.
3.  Sync Gradle (Min SDK: 26).
4.  Build and Run on an emulator or physical device.

## 🔄 Version History

### v1.1 (Current)
* **Added:** Activity Level selection in settings (Low, Medium, High) to fine-tune daily goals.
* **Improved:** UI animations and confetti effect on goal completion.
* **Fixed:** Persistence logic for "Today's" record to prevent ID desynchronization.

### v1.0
* Initial release with core Smart Pacing algorithm and basic Compose UI.

---
*Built with by Patryk Adamski*