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

	/** the value of nrof seconds in time unit -setting */
	private int secondsInTimeUnit;
	/** value of beta setting */
	private double beta;

	/** delivery predictabilities */
	private Map<DTNHost, Double> preds;
	/** last delivery predictability update (sim)time */
	private double lastAgeUpdate;

	private ForwardingStrategyEnum forwardingStrategyEnum;

	/** The selected queueing policy */
	private QueueingPolicyEnum queueingPolicyEnum;

	/** Map to store forward counts for MOFO policy */
	private Map<String, Integer> forwardedCounts;

	/** Map to store forwarding progress for MOPR policy (FP = sum of P(B,D)) */
	private Map<String, Double> forwardProgresses;

	/**
	 * The router instance this router was copied from (if replicated) - DITAMBAHKAN
	 * untuk fix NPE
	 */
	// transient agar tidak diserialisasi jika simulator mendukung serialisasi
	private transient ProphetRouterForwarding sourceRouterForCopy;

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
		this.beta = r.beta;
		this.queueingPolicyEnum = r.queueingPolicyEnum;

		initPreds();
		initPolicyMaps();
		this.sourceRouterForCopy = r;
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

	/**
	 * Copies policy state (forward counts, FP) from a source router. - METODE BARU
	 * - DITAMBAHKAN untuk fix NPE
	 * Should be called AFTER the router and its messages are initialized/copied.
	 * 
	 * @param sourceRouter The router to copy state from.
	 */
	private void copyPolicyStateFrom(ProphetRouterForwarding sourceRouter) {
		// Pastikan sourceRouter tidak null dan memiliki map state
		if (sourceRouter == null || sourceRouter.forwardedCounts == null || sourceRouter.forwardProgresses == null) {
			// Ini bukan salinan atau sumber tidak valid, tidak perlu menyalin state.
			return;
		}

		// Iterate melalui pesan di router BARU ini (this.getMessageCollection())
		// dan salin state terkait dari router SUMBER (sourceRouter)
		// Ini berasumsi bahwa pesan di router baru memiliki ID yang sama
		// dengan pesan di router sumber dari mana mereka disalin.
		for (Message m : this.getMessageCollection()) {
			String msgId = m.getId();
			if (sourceRouter.forwardedCounts.containsKey(msgId)) {
				this.forwardedCounts.put(msgId, sourceRouter.forwardedCounts.get(msgId));
			}
			if (sourceRouter.forwardProgresses.containsKey(msgId)) {
				this.forwardProgresses.put(msgId, sourceRouter.forwardProgresses.get(msgId));
			}
		}
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
			double pNew = pOld + (1 - pOld) * pForHost * e.getValue() * beta;
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

			// Ensure the oher router is also ProphetRouterForwarding,unless the strategy is
			// coin
			if (!(othRouter instanceof ProphetRouterForwarding)
					&& forwardingStrategyEnum != ForwardingStrategyEnum.COIN) {
				// System.err.println("Warning: Skipping non-Prophet router for non-COIN
				// strategy.");
				continue;
			}
			ProphetRouterForwarding othProphetRouterForwarding = null;
			if (forwardingStrategyEnum != ForwardingStrategyEnum.COIN) {
				othProphetRouterForwarding = (ProphetRouterForwarding) othRouter;
			}

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
						// fOR coin, and all messages to the lis,random choice happens later
						shouldConsider = true;
						break;
					case GRTR: // Fall-through intentional
					case GRTRSort: // Fall-through intentional
					case GRTRMax:
						// For GRTR-based strategies, apply the P(B,D) > P(A,D) filter
						if (othProphetRouterForwarding != null) { // Should always be true here due to outer check
							if (othProphetRouterForwarding.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
								shouldConsider = true;
							}
						}
						break;
				}
				if (shouldConsider) {
					messages.add(new Tuple<Message, Connection>(m, con));
				}
			}
		}

		if (messages.size() == 0) {
			return null;
		}

		// Sort or shuffle the messages based on strategy
		switch (forwardingStrategyEnum) {
			case COIN:
				// For COIN, shuffle the list of all potential messages
				Collections.shuffle(messages, new Random(SimClock.getIntTime())); // Use SimClock time for
																		// repeatability in simulation
				break;
			case GRTR:
				// GRTR orders based on queue mode. The list is already implicitly ordered
				// as messages were added from the message collection (which is linked hash set
				// usually preserving insertion order, though not guaranteed in all sets)
				// or we explicitly sort by queue mode here. Let's sort by queue mode
				// for explicit behavior matching compareByQueueMode.
				Collections.sort(messages, new GRTRTupleComparator());
				break;
			case GRTRSort:
				Collections.sort(messages, new GRTRSortTupleComparator());
				break;
			case GRTRMax:
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
		// Tambahkan info MOFO/MOPR jika aktif - DITAMBAHKAN
		if (queueingPolicyEnum == QueueingPolicyEnum.MOFO) {
			RoutingInfo mofoInfo = new RoutingInfo(forwardedCounts.size() + " msgs with forward counts");
			// Detail pesan/hitungan bisa ditambahkan di sini jika verbose mode
			ri.addMoreInfo(mofoInfo);
		}
		if (queueingPolicyEnum == QueueingPolicyEnum.MOPR) {
			RoutingInfo moprInfo = new RoutingInfo(forwardProgresses.size() + " msgs with forward progress");
			// Detail pesan/FP bisa ditambahkan
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

	private interface ForwardingStrategyComparator extends Comparator<Tuple<Message, Connection>> {
		// empty interface, just for clarity
	}

	/**
	 * (GRTRMax) Comparator for Message-Connection-Tuples that orders the tuples
	 * by their delivery probability by the host on the other side of the
	 * connection (descending).
	 */
	private class GRTRMaxTupleComparator implements ForwardingStrategyComparator {
		@Override
		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((ProphetRouterForwarding) tuple1.getValue().getOtherNode(getHost()).getRouter()).getPredFor(
					tuple1.getKey().getTo());
			// -"- tuple2...
			double p2 = ((ProphetRouterForwarding) tuple2.getValue().getOtherNode(getHost()).getRouter()).getPredFor(
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
	 * by the difference P(B,D) - P(A,D) (descending).
	 */
	private class GRTRSortTupleComparator implements ForwardingStrategyComparator {
		@Override
		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// Mendapatkan router peer untuk kedua tuple
			ProphetRouterForwarding otherRouter1 = (ProphetRouterForwarding) tuple1.getValue()
					.getOtherNode(getHost()).getRouter();
			ProphetRouterForwarding otherRouter2 = (ProphetRouterForwarding) tuple2.getValue()
					.getOtherNode(getHost()).getRouter();

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

	/**
	 * (GRTR) Comparator that orders tuples based on the router's configured
	 * queueing policy.
	 */
	private class GRTRTupleComparator implements ForwardingStrategyComparator {
		@Override
		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// Order based on the router's queue mode setting
			return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
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

	@Override // Meng-override transferDone dari ActiveRouter - LOGIKA BARU
	protected void transferDone(Connection con) {
		super.transferDone(con); // Panggil superclass method

		// Logika pembaruan state MOFO/MOPR hanya berjalan di sisi pengirim -
		// DITAMBAHKAN
		if (con.getSender() == getHost()) {
			Message transferredMsg = con.getMessage();
			String msgId = transferredMsg.getId();
			DTNHost receiver = con.getReceiver();

			// --- Update MOFO (Forward Count) ---
			// Ambil hitungan saat ini, jika tidak ada default 0, tambahkan 1 - DITAMBAHKAN
			int currentCount = forwardedCounts.getOrDefault(msgId, 0);
			forwardedCounts.put(msgId, currentCount + 1);

			// --- Update MOPR (Forwarding Progress) ---
			// FP = FPold + P(B,D) - DITAMBAHKAN
			// B adalah receiver, D adalah tujuan pesan transferredMsg.getTo()
			// Kita perlu nilai P(B,D), yaitu prediktabilitas receiver (B) ke tujuan pesan
			// (D).
			// Ini membutuhkan router 'receiver' untuk menjadi ProphetRouterForwarding.
			if (receiver.getRouter() instanceof ProphetRouterForwarding) {
				ProphetRouterForwarding receiverRouter = (ProphetRouterForwarding) receiver.getRouter();
				double pBD = receiverRouter.getPredFor(transferredMsg.getTo()); // Dapatkan P(B,D)

				// Ambil FP saat ini, jika tidak ada default 0.0, tambahkan P(B,D) - DITAMBAHKAN
				double currentFP = forwardProgresses.getOrDefault(msgId, 0.0);
				forwardProgresses.put(msgId, currentFP + pBD);
			}
		}
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		// Ini adalah metode dari MessageRouter, dipanggil di penerima setelah transfer.
		// Logika pembaruan state MOFO/MOPR seharusnya ada di sisi pengirim.
		// Jadi, kita hanya panggil superclass method di sini.
		Message m = super.messageTransferred(id, from);

		return m;
	}

	// --- START: Implementasi Queueing Policy (Drop Logic) --- //

	@Override // Override makeRoomForMessage dari ActiveRouter
	protected boolean makeRoomForMessage(int size) {
		if (size > this.getBufferSize()) {
			return false; // message too big for the buffer
		}

		int freeBuffer = this.getFreeBufferSize();
		/* delete messages from the buffer until there's enough space */
		while (freeBuffer < size) {
			// Panggil metode untuk memilih pesan yang akan di-drop berdasarkan kebijakan
			Message m = selectMessageToDrop(true); // true: jangan drop pesan yang sedang dikirim

			if (m == null) {
				// Tidak ada pesan yang bisa di-drop sama sekali dan buffer masih penuh.
				// Ini bisa terjadi jika semua pesan sedang dikirim dan buffer penuh.
				// Dalam kasus ini, tidak ada ruang yang bisa dibuat saat ini.
				return false;
			}

			/* delete message from the buffer as "drop" */
			deleteMessage(m.getId(), true); // Memanggil deleteMessage yang sudah di-override untuk clean up map
			freeBuffer += m.getSize();
		}
		return true;
	}

	/**
	 * Memilih pesan dari buffer untuk di-drop berdasarkan kebijakan antrian. -
	 * METODE BARU - DITAMBAHKAN
	 * 
	 * @param excludeMsgBeingSent Jangan pilih pesan yang sedang dikirim jika true.
	 * @return Pesan yang dipilih untuk di-drop atau null jika tidak ada.
	 */
	protected Message selectMessageToDrop(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		Message messageToDrop = null;

		// Buat daftar pesan yang bisa di-drop (tidak sedang dikirim jika
		// excludeMsgBeingSent true)
		List<Message> droppableMessages = new ArrayList<>();
		for (Message m : messages) {
			if (!(excludeMsgBeingSent && isSending(m.getId()))) {
				droppableMessages.add(m);
			}
		}

		if (droppableMessages.isEmpty()) {
			return null; // Tidak ada pesan yang bisa di-drop
		}

		switch (queueingPolicyEnum) {
			case FIFO_DROP:
				// FIFO: Drop yang paling tua (waktu terima paling kecil)
				messageToDrop = Collections.min(droppableMessages,
						Comparator.comparingDouble(Message::getReceiveTime));
				break;
			case MOFO:
				// MOFO: Drop yang paling sering diforward - LOGIKA MOFO - DITAMBAHKAN
				// Menggunakan map forwardedCounts
				int maxForwardCount = -1; // Cari nilai maksimum
				for (Message m : droppableMessages) {
					int count = forwardedCounts.getOrDefault(m.getId(), 0);
					if (count > maxForwardCount) {
						maxForwardCount = count;
					}
				}
				List<Message> mofoCandidates = new ArrayList<>(); // Kumpulkan kandidat dengan nilai maksimum
				for (Message m : droppableMessages) {
					if (forwardedCounts.getOrDefault(m.getId(), 0) == maxForwardCount) {
						mofoCandidates.add(m);
					}
				}
				// Tie-breaker (menggunakan FIFO)
				if (!mofoCandidates.isEmpty()) { // Check if not empty
					messageToDrop = Collections.min(mofoCandidates,
							Comparator.comparingDouble(Message::getReceiveTime));
				} else { // Fallback if somehow list is empty
					messageToDrop = Collections.min(droppableMessages,
							Comparator.comparingDouble(Message::getReceiveTime));
				}
				break;
			case SHLI:
				// SHLI: Drop yang sisa TTL-nya paling sedikit - LOGIKA SHLI - DITAMBAHKAN
				messageToDrop = Collections.min(droppableMessages, Comparator.comparingInt(Message::getTtl));
				break;
			case LEPR:
				// LEPR: Drop P terendah (dari node saat ini ke tujuan) - LOGIKA LEPR -
				// DITAMBAHKAN
				// Menggunakan getPredFor
				double minPred = Double.MAX_VALUE; // Cari nilai minimum
				for (Message m : droppableMessages) {
					double pred = getPredFor(m.getTo());
					if (pred < minPred) {
						minPred = pred;
					}
				}
				List<Message> leprCandidates = new ArrayList<>(); // Kumpulkan kandidat
				for (Message m : droppableMessages) {
					// HATI-HATI perbandingan double == minPred, mungkin perlu toleransi epsilon
					// jika tidak ingin tie-breaker sering dipanggil
					if (getPredFor(m.getTo()) == minPred) {
						leprCandidates.add(m);
					}
				}
				// Tie-breaker (menggunakan FIFO)
				if (!leprCandidates.isEmpty()) {
					messageToDrop = Collections.min(leprCandidates,
							Comparator.comparingDouble(Message::getReceiveTime));
				} else {
					messageToDrop = Collections.min(droppableMessages,
							Comparator.comparingDouble(Message::getReceiveTime));
				}
				break;
			case MOPR:
				// MOPR: Drop FP tertinggi - LOGIKA MOPR - DITAMBAHKAN
				// Menggunakan map forwardProgresses
				double maxFP = Double.MIN_VALUE; // Cari nilai maksimum
				for (Message m : droppableMessages) {
					double fp = forwardProgresses.getOrDefault(m.getId(), 0.0);
					if (fp > maxFP) {
						maxFP = fp;
					}
				}
				List<Message> moprCandidates = new ArrayList<>(); // Kumpulkan kandidat
				for (Message m : droppableMessages) {
					// HATI-HATI perbandingan double == maxFP
					if (forwardProgresses.getOrDefault(m.getId(), 0.0) == maxFP) {
						moprCandidates.add(m);
					}
				}
				// Tie-breaker (menggunakan FIFO)
				if (!moprCandidates.isEmpty()) {
					messageToDrop = Collections.min(moprCandidates,
							Comparator.comparingDouble(Message::getReceiveTime));
				} else {
					messageToDrop = Collections.min(droppableMessages,
							Comparator.comparingDouble(Message::getReceiveTime));
				}
				break;
			default:
				throw new SimError("Unknown queueing policy " + queueingPolicyEnum);
		}

		// Pengecekan keamanan (seharusnya tidak null)
		if (messageToDrop == null) {
			throw new SimError("Queueing policy " + queueingPolicyEnum
					+ " failed to select a message to drop from a non-empty droppable list.");
		}

		return messageToDrop;
	}
	// --- END: Implementasi Queueing Policy (Drop Logic) --- //
}