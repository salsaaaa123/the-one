// Version implemntasi code jika menggunkan dataset besar dan kecil

//   public class PeopleRankDecisonEngine implements RoutingDecisionEngine, RankingNodeValue {
//
//    /** Initialitation variable Dumping Factor to employ -setting id */
//    public static final String DUMPING_FACTOR_SETTING = "dumpingFactor";
//    public static final String TRESHOLD_SETTING = "threshold";
//
//    /**
//     * Map to store the PeopleRank values for each host along with the total number
//     * of friends
//     */
//    protected Map<DTNHost, Tuple<Double, Integer>> per; // PeopleRank and friend count for each host
//    protected Map<DTNHost, List<Duration>> connHistory; // Store connection history for each host
//    protected Map<DTNHost, Double> startTimestamps; // Store the start timestamps for each connection
//
//    // Community detection and damping factor
//    protected double dumpingFactor; // Damping factor used in the PeopleRank algorithm
//    protected double treshold; // Threshold for considering connections
//
//    /**
//     * Constructor for PeopleRank based on the specified settings.
//     *
//     * @param s The settings object containing configuration parameters
//     */
//    public PeopleRankDecisonEngine(Settings s) {
//        if (s.contains(DUMPING_FACTOR_SETTING)) {
//            dumpingFactor = s.getDouble(DUMPING_FACTOR_SETTING);
//        } else {
//            this.dumpingFactor = 0.85;
//        }
//        if (s.contains(TRESHOLD_SETTING)) {
//            treshold = s.getDouble(TRESHOLD_SETTING);
//        } else {
//            this.treshold = 700;
//        }
//        connHistory = new HashMap<>();
//        per = new HashMap<>();
//        startTimestamps = new HashMap<>();
//    }
//
//    /**
//     * Copy constructor for PeopleRank.
//     *
//     * @param r The PeopleRank object to replicate
//     */
//    public PeopleRankDecisonEngine(PeopleRankDecisonEngine r) {
//        // Replicate damping factor
//        this.dumpingFactor = r.dumpingFactor;
//        this.treshold = r.treshold;
//        // Initialize necessary maps
//        startTimestamps = new HashMap<>();
//        connHistory = new HashMap<>();
//        per = new HashMap<>();
//    }
//
//    @Override
//    public void connectionUp(DTNHost thisHost, DTNHost peer) {
//
//    }
//
//    @Override
//    public void connectionDown(DTNHost thisHost, DTNHost peer) {
//        // Get the start time of the previous connection and the current time
//        double time = getPreviousConnectionStartTime(thisHost, peer);
//        double etime = SimClock.getTime();
//
//        /**
//         * Check The Total ConnHistory to Find or create the Total Of connection history
//         * list
//         *
//         */
//        List<Duration> history;
//        // Check if there is existing connection history for the peer
//        if (!connHistory.containsKey(peer)) {
//            // If not, create a new list for connection history
//            history = new ArrayList<>();
//            // Put the new list into the connection history map for the peer
//            connHistory.put(peer, history);
//        } else {
//            // If there is existing history, retrieve it
//            history = connHistory.get(peer);
//        }
//
//        /**
//         * Check if the connection duration is greater than or equal to the familiar
//         * threshold
//         * If yes, add this connection to the list
//         */
//        if (etime - time >= treshold) {
//            history.add(new Duration(time, etime));
//            // Update friend count for thisHost
//            updateFriendCount(peer, getFriendCount(peer) + 1); // peer is a friend
//        }
//        updatePeopleRank(thisHost);
//        updatePeopleRank(peer);
//    }
//
//    @Override
//    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
//        // Get the local host from the connection
//        DTNHost myHost = con.getOtherNode(peer);
//        // Get the PeopleRank decision engine of the remote host (peer)
//        PeopleRankDecisonEngine de = this.getOtherDecisionEngine(peer);
//
//        // Update start timestamps for both hosts
//        this.startTimestamps.put(peer, SimClock.getTime());
//        if (de != null) {
//            de.startTimestamps.put(myHost, SimClock.getTime());
//        }
//
//        // Exchange PeopleRank and friend count information
//        double myRank = getPeopleRank(myHost);
//        int myFriendCount = getFriendCount(myHost);
//        Tuple<Double, Integer> myTuple = new Tuple<>(myRank, myFriendCount);
//
//        double peerRank;
//        int peerFriendCount;
//
//        if (de != null) {
//            Tuple<Double, Integer> peerTuple = de.getPeopleRankTuple(peer); // Cek apakah PeopleRank Tuple peer tidak null
//
//            if (peerTuple != null) {
//                peerRank = peerTuple.getFirst(); // Dapatkan Peole Rank
//                peerFriendCount = peerTuple.getSecond(); // Dapatkan Jumlah Teman
//            } else {
//                peerRank = 0.0; // Jika Tuple null, tetapkan rank ke 0.0
//                peerFriendCount = 0; // Jika Tuple null, tetapkan friend count ke 0
//            }
//            per.put(peer, new Tuple<>(peerRank, peerFriendCount));
//
//        } else {
//            peerRank = 0.0;
//            peerFriendCount = 0;
//        }
//
//        // Update this host's information on the peer (if available)
//        if (de != null) {
//            de.per.put(myHost, myTuple);
//        }
//
//    }
//
//    @Override
//    public boolean newMessage(Message m) {
//        return true;
//    }
//
//    @Override
//    public boolean isFinalDest(Message m, DTNHost targetHost) {
//        return m.getTo() == targetHost;
//    }
//
//    @Override
//    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
//        return m.getTo() != thisHost;
//    }
//
//    @Override
//    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
//        // Check if the destination of the message is the other host
//        if (m.getTo() == otherHost) {
//            return true; // Message should be sent directly to the destination
//        }
//
//        // Compare PeopleRank values
//        double thisHostRank = getPeopleRank(thisHost);
//        double otherHostRank = getPeopleRank(otherHost);
//
//        // Forward if the other host has a higher rank or is the destination
//        if (otherHostRank > thisHostRank) {
//            return true;
//        }
//
//        return false;
//    }
//
//    @Override
//    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
//        return false;
//    }
//
//    @Override
//    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
//        return true;
//    }
//
//    @Override
//    public void update(DTNHost thisHost) {
//        updatePeopleRank(thisHost);
//    }
//
//    @Override
//    public RoutingDecisionEngine replicate() {
//        return new PeopleRankDecisonEngine(this);
//    }
//
//    @Override
//    public Map<DTNHost, Double> getAllRankings() {
//        Map<DTNHost, Double> rankings = new HashMap<>();
//        for (Map.Entry<DTNHost, Tuple<Double, Integer>> entry : per.entrySet()) {
//            rankings.put(entry.getKey(), entry.getValue().getFirst());
//        }
//        return rankings;
//    }
//
//    @Override
//    public int getTotalTeman(DTNHost host) {
//        return getFriendCount(host);
//    }
//
//    /**
//     * Calculates the PeopleRank for a given host based on the formula:
//     * PeR(Ni) = (1 - d) + d * Σ PeR(Nj) / |F(Nj)|
//     *
//     * Where:
//     * - PeR(Ni) is the PeopleRank for the current host.
//     * - d is the damping factor obtained from the setting. If not specified, it
//     * defaults to 0.75.
//     * - PeR(Nj) is the ranking of other connected nodes (friends).
//     * - |F(Nj)| is the total number of friends of other nodes.
//     *
//     * @param host The host for which to calculate the PeopleRank.
//     * @return The PeopleRank for the specified host.
//     */
//    private double calculatePeopleRank(DTNHost host) {
//        double rank = (1 - dumpingFactor);
//        double sum = 0.0;
//        List<DTNHost> friends = getFriends(host);
//
//        if (friends == null || friends.isEmpty()) return rank; // No friends.
//
//        for (DTNHost friend : friends) {
//            double friendRank = getPeopleRank(friend);
//            int friendOfFriendCount = getFriendCount(friend);
//
//            if (friendOfFriendCount > 0) {
//                sum += friendRank / friendOfFriendCount;
//            }
//        }
//        rank += dumpingFactor * sum;
//        return rank;
//    }
//
//    private void updatePeopleRank(DTNHost host) {
//        double newRank = calculatePeopleRank(host);
//        int friendCount = getFriendCount(host);
//        per.put(host, new Tuple<>(newRank, friendCount));
//    }
//    private void updateFriendCount(DTNHost host, int newFriendCount) {
//        double currentRank = getPeopleRank(host);
//        per.put(host, new Tuple<>(currentRank, newFriendCount));
//    }
//
//    private PeopleRankDecisonEngine getOtherDecisionEngine(DTNHost h) {
//        MessageRouter otherRouter = h.getRouter();
//        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
//                + " with other routers of same type";
//
//        return (PeopleRankDecisonEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
//    }
//
//    // Helper methods to get PeopleRank and friend count
//    private double getPeopleRank(DTNHost host) {
//        Tuple<Double, Integer> tuple = per.get(host);
//        return (tuple != null) ? tuple.getFirst() : 1.0; // Default rank of 1.0
//    }
//    private Tuple<Double, Integer> getPeopleRankTuple(DTNHost host) {
//        return per.get(host);
//    }
//
//    private int getFriendCount(DTNHost host) {
//        Tuple<Double, Integer> tuple = per.get(host);
//        return (tuple != null) ? tuple.getSecond() : 0;
//    }
//
//    private List<DTNHost> getFriends(DTNHost host) {
//        List<DTNHost> friends = new ArrayList<>();
//        Iterator<Map.Entry<DTNHost, List<Duration>>> iterator = connHistory.entrySet().iterator();
//        while (iterator.hasNext()) {
//            Map.Entry<DTNHost, List<Duration>> entry = iterator.next();
//            if (entry.getKey().equals(host)) {
//                friends.add(entry.getKey());
//            }
//        }
//        return friends;
//    }
//
//    public double getPreviousConnectionStartTime(DTNHost thisHost, DTNHost peer) {
//        // Check if there is a previous connection start time recorded for this host and
//        // peer
//        if (startTimestamps.containsKey(peer)) { // Corrected to check peer
//            // If a record exists, return the start time of the previous connection
//            return startTimestamps.get(peer);
//        } else {
//            // If no record exists, return 0
//            return 0;
//        }
//    }
//}

//public class PeopleRankDecisonEngine implements RoutingDecisionEngine, RankingNodeValue {
//
//    /** Initialitation variable Dumping Factor to employ -setting id */
//    public static final String DUMPING_FACTOR_SETTING = "dumpingFactor";
//    public static final String TRESHOLD_SETTING = "threshold";
//
//    /**
//     * Map to store the PeopleRank values for each host along with the total number
//     * of friends
//     */
//    protected Map<DTNHost, Tuple<Double, Integer>> per; // PeopleRank and friend count for each host
//    protected Map<DTNHost, List<Duration>> connHistory; // Store connection history for each host
//    protected Map<DTNHost, Double> startTimestamps; // Store the start timestamps for each connection
//
//    // Community detection and damping factor
//    protected double dumpingFactor; // Damping factor used in the PeopleRank algorithm
//    protected double treshold; // Threshold for considering connections
//
//    /**
//     * Constructor for PeopleRank based on the specified settings.
//     *
//     * @param s The settings object containing configuration parameters
//     */
//    public PeopleRankDecisonEngine(Settings s) {
//        if (s.contains(DUMPING_FACTOR_SETTING)) {
//            dumpingFactor = s.getDouble(DUMPING_FACTOR_SETTING);
//        } else {
//            this.dumpingFactor = 0.85;
//        }
//        if (s.contains(TRESHOLD_SETTING)) {
//            treshold = s.getDouble(TRESHOLD_SETTING);
//        } else {
//            this.treshold = 700;
//        }
//        connHistory = new HashMap<>();
//        per = new HashMap<>();
//        startTimestamps = new HashMap<>();
//    }
//
//    /**
//     * Copy constructor for PeopleRank.
//     *
//     * @param r The PeopleRank object to replicate
//     */
//    public PeopleRankDecisonEngine(PeopleRankDecisonEngine r) {
//        // Replicate damping factor
//        this.dumpingFactor = r.dumpingFactor;
//        this.treshold = r.treshold;
//        // Initialize necessary maps
//        startTimestamps = new HashMap<>();
//        connHistory = new HashMap<>();
//        per = new HashMap<>();
//    }
//
//    @Override
//    public void connectionUp(DTNHost thisHost, DTNHost peer) {
//
//    }
//
//    @Override
//    public void connectionDown(DTNHost thisHost, DTNHost peer) {
//        // Get the start time of the previous connection and the current time
//        double time = getPreviousConnectionStartTime(thisHost, peer);
//        double etime = SimClock.getTime();
//
//        /**
//         * Check The Total ConnHistory to Find or create the Total Of connection history
//         * list
//         *
//         */
//        List<Duration> history;
//        // Check if there is existing connection history for the peer
//        if (!connHistory.containsKey(peer)) {
//            // If not, create a new list for connection history
//            history = new ArrayList<>();
//            // Put the new list into the connection history map for the peer
//            connHistory.put(peer, history);
//        } else {
//            // If there is existing history, retrieve it
//            history = connHistory.get(peer);
//        }
//
//        /**
//         * Check if the connection duration is greater than or equal to the familiar
//         * threshold
//         * If yes, add this connection to the list
//         */
//        if (etime - time >= treshold) {
//            history.add(new Duration(time, etime));
//            // Update friend count for thisHost
//            updateFriendCount(peer, calculateFriendCount(peer)); // Recalculate friend count
//        }
//        updatePeopleRank(thisHost);
//        updatePeopleRank(peer);
//    }
//
//    @Override
//    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
//        // Get the local host from the connection
//        DTNHost myHost = con.getOtherNode(peer);
//        // Get the PeopleRank decision engine of the remote host (peer)
//        PeopleRankDecisonEngine de = this.getOtherDecisionEngine(peer);
//
//        // Update start timestamps for both hosts
//        this.startTimestamps.put(peer, SimClock.getTime());
//        if (de != null) {
//            de.startTimestamps.put(myHost, SimClock.getTime());
//        }
//
//        // Exchange PeopleRank and friend count information
//        double myRank = getPeopleRank(myHost);
//        int myFriendCount = calculateFriendCount(myHost);
//        Tuple<Double, Integer> myTuple = new Tuple<>(myRank, myFriendCount);
//
//        double peerRank;
//        int peerFriendCount;
//
//        if (de != null) {
//            Tuple<Double, Integer> peerTuple = de.getPeopleRankTuple(peer); // Cek apakah PeopleRank Tuple peer tidak null
//
//            if (peerTuple != null) {
//                peerRank = peerTuple.getFirst(); // Dapatkan Peole Rank
//                peerFriendCount = peerTuple.getSecond(); // Dapatkan Jumlah Teman
//            } else {
//                peerRank = 0.0; // Jika Tuple null, tetapkan rank ke 0.0
//                peerFriendCount = 0; // Jika Tuple null, tetapkan friend count ke 0
//            }
//            per.put(peer, new Tuple<>(peerRank, peerFriendCount));
//
//        } else {
//            peerRank = 0.0;
//            peerFriendCount = 0;
//        }
//
//
//        // Update this host's information on the peer (if available)
//        if (de != null) {
//            de.per.put(myHost, myTuple);
//        }
//
//    }
//
//    @Override
//    public boolean newMessage(Message m) {
//        return true;
//    }
//
//    @Override
//    public boolean isFinalDest(Message m, DTNHost targetHost) {
//        return m.getTo() == targetHost;
//    }
//
//    @Override
//    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
//        return m.getTo() != thisHost;
//    }
//
//    @Override
//    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
//        // Check if the destination of the message is the other host
//        if (m.getTo() == otherHost) {
//            return true; // Message should be sent directly to the destination
//        }
//
//        // Compare PeopleRank values
//        double thisHostRank = getPeopleRank(thisHost);
//        double otherHostRank = getPeopleRank(otherHost);
//
//        // Forward if the other host has a higher rank or is the destination
//        if (otherHostRank > thisHostRank) {
//            return true;
//        }
//
//        return false;
//    }
//
//    @Override
//    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
//        return false;
//    }
//
//    @Override
//    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
//        return true;
//    }
//
//    @Override
//    public void update(DTNHost thisHost) {
//        updatePeopleRank(thisHost);
//    }
//
//    @Override
//    public RoutingDecisionEngine replicate() {
//        return new PeopleRankDecisonEngine(this);
//    }
//
//    @Override
//    public Map<DTNHost, Double> getAllRankings() {
//        Map<DTNHost, Double> rankings = new HashMap<>();
//        for (Map.Entry<DTNHost, Tuple<Double, Integer>> entry : per.entrySet()) {
//            rankings.put(entry.getKey(), entry.getValue().getFirst());
//        }
//        return rankings;
//    }
//
//    @Override
//    public int getTotalTeman(DTNHost host) {
//        return calculateFriendCount(host);
//    }
//
//    /**
//     * Calculates the PeopleRank for a given host based on the formula:
//     * PeR(Ni) = (1 - d) + d * Σ PeR(Nj) / |F(Nj)|
//     *
//     * Where:
//     * - PeR(Ni) is the PeopleRank for the current host.
//     * - d is the damping factor obtained from the setting. If not specified, it
//     * defaults to 0.75.
//     * - PeR(Nj) is the ranking of other connected nodes (friends).
//     * - |F(Nj)| is the total number of friends of other nodes.
//     *
//     * @param host The host for which to calculate the PeopleRank.
//     * @return The PeopleRank for the specified host.
//     */
//    private double calculatePeopleRank(DTNHost host) {
//        double rank = (1 - dumpingFactor);
//        double sum = 0.0;
//        List<DTNHost> friends = getFriends(host);
//
//        if (friends == null || friends.isEmpty()) return rank; // No friends.
//
//        for (DTNHost friend : friends) {
//            double friendRank = getPeopleRank(friend);
//            int friendOfFriendCount = calculateFriendCount(friend);
//
//            if (friendOfFriendCount > 0) {
//                sum += friendRank / friendOfFriendCount;
//            }
//        }
//        rank += dumpingFactor * sum;
//        return rank;
//    }
//
//    private void updatePeopleRank(DTNHost host) {
//        double newRank = calculatePeopleRank(host);
//        int friendCount = calculateFriendCount(host);
//        per.put(host, new Tuple<>(newRank, friendCount));
//    }
//
//    private void updateFriendCount(DTNHost host, int newFriendCount) {
//        double currentRank = getPeopleRank(host);
//        per.put(host, new Tuple<>(currentRank, newFriendCount));
//    }
//
//    private PeopleRankDecisonEngine getOtherDecisionEngine(DTNHost h) {
//        MessageRouter otherRouter = h.getRouter();
//        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
//                + " with other routers of same type";
//
//        return (PeopleRankDecisonEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
//    }
//
//    // Helper methods to get PeopleRank and friend count
//    private double getPeopleRank(DTNHost host) {
//        Tuple<Double, Integer> tuple = per.get(host);
//        return (tuple != null) ? tuple.getFirst() : 1.0; // Default rank of 1.0
//    }
//
//    private Tuple<Double, Integer> getPeopleRankTuple(DTNHost host) {
//        return per.get(host);
//    }
//
//    private int calculateFriendCount(DTNHost host) {
//        int friendCount = 0;
//        if(connHistory.containsKey(host)){
//            List<Duration> durations = connHistory.get(host);
//            if(durations != null){
//                friendCount = durations.size();
//            }
//        }
//        return friendCount;
//    }
//
//    private List<DTNHost> getFriends(DTNHost host) {
//        List<DTNHost> friends = new ArrayList<>();
//        Iterator<Map.Entry<DTNHost, List<Duration>>> iterator = connHistory.entrySet().iterator();
//        while (iterator.hasNext()) {
//            Map.Entry<DTNHost, List<Duration>> entry = iterator.next();
//            if (entry.getKey().equals(host)) {
//                friends.add(entry.getKey());
//            }
//        }
//        return friends;
//    }
//
//    public double getPreviousConnectionStartTime(DTNHost thisHost, DTNHost peer) {
//        // Check if there is a previous connection start time recorded for this host and
//        // peer
//        if (startTimestamps.containsKey(peer)) { // Corrected to check peer
//            // If a record exists, return the start time of the previous connection
//            return startTimestamps.get(peer);
//        } else {
//            // If no record exists, return 0
//            return 0;
//        }
//    }
//}