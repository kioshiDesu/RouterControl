# Architecture Overview - MikroTik Remote Manager

MikroTik Remote Manager is a multi-tier administration app supporting both a native Android (Jetpack Compose + Room SQLite + SSH) app and a React Web app accompanied by a local Node.js WebSocket/REST service.

## 1. Multiple Saved Device Profiles

Device Profiles allow managing connection parameters for different physical or simulated routers.
- **Passwords are NEVER stored on disk**: We strictly persist connection metadata (`host`, `port`, `username`, `name`, `isDemo`, `lastConnected`). This is handled in the frontend via client-side storage, and in the Android App via the local **Room SQLite** Database.
- **In-Memory Sessions**: When connecting to a router, the password is provided by the user in a safe interactive dialog, kept purely in runtime memory for the duration of the connection, and evicted instantly when disconnecting or on app shutdown.

## 2. Real-Time Terminal & WebSockets

To enable responsive, real-time command output, a custom WebSocket pipeline is established for the active terminal session.

```
┌─────────────────┐                       ┌────────────────┐
│ React Frontend  │ <--- WebSocket ---->  │ Node.js Server │
│ (Terminal.jsx)  │      (Real-time)      │  (server.js)   │
└─────────────────┘                       └────────────────┘
                                                  │
                                              SSH Tunnel
                                                  │
                                                  ▼
                                          ┌────────────────┐
                                          │ MikroTik Board │
                                          └────────────────┘
```

- **WebSocket Endpoint**: `ws://127.0.0.1:3000/api/terminal-ws`
- **Security Check**: Handshake upgrades require an authorized `token` query param matched against the secure session-specific runtime API token and the target `deviceId`.
- **Command Streaming**: Incoming commands are routed directly to the underlying SSH stream or simulated loop. Rather than waiting for execution to fully terminate (as in the legacy HTTP API), the server subscribes to `'data'` chunks and pipes them as they arrive back to the socket, ensuring a true live CLI terminal feel.
