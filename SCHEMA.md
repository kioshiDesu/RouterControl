# Schema & API Specifications

This document outlines the schemas and communication payloads for Device Profiles, Commands, and WebSocket-based Real-time terminals.

## 1. Device Profile Model

Each profile stores Connection Metadata without a password.

### Field Mapping

| Attribute | SQLite (Room) / LocalStorage Type | Description |
|---|---|---|
| `id` | `String` (UUID / Timestamp) | Primary Key identifying the router profile |
| `name` | `String` | Human-readable alias (e.g. "Main hEX Board") |
| `host` | `String` | Hostname or IP address |
| `port` | `Int` (Default: 22) | Target port |
| `username`| `String` | SSH connection login user |
| `lastConnected` | `String?` (ISO-8601 or Null) | Last successful connection timestamp |
| `isDemo` | `Boolean` (Default: false) | Offline/Mock simulation flag |

---

## 2. API Endpoints

### REST APIs (HTTP / JSON)

All HTTP endpoints require the authorized Header `x-app-token`.

#### `POST /api/connect`
Establish memory-based connection.
- **Request Body**:
  ```json
  {
    "id": "1721200000000",
    "name": "My Router",
    "host": "192.168.88.1",
    "port": 22,
    "username": "admin",
    "password": "RouterPassword123",
    "isDemo": false
  }
  ```
- **Response**:
  ```json
  {
    "error": false,
    "message": "Successfully established SSH session."
  }
  ```

#### `POST /api/connect/disconnect`
Terminate an active session and clear associated buffers.
- **Request Body**: `{"id": "1721200000000"}`
- **Response**: `{"error": false, "message": "Successfully disconnected."}`

---

## 3. Real-Time WebSocket Protocol

### Connection URL
`ws://127.0.0.1:3000/api/terminal-ws?token=<localApiToken>&deviceId=<deviceId>`

### Client-to-Server Payloads (JSON)
The client sends standard RouterOS commands as they are entered:
```json
{
  "command": "/ip address print"
}
```

### Server-to-Client Payloads (JSON)
The server streams response chunks or error events back in real-time.

#### 1. Streaming Chunk Message
```json
{
  "type": "chunk",
  "data": "0   192.168.88.1/24    192.168.88.0    bridge\n"
}
```

#### 2. Error Message
```json
{
  "type": "error",
  "data": "Command syntax error"
}
```

#### 3. Execution Completed Message
```json
{
  "type": "close",
  "code": 0
}
```
