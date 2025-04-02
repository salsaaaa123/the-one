package routing.people;

import core.DTNHost;
import routing.community.Duration;

import java.util.List;
import java.util.Map;

public interface Centrality {
    /**
     * Returns the computed global centrality based on the connection history
     * passed as an argument.
     *
     * @param connHistory Contact History on which to compute centrality
     * @return Value corresponding to the global centrality
     */
    double getGlobalCentrality(Map<DTNHost, List<Duration>> connHistory);

    /**
     * Returns the computed local centrality based on the connection history and
     * community detection objects passed as parameters.
     *
     * @param connHistory Contact history on which to compute centrality
     * @param cd          CommunityDetection object that knows the local community
     * @return Value corresponding to the local centrality
     */
    double getLocalCentrality(Map<DTNHost, List<Duration>> connHistory,
                              CommunityDetection cd);

    /**
     * Returns the PeopleRank values for all nodes
     *
     * @param connHistory Contact history on which to compute centrality
     * @return a Map of PeopleRank values
     */
    Map<DTNHost, Double> getAllPeopleRankings(Map<DTNHost, List<Duration>> connHistory);

    /**
     * Duplicates a Centrality object. This is a convention of the ONE to easily
     * create multiple instances of objects based on defined settings.
     *
     * @return A duplicate Centrality instance
     */
    Centrality replicate();
}