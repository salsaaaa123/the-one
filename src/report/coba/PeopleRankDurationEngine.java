///*
// * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
// */
//
//
//package report.coba;
//
//import core.*;
//import routing.DecisionEngineRouter;
//import routing.MessageRouter;
//import routing.RoutingDecisionEngine;
//import routing.peoplerank.NodeRanking;
//import routing.peoplerank.PeopleRankInfo;
//import routing.peoplerank.SocialInteraction;
//
//import java.util.*;
//
//public class PeopleRankDurationEngine implements RoutingDecisionEngine, NodeRanking {
//
//    public static final String PEOPLERANK_NS = "PeopleRankDurationEngine";
//    public static final String DAMPING = "dampingFactor";
//    public static final String DURATION_THRESHOLD = "durationThreshold";
//    public static final String RANK_NORMALIZATION = "rankNormalization";
//
//    private static final double DEFAULT_PEOPLE_RANK = 0.0;
//
//    private double previousPeopleRank;
//    private double dampingFactor;
//    private double peopleRank;
//    private double durationThreshold;
//    private boolean rankNormalization;
//
//    private Map<DTNHost, Double> startTime;
//    private Map<DTNHost, SocialInteraction> socialInteractions;
//    private Map<DTNHost, PeopleRankInfo> peerInfo;
//    private List<Message> messageBuffer;
//
//    private Map<DTNHost, Set<DTNHost>> socialGraph = new HashMap<DTNHost, Set<DTNHost>>();
//    private boolean isSocialGraphShared = true;
//
//    public PeopleRankDurationEngine(Settings s) {
//        Settings prSettings = new Settings(PEOPLERANK_NS);
//
//        if (prSettings.contains(DAMPING)) {
//            dampingFactor = prSettings.getDouble(DAMPING);
//        } else {
//            dampingFactor = 0.5;
//        }
//
//        if (prSettings.contains(DURATION_THRESHOLD)) {
//            durationThreshold = prSettings.getDouble(DURATION_THRESHOLD);
//        } else {
//            durationThreshold = 30.0;
//        }
//
//        if (prSettings.contains(RANK_NORMALIZATION)) {
//            rankNormalization = prSettings.getBoolean(RANK_NORMALIZATION);
//        } else {
//            rankNormalization = false;
//        }
//
//        startTime = new HashMap<DTNHost, Double>();
//        socialInteractions = new HashMap<DTNHost, SocialInteraction>();
//        peerInfo = new HashMap<DTNHost, PeopleRankInfo>();
//        messageBuffer = new ArrayList<Message>();
//
//        peopleRank = DEFAULT_PEOPLE_RANK;
//        previousPeopleRank = DEFAULT_PEOPLE_RANK;
//
//        System.out.println("Duration Threshold: " + durationThreshold);
//        System.out.println("Rank Normalization: " + rankNormalization);
//    }
//
//    public PeopleRankDurationEngine(PeopleRankDurationEngine proto) {
//        dampingFactor = proto.dampingFactor;
//        durationThreshold = proto.durationThreshold;
//        rankNormalization = proto.rankNormalization;
//        peopleRank = proto.peopleRank;
//        previousPeopleRank = proto.previousPeopleRank;
//
//        startTime = new HashMap<DTNHost, Double>();
//        socialInteractions = new HashMap<DTNHost, SocialInteraction>(proto.socialInteractions);
//        peerInfo = new HashMap<DTNHost, PeopleRankInfo>(proto.peerInfo);
//        messageBuffer = new ArrayList<Message>();
//
//        this.socialGraph = proto.socialGraph;
//        this.isSocialGraphShared = false;
//    }
//
//    @Override
//    public void connectionUp(DTNHost thisHost, DTNHost peer) {
//        double start = SimClock.getTime();
//        if (!socialInteractions.containsKey(peer)) {
//            socialInteractions.put(peer, new SocialInteraction());
//        }
//        startTime.put(peer, start);
//    }
//
//    @Override
//    public void connectionDown(DTNHost thisHost, DTNHost peer) {
//        double end = SimClock.getTime();
//        double start = check(peer);
//
//        if (start > 0) {
//            SocialInteraction interaction = socialInteractions.get(peer);
//            if (interaction != null) {
//                interaction.addInteraction(start, end);
//            }
//        }
//
//        startTime.remove(peer);
//        updateSocialGraph(thisHost, peer);
//        processMessageBuffer(thisHost, peer);
//    }
//
//    @Override
//    public void update(DTNHost thisHost) {
//        updateSocialGraphPeriodic(thisHost);
//        updatePeopleRank(thisHost);
//    }
//
//    private void updateSocialGraphPeriodic(DTNHost thisHost) {
//        Set<DTNHost> keys = new HashSet<DTNHost>(socialInteractions.keySet());
//        Iterator<DTNHost> it = keys.iterator();
//        while (it.hasNext()) {
//            DTNHost peer = it.next();
//            if (!peer.equals(thisHost)) {
//                updateSocialGraph(thisHost, peer);
//            }
//        }
//    }
//
//    private void updateSocialGraph(DTNHost thisHost, DTNHost peer) {
//        ensureSocialGraphIsMutable();
//        SocialInteraction interaction = socialInteractions.get(peer);
//        double totalDuration = 0.0;
//        if (interaction != null) {
//            totalDuration = interaction.getTotalDuration();
//        }
//
//        if (totalDuration >= durationThreshold) {
//            addNeighbor(thisHost, peer);
//            addNeighbor(peer, thisHost);
//        } else {
//            removeNeighbor(thisHost, peer);
//            removeNeighbor(peer, thisHost);
//        }
//    }
//
//    private void addNeighbor(DTNHost node, DTNHost neighbor) {
//        ensureSocialGraphIsMutable();
//        if (!socialGraph.containsKey(node)) {
//            socialGraph.put(node, new HashSet<DTNHost>());
//        }
//        socialGraph.get(node).add(neighbor);
//    }
//
//    private void removeNeighbor(DTNHost node, DTNHost neighbor) {
//        ensureSocialGraphIsMutable();
//        if (socialGraph.containsKey(node)) {
//            socialGraph.get(node).remove(neighbor);
//        }
//    }
//
//    private void ensureSocialGraphIsMutable() {
//        if (isSocialGraphShared) {
//            Map<DTNHost, Set<DTNHost>> newGraph = new HashMap<DTNHost, Set<DTNHost>>();
//            Iterator<Map.Entry<DTNHost, Set<DTNHost>>> it = socialGraph.entrySet().iterator();
//            while (it.hasNext()) {
//                Map.Entry<DTNHost, Set<DTNHost>> entry = it.next();
//                newGraph.put(entry.getKey(), new HashSet<DTNHost>(entry.getValue()));
//            }
//            socialGraph = newGraph;
//            isSocialGraphShared = false;
//        }
//    }
//
//    @Override
//    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
//        DTNHost myHost = con.getOtherNode(peer);
//        double currentPeopleRank = getPeopleRank();
//        double diff = Math.abs(currentPeopleRank - previousPeopleRank);
//
//        if (diff > 0.01) {
//            PeopleRankInfo data = new PeopleRankInfo(currentPeopleRank, getNeighborCount(myHost));
//            send(myHost, peer, data);
//            previousPeopleRank = currentPeopleRank;
//        }
//
//        PeopleRankInfo peerData = receive(peer);
//        peerInfo.put(peer, peerData);
//        updateSocialGraph(myHost, peer);
//    }
//
//    private void send(DTNHost myHost, DTNHost peer, PeopleRankInfo info) {
//        PeopleRankDurationEngine prde = getDecisionRouterFrom(peer);
//        prde.peerInfo.put(myHost, info);
//    }
//
//    private PeopleRankInfo receive(DTNHost peer) {
//        PeopleRankDurationEngine prde = getDecisionRouterFrom(peer);
//        return new PeopleRankInfo(prde.getPeopleRank(), prde.getNeighborCount(peer));
//    }
//
//    public void updatePeopleRank(DTNHost myHost) {
//        double sigma = 0.0;
//        Iterator<DTNHost> it = getNeighbors(myHost).iterator();
//        while (it.hasNext()) {
//            DTNHost neighbor = it.next();
//            if (peerInfo.containsKey(neighbor)) {
//                PeopleRankInfo info = peerInfo.get(neighbor);
//                sigma += info.peopleRank / (double) info.neighborCount;
//            }
//        }
//
//        peopleRank = (1 - dampingFactor) + dampingFactor * sigma;
//
//        if (rankNormalization) {
//            normalizePeopleRank();
//        }
//    }
//
//    private void normalizePeopleRank() {
//        double minRank = Double.MAX_VALUE;
//        double maxRank = Double.MIN_VALUE;
//
//        Iterator<DTNHost> it = socialGraph.keySet().iterator();
//        while (it.hasNext()) {
//            DTNHost node = it.next();
//            PeopleRankDurationEngine prde = getDecisionRouterFrom(node);
//            double rank = prde.getPeopleRank();
//            if (rank < minRank) minRank = rank;
//            if (rank > maxRank) maxRank = rank;
//        }
//
//        if (maxRank - minRank > 0) {
//            Iterator<DTNHost> it2 = socialGraph.keySet().iterator();
//            while (it2.hasNext()) {
//                DTNHost node = it2.next();
//                PeopleRankDurationEngine prde = getDecisionRouterFrom(node);
//                double normalizedRank = (prde.getPeopleRank() - minRank) / (maxRank - minRank);
//                prde.setPeopleRank(normalizedRank);
//            }
//        }
//    }
//
//    private void setPeopleRank(double rank) {
//        this.peopleRank = rank;
//    }
//
//    private List<DTNHost> getNeighbors(DTNHost node) {
//        Set<DTNHost> neighbors = socialGraph.get(node);
//        if (neighbors != null) {
//            return new ArrayList<DTNHost>(neighbors);
//        } else {
//            return new ArrayList<DTNHost>();
//        }
//    }
//
//    private double check(DTNHost peer) {
//        if (startTime.containsKey(peer)) {
//            return startTime.get(peer);
//        } else {
//            return 0.0;
//        }
//    }
//
//    public int getNeighborCount(DTNHost host) {
//        return getNeighbors(host).size();
//    }
//
//    @Override
//    public double getPeopleRank() {
//        return this.peopleRank;
//    }
//
//    @Override
//    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
//        if (!getNeighbors(thisHost).contains(otherHost)) return false;
//        if (m.getTo().equals(otherHost)) return true;
//
//        PeopleRankDurationEngine prde = getDecisionRouterFrom(otherHost);
//        return prde.getPeopleRank() >= peopleRank;
//    }
//
//    private PeopleRankDurationEngine getDecisionRouterFrom(DTNHost h) {
//        MessageRouter otherRouter = h.getRouter();
//        if (otherRouter instanceof DecisionEngineRouter) {
//            RoutingDecisionEngine engine = ((DecisionEngineRouter) otherRouter).getDecisionEngine();
//            if (engine instanceof PeopleRankDurationEngine) {
//                return (PeopleRankDurationEngine) engine;
//            } else {
//                throw new IllegalStateException("DecisionEngine is not PeopleRankDurationEngine!");
//            }
//        } else {
//            throw new IllegalStateException("Router is not a DecisionEngineRouter!");
//        }
//    }
//
//    @Override
//    public boolean newMessage(Message m) {
//        messageBuffer.add(m);
//        return true;
//    }
//
//    @Override
//    public boolean isFinalDest(Message m, DTNHost aHost) {
//        return m.getTo() == aHost;
//    }
//
//    @Override
//    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
//        return !m.getTo().equals(thisHost);
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
//    private void processMessageBuffer(DTNHost thisHost, DTNHost peer) {
//        List<Message> toRemove = new ArrayList<Message>();
//        Iterator<Message> it = messageBuffer.iterator();
//        while (it.hasNext()) {
//            Message m = it.next();
//            if (shouldSendMessageToHost(m, peer, thisHost)) {
//                forwardMessage(m, peer, thisHost);
//                toRemove.add(m);
//            }
//        }
//        messageBuffer.removeAll(toRemove);
//    }
//
//    private void forwardMessage(Message m, DTNHost peer, DTNHost thisHost) {
//        MessageRouter peerRouter = peer.getRouter();
//        if (peerRouter instanceof DecisionEngineRouter) {
//            ((DecisionEngineRouter) peerRouter).receiveMessage(m, thisHost);
//        } else {
//            System.err.println("Peer router is not DecisionEngineRouter");
//        }
//    }
//
//    @Override
//    public RoutingDecisionEngine replicate() {
//        return new PeopleRankDurationEngine(this);
//    }
//
//    @Override
//    public boolean hasHigherRankThan(NodeRanking otherNode) {
//        return otherNode == null || getPeopleRank() > otherNode.getPeopleRank();
//    }
//
//    @Override
//    public Map<DTNHost, Double> getAllRank() {
//        Map<DTNHost, Double> ranks = new HashMap<DTNHost, Double>();
//        Iterator<DTNHost> it = socialGraph.keySet().iterator();
//
//        while (it.hasNext()) {
//            DTNHost node = it.next();
//            PeopleRankDurationEngine prde = getDecisionRouterFrom(node);
//            double rank = prde.getPeopleRank();
//            ranks.put(node, rank);
//        }
//
//        return ranks;
//    }
//
//}
//
////    private static class PeopleRankInfo implements java.io.Serializable {
////        public double peopleRank;
////        public int neighborCount;
////
////        public PeopleRankInfo(double peopleRank, int neighborCount) {
////            this.peopleRank = peopleRank;
////            this.neighborCount = neighborCount;
////        }
////    }
//
//
////package routing.peoplerank;
////
////import core.Connection;
////import core.DTNHost;
////import core.Message;
////import core.Settings;
////import core.SimClock;
////import java.util.*;
////import routing.DecisionEngineRouter;
////import routing.MessageRouter;
////import routing.RoutingDecisionEngine;
////import routing.community.Duration;
////
////public class PeopleRankDurationEngine implements RoutingDecisionEngine, NodeRanking {
////
////    public static final String PEOPLERANK_NS = "PeopleRankDurationEngine";
////    public static final String DAMPING = "dampingFactor";
////    public static final String DURATION_THRESHOLD = "durationThreshold";
////
////    private static final double DEFAULT_PEOPLE_RANK = 1.0;
////    private double previousPeopleRank;
////    protected double dampingFactor;
////    protected double peopleRank;
////    protected double durationThreshold;
////
////    protected Map<DTNHost, Double> startTime;
////    protected Map<DTNHost, List<Duration>> connectionDurationHistory;
////    protected Map<DTNHost, PeopleRankInfo> peerInfo;
////    protected List<Message> messageBuffer;
////    protected Set<DTNHost> socialGraph;
////
////    public PeopleRankDurationEngine(Settings s) {
////        Settings prSettings = new Settings(PEOPLERANK_NS);
////
////        if (prSettings.contains(DAMPING)) {
////            dampingFactor = prSettings.getDouble(DAMPING);
////
////        } else {
////            dampingFactor = 0.5;
////        }
////
////
////        if (prSettings.contains(DURATION_THRESHOLD)) {
////            durationThreshold = prSettings.getDouble(DURATION_THRESHOLD);
////        } else {
////            durationThreshold = 60.0;
////        }
////
////        startTime = new HashMap<>();
////        connectionDurationHistory = new HashMap<>();
////        peerInfo = new HashMap<>();
////        messageBuffer = new ArrayList<>();
////        socialGraph = new HashSet<>();
////
////        this.peopleRank = DEFAULT_PEOPLE_RANK;
////        this.previousPeopleRank = DEFAULT_PEOPLE_RANK;
////
////        System.out.println("Duration Threshold: " + durationThreshold);
////    }
////
////    public PeopleRankDurationEngine(PeopleRankDurationEngine proto) {
////        this.dampingFactor = proto.dampingFactor;
////        this.durationThreshold = proto.durationThreshold;
////        this.peopleRank = proto.peopleRank;
////        this.previousPeopleRank = proto.previousPeopleRank;
////        startTime = new HashMap<>();
////        connectionDurationHistory = new HashMap<>(proto.connectionDurationHistory);
////        peerInfo = new HashMap<>();
////        messageBuffer = new ArrayList<>();
////        socialGraph = new HashSet<>(proto.socialGraph);
////    }
////
//    @Override
//    public void connectionUp(DTNHost thisHost, DTNHost peer) {
//        startTime.put(peer, SimClock.getTime());
//    }
//
//    @Override
//    public void connectionDown(DTNHost thisHost, DTNHost peer) {
//        double start = check(peer);
//        double end = SimClock.getTime();
//
//        List<Duration> history;
//
//        if (!connectionDurationHistory.containsKey(peer)) {
//            history = new ArrayList<>();
//            connectionDurationHistory.put(peer, history);
//        } else {
//            history = connectionDurationHistory.get(peer);
//        }
//
//        Duration meeting = new Duration(start, end);
//        history.add(meeting);
//
//        startTime.remove(peer);
//        updateSocialGraph(thisHost, peer);
//        processMessageBuffer(thisHost, peer);
//    }
//
//    private void updateSocialGraph(DTNHost thisHost, DTNHost peer) {
//        double totalMeetingTime = calculateTotalMeetingTimeWith(peer);
//        if (totalMeetingTime >= durationThreshold) {
//            socialGraph.add(peer);
//            socialGraph.add(thisHost);
//        } else {
//            socialGraph.remove(peer);
//            socialGraph.remove(thisHost);
//        }
//    }
//
//    private double calculateTotalMeetingTimeWith(DTNHost peer) {
//        if (!connectionDurationHistory.containsKey(peer)) {
//            return 0;
//        }
//
//        double total = 0;
//        List<Duration> meetingList = connectionDurationHistory.get(peer);
//        for (Duration duration : meetingList) {
//            total += (duration.getEnd() - duration.getStart());
//        }
//
//        return total;
//    }
//
//    @Override
//    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
//        DTNHost myHost = con.getOtherNode(peer);
//        double currentPeopleRank = getPeopleRank();
//
//        if (Math.abs(currentPeopleRank - previousPeopleRank) > 0.01) {
//            PeopleRankInfo data = new PeopleRankInfo(currentPeopleRank, getNeighborCount());
//            send(myHost, peer, data);
//            previousPeopleRank = currentPeopleRank;
//        }
//
//        PeopleRankInfo peerData = receive(peer);
//        this.peerInfo.put(peer, peerData);
//
//        updateSocialGraph(myHost, peer);
//    }
//
//    private void send(DTNHost myHost, DTNHost peer, PeopleRankInfo information) {
//        PeopleRankDurationEngine prde = getDecisionRouterFrom(peer);
//        prde.peerInfo.put(myHost, information);
//    }
//
//    private PeopleRankInfo receive(DTNHost peer) {
//        PeopleRankDurationEngine prde = getDecisionRouterFrom(peer);
//        double peerRank = prde.getPeopleRank();
//        int peerFriends = prde.getNeighborCount();
//
//        return new PeopleRankInfo(peerRank, peerFriends);
//    }
//
//    @Override
//    public void update(DTNHost thisHost) {
//        updatePeopleRank(thisHost);
//    }
//
//    public void updatePeopleRank(DTNHost myHost) {
//        double sigma = 0;
//        double totalRank = 0; // NEW: Total PeopleRank dalam jaringan
//
//        for (DTNHost neighbor : socialGraph) {
//            if (neighbor == myHost) continue;
//            if (peerInfo.containsKey(neighbor)) {
//                PeopleRankInfo neighborInfo = peerInfo.get(neighbor);
//                sigma += neighborInfo.peopleRank / (double) neighborInfo.neighborCount;
//                totalRank += neighborInfo.peopleRank; // NEW
//            }
//        }
//
//        if (totalRank > 0) { // NEW: Normalisasi dengan total network rank
//            this.peopleRank = ((1 - dampingFactor) + dampingFactor * sigma) / totalRank;
//        } else {
//            this.peopleRank = (1 - dampingFactor) + dampingFactor * sigma;
//        }
//    }
//
//
//    @Override
//    public double getPeopleRank() {
//        return this.peopleRank;
//    }
//
//    public int getNeighborCount() {
//        return socialGraph.size();
//    }
//
//    @Override
//    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
//        if (!socialGraph.contains(otherHost)) return false;
//
//        PeopleRankDurationEngine prde = getDecisionRouterFrom(otherHost);
//        double otherHostRank = prde.getPeopleRank();
//
//        // NEW: Batasi hop count
//        if (m.getHopCount() > 5) {
//            return false;
//        }
//
//        return (otherHostRank >= this.peopleRank || m.getTo().equals(otherHost));
//    }
//
//
//    public PeopleRankDurationEngine getDecisionRouterFrom(DTNHost h) {
//        MessageRouter otherRouter = h.getRouter();
//        if (otherRouter == null || !(otherRouter instanceof DecisionEngineRouter)) {
//            throw new IllegalStateException("Router tidak valid atau tidak ditemukan!");
//        }
//        return (PeopleRankDurationEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
//    }
//
//    private double check(DTNHost peer) {
//        if (startTime.containsKey(peer)) {
//            return startTime.get(peer);
//        } else {
//            System.out.println(" Warning: startTime tidak ditemukan untuk " + peer);
//            return 0;
//        }
//    }
//
//
//    @Override
//    public boolean newMessage(Message m) {
//        messageBuffer.add(m);
//        return true;
//    }
//
//    @Override
//    public boolean isFinalDest(Message m, DTNHost aHost) {
//        return m.getTo() == aHost;
//    }
//
//    @Override
//    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
//        return m.getTo() != thisHost;
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
//    private void processMessageBuffer(DTNHost thisHost, DTNHost peer) {
//        List<Message> toRemove = new ArrayList<>();
//        for (Message m : messageBuffer) {
//            if (shouldSendMessageToHost(m, peer, thisHost)) {
//                forwardMessage(m, peer, thisHost);
//                toRemove.add(m);
//            }
//        }
//        messageBuffer.removeAll(toRemove);
//    }
//
//    private void forwardMessage(Message message, DTNHost peer, DTNHost thisHost) {
//        MessageRouter peerRouter = peer.getRouter();
//        if (peerRouter instanceof DecisionEngineRouter) {
//            ((DecisionEngineRouter) peerRouter).receiveMessage(message, thisHost);
//        } else {
//            System.err.println("Error: Peer router is not a DecisionEngineRouter");
//        }
//    }
//
//    @Override
//    public RoutingDecisionEngine replicate() {
//        return new PeopleRankDurationEngine(this);
//    }
//
//    @Override
//    public boolean hasHigherRankThan(NodeRanking otherNode) {
//        if (otherNode == null) {
//            return true;
//        }
//        return this.getPeopleRank() > otherNode.getPeopleRank();
//    }
//
//    private static class PeopleRankInfo implements java.io.Serializable {
//
//        public double peopleRank;
//        public int neighborCount;
//
//        public PeopleRankInfo(double peopleRank, int neighborCount) {
//            this.peopleRank = peopleRank;
//            this.neighborCount = neighborCount;
//        }
//    }
//}