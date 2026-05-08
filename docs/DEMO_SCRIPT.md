# ChatLite Demo Script (3 Minutes)

Use this script while recording your required demo video.

## 0:00 - 0:20 | Server Startup

1. Open terminal in project root.
2. Run:
   - `.\gradlew.bat :server:run`
3. Show server console GUI is running.

## 0:20 - 0:50 | Start Two Clients

1. Open two client instances:
   - `.\gradlew.bat :client:run`
2. In client #1 enter username: `student1`, connect.
3. In client #2 enter username: `student2`, connect.
4. Show both users appear in online users list.

## 0:50 - 1:30 | Room Chat Workflow

1. Both clients select `general` room and click `Join`.
2. Client #1 sends: `Hello everyone`.
3. Client #2 sends: `Project ready`.
4. Show both messages appear in both clients with timestamp.

## 1:30 - 2:00 | Private Message Workflow

1. In client #1 private panel:
   - To: `student2`
   - Message: `Check private channel`
2. Click `Send PM`.
3. Show PM appears in client #2 as private event.

## 2:00 - 2:25 | Status and Users

1. In client #2 change status to `BUSY`.
2. Show users panel reflects status update.

## 2:25 - 2:45 | Leave Room Validation

1. Client #2 clicks `Leave` for current room.
2. Client #1 sends one more room message.
3. Show client #2 does not receive that room message after leaving.

## 2:45 - 3:00 | End Session

1. Close one client (or issue quit action).
2. Show server logs update with disconnect/session changes.
3. End video.
