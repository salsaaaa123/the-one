/*
 * Copyright 2024 [Your Name/Organization]
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimScenario;

import java.util.*;

/**
 * Kelas ini menghasilkan laporan tentang jumlah pesan yang di-drop oleh setiap node dalam simulasi DTN.
 * Laporan mencakup total drop per node selama simulasi (diurutkan berdasarkan ID Node secara ascending).
 * <p>
 * Tujuan: Untuk memberikan gambaran yang jelas dan terstruktur tentang bagaimana pesan
 * hilang di jaringan, dan mengidentifikasi node mana yang paling sering menjatuhkan pesan.
 */
public class DroppedMessagesPerHost_v3 extends Report implements MessageListener {

    /**
     * Total drop per node. Kunci adalah objek DTNHost (node), nilainya adalah jumlah total drop.
     */
    private Map<DTNHost, Integer> droppedPerNode;

    /**
     * Membuat instance baru dari trainingDropPerHost_Asc_v2.
     * Menginisialisasi struktur data untuk melacak drop pesan.
     */
    public DroppedMessagesPerHost_v3() {
        init();
        this.droppedPerNode = new HashMap<>();
    }

    /**
     * Menginisialisasi kelas laporan (dipanggil oleh konstruktor).
     * Memanggil `super.init()` untuk melakukan inisialisasi dasar yang diperlukan oleh kelas `Report`.
     */
    @Override
    protected void init() {
        super.init();
    }

    /**
     * Dipanggil ketika sebuah pesan dihapus dari buffer node.
     * Jika pesan di-drop (bukan dikirim), informasi tentang drop tersebut dicatat.
     *
     * @param m       Pesan yang dihapus.
     * @param where   Node tempat pesan dihapus.
     * @param dropped True jika pesan di-drop, false jika dikirim.
     */
    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        // Periksa apakah pesan tersebut benar-benar di-drop (bukan hanya dikirim) dan bukan bagian dari periode warmup
        if (dropped && !isWarmupID(m.getId())) {
            // Perbarui total drop per node
            droppedPerNode.put(where, droppedPerNode.getOrDefault(where, 0) + 1);
        }
    }


    /**
     * Dipanggil ketika simulasi selesai.
     * Menulis laporan akhir yang mencakup jumlah total drop pesan per node (diurutkan berdasarkan ID Node).
     */
    @Override
    public void done() {
        write("Message dropped for scenario " + getScenarioName() + "\n");
        write("+------+-------------+\n");
        write("| Host | Total Drops |\n");
        write("+------+-------------+\n");

        List<DTNHost> allHosts = SimScenario.getInstance().getHosts();
        TreeMap<DTNHost, Integer> sortedDropped = new TreeMap<>((a, b) -> a.getAddress() - b.getAddress());
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
     *
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
     *
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
     *
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
     *
     * @param from          Host pengirim
     * @param to            Host penerima
     * @param firstDelivery Nilai boolean yang menunjukkan apakah transfer ini adalah pengiriman pertama ke tujuan akhir.
     */
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        // Tidak ada tindakan khusus yang diperlukan untuk event ini.
    }
}