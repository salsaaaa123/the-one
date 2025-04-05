/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

package routing.rank;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.peoplerank.NodeRanking;
import routing.peoplerank.PeopleRankInfo;
import routing.peoplerank.SocialInteraction;

import java.util.*;

public class PeopleRankDurationEngineUpdate implements RoutingDecisionEngine, NodeRanking {
    public static final String PEOPLERANK_NS = "PeopleRankDurationEngineUpdate";
    public static final String DAMPING = "dampingFactor";
    public static final String DURATION_THRESHOLD = "durationThreshold";

    private static final double DEFAULT_PEOPLE_RANK = 0.0;

    private double dampingFactor;
    private double peopleRank;
    private double durationThreshold;

    private Map<DTNHost, Double> startTime;
    private Map<DTNHost, SocialInteraction> socialInteractions;
    private Map<DTNHost, PeopleRankInfo> peerInfo;

    private Map<DTNHost, Set<DTNHost>> socialGraph = new HashMap<>();

    public PeopleRankDurationEngineUpdate(Settings s) {
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


        startTime = new HashMap<>();
        socialInteractions = new HashMap<>();
        peerInfo = new HashMap<>();

        peopleRank = DEFAULT_PEOPLE_RANK;

        System.out.println("Duration Threshold: " + durationThreshold);
    }

    public PeopleRankDurationEngineUpdate(PeopleRankDurationEngineUpdate proto) {
        dampingFactor = proto.dampingFactor;
        durationThreshold = proto.durationThreshold;
        peopleRank = proto.peopleRank;

        startTime = new HashMap<>();
        socialInteractions = new HashMap<>(proto.socialInteractions);
        peerInfo = new HashMap<>(proto.peerInfo);

        this.socialGraph = new HashMap<>(proto.socialGraph); // Salin socialGraph
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
        if (!socialGraph.containsKey(node)) {
            socialGraph.put(node, new HashSet<>());
        }
        socialGraph.get(node).add(neighbor);
    }

    private void removeNeighbor(DTNHost node, DTNHost neighbor) {
        if (socialGraph.containsKey(node)) {
            socialGraph.get(node).remove(neighbor);
        }
    }


    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        double currentPeopleRank = getPeopleRank();

        //Kirimkan data peerrank
        PeopleRankInfo data = new PeopleRankInfo(currentPeopleRank, getNeighborCount(myHost));
        send(myHost, peer, data);

        //Menerima data peer
        PeopleRankInfo peerData = receive(peer);
        peerInfo.put(peer, peerData);

        updateSocialGraph(myHost, peer);
    }

    private void send(DTNHost myHost, DTNHost peer, PeopleRankInfo info) {
        PeopleRankDurationEngineUpdate prde = getDecisionRouterFrom(peer);
        prde.peerInfo.put(myHost, info);
    }

    private PeopleRankInfo receive(DTNHost peer) {
        PeopleRankDurationEngineUpdate prde = getDecisionRouterFrom(peer);
        return new PeopleRankInfo(prde.getPeopleRank(), prde.getNeighborCount(peer));
    }

    public void updatePeopleRank(DTNHost myHost) {
        double sigma = 0.0;
        Set<DTNHost> neighbors = socialGraph.get(myHost);
        //Pastikan node memiliki tetangga
        if(neighbors != null){
            int neighborCount = neighbors.size();
            //Pastikan bahwa terdapat tetangga
            if(neighborCount >0){
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
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (!getNeighbors(thisHost).contains(otherHost)) return false;
        if (m.getTo().equals(otherHost)) return true;

        PeopleRankDurationEngineUpdate prde = getDecisionRouterFrom(otherHost);
        return prde.getPeopleRank() >= peopleRank;
    }

    private PeopleRankDurationEngineUpdate getDecisionRouterFrom(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        if (otherRouter instanceof DecisionEngineRouter) {
            RoutingDecisionEngine engine = ((DecisionEngineRouter) otherRouter).getDecisionEngine();
            if (engine instanceof PeopleRankDurationEngineUpdate) {
                return (PeopleRankDurationEngineUpdate) engine;
            } else {
                throw new IllegalStateException("DecisionEngine is not PeopleRankDurationEngineUpdate!");
            }
        } else {
            throw new IllegalStateException("Router is not a DecisionEngineRouter!");
        }
    }
    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost targetHost) {
        return m.getTo() == targetHost;
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

    @Override
    public RoutingDecisionEngine replicate() {
        return new PeopleRankDurationEngineUpdate(this);
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
            PeopleRankDurationEngineUpdate prde = getDecisionRouterFrom(node);
            double rank = prde.getPeopleRank();
            ranks.put(node, rank);
        }

        return ranks;
    }
}
