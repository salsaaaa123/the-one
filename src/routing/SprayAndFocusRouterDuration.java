package routing;

import core.*;
import routing.community.Duration; // Import Duration

import java.util.*;

/**
 * An implementation of Spray and Focus DTN routing as described in
 * <em>Spray and Focus: Efficient Mobility-Assisted Routing for Heterogeneous
 * and Correlated Mobility</em> by Thrasyvoulos Spyropoulos et al.
 *
 * **Modifikasi:** Menggunakan Duration langsung, menghapus EncounterInfo,
 * mencatat waktu interaksi saat koneksi DOWN.
 *
 * @author PJ Dillon, University of Pittsburgh
 */
public class SprayAndFocusRouterDuration extends ActiveRouter {
	/**
	 * SprayAndFocus router's settings name space ({@value})
	 */
	public static final String SPRAYANDFOCUS_NS = "SprayAndFocusRouterDuration";
	/**
	 * identifier for the initial number of copies setting ({@value})
	 */
	public static final String NROF_COPIES_S = "nrofCopies";
	/**
	 * identifier for the difference in timer values needed to forward on a message
	 * copy
	 */
	public static final String TIMER_THRESHOLD_S = "transitivityTimerThreshold";
	/**
	 * Message property key for the remaining available copies of a message
	 */
	public static final String MSG_COUNT_PROP = "SprayAndFocusRouterDuration.copies";
	/**
	 * Message property key for summary vector messages exchanged between direct
	 * peers
	 */
	public static final String SUMMARY_XCHG_PROP = "SprayAndFocusRouterDuration.protoXchg";

	protected static final String SUMMARY_XCHG_IDPREFIX = "summary";
	protected static final double defaultTransitivityThreshold = 60.0;
	protected static int protocolMsgIdx = 0;

	protected int initialNrofCopies;
	protected double transitivityTimerThreshold;

	/**
	 * connHistory Map yang menyimpan informasi kontak dengan peer.
	 * Kunci = DTNHost (peer)
	 * Nilai = List<Duration> yang berisi daftar durasi kontak
	 */
	protected Map<DTNHost, List<Duration>> connHistory;

	/**
	 * startTime Map yang menyimpan waktu mulai koneksi dengan peer.
	 * Kunci = DTNHost (peer)
	 * Nilai = Waktu Mulai Kontak
	 */
	protected Map<DTNHost, Double> startTime;

	public SprayAndFocusRouterDuration(Settings s) {
		super(s);
		Settings snf = new Settings(SPRAYANDFOCUS_NS);
		initialNrofCopies = snf.getInt(NROF_COPIES_S);

		if (snf.contains(TIMER_THRESHOLD_S))
			transitivityTimerThreshold = snf.getDouble(TIMER_THRESHOLD_S);
		else
			transitivityTimerThreshold = defaultTransitivityThreshold;

		connHistory = new HashMap<DTNHost, List<Duration>>();
		startTime = new HashMap<DTNHost, Double>();
	}

	/**
	 * Copy Constructor.
	 *
	 * @param r The router from which settings should be copied
	 */
	public SprayAndFocusRouterDuration(SprayAndFocusRouterDuration r) {
		super(r);
		this.initialNrofCopies = r.initialNrofCopies;

		connHistory = new HashMap<DTNHost, List<Duration>>();
		startTime = new HashMap<DTNHost, Double>();
	}

	@Override
	public MessageRouter replicate() {
		return new SprayAndFocusRouterDuration(this);
	}

	/**
	 * Called whenever a connection goes up or comes down.
	 */
	@Override
	public void changedConnection(Connection con) {
		super.changedConnection(con);

		DTNHost thisHost = getHost();
		DTNHost peer = con.getOtherNode(thisHost);
		double currentTime = SimClock.getTime();

		if (con.isUp()) {
			// Do Nothing
			startTime.put(peer, currentTime);
		} else {
			// Catat waktu interaksi SAAT KONEKSI TURUN
			updateConnHistory(peer, currentTime);
			startTime.remove(peer); // Hapus waktu mulai
		}

		/*
		 * For this simulator, we just need a way to give the other node in this
		 * connection
		 * access to the peers we recently encountered; so we duplicate the connHistory
		 * Map and attach it to a message.
		 */
		int msgSize = connHistory.size() * 64 + getMessageCollection().size() * 8;
		Message newMsg = new Message(thisHost, peer, SUMMARY_XCHG_IDPREFIX + protocolMsgIdx++, msgSize);
		newMsg.addProperty(SUMMARY_XCHG_PROP, new HashMap<DTNHost, List<Duration>>(connHistory));

		createNewMessage(newMsg);
	}

	private void updateConnHistory(DTNHost peer, double currentTime) {
		SprayAndFocusRouterDuration myRouter = (SprayAndFocusRouterDuration) this.getHost().getRouter();
		// Konversi Collection ke List
		List<Message> messages = new ArrayList<>(myRouter.getMessageCollection());
		DTNHost destination = null;

		// Cari pesan yang relevan
		for (Message msg : messages) {
			if (msg.getTo().equals(peer)) { // Jika pesan ini akan dikirim ke peer
				destination = msg.getTo();
				break; // Asumsikan hanya satu pesan yang relevan untuk contoh ini
			}
			if (msg.getFrom().equals(peer)) { // Jika pesan ini diterima dari peer
				destination = msg.getTo();
				break; // Asumsikan hanya satu pesan yang relevan untuk contoh ini
			}
		}

		if (destination == null) {
			// Jika tidak ada pesan yang terkait, jangan lakukan apa-apa
			return;
		}

		// Dapatkan waktu mulai Koneksi
		Double connectionStartTime = startTime.get(peer);
		if (connectionStartTime == null) {
			// Jika tidak ada waktu mulai, mungkin ada kesalahan
			System.err.println("Error: No start time found for peer " + peer);
			return;
		}

		// Buat Durasi Baru
		Duration newDuration = new Duration(connectionStartTime, currentTime);

		// Tambahkan Durasi baru
		if (!connHistory.containsKey(destination)) {
			connHistory.put(destination, new ArrayList<>());
		}
		connHistory.get(destination).add(newDuration);
	}

	@Override
	public boolean createNewMessage(Message m) {
		makeRoomForNewMessage(m.getSize());

		m.addProperty(MSG_COUNT_PROP, Integer.valueOf(initialNrofCopies));
		addToMessages(m, true);
		return true;
	}

	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);

		/*
		 * Here we update our last encounter times based on the information sent
		 * from our peer.
		 */
		Map<DTNHost, List<Duration>> peerConnHistory = null;
		Object prop = m.getProperty(SUMMARY_XCHG_PROP);
		if (prop instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			Map<DTNHost, List<Duration>> tmp = (Map<DTNHost, List<Duration>>) prop;
			peerConnHistory = tmp;
		}

		// Pastikan peerConnHistory tidak null dan pesan BUKAN pesan deliverable
		if (peerConnHistory != null && !isDeliveredMessage(m)) {
			double distTo = getHost().getLocation().distance(from.getLocation());
			double speed = from.getPath() == null ? 0 : from.getPath().getSpeed();

			if (speed == 0.0)
				return m;

			double timediff = distTo / speed;

			/*
			 * We save the peer info for the utility based forwarding decisions, which are
			 * implemented in update()
			 */
			// neighborConnHistory.put(from, peerConnHistory);

			for (Map.Entry<DTNHost, List<Duration>> entry : peerConnHistory.entrySet()) {
				DTNHost h = entry.getKey();
				if (h == getHost())
					continue;

				List<Duration> peerDurations = entry.getValue();
				List<Duration> myDurations = connHistory.get(h);

				// Jika belum pernah ketemu h, buat info baru
				if (!connHistory.containsKey(h)) {
					connHistory.put(h, new ArrayList<Duration>());
					myDurations = connHistory.get(h);
				}
				// Iterasi semua durasi Peer dan tambahkan durasi yang sudah dikurangi timediff

				for (Duration d : peerDurations) {
					myDurations.add(new Duration(d.start - timediff, d.end - timediff));
				}
			}
		}

		// Normal message beyond here
		Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
		nrofCopies = (int) Math.ceil(nrofCopies / 2.0);
		m.updateProperty(MSG_COUNT_PROP, nrofCopies);

		return m;
	}

	@Override
	protected void transferDone(Connection con) {
		Integer nrofCopies;
		String msgId = con.getMessage().getId();
		/* get this router's copy of the message */
		Message msg = getMessage(msgId);

		if (msg == null) { // message has been dropped from the buffer after..
			return; // ..start of transfer -> no need to reduce amount of copies
		}

		if (msg.getProperty(SUMMARY_XCHG_PROP) != null) {
			deleteMessage(msgId, false);
			return;
		}

		/*
		 * reduce the amount of copies left. If the number of copies was at 1 and
		 * we apparently just transferred the msg (focus phase), then we should
		 * delete it.
		 */
		nrofCopies = (Integer) msg.getProperty(MSG_COUNT_PROP);
		if (nrofCopies > 1)
			nrofCopies /= 2;
		else
			deleteMessage(msgId, false);

		msg.updateProperty(MSG_COUNT_PROP, nrofCopies);
	}

	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() || isTransferring()) {
			return; // nothing to transfer or is currently transferring
		}

		/* try messages that could be delivered to final recipient */
		if (exchangeDeliverableMessages() != null) {
			return;
		}

		List<Message> spraylist = new ArrayList<Message>();
		List<Tuple<Message, Connection>> focuslist = new LinkedList<Tuple<Message, Connection>>();

		for (Message m : getMessageCollection()) {
			if (m.getProperty(SUMMARY_XCHG_PROP) != null)
				continue;

			Integer nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);
			assert nrofCopies != null : "SnF message " + m + " didn't have " +
					"nrof copies property!";
			if (nrofCopies > 1) {
				spraylist.add(m);
			} else {
				/*
				 * Here we implement the single copy utility-based forwarding scheme.
				 * The utility function is the last encounter time of the msg's
				 * destination node. If our peer has a newer time (beyond the threshold),
				 * we forward the msg on to it.
				 */
				DTNHost dest = m.getTo();
				Connection toSend = null;
				double minPeerAvgIntercontactTime = Double.MAX_VALUE;

				// Get the timestamp of the last time this Host saw the destination
				double thisAvgIntercontactTime = calculateAvgIntercontactTime(dest);

				for (Connection c : getHost()) {
					DTNHost peer = c.getOtherNode(getHost());
					// Pastikan destinasi ada di connHistory
					if (!connHistory.containsKey(dest))
						continue;

					// Pastikan peer juga punya info tentang destinasi
					if (!connHistory.containsKey(peer))
						continue;
					double peerAvgIntercontactTime = calculateAvgIntercontactTime(dest);

					/*
					 * We need to pick only one peer to send the copy on to; so lets find the
					 * one with the lowest average intercontact time.
					 */

					if (peerAvgIntercontactTime < minPeerAvgIntercontactTime) {
						toSend = c;
						minPeerAvgIntercontactTime = peerAvgIntercontactTime;
					}

				}
				// Forward jika peer memiliki AvgIntercontactTime lebih kecil dari thisHost
				if (toSend != null && minPeerAvgIntercontactTime < thisAvgIntercontactTime) {
					focuslist.add(new Tuple<Message, Connection>(m, toSend));
				}
			}
		}

		// arbitrarily favor spraying
		if (tryMessagesToAllConnections(spraylist) == null) {
			if (tryMessagesForConnected(focuslist) != null) {
			}
		}
	}

	/**
	 * Method to try to send messages for all connections
	 * 
	 * @param messages list of message to send
	 * @return connections that started a transfer or null if no connection accepted
	 *         a message.
	 */
	protected Connection tryMessagesToAllConnections(List<Message> messages) {
		List<Connection> connections = getHost().getConnections();
		if (connections.size() == 0 || this.getNrofMessages() == 0) {
			return null;
		}

		return tryMessagesToConnections(messages, connections);
	}

	/**
	 * Calculates the average intercontact time for a destination.
	 *
	 * @param dest The destination host.
	 * @return The average intercontact time, or Double.MAX_VALUE if not enough
	 *         data.
	 */
	protected double calculateAvgIntercontactTime(DTNHost dest) {
		List<Duration> durations = connHistory.get(dest);
		if (durations == null) {
			return Double.MAX_VALUE;
		}
		return calculateAvgIntercontactTime(durations);
	}

	/**
	 * Calculates the average intercontact time.
	 *
	 * @param durations The durations for the destination.
	 * @return The average intercontact time, or Double.MAX_VALUE if not enough
	 *         data.
	 */
	protected double calculateAvgIntercontactTime(List<Duration> durations) {
		if (durations == null || durations.size() < 2) {
			return Double.MAX_VALUE; // Not enough data to calculate
		}

		double totalIntercontactTime = 0;
		double previousEndTime = 0;
		boolean firstContact = true;
		double startTime = 0;

		for (Duration duration : durations) {
			if (firstContact) {
				startTime = duration.start;
				firstContact = false;
			} else {
				totalIntercontactTime += duration.start - previousEndTime;
			}
			previousEndTime = duration.end;
		}

		if (durations.size() > 1) {
			return totalIntercontactTime / (durations.size() - 1);
		} else {
			return Double.MAX_VALUE; // Not enough data to calculate
		}
	}
}