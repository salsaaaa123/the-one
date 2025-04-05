/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

package routing.peoplerank;

public class PeopleRankInfo {
    public double peopleRank;
    public int neighborCount;

    public PeopleRankInfo(double peopleRank, int neighborCount) {
        this.peopleRank = peopleRank;
        this.neighborCount = neighborCount;
    }

    public int getNeighborCount() {
        return neighborCount;
    }

    public double getPeopleRank() {
        return peopleRank;
    }
}
