package routing.activerouter;

import core.*;
import routing.ActiveRouter;
import routing.MessageRouter;
import routing.community.Duration;
import routing.peoplerank.PeopleRankInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeopleRankActiveRouter extends ActiveRouter {

    public static final String PEOPLERANK_NS = "PeopleRankActiveRouter";
    public static final String DAMPING = "dampingFactor";
    public static final String DURATION_THRESHOLD = "durationThreshold";

    private static final double DEFAULT_PEOPLE_RANK = 0.0;
    private double dampingFactor;
    private double durationThreshold;

    private double peopleRank;

    private Map<DTNHost, List<Duration>> connHistory; // Riwayat koneksi
    private Map<DTNHost, PeopleRankInfo> peerInfo;
    private Map<DTNHost, Double> startTime;


    public PeopleRankActiveRouter(Settings s) {
        super(s);
        Settings prSettings = new Settings(PEOPLERANK_NS);
        if (prSettings.contains(DAMPING)){
            dampingFactor=prSettings.getDouble(DAMPING);
        }else{
            dampingFactor=0.85;
        }
        if (prSettings.contains(DURATION_THRESHOLD)){
            durationThreshold=prSettings.getDouble(DURATION_THRESHOLD);
        }else{
            durationThreshold=60.0;
        }

        connHistory = new HashMap<>();
        peerInfo = new HashMap<>();
        startTime = new HashMap<>();
        peopleRank = DEFAULT_PEOPLE_RANK;

    }

    protected PeopleRankActiveRouter(PeopleRankActiveRouter proto) {
        super(proto);
        this.dampingFactor = proto.dampingFactor;
        this.durationThreshold = proto.durationThreshold;
        this.peopleRank = proto.peopleRank;

        this.connHistory = new HashMap<>(proto.connHistory);
        this.peerInfo = new HashMap<>(proto.peerInfo);
        this.startTime = new HashMap<>(proto.startTime);
    }

    @Override
    public MessageRouter replicate() {
        return new PeopleRankActiveRouter(this);
    }

    /**
     * Called when a connection's state changes.
     */
    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con); // Handle energy model

        DTNHost peer = con.getOtherNode(getHost());
        double currentTime = SimClock.getTime();

        if (con.isUp()) {
            startTime.put(peer, currentTime);
        } else {
            updateConnHistory(peer, currentTime);
            startTime.remove(peer);
            doExchangeForNewConnection(peer);
            updatePeopleRank(); // Update PeopleRank on connection down
        }
    }


    private void updateConnHistory(DTNHost peer, double currentTime) {
        Double connectionStartTime = startTime.get(peer);
        if (connectionStartTime == null) {
            return;
        }

        Duration newDuration = new Duration(connectionStartTime, currentTime);

        //Perbarui riwayat koneksi
        List<Duration> durations = connHistory.computeIfAbsent(peer, k -> new ArrayList<>());
        durations.add(newDuration);
    }

    @Override
    public void update() {
        super.update(); // Handle ActiveRouter's update tasks
        //Do nothing (PeopleRank is updated on connection changes)

        //Let ActiveRouter handle message transfers
        tryAllMessagesToAllConnections();
    }

    private void doExchangeForNewConnection(DTNHost peer) {
        DTNHost myHost = getHost();

        // Kirim PeopleRank kita ke peer
        PeopleRankInfo myInfo = new PeopleRankInfo(peopleRank, getNeighborCount());
        sendPeopleRank(peer, myInfo);

        // Terima PeopleRank peer
        PeopleRankInfo peerInfo = receivePeopleRank(peer);
        if (peerInfo != null) {
            this.peerInfo.put(peer, peerInfo);
        }
    }
    private void sendPeopleRank(DTNHost peer, PeopleRankInfo info) {
        PeopleRankActiveRouter otherRouter = getOtherRouter(peer);
        otherRouter.receivePeopleRankInfo(getHost(), info); // Panggil metode khusus
    }

    // Metode khusus untuk menerima PeopleRankInfo
    public void receivePeopleRankInfo(DTNHost from, PeopleRankInfo info) {
        this.peerInfo.put(from, info);
    }

    private PeopleRankInfo receivePeopleRank(DTNHost peer) {
        PeopleRankActiveRouter otherRouter = getOtherRouter(peer);
        if (otherRouter != null) {
            return otherRouter.getPeopleRankInfo(getHost()); // Panggil metode khusus
        }
        return null;
    }

    // Metode khusus untuk mendapatkan PeopleRankInfo
    public PeopleRankInfo getPeopleRankInfo(DTNHost forHost) {
        return this.peerInfo.get(forHost);
    }

    private void updatePeopleRank() {
        double sum = 0.0;
        int neighborCount = getNeighborCount();

        if (neighborCount > 0) {
            for (DTNHost neighbor : connHistory.keySet()) {
                PeopleRankInfo neighborInfo = peerInfo.get(neighbor);
                if (neighborInfo != null) {
                    sum += neighborInfo.peopleRank / neighborCount;
                }
            }
        }

        peopleRank = (1 - dampingFactor) + dampingFactor * sum;
    }

    private int getNeighborCount() {
        return connHistory.size();
    }

    private PeopleRankActiveRouter getOtherRouter(DTNHost host) {
        MessageRouter router = host.getRouter();
        if (router instanceof PeopleRankActiveRouter) {
            return (PeopleRankActiveRouter) router;
        }
        return null;
    }

    /**
     * Returns a list of connections this host currently has with other hosts.
     * @return a list of connections this host currently has with other hosts
     */
    protected List<Connection> getConnections() {
        return getHost().getConnections();
    }

    @Override
    public boolean requestDeliverableMessages(Connection con) {
        if (isTransferring()) {
            return false;
        }

        DTNHost other = con.getOtherNode(getHost());
        /* do a copy to avoid concurrent modification exceptions
         * (startTransfer may remove messages) */
        ArrayList<Message> temp =
                new ArrayList<Message>(this.getMessageCollection());
        for (Message m : temp) {
            if (other == m.getTo() && shouldForwardMessage(m, other)) {
                if (startTransfer(m, con) == RCV_OK) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Checks if router "wants" to start receiving message (i.e. router
     * isn't transferring, doesn't have the message and has room for it).
     * @param m The message to check
     * @return A return code similar to
     * {@link MessageRouter#receiveMessage(Message, DTNHost)}, i.e.
     * {@link MessageRouter#RCV_OK} if receiving seems to be OK,
     * TRY_LATER_BUSY if router is transferring, DENIED_OLD if the router
     * is already carrying the message or it has been delivered to
     * this router (as final recipient), or DENIED_NO_SPACE if the message
     * does not fit into buffer
     */
    @Override
    protected int checkReceiving(Message m) {
        if (isTransferring()) {
            return TRY_LATER_BUSY; // only one connection at a time
        }

        if ( hasMessage(m.getId()) || isDeliveredMessage(m) ){
            return DENIED_OLD; // already seen this message -> reject it
        }

        if (m.getTtl() <= 0 && m.getTo() != getHost()) {
            /* TTL has expired and this host is not the final recipient */
            return DENIED_TTL;
        }

        if (!makeRoomForMessage(m.getSize())) {
            return DENIED_NO_SPACE; // couldn't fit into buffer -> reject
        }

        return RCV_OK;
    }

    private boolean shouldForwardMessage(Message m, DTNHost other) {
        // 1. Check jika other adalah tetangga(ada di connHistory)
        if (!connHistory.containsKey(other)) return false;

        // 2. Apakah kita adalah tujuan akhir? Jika ya, kirim saja
        if (m.getTo().equals(other)) return true;

        // 3. Cek apakah peer memiliki PeopleRank yang lebih tinggi dari kita
        PeopleRankActiveRouter otherRouter = getOtherRouter(other);
        return otherRouter != null && otherRouter.getPeopleRank() >= this.peopleRank;
    }

    public double getPeopleRank() {
        return peopleRank;
    }
}
