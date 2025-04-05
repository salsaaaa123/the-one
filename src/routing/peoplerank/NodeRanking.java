/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

package routing.peoplerank;

import core.DTNHost;

import java.util.Map;

public interface NodeRanking {
    /**
     * Mengembalikan nilai PeopleRank dari node saat ini.
     * @return nilai PeopleRank
     */
    double getPeopleRank();

    /**
     * Membandingkan peringkat antara dua node.
     * @param otherNode node lain yang akan dibandingkan
     * @return true jika node saat ini memiliki peringkat lebih tinggi dari otherNode
     */
    boolean hasHigherRankThan(NodeRanking otherNode);
    Map<DTNHost, Double> getAllRank();
}
