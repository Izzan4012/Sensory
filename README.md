UNTUK VIDEO AGAK TELAT KARENA GA BISA DI TEST LEWAT EMULATOR, DAN HP JUGA GA BISA DI DOWNLOAD.



# Sensoryshi - Android Multi-Sensor Explorer 📱✨

Aplikasi Android berbasis **Kotlin** dan **Jetpack Compose** yang memanfaatkan berbagai sensor perangkat Android dari kategori **Environment**, **Position**, dan **Motion** untuk memberikan respons/efek langsung pada antarmuka pengguna (UI) serta umpan balik suara (*audio feedback*).

---

## 🌟 Sensor Yang Digunakan & Efek UI

Aplikasi ini mengintegrasikan 3 sensor dari 3 kategori berbeda untuk memenuhi dan melampaui syarat minimal tugas:

### 1. Light Sensor (`Sensor.TYPE_LIGHT`) - **Kategori: Environment**
* **Parameter Terukur:** Tingkat kecerahan cahaya ruangan dalam satuan **Lux**.
* **Efek Langsung ke UI:**
  * **Dinamis Theme/Latar Belakang:** Latar belakang aplikasi secara otomatis bertransisi warna sesuai kondisi cahaya ruangan:
    * `0 - 10 Lux`: Mode Malam / Sangat Gelap (*Midnight Indigo*).
    * `10 - 200 Lux`: Cahaya Redup (*Forest Emerald*).
    * `200 - 1000 Lux`: Cahaya Ruangan Normal (*Royal Slate*).
    * `> 1000 Lux`: Cahaya Terang / Luar Ruangan (*Warm Amber*).
  * **Indikator Visual:** Menampilkan nilai Lux real-time, badge status kondisi cahaya, serta *Progress Bar* intensitas cahaya.

### 2. Proximity Sensor (`Sensor.TYPE_PROXIMITY`) - **Kategori: Position**
* **Parameter Terukur:** Jarak objek/tangan di depan layar HP/emulator (cm).
* **Efek Langsung ke UI & Feedback Suara:**
  * **Card Alert Berubah Warna:** Saat objek mendekat (`< 5 cm`), Card Proximity berubah dari warna hijau normal menjadi **merah peringatan (*Alert Red*)** dengan ikon peringatan.
  * **Sound Feedback (Bunyi BEEP):** Ketika sensor mendeteksi objek mendekat, aplikasi memicu suara tone `ToneGenerator.TONE_PROP_BEEP`.
  * **Tombol "Tes Suara":** Disediakan tombol interaktif untuk menguji umpan balik audio secara manual.

### 3. Accelerometer (`Sensor.TYPE_ACCELEROMETER`) - **Kategori: Motion**
* **Parameter Terukur:** Percepatan & kemiringan perangkat pada sumbu **X, Y, Z**.
* **Efek Langsung ke UI:**
  * **Animasi Bola Physics 2D pada Canvas:** Terdapat kotak Canvas interaktif tempat bola 2D menggelinding mengikuti arah kemiringan HP/emulator.
  * **Deteksi Arah Kemiringan:** Menampilkan status kemiringan secara real-time (*Miring Kiri, Miring Kanan, Miring Depan, Miring Belakang, Datar*).
  * **Nilai Real-time:** Menampilkan bacaan angka presisi tinggi untuk sumbu X, Y, dan Z.
  * **Tombol Reset:** Mengembalikan posisi bola ke titik tengah canvas.

---

## 🔄 Manajemen Lifecycle Sensor (Sesuai Syarat)

Pengelolaan sensor dikendalikan secara ketat sesuai praktik terbaik Android:

* **Register Sensor di `onResume()`:** Sensor mendaftarkan listener saat Activity memasuki status aktif (`onResume`) menggunakan `sensorManager.registerListener(...)`.
* **Unregister Sensor di `onPause()`:** Listener sensor secara otomatis dilepas saat Activity tidak terlihat / di-background (`onPause`) menggunakan `sensorManager.unregisterListener(...)` untuk menghemat penggunaan baterai dan memori.
* **Indikator Lifecycle di UI:** Header aplikasi menampilkan badge status lifecycle secara live (`RESUMED (Sensor Aktif)` vs `PAUSED (Sensor Nonaktif)`).

---

## 🛠️ Cara Pengujian di Android Emulator

1. Jalankan aplikasi pada Android Emulator via Android Studio (pilih **Run/Play**).
2. Pada jendela emulator, klik tombol **Tiga Titik (...)** di toolbar samping untuk membuka **Extended Controls**.
3. Pilih menu **Virtual Sensors**:
   * **Menguji Light Sensor:** Geser slider **Ambient light (lux)**. Perhatikan warna latar belakang aplikasi dan nilai Lux berubah secara halus.
   * **Menguji Proximity Sensor:** Pada opsi **Proximity**, pilih **Near** untuk melihat Card berubah warna menjadi merah dan memicu bunyi BEEP. Pilih **Far** untuk kembali normal.
   * **Menguji Accelerometer:** Ubah rotasi/tilt pada gambar HP 3D atau slider percepatan sumbu X/Y/Z. Perhatikan bola 2D di layar bergerak mengikuti arah kemiringan.

---

## 📁 Struktur Kode Utama

* [`MainActivity.kt`](file:///C:/Users/Izzan%20Leandra/AndroidStudioProjects/Sensoryshi/app/src/main/java/com/izzan/sensoryshi/MainActivity.kt): Mengimplementasikan `SensorEventListener`, mendeteksi ketersediaan sensor, mengelola `onResume`/`onPause`, memicu `ToneGenerator`, dan merender UI Compose yang responsif.
* [`build.gradle.kts`](file:///C:/Users/Izzan%20Leandra/AndroidStudioProjects/Sensoryshi/app/build.gradle.kts): Konfigurasi dependensi Jetpack Compose dan Material 3.
