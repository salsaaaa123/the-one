/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

package routing.peoplerank;

import routing.community.Duration;

import java.util.ArrayList;
import java.util.List;

public class SocialInteraction {
    private final List<Duration> durations;
    private int frequency;

    public SocialInteraction() {
        this.durations = new ArrayList<Duration>();
        this.frequency = 0;
    }

    public void addInteraction(double start, double end) {
        durations.add(new Duration(start, end));
        frequency++;
    }

    public int getFrequency() {
        return frequency;
    }

    public double getTotalDuration() {
        double total = 0.0;
        for (int i = 0; i < durations.size(); i++) {
            Duration d = durations.get(i);
            total += d.getEnd() - d.getStart();
        }
        return total;
    }

    public List<Duration> getDurations() {
        return new ArrayList<Duration>(durations);
    }

}
