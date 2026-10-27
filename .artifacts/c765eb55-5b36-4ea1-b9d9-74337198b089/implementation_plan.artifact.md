# Implementation Plan — MoMoShield Professional Upgrade

Enhance MoMoShield into a purely professional, polished Android application with modern Material 3 styling, quick-test scam presets, filterable history, polished empty states, and transparent hybrid score explainability.

## User Review Required

> [!NOTE]
> All changes are 100% local, preserving user privacy and offline capability while significantly improving UI polish and UX.

## Proposed Changes

### Theme & Design System
#### [MODIFY] [Theme.kt](file:///C:/Users/POPOLY/Documents/projets/MoMoShield/app/src/main/java/cm/momoshield/app/ui/theme/Theme.kt)
- Upgrade Material 3 color schemes (`LightColors`, `DarkColors`) with professional security/finance color palettes (deep navy primary, refined surface tones, rich sémantic colors).

### Analyze Screen & Presets
#### [MODIFY] [AnalyzeScreen.kt](file:///C:/Users/POPOLY/Documents/projets/MoMoShield/app/src/main/java/cm/momoshield/app/ui/screens/AnalyzeScreen.kt)
- Add quick-test scam preset chips (e.g. "Faux gain MTN", "Erreur de transfert Orange", "Demande PIN").
- Enhance `ResultCard` to display a clear **Hybrid Score Breakdown** (Rule Engine score vs. TFLite model probability).
- Add quick action to copy or report detected numbers.

### History Screen & Filters
#### [MODIFY] [HistoryScreen.kt](file:///C:/Users/POpoly/Documents/projets/MoMoShield/app/src/main/java/cm/momoshield/app/ui/screens/HistoryScreen.kt)
- Add risk-level filter chips (All, High, Medium, Low).
- Polish empty state with an icon and professional message.

### Reported Numbers & Tips Screens
#### [MODIFY] [ReportedScreen.kt](file:///C:/Users/POPOLY/Documents/projets/MoMoShield/app/src/main/java/cm/momoshield/app/ui/screens/ReportedScreen.kt)
- Polish empty state with professional icon/placeholder.

## Verification Plan

### Automated Tests
- Run `./gradlew testDebugUnitTest` to ensure all unit tests and parity tests pass successfully.
- Run `app:assembleDebug` build check.

### Manual Verification
- Review Compose UI components or deploy via Android Studio.
