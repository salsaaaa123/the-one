/*
 * Copyright 2024 [Your Name/Organization]
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;
import core.SimScenario;

import java.util.*;

/**
 * Kelas ini menghasilkan laporan tentang jumlah pesan yang di-drop oleh setiap node dalam simulasi DTN.
 * Laporan mencakup:
 *  - Total drop per node selama simulasi (diurutkan berdasarkan ID Node secara ascending).
 *
 *  Tujuan: Untuk memberikan gambaran yang jelas dan terstruktur tentang bagaimana pesan
 *  hilang di jaringan, dan mengidentifikasi node mana yang paling sering menjatuhkan pesan.
 */
public class DroppedMessagesPerHost_v2 extends Report implements MessageListener {

    /**
     * Durasi setiap interval laporan (dalam detik simulasi).
     *  Konstan ini menentukan seberapa sering laporan interval *berpotensi* dihasilkan,
     *  meskipun dalam implementasi saat ini, laporan interval tidak dicetak secara terpisah.
     *  Nilai 300 mewakili 5 menit (300 detik).
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
     * waktu tertentu. Meskipun peta ini diperbarui, datanya tidak digunakan dalam
     * laporan akhir.
     */
    private Map<DTNHost, List<Integer>> droppedPerNodeInterval;
    /**
     * Waktu simulasi berikutnya laporan interval harus dihasilkan.
     * Nilai ini digunakan untuk menentukan kapan method `recordIntervalReport()`
     * harus dipanggil, meskipun dalam implementasi saat ini, method tersebut
     * *tidak* dipanggil.
     */
    private double nextReportTime;

    /**
     * Membuat instance baru dari trainingDropPerHost_Asc_v2.
     * Menginisialisasi struktur data untuk melacak drop pesan.
     */
    public DroppedMessagesPerHost_v2() {
        // Memanggil method init() dari superclass (Report) untuk melakukan inisialisasi dasar.
        init();
        // Membuat instance HashMap baru untuk menyimpan jumlah total drop per node.
        this.droppedPerNode = new HashMap<>();
        // Membuat instance HashMap baru untuk menyimpan jumlah drop per node per interval (meskipun tidak digunakan).
        this.droppedPerNodeInterval = new HashMap<>();
        // Mengatur nextReportTime ke panjang interval (untuk laporan berkala, meskipun tidak digunakan).
        this.nextReportTime = INTERVAL_LENGTH;
    }

    /**
     * Menginisialisasi kelas laporan (dipanggil oleh konstruktor).
     * Saat ini, method ini hanya memanggil `super.init()`, yang melakukan
     * inisialisasi dasar yang diperlukan oleh kelas `Report`. Di masa mendatang,
     * Anda dapat menambahkan inisialisasi khusus untuk laporan ini di sini.
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
     * 1. Memeriksa apakah pesan tersebut benar-benar di-drop (bukan hanya dikirim).
     * 2. Jika pesan di-drop DAN pesan *tidak* berada dalam periode pemanasan (warmup),
     *    lakukan hal berikut:
     *  a. Perbarui peta `droppedPerNode` untuk mencatat total drop untuk node
     *   tersebut.
     *   i. Gunakan `droppedPerNode.getOrDefault(where, 0)` untuk mendapatkan
     *      jumlah drop yang ada untuk node tersebut. Jika node belum ada di
     *      map, ini akan mengembalikan 0 (artinya ini adalah drop pertama untuk node ini).
     *   ii. Tambahkan 1 ke jumlah drop yang ada dan menempatkan nilai baru
     *       kembali ke map menggunakan `droppedPerNode.put(where, ...)`
     *  3. Memeriksa apakah waktu simulasi saat ini (`SimClock.getTime()`) lebih
     *     besar dari atau sama dengan `nextReportTime`.
     *   a. Perbarui nextReportTime untuk next simulasinya
     *     `nextReportTime += INTERVAL_LENGTH;`
     *     Artinya, laporan akan di update.
     *
     * @param m       Pesan yang dihapus.
     * @param where   Node tempat pesan dihapus.
     * @param dropped True jika pesan di-drop, false jika dikirim.
     */
    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        // Periksa apakah pesan tersebut benar-benar di-drop (bukan hanya dikirim) DAN bukan bagian dari warmup
        if (dropped && !isWarmupID(m.getId())) {
            // Tambahkan data ke variable droppedPerNode (Total Drop)
            droppedPerNode.put(where, droppedPerNode.getOrDefault(where, 0) + 1);
            List<Integer> intervalCounts = droppedPerNodeInterval.getOrDefault(where, new ArrayList<>());
            if (SimClock.getTime() < nextReportTime) {
                if (intervalCounts.isEmpty()) {
                    intervalCounts.add(1);
                } else {
                    intervalCounts.set(intervalCounts.size() - 1, intervalCounts.get(intervalCounts.size() - 1) + 1);
                }
            } else {
                intervalCounts.add(1);
            }
            droppedPerNodeInterval.put(where, intervalCounts);
        }

        // Periksa apakah sudah waktunya untuk membuat laporan interval
        if (SimClock.getTime() >= nextReportTime) {
            nextReportTime += INTERVAL_LENGTH;
        }
    }

    /**
     * Dipanggil ketika simulasi selesai.
     * Menulis laporan akhir yang menggabungkan semua laporan interval.
     *
     * Logika:
     * 1. Menulis header laporan utama (Nama Skenario).
     * 2. Menulis waktu simulasi
     * 3. Iterasi TreeMap
     * 4 .Menampilkan Data
     * 5.Tangkap Ekspeksi jika ada
     */
    @Override
    public void done() {
        write("Message dropped for scenario " + getScenarioName() + "\n");
        write("sim_time: " + format(getSimTime()) + "\n");
        write("+------+-------------+\n");
        write("| Host | Total Drops |\n");
        write("+------+-------------+\n");

        List<DTNHost> allHosts = SimScenario.getInstance().getHosts();
        TreeMap<DTNHost, Integer> sortedDropped = new TreeMap<>((a, b) -> b.getAddress() - a.getAddress());
        for (DTNHost host : allHosts) {
            sortedDropped.put(host, droppedPerNode.getOrDefault(host, 0));
        }
        try {
            for (Map.Entry<DTNHost, Integer> entry : sortedDropped.entrySet()) {
                DTNHost host = entry.getKey();
                write(String.format("| %-4s | %-11d |\n", host, entry.getValue()));
            }
            write("+------+-------------+\n");
        } catch (Exception e) {
            write("Terjadi kesalahan saat mencetak laporan: " + e.getMessage());
            e.printStackTrace();
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