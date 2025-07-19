# Java Terminal Chat Uygulaması

Bu proje, Java ile geliştirilmiş, çoklu istemci destekli, gerçek zamanlı, terminal tabanlı bir sohbet (chat) uygulamasıdır.

## Özellikler
- Çoklu kullanıcı desteği (her istemci için ayrı thread)
- Gerçek zamanlı grup sohbeti
- Özel mesajlaşma (/w kullanıcı mesaj)
- Spam tespiti ve küfür filtresi (yasaklı_kelimeler.txt ile)
- Mesaj geçmişi ve loglama (her oturum için ayrı dosya)
- Giriş/çıkış bildirimleri
- Yönetici komutları (/users, /kick, /shutdown)

## Kurulum ve Çalıştırma
1. Proje dizininde `src` klasörüne girin:
    ```
    cd src
    ```
2. Java dosyalarını derleyin:
    ```
    javac *.java
    ```
3. Sunucuyu başlatın:
    ```
    java Server
    ```
4. Her istemci için yeni bir terminal açıp istemciyi başlatın:
    ```
    java Client
    ```

## Ekran Görüntüleri

### 1. Genel Mesajlaşma ve Sohbet
![Genel mesajlaşma](Screenshots/Messages.png)
Sohbet ekranında birden fazla kullanıcı mesajlaşabilir.

### 2. Spam Tespiti
![Spam tespiti](Screenshots/spam.png)
Aynı mesajı arka arkaya gönderen kullanıcıya uyarı verilir.

### 3. Özel Mesaj (Whisper)
![Özel mesaj 1](Screenshots/privateMessage1.png)
![Özel mesaj 2](Screenshots/private2.png)
`/w kullanıcı mesaj` komutuyla sadece belirli bir kullanıcıya özel mesaj gönderilebilir.

### 4. Kullanıcı Listesi Komutu
![Kullanıcı listesi](Screenshots/usersKomut.png)
Sunucu terminalinden aktif kullanıcıları listeleme komutu (/users).

### 5. Kullanıcı Atma Komutu
![Kullanıcı atma](Screenshots/Kick.png)
Sunucu terminalinden bir kullanıcıyı sohbetten atma komutu (/kick kullanıcı).

### 6. Sunucuyu Kapatma Komutu
![Sunucu kapatma](Screenshots/shutdown.png)
Sunucu terminalinden sunucuyu kapatma komutu (/shutdown).

## Notlar
- Log dosyaları ve yasaklı kelimeler dosyası proje kökünde tutulur.
- Renkli mesajlar, sadece destekleyen terminallerde görünür.
 
