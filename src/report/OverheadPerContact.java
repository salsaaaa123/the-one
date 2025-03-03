package report;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.HashMap;
import java.util.Map;

public class OverheadPerContact extends Report implements MessageListener, ConnectionListener {

    private int lastRecord;
    private int interval;
    private int nrofContact; // Total Contacts
    private int nrofRelayed; // Total Messages Relayed
    private int nrofDelivered; // Total Messages Delivered
    private Map<Integer, Double> nrofOverhead;
    private static final String NROF_CONTACT_INTERVAL = "perTotalContact";
    private static final int DEFAULT_CONTACT_COUNT = 600;

    // Constructor:
    public OverheadPerContact() {
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
        this.lastRecord = 0;
        this.nrofContact = 0;
        this.nrofDelivered = 0;
        this.nrofRelayed = 0;
        this.nrofOverhead = new HashMap<>();

    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        nrofContact++;
        if (nrofContact - lastRecord >= interval) {
            lastRecord = nrofContact;
            double overHead = Double.NaN; // overhead ratio
            if (this.nrofDelivered > 0) {
                overHead = (1.0 * (this.nrofRelayed - this.nrofDelivered)) / this.nrofDelivered;
            }
            nrofOverhead.put(lastRecord, overHead);
        }

    }

    @Override
    public void done() {
        String output = "Contact\tOverhead\n";
        for (Map.Entry<Integer, Double> entry : nrofOverhead.entrySet()) {
            Integer key = entry.getKey();
            Double value = entry.getValue();
            output += key + "\t " + value + "\n";
        }
        write(output);
        super.done();
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {

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
        this.nrofRelayed++;
        if (firstDelivery) {
            this.nrofDelivered++;
        }

    }
}
