/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.ConnectionListener;
import core.DTNHost;
import core.Message;
import core.MessageListener;

import java.util.HashMap;
import java.util.Map;


public class MessageDeliveryReportPerContact extends Report implements MessageListener, ConnectionListener {

    private static final String NROFCONTACT_INTERVAL = "perTotalContact";
    public static final int DEFAULT_CONTACT_COUNT = 600;
    private int lastRecord;
    private int interval;
    private int nrofContacts;
    private int nrofDelivered;
//    private int totalMessagesCreated;
    private Map<Integer, Integer> nrofDeliver;
//    private Map<Integer, Double> deliveryRatioPerContact;


    // Constructor: Menentukan interval berdasarkan konfigurasi
    public MessageDeliveryReportPerContact() {
        init();
        if (getSettings().contains(NROFCONTACT_INTERVAL)) {
            interval = getSettings().getInt(NROFCONTACT_INTERVAL);
        } else {
            interval = DEFAULT_CONTACT_COUNT;
        }
    }


    public void init() {
        super.init();
        this.nrofDelivered = 0;
        this.nrofContacts = 0;
//        this.totalMessagesCreated = 0;
        this.lastRecord = 0;
        this.interval = 0;
        this.nrofDeliver = new HashMap<>();
//        this.deliveryRatioPerContact = new HashMap<>();
    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        nrofContacts++;
        if (nrofContacts - lastRecord >= interval) {
            lastRecord = nrofContacts;
            nrofDeliver.put(lastRecord, nrofDelivered);


//            // Perhitungan Delivery Ratio (%)
//            double deliveryRatio = totalMessagesCreated > 0 ? (nrofDelivered / (double) totalMessagesCreated) * 100 : 0;
//            deliveryRatioPerContact.put(lastRecord, deliveryRatio);
        }

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
        if (firstDelivery) {
            this.nrofDelivered++;
        }
    }

    @Override
    public void done() {
//        write("Total Pesan yang Dibuat: " + totalMessagesCreated);
//        write("Total Kontak: " + totalContacts);
//        write("Total Pesan yang Diterima: " + nrofDelivered);
//        write("contact\tNrofDelivered\tDeliveryRatio (%)");
        String statsText = "Contact\tNrofDelivered\n";

        for (Map.Entry<Integer, Integer> entry : nrofDeliver.entrySet()) {
            Integer key = entry.getKey();
            Integer value = entry.getValue();
//            Double ratio = deliveryRatioPerContact.getOrDefault(key, 0.0);
//            write(key + "\t" + value + "\t" + String.format("%.2f", ratio));
            statsText += key + "\t" + value + "\n";
        }
        write(statsText);
        super.done();
    }
}
