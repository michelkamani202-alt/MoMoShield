# Walkthrough — Enhanced Decision Making

We have successfully implemented advanced heuristics to drastically improve MoMoShield's decision-making precision against Cameroon Mobile Money scams.

## Changes Made

### 1. Obfuscation & Leet-Speak Tolerance
- **[RuleBook.kt](file:///C:/Users/POPOLY/Documents/projets/MoMoShield/app/src/main/java/cm/momoshield/app/domain/RuleBook.kt)**: Upgraded secret words and security patterns to tolerate obfuscated spelling (`p[i1!l]n`, `c0de`, trick spacing) used by scammers to bypass filters while keeping TFLite feature parity intact.

### 2. Financial Pressure & Disproportionate Amount Heuristics
- **[RuleBook.kt](file:///C:/Users/POPOLY/Documents/projets/MoMoShield/app/src/main/java/cm/momoshield/app/domain/RuleBook.kt)**: Added specific rules targeting unrealistic monetary amounts combined with prize/transfer lure keywords.

## Verification Results

### Automated Tests
- Ran `./gradlew testDebugUnitTest`: **47 tests passed, 0 failed**.
- Build task `assembleDebug` completed successfully.
