# ChatLite Project Report Template (3-5 Pages)

## Cover Information

- Course: Networks 1 (10636454)
- Project: ChatLite - Minimal Real-Time Chat System using Java Sockets
- Instructor: Dr. Eng. Saed TARAPIAH
- Academic Year / Semester:
- Student 1 (Name, ID):
- Student 2 (Name, ID):

---

## 1. Introduction

ChatLite is a minimal real-time chat system that demonstrates TCP socket communication, multi-client server design, and Java GUI integration. The system allows users to connect, join rooms, exchange public messages, and send private messages.

This project was developed to apply networking concepts from Networks 1 in a practical, event-driven client-server application.

---

## 2. System Architecture

### 2.1 Server Side

- Persistent TCP server accepting multiple clients
- Client session management (username, status, IP)
- Room management (join/leave/list)
- Public room broadcast handling
- Private message routing
- Server GUI for monitoring sessions and logs

### 2.2 Client Side

- Java Swing GUI
- Username-based login (`HELLO <username>`)
- Room selection and join/leave operations
- Real-time chat message panel
- Private messaging panel
- Search/filter for chat history
- Status controls (`ACTIVE`, `BUSY`, `AWAY`)

---

## 3. Communication Protocol

Protocol type: line-based ASCII over TCP (`\n` terminated commands)

### Core Commands

- `HELLO <username>` -> `200 WELCOME`
- `JOIN <room>` -> `210 JOINED <room>`
- `MSG <room> <message>` -> `211 SENT`
- `PM <username> <message>` -> `212 PRIVATE SENT`
- `USERS` -> `213 <count>`, `213U <username> <status>`, `213 END`
- `ROOMS` -> `214 <room> ...`, `214 END`
- `LEAVE <room>` -> `215 LEFT`
- `QUIT` -> `221 BYE`

### Extended Command (GUI status)

- `STATUS ACTIVE|BUSY|AWAY` -> `250 OK`

### Real-Time Events

- `ROOMMSG <room> <username> <time> <message>`
- `PMFROM <username> <time> <message>`

---

## 4. Concurrency and Design Notes

- Server uses non-blocking networking with selector-based handling.
- Multiple clients are served concurrently over independent sessions.
- Client uses a single reader thread to parse incoming TCP lines and split them into:
  - response queue (for command acknowledgments)
  - event queue (for asynchronous room/private messages)
- This avoids response/event stream collision and improves reliability.

---

## 5. GUI Design

### 5.1 Client GUI

- Header: connection button, status label, uptime, room controls
- Left pane: online users + user status selector
- Center pane: chat stream + search box
- Right pane: private messaging controls
- Footer: connection/system log line

### 5.2 Server GUI

- User management controls
- Active sessions and roster table
- Real-time logs
- Settings panel and administrative controls

---

## 6. Testing and Validation

### Test Environment

- OS:
- JDK version:
- Number of clients tested concurrently:

### Functional Tests

- Connect two users with unique usernames
- Join same room and exchange messages
- Send private message between users
- Leave room and verify no further room messages are received
- Change user status and verify roster update
- Verify `USERS` and `ROOMS` outputs

### Results Summary

All required core behaviors were tested successfully (replace with your final measured statement after final test run).

---

## 7. Limitations and Future Work

- Current login is username-based (no account authentication workflow)
- No message persistence/history database
- No file transfer/attachments
- Future improvements:
  - persistence layer
  - encryption/TLS
  - moderation/admin roles
  - room access controls

---

## 8. Conclusion

The ChatLite implementation demonstrates key networking competencies: TCP protocol design, multi-client concurrency, real-time event delivery, and GUI-based interaction. The project satisfies the assignment requirements for public and private messaging, room-based communication, and server/client operation.
