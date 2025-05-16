package report;

import core.DTNHost;
import core.SimScenario;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import routing.community.DistributedBubbleRapUTS;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class BubleRapIntervalReport extends Report {

    public BubleRapIntervalReport() {
        init();
    }

    @Override
    public void done() {
        List<DTNHost> hosts = SimScenario.getInstance().getHosts();

        write("Global Popularity Report\n");
        write("-------------------------\n");

        for (DTNHost host : hosts) {
            MessageRouter router = host.getRouter();
            if (!(router instanceof DecisionEngineRouter))
                continue;

            RoutingDecisionEngine de = ((DecisionEngineRouter) router).getDecisionEngine();
            if (!(de instanceof DistributedBubbleRapUTS))
                continue;

            DistributedBubbleRapUTS dbr = (DistributedBubbleRapUTS) de;

            // Dapatkan riwayat centrality (data sudah dihitung di CWindowCentrality)
            double[] popularityHistory = dbr.getGlobalPopularityHistory(host, 86400);

            // Buat Map untuk menyimpan history sebagai key-value pairs
            Map<Integer, Double> popularityMap = new HashMap<>();
            if (popularityHistory != null) {
                for (int i = 0; i < popularityHistory.length; i++) {
                    popularityMap.put(i + 1, popularityHistory[i]); // Key dimulai dari 1
                }
            }

            String reportLine = "Node " + host.getAddress() + ": ";
            if (!popularityMap.isEmpty()) {
                for (Map.Entry<Integer, Double> entry : popularityMap.entrySet()) {
                    reportLine += "(" + entry.getKey() + ": " + String.format("%.2f", entry.getValue()) + ") ";
                }
            } else {
                reportLine += "No popularity data available.";
            }
            write(reportLine + "\n");
        }

        super.done();
    }
}