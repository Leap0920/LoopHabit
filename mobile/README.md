# LoopHabit (React Native)

Expo + React Native port of the LoopHabit Android app.

## Run

From repo root:

```bash
npm run dev          # opens web in the browser
npm run web          # same
npm run android      # Expo Go / emulator
```

Or from this folder:

```bash
npm run web          # http://localhost:8082 → real UI
npm start            # Expo menu (press a / i / w)
```

**Browser:** open [http://localhost:8082](http://localhost:8082) after `npm run web` / `npm run dev`.  
You should see the LoopHabit UI (HTML), not a JSON manifest.

## What’s ported

| Feature | Status |
|--------|--------|
| Today swipe loop (complete / skip) | ✅ |
| Add / delete habits | ✅ |
| Focus timer + stopwatch | ✅ |
| Todos | ✅ |
| Insights (streak, calendar, focus total) | ✅ |
| Dark mode | ✅ |
| Local SQLite (Room schema equivalent) | ✅ |
| Supabase auth / sync | ⏳ later |
| Home screen widget | ⏳ later |
| Background focus foreground service | ⏳ later |
| JSON backup export/import | ⏳ later |
| GitHub in-app updates | ⏳ later |

The original Jetpack Compose Android project remains at the repository root (`app/`, `gradlew`, etc.).
