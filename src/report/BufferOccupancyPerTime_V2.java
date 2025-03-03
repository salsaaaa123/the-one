/*
 * Copyright 2024 [Your Name/Organization]
 * Released under [License, e.g., GPLv3]
 */
package report;

import java.util.*;
import core.DTNHost;
import core.Settings;
import core.SimClock;
import core.SimScenario;
import core.UpdateListener;

/**
 * Kelas ini menghasilkan laporan tentang penggunaan buffer setiap node dalam simulasi DTN.
 * Laporan ini mencatat snapshot penggunaan buffer pada interval waktu tertentu,
 * dan memberikan laporan akhir yang menunjukkan penggunaan buffer setiap node di akhir simulasi.
 */
public class BufferOccupancyPerTime_V2 extends Report implements UpdateListener {

	public static final String BUFFER_REPORT_INTERVAL = "occupancyInterval";
	public static final int DEFAULT_BUFFER_REPORT_INTERVAL = 5;
	private double lastRecord = Double.MIN_VALUE;
	private int interval;
	private Map<DTNHost, LinkedList<Double>> bufferOccupancyHistory = new HashMap<>();
	private static final int MAX_HISTORY = 5; // Batasi jumlah history yang disimpan

	public BufferOccupancyPerTime_V2() {
		super();
		Settings settings = getSettings();
		interval = settings.getInt(BUFFER_REPORT_INTERVAL, DEFAULT_BUFFER_REPORT_INTERVAL);
	}

	@Override
	public void updated(List<DTNHost> hosts) {
		if (SimClock.getTime() - lastRecord >= interval) {
			lastRecord = SimClock.getTime();
			recordBufferOccupancy(hosts);
		}
	}

	private void recordBufferOccupancy(List<DTNHost> hosts) {
		for (DTNHost host : hosts) {
			double occupancy = Math.min(host.getBufferOccupancy(), 100.0);
			bufferOccupancyHistory.computeIfAbsent(host, k -> new LinkedList<>());
			LinkedList<Double> history = bufferOccupancyHistory.get(host);
			if (history.size() >= MAX_HISTORY) {
				history.pollFirst(); // Hapus data lama jika sudah mencapai batas
			}
			history.add(occupancy);
		}
	}

	@Override
	public void done() {
		StringBuilder report = new StringBuilder();
		report.append("Buffer Occupancy Report for scenario ").append(getScenarioName()).append("\n");
		report.append("sim_time: ").append(format(getSimTime())).append("\n");
		report.append("===========================================\n");
		report.append("| Host       | Buffer Occupancy (%)            |\n");
		report.append("-------------------------------------------\n");

		List<DTNHost> hosts = SimScenario.getInstance().getHosts();
		TreeMap<DTNHost, LinkedList<Double>> sortedOccupancy = new TreeMap<>(Comparator.comparingInt(DTNHost::getAddress));
		for (DTNHost host : hosts) {
			sortedOccupancy.put(host, bufferOccupancyHistory.getOrDefault(host, new LinkedList<>())) ;
		}

		for (Map.Entry<DTNHost, LinkedList<Double>> entry : sortedOccupancy.entrySet()) {
			DTNHost host = entry.getKey();
			LinkedList<Double> occupancies = entry.getValue();
			double finalOccupancy = occupancies.isEmpty() ? 0.0 : occupancies.getLast();
			report.append(String.format("| %-10s | %-16.2f%% |\n", host, finalOccupancy));
		}
		report.append("===========================================\n");

		// Format history lebih rapi
		report.append("\nDetailed Buffer Occupancy History:\n");
		report.append("================================================\n");
		report.append("| Host       | Buffer History (Last 5 Entries)\n");
		report.append("------------------------------------------------\n");
		for (Map.Entry<DTNHost, LinkedList<Double>> entry : bufferOccupancyHistory.entrySet()) {
			String historyString = entry.getValue().stream()
					.map(value -> String.format("%.2f%%", value))
					.reduce((a, b) -> a + ", " + b)
					.orElse("No Data");
			report.append(String.format("| %-10s | %s\n", entry.getKey(), historyString));
		}
		report.append("===============================================\n");

		write(report.toString());
		super.done();
	}
}
