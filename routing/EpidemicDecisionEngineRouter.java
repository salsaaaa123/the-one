package routing;

import core.*;
import routing.*;

public class EpidemicDecisionEngineRouter implements RoutingDecisionEngine  {

     public EpidemicDecisionEngineRouter(Settings settings) {


     }

     public EpidemicDecisionEngineRouter(EpidemicDecisionEngineRouter prototype) {

     }
     @Override
     public void connectionUp(DTNHost thisHost, DTNHost peer) {

     }

     @Override
     public void connectionDown(DTNHost thisHost, DTNHost peer) {
     }

     @Override
     public void doExchangeForNewConnection(Connection con, DTNHost peer) {
     }

     @Override
     public boolean newMessage(Message m) {
          return true;
     }

     @Override
     public boolean isFinalDest(Message m, DTNHost aHost) {
          return m.getTo() == aHost;
     }

     @Override
     public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
          return !thisHost.getRouter().hasMessage(m.getId());
     }

     @Override
     public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
          return true;
     }

     @Override
     public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
          return false;
     }

     @Override
     public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
          return false;
     }

     private EpidemicDecisionEngineRouter getOtherEpidemicRouter(DTNHost host) {
          MessageRouter otherRouter = host.getRouter();
          assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                  + " with other routers of same type";

          return (EpidemicDecisionEngineRouter) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
     }

     @Override
     public RoutingDecisionEngine replicate() {
          return new EpidemicDecisionEngineRouter(this);
     }


}