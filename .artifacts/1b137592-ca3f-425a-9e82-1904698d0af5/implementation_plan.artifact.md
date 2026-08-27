# Implementation Plan: Transform UI to Real Material 3

This plan outlines the steps to modernize the CloudPlay app's UI by fully adopting Material 3 (M3) design principles, including dynamic color support, M3 component styling, and updated typography.

## User Review Required

> [!IMPORTANT]
> This change will significantly alter the look and feel of the app to match modern Android standards.
> Dynamic color will be enabled by default on supported devices (Android 12+), which means the app's primary color will match the user's wallpaper.

## Proposed Changes

### Foundation & Application

#### [MODIFY] [CloudStreamApp.kt](file:///C:/Users/greyd/StudioProjects/CloudPlay/app/src/main/java/com/lagradost/cloudstream3/CloudStreamApp.kt)
- Initialize `DynamicColors` to support system-wide dynamic color on Android 12+.

### Themes & Styles

#### [MODIFY] [styles.xml](file:///C:/Users/greyd/StudioProjects/CloudPlay/app/src/main/res/values/styles.xml)
- Update `AppTheme` to strictly follow Material 3 guidelines.
- Refactor custom styles (`NiceButton`, `WhiteButton`, `BlackButton`, etc.) to inherit from Material 3 widget styles.
- Replace custom color attributes with Material 3 theme attributes (e.g., `?attr/colorSurface`, `?attr/colorPrimary`).
- Add Material 3 typography tokens.

#### [MODIFY] [colors.xml](file:///C:/Users/greyd/StudioProjects/CloudPlay/app/src/main/res/values/colors.xml)
- Update color definitions to align with Material 3 naming conventions where appropriate.

#### [MODIFY] [attrs.xml](file:///C:/Users/greyd/StudioProjects/CloudPlay/app/src/main/res/values/attrs.xml)
- Ensure all necessary Material 3 attributes are defined or mapped.

### Layouts

#### [MODIFY] [fragment_home.xml](file:///C:/Users/greyd/StudioProjects/CloudPlay/app/src/main/res/layout/fragment_home.xml) (and others)
- Replace `androidx.cardview.widget.CardView` with `com.google.android.material.card.MaterialCardView`.
- Update components to use the new M3 styles.

### Navigation

#### [MODIFY] [MainActivity.kt](file:///C:/Users/greyd/StudioProjects/CloudPlay/app/src/main/java/com/lagradost/cloudstream3/MainActivity.kt)
- Refine `BottomNavigationView` and `NavigationRailView` to use M3 styling and behaviors.

## Verification Plan

### Automated Tests
- Build the project to ensure no resource conflicts or missing attributes.
- `gradlew assembleDebug`

### Manual Verification
- Deploy the app to an Android 12+ device (or emulator) to verify dynamic color support.
- Check various screens (Home, Search, Settings) to ensure M3 components (Buttons, Cards, Chips) are rendered correctly.
- Verify that the dark/light mode switching works as expected with M3 themes.
