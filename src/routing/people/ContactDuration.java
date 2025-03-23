/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

package routing.people;

import core.DTNHost;

public interface ContactDuration {
    double getDuration(DTNHost host1, DTNHost host2);
}
