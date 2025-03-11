package report;

import core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageLatencyPerContactReport extends Report implements MessageListener, ConnectionListener {

    private int nrofContacts; // Jumlah total kontak yang terjadi
    private int lastRecord; // Kontak terakhir yang dicatat
    private int interval; // Interval kontak untuk mencatat latensi
    private Map<Integer, String> avgLatencyPerContact; // Peta untuk menyimpan latensi rata-rata per kontak
    private Map<String, Double> creationTimes; // Peta untuk menyimpan waktu pembuatan pesan
    private List<Double> latencies; // Daftar untuk menyimpan semua latensi (sementara)

    public static final String NROF_CONTACT_INTERVAL = "perTotalContact"; // Setting untuk interval kontak
    public static final int DEFAULT_CONTACT_COUNT = 100; // Nilai default untuk interval kontak

    public MessageLatencyPerContactReport() {
        init();
        // Membaca interval dari pengaturan (settings). Jika tidak ada, gunakan nilai default.
        if (getSettings().contains(NROF_CONTACT_INTERVAL)) {
            interval = getSettings().getInt(NROF_CONTACT_INTERVAL);
        } else {
            interval = DEFAULT_CONTACT_COUNT;
        }
    }

    @Override
    protected void init() {
        super.init();
        // Inisialisasi variabel anggota
        this.nrofContacts = 0;
        this.lastRecord = 0;
        this.creationTimes = new HashMap<>();
        this.latencies = new ArrayList<>();
        this.avgLatencyPerContact = new HashMap<>(); // Pastikan map ini diinisialisasi
    }

    @Override
    public void newMessage(Message m) {
        // Metode ini dipanggil setiap kali pesan baru dibuat
        this.creationTimes.put(m.getId(), getSimTime()); // Simpan waktu pembuatan pesan
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        // Metode ini dipanggil ketika transfer pesan dimulai (saat ini tidak melakukan apa-apa)
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        // Metode ini dipanggil ketika pesan dihapus (saat ini tidak melakukan apa-apa)
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        // Metode ini dipanggil ketika transfer pesan dibatalkan (saat ini tidak melakukan apa-apa)
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        // Metode ini dipanggil setiap kali pesan berhasil ditransfer
        if (firstDelivery) {
            double creationTime = this.creationTimes.get(m.getId()); // Ambil waktu pembuatan pesan
            double latency = getSimTime() - creationTime; // Hitung latensi
            this.latencies.add(latency); // Tambahkan latensi ke daftar

        }
    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        // Metode ini dipanggil setiap kali dua host terhubung
        nrofContacts++;

        // Cek apakah sudah waktunya untuk mencatat data
        if (nrofContacts - lastRecord >= interval) {
            lastRecord = nrofContacts;
            String avgLatency = getAverage(latencies); // Hitung latensi rata-rata
            avgLatencyPerContact.put(lastRecord, avgLatency); // Simpan latensi rata-rata ke map

            latencies.clear(); // Bersihkan daftar latensi setelah dicatat
        }
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        // Metode ini dipanggil setiap kali dua host terputus (saat ini tidak melakukan apa-apa)
    }

    @Override
    public void done() {
        // Metode ini dipanggil ketika simulasi selesai
        String statsText = "Contact\tAvgLatency\n"; // Header untuk berkas laporan
        // Iterasi melalui map latensi dan tulis data ke berkas
        for (Map.Entry<Integer, String> entry : avgLatencyPerContact.entrySet()) {
            Integer key = entry.getKey(); // Dapatkan jumlah kontak
            String value = entry.getValue(); // Dapatkan latensi rata-rata
            statsText += key + "\t" + value + "\n"; // Tulis data ke string output
        }
        write(statsText); // Tulis string output ke berkas laporan
        super.done();
    }

}