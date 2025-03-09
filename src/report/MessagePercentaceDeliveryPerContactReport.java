package report;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.HashMap;
import java.util.Map;

public class MessagePercentaceDeliveryPerContactReport extends Report implements MessageListener, ConnectionListener {
    public static final String NROF_CONTACT_INTERVAL = "perTotalContact";
    public static final int DEFAULT_CONTACT_COUNT = 100; // Reduced default interval
    private int lastRecord;
    private int interval;
    private int nrofContact;
    private int nrofCreated;
    private int nrofDelivered;
    private Map<Integer, Double> nrofDeliver;

    // Constructor:
    public MessagePercentaceDeliveryPerContactReport() {
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
        this.nrofCreated = 0; // Initialize nrofCreated
        this.nrofDeliver = new HashMap<>();
    }


    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        nrofContact++;
//        System.out.println("Contact #" + nrofContact + " between " + host1 + " and " + host2); // Debug

        if (nrofContact - lastRecord >= interval) {
            lastRecord = nrofContact;
            double deliveryPercentage;
            if (nrofCreated > 0) {
                deliveryPercentage = ((1.0 * this.nrofDelivered) / this.nrofCreated) * 100;
            } else {
                deliveryPercentage = 0.0; // Avoid division by zero
            }
            nrofDeliver.put(lastRecord, deliveryPercentage);
//            System.out.println("Recording data at contact #" + lastRecord +
//                    ", Created: " + nrofCreated +
//                    ", Delivered: " + nrofDelivered +
//                    ", Percentage: " + deliveryPercentage); // Debug
        }
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {

    }

    @Override
    public void newMessage(Message m) {
        this.nrofCreated++;
//        System.out.println("New message created: " + m.getId() + ", Total created: " + nrofCreated); // Debug
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
//            System.out.println("Message delivered: " + m.getId() +
//                    ", from: " + from +
//                    ", to: " + to +
//                    ", Total delivered: " + nrofDelivered); // Debug
        }
    }
    @Override
    public void done() {
        String output = "Contact\tDeliveryPercentage\n";
        for (Map.Entry<Integer, Double> entry : nrofDeliver.entrySet()) {
            Integer key = entry.getKey();
            Double value = entry.getValue();
            output += key + "\t" + value + "\n";
        }
        write(output);
        super.done();
    }
}