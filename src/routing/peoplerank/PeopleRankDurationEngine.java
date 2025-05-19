/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

package routing.peoplerank;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

import java.util.*;

public class PeopleRankDurationEngine implements RoutingDecisionEngine, NodeRanking {

    public static final String PEOPLERANK_NS = "PeopleRankDurationEngine";
    public static final String DAMPING = "dampingFactor";
    public static final String DURATION_THRESHOLD = "durationThreshold";
    public static final String RANK_NORMALIZATION = "rankNormalization";

    private static final double DEFAULT_PEOPLE_RANK = 0.0;

    private double dampingFactor;
    private double peopleRank;
    private double durationThreshold;
    private boolean rankNormalization;

    private Map<DTNHost, Double> startTime;
    private Map<DTNHost, SocialInteraction> socialInteractions;
    private Map<DTNHost, PeopleRankInfo> peerInfo;
    // private List<Message> messageBuffer; // Hapus message buffer

    private Map<DTNHost, Set<DTNHost>> socialGraph = new HashMap<>();
    private boolean isSocialGraphShared = true;

    public PeopleRankDurationEngine(Settings s) {
        Settings prSettings = new Settings(PEOPLERANK_NS);

        if (prSettings.contains(DAMPING)) {
            dampingFactor = prSettings.getDouble(DAMPING);
        } else {
            dampingFactor = 0.5;
        }

        if (prSettings.contains(DURATION_THRESHOLD)) {
            durationThreshold = prSettings.getDouble(DURATION_THRESHOLD);
        } else {
            durationThreshold = 30.0;
        }

        if (prSettings.contains(RANK_NORMALIZATION)) {
            rankNormalization = prSettings.getBoolean(RANK_NORMALIZATION);
        } else {
            rankNormalization = false;
        }

        startTime = new HashMap<>();
        socialInteractions = new HashMap<>();
        peerInfo = new HashMap<>();
        // messageBuffer = new ArrayList<>(); // Hapus inisialisasi message buffer

        peopleRank = DEFAULT_PEOPLE_RANK;

        System.out.println("Duration Threshold: " + durationThreshold);
        System.out.println("Rank Normalization: " + rankNormalization);
    }

    public PeopleRankDurationEngine(PeopleRankDurationEngine proto) {
        dampingFactor = proto.dampingFactor;
        durationThreshold = proto.durationThreshold;
        rankNormalization = proto.rankNormalization;
        peopleRank = proto.peopleRank;

        startTime = new HashMap<>();
        socialInteractions = new HashMap<>(proto.socialInteractions);
        peerInfo = new HashMap<>(proto.peerInfo);
        // messageBuffer = new ArrayList<>(); // Hapus inisialisasi message buffer

        this.socialGraph = proto.socialGraph;
        this.isSocialGraphShared = false;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        double start = SimClock.getTime();
        if (!socialInteractions.containsKey(peer)) {
            socialInteractions.put(peer, new SocialInteraction());
        }
        startTime.put(peer, start);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        double end = SimClock.getTime();
        double start = check(peer);

        if (start > 0) {
            SocialInteraction interaction = socialInteractions.get(peer);
            if (interaction != null) {
                interaction.addInteraction(start, end);
            }
        }

        startTime.remove(peer);
        updateSocialGraph(thisHost, peer);
        // processMessageBuffer(thisHost, peer); // Hapus pemanggilan
        // processMessageBuffer
    }

    @Override
    public void update(DTNHost thisHost) {
        updateSocialGraphPeriodic(thisHost);
        updatePeopleRank(thisHost);
    }

    private void updateSocialGraphPeriodic(DTNHost thisHost) {
        Set<DTNHost> keys = new HashSet<>(socialInteractions.keySet());
        Iterator<DTNHost> it = keys.iterator();
        while (it.hasNext()) {
            DTNHost peer = it.next();
            if (!peer.equals(thisHost)) {
                updateSocialGraph(thisHost, peer);
            }
        }
    }

    private void updateSocialGraph(DTNHost thisHost, DTNHost peer) {
        ensureSocialGraphIsMutable();
        SocialInteraction interaction = socialInteractions.get(peer);
        double totalDuration = 0.0;
        if (interaction != null) {
            totalDuration = interaction.getTotalDuration();
        }

        if (totalDuration >= durationThreshold) {
            addNeighbor(thisHost, peer);
            addNeighbor(peer, thisHost);
        } else {
            removeNeighbor(thisHost, peer);
            removeNeighbor(peer, thisHost);
        }
    }

    private void addNeighbor(DTNHost node, DTNHost neighbor) {
        ensureSocialGraphIsMutable();
        if (!socialGraph.containsKey(node)) {
            socialGraph.put(node, new HashSet<>());
        }
        socialGraph.get(node).add(neighbor);
    }

    private void removeNeighbor(DTNHost node, DTNHost neighbor) {
        ensureSocialGraphIsMutable();
        if (socialGraph.containsKey(node)) {
            socialGraph.get(node).remove(neighbor);
        }
    }

    private void ensureSocialGraphIsMutable() {
        if (isSocialGraphShared) {
            Map<DTNHost, Set<DTNHost>> newGraph = new HashMap<>();
            Iterator<Map.Entry<DTNHost, Set<DTNHost>>> it = socialGraph.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<DTNHost, Set<DTNHost>> entry = it.next();
                newGraph.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
            socialGraph = newGraph;
            isSocialGraphShared = false;
        }
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        double currentPeopleRank = getPeopleRank();

        // Kirimkan data peerrank
        PeopleRankInfo data = new PeopleRankInfo(currentPeopleRank, getNeighborCount(myHost));
        send(myHost, peer, data);

        // Menerima data peer
        PeopleRankInfo peerData = receive(peer);
        peerInfo.put(peer, peerData);

        updateSocialGraph(myHost, peer);
        // processMessageBuffer(myHost, peer); // Hapus pemanggilan processMessageBuffer
    }

    private void send(DTNHost myHost, DTNHost peer, PeopleRankInfo info) {
        PeopleRankDurationEngine prde = getDecisionRouterFrom(peer);
        prde.peerInfo.put(myHost, info);
    }

    private PeopleRankInfo receive(DTNHost peer) {
        PeopleRankDurationEngine prde = getDecisionRouterFrom(peer);
        return new PeopleRankInfo(prde.getPeopleRank(), prde.getNeighborCount(peer));
    }

    public void updatePeopleRank(DTNHost myHost) {
        double sigma = 0.0;
        Set<DTNHost> neighbors = socialGraph.get(myHost);
        // Pastikan node memiliki tetangga
        if (neighbors != null) {
            int neighborCount = neighbors.size();
            // Pastikan bahwa terdapat tetangga
            if (neighborCount > 0) {
                Iterator<DTNHost> it = neighbors.iterator();
                while (it.hasNext()) {
                    DTNHost neighbor = it.next();
                    if (peerInfo.containsKey(neighbor)) {
                        PeopleRankInfo info = peerInfo.get(neighbor);
                        sigma += info.peopleRank / (double) neighborCount;
                    }
                }
            }
        }

        peopleRank = (1 - dampingFactor) + dampingFactor * sigma;

        if (rankNormalization) {
            normalizePeopleRank();
        }
    }

    private void normalizePeopleRank() {
        double minRank = Double.MAX_VALUE;
        double maxRank = Double.MIN_VALUE;

        Iterator<DTNHost> it = socialGraph.keySet().iterator();
        while (it.hasNext()) {
            DTNHost node = it.next();
            PeopleRankDurationEngine prde = getDecisionRouterFrom(node);
            double rank = prde.getPeopleRank();
            if (rank < minRank)
                minRank = rank;
            if (rank > maxRank)
                maxRank = rank;
        }

        if (maxRank - minRank > 0) {
            Iterator<DTNHost> it2 = socialGraph.keySet().iterator();
            while (it2.hasNext()) {
                DTNHost node = it2.next();
                PeopleRankDurationEngine prde = getDecisionRouterFrom(node);
                double normalizedRank = (prde.getPeopleRank() - minRank) / (maxRank - minRank);
                prde.setPeopleRank(normalizedRank);
            }
        } else {
            // Tangani kasus di mana semua rank sama
            System.out.println("Semua rank sama, tidak ada normalisasi yang dilakukan");
            // Setel semua rank ke nilai default (misalnya, 0.0)
            Iterator<DTNHost> it2 = socialGraph.keySet().iterator();
            while (it2.hasNext()) {
                DTNHost node = it2.next();
                PeopleRankDurationEngine prde = getDecisionRouterFrom(node);
                prde.setPeopleRank(0.0);
            }
        }
    }

    private void setPeopleRank(double rank) {
        this.peopleRank = rank;
    }

    private List<DTNHost> getNeighbors(DTNHost node) {
        Set<DTNHost> neighbors = socialGraph.get(node);
        if (neighbors != null) {
            return new ArrayList<>(neighbors);
        } else {
            return new ArrayList<>();
        }
    }

    private double check(DTNHost peer) {
        if (startTime.containsKey(peer)) {
            return startTime.get(peer);
        } else {
            return 0.0;
        }
    }

    public int getNeighborCount(DTNHost host) {
        List<DTNHost> neighbors = getNeighbors(host);
        return neighbors == null ? 0 : neighbors.size();
    }

    @Override
    public double getPeopleRank() {
        return this.peopleRank;
    }

    // @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (!getNeighbors(thisHost).contains(otherHost))
            return false;
        if (m.getTo().equals(otherHost))
            return true;

        PeopleRankDurationEngine prde = getDecisionRouterFrom(otherHost);
        return prde.getPeopleRank() >= peopleRank;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        return false;
    }

    private PeopleRankDurationEngine getDecisionRouterFrom(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        if (otherRouter instanceof DecisionEngineRouter) {
            RoutingDecisionEngine engine = ((DecisionEngineRouter) otherRouter).getDecisionEngine();
            if (engine instanceof PeopleRankDurationEngine) {
                return (PeopleRankDurationEngine) engine;
            } else {
                throw new IllegalStateException("DecisionEngine is not PeopleRankDurationEngine!");
            }
        } else {
            throw new IllegalStateException("Router is not a DecisionEngineRouter!");
        }
    }

    @Override
    public boolean newMessage(Message m) {
        // messageBuffer.add(m); // Hapus penambahan pesan ke buffer
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    // private void processMessageBuffer(DTNHost thisHost, DTNHost peer) { // Hapus
    // method processMessageBuffer
    // List<Message> toRemove = new ArrayList<>();
    // Iterator<Message> it = messageBuffer.iterator();
    // while (it.hasNext()) {
    // Message m = it.next();
    // if (shouldSendMessageToHost(m, peer, thisHost)) {
    // forwardMessage(m, peer, thisHost);
    // toRemove.add(m);
    // }
    // }
    // messageBuffer.removeAll(toRemove);
    // }

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRankDurationEngine(this);
    }

    @Override
    public boolean hasHigherRankThan(NodeRanking otherNode) {
        return otherNode == null || getPeopleRank() > otherNode.getPeopleRank();
    }

    @Override
    public Map<DTNHost, Double> getAllRank() {
        Map<DTNHost, Double> ranks = new HashMap<>();
        Iterator<DTNHost> it = socialGraph.keySet().iterator();

        while (it.hasNext()) {
            DTNHost node = it.next();
            PeopleRankDurationEngine prde = getDecisionRouterFrom(node);
            double rank = prde.getPeopleRank();
            ranks.put(node, rank);
        }

        return ranks;
    }
}
