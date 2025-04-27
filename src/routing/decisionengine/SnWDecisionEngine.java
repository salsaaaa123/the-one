

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
     * Kunci properti pesan untuk menyimpan jumlah salinan (nilai = "SnWDecisionEngine.copies")
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
     * Menyimpan waktu pembuatan setiap pesan. Kunci adalah ID pesan, dan nilainya adalah waktu pembuatan.
     */
    private final Map<String, Double> messageCreationTimes = new HashMap<>();


    /**
     * Konstruktor untuk kelas SprayAndWaitDecisionEngine.
     *
     * @param s Objek Settings yang berisi pengaturan konfigurasi.
     */
    public SnWDecisionEngine(Settings s) {
        Settings snwSettings = new Settings(SPRAYANDWAIT_NS);
//        initialNrofCopies = snwSettings.getInt(NROF_COPIES);
        if (snwSettings.contains(NROF_COPIES)) {
            initialNrofCopies = snwSettings.getInt(NROF_COPIES);
            System.out.println("initialNrofCopies: " + initialNrofCopies); // debug
        } else {
            this.initialNrofCopies = 2;
        }
//        isBinary = snwSettings.getBoolean(BINARY_MODE);

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
     * @param con  Objek {@link Connection} yang merepresentasikan koneksi yang baru dibuat.
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
        boolean shouldSave = false; // Oke, awalnya kita anggap gak perlu nyimpen pesan

        // Cek dulu, pesan ini udah kadaluarsa belum?
        if (isMessageExpired(m)) {
            shouldSave = false; // Udah basi? Yaudah, jangan simpan lah!
        } else {
            // Hmm, belum kadaluarsa nih, coba kita cek lagi lebih dalam...

            // Eh, host ini udah punya pesan dengan ID yang sama belum?
            if (thisHost.getRouter().hasMessage(m.getId())) {
                shouldSave = false; // Udah punya? Ya ngapain disimpan lagi, mubazir bro! wkwkwkw
            } else {
                // Oke, pesan ini unik! Tapi ada satu hal lagi yang perlu dicek...

                // Pesan ini dikirim sama siapa sih? Host ini sendiri?
                if (m.getFrom() == thisHost) {
                    shouldSave = false; // Wah, pesan sendiri? Jangan simpan dong, nanti looping mulu!
                } else {
                    shouldSave = true; // Mantap! Pesan ini fresh & belum ada di sini, simpan aja!
                }
            }
        }

        return shouldSave; // Oke deh, kasih tahu hasil akhirnya: simpan atau tidak?
    }


    /**
     * @param m         Pesan yang akan dievaluasi untuk pengiriman.
     * @param otherHost Host peer (host lain) yang berpotensi menjadi tujuan pengiriman.
     * @param thisHost  Host yang menjalankan router ini (host lokal).
     * @return
     */
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        boolean shouldSend = false; // Apakah pesan kamu harus dikirim? default: tidak
        Integer copies = (Integer) m.getProperty(MSG_COUNT_PROPERTY); // Tolong dapatkan jumlah salinan pesan
        if (copies != null) { // Tolong periksa apakah jumlah pesan salinan tidak null,oh gk null niehh
            if (copies > 1) { // gitu po, tapi bantu periksa lagi apakah jumlah salinan lebih besar dari 1
                shouldSend = true; // Iya lebih, kirim pesan lahhhh
            } else {
                shouldSend = false; // aduh gk lebih nih, you jangan kirim pesan
            }
        } else {
            shouldSend = false; // Wah jumlah salinan null niehh, ah gk usah kirm pesan lah
        }
        return shouldSend; // Ok,sekarang kembalikan dong nilaiNya saya mau kirim nih
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        return false;
    }

    /**
     * @param m         Pesan yang telah berhasil dikirim.
     * @param otherHost Host peer (host lain) yang menerima pesan.
     * @return
     */
    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        boolean deleteMessage = false; // Apakah pesan harus dihapus? Default: tidak
        DTNHost thisHost = (DTNHost) m.getProperty("thisHost"); // Tolong ambil thisHost dari properti pesan

        if (thisHost == null) { // Loh kok thisHost gak ada?
            System.out.println("Error: thisHost tidak ditemukan dalam properti pesan!"); // Wah error nih, kasih tahu user
            return false; // Jangan hapus pesan lah, nanti error!
        }

        if (isFinalDest(m, otherHost)) { // Eh pesan udah sampai tujuan akhir kah?
            messageCreationTimes.remove(m.getId()); // Iyalah, hapus waktu pembuatan pesan
            deleteMessage = true; // Hapus pesan dong, udah gak perlu lagi
        } else if (isMessageExpired(m)) { // Periksa lagi, pesan ini udah kadaluarsa belum ya?
            messageCreationTimes.remove(m.getId()); // Udah? Yaudah hapus aja dari map
            deleteMessage = true; // Hapus pesan, udah basi nih
        } else { // Belum sampai tujuan & belum kadaluarsa, kita cek yang lain dulu
            Integer copies = (Integer) m.getProperty(MSG_COUNT_PROPERTY); // Coba ambil jumlah salinan pesan dulu

            if (copies == null) { // Loh, jumlah salinan pesan kok gak ada?
                deleteMessage = true; // Ah hapus aja, udah gak jelas ini
            } else if (copies <= 0) { // Eh jumlah salinan 0 atau negatif kah?
                deleteMessage = true; // Iya nih, udah hapus aja pesannya
            } else { // Oh ada salinannya? Aman kok, jangan hapus dulu
                deleteMessage = false; // Pesan tetap disimpan ya!
            }
        }

        return deleteMessage; // Oke, kasih tahu hasilnya apakah pesan dihapus atau tidak
    }


    /**
     * @param m                Pesan yang dianggap sudah lama.
     * @param hostReportingOld Host peer (host lain) yang melaporkan bahwa pesan tersebut sudah lama.
     * @return
     */
    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        boolean deleteMessage = false; // ini, awalnya kita anggap pesannya gak perlu fihapus
        if (isMessageExpired(m)) { // eh benar, pesannya udah kedaluwarsa belum ya?
            deleteMessage = true; // Wah,udah basi! Hapus aja biar gak numpuk
        } else {
            deleteMessage = false; // oh,masih fresh? Yaudah,biarin aja dulu
        }
        return deleteMessage; // Oke, kasih tahu hasil akhirnya,dihapus gk nih?
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
        Double creationTime = messageCreationTimes.get(m.getId()); // Cek dulu ,kapan sih pesan ini dihapus?
        boolean messageExpired = false; // Awalnya kita anggap pesannya masih fresh,belum basi
        if (creationTime == null) { // LOh,kok gak ada waktu pembuatannya?
            messageExpired = false; // Hmm,anggap aja belum kedaluwarsa
        } else {
            double age = SimClock.getTime() - creationTime; // Hitung usia pesan,udah tua belum nih?
            if (age > m.getTtl()) { // Eh, lebih tua dari batas waktu hidupnya kah?
                messageExpired = true;  // Iya, udah terlalu tua! Buang aja deh
            } else {
                messageExpired = false; // Masih muda nih, biarin aja lanjut hidup
            }
        }
        return messageExpired; // Oke, kasih tahu hasilnya, expired atau masih bisa dipakai?
    }

    /**
     * @return
     */
    @Override
    public RoutingDecisionEngine replicate() {
        return new SnWDecisionEngine(this);
    }
}
