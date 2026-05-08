# ChatLite - Java Socket Chat System

ChatLite is a minimal real-time chat application built with Java sockets and Swing GUI.
This is a university project for the Networks 1 course.
It includes a multi-client TCP server and a desktop client that supports:

- User login with username and password over a persistent TCP connection
- Chat rooms (join, leave, room listing)
- Real-time room messaging
- Private messaging between online users
- Online users list
- Client-side message search/filtering
- Server monitoring GUI with logs and user management

---

## Project Structure

- `server/` - ChatLite server source code and server GUI
- `client/` - ChatLite client source code and client GUI
- `docs/REPORT_TEMPLATE.md` - ready-to-fill report template (3-5 pages)
- `docs/DEMO_SCRIPT.md` - 3-minute video scenario
- `docs/TEST_CHECKLIST.md` - final verification checklist
- `sample-users-rooms.txt` - sample users and room names for testing
- `build.gradle.kts` / `settings.gradle.kts` - Gradle multi-module build files
- `gradlew.bat` / `gradlew` - Gradle wrapper scripts

---

## Requirements

- Windows 10/11 (or any OS with Java support)
- Java Development Kit (JDK) 17
- Git (optional, for cloning/version control)

No manual Gradle installation is required if you use the included Gradle Wrapper.

---

## Install Java 17 on Windows

### Option A (recommended): Winget

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
```

### Option B: Manual installer

1. Download JDK 17 from [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=17).
2. Install using default options.
3. Set `JAVA_HOME` to your JDK path (for example: `C:\Program Files\Eclipse Adoptium\jdk-17.x.x`).
4. Add `%JAVA_HOME%\bin` to your system `Path`.

### Verify installation

```powershell
java -version
javac -version
```

Both commands must report Java 17.

---

## Build the Project

From the project root:

```powershell
.\gradlew.bat clean build
```

If the build succeeds, all modules compile correctly.

---

## Run the Server

```powershell
.\gradlew.bat :server:run
```

Expected result:

- Server starts on TCP port `2525`
- Server GUI window opens (`ChatLite Server Console`)

![ChatLite Server Console](server/Screenshot%202026-05-08%20143935.png)

---

## Run the Client

Open a second terminal in the same project folder:

```powershell
.\gradlew.bat :client:run
```

You can start multiple client instances to test real-time communication.

![ChatLite Client](client/Screenshot%202026-05-08%20143609.png)

---

## Quick Test Plan

1. Start the server.
2. Launch two client windows.
3. Choose two registered usernames and enter their passwords.
4. In both clients, join the same room (for example `general`).
5. Send room messages and verify both clients receive them.
6. Send a private message from one user to the other.
7. Check users list and room list behavior.
8. Leave a room and verify room messages stop for that client.
9. Quit clients and verify server logs update.

---

## Protocol Overview

The application uses a text-based protocol over TCP (`\n` terminated lines).

Main commands:

- `HELLO <username>`
- `JOIN <room>`
- `MSG <room> <message>`
- `PM <username> <message>`
- `USERS`
- `ROOMS`
- `LEAVE <room>`
- `QUIT`

Extended status command implemented for GUI controls:

- `STATUS ACTIVE|BUSY|AWAY` (alias `SETSTAT`)

Example responses/events:

- `200 WELCOME`
- `210 JOINED <room>`
- `211 SENT`
- `212 PRIVATE SENT`
- `213U <username> <status>`
- `214 <room>`
- `ROOMMSG <room> <username> <time> <message>`
- `PMFROM <username> <time> <message>`
- `221 BYE`

---

## Notes

- Default server port is `2525`.
- Use the server GUI to monitor users, logs, and settings.
- If login fails, verify user credentials in server-side user storage.

---

## Troubleshooting

- **`JAVA_HOME is not set`**
  - Install JDK 17 and set `JAVA_HOME` correctly.
- **`java is not recognized`**
  - Add `%JAVA_HOME%\bin` to `Path`, then reopen terminal.
- **Client cannot connect**
  - Ensure server is running first and port `2525` is not blocked.
- **Build errors after migration**
  - Run:
    ```powershell
    .\gradlew.bat --stop
    .\gradlew.bat clean build
    ```

---

## License

This project is provided for academic and educational use.


## Server Features
- User management (create, delete, reset password)
- Active sessions monitoring (username, IP, status)
- Mailbox statistics (sent, received, total messages)
- Broadcast messaging to all users
- Real-time system logs (info, warnings, errors)
- Configurable maximum message size


## Additional Features
- UDP-based notification system for lightweight message alerts (optional enhancement)


## Architecture Overview

The system follows a client-server architecture using TCP sockets. The server manages users, rooms, and message routing, while clients provide a graphical interface for interaction. Each client maintains a persistent TCP connection to the server.