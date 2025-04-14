///*
// * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
// */
//
///*
// * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
// */
//
//package report.coba;
//
//import core.*;
//
//import java.util.*;
//
//import routing.DecisionEngineRouter;
//import routing.MessageRouter;
//import routing.RoutingDecisionEngine;
//import routing.peoplerank.NodeRanking;
//
//public class PeopleRankDecisionEngine implements RoutingDecisionEngine, NodeRanking {
//
//    public static final String PEOPLERANK_NS = "PeopleRankDecisionEngine";
//    public static final String DAMPING = "dampingFactor";
//    public static final String FREQUENCY_THRESHOLD = "frequencyThreshold";
//    public static final String DURATION_THRESHOLD = "durationThreshold";
//
//    private static final double DEFAULT_PEOPLE_RANK = 1.0;
//    protected double dampingFactor;
//    protected double peopleRank;
//    protected int frequencyThreshold;
//    protected double durationThreshold;
//
//    protected Map<DTNHost, Double> startTime;
//    protected Map<DTNHost, Tuple<Double, Integer>> socialInteractions;
//    protected Map<DTNHost, PeopleRankInfo> peerInfo;
//    protected List<Message> messageBuffer;
//    protected Set<DTNHost> socialGraph;
//
//    public PeopleRankDecisionEngine(Settings s) {
//        Settings prSettings = new Settings(PEOPLERANK_NS);
//
//        if (prSettings.contains(DAMPING)) {
//            dampingFactor = prSettings.getDouble(DAMPING);
//        } else {
//            dampingFactor = 0.5;
//        }
//
//        if (prSettings.contains(FREQUENCY_THRESHOLD)) {
//            frequencyThreshold = prSettings.getInt(FREQUENCY_THRESHOLD);
//        } else {
//            frequencyThreshold = 3;
//        }
//
//        if (prSettings.contains(DURATION_THRESHOLD)) {
//            durationThreshold = prSettings.getDouble(DURATION_THRESHOLD);
//        } else {
//            durationThreshold = 60.0;
//        }
//
//        startTime = new HashMap<>();
//        socialInteractions = new HashMap<>();
//        peerInfo = new HashMap<>();
//        messageBuffer = new ArrayList<>();
//        socialGraph = new HashSet<>();
//
//        this.peopleRank = DEFAULT_PEOPLE_RANK;
//
//        System.out.println("Frequency Threshold: " + frequencyThreshold);
//        System.out.println("Duration Threshold: " + durationThreshold);
//    }
//
//    public PeopleRankDecisionEngine(PeopleRankDecisionEngine proto) {
//        this.dampingFactor = proto.dampingFactor;
//        this.frequencyThreshold = proto.frequencyThreshold;
//        this.durationThreshold = proto.durationThreshold;
//        this.peopleRank = proto.peopleRank;
//
//        startTime = new HashMap<>();
//        socialInteractions = new HashMap<>();
//        peerInfo = new HashMap<>();
//        messageBuffer = new ArrayList<>();
//        socialGraph = new HashSet<>(proto.socialGraph);
//    }
//
//    @Override
//    public void connectionUp(DTNHost thisHost, DTNHost peer) {
//        if (!startTime.containsKey(peer)) {
//            startTime.put(peer,  Double.valueOf(SimClock.getTime()));
//        }
//    }
//
//
//    @Override
//    public void connectionDown(DTNHost thisHost, DTNHost peer) {
//        Double start = startTime.remove(peer);
//        double end = SimClock.getTime();
//        double duration = (start != null) ? end - start : 0.0;
//
//        Tuple<Double, Integer> interaction = socialInteractions.get(peer);
//        if (interaction == null) {
//            interaction = new Tuple<>(0.0, 0);
//        }
//
//        double updatedDuration = interaction.getFirst() + duration;
//        int updatedFrequency = interaction.getSecond() + 1;
//        socialInteractions.put(peer, new Tuple<>(updatedDuration, updatedFrequency));
//
//        updateSocialGraph(thisHost, peer);
//        processMessageBuffer(thisHost, peer);
//    }
//
//
//    private void updateSocialGraph(DTNHost thisHost, DTNHost peer) {
//        Tuple<Double, Integer> interaction = socialInteractions.get(peer);
//        if (interaction == null) return;
//
//        boolean meetsFrequency = interaction.getSecond() >= frequencyThreshold;
//        boolean meetsDuration = interaction.getFirst() >= durationThreshold;
//
//        if (meetsFrequency || meetsDuration) {
//            socialGraph.add(peer);
//            socialGraph.add(thisHost);
//        } else {
//            socialGraph.remove(peer);
//            socialGraph.remove(thisHost);
//        }
//    }
//
//    @Override
//    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
//        DTNHost myHost = con.getOtherNode(peer);
//
//        PeopleRankInfo myInfo = new PeopleRankInfo(getPeopleRank(), getNeighborCount());
//        send(myHost, peer, myInfo);
//
//        PeopleRankInfo receivedInfo = receive(peer);
//        peerInfo.put(peer, receivedInfo);
//
//        updateSocialGraph(myHost, peer);
//    }
//
//    private void send(DTNHost myHost, DTNHost peer, PeopleRankInfo information) {
//        PeopleRankDecisionEngine prde = getDecisionRouterFrom(peer);
//        prde.peerInfo.put(myHost, information);
//    }
//
//    private PeopleRankInfo receive(DTNHost peer) {
//        PeopleRankDecisionEngine prde = getDecisionRouterFrom(peer);
//        return new PeopleRankInfo(prde.getPeopleRank(), prde.getNeighborCount());
//    }
//
//    @Override
//    public void update(DTNHost thisHost) {
//        updatePeopleRank(thisHost);
//    }
//
//    public void updatePeopleRank(DTNHost myHost) {
//        double sigma = 0.0;
//
//        for (DTNHost neighbor : socialGraph) {
//            if (neighbor == myHost) continue;
//
//            PeopleRankInfo neighborInfo = peerInfo.get(neighbor);
//            if (neighborInfo != null) {
//                sigma += neighborInfo.peopleRank / ((double) neighborInfo.neighborCount);
//            }
//        }
//
//        this.peopleRank = (1.0 - dampingFactor) + dampingFactor * sigma;
//    }
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
//        if (!socialGraph.contains(otherHost) && !m.getTo().equals(otherHost)) {
//            return false;
//        }
//
//        double otherRank = getDecisionRouterFrom(otherHost).getPeopleRank();
//        return otherRank >= this.peopleRank || m.getTo().equals(otherHost);
//    }
//
//    private PeopleRankDecisionEngine getDecisionRouterFrom(DTNHost h) {
//        MessageRouter router = h.getRouter();
//        if (router instanceof DecisionEngineRouter) {
//            return (PeopleRankDecisionEngine) ((DecisionEngineRouter) router).getDecisionEngine();
//        }
//        throw new IllegalStateException("Router tidak valid atau tidak ditemukan!");
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
//
//        for (Message m : messageBuffer) {
//            if (shouldSendMessageToHost(m, peer, thisHost)) {
//                forwardMessage(m, peer, thisHost);
//                toRemove.add(m);
//            }
//        }
//
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
//        return new PeopleRankDecisionEngine(this);
//    }
//
//    @Override
//    public boolean hasHigherRankThan(NodeRanking otherNode) {
//        return otherNode == null || this.peopleRank > otherNode.getPeopleRank();
//    }
//
//    private static class PeopleRankInfo implements java.io.Serializable {
//        public double peopleRank;
//        public int neighborCount;
//
//        public PeopleRankInfo(double peopleRank, int neighborCount) {
//            this.peopleRank = peopleRank;
//            this.neighborCount = neighborCount;
//        }
//    }
//}
