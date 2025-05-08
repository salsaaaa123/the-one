package routing.decisionengine;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.Duration;

import java.util.*;

/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */
public class SnFDecisionEngineDuration implements RoutingDecisionEngine {

    public static final String NROF_COPIES_S = "nrofCopies";
    public static final String MSG_COUNT_PROP = "SprayAndFocus.copies";
    public static final String TIMER_THRESHOLD_S = "transitivityTimerThreshold";
    protected static final double DEFAULT_TIMEDIFF = 300;
    protected static final double defaultTransitivityThreshold = 1.0;
    protected int initialNrofCopies;
    protected double transitivityTimerThreshold;

    /**
     * recentEncounters Map yang nyimpen informasi kontak dengan peer.
     * Kunci = DTNHost (peer)
     * Nilai = List<Duration> yang berisi daftar durasi kontak dengan peer
     */
    protected Map<DTNHost, List<Duration>> connHistory;

    /**
     * startTimestamps Map yang menyimpan waktu mulai kontak dengan peer.
     * Kunci = DTNHost (peer)
     * Nilai = Waktu Mulai Kontak
     */
    protected Map<DTNHost, Double> startTimestamps;


    public SnFDecisionEngineDuration(Settings s) {
        initialNrofCopies = s.getInt(NROF_COPIES_S);
        if (s.contains(TIMER_THRESHOLD_S))
            transitivityTimerThreshold = s.getDouble(TIMER_THRESHOLD_S);
        else
            transitivityTimerThreshold = defaultTransitivityThreshold;

        connHistory = new HashMap<DTNHost, List<Duration>>();
        startTimestamps = new HashMap<DTNHost, Double>();
    }

    public SnFDecisionEngineDuration(SnFDecisionEngineDuration r) {
        this.initialNrofCopies = r.initialNrofCopies;
        this.transitivityTimerThreshold = r.transitivityTimerThreshold;
        connHistory = new HashMap<DTNHost, List<Duration>>();
        startTimestamps = new HashMap<DTNHost, Double>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        // Catat waktu mulai koneksi
//        double currentTime = SimClock.getTime();
//        startTimestamps.put(peer, currentTime);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // Pastikan waktu mulai koneksi ada
        if (!startTimestamps.containsKey(peer)) {
            System.err.println("Error: No start time found for peer " + peer +
                    ", cannot record connection duration");
            return; // Keluar dari method jika tidak ada waktu mulai
        }

        double time = startTimestamps.get(peer);
        double etime = SimClock.getTime();

        // Find or create the connection history list
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else {
            history = connHistory.get(peer);
        }

        // add this connection to the list
        if (etime - time > 0) {
            history.add(new Duration(time, etime));
        }

        // Hapus start time karena koneksi sudah putus
        startTimestamps.remove(peer);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        SnFDecisionEngineDuration de = this.getOtherSnFDecisionEngine(peer);
        DTNHost myHost = con.getOtherNode(peer);

        // Catat waktu mulai koneksi
        double currentTime = SimClock.getTime();
        startTimestamps.put(peer, currentTime);
        de.startTimestamps.put(myHost, currentTime);

        DecisionEngineRouter myRouter = (DecisionEngineRouter) myHost.getRouter();
        List<Message> messages = new ArrayList<>(myRouter.getMessageCollection());

        DTNHost destination = null;
        for (Message msg : messages) {
            if (msg.getTo().equals(peer)) { // Jika pesan ini akan dikirim ke peer
                destination = msg.getTo();
                break; // Asumsikan hanya satu pesan yang relevan untuk contoh ini
            }
            if (msg.getFrom().equals(peer)) { // Jika pesan ini akan dikirim ke peer
                destination = msg.getTo();
                break; // Asumsikan hanya satu pesan yang relevan untuk contoh ini
            }
        }
        if(destination == null){
            return;
        }
        Double connectionStartTime = startTimestamps.get(peer);
        if (connectionStartTime == null) {
            // Jika tidak ada waktu mulai, mungkin ada kesalahan
            System.err.println("Error: No start time found for peer " + peer);
            return;
        }
        Double connectionEndTime = currentTime;
        Duration connectionDuration = new Duration(connectionStartTime, connectionEndTime);

        if(connHistory.get(destination) == null){
            connHistory.put(destination, new ArrayList<Duration>());
        }

        connHistory.get(destination).add(connectionDuration);

        DecisionEngineRouter peerRouter = (DecisionEngineRouter) peer.getRouter();
        List<Message> messagesPeer = new ArrayList<>(peerRouter.getMessageCollection());
        DTNHost destinationPeer = null;

        for (Message msg : messagesPeer) {
            if (msg.getTo().equals(myHost)) { // Jika pesan ini akan dikirim ke peer
                destinationPeer = msg.getTo();
                break; // Asumsikan hanya satu pesan yang relevan untuk contoh ini
            }
            if (msg.getFrom().equals(myHost)) { // Jika pesan ini akan dikirim ke peer
                destinationPeer = msg.getTo();
                break; // Asumsikan hanya satu pesan yang relevan untuk contoh ini
            }
        }
        if(destinationPeer == null){
            return;
        }
        Double connectionStartTimePeer = de.startTimestamps.get(myHost);
        if (connectionStartTimePeer == null) {
            // Jika tidak ada waktu mulai, mungkin ada kesalahan
            System.err.println("Error: No start time found for peer " + myHost);
            return;
        }

        Duration connectionDurationPeer = new Duration(connectionStartTimePeer, connectionEndTime);
        if(de.connHistory.get(destinationPeer) == null){
            de.connHistory.put(destinationPeer, new ArrayList<Duration>());
        }
        de.connHistory.get(destinationPeer).add(connectionDurationPeer);
    }

    private void updateEncounter(DTNHost myHost, DTNHost peer, Connection con) {
        DTNHost destination = null;

        if (destination == null) {
            // Jika tidak ada pesan yang terkait, jangan lakukan apa-apa
            return;
        }

        double currentTime = SimClock.getTime();

        // Buat objek Duration baru
        Double connectionStartTime = startTimestamps.get(peer);
        if (connectionStartTime == null) {
            // Jika tidak ada waktu mulai, mungkin ada kesalahan
            System.err.println("Error: No start time found for peer " + peer);
            return;
        }
        double connectionEndTime = currentTime;
        Duration connectionDuration = new Duration(connectionStartTime, connectionEndTime);

        // Ambil daftar durasi kontak untuk tujuan ini
        List<Duration> durations = connHistory.get(destination);
        if (durations == null) {
            // Jika belum ada daftar, buat baru
            durations = new ArrayList<>();
            connHistory.put(destination, durations);
        }

        // Tambahkan durasi kontak baru ke daftar
        durations.add(connectionDuration);

        //Update StartTimestamps dengan current Time
        startTimestamps.replace(peer, currentTime);
    }

    private SnFDecisionEngineDuration getOtherSnFDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " +
                " with other routers of same type";

        return (SnFDecisionEngineDuration) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    private double getAverageIntercontactTime(DTNHost node, DTNHost destination) {
        List<Duration> durations = connHistory.get(destination);
        if (durations == null || durations.isEmpty()) {
            return Double.MAX_VALUE; // Belum pernah kontak, anggap waktu interkontak sangat besar
        }

        double totalDuration = 0;
        for (Duration duration : durations) {
            totalDuration += duration.end - duration.start;
        }

        return totalDuration / durations.size(); // Rata-rata durasi per kontak
    }


    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNT_PROP, initialNrofCopies);
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost targetHost) {
        Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
        nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
        m.updateProperty(MSG_COUNT_PROP, nrofCopies);

        return m.getTo() == targetHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

//    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        // 1. Cek apakah otherHost adalah tujuan akhir
        if (m.getTo() == otherHost) return true;

        // 2. Ambil jumlah salinan yang tersisa
        int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);

        // 3. Kalau masih ada salinan, forward (Spray Phase)
        if (nrofCopies > 1) return true;

        // 4. Ambil tujuan dari pesan
        DTNHost destination = m.getTo();

        // 5. Hitung Average Intercontact Time antara thisHost dan tujuan
        double myAvgIntercontactTime = getAverageIntercontactTime(thisHost, destination);

        // 6. Hitung Average Intercontact Time antara otherHost dan tujuan
        double otherAvgIntercontactTime = getAverageIntercontactTime(otherHost, destination);

        // 7. Forward jika otherHost memiliki Average Intercontact Time yang lebih kecil (Focus Phase)
        return otherAvgIntercontactTime < myAvgIntercontactTime;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        int nrofCopies;

        nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);

        if (nrofCopies > 1)
            nrofCopies /= 2;
        else
            return true;

        m.updateProperty(MSG_COUNT_PROP, nrofCopies);

        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    @Override
    public void update(DTNHost thisHost) {

    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new SnFDecisionEngineDuration(this);
    }
}