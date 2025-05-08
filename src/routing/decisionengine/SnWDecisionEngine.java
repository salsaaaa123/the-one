package routing.decisionengine;

import core.*;
import routing.*;

import java.util.HashMap;
import java.util.Map;

/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */

public class SnWDecisionEngine implements RoutingDecisionEngine {

    /**
     * Identifier untuk pengaturan jumlah salinan awal (nilai = "nrofCopies")
     */
    public static final String NROF_COPIES = "nrofCopies";
    /**
     * Identifier untuk pengaturan mode biner (nilai = "binaryMode")
     */
    public static final String BINARY_MODE = "binaryMode";
    /**
     * Namespace untuk pengaturan SprayAndWait (nilai = "SnWDecisionEngine")
     */
    public static final String SPRAYANDWAIT_NS = "SnWDecisionEngine";
    /**
     * Kunci properti pesan untuk menyimpan jumlah salinan (nilai =
     * "SnWDecisionEngine.copies")
     */
    public static final String MSG_COUNT_PROPERTY = SPRAYANDWAIT_NS + "." + "copies";
    /**
     * Jumlah awal salinan pesan yang akan di-spray
     */
    protected int initialNrofCopies;
    /**
     * Apakah menggunakan mode biner atau tidak
     */
    protected boolean isBinary;
    /**
     * Menyimpan waktu pembuatan setiap pesan. Kunci adalah ID pesan, dan nilainya
     * adalah waktu pembuatan.
     */
    private final Map<String, Double> messageCreationTimes = new HashMap<>();

    /**
     * Konstruktor untuk kelas SprayAndWaitDecisionEngine.
     *
     * @param s Objek Settings yang berisi pengaturan konfigurasi.
     */
    public SnWDecisionEngine(Settings s) {
        Settings snwSettings = new Settings(SPRAYANDWAIT_NS);
        if (snwSettings.contains(NROF_COPIES)) {
            initialNrofCopies = snwSettings.getInt(NROF_COPIES);
            System.out.println("initialNrofCopies: " + initialNrofCopies); // debug
        } else {
            this.initialNrofCopies = 2;
        }

        if (snwSettings.contains(BINARY_MODE)) {
            isBinary = snwSettings.getBoolean(BINARY_MODE);
            System.out.println("isBinary: " + isBinary); // debug
        } else {
            this.isBinary = true;
        }
    }

    public SnWDecisionEngine(SnWDecisionEngine r) {
        this.initialNrofCopies = r.initialNrofCopies;
        this.isBinary = r.isBinary;
    }

    /**
     * @param thisHost Host yang menjalankan router ini (host lokal).
     * @param peer     Host peer (host lain) yang terhubung.
     */
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {

    }

    /**
     * @param thisHost Host yang menjalankan router ini (host lokal).
     * @param peer     Host peer (host lain) yang koneksinya terputus.
     */
    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {

    }

    /**
     * @param con  Objek {@link Connection} yang merepresentasikan koneksi yang baru
     *             dibuat.
     * @param peer Host peer (host lain) yang terhubung melalui koneksi ini.
     */
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {

    }

    /**
     * @param m Pesan baru yang akan dipertimbangkan untuk routing.
     * @return
     */
    @Override
    public boolean newMessage(Message m) {
        try {
            m.addProperty(MSG_COUNT_PROPERTY, initialNrofCopies);
        } catch (SimError e) {
            System.out.println("Error adding property to message: " + e.getMessage());
            return false;
        }
        messageCreationTimes.put(m.getId(), SimClock.getTime());
        return true;
    }

    /**
     * @param m     Pesan yang baru diterima.
     * @param aHost Host yang akan diperiksa apakah merupakan tujuan akhir.
     * @return
     */
    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost;
    }

    /**
     * @param m        Pesan yang baru diterima dari peer.
     * @param thisHost Host yang menjalankan router ini (host lokal).
     * @return
     */
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        boolean shouldSave = false;

        if (isMessageExpired(m)) {
            shouldSave = false;
        } else {
            if (thisHost.getRouter().hasMessage(m.getId())) {
                shouldSave = false;
            } else {
                if (m.getFrom() == thisHost) {
                    shouldSave = false;
                } else {
                    shouldSave = true;
                }
            }
        }

        return shouldSave;
    }

    /**
     * @param m         Pesan yang akan dievaluasi untuk pengiriman.
     * @param otherHost Host peer (host lain) yang berpotensi menjadi tujuan
     *                  pengiriman.
     * @return
     */
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        boolean shouldSend = false;
        Integer copies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);
        if (copies != null) {
            if (copies > 1) {
                shouldSend = true;
            } else {
                shouldSend = false;
            }
        } else {
            shouldSend = false;
        }
        return shouldSend;
    }

    /**
     * @param m         Pesan yang telah berhasil dikirim.
     * @param otherHost Host peer (host lain) yang menerima pesan.
     * @return
     */
    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        boolean deleteMessage = false;

        if (isFinalDest(m, otherHost)) {
            messageCreationTimes.remove(m.getId());
            deleteMessage = true;
        } else if (isMessageExpired(m)) {
            messageCreationTimes.remove(m.getId());
            deleteMessage = true;
        } else {
            Integer copies = (Integer) m.getProperty(MSG_COUNT_PROPERTY);

            if (copies == null) {
                deleteMessage = true;
            } else if (copies <= 0) {
                deleteMessage = true;
            } else {
                deleteMessage = false;
            }
        }

        return deleteMessage;
    }

    /**
     * @param m                Pesan yang dianggap sudah lama.
     * @param hostReportingOld Host peer (host lain) yang melaporkan bahwa pesan
     *                         tersebut sudah lama.
     * @return
     */
    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        boolean deleteMessage = false;
        if (isMessageExpired(m)) {
            deleteMessage = true;
        } else {
            deleteMessage = false;
        }
        return deleteMessage;
    }

    /**
     * @param thisHost Host yang menjalankan router ini (host lokal).
     */
    @Override
    public void update(DTNHost thisHost) {

    }

    /**
     * Memeriksa apakah pesan sudah kedaluwarsa berdasarkan TTL.
     *
     * @param m Pesan yang akan diperiksa.
     * @return true jika pesan sudah kedaluwarsa, false jika tidak.
     */
    public boolean isMessageExpired(Message m) {
        Double creationTime = messageCreationTimes.get(m.getId());
        boolean messageExpired = false;
        if (creationTime == null) {
            messageExpired = false;
        } else {
            double age = SimClock.getTime() - creationTime;
            if (age > m.getTtl()) {
                messageExpired = true;
            } else {
                messageExpired = false;
            }
        }
        return messageExpired;
    }

    /**
     * @return
     */
    @Override
    public RoutingDecisionEngine replicate() {
        return new SnWDecisionEngine(this);
    }
}