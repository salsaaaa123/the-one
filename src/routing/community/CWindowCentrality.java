package routing.community;

import java.util.*;

import core.*;

public class CWindowCentrality implements Centrality {
    public static final String CENTRALITY_WINDOW_SETTING = "timeWindow";
    public static final String COMPUTATION_INTERVAL_SETTING = "computeInterval";
    public static final String EPOCH_COUNT_SETTING = "nrOfEpochsToAvg";

    protected static int COMPUTE_INTERVAL = 600;
    protected static int CENTRALITY_TIME_WINDOW = 21600;
    protected static int EPOCH_COUNT = 5;

    protected double globalCentrality;
    protected double localCentrality;

    protected int lastGlobalComputationTime;
    protected int lastLocalComputationTime;

    // Array untuk menyimpan history centrality
    private double[] globalCentralityHistory;

    public CWindowCentrality(Settings s) {
        if (s.contains(CENTRALITY_WINDOW_SETTING)) {
            CENTRALITY_TIME_WINDOW = s.getInt(CENTRALITY_WINDOW_SETTING);
            if (CENTRALITY_TIME_WINDOW <= 0) {
                System.err.println("Error: " + CENTRALITY_WINDOW_SETTING + " must be positive. Using default value.");
                CENTRALITY_TIME_WINDOW = 21600;
            }
        }

        if (s.contains(COMPUTATION_INTERVAL_SETTING))
            COMPUTE_INTERVAL = s.getInt(COMPUTATION_INTERVAL_SETTING);

        if (s.contains(EPOCH_COUNT_SETTING))
            EPOCH_COUNT = s.getInt(EPOCH_COUNT_SETTING);


    }

    public CWindowCentrality(CWindowCentrality proto) {
        this.lastGlobalComputationTime = this.lastLocalComputationTime = -COMPUTE_INTERVAL;
    }

    public double getGlobalCentrality(Map<DTNHost, List<Duration>> connHistory) {
        if (SimClock.getIntTime() - this.lastGlobalComputationTime < COMPUTE_INTERVAL)
            return globalCentrality;

        int[] centralities = new int[EPOCH_COUNT];
        int epoch, timeNow = SimClock.getIntTime();
        Map<Integer, Set<DTNHost>> nodesCountedInEpoch = new HashMap<>();

        for (int i = 0; i < EPOCH_COUNT; i++)
            nodesCountedInEpoch.put(i, new HashSet<>());

        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            DTNHost h = entry.getKey();
            for (Duration d : entry.getValue()) {
                int timePassed = (int) (timeNow - d.end);

                if (timePassed > CENTRALITY_TIME_WINDOW * EPOCH_COUNT)
                    break;

                epoch = timePassed / CENTRALITY_TIME_WINDOW;

                if (epoch < 0 || epoch >= EPOCH_COUNT) {
                    continue;
                }

                Set<DTNHost> nodesAlreadyCounted = nodesCountedInEpoch.get(epoch);
                if (nodesAlreadyCounted.contains(h))
                    continue;

                centralities[epoch]++;
                nodesCountedInEpoch.get(epoch).add(h);
            }
        }

        int sum = 0;
        for (int i = 0; i < EPOCH_COUNT; i++)
            sum += centralities[i];

        if (EPOCH_COUNT > 0) {
            this.globalCentrality = ((double) sum) / EPOCH_COUNT;
        } else {
            this.globalCentrality = 0;
        }

        this.lastGlobalComputationTime = SimClock.getIntTime();

        return this.globalCentrality;
    }

    public double getLocalCentrality(Map<DTNHost, List<Duration>> connHistory, CommunityDetection cd) {
        if (SimClock.getIntTime() - this.lastLocalComputationTime < COMPUTE_INTERVAL)
            return localCentrality;

        int[] centralities = new int[EPOCH_COUNT];
        int epoch, timeNow = SimClock.getIntTime();
        Map<Integer, Set<DTNHost>> nodesCountedInEpoch = new HashMap<>();

        for (int i = 0; i < EPOCH_COUNT; i++)
            nodesCountedInEpoch.put(i, new HashSet<>());

        Set<DTNHost> community = cd.getLocalCommunity();

        for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
            DTNHost h = entry.getKey();

            if (!community.contains(h))
                continue;

            for (Duration d : entry.getValue()) {
                int timePassed = (int) (timeNow - d.end);

                if (timePassed > CENTRALITY_TIME_WINDOW * EPOCH_COUNT)
                    break;

                epoch = timePassed / CENTRALITY_TIME_WINDOW;

                if (epoch < 0 || epoch >= EPOCH_COUNT) {
                    continue;
                }

                Set<DTNHost> nodesAlreadyCounted = nodesCountedInEpoch.get(epoch);
                if (nodesAlreadyCounted.contains(h))
                    continue;

                centralities[epoch]++;
                nodesCountedInEpoch.get(epoch).add(h);
            }
        }

        int sum = 0;
        for (int i = 0; i < EPOCH_COUNT; i++)
            sum += centralities[i];
        this.localCentrality = ((double) sum) / EPOCH_COUNT;

        this.lastLocalComputationTime = SimClock.getIntTime();

        return this.localCentrality;
    }

    public Centrality replicate() {
        return new CWindowCentrality(this);
    }

    @Override
    public double[] getGlobalCentralityHistory(Map<DTNHost, List<Duration>> connHistory, int interval) {
        int simTime = SimClock.getIntTime();
        int nrofInterval = simTime / interval + 1;

        globalCentralityHistory = new double[nrofInterval];
        Arrays.fill(globalCentralityHistory, 0.0);

        for (int i = 0; i < nrofInterval; i++) {
            int startTime = i * interval;
            int endTime = (i + 1) * interval;
            if (endTime > simTime)
                endTime = simTime;

            // Inisialisasi variable untuk perhitungan centrality dalam interval ini
            int[] centralities = new int[EPOCH_COUNT];
            int epoch;
            int timeNow = endTime;
            Map<Integer, Set<DTNHost>> nodesCountedInEpoch = new HashMap<>();

            // Inisialisasi nodesCountedInEpoch
            for (int j = 0; j < EPOCH_COUNT; j++)
                nodesCountedInEpoch.put(j, new HashSet<>());

            // Looping untuk menghitung centrality di dalam interval waktu
            for (Map.Entry<DTNHost, List<Duration>> entry : connHistory.entrySet()) {
                DTNHost h = entry.getKey();
                for (Duration d : entry.getValue()) {
                    // Hanya hitung koneksi yang terjadi dalam interval waktu ini
                    if (d.end < startTime || d.start > endTime) {
                        continue;
                    }

                    // Filter durasi diluar EPOCH COUNT
                    int timePassed = (int) (timeNow - d.end);

                    if (timePassed > CENTRALITY_TIME_WINDOW * EPOCH_COUNT)
                        break;
                    // Hitung epoch
                    epoch = timePassed / CENTRALITY_TIME_WINDOW;

                    // Validasi epoch
                    if (epoch < 0 || epoch >= EPOCH_COUNT) {
                        continue;
                    }

                    // Cek apakah node sudah dihitung pada epoch ini
                    Set<DTNHost> nodesAlreadyCounted = nodesCountedInEpoch.get(epoch);
                    if (nodesAlreadyCounted.contains(h))
                        continue;

                    // Tambah nilai centrality pada epoch ini
                    centralities[epoch]++;
                    // Tandai node sudah dihitung dalam epoch ini
                    nodesCountedInEpoch.get(epoch).add(h);
                }
            }

            // Hitung total
            double sum = 0;
            for (int k = 0; k < EPOCH_COUNT; k++)
                sum += centralities[k];

            // Hitung centralitas dengan EPOCH COUNT>0
            double centrality = 0;
            if (EPOCH_COUNT > 0) {
                centrality = ((double) sum) / EPOCH_COUNT;
            }

            // Simpan dan lanjut ke interval berikutnya
            globalCentralityHistory[i] = centrality;
        }

        return globalCentralityHistory;
    }
}