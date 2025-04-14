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
//import routing.DecisionEngineRouter;
//import routing.MessageRouter;
//import routing.RoutingDecisionEngine;
//import routing.peoplerank.NodeRanking;
//import routing.peoplerank.SocialInteraction;
//
//import java.util.*;
//
//public class PeopleRankFrequencyEngineTuple implements RoutingDecisionEngine, NodeRanking {
//    public static final String PEOPLERANK_NS = "PeopleRankFrequencyEngineTuple";
//    public static final String DAMPING = "dampingFactor";
//    public static final String FREQUENCY_THRESHOLD = "frequencyThreshold";
//    private static final double DEFAULT_PEOPLE_RANK = 0.0;
//
//    private double previousPeopleRank;
//    protected double dampingFactor;
//    protected double peopleRank;
//    protected int frequencyThreshold;
//
//    protected Map<DTNHost, Double> startTime;
//    protected Map<DTNHost, SocialInteraction> socialInteractions;
//    protected Map<DTNHost, Tuple<Double, Integer>> peerInfo;
//    protected List<Message> messageBuffer;
//    protected Set<DTNHost> socialGraph;
//
//    public PeopleRankFrequencyEngineTuple(Settings s) {
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
//        startTime = new HashMap<DTNHost, Double>();
//        socialInteractions = new HashMap<DTNHost, SocialInteraction>();
//        peerInfo = new HashMap<DTNHost, Tuple<Double, Integer>>();
//        messageBuffer = new ArrayList<Message>();
//        socialGraph = new HashSet<DTNHost>();
//
//        peopleRank = DEFAULT_PEOPLE_RANK;
//        previousPeopleRank = DEFAULT_PEOPLE_RANK;
//
//        System.out.println("Frequency Threshold: " + frequencyThreshold);
//    }
//
//    public PeopleRankFrequencyEngineTuple(PeopleRankFrequencyEngineTuple proto) {
//        dampingFactor = proto.dampingFactor;
//        frequencyThreshold = proto.frequencyThreshold;
//        peopleRank = proto.peopleRank;
//        previousPeopleRank = proto.previousPeopleRank;
//
//        startTime = new HashMap<DTNHost, Double>();
//        socialInteractions = new HashMap<DTNHost, SocialInteraction>(proto.socialInteractions);
//        peerInfo = new HashMap<DTNHost, Tuple<Double, Integer>>(proto.peerInfo);
//        messageBuffer = new ArrayList<Message>();
//        socialGraph = new HashSet<DTNHost>(proto.socialGraph);
//    }
//
//    @Override
//    public void connectionUp(DTNHost thisHost, DTNHost peer) {
//        double start = SimClock.getTime();
//
//        SocialInteraction interaction = socialInteractions.get(peer);
//        if (interaction == null) {
//            interaction = new SocialInteraction();
//            socialInteractions.put(peer, interaction);
//        }
//
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
//    private void updateSocialGraph(DTNHost thisHost, DTNHost peer) {
//        SocialInteraction interaction = socialInteractions.get(peer);
//        int frequency = (interaction != null) ? interaction.getFrequency() : 0;
//
//        if (frequency >= frequencyThreshold) {
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
//        double currentPeopleRank = getPeopleRank();
//
//        double diff = currentPeopleRank - previousPeopleRank;
//        if (diff < 0) {
//            diff = -diff;
//        }
//
//        if (diff > 0.01) {
//            Tuple<Double, Integer> data = new Tuple<Double, Integer>(currentPeopleRank, getNeighborCount());
//            send(myHost, peer, data);
//            previousPeopleRank = currentPeopleRank;
//        }
//
//        Tuple<Double, Integer> peerData = receive(peer);
//        this.peerInfo.put(peer, peerData);
//
//        updateSocialGraph(myHost, peer);
//    }
//    private void send(DTNHost myHost, DTNHost peer, Tuple<Double, Integer> information) {
//        PeopleRankFrequencyEngineTuple prde = getDecisionRouterFrom(peer);
//        prde.peerInfo.put(myHost, information);
//    }
//
//    private Tuple<Double, Integer> receive(DTNHost peer) {
//        PeopleRankFrequencyEngineTuple prde = getDecisionRouterFrom(peer);
//        double peerRank = prde.getPeopleRank();
//        int peerFriends = prde.getNeighborCount();
//        return new Tuple<Double, Integer>(peerRank, peerFriends);
//    }
//
//    @Override
//    public boolean newMessage(Message m) {
//        messageBuffer.add(m);
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
//        if (m.getTo().equals(otherHost)) {
//            return true;
//        }
//
//        if (!socialGraph.contains(otherHost)) {
//            return false;
//        }
//
//        PeopleRankFrequencyEngineTuple prde = getDecisionRouterFrom(otherHost);
//        double otherHostRank = prde.getPeopleRank();
//
//        return otherHostRank >= peopleRank;
//    }
//    public PeopleRankFrequencyEngineTuple getDecisionRouterFrom(DTNHost h) {
//        MessageRouter otherRouter = h.getRouter();
//        if (otherRouter instanceof DecisionEngineRouter) {
//            RoutingDecisionEngine engine = ((DecisionEngineRouter) otherRouter).getDecisionEngine();
//            if (engine instanceof PeopleRankFrequencyEngineTuple) {
//                return (PeopleRankFrequencyEngineTuple) engine;
//            } else {
//                throw new IllegalStateException("DecisionEngine is not an instance of PeopleRankFrequencyEngineTuple!");
//            }
//        } else {
//            throw new IllegalStateException("Router is not a DecisionEngineRouter!");
//        }
//    }
//    private double check(DTNHost peer) {
//        if (startTime.containsKey(peer)) {
//            return startTime.get(peer);
//        }
//        return 0;
//    }
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
//    public void updatePeopleRank(DTNHost myHost) {
//        double sigma = 0.0;
//
//        Iterator<DTNHost> it = socialGraph.iterator();
//        while (it.hasNext()) {
//            DTNHost neighbor = it.next();
//
//            if (neighbor == myHost) {
//                continue;
//            }
//
//            if (peerInfo.containsKey(neighbor)) {
//                Tuple<Double, Integer> neighborInfo = peerInfo.get(neighbor);
//                double rank = neighborInfo.getFirst();
//                int neighbors = neighborInfo.getSecond();
//                sigma += rank / (double) neighbors;
//            }
//        }
//
//        peopleRank = (1 - dampingFactor) + dampingFactor * sigma;
//    }
//
//
//    public int getNeighborCount() {
//        return socialGraph.size();
//    }
//
//    @Override
//    public RoutingDecisionEngine replicate() {
//        return new PeopleRankFrequencyEngineTuple(this);
//    }
//
//    private void processMessageBuffer(DTNHost thisHost, DTNHost peer) {
//        List<Message> toRemove = new ArrayList<Message>();
//        for (int i = 0; i < messageBuffer.size(); i++) {
//            Message m = messageBuffer.get(i);
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
//    public double getPeopleRank() {
//        return this.peopleRank;
//    }
//
//    public boolean hasHigherRankThan(NodeRanking otherNode) {
//        if (otherNode == null) {
//            return true;
//        }
//        return getPeopleRank() > otherNode.getPeopleRank();
//    }
//}
