/*
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */
package routing.decisionengine;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import routing.RoutingDecisionEngine;

/**
 * @author PJ Dillon, University of Pittsburgh (original code)
 * @author Hendrowunga, Sanata Dharma University, Network Laboratory (modifications and comments)
 */
public class EpidemicDecisionEngine implements RoutingDecisionEngine {

	/**
	 * Konstruktor copy
	 */
	public EpidemicDecisionEngine(EpidemicDecisionEngine e) {
	}

	/**
	 * Konstruktor untuk EpidemicDecisionEngine yang menerima objek Settings.
	 * @param settings Objek Settings yang berisi konfigurasi.
	 */
	public EpidemicDecisionEngine(Settings settings) {
		// Tidak ada pengaturan khusus untuk Epidemic, jadi kita tidak perlu melakukan apa pun di sini.
	}

	/**
	 * Dipanggil saat koneksi terbentuk antara host ini dan peer.
	 * <p>
	 * Dalam implementasi Epidemic, tidak ada tindakan khusus yang perlu dilakukan saat koneksi naik,
	 * karena kita hanya perlu bertukar pesan yang tersedia.
	 * </p>
	 * @param thisHost Host yang menjalankan router ini (host lokal).
	 * @param peer     Host peer (host lain) yang terhubung.
	 */
	@Override
	public void connectionUp(DTNHost thisHost, DTNHost peer) {
		// Tidak ada tindakan khusus yang diperlukan saat koneksi naik.
	}

	/**
	 * Dipanggil saat koneksi terputus antara host ini dan peer.
	 * <p>
	 * Seperti halnya `connectionUp`, tidak ada tindakan khusus yang perlu dilakukan saat koneksi turun.
	 * </p>
	 * @param thisHost Host yang menjalankan router ini (host lokal).
	 * @param peer     Host peer (host lain) yang koneksinya terputus.
	 */
	@Override
	public void connectionDown(DTNHost thisHost, DTNHost peer) {
		// Tidak ada tindakan khusus yang diperlukan saat koneksi turun.
	}

	/**
	 * Dipanggil untuk melakukan pertukaran informasi saat koneksi baru dibuat.
	 * <p>
	 * Dalam Epidemic, kita tidak melakukan pertukaran informasi khusus di awal koneksi.
	 * Pertukaran pesan terjadi secara oportunistik saat `shouldSendMessageToHost` dipanggil.
	 * </p>
	 * @param con  Objek {@link Connection} yang merepresentasikan koneksi yang baru dibuat.
	 * @param peer Host peer (host lain) yang terhubung melalui koneksi ini.
	 */
@Override
	public void doExchangeForNewConnection(Connection con, DTNHost peer) {
		// Tidak ada pertukaran informasi khusus yang diperlukan saat koneksi baru dibuat.
	}

	/**
	 * Dipanggil saat pesan baru dibuat.
	 * <p>
	 * Dalam Epidemic, kita selalu menerima pesan baru untuk disebarkan di jaringan.
	 * </p>
	 * @param m Pesan baru yang akan dipertimbangkan untuk routing.
	 * @return `true` untuk menerima pesan dan menyimpannya dalam buffer, `false` untuk menolak pesan.
	 */
	@Override
	public boolean newMessage(Message m) {
		return true; // Selalu terima pesan baru
	}

	/**
	 * Menentukan apakah host tertentu adalah tujuan akhir dari pesan.
	 * @param m     Pesan yang baru diterima.
	 * @param aHost Host yang akan diperiksa apakah merupakan tujuan akhir.
	 * @return `true` jika host adalah tujuan akhir, `false` jika bukan.
	 */
	@Override
	public boolean isFinalDest(Message m, DTNHost aHost) {
		return m.getTo() == aHost; // Periksa apakah tujuan pesan sama dengan host yang diberikan
	}

	/**
	 * Menentukan apakah pesan yang baru diterima dari peer harus disimpan di buffer dan diteruskan lebih lanjut.
	 * <p>
	 * Dalam Epidemic, kita selalu menyimpan pesan yang belum kita miliki sebelumnya,
	 * dengan mempertimbangkan keterbatasan buffer (drop-oldest).
	 * </p>
	 * @param m        Pesan yang baru diterima dari peer.
	 * @param thisHost Host yang menjalankan router ini (host lokal).
	 * @return `true` jika pesan harus disimpan dan diteruskan, `false` jika tidak.
	 */
	@Override
	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
		return !thisHost.getRouter().hasMessage(m.getId()); // Simpan hanya jika kita belum memiliki pesan ini
	}

	/**
	 * Menentukan apakah pesan tertentu harus dikirim ke host lain.
	 * <p>
	 * Dalam Epidemic, kita ingin mengirim pesan *sebanyak mungkin* ke host lain,
	 * selama koneksi tersedia dan kita memiliki pesan yang belum dimiliki host lain.
	 * </p>
	 * @param m         Pesan yang akan dievaluasi untuk pengiriman.
	 * @param otherHost Host peer (host lain) yang berpotensi menjadi tujuan pengiriman.
	 * @param thisHost  Host yang menjalankan router ini (host lokal).
	 * @return `true` jika pesan harus dikirim, `false` jika tidak.
	 */
	@Override
	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
		return !otherHost.getRouter().hasMessage(m.getId()); // Kirim hanya jika host lain belum memiliki pesan ini
	}

	/**
	 * Dipanggil setelah pesan berhasil dikirim ke peer, untuk menentukan apakah pesan tersebut sekarang harus dihapus dari penyimpanan pesan.
	 * <p>
	 * Dalam implementasi Epidemic dasar, kita *tidak* menghapus pesan setelah dikirim.
	 * Kita terus menyebarkan pesan tersebut ke host lain.
	 * </p>
	 * @param m         Pesan yang telah berhasil dikirim.
	 * @param otherHost Host peer (host lain) yang menerima pesan.
	 * @return `true` jika pesan harus dihapus, `false` jika tidak.
	 */
	@Override
	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
		return false; // Jangan hapus pesan setelah dikirim
	}

	/**
	 * Dipanggil jika upaya pengiriman pesan ke peer gagal dan kode pengembalian menunjukkan
	 * bahwa pesan tersebut sudah lama atau sudah dikirim sebelumnya.
	 * <p>
	 * Dalam Epidemic, kita tidak menghapus pesan berdasarkan laporan dari host lain.
	 * Kita hanya menghapus pesan jika buffer penuh (drop-oldest).
	 * </p>
	 * @param m              Pesan yang dianggap sudah lama.
	 * @param hostReportingOld Host peer (host lain) yang melaporkan bahwa pesan tersebut sudah lama.
	 * @return `true` jika pesan harus dihapus, `false` jika tidak.
	 */
	@Override
	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
		return false; // Jangan hapus pesan berdasarkan laporan dari host lain
	}

	/**
	 * Dipanggil secara berkala untuk memberi kesempatan kepada Decision Engine untuk memperbarui status internalnya.
	 * <p>
	 * Dalam implementasi Epidemic dasar, kita tidak perlu melakukan pembaruan status khusus.
	 * </p>
	 * @param thisHost Host yang menjalankan router ini (host lokal).
	 */
	@Override
	public void update(DTNHost thisHost) {
		// Tidak ada pembaruan status khusus yang diperlukan.
	}

	/**
	 * Membuat duplikat (replika) dari Decision Engine ini.
	 * <p>
	 * Karena Epidemic tidak memiliki stateful data, kita dapat mengembalikan instance baru.
	 * </p>
	 * @return Salinan baru dari Decision Engine ini.
	 */
	@Override
	public RoutingDecisionEngine replicate() {
		return new EpidemicDecisionEngine(this);
	}
}