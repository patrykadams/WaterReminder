# WaterReminder 💧

**WaterReminder** is a modern Android application designed to help users maintain healthy hydration levels. It features a **smart notification scheduling algorithm** that dynamically adjusts drinking intervals based on the user's daily progress, remaining time until sleep, and physical activity level.

The application is built using **Kotlin** and **Jetpack Compose**, following the **MVVM** architecture pattern.

## ✨ Key Features

* **🧠 Smart Scheduler (Smart Interval):** The app avoids rigid, fixed intervals. The algorithm calculates the optimal time for the next reminder based on:
    * Remaining water to reach the goal.
    * Time remaining until the user's wake/sleep schedule.
    * Recent activity levels (prevents notification fatigue).
* **🚫 Anti-Spam System:**
    * **Cooldown:** Hard block on notifications occurring more frequently than every 15 minutes.
    * **Evening Calm:** Automatic silence mode starting 60 minutes before bed.
    * **Last Glass Mode:** A relaxed schedule when the goal is nearly reached.
* **📊 Goal Personalization:** Automatically calculates daily water intake needs based on:
    * Gender (different motivational messages for men and women).
    * Weight.
    * Physical Activity Level (None, Low, Medium, High).
* **🎨 Modern UI (Jetpack Compose):**
    * Animated water droplet visualizing progress.
    * Bar charts displaying 7-day history.
    * Confetti effect upon reaching the daily goal.
    * Native Material 3 Time Picker for scheduling.
* **🔔 Interactive Notifications:** Quick actions to add water (+250ml) or enter a custom amount directly from the notification shade.

## 🛠 Tech Stack & Libraries

The project utilizes a modern Android development stack:

* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material Design 3)
* **Architecture:** MVVM (Model-View-ViewModel)
* **Database:** [Room](https://developer.android.com/training/data-storage/room) (SQLite) + Kotlin Coroutines & Flow
* **Scheduling:** [AlarmManager](https://developer.android.com/reference/android/app/AlarmManager) (using `setAndAllowWhileIdle` for precision)
* **Background:** BroadcastReceiver (handling notification actions and alarms)
* **Local Storage:** SharedPreferences (for user settings)

## 📱 How the Algorithm Works

The core of the application is the `AlarmScheduler` object. Every time water is added, it executes the following logic:

1.  **State Analysis:** Checks how much water is missing and how much time is left before sleep.
2.  **Mathematical Calculation:** Divides the available time by the number of remaining portions.
3.  **Safety Correction:**
    * If the result is < 45 min -> Enforces a minimum of 45 min.
    * If the result is > 180 min -> Caps at a maximum of 3 hours.
4.  **Historical Verification:** Checks `last_notification_time`. If the previous notification was sent less than 15 minutes ago, it forces a delay (Cooldown).
5.  **Night Block:** If there is less than 1 hour until sleep, no further alarms are scheduled for the day.

## 🚀 Installation

To run the project locally:

1.  Clone the repository:
    ```bash
    git clone [https://github.com/YourUsername/WaterReminder.git](https://github.com/YourUsername/WaterReminder.git)
    ```
2.  Open the project in **Android Studio** (Hedgehog or newer recommended).
3.  Wait for Gradle synchronization to complete.
4.  Run the app on an emulator or physical device (Min SDK: 26 - Android 8.0).

> **Note:** On Android 13+, the app will request permission to send notifications upon the first launch. This is required for the reminders to function.

## 📂 Project Structure

* `MainActivity.kt` - Entry point, notification channel configuration.
* `WaterScreen.kt` - Complete UI implementation using Jetpack Compose.
* `WaterViewModel.kt` - Business logic, state management, and database communication.
* `AlarmScheduler.kt` - The "Brain" of the app responsible for calculating notification times.
* `ReminderReceiver.kt` - BroadcastReceiver that triggers the notifications.
* `WaterDatabase` / `WaterDao` / `WaterEntity` - Data layer (Room).

## 📝 License

Created for educational and demonstration purposes.

---
