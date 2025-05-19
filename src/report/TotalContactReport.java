/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.*;

import java.util.HashMap;
import java.util.Map;

public class TotalContactReport extends Report implements ConnectionListener {

    private int totalContacts;
    private int lastRecord;
    private int interval;
    private Map<Integer, Integer> contactRecords;

    public static final String NROF_CONTACT_INTERVAL = "perTotalContact";
    public static final int DEFAULT_CONTACT_COUNT = 600;

    public TotalContactReport() {
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
        this.totalContacts = 0;
        this.lastRecord = 0;
        this.contactRecords = new HashMap<>();
    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        totalContacts++;
        if (totalContacts - lastRecord >= interval) {
            lastRecord = totalContacts;
            contactRecords.put(lastRecord, totalContacts);
        }
    }

    @Override
    public void done() {
        String output = "Contacts\tTotalContacts\n";
        for (Map.Entry<Integer, Integer> entry : contactRecords.entrySet()) {
            output += entry.getKey() + "\t" + entry.getValue() + "\n";
        }
        write(output);
        super.done();
    }

    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {
    }
}

// package report;
//
// import core.ConnectionListener;
// import core.DTNHost;
// import core.Message;
// import core.MessageListener;
// import core.Settings;
// import core.UpdateListener;
// import java.routing.util.HashMap;
// import java.routing.util.List;
// import java.routing.util.Map;
//
/// **
// * TotalContactListener - Menghitung total kontak antar node selama simulasi.
// */
// public class TotalContactListener extends Report implements UpdateListener,
// ConnectionListener, MessageListener {
//
// private int nrofContacts; // Total kontak yang terjadi
// private int nrofCreatedMessages; // Total pesan yang dibuat
// private Map<Integer, Double> contactTimes; // Waktu saat kontak terjadi
//
// public TotalContactListener() {
// init();
// }
//
// /**
// * Inisialisasi variabel
// */
// protected void init() {
// super.init();
// this.nrofContacts = 0;
// this.nrofCreatedMessages = 0;
// this.contactTimes = new HashMap<>();
// }
//
// @Override
// public void updated(List<DTNHost> hosts) {
// // Tidak diperlukan untuk menghitung total kontak
// }
//
// @Override
// public void hostsConnected(DTNHost host1, DTNHost host2) {
// nrofContacts++;
// double currentTime = getSimTime();
// contactTimes.put(nrofContacts, currentTime);
//
// // Debugging output
// System.out.println("[TRACE] Kontak #" + nrofContacts + " terjadi antara " +
// host1.getAddress() + " dan " + host2.getAddress() +
// " pada waktu: " + currentTime);
// }
//
// @Override
// public void hostsDisconnected(DTNHost host1, DTNHost host2) {
// // Tidak digunakan dalam perhitungan total kontak
// }
//
// @Override
// public void newMessage(Message m) {
// if (isWarmup()) {
// addWarmupID(m.getId());
// return;
// }
// nrofCreatedMessages++;
//
// // Debugging output
// System.out.println("[TRACE] Pesan baru: " + m.getId() +
// " dibuat pada waktu " + getSimTime());
// }
//
// @Override
// public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
// // Tidak digunakan dalam perhitungan total kontak
// }
//
// @Override
// public void messageDeleted(Message m, DTNHost where, boolean dropped) {
// // Tidak digunakan dalam perhitungan total kontak
// }
//
// @Override
// public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
// // Tidak digunakan dalam perhitungan total kontak
// }
//
// @Override
// public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean
// firstDelivery) {
// // Tidak digunakan dalam perhitungan total kontak
// }
//
// @Override
// public void done() {
// write("Total Kontak: " + nrofContacts);
// write("Total Pesan Dibuat: " + nrofCreatedMessages);
// write("Kontak ke\tWaktu Kontak");
//
// for (Map.Entry<Integer, Double> entry : contactTimes.entrySet()) {
// write(entry.getKey() + "\t" + entry.getValue());
// }
//
// super.done();
// }
// }
