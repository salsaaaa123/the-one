/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

package routing.people;

import core.DTNHost;
import routing.community.Duration;

import java.util.List;
import java.util.Map;

public interface Centrality {
    double getCentrality(DTNHost host, Map<DTNHost, List<Duration>> connHistory);

    Centrality replicate();
}
