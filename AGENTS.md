# AGENTS.md — instructions for AI assistants

This repository has two parts:

| Part | Where | Use it to |
|---|---|---|
| **Design system** | [`design/`](design/) | Design or build new screens that look like Tivi Home (any platform) |
| **Android TV launcher** | [`app/`](app/), `*.ps1` | Build and install the real home screen on an Android TV / TV box |

## Job 1: design something in the Tivi Home style

Read, in order:

1. [`design/DESIGN.md`](design/DESIGN.md) — principles, layout grid, focus states, components, D-pad rules.
2. [`design/tokens/tokens.json`](design/tokens/tokens.json) — exact values; [`design/tokens/tokens.css`](design/tokens/tokens.css)
   is the same as CSS variables.
3. [`design/reference/`](design/reference/) — working HTML per screen (`home.html`, `apps.html`, `settings.html`,
   `child-lock.html?page=pick|active|pin|overlay`), shared `style.css`, icons in `icons.js`, Vietnamese lunar calendar in
   `lunar.js`.
4. [`design/screenshots/`](design/screenshots/) — 1280×720 renders of the reference pages; check your result against them.

Rules:

- Fixed 960×540 dp stage (1 dp = 1 CSS px), scaled uniformly to the screen (`design/reference/stage.js`). No responsive
  reflow.
- Only the colors, radii, font sizes and spacing in `tokens.json`. Background is one of the three mesh themes.
- Surfaces are glass: translucent white fill + 1dp translucent white border, 18dp corners. No shadows, no real blur.
- One focus style everywhere: cream `#F3EEE8` fill, 3dp `#8EE08F` ring, `#15151C` text, slight scale-up. Amber
  `#FFC857` ring = item being moved.
- Roboto; big numbers Light (clock 138dp, temperature 54dp); labels Bold/Medium; content text ≥ 12dp.
- Remote control only: ◀ ▶ ▲ ▼ / OK / Back, hold OK for a context menu, key hint line at the bottom of sub-screens.
- Copy is short, friendly Vietnamese.
- Reuse existing components (glass card, dock tile, choice card, settings row, PIN pad) before inventing new ones.

## Job 2: build and install the launcher on a TV

Follow [README.md](README.md) ("Cài lên TV"). Safety rules — do not skip:

- **Back up first** (`pm list packages -f`, `pm list packages -d`, `settings list global|secure|system`) into `backup/`
  (git-ignored: it contains serial numbers).
- **Disable, never uninstall** system apps: `pm disable-user --user 0 <pkg>`, one package per shell call. Keep the TV
  input/HDMI services, input method, Bluetooth/remote pairing, Play Store and Play Services.
- **Home guard**: make Tivi Home the default (`cmd package set-home-activity com.tivibox.home/.MainActivity`), press
  Home and confirm with `dumpsys activity activities | grep mFocusedApp` **before** disabling the old launcher. Ask the
  person before changing Home or disabling anything.
- Never open factory/test packages (names with factory, test, fac menu) — inspect with `dumpsys package` only.
- Child lock needs "display over other apps": `install.ps1` grants it and runs `appops write-settings` so it survives a
  reboot or power cut.
- Never commit `debug.keystore`, `build/`, `backup/` or screenshots of the person's TV.

## Code map (app)

| File | Role |
|---|---|
| `MainActivity.java` | Home: clock, date + lunar line, calendar, weather, dock, child-lock button |
| `CalendarCardView.java`, `LunarCalendar.java` | Month grid with lunar days (Ho Ngoc Duc algorithm, UTC+7) |
| `WeatherCardView.java`, `Weather.java` | Open-Meteo forecast + geocoding (no API key) |
| `AppsActivity.java`, `AppCatalog.java`, `DockEditor.java` | Apps grid, dock entries, ordering, add/remove |
| `SettingsActivity.java` | "Cài đặt giao diện": greeting, weather place, theme, dock, order reset |
| `ChildLock*.java`, `LockService.java`, `LockScreenView.java`, `PinPadView.java`, `StepperView.java` | Child lock |
| `KeyboardService.java`, `KeyboardView.java`, `Telex.java`, `VietTone.java` | "Bàn phím Tivi": themed QWERTY system keyboard with Telex |
| `Glass.java`, `Glyphs.java`, `MeshBackgroundView.java`, `TileView.java`, `ChoiceView.java`, `RoundButton.java` | Drawing primitives of the design system |
