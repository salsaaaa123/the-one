package report;

import core.*;

import java.util.*;

public class MessageForwardingPerContactReport extends Report implements MessageListener, ConnectionListener, UpdateListener {

    private double lastRecord;
    private int interval;
    private Map<DTNHost, Integer> nrofForwards;
    private Map<Integer, Integer> nrofForwardRecords;
    private static final String NROF_CONTACT_INTERVAL = "perTotalContact";
    private static final int DEFAULT_CONTACT_COUNT = 600;
    private int nrofContacts;

    public MessageForwardingPerContactReport() {
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
        this.nrofContacts = 0;
    }


    @Override
    public void done() {

        StringBuilder output = new StringBuilder("Contacts\tForward Counts\n");
        List<Map.Entry<Integer, Integer>> sortedEntry = new ArrayList<>(nrofForwardRecords.entrySet());
        if (sortedEntry.size() > 10_000) {
            sortedEntry = sortedEntry.parallelStream()
                    .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder())) // Descending
                    .toList();
            output.append("\n# Descending Order with parallelStream (Data > 10.000)\n");
//            sortedEntry = sortedEntry.parallelStream()
//                    .sorted(Map.Entry.comparingByKey()) // Ascending
//                    .toList();
        } else {
            sortedEntry.sort(Map.Entry.comparingByKey()); // Ascending
            output.append("\n# Ascending Order (Data < 10.000)\n");
//            sortedEntry.sort(Map.Entry.comparingByKey(Comparator.reverseOrder())); // Descending
        }
        sortedEntry.forEach(entry -> output.append(entry.getKey()).append("\t").append(entry.getValue()).append("\n"));
        write(output.toString());
        super.done();
    }


    @Override
    public void updated(List<DTNHost> hosts) {
        if (nrofContacts - lastRecord >= interval) {
            int totalForwardCount = nrofForwards.values().stream().mapToInt(Integer::intValue).sum();
//            System.out.println("Recording at contacts: " + nrofContacts + " | Total Forward Count: " + totalForwardCount); // debuging

            nrofForwardRecords.put(nrofContacts, totalForwardCount);
            lastRecord = nrofContacts;
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
//        System.out.println("Message transferred from " + from.getAddress() + " to " + to.getAddress() +
//                " | First Delivery: " + firstDelivery);

        // Tetap catat meskipun bukan first delivery
        nrofForwards.put(from, nrofForwards.getOrDefault(from, 0) + 1);
//        System.out.println("Updated forward count for " + from.getAddress() + ": " + nrofForwards.get(from));
    }




    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        nrofContacts++;
//        System.out.println("New Connection: " + host1.getAddress() + " <-> " + host2.getAddress() + " | Total Contacts: " + nrofContacts); // debuging
    }


    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {

    }
}

//        String output = "Contacts\tTotalForwards\n";
//        for (Map.Entry<Double, Integer> entry : nrofForwardRecords.entrySet()) {
//            output += entry.getKey() + "\t" + entry.getValue() + "\n";
//        }