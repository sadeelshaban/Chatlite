package com.ChatLite.server;

public class AdminConsole implements Runnable {

    @Override
    public void run() {
        try {
            java.util.Scanner sc = new java.util.Scanner(System.in);

            while (true) {
                System.out.print("admin> ");
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] t = line.split("\\s+");
                String cmd = t[0].toLowerCase();

                switch (cmd) {

                    case "adduser" -> {
                        if (t.length < 3) {
                            System.out.println("Usage: adduser <username> <password>");
                            break;
                        }
                        boolean ok = UserStore.addUser(t[1], t[2]);
                        System.out.println(ok ? "OK" : "ERR duplicate username");
                    }

                    case "passwd" -> {
                        if (t.length < 3) {
                            System.out.println("Usage: passwd <username> <newpassword>");
                            break;
                        }
                        boolean ok = UserStore.updatePassword(t[1], t[2]);
                        System.out.println(ok ? "OK" : "ERR user not found");
                    }

                    case "deluser" -> {
                        if (t.length < 2) {
                            System.out.println("Usage: deluser <username>");
                            break;
                        }
                        boolean ok = UserStore.removeUser(t[1]);
                        System.out.println(ok ? "OK" : "ERR user not found");
                    }

                    case "listusers" -> {
                        for (String user : UserStore.snapshot()) {
                            System.out.println(user);
                        }
                    }

                    case "help" -> {
                        System.out.println("Commands:");
                        System.out.println("  adduser <u> <p>");
                        System.out.println("  passwd <u> <newp>");
                        System.out.println("  deluser <u>");
                        System.out.println("  listusers");
                        System.out.println("  help");
                    }

                    default -> System.out.println("Unknown command. Try 'help'.");
                }
            }

        } catch (Exception ex) {
            ServerLog.error("AdminConsole", ex.toString());
            System.out.println("ERR " + ex.getMessage());
        }
    }
}