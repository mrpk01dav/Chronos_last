# Chronos - Native Android Task Tracker & Smart Reminders

A production-ready native Android task tracker built with **Kotlin**, **Jetpack Compose**, **Room Database**, and **AlarmManager**.

## Features
- **Jetpack Compose & Material 3**: Dark charcoal UI designed for OLED displays.
- **Room Database (Local Storage)**: All tasks, subtasks, notes, and recurrence configurations persist across device reboots and app updates.
- **Exact Alarms & Doze Mode**: Schedules alarms using `AlarmManager.setExactAndAllowWhileIdle()` with wakeup capabilities.
- **Notification Actions**: Snooze 10m and Complete tasks directly from the Android notification shade.
- **Reboot Persistence**: Registered `BootReceiver` reschedules all pending task alarms on `BOOT_COMPLETED`.

## Building the APK & Testing
1. Unzip the project folder.
2. Open in **Android Studio Ladybug (2024.2+)** or newer.
3. Allow Gradle to sync.
4. Run on Android Emulator or physical device:
   ```bash
   ./gradlew installDebug
   ```
5. Build signed production APK:
   ```bash
   ./gradlew assembleRelease
   ```


## Build an APK using GitHub Actions (no Android Studio required locally)

1. Upload/push the contents of this project to a GitHub repository on the `main` branch.
2. Open the repository's **Actions** tab and select **Build Chronos APK**.
3. Choose **Run workflow** if it has not already run automatically.
4. Open the completed run and download the **Chronos-debug-apk** artifact.
5. Extract the downloaded artifact to get `app-debug.apk`, then transfer it to your Android phone and install it.

This workflow builds a **debug APK** for testing. It is not a Play Store release-signed APK.
