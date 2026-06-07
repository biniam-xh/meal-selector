# Meal Selector

An Android app that helps you pick what to eat. It shows one meal at a time and cycles through your list in order when you tap **Next Meal**. Multiple people can share the same list in real time via Firebase.

## Features

- **Ordered rotation** — meals advance A → B → C → … → back to A
- **Real-time sync** — everyone in the same group sees the same meal, lock state, and list
- **Create or join a group** — share a 6-character code with family or friends
- **Lock toggle** — freeze the current meal so nobody rotates by accident
- **Add meals in Settings** — new meals are appended to the end of the list
- **Position preserved** — if you're viewing meal B and add D, you stay on B; D appears after C in the rotation

## Default meals

Pizza, Pasta, Tacos, Sushi, Burgers, Salad, Stir Fry

## Requirements

- [Android Studio](https://developer.android.com/studio) (Hedgehog or newer recommended)
- JDK 17 (bundled with Android Studio)
- Android SDK with API 34
- A [Firebase](https://console.firebase.google.com/) project (free tier is enough)

## Firebase setup (required before first run)

1. Go to [Firebase Console](https://console.firebase.google.com/) and **Create a project**
2. Click **Add app → Android**
   - Package name: `com.mealselector.app`
   - Download `google-services.json`
3. Place the file here: `app/google-services.json`
   - See `app/google-services.json.example` for the expected structure
4. In Firebase Console, enable **Firestore Database** (Start in **test mode** for quick setup, then deploy rules below)
5. Enable **Authentication → Sign-in method → Anonymous**
6. Deploy security rules from `firebase/firestore.rules`:
   - Firebase Console → Firestore → **Rules** tab, paste the contents, click **Publish**
   - Or use Firebase CLI: `firebase deploy --only firestore:rules`

### Sharing with others

1. One person taps **Create new group** and gets a code like `ABC123`
2. Share that code (text, WhatsApp, etc.)
3. Others tap **Join group** and enter the same code
4. Everyone sees the same meal list, current meal, and lock state instantly

## Run the app

1. Complete [Firebase setup](#firebase-setup-required-before-first-run)
2. Open Android Studio
3. **File → Open** and select this project folder
4. Wait for Gradle sync to finish
5. Connect a device or start an emulator (needs internet for sync)
6. Click **Run**

### OneDrive / build errors

If you see `AccessDeniedException` or missing `redirect.txt` under `app\build`, that is usually OneDrive locking Gradle output. This project uses a **Windows junction** so:

- Android Studio and Gradle use the normal path: `app\build\...`
- Actual files are stored locally at: `%LOCALAPPDATA%\MealSelectorBuild\app\`

After pulling this fix:

1. **File → Sync Project with Gradle Files**
2. **Build → Clean Project**
3. **Build → Rebuild Project**, then Run

If problems persist, move the project out of OneDrive (e.g. `C:\Projects\Meal Selector`).

## Build from command line

```bash
# Windows
gradlew.bat assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## How rotation works

| List        | Current | Action   | Result              |
|-------------|---------|----------|---------------------|
| A, B, C     | B (index 1) | Add D | Still B; order is A, B, C, D |
| A, B, C, D  | B       | Next     | C                   |
| A, B, C, D  | D       | Next     | A (wraps around)    |

## Firestore data model

```
groups/{6-char-code}
  meals: ["Pizza", "Pasta", ...]
  currentIndex: 0
  isLocked: false
```
