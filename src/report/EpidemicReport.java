package report;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.*;

public class EpidemicReport extends Report implements MessageListener, ConnectionListener {
    public static final String NROF_CONTACT_INTERVAL = "perTotalContact";
    public static final int DEFAULT_CONTACT_COUNT = 100;
    private int lastRecord;
    private int interval;
    private int nrofContact;
    private int nrofCreated;
    private int nrofDelivered;

    private Map<Integer, Double> overheadRatioPerContact;
    private Map<Integer, Double> avgLatencyPerContact;
    private Map<Integer, Double> totalForwardsPerContact;
    private double totalLatency = 0;
    private int totalForwards = 0;
    private int totalStarted = 0;

    private Map<Integer, Double> nrofDeliver;

    // Constructor
    public EpidemicReport() {
        init();
        if (getSettings().contains(NROF_CONTACT_INTERVAL)) {
            interval = getSettings().getInt(NROF_CONTACT_INTERVAL);
            System.out.println("interval " + interval);
        } else {
            interval = DEFAULT_CONTACT_COUNT;
            System.out.println("default " + interval);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.nrofDelivered = 0;
        this.lastRecord = 0;
        this.nrofContact = 0;
        this.nrofCreated = 0;
        this.totalStarted = 0;
        this.totalLatency = 0;
        this.totalForwards = 0;
        this.nrofDeliver = new HashMap<>();
        this.overheadRatioPerContact = new HashMap<>();
        this.avgLatencyPerContact = new HashMap<>();
        this.totalForwardsPerContact = new HashMap<>();
    }


    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        nrofContact++;

        if (nrofContact - lastRecord >= interval) {
            lastRecord = nrofContact;
            double deliveryPercentage;
            double overheadRatio;
            double avgLatency;
            double avgForwards;

            if (nrofCreated > 0) {
                deliveryPercentage = ((1.0 * this.nrofDelivered) / this.nrofCreated) * 100;
            } else {
                deliveryPercentage = 0.0;
            }

            if (nrofDelivered > 0) {
                overheadRatio = (1.0 * totalStarted) / nrofDelivered;
                avgLatency = totalLatency / nrofDelivered;
            } else {
                overheadRatio = 0.0;
                avgLatency = 0.0;
            }
            if (nrofCreated > 0) {
                avgForwards = (double) totalForwards / nrofCreated;
            } else {
                avgForwards = 0.0;
            }

            nrofDeliver.put(lastRecord, deliveryPercentage);
            overheadRatioPerContact.put(lastRecord, overheadRatio);
            avgLatencyPerContact.put(lastRecord, avgLatency);
            totalForwardsPerContact.put(lastRecord, avgForwards);
        }
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
        // Tidak ada perubahan spesifik untuk Epidemic di sini
    }

    @Override
    public void newMessage(Message m) {
        this.nrofCreated++;
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        totalStarted++; // Increment setiap kali transfer dimulai
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        // Tidak ada perubahan spesifik untuk Epidemic di sini
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        // Tidak ada perubahan spesifik untuk Epidemic di sini
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        if (firstDelivery) {
            this.nrofDelivered++;
            totalLatency += (m.getReceiveTime() - m.getCreationTime());
        }
        //Karena setiap tranfer dihitung, disini total forward akan bertambah
        totalForwards++;
    }

    @Override
    public void done() {
        String output = "Contact\tDeliveryPercentage\tOverheadRatio\tAvgLatency\tTotalForwards\n";

        List<Map.Entry<Integer, Double>> sortedEntries = new ArrayList<>(nrofDeliver.entrySet());
        Collections.sort(sortedEntries, Map.Entry.comparingByKey());

        for (Map.Entry<Integer, Double> entry : sortedEntries) {
            Integer key = entry.getKey();
            Double deliveryPercentage = entry.getValue();
            Double overheadRatio = overheadRatioPerContact.get(key);
            Double avgLatency = avgLatencyPerContact.get(key);
            Double avgForwards = totalForwardsPerContact.get(key);

            output += key + "\t" + deliveryPercentage + "\t" + overheadRatio + "\t" + avgLatency + "\t" + avgForwards + "\n";
        }
        write(output);
        super.done();
    }
}