package report;

import core.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TotalForwardPerContact extends Report implements MessageListener, UpdateListener {

    private double lastRecord;
    private int interval;
    private Map<DTNHost, Integer> nrofForwards;
    private Map<Double, Integer> nrofForwardRecords;
    private static final String NROF_CONTACT_INTERVAL = "perTotalContact";
    private static final int DEFAULT_CONTACT_COUNT = 600;

    public TotalForwardPerContact() {
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
        nrofForwards = new HashMap<>();
        this.lastRecord = 0;
        this.interval = 0;
        nrofForwardRecords = new HashMap<>();
    }


    @Override
    public void done() {
        String output = "Contacts\tTotalForwards\n";
        for (Map.Entry<Double, Integer> entry : nrofForwardRecords.entrySet()) {
            output += entry.getKey() + "\t" + entry.getValue() + "\n";
        }
        write(output);
        super.done();
    }


    @Override
    public void updated(List<DTNHost> hosts) {
        if (SimClock.getTime() - lastRecord >= interval) {
            int totalCount = 0;
            for (Map.Entry<DTNHost, Integer> entry : nrofForwards.entrySet()) {
                Integer value = entry.getValue();
                totalCount += value;
            }
            nrofForwardRecords.put(lastRecord, totalCount);
            lastRecord = SimClock.getTime();
        }
    }

    @Override
    public void newMessage(Message m) {

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
            if (nrofForwards.containsKey(from)) {
                nrofForwards.put(from, nrofForwards.get(from) + 1);
            } else {
                nrofForwards.put(from, 1);
            }
        }
    }
}
