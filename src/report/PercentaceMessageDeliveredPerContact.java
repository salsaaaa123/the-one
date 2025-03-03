package report;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.HashMap;
import java.util.Map;

public class PercentaceMessageDeliveredPerContact extends Report implements MessageListener, ConnectionListener {
    public static final String NROF_CONTACT_INTERVAL = "perTotalContact";
    public static final int DEFAULT_CONTACT_COUNT = 500;
    private int lastRecord;
    private int interval;
    private int nrofContact;
    private int nrofCreated;
    private int nrofDelivered;
    private Map<Integer, Double> nrofDeliver;

    // Constructor:
    public PercentaceMessageDeliveredPerContact() {
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
        this.nrofDelivered = 0;
        this.lastRecord = 0;
        this.nrofContact = 0;
        this.nrofDeliver = new HashMap<>();
    }


    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        nrofContact++;
        if (nrofContact - lastRecord >= interval) {
            lastRecord = nrofContact;
            double deliveryPercentage = ((1.0 * this.nrofDelivered) / this.nrofCreated) * 100;
            nrofDeliver.put(lastRecord, deliveryPercentage);
        }
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {

    }

    @Override
    public void newMessage(Message m) {
        this.nrofCreated++;
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
            this.nrofDelivered++;
        }
    }
    @Override
    public void done() {
        String output = "Contact\tNrofDelivered\n";
        for (Map.Entry<Integer, Double> entry : nrofDeliver.entrySet()) {
            Integer key = entry.getKey();
            Double value = entry.getValue();
            output += key + "\t" + value + "\n";
        }
        write(output);
        super.done();
    }
}
