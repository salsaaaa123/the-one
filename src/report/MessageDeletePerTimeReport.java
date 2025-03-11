package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;

import java.util.*;

public class MessageDeletePerTimeReport extends Report implements MessageListener {

    private Map<Integer, Map<DTNHost, Integer>> deleteMessagePerTime; // Data penghapusan per waktu
    private int interval; // Interval pencatatan dalam detik
    private double lastRecord; // Waktu terakhir pencatatan
    private Map<Integer, Integer> droppedMessagesPerTime;

    public static final String DELETE_REPORT_INTERVAL = "deleteInterval";
    public static final int DEFAULT_DELETE_REPORT_INTERVAL = 3600;

    public MessageDeletePerTimeReport() {
        init();
        // Mengambil nilai interval dari pengaturan jika ada, jika tidak, gunakan default
        Settings settings = getSettings();
        if (settings.contains(DELETE_REPORT_INTERVAL)) {
            this.interval = settings.getInt(DELETE_REPORT_INTERVAL);
        } else {
            this.interval = DEFAULT_DELETE_REPORT_INTERVAL;
        }
    }

    public void init() {
        super.init();
        this.deleteMessagePerTime = new HashMap<>();
        this.lastRecord = Double.MIN_VALUE;
        this.droppedMessagesPerTime = new HashMap<>();
    }

    @Override
    public void newMessage(Message m) {
        // Tidak perlu ditangani untuk laporan ini
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        // Tidak perlu ditangani untuk laporan ini
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        double simTime = getSimTime();
        int currentTimeSlot = (int) (simTime / interval); // Hitung slot waktu saat ini

        // Ambil atau buat slot waktu baru
        deleteMessagePerTime.putIfAbsent(currentTimeSlot, new HashMap<>());
        droppedMessagesPerTime.putIfAbsent(currentTimeSlot, 0);

        Map<DTNHost, Integer> deleteMessage = deleteMessagePerTime.get(currentTimeSlot);

        // Tambahkan jumlah pesan yang dihapus oleh masing-masing host
        deleteMessage.put(where, deleteMessage.getOrDefault(where, 0) + 1);

        // Jika pesan di-drop, tambahkan ke total drop per interval waktu
        if (dropped) {
            droppedMessagesPerTime.put(currentTimeSlot, droppedMessagesPerTime.get(currentTimeSlot) + 1);
        }
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        // Tidak perlu ditangani untuk laporan ini
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        // Tidak perlu ditangani untuk laporan ini
    }

    @Override
    public void done() {
        write("Time\tHost\tDrop\tTotal Drop");

        // Urutkan berdasarkan waktu secara ascending
        List<Integer> sortedTimeSlots = new ArrayList<>(deleteMessagePerTime.keySet());
        Collections.sort(sortedTimeSlots);

        // Loop melalui setiap slot waktu yang telah tercatat
        for (int timeSlot : sortedTimeSlots) {
            Map<DTNHost, Integer> deleteMessage = deleteMessagePerTime.get(timeSlot);
            int totalDropped = droppedMessagesPerTime.getOrDefault(timeSlot, 0); // Ambil jumlah pesan yang di-drop

            // Urutkan host berdasarkan ID secara ascending
            List<DTNHost> sortedHosts = new ArrayList<>(deleteMessage.keySet());
            sortedHosts.sort(Comparator.comparingInt(DTNHost::getAddress));

            for (DTNHost host : sortedHosts) {
                write(timeSlot * interval + "\t" + host + "\t\t" + deleteMessage.get(host) + "\t\t" + totalDropped);
            }
        }

        super.done();
    }
}
