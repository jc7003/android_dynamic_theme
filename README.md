# Dynamic Theme (Android, XML theme tokens)

A sample app whose colors and corner radius come from a JSON theme downloaded from an API,
while all view styling stays in XML (theme tokens → theme attrs → styles).

## How it works

```
JSON ──▶ ThemeRepository ──▶ TokenThemeManager ──▶ in-memory resources.arsc ──▶ ResourcesLoader
                                                                         │ overrides values of
                                                                         ▼
                              @color/token_* , @dimen/token_corner_radius   (res/values/tokens.xml)
                                                                         ▲
                              ?attr/colorPrimary … shapeAppearanceCorner…   (res/values/themes.xml)
                                                                         ▲
                              styles / selectors / layouts                 ← no code changes
```

1. `res/values/tokens.xml` (+ `values-night/`) is the only place with raw values.
   `themes.xml` maps every Material attr to a token; layouts and styles use `?attr/...` only.
2. On startup `App` loads the last good theme from DataStore (`ThemeCache`) synchronously.
3. **API 30+** `TokenThemeManager` builds a resource table with the same resource ids
   (`ResourceTableBuilder`), wraps it in a `ResourcesLoader` (`TokenOverride`) and attaches it to
   every Activity in `onActivityPreCreated`, before `setContentView`. The table has a default
   entry (light palette) and a `night` entry (dark palette), so dark mode needs no extra code.
4. `MainActivity` refreshes from `GET /theme?name=…`. A new theme is cached, then emitted;
   live Activities are `recreate()`d because resources are read at inflate time.
5. No colors are set in code anywhere. **API < 30** has no `ResourcesLoader`, so those devices show
   the XML default tokens (screens are still recreated on a theme change).

Invalid responses (no valid `colors.light.primary`) are rejected; network errors keep the current theme.

## Adopting this in an existing token-based project

1. Make sure every color goes through a token: `?attr/x` → `@color/token_x`. Hex values in layouts,
   styles or drawables bypass the override.
2. Copy `theme/tokens/` (`ResourceTableBuilder`, `TokenOverride`, `TokenThemeManager`, `TokenRegistry`).
3. List your tokens in `TokenRegistry` (JSON field → `R.color.*` / `R.dimen.*`).
4. Call `TokenThemeManager.install(app, cachedTheme)` in `Application.onCreate` and feed later
   themes to `TokenThemeManager.onThemeChanged(theme)`.

### Adding a token
`tokens.xml` (+ `values-night`) → `PaletteDto`/`Palette` (+ `ThemeMapper`) → one line in `TokenRegistry`.

### Limits
- Only colors and dimens are overridden; fonts and drawable files are not.
- Token values may only vary by night mode. Other qualifiers (e.g. `values-sw600dp/tokens.xml`)
  are more specific than the override and would win.
- Colors cached in code (e.g. a `getColor()` stored in a field) don't update.
- A theme change needs `recreate()` (brief flicker; state is restored via `savedInstanceState`).
- Material 3 attrs that aren't mapped in `themes.xml` keep the library default color.

## Custom views

**No theme code needed.** Read colors from attrs at construction and they already hold the
JSON values. Example: `ui/widget/StatusBadgeView.kt`:

```kotlin
context.withStyledAttributes(attrs, R.styleable.StatusBadgeView, R.attr.statusBadgeStyle, R.style.Widget_App_StatusBadge) {
    bgPaint.color = getColor(R.styleable.StatusBadgeView_statusBadgeColor, Color.GRAY)
}
```
```xml
<style name="Widget.App.StatusBadge" parent="">
    <item name="statusBadgeColor">?attr/colorSecondary</item>  <!-- → @color/token_secondary -->
</style>
```
Rules:
- Colors come from attrs / theme attrs, never hex in code or XML.
- Resolve them in the constructor (or `onFinishInflate`); don't cache them in static/companion fields.
  A theme change `recreate()`s the screen, which rebuilds the view with the new tokens.
- Material's default text colors (`android:textColorPrimary`…) are mapped to tokens in `themes.xml`;
  map any other attr your views rely on, or it keeps the library's day/night default.
## JSON schema
```json
{
  "id": "ocean", "version": 3,
  "colors": {
    "light": { "primary": "#0061A4", "onPrimary": "#FFFFFF", "secondary": "...", "onSecondary": "...",
               "background": "...", "onBackground": "...", "surface": "...", "onSurface": "...",
               "error": "...", "onError": "..." },
    "dark":  { "...same keys, optional..." }
  },
  "shape": { "cornerRadiusDp": 12 }
}
```
Only `colors.light.primary` is required. Missing keys fall back to defaults, a missing `dark` reuses `light`.
Colors are `#RRGGBB` or `#AARRGGBB`.

## Use your real API
In `ServiceLocator.kt`: set `BASE_URL` and remove `.addInterceptor(mockInterceptor)`.
Mock responses live in `app/src/main/assets/themes/`.

## Build & test
```
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew installDebug
```
