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
    public static final String PROPHET_NS = "ProphetRouterWithPreP";
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

    // @Override
    // public int receiveMessage(Message m, DTNHost from) {
    // // Ambil dan simpan prediktabilitas saat ini SEBELUM memproses lebih lanjut
    // // Ini menangkap P(penerima, tujuan) pada saat penerimaan.
    // DTNHost destination = m.getTo();
    // double currentPredForDest = getPredFor(destination); // Mengambil P(A,D)
    // penerima
    //
    // int receptionStatus = super.receiveMessage(m, from);
    //
    // // Jika pesan berhasil diterima (bukan duplikat, buffer cukup)
    // if (receptionStatus == RCV_OK) {
    // // Simpan prediktabilitas SAAT PENERIMAAN sebagai preP untuk tujuan ini.
    // // Ini meng-overwrite preP sebelumnya jika ada pesan lain untuk tujuan yg
    // sama diterima.
    // this.prevPreds.put(destination, currentPredForDest); // Menyimpan P(A,D)
    // sebagai preP(A,D)
    // }
    //
    // return receptionStatus;
    // }

    //
    // @Override
    // public Message messageTransferred(String id, DTNHost from) {
    // Message m = super.messageTransferred(id, from);
    //
    //
    // if (m != null) {
    // DTNHost destination = m.getTo();
    // // Ambil prediktabilitas SAAT INI (setelah transfer selesai)
    // // Pastikan aging sudah diperhitungkan jika getPredFor memanggilnya
    // double predAtTransferEnd = getPredFor(destination);
    //
    // // Simpan nilai ini sebagai preP untuk tujuan tersebut
    // this.prevPreds.put(destination, predAtTransferEnd);
    // }
    //
    // return m;
    // }

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
        Map<DTNHost, Double> othersPreds = ((ProphetRouterWithPreP) otherRouter).getDeliveryPreds();

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
     * @param //destinationHost The destination host to look the preP for
     * @return the preP value, or null if not set
     */
    public Double getPrevPredFor(DTNHost destinationHost) {
        // prevPreds tidak di-aging, langsung ambil dari map.
        return this.prevPreds.get(destinationHost);
    }

    /**
     * Merekam prediktabilitas pengiriman saat ini ke tujuan yang diberikan
     * sebagai nilai preP baru untuk tujuan tersebut.
     * Metode ini dipanggil oleh router PENGIRIM.
     *
     * @param destination Tujuan akhir dari pesan yang akan diteruskan.
     */
    public void recordCurrentPredictabilityAsPreP(DTNHost destination) {
        double currentPredForDest = getPredFor(destination);
        this.prevPreds.put(destination, currentPredForDest);
    }

    /**
     * Tries to send all other messages to all connected hosts ordered by
     * their delivery probability
     *
     * @return The return value of {@link #tryMessagesForConnected(List)}
     */
    // private Tuple<Message, Connection> tryOtherMessages() {
    // List<Tuple<Message, Connection>> messages =
    // new ArrayList<Tuple<Message, Connection>>();
    //
    // Collection<Message> msgCollection = getMessageCollection();
    //
    // /* for all connected hosts collect all messages that have a higher
    // probability of delivery by the other host */
    //// for (Connection con : getHost()) {
    // for (Connection con : getConnections()) {
    // DTNHost other = con.getOtherNode(getHost());
    //// MessageRouter othRouterUncasted = other.getRouter();
    //// if (!(othRouterUncasted instanceof ProphetRouterWithPreP)) {
    //// continue;
    //// }
    // ProphetRouterWithPreP othRouter = (ProphetRouterWithPreP) other.getRouter();
    //
    // if (othRouter.isTransferring()) {
    // continue; // skip hosts that are transferring
    // }
    //
    // for (Message m : msgCollection) {
    // DTNHost destinationD = m.getTo(); // Tujuan akhir pesan ( D)
    //// if (m.getTo() == other) {
    //// continue;
    //// }
    // // Jangan proses jika pesan untuk node B itu sendiri
    // if (destinationD == other) {
    // continue;
    // }
    // // Jangan proses jika node B sudah punya pesan ini
    // if (othRouter.hasMessage(m.getId())) {
    // continue; // skip messages that the other one has
    // }
    // // --- MULAI LOGIKA INTI FLOWCHART (untuk B != D) ---
    //
    // // Dapatkan P(A, D): Prediktabilitas node ini (A) ke tujuan D.
    // double p_ad = getPredFor(destinationD);
    // // Dapatkan P(B, D): Prediktabilitas node kontak (B) ke tujuan D.
    // double p_bd = othRouter.getPredFor(destinationD);
    //
    // /*
    // * Implementasi Flowchart Langkah 5: Apakah P(B,D) > P(A,D) ?
    // * Cek apakah Node B saat ini lebih baik daripada Node A untuk mencapai D.
    // */
    // if (p_bd > p_ad) {
    // /*
    // * Jika Ya (P(B,D) > P(A,D)), Node B berpotensi jadi penerus.
    // * Sekarang terapkan syarat tambahan dari protokol ini (cek preP).
    // */
    //
    // // Dapatkan preP(A, D): Prediktabilitas historis Node A ke D
    // // yang disimpan saat Node A menerima pesan ini (atau pesan lain untuk D).
    // Double preP_ad = getPrevPredFor(destinationD);
    //
    // // Flag untuk menandai apakah pesan ini akhirnya boleh diforward atau tidak.
    // boolean forwardThisMessage = false;
    //
    // /*
    // * Implementasi Flowchart Langkah 6: Apakah Node A punya informasi preP(A,D)?
    // * Cek apakah ada catatan historis untuk tujuan D.
    // */
    // if (preP_ad == null) {
    // /*
    // * Jika Tidak (preP_ad tidak ada): Ini biasanya berarti pesan ini
    // * berasal dari Node A sendiri. Tidak ada pembanding historis.
    // * Karena syarat P(B,D) > P(A,D) sudah terpenuhi, kita putuskan
    // * untuk meneruskan pesan. Flowchart -> Ya (Boleh Forward).
    // */
    // forwardThisMessage = true;
    // } else {
    // /*
    // * Jika Ya (preP_ad ada): Ada standar historis. Kita perlu
    // * membandingkan performa Node B saat ini dengan standar tersebut.
    // * Lanjut ke langkah berikutnya.
    // */
    //
    // /*
    // * Implementasi Flowchart Langkah 7: Apakah P(B,D) >= preP(A,D) ?
    // * Cek apakah Node B saat ini setidaknya sama baiknya atau lebih baik
    // * daripada 'standar' historis Node A (preP_ad).
    // */
    // if (p_bd >= preP_ad) {
    // /*
    // * Jika Ya: Node B memenuhi kedua syarat (lebih baik dari A saat ini
    // * DAN lebih baik/sama dengan standar historis).
    // * Flowchart -> Ya (Boleh Forward).
    // */
    // forwardThisMessage = true;
    // } else {
    // /*
    // * Jika Tidak: Node B mungkin lebih baik dari A sekarang, tapi
    // * tidak cukup baik dibandingkan standar historis (preP).
    // * Flowchart -> Tidak (Jangan Forward).
    // * Flag forwardThisMessage tetap false.
    // */
    // forwardThisMessage = false; // Eksplisit (opsional, sudah false by default)
    // }
    // } // Akhir dari blok cek preP_ad
    //
    // /*
    // * Setelah melalui semua cek logika flowchart, jika flag forwardThisMessage
    // * adalah true, tambahkan pesan dan koneksi ini ke daftar kandidat
    // * yang akan diurutkan dan dicoba kirim.
    // */
    // if (forwardThisMessage) {
    // messages.add(new Tuple<Message, Connection>(m, con));
    // }
    //
    // } // End dari blok if (p_bd > p_ad)
    // } // End loop 'for (Message m ...)'
    // } // End Connection con ...)'
    //
    // // Jika tidak ada pesan yang memenuhi syarat untuk diteruskan, selesai.
    // if (messages.size() == 0) {
    // return null;
    // }
    //
    // /*
    // * Ada pesan kandidat. Urutkan pesan-pesan ini berdasarkan kriteria
    // * di TupleComparator (prioritas utama: P(B,D) tertinggi,
    // * prioritas kedua: ID pesan jika P(B,D) sama).
    // */
    // Collections.sort(messages, new TupleComparator());
    //
    // /*
    // * Coba kirim pesan dari daftar yang sudah terurut melalui koneksi yang
    // sesuai.
    // * Metode tryMessagesForConnected akan mencoba mengirim pesan pertama,
    // * jika berhasil ia akan mengembalikan Tuple-nya, jika gagal (misal koneksi
    // sibuk),
    // * ia akan mencoba pesan berikutnya, dst.
    // */
    // return tryMessagesForConnected(messages);
    // }

    private Tuple<Message, Connection> tryOtherMessages() {
        List<Tuple<Message, Connection>> messages = new ArrayList<>(); // Gunakan diamond operator
        Collection<Message> msgCollection = getMessageCollection();

        for (Connection con : getHost().getConnections()) {
            DTNHost other = con.getOtherNode(getHost()); // Node B
            // Pastikan casting aman
            if (!(other.getRouter() instanceof ProphetRouterWithPreP))
                continue;
            ProphetRouterWithPreP othRouter = (ProphetRouterWithPreP) other.getRouter();

            // Lewati jika node lain sedang sibuk
            if (othRouter.isTransferring()) {
                continue;
            }

            for (Message m : msgCollection) {
                DTNHost destinationD = m.getTo(); // Node D (tujuan akhir)

                // Lewati jika node lain adalah tujuan akhir
                if (destinationD == other) {
                    continue;
                }
                // Lewati jika node lain sudah punya pesan ini
                if (othRouter.hasMessage(m.getId())) {
                    continue;
                }

                // Dapatkan prediktabilitas
                double p_ad = getPredFor(destinationD); // P(A, D)
                double p_bd = othRouter.getPredFor(destinationD); // P(B, D)

                // Logika Flowchart Inti
                if (p_bd > p_ad) { // Langkah 5: P(B,D) > P(A,D)?
                    Double preP_ad = getPrevPredFor(destinationD); // Ambil preP(A, D)
                    boolean forwardThisMessage = false;

                    if (preP_ad == null) { // Langkah 6: Ada info preP(A,D)? Tidak -> Boleh forward
                        forwardThisMessage = true;
                    } else { // Ada info preP(A,D)
                        if (p_bd >= preP_ad) { // Langkah 7: P(B,D) >= preP(A,D)? Ya -> Boleh forward
                            forwardThisMessage = true;
                        }
                        // Jika Langkah 7 Tidak, forwardThisMessage tetap false
                    }

                    // Jika diputuskan untuk forward berdasarkan logika di atas
                    if (forwardThisMessage) {
                        // 1. Tambahkan ke daftar kandidat
                        messages.add(new Tuple<>(m, con)); // Gunakan diamond operator

                        // 2. Panggil metode di Node B untuk merekam preP-nya
                        // (Pendekatan memicu dari pengirim)
                        othRouter.recordCurrentPredictabilityAsPreP(destinationD);
                    }
                } // End if (p_bd > p_ad)
            } // End for (Message m ...)
        } // End for (Connection con ...)

        // Jika tidak ada kandidat pesan, keluar
        if (messages.isEmpty()) { // Gunakan isEmpty()
            return null;
        }

        // Urutkan kandidat pesan (pastikan TupleComparator sudah diperbaiki)
        Collections.sort(messages, new TupleComparator());

        // Coba kirim pesan sesuai urutan
        return tryMessagesForConnected(messages);
    }

    /**
     * Comparator for Message-Connection-Tuples that orders the tuples by
     * their delivery probability by the host on the other side of the
     * connection (GRTRMax)
     */
    private class TupleComparator implements Comparator<Tuple<Message, Connection>> {

        public int compare(Tuple<Message, Connection> tuple1,
                Tuple<Message, Connection> tuple2) {
            // delivery probability of tuple1's message with tuple1's connection
            double p1 = ((ProphetRouterWithPreP) tuple1.getValue().getOtherNode(getHost()).getRouter()).getPredFor(
                    tuple1.getKey().getTo());
            // -"- tuple2...
            double p2 = ((ProphetRouterWithPreP) tuple2.getValue().getOtherNode(getHost()).getRouter()).getPredFor(
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
