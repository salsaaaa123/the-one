package routing;

import core.*;
import java.util.HashSet;

public class EpidemicDecisionEngineRouter implements RoutingDecisionEngine {

     private HashSet<Integer> deliveredMessageIds;

     public EpidemicDecisionEngineRouter() {
          this.deliveredMessageIds = new HashSet<>();
     }

     @Override
     public void connectionUp(DTNHost thisHost, DTNHost peer) {
          // Tidak ada tindakan khusus yang diperlukan saat koneksi naik
     }

     @Override
     public void connectionDown(DTNHost thisHost, DTNHost peer) {
          // Tidak ada tindakan khusus yang diperlukan saat koneksi turun
     }

     @Override
     public void doExchangeForNewConnection(Connection con, DTNHost peer) {
          // Tidak ada pertukaran informasi khusus yang diperlukan di sini
     }

     @Override
     public boolean newMessage(Message m) {
          // Selalu teruskan pesan baru (epidemic)
          return true;
     }

     @Override
     public boolean isFinalDest(Message m, DTNHost aHost) {
          return m.getTo() == aHost;
     }

     @Override
     public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
          // Simpan pesan yang belum pernah diterima sebelumnya
          return !deliveredMessageIds.contains(m.getId());
     }

     @Override
     public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
          return true;
     }


     @Override
     public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
          // Pesan tidak perlu dihapus setelah dikirim dalam pendekatan Epidemic murni
          return false;
     }

     @Override
     public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
          // Hapus pesan yang sudah pernah terkirim
          if(deliveredMessageIds.contains(m.getId())){
               return true;
          }
          return false;
     }


     @Override
     public RoutingDecisionEngine replicate() {
          return new EpidemicDecisionEngineRouter();
     }

     @Override
     public String toString() {
          return "EpidemicDecisionEngine";
     }
}