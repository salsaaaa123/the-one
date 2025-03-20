/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

package routing.social;

import core.DTNHost;

public interface SocialMetric {
    boolean isFriend(DTNHost node1,DTNHost node2);
}
