/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.SimError;
import core.Tuple;

/**
 * Implementation of PRoPHET router as described in
 * <I>Probabilistic routing in intermittently connected networks</I> by
 * Anders Lindgren et al.
 */
public class ProphetRouterForwarding extends ActiveRouter {
	/** delivery predictability initialization constant */
	public static final double P_INIT = 0.75;
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_BETA = 0.25;
	/** delivery predictability aging constant */
	public static final double GAMMA = 0.98;

	/** Prophet router's setting namespace ({@value}) */
	public static final String PROPHET_NS = "ProphetRouterForwarding";
	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of
	 * delivery predictions. Should be tweaked for the scenario.
	 */
	public static final String SECONDS_IN_UNIT_S = "secondsInTimeUnit";

	/**
	 * Transitivity scaling constant (beta) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_BETA}.
	 */
	public static final String BETA_S = "beta";

	/**
	 * Forwarding strategy -setting id ({@value}). The forwarding strategy
	 * defines how the router should choose the next hop for a message.
	 * Possible values: GRTRMax, GRTRSort, GRTR, COIN.
	 * Default value for setting is GRTRMax.
	 */
	public static final String FORWARDING_STRATEGY_S = "forwardingStrategy";

	/**
	 * Queueing policy -setting id ({@value}). The policy that determines
	 * which message is dropped when the buffer is full.
	 * Possible values: FIFO_DROP, MOFO, SHLI, LEPR, MOPR.
	 * Default value for setting is FIFO_DROP.
	 */
	public static final String QUEUEING_POLICY_S = "queueingPolicy";

	private int secondsInTimeUnit;
	private double beta;

	/** delivery predictabilities */
	private Map<DTNHost, Double> preds;
	/** last delivery predictability update (sim)time */
	private double lastAgeUpdate;

	private ForwardingStrategyEnum forwardingStrategyEnum;
	private QueueingPolicyEnum queueingPolicyEnum;

	/** Map to store forward counts for MOFO policy */
	private Map<String, Integer> forwardedCounts;
	/** Map to store forwarding progress for MOPR policy (FP = sum of P(B,D)) */
	private Map<String, Double> forwardProgresses;

	private Random coinRandom;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * 
	 * @param s The settings object
	 */
	public ProphetRouterForwarding(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(PROPHET_NS);
		secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
		if (prophetSettings.contains(BETA_S)) {
			beta = prophetSettings.getDouble(BETA_S);
		} else {
			beta = DEFAULT_BETA;
		}

		// Read and set the forwarding strategy
		if (prophetSettings.contains(FORWARDING_STRATEGY_S)) {
			this.forwardingStrategyEnum = ForwardingStrategyEnum
					.of(prophetSettings.getSetting(FORWARDING_STRATEGY_S));
		} else {
			// Default strategy if not specified
			this.forwardingStrategyEnum = ForwardingStrategyEnum.GRTRMax;
		}

		// Read and set the queueing policy
		if (prophetSettings.contains(QUEUEING_POLICY_S)) {
			this.queueingPolicyEnum = QueueingPolicyEnum.of(prophetSettings.getSetting(QUEUEING_POLICY_S));
		} else {
			this.queueingPolicyEnum = QueueingPolicyEnum.FIFO_DROP; // Default policy
		}
		initPolicyMaps();

		initPreds();
		// this.lastAgeUpdate = SimClock.getTime(); // Initialize lastAgeUpdate
		this.coinRandom = new Random(SimClock.getIntTime()); // Initialize random for COIN
	}

	/**
	 * Copyconstructor.
	 * 
	 * @param r The router prototype where setting values are copied from
	 */
	protected ProphetRouterForwarding(ProphetRouterForwarding r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.forwardingStrategyEnum = r.forwardingStrategyEnum;
		// this.lastAgeUpdate = r.lastAgeUpdate;
		this.beta = r.beta;
		this.queueingPolicyEnum = r.queueingPolicyEnum;
		this.coinRandom = new Random(SimClock.getIntTime());

		initPreds();
		initPolicyMaps();
	}

	/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
	}

	/**
	 * Initializes policy maps (forwardedCounts, forwardProgresses). - METODE BARU -
	 * DITAMBAHKAN
	 * Ini hanya menginisialisasi map kosong.
	 */
	private void initPolicyMaps() {
		this.forwardedCounts = new HashMap<>();
		this.forwardProgresses = new HashMap<>();
	}

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateDeliveryPredFor(otherHost);
			updateTransitivePreds(otherHost);
		}
	}

	/**
	 * Updates delivery predictions for a host.
	 * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * P_INIT</CODE>
	 * 
	 * @param host The host we just met
	 */
	private void updateDeliveryPredFor(DTNHost host) {
		double oldValue = getPredFor(host);
		double newValue = oldValue + (1 - oldValue) * P_INIT;
		preds.put(host, newValue);
	}

	/**
	 * Returns the current prediction (P) value for a host or 0 if entry for
	 * the host doesn't exist.
	 * 
	 * @param host The host to look the P for
	 * @return the current P value
	 */
	public double getPredFor(DTNHost host) {
		ageDeliveryPreds(); // make sure preds are updated before getting
		if (preds.containsKey(host)) {
			return preds.get(host);
		} else {
			return 0;
		}
	}

	/**
	 * Updates transitive (A->B->C) delivery predictions.
	 * <CODE>P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
	 * </CODE>
	 * 
	 * @param host The B host who we just met
	 */
	private void updateTransitivePreds(DTNHost host) {
		MessageRouter otherRouter = host.getRouter();
		assert otherRouter instanceof ProphetRouterForwarding : "PRoPHET only works " +
				" with other routers of same type";

		double pForHost = getPredFor(host); // P(a,b)
		Map<DTNHost, Double> othersPreds = ((ProphetRouterForwarding) otherRouter).getDeliveryPreds();

		for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
			if (e.getKey() == getHost()) {
				continue; // don't add yourself
			}

			double pOld = getPredFor(e.getKey()); // P(a,c)_old
			double pNew = pOld + (1 - pOld) * pForHost * e.getValue() * beta; // P(a,c) = P(a,c)_old + (1 -
																	// P(a,c)_old) * P(a,b) * P(b,c) *
																	// BETA
			preds.put(e.getKey(), pNew);
		}
	}

	/**
	 * Ages all entries in the delivery predictions.
	 * <CODE>P(a,b) = P(a,b)_old * (GAMMA ^ k)</CODE>, where k is number of
	 * time units that have elapsed since the last time the metric was aged.
	 * 
	 * @see #SECONDS_IN_UNIT_S
	 */
	private void ageDeliveryPreds() {
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) /
				secondsInTimeUnit;

		if (timeDiff == 0) { // Also handle timeDiff = 0 or negative (shouldn't happen but for safety)
			return;
		}

		double mult = Math.pow(GAMMA, timeDiff);
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			e.setValue(e.getValue() * mult);
		}

		this.lastAgeUpdate = SimClock.getTime();
	}

	/**
	 * Returns a map of this router's delivery predictions
	 * 
	 * @return a map of this router's delivery predictions
	 */
	private Map<DTNHost, Double> getDeliveryPreds() {
		ageDeliveryPreds(); // make sure the aging is done
		return this.preds;
	}

	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}

		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		// If no deliverable message, try other messages based on forwarding strategy
		tryOtherMessages();
	}

	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * 
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages = new ArrayList<Tuple<Message, Connection>>();

		Collection<Message> msgCollection = getMessageCollection();

		/*
		 * for all connected hosts collect all messages that have a higher
		 * probability of delivery by the other host
		 */
		// for (Connection con : getHost()) {
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			ProphetRouterForwarding othRouter = (ProphetRouterForwarding) other.getRouter();

			// Ensure the oher router is also ProphetRouterForwarding,unless the strategy
			// is
			// coin
			// if (forwardingStrategyEnum != ForwardingStrategyEnum.COIN
			// && !(othRouter instanceof ProphetRouterForwarding)) {
			// // System.err.println("Warning: Skipping non-Prophet router for non-COIN
			// // strategy.");
			// continue;
			// }
			// ProphetRouterForwarding othProphetRouterForwarding = null;
			// if (othRouter instanceof ProphetRouterForwarding) {
			// othProphetRouterForwarding = (ProphetRouterForwarding) othRouter;
			// }

			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}

			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				boolean shouldConsider = false;
				switch (forwardingStrategyEnum) {
					case COIN:
						// *** REVISED COIN LOGIC with NULL CHECK ***
						// Ensure coinRandom is initialized before use
						if (this.coinRandom == null) {
							// System.err.println(
							// "WARNING: coinRandom was NULL for COIN strategy! Re-initializing in
							// tryOtherMessages for router "
							// + getHost() + ".");
							this.coinRandom = new Random(SimClock.getIntTime());
						}
						if (this.coinRandom.nextDouble() > 0.5) {
							shouldConsider = true;
						}
						break;
					case GRTR: // Fall-through intentional
					case GRTRSort: // Fall-through intentional
					case GRTRMax:
						// For GRTR-based strategies, apply the P(B,D) > P(A,D) filter
						// othProphetRouterForwarding is guaranteed to be non-null here by the outer
						// check
						if (othRouter instanceof ProphetRouterForwarding) {
							ProphetRouterForwarding othProphetRouter = (ProphetRouterForwarding) othRouter;
							if (othProphetRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
								shouldConsider = true;
							}
						}
						// Jika peer bukan Prophet, pesan tidak lolos filter GRTR/Sort/Max.
						break;
					default:
						// Should not happen with current enums, but good practice
						throw new SimError("Unknown forwarding strategy: " + forwardingStrategyEnum);
				}
				if (shouldConsider) {
					messages.add(new Tuple<Message, Connection>(m, con));
				}
			}
		}

		if (messages.size() == 0) {
			return null;
		}

		// --- Sort the list of potential transfers based on strategy (if required) ---
		switch (forwardingStrategyEnum) {
			case COIN:
			case GRTR:
				// COIN's selection is already probabilistic during list population.
				// GRTR relies on the linear scan order from msgCollection iteration.
				break;
			case GRTRSort:
				// Sort by P(B,D) - P(A,D) difference (descending)
				Collections.sort(messages, new GRTRSortTupleComparator());
				break;
			case GRTRMax:
				// Sort by P(B,D) probability (descending)
				Collections.sort(messages, new GRTRMaxTupleComparator());
				break;
		}

		// Try to send the messages in the determined order
		return tryMessagesForConnected(messages);
	}

	@Override
	public RoutingInfo getRoutingInfo() {
		ageDeliveryPreds();
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(preds.size() +
				" delivery prediction(s), strategy: " + forwardingStrategyEnum + ", policy: " + queueingPolicyEnum);

		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			DTNHost host = e.getKey();
			Double value = e.getValue();

			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f",
					host, value)));
		}
		// Tambahkan info MOFO/MOPR jika aktif
		if (queueingPolicyEnum == QueueingPolicyEnum.MOFO) {
			RoutingInfo mofoInfo = new RoutingInfo(forwardedCounts.size() + " msgs with forward counts");
			// Detail pesan/hitungan bisa ditambahkan di sini jika verbose mode
			// if (get){ // Check if detailed info is requested, e.g., via a setting
			// for(Map.Entry<String, Integer> entry : forwardedCounts.entrySet()) {
			// mofoInfo.addMoreInfo(new RoutingInfo(entry.getKey() + ": " +
			// entry.getValue()));
			// }
			// }
			ri.addMoreInfo(mofoInfo);
		}
		if (queueingPolicyEnum == QueueingPolicyEnum.MOPR) {
			RoutingInfo moprInfo = new RoutingInfo(forwardProgresses.size() + " msgs with forward progress");
			// Detail pesan/FP bisa ditambahkan
			// if (get){ // Check if detailed info is requested
			// for(Map.Entry<String, Double> entry : forwardProgresses.entrySet()) {
			// moprInfo.addMoreInfo(new RoutingInfo(String.format("%s: %.6f",
			// entry.getKey(), entry.getValue())));
			// }
			// }
			ri.addMoreInfo(moprInfo);
		}

		top.addMoreInfo(ri);
		return top;
	}

	@Override
	public MessageRouter replicate() {
		ProphetRouterForwarding r = new ProphetRouterForwarding(this);
		return r;
	}

	public enum ForwardingStrategyEnum {
		GRTRMax("GRTRMax"),
		GRTRSort("GRTRSort"),
		GRTR("GRTR"),
		COIN("COIN");

		private String name;

		private ForwardingStrategyEnum(String name) {
			this.name = name;
		}

		/**
		 * Returns the enum constant for the given strategy name string.
		 *
		 * @param name The name of the strategy (case-insensitive)
		 * @return The corresponding enum constant
		 * @throws IllegalArgumentException if the name is unknown
		 */
		public static ForwardingStrategyEnum of(String name) {
			for (ForwardingStrategyEnum strategy : values()) {
				if (strategy.name.equalsIgnoreCase(name)) {
					return strategy;
				}
			}
			throw new IllegalArgumentException("Unknown forwarding strategy: " + name);
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * (GRTRMax) Comparator for Message-Connection-Tuples that orders the tuples
	 * by their delivery probability by the host on the other side of the
	 * connection (descending). Uses queue mode as tie-breaker.
	 */
	private class GRTRMaxTupleComparator implements Comparator<Tuple<Message, Connection>> {
		@Override
		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {

			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((ProphetRouterForwarding) tuple1.getValue().getOtherNode(getHost()).getRouter())
					.getPredFor(
							tuple1.getKey().getTo());
			// -"- tuple2...
			double p2 = ((ProphetRouterForwarding) tuple2.getValue().getOtherNode(getHost()).getRouter())
					.getPredFor(
							tuple2.getKey().getTo());

			// Sort descending by probability (p2 - p1)
			if (p2 - p1 == 0) {
				/* equal probabilities -> let queue mode decide as a tie-breaker */
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());

			} else if (p2 - p1 < 0) {
				return -1; // p1 > p2, tuple1 comes after tuple2
			} else {
				return 1; // p2 > p1, tuple2 comes after tuple1
			}
		}
	}

	/**
	 * (GRTRSort) Comparator for Message-Connection-Tuples that orders the tuples
	 * by the difference P(B,D) - P(A,D) (descending). Uses queue mode as
	 * tie-breaker.
	 */
	private class GRTRSortTupleComparator implements Comparator<Tuple<Message, Connection>> {
		@Override
		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// Note: othProphetRouterForwarding is guaranteed non-null when this comparator
			// is used (GRTRMax/GRTRSort)

			// Mendapatkan router peer untuk kedua tuple
			ProphetRouterForwarding otherRouter1 = (ProphetRouterForwarding) tuple1.getValue()
					.getOtherNode(getHost()).getRouter(); // --- MODIFIED (Cast here) ---
			ProphetRouterForwarding otherRouter2 = (ProphetRouterForwarding) tuple2.getValue()
					.getOtherNode(getHost()).getRouter(); // --- MODIFIED (Cast here) ---

			// Menghitung P(B,D) dan P(A,D) untuk pesan pertama (tuple1)
			double pBD1 = otherRouter1.getPredFor(tuple1.getKey().getTo()); // P(B1, D1)
			double pAD1 = getPredFor(tuple1.getKey().getTo()); // P(A, D1) - Prediktabilitas node saat ini (A) ke
														// tujuan pesan 1 (D1)

			// Menghitung P(B,D) dan P(A,D) untuk pesan kedua (tuple2)
			double pBD2 = otherRouter2.getPredFor(tuple2.getKey().getTo()); // P(B2, D2)
			double pAD2 = getPredFor(tuple2.getKey().getTo()); // P(A, D2) - Prediktabilitas node saat ini (A) ke
														// tujuan pesan 2 (D2)

			// Menghitung selisih P(B,D) - P(A,D) untuk kedua pesan
			double difference1 = pBD1 - pAD1;
			double difference2 = pBD2 - pAD2;

			// Mengurutkan menurun berdasarkan selisih (difference2 - difference1)
			if (difference2 - difference1 == 0) {
				/* equal differences -> let queue mode decide as a tie-breaker */
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
			} else if (difference2 - difference1 < 0) {
				return -1; // difference1 > difference2, tuple1 comes setelah tuple2
			} else {
				return 1; // difference2 > difference1, tuple2 comes setelah tuple1
			}
		}
	}

	public enum QueueingPolicyEnum {
		FIFO_DROP("FIFO_DROP"), // Drop terlama (standard di ActiveRouter saat ini)
		MOFO("MOFO"), // Drop paling sering diforward
		SHLI("SHLI"), // Drop TTL tersingkat
		LEPR("LEPR"), // Drop P terendah (dari node saat ini ke tujuan)
		MOPR("MOPR"); // Drop Forwarding Progress tertinggi

		private final String name;

		QueueingPolicyEnum(String name) {
			this.name = name;
		}

		/**
		 * Returns the enum constant for the given policy name string.
		 *
		 * @param name The name of the policy (case-insensitive)
		 * @return The corresponding enum constant
		 * @throws IllegalArgumentException if the name is unknown
		 */
		public static QueueingPolicyEnum of(String name) {
			for (QueueingPolicyEnum policy : values()) {
				if (policy.name.equalsIgnoreCase(name)) {
					return policy;
				}
			}
			throw new IllegalArgumentException("Unknown queueing policy: " + name);
		}

		@Override
		public String toString() {
			return name;
		}
	}

	@Override
	public void deleteMessage(String id, boolean drop) {
		forwardedCounts.remove(id);
		forwardProgresses.remove(id);

		super.deleteMessage(id, drop);
	}

	@Override
	protected void transferDone(Connection con) {
		super.transferDone(con);

		// Logika pembaruan state MOFO/MOPR hanya berjalan di sisi pengirim
		// Menggunakan getHost() dan getOtherNode() karena Connection tidak punya method
		// getSender/Receiver
		// Asumsikan router ini adalah pengirim karena transferDone dipanggil untuk
		// koneksi di sendingConnections
		DTNHost sender = getHost();
		// Penerima adalah node lain di koneksi ini
		DTNHost receiver = con.getOtherNode(sender);

		Message transferredMsg = con.getMessage();
		if (transferredMsg == null) {
			return;
		}
		String msgId = transferredMsg.getId();

		// --- Update MOFO (Forward Count) ---
		// Ambil hitungan saat ini, jika tidak ada default 0, tambahkan 1
		int currentCount = forwardedCounts.getOrDefault(msgId, 0);
		forwardedCounts.put(msgId, currentCount + 1);

		// --- Update MOPR (Forwarding Progress) ---
		// FP = FPold + P(B,D)
		// B adalah receiver, D adalah tujuan pesan transferredMsg.getTo()
		// Kita perlu nilai P(B,D), yaitu prediktabilitas receiver (B) ke tujuan pesan
		// (D).
		// Ini membutuhkan router 'receiver' untuk menjadi ProphetRouterForwarding.
		// Jika bukan, P(B,D) dianggap 0 karena tidak ada prediktabilitas Prophet.
		double pBD = 0.0; // Default P(B,D) = 0 jika peer bukan Prophet
		if (receiver.getRouter() instanceof ProphetRouterForwarding) {
			ProphetRouterForwarding receiverRouter = (ProphetRouterForwarding) receiver.getRouter();
			pBD = receiverRouter.getPredFor(transferredMsg.getTo()); // getPredFor() dari ProphetRouterForwarding
		}

		// Ambil FP saat ini, jika tidak ada default 0.0, tambahkan P(B,D)
		double currentFP = forwardProgresses.getOrDefault(msgId, 0.0);
		forwardProgresses.put(msgId, currentFP + pBD);
	}

	// @Override
	// public Message messageTransferred(String id, DTNHost from) {
	// // Ini adalah metode dari MessageRouter, dipanggil di penerima setelah
	// transfer.
	// // Logika pembaruan state MOFO/MOPR seharusnya ada di sisi pengirim.
	// // Jadi, kita hanya panggil superclass method di sini.
	// Message m = super.messageTransferred(id, from);

	// return m;
	// }

	// --- START: Implementasi Queueing Policy (Drop Logic) --- //

	// @Override // Override makeRoomForMessage dari ActiveRouter
	// protected boolean makeRoomForMessage(int size) {
	// if (size > this.getBufferSize()) {
	// return false; // message too big for the buffer
	// }

	// int freeBuffer = this.getFreeBufferSize();
	// /* delete messages from the buffer until there's enough space */
	// while (freeBuffer < size) {
	// // Panggil metode untuk memilih pesan yang akan di-drop berdasarkan kebijakan
	// Message m = selectMessageToDrop(true); // true: jangan drop pesan yang sedang
	// dikirim

	// if (m == null) {
	// // Tidak ada pesan yang bisa di-drop sama sekali dan buffer masih penuh.
	// // Ini bisa terjadi jika semua pesan sedang dikirim dan buffer penuh.
	// // Dalam kasus ini, tidak ada ruang yang bisa dibuat saat ini.
	// return false;
	// }

	// /* delete message from the buffer as "drop" */
	// deleteMessage(m.getId(), true); // Memanggil deleteMessage yang sudah
	// di-override untuk clean up map
	// freeBuffer += m.getSize();
	// }
	// return true;
	// }

	/**
	 * Memilih pesan dari buffer untuk di-drop berdasarkan kebijakan antrian.
	 *
	 * @param excludeMsgBeingSent Jangan pilih pesan yang sedang dikirim jika true.
	 * @return Pesan yang dipilih untuk di-drop atau null jika tidak ada.
	 */
	protected Message selectMessageToDrop(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		List<Message> droppableMessages = new ArrayList<>();
		for (Message m : messages) {
			if (!(excludeMsgBeingSent && isSending(m.getId()))) {
				droppableMessages.add(m);
			}
		}

		if (droppableMessages.isEmpty()) {
			return null; // Tidak ada pesan yang bisa di-drop
		}

		Comparator<Message> droppingComparator = null;

		switch (queueingPolicyEnum) {
			case FIFO_DROP:
				// Drop paling tua (receive time terkecil) = TERTINGGI prioritas drop
				droppingComparator = Comparator.comparingDouble(Message::getReceiveTime);
				// Tie-breaker: gunakan compareByQueueMode dari superclass
				droppingComparator = droppingComparator.thenComparing(super::compareByQueueMode); // <-- PANGGIL
																					// super.compareByQueueMode
				break;

			case MOFO:
				// MOFO: Drop paling sering diforward (count tertinggi) = TERTINGGI prioritas
				// drop
				droppingComparator = Comparator
						.comparingInt((Message m) -> forwardedCounts.getOrDefault(m.getId(), 0)).reversed();
				// Tie-breaker: gunakan compareByQueueMode dari superclass
				droppingComparator = droppingComparator.thenComparing(super::compareByQueueMode); // <-- PANGGIL
																					// super.compareByQueueMode
				break;

			case SHLI:
				// SHLI: Drop TTL tersingkat (TTL terkecil) = TERTINGGI prioritas drop
				droppingComparator = Comparator.comparingInt(Message::getTtl);
				// Tie-breaker: gunakan compareByQueueMode dari superclass
				droppingComparator = droppingComparator.thenComparing(super::compareByQueueMode); // <-- PANGGIL
																					// super.compareByQueueMode
				break;

			case LEPR:
				// LEPR: Drop P(A,D) terendah (prediksi node saat ini ke tujuan terendah) =
				// TERTINGGI prioritas drop
				droppingComparator = Comparator.comparingDouble((Message m) -> getPredFor(m.getTo())); // getPredFor()
																						// dari
																						// ProphetRouterForwarding
				// Tie-breaker: gunakan compareByQueueMode dari superclass
				droppingComparator = droppingComparator.thenComparing(super::compareByQueueMode); // <-- PANGGIL
																					// super.compareByQueueMode
				break;

			case MOPR:
				// MOPR: Drop FP tertinggi (FP tertinggi) = TERTINGGI prioritas drop
				droppingComparator = Comparator
						.comparingDouble((Message m) -> forwardProgresses.getOrDefault(m.getId(), 0.0))
						.reversed(); // forwardedProgresses dari ProphetRouterForwarding
				// Tie-breaker: gunakan compareByQueueMode dari superclass
				droppingComparator = droppingComparator.thenComparing(super::compareByQueueMode); // <-- PANGGIL
																					// super.compareByQueueMode
				break;

			default:
				throw new SimError("Unknown queueing policy " + queueingPolicyEnum + " in selectMessageToDrop");
		}

		if (droppingComparator == null) {
			throw new SimError("Dropping comparator is null for policy " + queueingPolicyEnum);
		}

		// Cari pesan dengan prioritas dropping tertinggi menggunakan comparator yang
		// sudah didefinisikan
		Message messageToDrop = Collections.max(droppableMessages, droppingComparator);

		// Collections.max akan mengembalikan null jika list kosong, tapi kita sudah cek
		// isEmpty().
		// Jadi, messageToDrop seharusnya tidak null di sini.
		// Final null check untuk keamanan.
		if (messageToDrop == null && !droppableMessages.isEmpty()) {
			throw new SimError("selectMessageToDrop logic failed to select a message from a non-empty list.");
		}

		return messageToDrop;
	}
	// --- END: Implementasi Queueing Policy (Drop Logic) --- //
}