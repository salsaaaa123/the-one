/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

package routing.people;

import core.DTNHost;

import java.util.Map;

public interface RankingNodeValue {
    /**
     * Mendapatkan peringkat PeopleRank dari semua node.
     *
     * @return Peta yang berisi peringkat PeopleRank untuk semua node.
     */
    Map<DTNHost, Double> getAllPeopleRankings();

    /**
     * Mendapatkan jumlah teman dari sebuah node.
     *
     * @param host Node yang ingin diketahui jumlah temannya.
     * @return Jumlah teman dari node tersebut.
     */
    int getFriendCountForHost(DTNHost host);
}
