# ChatLite Final Test Checklist

Mark each item before submission.

## A) Environment

- [ ] JDK 17 installed (`java -version`, `javac -version`)
- [ ] Project builds successfully (`.\gradlew.bat clean build`)
- [ ] Server runs (`.\gradlew.bat :server:run`)
- [ ] Client runs (`.\gradlew.bat :client:run`)

## B) Protocol Compliance

- [ ] `HELLO <username>` returns `200 WELCOME`
- [ ] `JOIN <room>` returns `210 JOINED <room>`
- [ ] `MSG <room> <message>` returns `211 SENT`
- [ ] `PM <username> <message>` returns `212 PRIVATE SENT`
- [ ] `USERS` returns `213 ... 213U ... 213 END`
- [ ] `ROOMS` returns `214 ... 214 END`
- [ ] `LEAVE <room>` returns `215 LEFT`
- [ ] `QUIT` returns `221 BYE`
- [ ] `STATUS ACTIVE|BUSY|AWAY` returns `250 OK`

## C) Functional Behavior

- [ ] Two clients can connect concurrently
- [ ] Both can join same room
- [ ] Room messages are visible to room members
- [ ] Private message is delivered only to target user
- [ ] After leaving a room, user no longer receives that room messages
- [ ] Online users list refreshes correctly
- [ ] User status change is visible
- [ ] Search/filter works in client message area

## D) GUI Expectations

- [ ] Client contains room controls, users panel, chat area, PM panel, search, status bar
- [ ] Server GUI shows sessions/roster and logs
- [ ] Buttons are connected to working actions (join, leave, send, private send, status)

## E) Submission Package

- [ ] Source code (client + server)
- [ ] README with compile/run steps
- [ ] Report (3-5 pages)
- [ ] Demo video (about 3 minutes)
- [ ] Sample users/rooms file
