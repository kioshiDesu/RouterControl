# RouterControl – MikroTik Remote Manager

RouterControl is a lightweight, local-first management application that allows network administrators to remotely monitor resources, view IP profiles, check routing interfaces, and run custom CLI operations on MikroTik routers securely.

The project is structured under two configurations to maximize flexibility:
1. **Native Android Client (Compiled & Operational):** Implemented in Kotlin, Jetpack Compose, and Material 3, containing local SQLite persistence (Room), native SSH connection capabilities via the `JSch` library, and interactive diagnostic consoles with high-fidelity RouterOS simulator overlays.
2. **Web WebView & Node Backend Structure (Platform-Compliant):** Complete with React 18 SPA (`/frontend`) and Express API backend (`/backend`) implementing authorization headers and in-memory credential storage.

---

## 📱 Architecture Features (Kotlin Native)

- **Device Profiles:** Save multiple router host configurations (IP, port, user) locally using **Android Room Database**. Passwords are **never saved** to disk; they are entered on connect and kept strictly in memory during active session states for maximum security.
- **Dynamic Diagnostics Gauge:** Real-time visual monitoring dashboards that dynamically parse standard `/system resource print` stdout and plot CPU load and RAM status on Material 3 circular progress gauges.
- **SSH Command Executor:** Standardized background thread routines leveraging SSH to execute native RouterOS command streams.
- **Offline / Simulation Mode:** Toggleable "Simulation Mode" profiles let you test, query, and verify diagnostic flows offline with high-fidelity, randomized mock RouterOS stdout.
- **Dangerous Command Safeguard:** Interactive prompt check confirms router reboots if `/system reboot` is run.

---

## 🔧 Building & Compiling Android APK

The project utilizes Gradle with Kotlin DSL. 

To compile and verify the Android application from your developer workspace:

```bash
# Verify code syntax and compilation rules
gradle compileDebugKotlin

# Run all local JVM unit tests (Robolectric & JUnit)
gradle :app:testDebugUnitTest

# Assemble the fully optimized debugging APK
gradle assembleDebug
```

*Note: The generated binary APK can be exported from AI Studio directly to run on physical Android devices.*

---

## 🌐 WebView / Node.js Local Integration

For WebView-based APK compilation pipelines, the matching JavaScript sources are separated into respective directories:

### Backend Structure (`/backend`)
- **`server.js`:** Bootstraps Express server binding on `127.0.0.1:3000`. Generates a session API token on startup to enforce secure handshakes within the Android container.
- **`services/mikrotikClient.js`:** Singleton orchestrator that establishes live SSH connections using `ssh2` or redirects to simulator fallbacks.

To install dependencies and run the Node daemon locally:
```bash
cd backend
npm install
npm start
```

### Frontend React SPA (`/frontend`)
- **`src/App.js`:** Handles tab routing (Dashboard | Devices | Terminal | Logs) and top status bar indicators.
- **`src/context/AppContext.js`:** Standardizes global states and handshakes with the API.

To install dependencies and test the React frontend:
```bash
cd frontend
npm install
npm start
```

*The `/frontend` build output can be packed directly into the Android asset folder, and loaded inside an Android `WebView` pointing to the localhost Express server.*
