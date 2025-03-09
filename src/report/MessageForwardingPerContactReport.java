package report;

import core.*;

import java.util.*;

/**
 * Laporan ini mencatat jumlah total penerusan pesan per interval kontak.
 */
public class MessageForwardingPerContactReport extends Report implements MessageListener, ConnectionListener{

    private int nrofContacts; // Jumlah total kontak
    private int lastRecord; // Jumlah kontak terakhir data direkam
    private int interval; // Interval pencatatan data berdasarkan jumlah kontak
    private Map<Integer, Integer> nrofForwardRecords; // Map untuk menyimpan jumlah penerusan per interval kontak
    private Map<DTNHost, Integer> nrofForwards; // Map untuk menyimpan jumlah penerusan per host
    private static final String NROF_CONTACT_INTERVAL = "perTotalContact"; // Setting untuk interval kontak
    private static final int DEFAULT_CONTACT_COUNT = 100; // Nilai default untuk interval kontak

    public MessageForwardingPerContactReport() {
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
        this.nrofForwardRecords = new HashMap<>();
        this.nrofForwards = new HashMap<>();
    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        // Metode ini dipanggil setiap kali dua host terhubung
        nrofContacts++; // Tingkatkan jumlah kontak

        // Periksa apakah sudah waktunya untuk mencatat data berdasarkan interval kontak
        if (nrofContacts - lastRecord >= interval) {
            recordData(); // Catat data penerusan
        }
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
        // Tambahkan jumlah penerusan untuk host pengirim
        nrofForwards.put(from, nrofForwards.getOrDefault(from, 0) + 1);
    }

    /**
     * Mencatat data penerusan ke map nrofForwardRecords.
     */
    private void recordData() {
        // Hitung jumlah total penerusan dari semua host
        int totalForwardCount = nrofForwards.values().stream().mapToInt(Integer::intValue).sum();
        nrofForwardRecords.put(nrofContacts, totalForwardCount); // Simpan jumlah penerusan ke map

        lastRecord = nrofContacts; // Perbarui kontak terakhir yang dicatat
    }

    @Override
    public void done() {
        // Metode ini dipanggil ketika simulasi selesai
        StringBuilder output = new StringBuilder("Contacts\tTotalForwards\n"); // Header untuk berkas laporan

        List<Map.Entry<Integer, Integer>> sortedEntry = new ArrayList<>(nrofForwardRecords.entrySet());
        sortedEntry.sort(Map.Entry.comparingByKey());

        // Tulis data ke berkas laporan
        for (Map.Entry<Integer, Integer> entry : sortedEntry) {
            output.append(entry.getKey()).append("\t").append(entry.getValue()).append("\n");
        }

        write(output.toString()); // Tulis string output ke berkas laporan
        super.done();
    }

}

//@Override
//public void done() {
//
//    StringBuilder output = new StringBuilder("Contacts\tForward Counts\n");
//    List<Map.Entry<Integer, Integer>> sortedEntry = new ArrayList<>(nrofForwardRecords.entrySet());
//    if (sortedEntry.size() > 10_000) {
//        sortedEntry = sortedEntry.parallelStream()
//                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder())) // Descending
//                .toList();
//        output.append("\n# Descending Order with parallelStream (Data > 10.000)\n");
////            sortedEntry = sortedEntry.parallelStream()
////                    .sorted(Map.Entry.comparingByKey()) // Ascending
////                    .toList();
//    } else {
//        sortedEntry.sort(Map.Entry.comparingByKey()); // Ascending
//        output.append("\n# Ascending Order (Data < 10.000)\n");
////            sortedEntry.sort(Map.Entry.comparingByKey(Comparator.reverseOrder())); // Descending
//    }
//    sortedEntry.forEach(entry -> output.append(entry.getKey()).append("\t").append(entry.getValue()).append("\n"));
//    write(output.toString());
//    super.done();
//}