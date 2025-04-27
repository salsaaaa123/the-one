package routing.decisionengine;

import java.util.*;

import core.*;
import routing.*;

/**
 * An implementation of the Spray and Focus Routing protocol using the
 * Decision Engine framework.
 *
 * @author PJ Dillon, University of Pittsburgh
 *
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */
public class SnFDecisionEngine implements RoutingDecisionEngine
{
	/**
	 * Jumlah awal salinan pesan yang akan disebarkan
	 * Misalnya,kalau kita set 10, berarti pesan ini awalnya 10 salinan yang bisa dibagikan.
	 *
	 */
	public static final String NROF_COPIES_S = "nrofCopies";
	/**
	 * Properti dalam pesan yang nyimpen jumlah salinan yang masi tersedia.
	 * Jadi kalau pesan ini masih dibagi,nilainya bakal lebih dari 1.
	 */
	public static final String MSG_COUNT_PROP = "SprayAndFocus.copies";
	/**
	 * Perbedaan waktu yang diperluka buat bisa meneruskan ke node lain.
	 * Semakin besar thershold,semakin "pemilih" si protokol ini dalam berbagi pesan.
	 */
	public static final String TIMER_THRESHOLD_S = "transitivityTimerThreshold";
	/**
	 * Nilai default kalau ita butuh asumsi waktu tempuh antara node.
	 * Misalnya,kalau kecepatan host 0 ( lagi diem),kita pakai nilai ini.
	 */
	protected static final double DEFAULT_TIMEDIFF = 300;
	/**
	 * Nilai default buat transitivity thersholdd
	 * Ini merupakan seberapa besar perbedaan timer yang dianggap cukup buat berbagi pesan
	 */
	protected static final double defaultTransitivityThreshold = 1.0;
	/**
	 * Jumlah awal salinan pesan yang bisa dibagikan.
	 * Nilai ini diatur dari konfigurasi awal dan akan berkurang seiring pesan tersebar.
	 */
	protected int initialNrofCopies;
	/**
	 * Ambang batas perbedaan timer untuk memutuskan apakah pesan bisa diteruskan.
	 * Semakin kecil nilainya, semakin sering pesan bisa diteruskan.
	 */
	protected double transitivityTimerThreshold;

	/**
	 * Map yang nyimpen informasi tentang node-node yang udah pernah ketemu sama host ini.
	 * Kuncinya adalah host yang pernah ketemu, nilainya adalah waktu terakhir ketemu.
	 * Digunakan buat bantu keputusan apakah pesan perlu diteruskan atau nggak.
	 */
	protected Map<DTNHost, Double> recentEncounters;

	public SnFDecisionEngine(Settings s)
	{
		// Ambil jumlah awal salinan pesan dari konfigurasi
		initialNrofCopies = s.getInt(NROF_COPIES_S);

		// Cek apakah ada threshold transitivity yang diatur di konfigurasi
		if (s.contains(TIMER_THRESHOLD_S))
			transitivityTimerThreshold = s.getDouble(TIMER_THRESHOLD_S); // Kalau ada, pakai yang dikonfigurasi
		else
			transitivityTimerThreshold = defaultTransitivityThreshold; // Kalau nggak ada, pakai nilai default

		// Siapkan tempat buat nyimpen daftar host yang pernah ketemu!
		recentEncounters = new HashMap<DTNHost, Double>();
	}

	public SnFDecisionEngine(SnFDecisionEngine r) {
		this.initialNrofCopies = r.initialNrofCopies;
		this.transitivityTimerThreshold = r.transitivityTimerThreshold;
		recentEncounters = new HashMap<>();
	}

	/**
	 * @param thisHost Host lokal (yang menjalankan router ini).
	 * @param peer     Host peer (host lain) yang terhubung.
	 */
	@Override
	public void connectionUp(DTNHost thisHost, DTNHost peer) {
		// Tidak ada tindakan khusus yang diperlukan saat koneksi naik
	}

	/**
	 * @param thisHost Host lokal (yang menjalankan router ini).
	 * @param peer     Host peer (host lain) yang koneksinya terputus.
	 */
	@Override
	public void connectionDown(DTNHost thisHost, DTNHost peer) {
		// Tidak ada tindakan khusus yang diperlukan saat koneksi turun
	}

	/**
	 * @param con  Objek {@link Connection} yang merepresentasikan koneksi baru.
	 * @param peer Host peer (host lain) yang terhubung melalui koneksi ini.
	 */
	@Override
	public void doExchangeForNewConnection(Connection con, DTNHost peer) {
		// Ambil decision engine dari peer yang baru terkoneksi
		// Ini gunanya buat bisa saling bertukar informasi encounter nanti
		SnFDecisionEngine de = this.getOtherSnFDecisionEngine(peer);

		// Cari tahu siapa "saya" dalam koneksi ini
		// `con.getOtherNode(peer)` artinya kita ingin tahu siapa "saya"
		// dalam koneksi ini dari sudut pandang peer
		DTNHost myHost = con.getOtherNode(peer);

		// Hitung jarak antara saya (myHost) dan peer
		// Method `getLocation()` ini bakal ngasih tahu lokasi dari masing-masing host di dalam simulasi
		double distBwt = myHost.getLocation().distance(peer.getLocation());

		// Cek kecepatan masing-masing host
		// Kalau `getPath()` == null, berarti host ini sedang diam alias nggak bergerak
		// Kalau nggak null, kita bisa ambil kecepatannya lewat `getSpeed()`
		double mySpeed = myHost.getPath() == null ? 0 : myHost.getPath().getSpeed();
		double peerSpeed = peer.getPath() == null ? 0 : peer.getPath().getSpeed();

		// Variabel buat nyimpen waktu tempuh antara dua host
		double myTimediff, peerTimediff;

		// Kalau saya (myHost) diam alias `mySpeed == 0.0`, kasih nilai default waktu tempuhnya
		if (mySpeed == 0.0)
			myTimediff = DEFAULT_TIMEDIFF; // DEFAULT_TIMEDIFF ini default 300 detik
		else
			myTimediff = distBwt / mySpeed; // Kalau saya bergerak, waktu tempuh = jarak / kecepatan

		// Lakukan hal yang sama buat peer
		if (peerSpeed == 0.0)
			peerTimediff = DEFAULT_TIMEDIFF; // Kasih nilai default kalau peer diam
		else
			peerTimediff = distBwt / peerSpeed; // Hitung waktu tempuh peer

		// Catat waktu pertemuan terbaru ke dalam daftar "recentEncounters" masing-masing host
		// `SimClock.getTime()` ini bakal ngasih waktu sekarang di dalam simulasi
		recentEncounters.put(peer, SimClock.getTime());
		de.recentEncounters.put(myHost, SimClock.getTime());

		// Gabungkan daftar host yang pernah ditemui oleh saya dan peer
		// Kita pakai HashSet supaya nggak ada duplikasi
		Set<DTNHost> hostSet = new HashSet<>(this.recentEncounters.size() + de.recentEncounters.size());
		hostSet.addAll(this.recentEncounters.keySet()); // Tambahkan semua host yang saya pernah temui
		hostSet.addAll(de.recentEncounters.keySet());  // Tambahkan semua host yang peer pernah temui

		// Update informasi encounter buat setiap host yang pernah kita temui
		for (DTNHost h : hostSet) {
			double myTime, peerTime;

			// Cek apakah saya punya catatan waktu terakhir bertemu host ini
			if (this.recentEncounters.containsKey(h))
				myTime = this.recentEncounters.get(h); // Ambil waktu terakhir saya bertemu host h
			else
				myTime = -1.0; // Kalau nggak ada, berarti saya belum pernah ketemu host ini

			// Lakukan hal yang sama buat peer
			if (de.recentEncounters.containsKey(h))
				peerTime = de.recentEncounters.get(h); // Ambil waktu terakhir peer bertemu host h
			else
				peerTime = -1.0;

			// Update catatan waktu encounter saya berdasarkan informasi dari peer
			if (myTime < 0.0 || myTime + myTimediff < peerTime)
				recentEncounters.put(h, peerTime - myTimediff);

			// Update catatan waktu encounter peer berdasarkan informasi dari saya
			if (peerTime < 0.0 || peerTime + peerTimediff < myTime)
				de.recentEncounters.put(h, myTime - peerTimediff);
		}
	}

	/**
	 * @param m Pesan baru yang akan dievaluasi.
	 * @return
	 */
	@Override
	public boolean newMessage(Message m) {
		// Oke, kita kasih properti tambahan ke pesan ini!
		// Kita tambahin jumlah salinan awal (initialNrofCopies) biar tahu berapa banyak pesan ini bisa disebarkan.
		m.addProperty(MSG_COUNT_PROP, initialNrofCopies);

		// Misinya sukses, kita return true buat kasih tahu kalau pesan ini berhasil diproses!
		return true;
	}


	/**
	 * @param m          Pesan yang diterima.
	 * @param targetHost Host yang akan diperiksa apakah merupakan tujuan akhir.
	 * @return
	 */
	@Override
	public boolean isFinalDest(Message m, DTNHost targetHost) {
		// Cek apakah target pengiriman pesan ini adalah si targetHost yang dimaksud?
		return m.getTo() == targetHost;

//		// Ambil tujuan akhir dari pesan ini
//		DTNHost messageDestination = m.getTo();
//
//		// Cek apakah tujuan pesan ini sama dengan targetHost yang diperiksa
//		if (messageDestination == targetHost) {
//			// Kalau iya, berarti pesan udah sampai di tujuan!
//			return true;
//		} else {
//			// Kalau belum, berarti masih harus lanjut perjalanan
//			return false;
//		}
	}

	/**
	 * @param m        Pesan yang diterima.
	 * @param thisHost Host lokal (yang menjalankan router ini).
	 * @return
	 */
	@Override
	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
		return m.getTo() != thisHost;
//		// Cek siapa tujuan akhirnya
//		DTNHost messageDestination = m.getTo();
//
//		// Kalau tujuan akhirnya BUKAN thisHost, berarti pesan masih perlu disimpan
//		if (messageDestination != thisHost) {
//			return true; // Simpan pesan karena bukan buat kita!
//		} else {
//			return false; // Gak perlu simpan, ini pesan buat kita sendiri!
//		}
	}

	/**
	 * @param m         Pesan yang akan dievaluasi untuk pengiriman.
	 * @param otherHost Host peer yang berpotensi menjadi tujuan pengiriman.
	 * @param thisHost  Host lokal (yang menjalankan router ini).
	 * @return
	 */
	@Override
	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
		// 1. Cek dulu, apakah si otherHost ini adalah tujuan akhir dari pesan
		if (m.getTo() == otherHost) return true; // Kalau iya, langsung kirim

		// 2. Ambil jumlah salinan pesan saat ini (berapa banyak "copy" pesan yang tersedia)
		int nrofCopies = (Integer) m.getProperty(MSG_COUNT_PROP);

		// Kalau masih ada lebih dari 1 copy, kita bisa bagi ke host lain
		if (nrofCopies > 1) return true;

		// 3. Ambil tujuan akhir dari pesan ini
		DTNHost dest = m.getTo();

		// 4. Ambil decision engine dari host tujuan kita
		SnFDecisionEngine de = this.getOtherSnFDecisionEngine(otherHost);

		// 5. Cek apakah otherHost pernah ketemu dengan tujuan pesan sebelumnya
		if (!de.recentEncounters.containsKey(dest))
			return false; // Kalau belum pernah ketemu, tidak perlu dikirim

		// 6. Kalau kita sendiri juga belum pernah ketemu dengan tujuan pesan, kita kasih saja ke otherHost
		if (!this.recentEncounters.containsKey(dest))
			return true;

		// 7. Ambil waktu terakhir kali kita dan otherHost melihat tujuan pesan
		double myLastSeen = this.recentEncounters.get(dest); // Waktu terakhir kali kita lihat tujuan pesan
		double peerLastSeen = de.recentEncounters.get(dest); // Waktu terakhir kali si otherHost lihat tujuan pesan

		// 8. Kirim pesan ke otherHost kalau dia lebih baru melihat tujuan pesan dibanding kita
		return (peerLastSeen > myLastSeen + transitivityTimerThreshold);
	}

	@Override
	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
		return false;
	}


	/**
	 * @param m         Pesan yang telah berhasil dikirim.
	 * @param otherHost Host peer yang menerima pesan.
	 * @return
	 */
	@Override
	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
		// Selalu hapus pesan setelah dikirim (sesuai dengan logika Spray and Focus)
		return true;
	}

	/**
	 * @param m                Pesan yang dianggap sudah lama.
	 * @param hostReportingOld Host peer yang melaporkan bahwa pesan tersebut sudah lama.
	 * @return
	 */
	@Override
	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
		// Selalu hapus pesan lama
		return true;
	}

	/**
	 * @param thisHost Host lokal (yang menjalankan router ini).
	 */
	@Override
	public void update(DTNHost thisHost) {
		// Tidak ada logika pemeliharaan rutin dalam implementasi ini
	}

	/**
	 * @return
	 */
	@Override
	public RoutingDecisionEngine replicate() {
		return new SnFDecisionEngine(this);
	}

	/**
	 * Fungsi buat ngambil Decision Engine dari host lain
	 * Jadi, kalau kita ingin tahu decision engine yang dipakai peer,
	 * kita bisa pakai method ini
	 */
	private SnFDecisionEngine getOtherSnFDecisionEngine(DTNHost h) {
		// Ambil router yang digunakan oleh host h
		MessageRouter otherRouter = h.getRouter();

		// Cek dulu, apakah router yang dipakai host ini adalah DecisionEngineRouter?
		// Kalau bukan, berarti nggak kompatibel dan kita lempar error
		if (!(otherRouter instanceof DecisionEngineRouter)) {
			throw new SimError("Router ini cuma bisa bekerja dengan DecisionEngineRouter");
		}

		// Kalau lolos, kita ubah router jadi DecisionEngineRouter
		DecisionEngineRouter deRouter = (DecisionEngineRouter) otherRouter;

		// Ambil decision engine dari router peer
		RoutingDecisionEngine otherEngine = deRouter.getDecisionEngine();

		// Cek lagi, apakah decision engine-nya tipe SnFDecisionEngine?
		// Kalau bukan, kita kasih error biar nggak terjadi kesalahan
		if (!(otherEngine instanceof SnFDecisionEngine)) {
			throw new SimError("DecisionEngineRouter harus dikonfigurasi dengan SnFDecisionEngine");
		}

		// Semua aman, return SnFDecisionEngine dari peer!
		return (SnFDecisionEngine) otherEngine;
	}

}