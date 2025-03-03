/*
 * Copyright 2024 [Your Name/Organization]
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;

import java.util.*;

/**
 * Kelas ini menghasilkan laporan tentang jumlah pesan yang di-drop oleh setiap node dalam simulasi DTN.
 * Laporan mencakup:
 *  - Drop per node per interval 5 menit (diurutkan berdasarkan ID Node secara ascending).
 *  - Total drop per node selama simulasi (diurutkan berdasarkan ID Node secara ascending).
 *
 *  Tujuan: Untuk memberikan gambaran yang jelas dan terstruktur tentang bagaimana pesan
 *  hilang di jaringan, dan mengidentifikasi node mana yang paling sering menjatuhkan pesan.
 */
public class MessageDeleteReportPerTime_v1 extends Report implements MessageListener {

    /**
     * Durasi setiap interval laporan (dalam detik simulasi).
     *  Konstan ini menentukan seberapa sering laporan interval dihasilkan. Nilai 300
     *  mewakili 5 menit (300 detik).
     */
    private static final int INTERVAL_LENGTH = 300;
    /**
     * Total drop per node.
     *  Peta ini menyimpan jumlah total pesan yang di-drop oleh setiap node selama
     *  seluruh simulasi. Kunci adalah objek `DTNHost` (yang mewakili node), dan
     *  nilainya adalah integer yang mewakili jumlah total drop.
     */
    private Map<DTNHost, Integer> droppedPerNode;
    /**
     * Drop per node per interval.
     * Peta ini menyimpan jumlah drop pesan untuk setiap node dalam setiap
     * interval waktu. Kunci adalah objek `DTNHost`, dan nilainya adalah daftar
     * integer. Setiap integer dalam daftar mewakili jumlah drop untuk interval
     * waktu tertentu.
     */
    private Map<DTNHost, List<Integer>> droppedPerNodeInterval;
    /**
     * Waktu simulasi berikutnya laporan interval harus dihasilkan.
     * Nilai ini digunakan untuk menentukan kapan method `recordIntervalReport()`
     * harus dipanggil untuk menghasilkan laporan interval baru.
     */
    private double nextReportTime;
    /**
     * Daftar untuk menyimpan data laporan interval.
     * Setiap elemen dalam daftar ini adalah string yang diformat yang
     * mewakili laporan interval. Laporan-laporan ini kemudian dicetak pada
     * akhir simulasi.
     */
    private List<String> intervalReports;

    /**
     * Membuat instance baru dari trainingDropPerHost_Asc_v1.
     * Menginisialisasi struktur data untuk melacak drop pesan.
     */
    public MessageDeleteReportPerTime_v1() {
        init();
        this.droppedPerNode = new HashMap<>();
        this.droppedPerNodeInterval = new HashMap<>();
        this.nextReportTime = INTERVAL_LENGTH;
        this.intervalReports = new ArrayList<>();
    }


    /**
     * Menginisialisasi kelas laporan (dipanggil oleh konstruktor).
     * Memanggil `super.init()` untuk melakukan inisialisasi dasar yang diperlukan
     * oleh kelas `Report`. Di masa mendatang, Anda dapat menambahkan inisialisasi
     * khusus untuk laporan ini di sini.
     */
    @Override
    protected void init() {
        super.init();
    }

    /**
     * Dipanggil ketika sebuah pesan dihapus dari buffer node.
     * Jika pesan di-drop (bukan dikirim), informasi tentang drop tersebut dicatat.
     *
     * Logika:
     * 1. Memeriksa apakah parameter `dropped` adalah `true`. Jika tidak, ini
     *    berarti pesan berhasil dikirim, jadi kita tidak melakukan apa pun.
     * 2. Memeriksa apakah ID pesan termasuk dalam set ID pemanasan (warmupIDs).
     *    Ini digunakan untuk mengecualikan pesan yang di-drop selama periode
     *    pemanasan dari laporan.
     *  a. Panggil `isWarmupID(m.getId())` diwarisi dari class `Report`, dengan logika jika iya maka abaikan;
     * 3. Jika pesan itu di-drop dan *tidak* termasuk dalam periode pemanasan,
     *    lakukan hal berikut:
     *  a. Perbarui peta `droppedPerNode` untuk mencatat total drop untuk node
     *   tersebut.
     *   i. Gunakan `droppedPerNode.getOrDefault(where, 0)` untuk mendapatkan
     *      jumlah drop yang ada untuk node tersebut. Jika node belum ada di
     *      map, ini akan mengembalikan 0 (artinya ini adalah drop pertama untuk node ini).
     *   ii. Tambahkan 1 ke jumlah drop yang ada dan menempatkan nilai baru
     *       kembali ke map menggunakan `droppedPerNode.put(where, ...)`
     *  b. Perbarui peta `droppedPerNodeInterval` untuk mencatat drop dalam
     *   interval waktu yang sesuai.
     *   i. Gunakan `droppedPerNodeInterval.getOrDefault(where, new ArrayList<>())`
     *      untuk mendapatkan daftar jumlah interval yang ada untuk node tersebut.
     *      Jika node belum ada di map, ini akan membuat daftar baru.
     *   ii. Memeriksa apakah waktu simulasi saat ini (`SimClock.getTime()`)
     *       kurang dari `nextReportTime`. Ini menentukan apakah kita masih
     *       berada dalam interval waktu yang sama dengan laporan sebelumnya.
     *   iii. Jika ya, tambahkan hitungan drop ke interval yang ada:
     *    - Jika `intervalCounts` kosong, ini berarti ini adalah drop pertama
     *      dalam interval ini, jadi kita menambahkan 1 ke daftar.
     *    - Jika tidak, kita mendapatkan hitungan terakhir dari daftar dan
     *      menambahkannya dengan 1, lalu memperbarui hitungan terakhir dalam
     *      daftar.
     *   iv. Jika tidak (waktu simulasi saat ini lebih besar dari atau sama
     *       dengan `nextReportTime`), ini berarti kita telah melampaui interval
     *       waktu saat ini, jadi kita memulai hitungan baru dengan menambahkan
     *       1 ke daftar.
     *  4. Memeriksa apakah sudah waktunya untuk menghasilkan laporan interval
     *     (berdasarkan `nextReportTime`). Jika ya, lakukan hal berikut:
     *  a. Panggil method `recordIntervalReport()` untuk menghasilkan laporan
     *     interval baru.
     *  b. Perbarui `nextReportTime` dengan menambahkan `INTERVAL_LENGTH`. Ini
     *     menjadwalkan laporan interval berikutnya.
     *
     * @param m       Pesan yang dihapus.
     * @param where   Node tempat pesan dihapus.
     * @param dropped True jika pesan di-drop, false jika dikirim.
     */
    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        // Periksa apakah pesan tersebut benar-benar di-drop (bukan hanya dikirim)
        if (dropped && !isWarmupID(m.getId())) {
            // Perbarui total drop per node
            droppedPerNode.put(where, droppedPerNode.getOrDefault(where, 0) + 1);

            // Perbarui drop per node per interval
            List<Integer> intervalCounts = droppedPerNodeInterval.getOrDefault(where, new ArrayList<>());
            if (SimClock.getTime() < nextReportTime) {
                // Masih dalam interval yang sama, tambahkan ke hitungan terakhir
                if (intervalCounts.isEmpty()) {
                    intervalCounts.add(1); // Mulai interval baru
                } else {
                    intervalCounts.set(intervalCounts.size() - 1, intervalCounts.get(intervalCounts.size() - 1) + 1); // Tambahkan ke interval terakhir
                }
            } else {
                // Memulai hitungan baru jika melampaui interval saat ini
                intervalCounts.add(1);
            }
            droppedPerNodeInterval.put(where, intervalCounts);
        }

        // Periksa apakah sudah waktunya untuk mencetak laporan interval
        if (SimClock.getTime() >= nextReportTime) {
            recordIntervalReport(); // Buat dan simpan laporan interval
            nextReportTime += INTERVAL_LENGTH; // Jadwalkan waktu laporan berikutnya
        }
    }

    /**
     * Membuat dan menyimpan laporan interval dalam format string.
     *
     * Logika:
     * 1. Membuat StringBuilder untuk membangun laporan secara efisien.
     * 2. Menambahkan header laporan, termasuk waktu simulasi.
     * 3. Menambahkan header kolom untuk laporan CSV (Host, Dropped (interval), Total Dropped).
     * 4. Membuat TreeMap untuk mengurutkan node berdasarkan alamat (ID) secara ascending.
     * 5. Melakukan iterasi melalui TreeMap yang diurutkan.
     * 6. Untuk setiap node, dapatkan jumlah drop selama interval dan total drop (dari peta yang sesuai).
     * 7. Tambahkan baris ke laporan StringBuilder, diformat sebagai baris CSV.
     * 8. Tambahkan laporan yang sudah selesai ke IntervalReport (daftar string).
     * 9. Bersihkan droppedPerNodeInterval untuk siklus laporan berikutnya.
     */
    private void recordIntervalReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n"); //Spacing di buat lebih Bagus
        sb.append("Interval sim_time: ").append(format(getSimTime())).append("\n"); //Time DI tambahkan Di Header saja
        sb.append("+------------+--------------------+----------------+\n");
        sb.append(String.format("%-10s | %-18s | %-13s\n", "Host", "Dropped (interval)", "Total Dropped"));
        sb.append("----------------------------------------------------\n");

        TreeMap<DTNHost, List<Integer>> sortedInterval = new TreeMap<>((a, b) -> Integer.compare(a.getAddress(), b.getAddress()));
        sortedInterval.putAll(droppedPerNodeInterval);

        for (Map.Entry<DTNHost, List<Integer>> entry : sortedInterval.entrySet()) {
            DTNHost host = entry.getKey();
            List<Integer> intervalCounts = entry.getValue();
            int currentIntervalCount = intervalCounts.isEmpty() ? 0 : intervalCounts.get(intervalCounts.size() - 1);
            int totalDropped = droppedPerNode.getOrDefault(host, 0);
            sb.append(String.format("%-10s | %-18d | %-13d |\n", host, currentIntervalCount, totalDropped));
        }

        sb.append("----------------------------------------------------\n");
        intervalReports.add(sb.toString());
        droppedPerNodeInterval.clear();
    }

    /**
     * Dipanggil ketika simulasi selesai.
     * Menulis laporan akhir yang menggabungkan semua laporan interval.
     *
     * Logika:
     * 1. Menulis header laporan utama (Nama Skenario).
     * 2. Iterasi melalui IntervalReport (daftar string yang berisi laporan interval).
     * 3. Untuk setiap laporan interval, tulis konten string ke file laporan.
     * 4. Panggil `super.done()` untuk melakukan tugas-tugas penyelesaian yang diperlukan.
     */
    @Override
    public void done() {
        write("Message dropped for scenario " + getScenarioName() + "\n");
        for (String report : intervalReports) {
            write(report);
        }
        super.done();
    }

    /**
     * Implementasi kosong dari method MessageListener yang tidak digunakan.
     * Method ini harus diimplementasikan karena kelas ini mengimplementasikan interface MessageListener,
     * tetapi tidak ada tindakan khusus yang perlu diambil ketika pesan baru dibuat.
     * @param m Pesan yang dibuat.
     */
    @Override
    public void newMessage(Message m) {
        // Tidak ada tindakan khusus yang diperlukan untuk event ini.
    }

    /**
     * Implementasi kosong dari method MessageListener yang tidak digunakan.
     * Method ini harus diimplementasikan karena kelas ini mengimplementasikan interface MessageListener,
     * tetapi tidak ada tindakan khusus yang perlu diambil ketika transfer pesan dimulai.
     * @param from Host pengirim
     * @param to   Host penerima
     */
    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        // Tidak ada tindakan khusus yang diperlukan untuk event ini.
    }

    /**
     * Implementasi kosong dari method MessageListener yang tidak digunakan.
     * Method ini harus diimplementasikan karena kelas ini mengimplementasikan interface MessageListener,
     * tetapi tidak ada tindakan khusus yang perlu diambil ketika transfer pesan dibatalkan.
     * @param from Host pengirim
     * @param to   Host penerima
     */
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        // Tidak ada tindakan khusus yang diperlukan untuk event ini.
    }

    /**
     * Implementasi kosong dari method MessageListener yang tidak digunakan.
     * Method ini harus diimplementasikan karena kelas ini mengimplementasikan interface MessageListener,
     * tetapi tidak ada tindakan khusus yang perlu diambil ketika pesan berhasil ditransfer.
     * @param from          Host pengirim
     * @param to            Host penerima
     * @param firstDelivery Nilai boolean yang menunjukkan apakah transfer ini adalah pengiriman pertama ke tujuan akhir.
     */
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        // Tidak ada tindakan khusus yang diperlukan untuk event ini.
    }
}