# Fix Layout and Code Reference Errors

The user reported errors in layouts (missing widths/lengths) and code (failed references). Analysis shows several missing IDs in layout files that are referenced in Kotlin adapters and fragments, an empty bottom navigation menu, and some misaligned IDs.

## User Review Required

> [!IMPORTANT]
> I will be adding missing IDs to several layout files and populating the `bottom_nav_menu.xml` to match the navigation graph. I will also check for any tags missing mandatory `layout_width` and `layout_height` attributes.

## Proposed Changes

### [Layouts]

#### [MODIFY] [item_nearby_card.xml](file:///C:/Users/User/AndroidStudioProjects/FeastVibe/app/src/main/res/layout/item_nearby_card.xml)
- Add IDs: `tv_card_name`, `tv_card_meta`, `tv_card_rating` to match `NearbyAdapter.kt`.

#### [MODIFY] [activity_map.xml](file:///C:/Users/User/AndroidStudioProjects/FeastVibe/app/src/main/res/layout/activity_map.xml)
- Add IDs: `card_place`, `tv_map_name`, `tv_map_meta`, `tv_map_status`, `tv_map_rating` to match `MapFragment.kt`.

#### [MODIFY] [activity_restaurant_detail.xml](file:///C:/Users/User/AndroidStudioProjects/FeastVibe/app/src/main/res/layout/activity_restaurant_detail.xml)
- Move `iv_detail_back` from the gallery image to the actual back button icon.

#### [MODIFY] [bottom_nav_menu.xml](file:///C:/Users/User/AndroidStudioProjects/FeastVibe/app/src/main/res/menu/bottom_nav_menu.xml)
- Add menu items for `exploreFragment`, `mapFragment`, `favouritesFragment`, `eventsFragment`, and `profileFragment`.

### [Navigation]

#### [MODIFY] [nav_graph.xml](file:///C:/Users/User/AndroidStudioProjects/FeastVibe/app/src/main/res/navigation/nav_graph.xml)
- Ensure all fragment IDs match the menu and code references (already appears correct, but will double check consistency).

### [Code]

#### [MODIFY] [MainActivity.kt](file:///C:/Users/User/AndroidStudioProjects/FeastVibe/app/src/main/java/com/example/feastvibe/MainActivity.kt)
- Ensure all imports and references are resolved (should be fine once resources are fixed).

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure all resources compile and all code references are resolved.

### Manual Verification
- Deploy to device/emulator and verify that the bottom navigation works and that data is correctly bound in `NearbyAdapter` and `MapFragment`.
