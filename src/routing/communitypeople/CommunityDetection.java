/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */


package routing.communitypeople;

import core.DTNHost;
import routing.community.Duration;

import java.util.List;
import java.util.Map;

public interface CommunityDetection {
    boolean isInCommunity(DTNHost host, DTNHost destination,
                          Map<DTNHost, List<Duration>> connHistory);

    CommunityDetection replicate();
}
