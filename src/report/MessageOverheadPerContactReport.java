package report;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.HashMap;
import java.util.Map;

public class MessageOverheadPerContactReport extends Report implements MessageListener, ConnectionListener {

    private int lastRecord; // Terakhir kali data dicatat
    private int interval; // Interval kontak untuk mencatat data
    private int nrofContact; // Jumlah total kontak
    private int nrofRelayed; // Jumlah total pesan yang diteruskan (relayed)
    private int nrofDelivered; // Jumlah total pesan yang berhasil dikirim (delivered)
    private Map<Integer, Double> nrofOverhead; // Map untuk menyimpan rasio overhead per kontak

    private static final String NROF_CONTACT_INTERVAL = "perTotalContact"; // Setting untuk interval kontak
    private static final int DEFAULT_CONTACT_COUNT = 100; // Nilai default untuk interval kontak

    // Constructor:
    public MessageOverheadPerContactReport() {
        init();
        // Membaca interval dari pengaturan (settings). Jika tidak ada, gunakan nilai default.
        if (getSettings().contains(NROF_CONTACT_INTERVAL)) {
            interval = getSettings().getInt(NROF_CONTACT_INTERVAL);
        } else {
            interval = DEFAULT_CONTACT_COUNT;
        }
//        System.out.println("MessageOverheadPerContactReport initialized with interval: " + interval); // Debug
    }

    @Override
    protected void init() {
        super.init();
        // Inisialisasi variabel anggota
        this.lastRecord = 0;
        this.nrofContact = 0;
        this.nrofDelivered = 0;
        this.nrofRelayed = 0;
        this.nrofOverhead = new HashMap<>();
//        System.out.println("Overhead report initialized"); // Debug

    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        // Metode ini dipanggil setiap kali dua host terhubung

        nrofContact++; // Tingkatkan jumlah kontak
//        System.out.println("Contact #" + nrofContact + " between " + host1 + " and " + host2); // Debug

        // Cek apakah sudah waktunya untuk mencatat data
        if (nrofContact - lastRecord >= interval) {
            lastRecord = nrofContact; // Update waktu terakhir data dicatat
            double overHead=Double.NaN; // overhead ratio

            // Hitung overhead ratio
            if (this.nrofDelivered > 0) {
                overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) / this.nrofDelivered;
            } else {
                overHead = 0.0; // Hindari pembagian dengan nol
            }

            nrofOverhead.put(lastRecord, overHead); // Simpan overhead ratio ke map
//            System.out.println("Recording overhead at contact #" + lastRecord +
//                    ", Relayed: " + nrofRelayed +
//                    ", Delivered: " + nrofDelivered +
//                    ", Overhead: " + overHead); // Debug
        }

    }

    @Override
    public void done() {
        // Metode ini dipanggil ketika simulasi selesai
        String output = "Contact\tOverhead\n"; // Header untuk berkas laporan
        // Iterasi melalui map overhead dan tulis data ke berkas
        for (Map.Entry<Integer, Double> entry : nrofOverhead.entrySet()) {
            Integer key = entry.getKey(); // Dapatkan jumlah kontak
            Double value = entry.getValue(); // Dapatkan overhead ratio
            output += key + "\t" + value + "\n"; // Tulis data ke string output
        }
        write(output); // Tulis string output ke berkas laporan
        super.done();
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        // Metode ini dipanggil setiap kali dua host terputus (saat ini tidak melakukan apa-apa)
    }

    @Override
    public void newMessage(Message m) {
        // Metode ini dipanggil setiap kali pesan baru dibuat (saat ini tidak melakukan apa-apa)
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        // Metode ini dipanggil setiap kali transfer pesan dimulai (saat ini tidak melakukan apa-apa)
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        // Metode ini dipanggil setiap kali pesan dihapus (saat ini tidak melakukan apa-apa)
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        // Metode ini dipanggil setiap kali transfer pesan dibatalkan (saat ini tidak melakukan apa-apa)
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        // Metode ini dipanggil setiap kali pesan berhasil ditransfer
        nrofRelayed++; // Setiap kali pesan diteruskan, kita tingkatkan jumlah pesan yang diteruskan

        // Jika ini adalah pengiriman pertama ke tujuan akhir, kita tingkatkan jumlah pesan yang dikirim
        if (firstDelivery) {
            nrofDelivered++;
        }

    }
}