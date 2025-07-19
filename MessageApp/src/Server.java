import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class Server {
    private static final int PORT = 12345;
    public static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static String logFileName;

    public static void main(String[] args) {
        System.out.println("[Server] Sunucu başlatılıyor...");
        // Log dosyası adı oluştur
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        logFileName = "chat_history_" + timestamp + ".txt";
        // Yönetici komutlarını dinleyen thread
        Thread adminThread = new Thread(() -> {
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            String cmd;
            try {
                while ((cmd = console.readLine()) != null) {
                    if (cmd.equals("/users")) {
                        synchronized (clientHandlers) {
                            System.out.println("Aktif kullanıcılar:");
                            for (ClientHandler handler : clientHandlers) {
                                System.out.println("- " + handler.getUsername());
                            }
                        }
                    } else if (cmd.startsWith("/kick ")) {
                        String target = cmd.substring(6).trim();
                        boolean found = false;
                        synchronized (clientHandlers) {
                            for (ClientHandler handler : clientHandlers) {
                                if (handler.getUsername() != null && handler.getUsername().equals(target)) {
                                    handler.kick();
                                    System.out.println(target + " atıldı.");
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) System.out.println("Kullanıcı bulunamadı: " + target);
                    } else if (cmd.equals("/shutdown")) {
                        System.out.println("Sunucu kapatılıyor...");
                        synchronized (clientHandlers) {
                            for (ClientHandler handler : clientHandlers) {
                                handler.sendMessage("> Sunucu kapatılıyor. Bağlantı sonlandırılıyor.");
                                handler.kick();
                            }
                        }
                        System.exit(0);
                    } else {
                        System.out.println("Bilinmeyen komut: " + cmd);
                    }
                }
            } catch (IOException e) {
                //
            }
        });
        adminThread.setDaemon(true);
        adminThread.start();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Mesajı tüm istemcilere gönder
    public static void broadcast(String message, ClientHandler exclude) {
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers) {
                if (handler != exclude) {
                    handler.sendMessage(message);
                }
            }
        }
    }

    // İstemci ayrıldığında çağrılır
    public static void removeClient(ClientHandler handler) {
        clientHandlers.remove(handler);
    }

    // Zaman damgası
    public static String timestamp() {
        return sdf.format(new Date());
    }
} 