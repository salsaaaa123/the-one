package routing.community;

import core.DTNHost;

public interface PopularityTracker {
    double[] getGlobalPopularityHistory(DTNHost host, int interval);
}