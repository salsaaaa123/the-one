
/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

package routing.people;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.Duration;

import java.util.*;

public class PeopleRankDecisionEngine implements RoutingDecisionEngine, RankingNodeValue {
    public static final String PEOPLERANK_NS = "PeopleRankDecisionEngine";
    /**
     * Inisialisasi variabel faktor redaman yang digunakan - setting id
     */
    public static final String DUMPING_FACTOR_SETTING = "dumpingFactor";
    public static final String TRESHOLD_SETTING = "threshold";

    /**
     * Map untuk menyimpan nilai PeopleRank untuk setiap host beserta jumlah teman
     */
    protected Map<DTNHost, Tuple<Double, Integer>> PeR; // PeR(Ni) : PeopleRank dari node i
    protected Map<DTNHost, List<Duration>> Gs;
    protected Map<DTNHost, Double> startTimes; // Waktu mulai koneksi

    // Faktor redaman dan threshold
    protected double d; // Faktor redaman (damping factor)
    protected double threshold; // Ambang batas waktu koneksi

    private DTNHost thisHost;

    /**
     * Konstruktor untuk PeopleRank berdasarkan pengaturan yang ditentukan.
     *
     * @param s Objek pengaturan yang berisi parameter konfigurasi
     */
    public PeopleRankDecisionEngine(Settings s) {
        Settings prSettings = new Settings(PEOPLERANK_NS);
        if (prSettings.contains(DUMPING_FACTOR_SETTING)) {
            d = prSettings.getDouble(DUMPING_FACTOR_SETTING);
            System.out.println("d: " + d); // debug
        } else {
            this.d = 0.85;
        }
        if (prSettings.contains(TRESHOLD_SETTING)) {
            threshold = prSettings.getDouble(TRESHOLD_SETTING);
            System.out.println("threshold: " + threshold); // debug
        } else {
            this.threshold = 700;
//            System.out.println("threshold: " + this.threshold); // debug

        }
        Gs = new HashMap<>();
        PeR = new HashMap<>();
        startTimes = new HashMap<>();
    }

    /**
     * Konstruktor salinan untuk PeopleRank.
     *
     * @param r Objek PeopleRank untuk direplikasi
     */
    public PeopleRankDecisionEngine(PeopleRankDecisionEngine r) {
        // Replikasi faktor redaman
        this.d = r.d;
        this.threshold = r.threshold;
        // Inisialisasi map
        startTimes = new HashMap<>();
        Gs = new HashMap<>();
        PeR = new HashMap<>();
        this.thisHost = r.thisHost;
    }

//    @Override
//    public void connectionUp(DTNHost thisHost, DTNHost peer) {
//
//    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        this.thisHost = thisHost;
        updatePeopleRank(thisHost);
        updatePeopleRank(peer);
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        // Dapatkan waktu mulai koneksi sebelumnya
        double time = getPreviousConnectionStartTime(thisHost, peer);
        double etime = SimClock.getTime();

        /**
         * Periksa ConnHistory untuk menemukan atau membuat daftar riwayat koneksi
         */
        List<Duration> history;
        // Periksa apakah ada riwayat koneksi untuk peer
        if (!Gs.containsKey(peer)) {
            // Jika tidak, buat daftar baru
            history = new ArrayList<>();
            // Masukkan daftar baru ke Gs
            Gs.put(peer, history);
        } else {
            // Jika ada, ambil riwayatnya
            history = Gs.get(peer);
        }

        /**
         * Periksa apakah durasi koneksi lebih besar atau sama dengan threshold
         * Jika ya, tambahkan koneksi ini ke daftar
         */
        if (etime - time >= threshold) {
            history.add(new Duration(time, etime));
            // Perbarui jumlah teman untuk peer
            updateFriendCount(peer, getFriendCount(peer) + 1); // peer adalah teman
        }
        updatePeopleRank(thisHost);
        updatePeopleRank(peer);
    }

//    @Override
//    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
//        // Dapatkan host lokal
//        DTNHost Ni = con.getOtherNode(peer);
//        // Dapatkan decision engine dari peer
//        PeopleRankDecisionEngine de = this.getOtherDecisionEngine(peer);
//
//        // Perbarui waktu mulai
//        this.startTimes.put(peer, SimClock.getTime());
//        if (de != null) {
//            de.startTimes.put(Ni, SimClock.getTime());
//        }
//
//        // Tukar informasi PeopleRank
//        double PeR_Ni = getPeopleRank(Ni);
//        int F_Ni = getFriendCount(Ni);
//        Tuple<Double, Integer> tuple_Ni = new Tuple<>(PeR_Ni, F_Ni);
//
//        double PeR_Nj;
//        int F_Nj;
//
//        if (de != null) {
//            Tuple<Double, Integer> peerTuple = de.getPeopleRankTuple(peer);
//
//            if (peerTuple != null) {
//                PeR_Nj = peerTuple.getFirst();
//                F_Nj = peerTuple.getSecond();
//            } else {
//                PeR_Nj = 0.0;
//                F_Nj = 0;
//            }
//            PeR.put(peer, new Tuple<>(PeR_Nj, F_Nj));
//
//        } else {
//            PeR_Nj = 0.0;
//            F_Nj = 0;
//        }
//        if (de != null) {
//            de.PeR.put(Ni, tuple_Ni);
//        }
//
//
//    }


    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        // Dapatkan host lokal
        DTNHost Ni = con.getOtherNode(peer);
        // Dapatkan decision engine dari peer
        PeopleRankDecisionEngine de = this.getOtherDecisionEngine(peer);

        // Perbarui waktu mulai
        this.startTimes.put(peer, SimClock.getTime());
        if (de != null) {
            de.startTimes.put(Ni, SimClock.getTime());
        }

        // Tukar informasi PeopleRank
        double PeR_Ni = getPeopleRank(Ni);
        int F_Ni = getFriendCount(Ni);
        Tuple<Double, Integer> tuple_Ni = new Tuple<>(PeR_Ni, F_Ni);

        double PeR_Nj;
        int F_Nj;

        if (de != null) {
            Tuple<Double, Integer> peerTuple = de.getPeopleRankTuple(peer);

            if (peerTuple != null) {
                PeR_Nj = peerTuple.getFirst();
                F_Nj = peerTuple.getSecond();
            } else {
                PeR_Nj = 0.0;
                F_Nj = 0;
            }
            PeR.put(peer, new Tuple<>(PeR_Nj, F_Nj));
            de.PeR.put(Ni, tuple_Ni);

            //Perbarui PeopleRank setelah bertukar informasi
            updatePeopleRank(Ni);
            de.updatePeopleRank(peer);
        } else {
            PeR_Nj = 0.0;
            F_Nj = 0;
        }


    }

    @Override
    public boolean newMessage(Message m) {
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost targetHost) {
        return m.getTo() == targetHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

//    @Override
//    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
//        // Periksa apakah tujuan pesan adalah otherHost
//        if (m.getTo() == otherHost) {
//            forward(m, otherHost); // Kirim langsung ke tujuan
//            return true; // Berhenti di sini, sudah diteruskan
//        }
//
//        // Bandingkan PeopleRank
//        double PeR_thisHost = getPeopleRank(thisHost);
//        double PeR_otherHost = getPeopleRank(otherHost);
//
//        // Teruskan jika peringkat otherHost lebih tinggi
//        if (PeR_otherHost >= PeR_thisHost) {
//            forward(m, otherHost); // Teruskan karena peringkat lebih tinggi
//            return true; // Berhenti di sini, sudah diteruskan
//        }
//
//        return false;
//    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        // Periksa apakah tujuan pesan adalah otherHost
        if (m.getTo() == otherHost) {
            forward(m, otherHost); // Kirim langsung ke tujuan
            return true; // Berhenti di sini, sudah diteruskan
        }

        // Bandingkan PeopleRank
        double PeR_thisHost = getPeopleRank(thisHost);
        double PeR_otherHost = getPeopleRank(otherHost);

        // Teruskan jika peringkat otherHost lebih tinggi
        if (PeR_otherHost >= PeR_thisHost) {
            // Tambahkan pemeriksaan kapasitas buffer
            MessageRouter router = otherHost.getRouter();
            if (router.getFreeBufferSize() < m.getSize()) {
                return false; // Jangan teruskan jika buffer penuh
            }

            // Tambahkan probabilitas penerusan
            double rankDiff = PeR_otherHost - PeR_thisHost;
            double forwardingProbability = Math.min(1.0, rankDiff * 2); // Sesuaikan faktor 2 sesuai kebutuhan

            if (Math.random() < forwardingProbability) {
                forward(m, otherHost); // Teruskan karena peringkat lebih tinggi
                return true; // Berhenti di sini, sudah diteruskan
            }

        }

        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    @Override
    public void update(DTNHost thisHost) {
        this.thisHost = thisHost;
        updatePeopleRank(thisHost);
    }

    @Override
    public RoutingDecisionEngine replicate() {
        PeopleRankDecisionEngine replicate = new PeopleRankDecisionEngine(this);
        replicate.setThisHost(this.thisHost);
        return replicate;
    }

    @Override
    public Map<DTNHost, Double> getAllPeopleRankings() {
        Map<DTNHost, Double> rankings = new HashMap<>();
        for (Map.Entry<DTNHost, Tuple<Double, Integer>> entry : PeR.entrySet()) {
            rankings.put(entry.getKey(), entry.getValue().getFirst());
        }
        return rankings;
    }

    @Override
    public int getFriendCountForHost(DTNHost host) {
        return getFriendCount(host);
    }

    /**
     * Menghitung PeopleRank berdasarkan Formula (2) dari paper.
     * PeR(Ni) = (1 − d) + d Σ Nj∈F(Ni) PeR(Nj) / |F(Nj)|
     *
     * @param host Node untuk menghitung PeopleRank
     * @return Nilai PeopleRank
     */
//    private double calculatePeopleRank(DTNHost host) {
//        double PeR_Ni = (1 - d);
//        double sum = 0.0;
//        List<DTNHost> friends = getFriends(host);
//
//        if (friends == null || friends.isEmpty()) return PeR_Ni; // Tidak ada teman
//
//        for (DTNHost friend : friends) {
//            double PeR_Nj = getPeopleRank(friend);
//            int F_Nj = getFriendCount(friend);
//
//            if (F_Nj > 0) {
//                sum += PeR_Nj / F_Nj;
//            }
//        }
//        PeR_Ni += d * sum;
//        return PeR_Ni;
//    }
    private double calculatePeopleRank(DTNHost host) {
        double PeR_Ni = (1 - d);
        double sum = 0.0;
        List<DTNHost> friends = getFriends(host);

        if (friends == null || friends.isEmpty()) return PeR_Ni; // Tidak ada teman

        for (DTNHost friend : friends) {
            double PeR_Nj = getPeopleRank(friend);
            int F_Nj = getFriendCount(friend);

            if (F_Nj > 0) {
                sum += PeR_Nj / F_Nj;
            }
        }
        PeR_Ni += d * sum;
        //Normalisasi People Rank
        PeR_Ni = Math.min(PeR_Ni, 10.0);

        return PeR_Ni;
    }

    private void updatePeopleRank(DTNHost host) {
        double newRank = calculatePeopleRank(host);
        int friendCount = getFriendCount(host);
        PeR.put(host, new Tuple<>(newRank, friendCount));
    }

    private void updateFriendCount(DTNHost host, int newFriendCount) {
        double currentRank = getPeopleRank(host);
        PeR.put(host, new Tuple<>(currentRank, newFriendCount));
    }


    private PeopleRankDecisionEngine getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "Router ini hanya bekerja dengan router jenis yang sama";

        return (PeopleRankDecisionEngine) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    // Metode pembantu untuk mendapatkan PeopleRank dan jumlah teman
    private double getPeopleRank(DTNHost host) {
        Tuple<Double, Integer> tuple = PeR.get(host);
        return (tuple != null) ? tuple.getFirst() : 1.0; // Nilai default 1.0
    }

    private Tuple<Double, Integer> getPeopleRankTuple(DTNHost host) {
        return PeR.get(host);
    }

    private int getFriendCount(DTNHost host) {
        Tuple<Double, Integer> tuple = PeR.get(host);
        return (tuple != null) ? tuple.getSecond() : 0;
    }

    private List<DTNHost> getFriends(DTNHost host) {
        List<DTNHost> friends = new ArrayList<>();
        Iterator<Map.Entry<DTNHost, List<Duration>>> iterator = Gs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DTNHost, List<Duration>> entry = iterator.next();
            if (entry.getKey().equals(host)) {
                friends.add(entry.getKey());
            }
        }
        return friends;
    }

    public double getPreviousConnectionStartTime(DTNHost thisHost, DTNHost peer) {
        // Periksa apakah ada waktu mulai koneksi yang tercatat
        if (startTimes.containsKey(peer)) {
            // Jika ada, kembalikan waktu mulai
            return startTimes.get(peer);
        } else {
            // Jika tidak ada, kembalikan 0
            return 0;
        }
    }

    /**
     * Forward(m,j) dari Algoritma 1 dengan pencegahan loop dan TTL.
     *
     * @param m Pesan yang akan diteruskan
     * @param j Node tujuan
     */
    private void forward(Message m, DTNHost j) {
        // Dapatkan MessageRouter dari node j
        MessageRouter router = j.getRouter();

        // Dapatkan thisHost dari router ini
        //MessageRouter thisRouter = this.getOtherDecisionEngine(j).getHost().getRouter();
        //DTNHost thisHost = thisRouter.getHost();

        // Periksa apakah pesan sudah pernah mengunjungi node ini
        if (m.getHops().contains(j)) {
            //System.out.println("Pesan sudah mengunjungi node ini, mencegah loop.");
            return; // Jangan teruskan, cegah loop
        }

        // Periksa TTL
        if (m.getTtl() <= 0) {
            //System.out.println("TTL sudah habis, pesan dibatalkan.");
            return; // Jangan teruskan, TTL habis
        }

        // Buat salinan pesan
        Message mCopy = m.replicate();

        // Kurangi TTL
        mCopy.setTtl(m.getTtl() - 1);

        // Tambahkan node ini ke riwayat hop
        mCopy.addNodeOnPath(j);

        // Dapatkan MessageRouter dari node saat ini
        DecisionEngineRouter thisRouter = (DecisionEngineRouter) thisHost.getRouter();
        // Teruskan salinan pesan ke router node j
        int result = router.receiveMessage(mCopy, thisHost);

        // Periksa apakah pesan diterima (opsional)
        if (result != MessageRouter.RCV_OK) {
            //System.out.println("Pesan tidak diterima oleh " + j + ", kode: " + result);
        }

        //System.out.println("Meneruskan pesan dari " + thisHost + " ke " + j);
    }

    public void setThisHost(DTNHost thisHost) {
        this.thisHost = thisHost;
    }


}