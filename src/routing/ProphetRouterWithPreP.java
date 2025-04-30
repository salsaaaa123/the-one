/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 *
 * An Efficient Routing Protocol Using the History of Delivery Predictability in Opportunistic Networks
 */
package routing;

import core.*;

import java.util.*;

/**
 * Implementation of PRoPHET router as described in
 * <I>Probabilistic routing in intermittently connected networks</I> by
 * Anders Lindgren et al.
 */
public class ProphetRouterWithPreP extends ActiveRouter {
    /**
     * delivery predictability initialization constant
     */
    public static final double P_INIT = 0.75;
    /**
     * delivery predictability transitivity scaling constant default value
     */
    public static final double DEFAULT_BETA = 0.25;
    /**
     * delivery predictability aging constant
     */
    public static final double GAMMA = 0.98;

    /**
     * Prophet router's setting namespace ({@value})
     */
    public static final String PROPHET_NS = "ProphetRouter";
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
     * the value of nrof seconds in time unit -setting
     */
    private int secondsInTimeUnit;
    /**
     * value of beta setting
     */
    private double beta;

    /**
     * delivery predictabilities
     */
    private Map<DTNHost, Double> preds;
    /**
     * last delivery predictability update (sim)time
     */
    private double lastAgeUpdate;

    private Map<DTNHost, Double> prevPreds;

    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     *
     * @param s The settings object
     */
    public ProphetRouterWithPreP(Settings s) {
        super(s);
        Settings prophetSettings = new Settings(PROPHET_NS);
        secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
        if (prophetSettings.contains(BETA_S)) {
            beta = prophetSettings.getDouble(BETA_S);
        } else {
            beta = DEFAULT_BETA;
        }

        initPreds();
    }

    /**
     * Copyconstructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected ProphetRouterWithPreP(ProphetRouterWithPreP r) {
        super(r);
        this.secondsInTimeUnit = r.secondsInTimeUnit;
        this.beta = r.beta;
        initPreds();
    }

    /**
     * Initializes predictability hash
     */
    private void initPreds() {
        this.preds = new HashMap<DTNHost, Double>();
        this.prevPreds = new HashMap<>();
    }


    @Override
    public int receiveMessage(Message m, DTNHost from) {
        // Ambil dan simpan prediktabilitas saat ini SEBELUM memproses lebih lanjut
        // Ini menangkap P(penerima, tujuan) pada saat penerimaan.
        DTNHost destination = m.getTo();
        double currentPredForDest = getPredFor(destination); // Mengambil P(A,D) penerima

        int receptionStatus = super.receiveMessage(m, from);

        // Jika pesan berhasil diterima (bukan duplikat, buffer cukup)
        if (receptionStatus == RCV_OK) {
            // Simpan prediktabilitas SAAT PENERIMAAN sebagai preP untuk tujuan ini.
            // Ini meng-overwrite preP sebelumnya jika ada pesan lain untuk tujuan yg sama diterima.
            this.prevPreds.put(destination, currentPredForDest); // Menyimpan P(A,D) sebagai preP(A,D)
        }

        return receptionStatus;
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
        assert otherRouter instanceof ProphetRouterWithPreP : "PRoPHET only works " +
                " with other routers of same type";

        double pForHost = getPredFor(host); // P(a,b)
        Map<DTNHost, Double> othersPreds =
                ((ProphetRouterWithPreP) otherRouter).getDeliveryPreds();

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

        if (timeDiff == 0) {
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

        tryOtherMessages();
    }

    /**
     * Returns the previous prediction preP(this, host) value for a destination,
     * or null if no preP value is recorded (e.g., message originated here).
     *
     * @param destinationHost The destination host to look the preP for
     * @return the preP value, or null if not set
     */
    public Double getPrevPredFor(DTNHost destinationHost) {
        // prevPreds tidak di-aging, langsung ambil dari map.
        return this.prevPreds.get(destinationHost);
    }

    /**
     * Tries to send all other messages to all connected hosts ordered by
     * their delivery probability
     *
     * @return The return value of {@link #tryMessagesForConnected(List)}
     */
    private Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages =
                new ArrayList<Tuple<Message, Connection>>();

        Collection<Message> msgCollection = getMessageCollection();

		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
//        for (Connection con : getHost()) {
        for (Connection con : getConnections()) {
            DTNHost other = con.getOtherNode(getHost());
            MessageRouter othRouterUncasted = other.getRouter();
            if (!(othRouterUncasted instanceof ProphetRouterWithPreP)) {
                continue;
            }
            ProphetRouterWithPreP othRouter = (ProphetRouterWithPreP) other.getRouter();

            if (othRouter.isTransferring()) {
                continue; // skip hosts that are transferring
            }

            for (Message m : msgCollection) {
                DTNHost destinationD = m.getTo(); // Tujuan akhir pesan ( D)
//                if (m.getTo() == other) {
//                    continue;
//                }
                // Jangan proses jika pesan untuk node B itu sendiri (sudah dihandle exchangeDeliverableMessages)
                if (destinationD == other) {
                    continue;
                }
                // Jangan proses jika node B sudah punya pesan ini
                if (othRouter.hasMessage(m.getId())) {
                    continue; // skip messages that the other one has
                }
                // --- Terapkan Logika Forwarding Prophet + preP ---
                double p_ad = getPredFor(destinationD);      // P(A, D) - A = this
                double p_bd = othRouter.getPredFor(destinationD); // P(B, D) - B = nodeB

                // 1. Cek Kondisi Dasar Prophet:(PB,D) > P(A,D)
                if (p_bd > p_ad) {
                    // 2. cek Kondisi PreP
                    Double preP_ad = getPrevPredFor(destinationD);// Ambil preP(A, D)
                    boolean forwardThisMessage = false;

                    if (preP_ad == null) {
                        // preP(A,D) tidak ada (misal: pesan berasal dari A, hop pertama)
                        // Forward diizinkan hanya berdasarkan P(B,D) > P(A,D) ( flowchart=YA )
                        forwardThisMessage = true;
                    } else {
                        // 3. preP(A,D) ada. Cek kondisi tambahan: P(B,D) >= preP(A,D)
                        if (p_bd >= preP_ad) {
                            forwardThisMessage = true; // Boleh forward (flowchart: Ya)
                        } else {
                            // Kondisi P(B,D) >= preP(A,D) gagal
                            forwardThisMessage = false;// Tidak boleh forward (flowchart: Tidak)
                        }
                    }
                    if (forwardThisMessage) {
                        messages.add(new Tuple<Message, Connection>(m, con));
                    }

                }
            }
        }

        if (messages.size() == 0) {
            return null;
        }

        // sort the message-connection tuples
        Collections.sort(messages, new TupleComparator());
        return tryMessagesForConnected(messages);    // try to send messages
    }

    /**
     * Comparator for Message-Connection-Tuples that orders the tuples by
     * their delivery probability by the host on the other side of the
     * connection (GRTRMax)
     */
    private class TupleComparator implements Comparator
            <Tuple<Message, Connection>> {

        public int compare(Tuple<Message, Connection> tuple1,
                           Tuple<Message, Connection> tuple2) {
            // delivery probability of tuple1's message with tuple1's connection
            double p1 = ((ProphetRouterWithPreP) tuple1.getValue().
                    getOtherNode(getHost()).getRouter()).getPredFor(
                    tuple1.getKey().getTo());
            // -"- tuple2...
            double p2 = ((ProphetRouterWithPreP) tuple2.getValue().
                    getOtherNode(getHost()).getRouter()).getPredFor(
                    tuple2.getKey().getTo());

            // bigger probability should come first
            if (p2 - p1 == 0) {
                /* equal probabilities -> let queue mode decide */
                return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
            } else if (p2 - p1 < 0) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    @Override
    public RoutingInfo getRoutingInfo() {
        ageDeliveryPreds();
        RoutingInfo top = super.getRoutingInfo();
        RoutingInfo ri = new RoutingInfo(preds.size() +
                " delivery prediction(s)");

        for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
            DTNHost host = e.getKey();
            Double value = e.getValue();

            ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f",
                    host, value)));
        }

        top.addMoreInfo(ri);
        return top;
    }

    @Override
    public MessageRouter replicate() {
        ProphetRouterWithPreP r = new ProphetRouterWithPreP(this);
        return r;
    }

}
