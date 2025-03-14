package routing;

import core.*;

/**
 * Antarmuka yang mendefinisikan bagaimana sebuah router membuat keputusan tentang
 * bagaimana pesan harus dikirimkan dalam jaringan.
 * <p>
 * Class yang mengimplementasikan antarmuka ini (disebut "Decision Engine") berisi
 * logika routing spesifik untuk algoritma routing tertentu (misalnya, Epidemic,
 * Spray and Wait). Decision EngineRouter menggunakan interface ini untuk
 * memisahkan logika routing dari mekanisme pengiriman pesan dasar.
 * </p>
 *
 * © 2025 Hendro Wunga, Sanata Dharma University, Network Laboratory
 */
public interface RoutingDecisionEngine {

	/**
	 * Dipanggil ketika sebuah koneksi (hubungan langsung) terbentuk antara host ini
	 * dan host lain (peer).
	 * <p>
	 * Memberi tahu Decision Engine bahwa koneksi baru telah terbentuk. Decision
	 * Engine dapat menggunakan informasi ini untuk memperbarui informasi routing,
	 * memulai pertukaran informasi dengan peer, atau melakukan tindakan lain
	 * yang relevan.
	 * </p>
	 * <p>
	 * Penting: Method `doExchangeForNewConnection()` mungkin dipanggil
	 * *sebelum* method ini dipanggil, untuk memungkinkan pertukaran informasi
	 * awal sebelum koneksi sepenuhnya siap.
	 * </p>
	 * @param thisHost Host lokal (yang menjalankan router ini).
	 * @param peer     Host peer (host lain) yang terhubung.
	 */
	public void connectionUp(DTNHost thisHost, DTNHost peer);

	/**
	 * Dipanggil ketika sebuah koneksi (hubungan langsung) terputus antara host ini
	 * dan host lain (peer).
	 * <p>
	 * Memberi tahu Decision Engine bahwa koneksi telah hilang. Decision Engine
	 * dapat menggunakan informasi ini untuk menghapus rute yang tidak valid,
	 * memperbarui informasi routing, atau melakukan tindakan lain yang relevan.
	 * </p>
	 * @param thisHost Host lokal (yang menjalankan router ini).
	 * @param peer     Host peer (host lain) yang koneksinya terputus.
	 */
	public void connectionDown(DTNHost thisHost, DTNHost peer);

	/**
	 * Dipanggil sekali untuk setiap koneksi baru yang terbentuk, untuk memungkinkan
	 * dua Decision Engine di kedua ujung koneksi bertukar informasi routing
	 * secara simultan.
	 * <p>
	 * Pertukaran informasi ini penting untuk menghindari masalah di mana satu host
	 * memperbarui informasinya berdasarkan informasi yang *baru* dari host lain,
	 * sementara host lain menggunakan informasi yang *lama*. Dengan pertukaran
	 * yang simultan, kedua host menggunakan informasi yang konsisten.
	 * </p>
	 * @param con  Objek {@link Connection} yang merepresentasikan koneksi baru.
	 * @param peer Host peer (host lain) yang terhubung melalui koneksi ini.
	 */
	public void doExchangeForNewConnection(Connection con, DTNHost peer);

	/**
	 * Dipanggil ketika sebuah pesan baru dibuat di host ini.
	 * <p>
	 * Memberi kesempatan kepada Decision Engine untuk memeriksa pesan dan memutuskan
	 * apakah pesan tersebut harus diterima dan diproses lebih lanjut, atau ditolak.
	 * </p>
	 * <p>
	 * Penting: Method ini hanya dipanggil untuk pesan yang *dibuat* oleh host ini,
	 * bukan pesan yang diterima dari peer.
	 * </p>
	 * @param m Pesan baru yang akan dievaluasi.
	 * @return `true` jika pesan harus diterima dan diproses lebih lanjut,
	 *         `false` jika pesan harus ditolak.
	 */
	public boolean newMessage(Message m);

	/**
	 * Menentukan apakah host tertentu (targetHost) adalah tujuan akhir dari pesan
	 * tertentu (m).
	 * <p>
	 * Dipanggil ketika sebuah pesan diterima, untuk menentukan apakah pesan telah
	 * mencapai tujuannya.
	 * </p>
	 * @param m          Pesan yang diterima.
	 * @param targetHost Host yang akan diperiksa apakah merupakan tujuan akhir.
	 * @return `true` jika host yang diberikan adalah tujuan akhir dari pesan,
	 *         `false` jika bukan.
	 */
	public boolean isFinalDest(Message m, DTNHost targetHost);

	/**
	 * Menentukan apakah pesan yang diterima dari peer harus disimpan dalam penyimpanan
	 * pesan host ini dan diproses lebih lanjut.
	 * <p>
	 * Memungkinkan Decision Engine untuk menerapkan kebijakan penyimpanan pesan,
	 * seperti membatasi penyimpanan pesan yang duplikat, pesan yang sudah kadaluarsa,
	 * atau pesan dari sumber yang tidak dipercaya.
	 * </p>
	 * @param m        Pesan yang diterima.
	 * @param thisHost Host lokal (yang menjalankan router ini).
	 * @return `true` jika pesan harus disimpan dan diproses lebih lanjut,
	 *         `false` jika tidak.
	 */
	public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost);

	/**
	 * Menentukan apakah pesan tertentu (m) harus dikirim ke host tertentu (otherHost).
	 * <p>
	 * Method ini merupakan inti dari logika routing. Decision Engine harus
	 * mempertimbangkan berbagai faktor, seperti konektivitas jaringan,
	 * kedekatan ke tujuan, jumlah salinan pesan yang sudah ada, dan kebijakan
	 * routing lainnya, untuk menentukan apakah pengiriman pesan ke host yang
	 * diberikan akan meningkatkan kemungkinan pengiriman atau mengurangi overhead.
	 * </p>
	 * @param m         Pesan yang akan dievaluasi untuk pengiriman.
	 * @param otherHost Host peer yang berpotensi menjadi tujuan pengiriman.
	 * @param thisHost  Host lokal (yang menjalankan router ini).
	 * @return `true` jika pesan harus dikirim ke host yang diberikan,
	 *         `false` jika tidak.
	 */
	public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost);

	/**
	 * Dipanggil setelah pesan berhasil dikirim ke peer, untuk menentukan apakah pesan
	 * tersebut sekarang harus dihapus dari penyimpanan pesan host ini.
	 * <p>
	 * Memungkinkan Decision Engine untuk menerapkan kebijakan penyimpanan pesan,
	 * seperti menghapus pesan setelah berhasil dikirim ke tujuan akhir, atau setelah
	 * sejumlah salinan pesan telah tersebar.
	 * </p>
	 * @param m         Pesan yang telah berhasil dikirim.
	 * @param otherHost Host peer yang menerima pesan.
	 * @return `true` jika pesan harus dihapus dari penyimpanan pesan host ini,
	 *         `false` jika tidak.
	 */
	public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost);

	/**
	 * Dipanggil jika upaya pengiriman pesan ke peer gagal dan kode pengembalian
	 * menunjukkan bahwa pesan tersebut sudah lama atau sudah dikirim sebelumnya.
	 * Dalam kasus ini, pesan mungkin perlu dihapus.
	 * <p>
	 * Memberi kesempatan kepada Decision Engine untuk membersihkan pesan-pesan
	 * yang tidak lagi berguna, berdasarkan laporan dari peer lain.
	 * </p>
	 * @param m              Pesan yang dianggap sudah lama.
	 * @param hostReportingOld Host peer yang melaporkan bahwa pesan tersebut sudah lama.
	 * @return `true` jika pesan harus dihapus dari penyimpanan pesan host ini,
	 *         `false` jika tidak.
	 */
	public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld);

	/**
	 * Dipanggil secara berkala untuk memberi kesempatan kepada Decision Engine untuk
	 * memperbarui state internalnya, melakukan pembersihan berkala, atau melakukan
	 * tindakan pemeliharaan lainnya.
	 * @param thisHost Host lokal (yang menjalankan router ini).
	 */
	public void update(DTNHost thisHost);

	/**
	 * Membuat duplikat (replika) dari Decision Engine ini.
	 * <p>
	 * Penting karena setiap host dalam simulasi akan memiliki instance Decision
	 * Engine sendiri.
	 * </p>
	 * @return Salinan baru dari Decision Engine ini.
	 */
	public RoutingDecisionEngine replicate();
}