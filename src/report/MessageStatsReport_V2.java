/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report for generating different kind of total statistics about message
 * relaying performance. Messages that were created during the warm up period
 * are ignored.
 * <P><strong>Note:</strong> if some statistics could not be created (e.g.
 * overhead ratio if no messages were delivered) "NaN" is reported for
 * double values and zero for integer median(s).
 */
public class MessageStatsReport_V2 extends Report implements MessageListener, UpdateListener, ConnectionListener {
	private Map<String, Double> creationTimes;
	private List<Double> latencies;
	private List<Integer> hopCounts;
	private List<Double> msgBufferTime;
	private List<Double> rtt; // round trip times

	private int nrofDropped;
	private int nrofRemoved;
	private int nrofStarted;
	private int nrofAborted;
	private int nrofRelayed;
	private int nrofCreated;
	private int nrofResponseReqCreated;
	private int nrofResponseDelivered;
	private int nrofDelivered;

	private int totalContacts;
	private final int contactInterval = 60; // Interval berdasarkan jumlah kontak

	// Struktur data untuk menyimpan data per interval kontak
	private List<Integer> intervalContactsList = new ArrayList<>();

	/**
	 * Constructor.
	 */
	public MessageStatsReport_V2() {
		init();
	}

	@Override
	protected void init() {
		super.init();
		this.creationTimes = new HashMap<>();
		this.latencies = new ArrayList<>();
		this.msgBufferTime = new ArrayList<>();
		this.hopCounts = new ArrayList<>();
		this.rtt = new ArrayList<>();

		this.nrofDropped = 0;
		this.nrofRemoved = 0;
		this.nrofStarted = 0;
		this.nrofAborted = 0;
		this.nrofRelayed = 0;
		this.nrofCreated = 0;
		this.nrofResponseReqCreated = 0;
		this.nrofResponseDelivered = 0;
		this.nrofDelivered = 0;
		this.totalContacts = 0;
		this.intervalContactsList.clear();
	}


	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if (isWarmupID(m.getId())) {
			return;
		}

		if (dropped) {
			this.nrofDropped++;
		}
		else {
			this.nrofRemoved++;
		}

		this.msgBufferTime.add(getSimTime() - m.getReceiveTime());
	}


	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofAborted++;
	}


	public void messageTransferred(Message m, DTNHost from, DTNHost to,
								   boolean finalTarget) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofRelayed++;
		if (finalTarget) {
			double latency = getSimTime() -
					this.creationTimes.get(m.getId()) ;
			this.latencies.add(latency);
			this.nrofDelivered++;
			this.hopCounts.add(m.getHops().size() - 1);

			if (m.isResponse()) {
				this.rtt.add(getSimTime() -  m.getRequest().getCreationTime());
				this.nrofResponseDelivered++;
			}
		}
	}


	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}

		this.creationTimes.put(m.getId(), getSimTime());
		this.nrofCreated++;
		if (m.getResponseSize() > 0) {
			this.nrofResponseReqCreated++;
		}
	}


	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		if (isWarmupID(m.getId())) {
			return;
		}

		this.nrofStarted++;
	}

	@Override
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		totalContacts++;
		if (totalContacts % contactInterval == 0) {
			intervalContactsList.add(totalContacts);
		}
	}

	@Override
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		//totalContacts--; // Jika Anda menghitung kontak unik, kurangi di sini
	}

	@Override
	public void updated(List<DTNHost> hosts) {

//		if (getSimTime() >= intervalLength && (getSimTime() % intervalLength) < 1) {
//			intervalDataList.add(getSimTime());
//		}
	}

	@Override
	public void done() {
		write("Message stats for scenario " + getScenarioName() +
				"\nsim_time: " + format(getSimTime()));

		// Tulis header
		write(String.format("%-15s\t%-15s\t%-15s\t%-15s\t%-15s\t%-15s%n",
				"Interval Contacts", "Total Contacts", "Delivery Ratio", "Overhead Ratio", "Average Latency", "Total Forwards"));
		write("-------------------------------------------------------------------------------------------------------------------------\n");

		// Debugging
		System.out.println("Ukuran intervalContactsList: " + intervalContactsList.size());

		// Tulis data interval kontak
		for (Integer contactCount : intervalContactsList) {
			write(String.format(
					"%-15d\t%-15d\t%-15.2f\t%-15.2f\t%-15.2f\t%-15d%n",
					contactCount,
					totalContacts,
					(this.nrofCreated > 0) ? (100.0 * this.nrofDelivered) / this.nrofCreated : 0,
					(this.nrofDelivered > 0) ? (1.0 * (this.nrofRelayed - this.nrofDelivered)) / this.nrofDelivered : Double.NaN,
					(this.nrofDelivered > 0) ? this.latencies.stream().mapToDouble(Double::doubleValue).sum() / this.nrofDelivered : 0,
					nrofRelayed
			));
		}

		super.done();
	}

}