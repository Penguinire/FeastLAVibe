🍽️ Feast & Vibe
> *Find somewhere to eat, find something to do, and get there — all from one app.*
Feast & Vibe is a native Android app that brings restaurant discovery, events, maps, weather and personal favourites/reviews into a single experience. It is a final-year university project built by a small student team.
---
Table of Contents
About the App
Purpose
Who It's For
Technologies
Features
Project Status
Getting Started
Project Structure
Important Project Information
Team
Latest Updates
Documentation
---
About the App
Young adults deciding where to eat and what to do usually juggle several unconnected apps: one for restaurants, one for reviews, one for maps, one for weather, and another for events and tickets. Every switch loses context.
Feast & Vibe combines these into one flow:
"Where should I eat?" → "What's happening nearby?" → "Get me there."
Directions are intentionally handed off to Google Maps at the final step, rather than rebuilt in-app.
Purpose
Let users find restaurants by name, food category (e.g. "pizza") or area (e.g. "Hatfield") without needing a separate maps app.
Show places on a map alongside the user's current location.
Give enough restaurant detail to decide where to go.
Surface nearby events with a link to tickets.
Show a simple current-weather indicator.
Let returning users keep favourites and reviews.
Who It's For
The audience is primarily young adults in their twenties, though the app is public-facing.
Persona	Needs
The spontaneous socialiser	Fast area/category search, map view, opening status, quick favourites
The planner	Reliable event listings, event location on the map, ticket link, nearby restaurants
The regular	Favourites, past reviews, quick re-access to saved places
Technologies
Layer	Technology	Purpose
Platform	Android Studio, Kotlin	Native app development
UI	XML layouts, Fragments, Navigation Component, Material Components	Screens and navigation (no Jetpack Compose)
Lists	RecyclerView	Restaurant, event and favourite lists
Maps & Search	Mapbox Maps SDK, Mapbox Search SDK	In-app map, markers, place search
Events	Ticketmaster Discovery API	Event listings and details
Weather	Open-Meteo	Lightweight current-weather indicator
Backend	Firebase (Authentication, database)	Accounts, profile, favourites, reviews
Location	Fused Location Provider	User's current location
Directions	External Google Maps	Turn-by-turn navigation (hand-off)
Build	Gradle (Kotlin DSL), version catalog	Dependencies and build config
Build configuration: `minSdk 24` · `targetSdk 37` · `compileSdk 37` · Java 11 · Android Gradle Plugin 9.3.2 · package `com.example.feastvibe`
Features
Screens
Screen	Purpose
Login / Sign Up	Create an account or sign in
Explore (Home)	Search bar, category shortcuts, Nearby Picks, Popular Restaurants, Nightlife & Socials
Search	Recent searches and suggestions
Filters	Price range, cuisine/category, minimum rating, dietary options, venue amenities
Map	Map with current location, markers, info card and weather chip
Restaurant Details	Rating, price, opening status, contact, menu highlights, review summary, directions
Directions	Route overview and guidance steps (external Google Maps hand-off for navigation)
Events / Event Details	Browse events (This Week / Weekend / Month), details, tickets, directions
Favourites	Saved places, filterable by type
Profile	Account details, review/favourite/visited counts, log out
Reviews	Full list of a restaurant's reviews
Core design decisions
One shared `Place` model for every restaurant, whatever its source (Mapbox, dummy data, Firebase).
`PlaceCache` carries a real selected place between screens so it is never silently replaced by dummy data.
Autocomplete vs. full search: suggestions appear as the user types (debounced); a full search runs only when a suggestion is selected or the user submits a query.
Area vs. category search: "Hatfield" returns restaurants/bars/clubs in that area, not street addresses; "pizza" returns pizza places near the user.
Map entry states: opening the Map directly centres on the user (Pretoria as fallback) with nothing selected; "View on Map" from Restaurant Details centres on that restaurant. Unselected searches show up to ~5 markers.
Dummy data fallback keeps the app usable if a live API result is unavailable.
One responsibility per API so a fault in one service doesn't force a redesign of the rest.
Out of scope (by design)
In-app ordering/payments, turn-by-turn navigation, and AI recommendations.
Project Status
The app is under active development. This table reflects the repository as it currently stands, and should be updated as features land.
Area	Status
Project setup (Gradle, SDK config, dark theme, brand colours)	✅ Done
UI layouts for all main screens (Login, Sign Up, Explore, Search, Filters, Map, Restaurant Details, Directions, Events, Event Details, Favourites, Profile, Reviews)	✅ Done (static layouts with placeholder content)
Navigation between screens (Fragments / Navigation Component)	🔲 Not yet wired up
Firebase Authentication, profile, favourites, reviews	🔲 Planned
Mapbox map & search (`Place`, `PlaceCache`, autocomplete)	🔲 Planned
Location & directions hand-off	🔲 Planned
Ticketmaster events	🔲 Planned
Open-Meteo weather	🔲 Planned
> The screens currently show **placeholder text and sample data**; no live API or database is connected yet.
Getting Started
Prerequisites
Android Studio (recent stable version that supports AGP 9.3.x)
JDK 11 or newer (Android Studio's bundled JDK works)
Android SDK Platform 37 (install via Tools → SDK Manager)
An emulator or physical device running Android 7.0 (API 24) or higher
Git
Setup
```bash
# 1. Clone the repository
git clone <repository-url>
cd FeastVibe

# 2. Open in Android Studio (File → Open → select the FeastVibe folder)
#    Let Gradle sync finish.
```
Android Studio generates `local.properties` (your SDK path) automatically. It is git-ignored; never commit it.
Run
Select a device or emulator in the toolbar.
Press Run ▶ (or `Shift + F10`).
Or from the command line:
```bash
./gradlew assembleDebug        # build a debug APK (gradlew.bat on Windows)
./gradlew installDebug         # install on a connected device/emulator
./gradlew test                 # run unit tests
```
API keys and configuration (needed as integrations land)
These services need credentials once they are wired in. Do not hard-code or commit keys.
Service	What's needed
Mapbox	Public access token (and a secret downloads token for the SDK) with available quota
Ticketmaster	Discovery API key
Firebase	`google-services.json` for package `com.example.feastvibe` placed in `app/`
Open-Meteo	No key required
Store keys in your git-ignored `local.properties` (or another untracked file) and read them via `BuildConfig`/Gradle properties.
> **Troubleshooting tip:** a Mapbox `401 Not Authorized – Invalid Token` error doesn't always mean a code bug. During development it was caused by the account hitting its usage limit. Check the provider's dashboard before rewriting working code.
Project Structure
```
FeastVibe/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/feastvibe/   # Kotlin sources (MainActivity)
│       └── res/
│           ├── layout/                   # activity_*.xml screens, item_*.xml list rows, bottom_nav.xml
│           ├── drawable/                 # backgrounds for cards, chips, inputs, badges, rating bars
│           ├── values/                   # colors, strings, themes
│           └── values-night/             # dark-theme overrides
├── gradle/libs.versions.toml             # Version catalog
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```
Important Project Information
Master project: Feast & Vibe is the master application. Teammates' work is merged in, never swapped in wholesale.
Merge rules:
Compare incoming code and extract the useful logic; never overwrite outright.
Add only the specific Gradle dependency needed; don't copy whole build files.
Merge required destinations into the master navigation graph; don't replace it.
Keep one Firebase implementation and one `Place` model.
Reuse Ticketmaster API logic but keep the master's Events UI.
Keep Fused Location Provider + external Google Maps; don't introduce Mapbox Navigation.
Delivery priorities: one project → builds → major features work → fix crashes/navigation → fix API/database → fix UI → polish last.
Debug order: Launch → Navigation → Explore → Search → Mapbox → Location → Restaurant Details → Directions → Events → Weather → Login/Register → Profile → Favourites → Reviews → Filters.
Build troubleshooting order: my code → dependency → SDK → Gradle → JDK → Android Studio configuration.
Visual identity: dark theme with a pink primary (`#FF4D6D`).
Security: no secrets or `local.properties` in the repo; credentials handled via Firebase Auth rather than custom storage.
Team
Member	Area
Phungo (PM)	Main application, Explore/Home, Mapbox, integration
Fridah	Firebase: auth, login/register, profile, favourites, reviews; repository README
Bear	Events: Ticketmaster, event list/details, external links
Mfundo	Search: discovery, filters, recommendations
Qhayiya	Location & weather: directions, location, Open-Meteo; POE documentation
Latest Updates
> Keep this section current. Add the newest entry at the top.
Date	Update
Sep 2026	Project scaffolded (Gradle, SDK 37, version catalog). Dark theme and brand colour palette added. XML layouts built for all main screens, list items and bottom navigation.
Sep 2026	Research, Planning & Design Report completed (requirements, architecture, scope, integration plan).
Next up: wire navigation, then integrate Firebase, Mapbox, Ticketmaster and Open-Meteo following the merge rules above.
Documentation
Full requirements, research, architecture and design rationale are in the Feast & Vibe: Research, Planning & Design Report (`Feast_and_Vibe_Research_Planning_Design_Report.docx`). Consider adding it to a `docs/` folder in the repo.
---
Final-Year Project · Android · Kotlin · Mapbox · Firebase · Ticketmaster · Open-Meteo
