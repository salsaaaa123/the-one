/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

package routing.social;

import core.DTNHost;
import routing.community.Duration;

import java.util.List;
import java.util.Map;

public class DurationSocialMetric implements SocialMetric {
    private final Map<Pair<DTNHost, DTNHost>, List<Duration>> connectionHistory;
    private final double threshold;

    public DurationSocialMetric(Map<Pair<DTNHost, DTNHost>, List<Duration>> connectionHistory, double threshold) {
        this.connectionHistory = connectionHistory;
        this.threshold = threshold;
    }

    @Override
    public boolean isFriend(DTNHost node1, DTNHost node2) {
        List<Duration> history = getConnectionHistory(node1, node2);
        if (history == null) {
            return false; // Belum pernah kontak
        }

        double totalDuration = 0;
        for (Duration d : history) {
            totalDuration += (d.getEnd() - d.getStart());
        }
        return totalDuration >= threshold;
    }

    private List<Duration> getConnectionHistory(DTNHost node1, DTNHost node2) {
        Pair<DTNHost, DTNHost> pair = createPair(node1, node2);
        return connectionHistory.get(pair);
    }

    private Pair<DTNHost, DTNHost> createPair(DTNHost a, DTNHost b) {
        if (a.getAddress() < b.getAddress()) {
            return new Pair<>(a, b);
        } else {
            return new Pair<>(b, a);
        }
    }
}
