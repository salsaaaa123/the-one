package report;

import core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LatencyPerContact extends Report implements MessageListener, ConnectionListener {

    private int nrofContacts;
    private int lastRecord;
    private int interval;
    private Map<Integer, String> nrofLatency;
    private Map<String, Double> creationTimes;
    private List<Double> latencies;
    public static final String NROF_CONTACT_INTERVAL = "perTotalContact";
    public static final int DEFAULT_CONTACT_COUNT = 600;

    public LatencyPerContact() {
        init();
        if (getSettings().contains(NROF_CONTACT_INTERVAL)) {
            interval = getSettings().getInt(NROF_CONTACT_INTERVAL);
        } else {
            interval = DEFAULT_CONTACT_COUNT;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.nrofContacts = 0;
        this.lastRecord = 0;
        this.interval = 0;
        this.creationTimes = new HashMap<String, Double>();
        this.nrofLatency = new HashMap<>();
        this.latencies = new ArrayList<Double>();

    }

    @Override
    public void newMessage(Message m) {
        this.creationTimes.put(m.getId(), getSimTime());

    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {

    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {

    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {

    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        if (firstDelivery) {
            double latenciesValue = getSimTime() - this.creationTimes.get(m.getId());
            this.latencies.add(latenciesValue);
        }

    }


    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        nrofContacts++;
        if (nrofContacts - lastRecord >= interval) {
            lastRecord = nrofContacts;
//            String avgLatency = getAverage(latencies);
            // double avgLatency = Double.parseDouble(getAverage(latencies)); // Jika Map<Integer, String> nrofLatency di parsing ke Double
            nrofLatency.put(lastRecord, getAverage(latencies));
        }

    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {

    }

    @Override
    public void done() {
        String statsText = "Contact\tLatencies\n";
        for (Map.Entry<Integer, String> entry : nrofLatency.entrySet()) {
            Integer key = entry.getKey();
            String value = entry.getValue();
            statsText += key + "\t" + value + "\n";
        }
        write(statsText);
        super.done();
    }

    ;
}

