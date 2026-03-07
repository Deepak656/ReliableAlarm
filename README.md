# ReliableAlarm

A free, open-source Android alarm app that actually gets you out of bed. Built in Kotlin.

Inspired by apps like Alarmy — but completely free, no paywalls, no subscriptions, and fully open source.

---

## Why ReliableAlarm

Most alarm apps let you dismiss with a single tap. ReliableAlarm forces you to complete a wake task before the alarm stops — making it physically impossible to fall back asleep without getting your brain and body moving first.

---

## Features

**Wake Tasks** — assign one or more tasks to any alarm. All tasks are fully configurable and previewable before saving.

- **Math Challenge** — solve arithmetic problems to dismiss. Configure difficulty and number of problems.
- **Walking Challenge** — hit a step count target. Configure required steps.
- **Shake Challenge** — shake your phone for a set duration. Configure intensity and duration.
- **QR / Barcode Scan** — scan a specific QR code (e.g. one placed in your bathroom). Configure number of codes required.
- **Typing Challenge** — type out a paragraph. Configure length.
- **Tap Challenge** — tap the screen a set number of times. Configure tap count.
- **Color Balls** — sort colored balls across rounds. Configure number of rounds.

**Multiple tasks per alarm** — stack tasks so the alarm only dismisses after all are completed.

**Task Preview** — try any task before saving the alarm so you know exactly what you're signing up for.

**Streak Tracking** — tracks your wake consistency per alarm. See your current streak, best streak, success rate, and a 90-day history grid.

**Reliability Configuration** — fine-grained controls for alarm delivery including dual scheduling (AlarmManager + WorkManager), foreground service, volume escalation, auto re-ring, boot persistence, and more.

**Repeat Schedule** — set alarms to repeat on specific days of the week.

---

## Tech Stack

- **Language:** Kotlin
- **Min SDK:** 26 (Android 8.0)
- **Architecture:** Single-activity with repository pattern
- **Alarm scheduling:** AlarmManager + WorkManager (dual mode)
- **Storage:** SharedPreferences (JSON serialization)
- **UI:** Material Design 3, ConstraintLayout, RecyclerView

---

## Getting Started

```bash
git clone https://github.com/Deepak656/ReliableAlarm.git
```

Open in Android Studio, sync Gradle, and run on any device or emulator with Android 8.0+.

No API keys or external services required. Works fully offline.

---

## Project Structure

```
app/src/main/java/com/reliablealarm/app/
├── domain/          # Alarm + Streak repositories, scheduler, models
├── ui/              # Activities, adapters, bottom sheets
│   └── config/      # Per-task configuration screens
└── waketasks/       # Task logic, task types, config models
```

---

## Contributing

PRs are welcome. If you're adding a new wake task, follow the pattern in `waketasks/` and add a corresponding config activity under `ui/config/`.

Open an issue before starting large changes.

---

## License

MIT License. Free to use, modify, and distribute.

---

## Comparison with Alarmy

| Feature | ReliableAlarm | Alarmy |
|---|---|---|
| Price | Free | Freemium (most tasks paywalled) |
| Wake tasks | 7 | 10+ |
| Multiple tasks per alarm | ✓ | Paid |
| Task preview | ✓ | Limited |
| Open source | ✓ | ✗ |
| Streak tracking | ✓ | ✓ |
| Offline | ✓ | ✓ |