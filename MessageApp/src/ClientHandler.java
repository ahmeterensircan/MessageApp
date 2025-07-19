import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class ClientHandler extends Thread {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private static List<String> bannedWords;
    private String lastMessage = null;
    private int spamCount = 0;
    private static final int SPAM_LIMIT = 5;
    private static final String[] COLORS = {
        "\u001B[31m", // Kırmızı
        "\u001B[32m", // Yeşil
        "\u001B[33m", // Sarı
        "\u001B[34m", // Mavi
        "\u001B[35m", // Mor
        "\u001B[36m"  // Cyan
    };
    private static int colorIndex = 0;
    private String userColor;
    private static final String RESET = "\u001B[0m";
    // Terminal renk desteği (manuel ayar)
    private static final boolean ENABLE_COLORS = supportsAnsi();

    // Terminal ANSI desteğini kontrol eden basit fonksiyon (Windows Terminal, VSCode, Git Bash için true)
    private static boolean supportsAnsi() {
        String term = System.getenv("TERM");
        String wtSession = System.getenv("WT_SESSION");
        String vscode = System.getenv("TERM_PROGRAM");
        return (wtSession != null) || (vscode != null) || (term != null && !term.equals("dumb"));
    }

    static {
        // Yasaklı kelimeleri yükle
        bannedWords = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get("yasakli_kelimeler.txt"));
            for (String line : lines) {
                if (!line.trim().isEmpty()) bannedWords.add(line.trim().toLowerCase());
            }
        } catch (Exception e) {
            System.out.println("[UYARI] yasakli_kelimeler.txt dosyası okunamadı!");
        }
    }

    public ClientHandler(Socket socket) {
        this.socket = socket;
        synchronized (ClientHandler.class) {
            this.userColor = ENABLE_COLORS ? COLORS[colorIndex % COLORS.length] : "";
            colorIndex++;
        }
    }

    public void run() {
        try {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // Kullanıcı adı al
            while (true) {
                out.println("Kullanıcı adınızı girin:");
                username = in.readLine();
                if (username == null || username.trim().isEmpty()) {
                    out.println("> Kullanıcı adı boş olamaz!");
                    continue;
                }
                boolean exists = false;
                synchronized (Server.clientHandlers) {
                    for (ClientHandler handler : Server.clientHandlers) {
                        if (handler != this && handler.username != null && handler.username.equals(username)) {
                            exists = true;
                            break;
                        }
                    }
                }
                if (exists) {
                    out.println("> Bu kullanıcı adı zaten kullanımda, lütfen başka bir ad girin.");
                } else {
                    break;
                }
            }
            // Son 5 mesajı göster
            List<String> lastMessages = readLastMessages(5);
            if (!lastMessages.isEmpty()) {
                out.println("> Son mesajlar:");
                for (String msg : lastMessages) {
                    out.println(msg);
                }
            }
            String joinMsg = "> " + username + " sohbete katıldı.";
            System.out.println(joinMsg);
            Server.broadcast(joinMsg, this);
            logToFile(String.format("[%s] %s", Server.timestamp(), joinMsg));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String filteredInput = filterBannedWords(inputLine);
                // Spam kontrolü
                if (lastMessage != null && lastMessage.equals(filteredInput)) {
                    spamCount++;
                    if (spamCount >= SPAM_LIMIT) {
                        this.sendMessage("> Spam tespit edildi! Aynı mesajı tekrar gönderemezsiniz.");
                        continue;
                    }
                } else {
                    spamCount = 0;
                }
                lastMessage = filteredInput;

                if (inputLine.startsWith("/w ")) {
                    // /w kullanıcı_adi mesaj
                    String[] parts = inputLine.split(" ", 3);
                    if (parts.length >= 3) {
                        String targetUser = parts[1];
                        String privateMsg = filterBannedWords(parts[2]);
                        boolean found = false;
                        synchronized (Server.clientHandlers) {
                            for (ClientHandler handler : Server.clientHandlers) {
                                if (handler != this && handler.username != null && handler.username.equals(targetUser)) {
                                    String msg = String.format("%s[%s] <%s> (özel): %s%s", userColor, Server.timestamp(), username, privateMsg, ENABLE_COLORS ? RESET : "");
                                    handler.sendMessage(msg);
                                    this.sendMessage(msg); // Gönderen de görebilsin
                                    logToFile(stripColor(msg));
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            this.sendMessage("> Kullanıcı bulunamadı: " + targetUser);
                        }
                    } else {
                        this.sendMessage("> Hatalı whisper komutu. Kullanım: /w kullanıcı_adi mesaj");
                    }
                } else {
                    String msg = String.format("%s[%s] <%s>: %s%s", userColor, Server.timestamp(), username, filteredInput, ENABLE_COLORS ? RESET : "");
                    System.out.println(msg);
                    Server.broadcast(msg, this);
                    logToFile(stripColor(msg)); // Zaten filtrelenmiş
                }
            }
        } catch (IOException e) {
            //
        } finally {
            try {
                if (username != null) {
                    String leaveMsg = "> " + username + " sohbetten ayrıldı.";
                    System.out.println(leaveMsg);
                    Server.broadcast(leaveMsg, this);
                    logToFile(String.format("[%s] %s", Server.timestamp(), leaveMsg));
                }
                Server.removeClient(this);
                if (socket != null) socket.close();
            } catch (IOException e) {
                //
            }
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getUsername() {
        return username;
    }

    public void kick() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            //
        }
    }

    // Yasaklı kelimeleri sansürle
    private String filterBannedWords(String message) {
        String filtered = message;
        for (String word : bannedWords) {
            if (word.isEmpty()) continue;
            String regex = "(?i)" + java.util.regex.Pattern.quote(word);
            String replacement = word.charAt(0) + "*".repeat(word.length() - 1);
            filtered = filtered.replaceAll(regex, replacement);
        }
        return filtered;
    }

    private void logToFile(String message) {
        if (message == null) return;
        try (PrintWriter out = new PrintWriter(
                new BufferedWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(Server.logFileName, true), StandardCharsets.UTF_8)))) {
            out.println(message);
        } catch (IOException e) {
            e.printStackTrace(); // Hata varsa terminalde göster
        }
    }

    // ANSI renk kodlarını kaldır (log için)
    private String stripColor(String msg) {
        return msg.replaceAll("\\u001B\\[[;\\d]*m", "");
    }

    // chat_history.txt'den son N mesajı oku
    private List<String> readLastMessages(int n) {
        List<String> result = new ArrayList<>();
        try {
            List<String> all = Files.readAllLines(Paths.get(Server.logFileName), StandardCharsets.UTF_8);
            int start = Math.max(0, all.size() - n);
            for (String line : all.subList(start, all.size())) {
                if (line != null && !line.trim().isEmpty() && !line.trim().equalsIgnoreCase("null")) {
                    result.add(line);
                }
            }
        } catch (IOException e) {
            // Dosya yoksa sessizce geç
        }
        return result;
    }
} 