import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatServer extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton, fileButton;
    private PrintWriter writer;
    private BufferedReader reader;
    private Socket socket;
    private ServerSocket serverSocket;
    private DataInputStream dataIn;
    private DataOutputStream dataOut;
    private static final int PORT = 12345;

    public ChatServer() {
        setTitle("Server Chat");
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10,10));
        getContentPane().setBackground(new Color(40, 44, 52));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(30, 33, 40));
        chatArea.setForeground(Color.WHITE);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        inputField = new JTextField();
        sendButton = new JButton("Send");
        fileButton = new JButton(" Send File");
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(fileButton, BorderLayout.WEST);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());

        new Thread(this::setupServer).start();
    }

    private void setupServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            appendMessage("🔌 Waiting for client...");
            socket = serverSocket.accept();
            appendMessage(" Client connected!");

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            dataIn = new DataInputStream(socket.getInputStream());
            dataOut = new DataOutputStream(socket.getOutputStream());

            String msg;
            while ((msg = reader.readLine()) != null) {
                if (msg.equals("FILE_TRANSFER")) receiveFile();
                else appendMessage(" Client: " + msg);
            }
        } catch (Exception ex) {
            appendMessage(" Error: " + ex.getMessage());
        }
    }

    private void sendMessage() {
        try {
            String msg = inputField.getText();
            if (msg.isEmpty()) return;
            writer.println(msg);
            appendMessage(" You: " + msg);
            inputField.setText("");
        } catch (Exception ex) {
            appendMessage(" Failed to send message.");
        }
    }

    private void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            chatArea.append("[" + time + "] " + msg + "\n");
        });
    }

    private void sendFile() {
        try {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                writer.println("FILE_TRANSFER");
                dataOut.writeUTF(file.getName());
                dataOut.writeLong(file.length());

                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytes;
                    while ((bytes = fis.read(buffer)) != -1)
                        dataOut.write(buffer, 0, bytes);
                }
                appendMessage(" Sent file: " + file.getName());
            }
        } catch (Exception e) {
            appendMessage(" File sending failed.");
        }
    }

    private void receiveFile() throws IOException {
        String fileName = dataIn.readUTF();
        long fileSize = dataIn.readLong();
        File dir = new File("ReceivedFiles");
        if (!dir.exists()) dir.mkdir();

        File file = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long remaining = fileSize;
            while (remaining > 0 && (bytesRead = dataIn.read(buffer, 0, (int)Math.min(buffer.length, remaining))) != -1) {
                fos.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
        }
        appendMessage(" Received file: " + fileName);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatServer().setVisible(true));
    }
}
