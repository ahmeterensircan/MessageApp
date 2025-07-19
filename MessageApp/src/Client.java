import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Client {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 12345;
        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

            // Sunucudan gelen mesajları dinleyecek thread
            Thread readerThread = new Thread(() -> {
                String fromServer;
                try {
                    while ((fromServer = in.readLine()) != null) {
                        System.out.println(fromServer);
                    }
                } catch (IOException e) {
                    //
                }
            });
            readerThread.start();

            // Kullanıcıdan mesaj alıp sunucuya gönder
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
            }
        } catch (IOException e) {
            System.out.println("Sunucuya bağlanılamadı: " + e.getMessage());
        }
    }
} 