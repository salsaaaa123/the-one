<h1 align="center">🚀 The ONE v1.4.1 📡</h1>

<p align="center">The ONE (Opportunistic Network Environment) adalah simulator yang dirancang untuk meneliti dan menganalisis jaringan toleran-delay (DTN). Dengan alat ini, Anda dapat:</p>

<p align="center">
  🗺️ Menghasilkan jejak mobilitas untuk node dalam simulasi <br>
  💬 Menjalankan simulasi pertukaran pesan dengan berbagai protokol routing DTN <br>
  📊 Memvisualisasikan hasil simulasi secara real-time dan menganalisisnya setelah simulasi selesai
</p>

---

## 📖 Pendahuluan
Sebelum menggunakan repositori ini, disarankan untuk terlebih dahulu memahami konsep dasar ONE Simulator dengan mencoba repositori latihan berikut:

🔗 **Latihan Awal:** [Opportunistic Network Environment](https://github.com/hendrowunga/Opportunistic-Network-Environment.git)

Repositori latihan ini akan membantu Anda memahami dasar-dasar penggunaan ONE Simulator sebelum mulai melakukan analisis dan eksperimen lebih lanjut.

Setelah memahami dasar-dasar, Anda dapat mulai melakukan eksperimen dan analisis lebih lanjut dengan membuat laporan hasil simulasi dalam bentuk grafik.

📂 **Laporan Simulasi:** [report](https://github.com/hendrowunga/Opportunistic-Network-Environment/tree/main/src/report)

📊 **Hasil Analisis & Grafik:** [Algorithmic Comparison](https://github.com/hendrowunga/Opportunistic-Network-Environment/tree/main/discussion/AlgorithmicComparison)

---

## 🏁 Quick Start

### ⚙️ Compiling

Untuk menjalankan ONE Simulator dari source code, Anda perlu mengompilasinya menggunakan **Gradle 8.10**. Pastikan **Java 23 atau lebih baru** sudah terinstal sebelum melanjutkan.

#### **Langkah-langkah untuk Linux/macOS:**
```sh
  ./gradlew clean build
```
atau
```sh
  ./gradlew build
```


#### **Langkah-langkah untuk Windows:**
```sh
  gradlew.bat clean build
```
atau
```sh
  gradlew.bat build
```

Jika menggunakan **IntelliJ IDEA**, pastikan untuk menambahkan beberapa JAR library berikut ke dalam build path agar proses kompilasi berjalan dengan lancar:

1. Masuk ke: `Project Structure` -> `Dependencies`
2. Pilih tab simbol `+ ( Add )`
3. Klik `JARs or Directories`
4. Tambahkan file berikut dari folder `lib/`:
    - `DTNConsoleConnection.jar`
    - `ECLA.jar`
    - `junit-4.8.2.jar`
    - `uncommons-maths-1.2.1.jar`
5. Klik "OK"

Setelah langkah ini selesai, **IntelliJ IDEA** seharusnya dapat mengompilasi ONE tanpa kesalahan. ✅

---

### 🏃 Running

ONE dapat dijalankan menggunakan skrip yang telah disediakan dalam repositori ini.

#### **Linux/macOS:**
```sh
    ./gradlew run
```

#### **Windows:**
```sh
    gradlew.bat run
```

Untuk menjalankan simulasi dalam mode batch (tanpa antarmuka grafis):
```sh
    ./gradlew run -b 1 default_settings.txt
```

**Parameter yang tersedia:**
- `-b <jumlah>`: Menjalankan simulasi dalam mode batch tanpa GUI.
- `conf-files`: Nama file konfigurasi yang digunakan dalam simulasi.

---

### 🛠️ Configuring

Simulasi dalam ONE dikendalikan oleh file konfigurasi. File ini adalah file teks biasa dengan format `key = value` yang memungkinkan Anda mengatur parameter simulasi sesuai kebutuhan.

Contoh pengaturan seed acak:
```ini
    MovementModel.rngSeed = [1; 2; 3; 4; 5]
```
Konfigurasi di atas akan menjalankan simulasi dengan 5 seed berbeda untuk variasi skenario simulasi.

Untuk melihat contoh lengkap konfigurasi, lihat file `default_settings.txt` dan `snw_comparison_settings.txt` dalam repositori ini.

---

### 🔢 Run Indexing

Run indexing memungkinkan Anda menjalankan berbagai konfigurasi hanya dengan satu file konfigurasi.
Misalnya, jika Anda ingin menjalankan simulasi dengan berbagai nilai seed:
```ini
    MovementModel.rngSeed = [1; 2; 3; 4; 5]
```
Gunakan perintah berikut untuk menjalankan simulasi dalam mode batch:
```sh
    ./gradlew run -b 5 my_config.txt
```
Ini akan menjalankan simulasi secara otomatis dengan setiap nilai seed yang ditentukan.

---

## 📜 Dependencies

Proyek ini menggunakan dependensi berikut untuk memastikan kelancaran eksekusi simulasi:

```gradle
plugins {
    id 'java'
    id 'application'
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(23))
}

application {
    mainClass.set("core.DTNSim")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("lib/ECLA.jar"))
    implementation(files("lib/DTNConsoleConnection.jar"))
    implementation(files("lib/junit-4.8.2.jar"))
    implementation(files("lib/uncommons-maths-1.2.1.jar"))
}

sourceSets.main {
    java.srcDirs("src")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "core.DTNSim"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

tasks.named<JavaExec>("run") {
    dependsOn("classes")
    jvmArgs = listOf("-Xmx512M")
    classpath = sourceSets.main.get().runtimeClasspath
    args = listOf("1", "default_settings.txt")
}
```

Pastikan semua dependensi ada di folder `lib/` sebelum menjalankan proyek.

---

## 📌 Notes

- Untuk menggunakan proyek ini, pastikan **Java 23 atau lebih baru** sudah terinstal.
- Jika mengalami masalah saat menjalankan di Windows, jalankan perintah dari **Command Prompt (cmd) sebagai Administrator**.
- Semua file konfigurasi dapat diedit sesuai kebutuhan untuk menyesuaikan simulasi.


<h1 align="center">🚀 Selamat bereksperimen! 🎉</h1>

