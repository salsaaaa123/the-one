/*
 * Copyright 2024 [Your Name/Organization]
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.SimClock;

import java.util.*;

/**
 * Kelas ini menghasilkan laporan tentang jumlah pesan yang di-drop oleh setiap node dalam simulasi DTN.
 * Laporan mencakup:
 *  - Total drop per node selama simulasi (diurutkan berdasarkan total drop secara descending).
 *
 *  Tujuan: Untuk memberikan gambaran yang jelas dan terstruktur tentang bagaimana pesan
 *  hilang di jaringan, dan mengidentifikasi node mana yang paling sering menjatuhkan pesan.
 */
public class DroppedMessagesPerHostByTotal_v1 extends Report implements MessageListener {

    private static final int INTERVAL_LENGTH = 300;
    private Map<DTNHost, Integer> droppedPerNode;
    private Map<DTNHost, List<Integer>> droppedPerNodeInterval;
    private double nextReportTime;
    private List<String> intervalReports = new ArrayList<>();

    public DroppedMessagesPerHostByTotal_v1() {
        init();
        this.droppedPerNode = new HashMap<>();
        this.droppedPerNodeInterval = new HashMap<>();
        this.nextReportTime = INTERVAL_LENGTH;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
        if (dropped && !isWarmupID(m.getId())) {
            droppedPerNode.put(where, droppedPerNode.getOrDefault(where, 0) + 1);

            List<Integer> intervalCounts = droppedPerNodeInterval.getOrDefault(where, new ArrayList<>());
            if (SimClock.getTime() < nextReportTime) {
                if (intervalCounts.isEmpty()) {
                    intervalCounts.add(1);
                } else {
                    intervalCounts.set(intervalCounts.size() - 1, intervalCounts.get(intervalCounts.size() - 1) + 1);
                }
            } else {
                intervalCounts.add(1);
            }
            droppedPerNodeInterval.put(where, intervalCounts);
        }

        if (SimClock.getTime() >= nextReportTime) {
            recordIntervalReport();
            nextReportTime += INTERVAL_LENGTH;
        }
    }

    private void recordIntervalReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n");
        sb.append("Interval sim_time: ").append(format(getSimTime())).append("\n");
        sb.append("+------------+--------------------+----------------+\n");
        sb.append(String.format("%-10s | %-18s | %-13s\n", "Host", "Dropped (interval)", "Total Dropped"));
        sb.append("----------------------------------------------------\n");

        List<Map.Entry<DTNHost, Integer>> sortedList = new ArrayList<>(droppedPerNode.entrySet());
        sortedList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        for (Map.Entry<DTNHost, Integer> entry : sortedList) {
            DTNHost host = entry.getKey();
            int totalDropped = entry.getValue();
            List<Integer> intervalCounts = droppedPerNodeInterval.getOrDefault(host, new ArrayList<>());
            int currentIntervalCount = intervalCounts.isEmpty() ? 0 : intervalCounts.get(intervalCounts.size() - 1);
            sb.append(String.format("%-10s | %-18d | %-13d |\n", host, currentIntervalCount, totalDropped));
        }

        sb.append("----------------------------------------------------\n");
        intervalReports.add(sb.toString());
        droppedPerNodeInterval.clear();
    }

    @Override
    public void done() {
        write("Message dropped for scenario " + getScenarioName() + "\n");
        for (String report : intervalReports) {
            write(report);
            write("\n");
        }
        super.done();
    }

    @Override
    public void newMessage(Message m) {}
    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {}
}