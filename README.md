# LoopHabit

LoopHabit is a modern, premium habit tracking and focus application built for Android using Jetpack Compose, Material Design 3, Room Database, and Supabase integration. It helps you build healthy habits and dedicate uninterrupted time to achieve your goals.

## 🚀 Key Features

* **Today's Loop (Swipe Stack)**: A swipeable card stack of today's habits. Swipe right to mark a habit as completed, and swipe left to skip/cycle to the next card.
* **Focus Mode**:
  * **Countdown Timer & Stopwatch**: Dedicate focused intervals to specific habits with clean circular progress visualizers.
  * **Custom Timer Durations**: Choose preset durations (1m, 15m, 25m, 45m, 60m) or tap the countdown display or "Custom" pill to set any custom duration between 1 and 999 minutes.
* **Insights & Log History**:
  * Monthly view calendar and Year-At-A-Glance grid tracking completed logs.
  * Streak statistics, productivity heatmaps, and focus duration analytics charts.
  * Milestone achievements system to celebrate consistency landmarks.
* **Lucide-Style Premium Aesthetic**:
  * Modern, unified Outlined Icons (reminiscent of Lucide icons) replacing traditional emojis across performance stats, achievement headers, and focus widgets.
* **Local & Auto-Backup**: Export/import database records locally, or configure automated background backups to a custom directory.
* **In-App Updates**: Check, download, and install application updates directly from GitHub Releases without needing to reinstall the APK manually.

---

## 🛠️ Technology Stack

* **UI Framework**: Jetpack Compose, Material Design 3 components, and extended outlined vector graphics.
* **Local Storage**: Room Database for relational logging history, and DataStore Preferences for session states.
* **Widgets**: Jetpack Glance for high-performance home screen widgets.
* **Background Worker**: WorkManager handling automated data syncs and backup workflows.
* **Sync Provider**: Supabase SDK (Postgrest, Auth, Realtime).

---

## 💻 Getting Started

### Prerequisites

* Android Studio (Koala or newer recommended)
* JDK 17+
* Android device or emulator running API 24 (Nougat) or higher

### Building the Project

Open the project in Android Studio, or compile it from the command line using the Gradle Wrapper:

```bash
# Compile and build debug APK
./gradlew assembleDebug

# Compile Kotlin classes and run static checks
./gradlew compileDebugKotlin
```

The generated APK will be available at:
`app/build/outputs/apk/debug/LoopHabit-debug.apk`
