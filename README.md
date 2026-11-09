# Dynamic Theme（Android，XML Theme Tokens）

一個示範 App：**顏色與圓角從 API 下載的 JSON 主題動態套用**，而所有 View 的樣式仍然寫在 XML 中
（theme tokens → theme attrs → styles）。切換主題時不需要在程式碼中設定任何顏色，
Layout、Style、Selector 與自訂 View 都會自動吃到新的值，並且支援深色模式。

## Demo

<video src="docs/demo.webm" controls muted width="320"></video>

> 若影片無法在頁面上直接播放，請點此觀看：[docs/demo.webm](docs/demo.webm)

影片展示：從 API 取得不同主題後，整個畫面（按鈕、背景、文字、卡片圓角、自訂 View）即時切換配色與圓角。

## 特色

- 🎨 **伺服器驅動主題**：以 JSON 描述 light / dark 兩組配色與圓角，App 啟動或刷新時套用。
- 🧩 **純 XML 樣式**：Layout / Style 只使用 `?attr/...`，不需改寫既有畫面，也不在程式碼中 `setColor`。
- 🌙 **深色模式免額外程式碼**：覆寫的資源表同時包含 default 與 `night` 兩組值。
- 💾 **離線可用**：最後一次成功的主題快取在 DataStore，啟動時同步載入，避免閃爍。
- 🛡️ **容錯**：無效的回應會被拒絕，網路錯誤則保留目前主題。

## 運作原理

```
JSON ──▶ ThemeRepository ──▶ TokenThemeManager ──▶ in-memory resources.arsc ──▶ ResourcesLoader
                                                                         │ 覆寫以下資源的值
                                                                         ▼
                              @color/token_* , @dimen/token_corner_radius   (res/values/tokens.xml)
                                                                         ▲
                              ?attr/colorPrimary … shapeAppearanceCorner…   (res/values/themes.xml)
                                                                         ▲
                              styles / selectors / layouts                 ← 不需修改程式碼
```

1. `res/values/tokens.xml`（+ `values-night/`）是唯一存放原始數值的地方。
   `themes.xml` 把每個 Material attr 對應到 token；layouts 與 styles 只使用 `?attr/...`。
2. 啟動時 `App` 會從 DataStore（`ThemeCache`）同步載入上一次成功的主題。
3. **API 30+**：`TokenThemeManager` 以相同的 resource id 建立一份資源表（`ResourceTableBuilder`），
   包成 `ResourcesLoader`（`TokenOverride`），並在 `onActivityPreCreated`（`setContentView` 之前）
   掛到每個 Activity 上。資源表包含 default（淺色）與 `night`（深色）兩組，因此深色模式無需額外處理。
4. `MainActivity` 透過 `GET /theme?name=…` 更新主題。新主題先寫入快取再發送；
   由於資源是在 inflate 時讀取，現存的 Activity 會被 `recreate()`。
5. 程式碼中完全沒有設定顏色。**API < 30** 沒有 `ResourcesLoader`，這些裝置會顯示 XML 預設 token
   （主題變更時畫面仍會 recreate）。

無效回應（沒有合法的 `colors.light.primary`）會被拒絕；網路錯誤會保留目前主題。

## 套用到既有的 Token 專案

1. 確認所有顏色都經過 token：`?attr/x` → `@color/token_x`。寫死在 layout、style 或 drawable 的
   hex 值會繞過覆寫。
2. 複製 `theme/tokens/`（`ResourceTableBuilder`、`TokenOverride`、`TokenThemeManager`、`TokenRegistry`）。
3. 在 `TokenRegistry` 列出你的 token（JSON 欄位 → `R.color.*` / `R.dimen.*`）。
4. 在 `Application.onCreate` 呼叫 `TokenThemeManager.install(app, cachedTheme)`，
   之後的主題交給 `TokenThemeManager.onThemeChanged(theme)`。

### 新增一個 Token
`tokens.xml`（+ `values-night`）→ `PaletteDto` / `Palette`（+ `ThemeMapper`）→ 在 `TokenRegistry` 加一行。

### 限制
- 只覆寫 colors 與 dimens；字型與 drawable 檔案不會被覆寫。
- Token 只能依深色模式變化。其他 qualifier（例如 `values-sw600dp/tokens.xml`）比覆寫更具體，會優先生效。
- 在程式碼中快取的顏色（例如把 `getColor()` 存在欄位裡）不會更新。
- 主題變更需要 `recreate()`（會有短暫閃爍；狀態透過 `savedInstanceState` 還原）。
- 未在 `themes.xml` 對應的 Material 3 attr 會維持函式庫預設顏色。

## 自訂 View

**不需要任何主題相關程式碼。** 在建構時從 attrs 讀取顏色，取得的就已經是 JSON 的值。
範例：`ui/widget/StatusBadgeView.kt`

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

規則：
- 顏色一律來自 attrs / theme attrs，不要在程式碼或 XML 中寫死 hex。
- 在建構子（或 `onFinishInflate`）中解析；不要快取在 static / companion 欄位。
  主題變更會 `recreate()` 畫面，View 會以新的 token 重建。
- Material 預設文字顏色（`android:textColorPrimary`…）已在 `themes.xml` 對應到 token；
  你的 View 若依賴其他 attr，請一併對應，否則會沿用函式庫的日/夜預設值。

## JSON 格式

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

只有 `colors.light.primary` 為必填。缺少的欄位使用預設值，缺少 `dark` 時沿用 `light`。
顏色格式為 `#RRGGBB` 或 `#AARRGGBB`。

## 串接真實 API

在 `ServiceLocator.kt` 設定 `BASE_URL`，並移除 `.addInterceptor(mockInterceptor)`。
Mock 回應放在 `app/src/main/assets/themes/`。

## 建置與測試

```
./gradlew assembleDebug testDebugUnitTest lintDebug
./gradlew installDebug
```
