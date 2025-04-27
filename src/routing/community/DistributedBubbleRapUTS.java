package routing.community;

import core.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

import java.util.*;

public class DistributedBubbleRapUTS implements RoutingDecisionEngine, CommunityDetectionEngine, PopularityTracker {
    public static final String COMMUNITY_ALG_SETTING = "communityDetectAlg";
    public static final String CENTRALITY_ALG_SETTING = "centralityAlg";

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;

    protected CommunityDetection community;
    protected Centrality centrality;

    public DistributedBubbleRapUTS(Settings s) {
        if (s.contains(COMMUNITY_ALG_SETTING))
            this.community = (CommunityDetection)
                    s.createIntializedObject(s.getSetting(COMMUNITY_ALG_SETTING));
        else
            this.community = new SimpleCommunityDetection(s);

        if (s.contains(CENTRALITY_ALG_SETTING))
            this.centrality = (Centrality)
                    s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
        else
            this.centrality = new SWindowCentrality(s); // Or another default

        startTimestamps = new HashMap<DTNHost, Double>();
        connHistory = new HashMap<DTNHost, List<Duration>>();
    }

    public DistributedBubbleRapUTS(DistributedBubbleRapUTS proto) {
        this.community = proto.community.replicate();
        this.centrality = proto.centrality.replicate();
        startTimestamps = new HashMap<DTNHost, Double>();
        connHistory = new HashMap<DTNHost, List<Duration>>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        DTNHost myHost = thisHost;
        DistributedBubbleRapUTS de = getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());

        this.community.newConnection(myHost, peer, de.community);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        DistributedBubbleRapUTS de = getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());

        this.community.newConnection(myHost, peer, de.community);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        Double time = startTimestamps.get(peer);
        if (time == null) {
            return;
        }
        double etime = SimClock.getTime();

        // Find or create the connection history list
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else
            history = connHistory.get(peer);

        // add this connection to the list
        if (etime - time > 0)
            history.add(new Duration(time, etime));

        CommunityDetection peerCD = this.getOtherDecisionEngine(peer).community;

        // inform the community detection object that a connection was lost.
        // The object might need the whole connection history at this point.
        community.connectionLost(thisHost, peer, peerCD, history);

        startTimestamps.remove(peer);
    }

    @Override
    public boolean newMessage(Message m) {
        return true; // Always keep and attempt to forward a created message
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost; // Unicast Routing
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        return shouldSendMessageToHost(m, otherHost);
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        if (m.getTo() == otherHost) return true; // trivial to deliver to final dest

        /*
         * Here is where we decide when to forward along a message.
         *
         * DiBuBB works such that it first forwards to the most globally central
         * nodes in the network until it finds a node that has the message's
         * destination as part of it's local community. At this point, it uses
         * the local centrality metric to forward a message within the community.
         */
        DTNHost dest = m.getTo();
        DistributedBubbleRapUTS de = getOtherDecisionEngine(otherHost);

        // Which of us has the dest in our local communities, this host or the peer
        boolean peerInCommunity = de.commumesWithHost(dest);
        boolean meInCommunity = this.commumesWithHost(dest);

        if (peerInCommunity && !meInCommunity) // peer is in local commun. of dest
            return true;
        else if (!peerInCommunity && meInCommunity) // I'm in local commun. of dest
            return false;
        else if (peerInCommunity) // we're both in the local community of destination
        {
            // Forward to the one with the higher local centrality (in our community)
            if (de.getLocalCentrality() > this.getLocalCentrality())
                return true;
            else
                return false;
        }
        // Neither in local community, forward to more globally central node
        else if (de.getGlobalCentrality() > this.getGlobalCentrality())
            return true;

        return false;
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        // DiBuBB allows a node to remove a message once it's forwarded it into the
        // local community of the destination
        DistributedBubbleRapUTS de = this.getOtherDecisionEngine(otherHost);
        return de.commumesWithHost(m.getTo()) &&
                !this.commumesWithHost(m.getTo());
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        DistributedBubbleRapUTS de = this.getOtherDecisionEngine(hostReportingOld);
        return de.commumesWithHost(m.getTo()) &&
                !this.commumesWithHost(m.getTo());
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new DistributedBubbleRapUTS(this);
    }

    @Override
    public void update(DTNHost thisHost) {

    }

    protected boolean commumesWithHost(DTNHost h) {
        return community.isHostInCommunity(h);
    }

    protected double getLocalCentrality() {
        return this.centrality.getLocalCentrality(connHistory, community);
    }

    protected double getGlobalCentrality() {
        return this.centrality.getGlobalCentrality(connHistory);
    }

    private DistributedBubbleRapUTS getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " +
                " with other routers of same type";

        return (DistributedBubbleRapUTS) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    public Set<DTNHost> getLocalCommunity() {
        return this.community.getLocalCommunity();
    }

    @Override
    public double[] getGlobalPopularityHistory(DTNHost host, int interval) {
        if (centrality instanceof CWindowCentrality) {
            int simTime = SimClock.getIntTime();
            int maxInterval = simTime / interval + 1;
            return ((CWindowCentrality) centrality).getGlobalCentralityHistory(getConnHistory(), interval);
        } else {
            System.err.println("Error: Centrality algorithm is not CWindowCentrality");
            return new double[0]; // Return empty array or handle error appropriately
        }
    }

    public Map<DTNHost, List<Duration>> getConnHistory() {
        return connHistory;
    }

    public Centrality getCentrality() {
        return centrality;
    }
}