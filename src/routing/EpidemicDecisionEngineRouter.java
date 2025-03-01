package routing;

import core.*;

/**
 * @author Hendro Wunga, University Sanata Dharma
 */

public class EpidemicDecisionEngineRouter implements RoutingDecisionEngine {


     public EpidemicDecisionEngineRouter(Settings s) {
          // Tidak perlu inisialisasi parameter khusus
     }

     public EpidemicDecisionEngineRouter(EpidemicDecisionEngineRouter other) {
          // Copy constructor untuk replikasi
     }

     @Override
     public void connectionUp(DTNHost thisHost, DTNHost peer) {
          // Tidak ada tindakan khusus saat koneksi naik
     }

     @Override
     public void connectionDown(DTNHost thisHost, DTNHost peer) {
          // Tidak ada tindakan khusus saat koneksi turun
     }

     @Override
     public void doExchangeForNewConnection(Connection con, DTNHost peer) {
          // Tidak perlu pertukaran informasi khusus untuk Epidemic
     }

     @Override
     public boolean newMessage(Message m) {
          // Terima semua pesan baru
          return true;
     }

     @Override
     public boolean isFinalDest(Message m, DTNHost aHost) {
          // Tujuan akhir adalah alamat yang ditentukan dalam pesan
          return m.getTo() == aHost;
     }

     @Override
     public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
          // Simpan pesan jika ini bukan tujuan akhir
          return !isFinalDest(m, thisHost);
     }

     @Override
     public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
          /* Kirim pesan jika:
           * 1. Node tujuan belum memiliki pesan
           * 2. Node tujuan adalah penerima akhir
           */
          return !otherHost.getRouter().hasMessage(m.getId()) ||
                  isFinalDest(m, otherHost);
     }

     @Override
     public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
          // Jangan hapus pesan setelah dikirim (bisa dikirim ke node lain)
          return false;
     }

     @Override
     public boolean shouldDeleteOldMessage(Message m, DTNHost hostReporting) {
          // Hapus pesan jika TTL habis
          return m.getTtl() <= 0;
     }

     @Override
     public RoutingDecisionEngine replicate() {
          return new EpidemicDecisionEngineRouter(this);
     }

     // Helper untuk debugging
     private void debugLog(DTNHost host, String message) {
          System.out.println("[Epidemic] Host " + host + ": " + message);
     }
}

