public class ChatLauncher {
    public static void main(String[] args) {
        try {
            // Start server
            Thread serverThread = new Thread(() -> {
                try {
                    ChatServer.main(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            serverThread.start();

            // Give it time to start up
            Thread.sleep(2000);

            // Start client
            Thread clientThread = new Thread(() -> {
                try {
                    ChatClient.main(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            clientThread.start();

            // Close both when program ends
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n Closing chat system...");
                serverThread.interrupt();
                clientThread.interrupt();
            }));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
