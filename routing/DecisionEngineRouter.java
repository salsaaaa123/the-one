package routing; // Mendeklarasikan package tempat class ini berada, yaitu 'routing'. Package ini mengorganisir class-class terkait routing dalam struktur direktori.

import java.util.*; // Mengimpor semua class dari package java.util, yang berisi berbagai class utilitas seperti List, Map, Set, dan Iterator. Ini memberikan akses ke struktur data koleksi dan fungsionalitas utilitas lainnya.

import core.*; // Mengimpor semua class dari package core, yang diasumsikan berisi class-class inti untuk simulasi jaringan seperti Message, DTNHost, Connection. Ini menyediakan elemen dasar dari lingkungan simulasi.

/**
 * Kelas ini menimpa ActiveRouter untuk menyuntikkan panggilan ke objek
 * DecisionEngine (Mesin Pengambil Keputusan) di mana diperlukan dan mengekstrak
 * sebanyak mungkin kode dari method update().
 *
 * <strong>Logika Penerusan (Forwarding):</strong>
 * <p>
 * DecisionEngineRouter memelihara List dari Tuple<Message, Connection> untuk
 * mendukung panggilan ke ActiveRouter.tryMessagesForConnected() di dalam
 * DecisionEngineRouter.update(). Karena update() dipanggil begitu sering, kita
 * ingin sesedikit mungkin komputasi dilakukan di dalamnya; oleh karena itu,
 * List tersebut diperbarui ketika terjadi event. Empat event menyebabkan List
 * diperbarui: pesan baru dari host ini, pesan baru yang diterima, koneksi
 * naik, atau koneksi turun. Pada pesan baru (baik dari host ini atau diterima
 * dari peer), koleksi koneksi terbuka diperiksa untuk melihat apakah pesan
 * harus diteruskan melalui koneksi tersebut. Jika ya, Tuple baru ditambahkan
 * ke List. Ketika koneksi naik, koleksi pesan diperiksa untuk menentukan
 * apakah ada pesan yang harus dikirim ke peer baru ini, menambahkan Tuple
 * ke list jika ya. Ketika koneksi turun, setiap Tuple dalam list yang
 * terkait dengan koneksi tersebut dihapus dari List.
 *
 * <strong>Mesin Pengambil Keputusan (Decision Engines)</strong>
 * <p>
 * Sebagian besar (jika tidak semua) pengambilan keputusan routing disediakan
 * oleh objek RoutingDecisionEngine. Antarmuka DecisionEngine mendefinisikan
 * method yang melakukan komputasi dan mengembalikan keputusan sebagai berikut:
 *
 * <ul>
 *   <li>Dalam createNewMessage(), panggilan ke RoutingDecisionEngine.newMessage()
 *    dilakukan. Nilai kembalian true mengindikasikan bahwa pesan harus
 *    ditambahkan ke penyimpanan pesan untuk routing. Nilai false
 *    mengindikasikan pesan harus dibuang.
 *   </li>
 *   <li>changedConnection() mengindikasikan koneksi naik atau turun. Method
 *   connectionUp() atau connectionDown() yang sesuai dipanggil pada objek
 *   RoutingDecisionEngine. Juga, pada event koneksi naik, peer pertama yang
 *   memanggil changedConnection() juga akan memanggil
 *   RoutingDecisionEngine.doExchangeForNewConnection() sehingga kedua objek
 *   decision engine dapat secara bersamaan bertukar informasi dan memperbarui
 *   tabel routing mereka (tanpa takut method ini dipanggil kedua kali).
 *   </li>
 *   <li>Memulai transfer Message, protokol pertama kali bertanya kepada peer
 *   tetangga apakah boleh mengirim Message. Jika peer mengindikasikan bahwa
 *   Message sudah LAMA atau TERKIRIM, panggilan ke
 *   RoutingDecisionEngine.shouldDeleteOldMessage() dilakukan untuk
 *   menentukan apakah Message harus dihapus dari penyimpanan pesan.
 *   <em>Catatan: jika tombstone diaktifkan atau deleteDelivered dinonaktifkan,
 *   Message akan dihapus dan tidak ada panggilan ke method ini yang akan
 *   dilakukan.</em>
 *   </li>
 *   <li>Ketika pesan diterima (dalam messageTransferred), panggilan ke
 *   RoutingDecisionEngine.isFinalDest() dilakukan untuk menentukan apakah host
 *   penerima (ini) adalah penerima yang dimaksudkan dari Message. Selanjutnya,
 *   panggilan ke RoutingDecisionEngine.shouldSaveReceivedMessage() dilakukan
 *   untuk menentukan apakah pesan baru harus disimpan dan upaya untuk
 *   meneruskannya harus dilakukan. Jika ya, set Koneksi diperiksa untuk
 *   peluang transfer seperti yang dijelaskan di atas.
 *   </li>
 *   <li>Ketika pesan dikirim (dalam transferDone()), panggilan ke
 *   RoutingDecisionEngine.shouldDeleteSentMessage() dilakukan untuk
 *   bertanya apakah Message yang telah dikirim dan sekarang berada di peer
 *   harus dihapus dari penyimpanan pesan.
 *   </li>
 * </ul>
 *
 * <strong>Tombstone</strong>
 * <p>
 * ONE memiliki opsi deleteDelivered yang memungkinkan host menghapus pesan
 * jika ia bersentuhan dengan tujuan pesan. Pendekatan yang lebih agresif
 * memungkinkan host untuk mengingat bahwa pesan tertentu telah dikirimkan
 * dengan menyimpan ID pesan dalam list pesan terkirim (yang disebut list
 * tombstone di sini). Setiap kali node mencoba mengirim pesan ke host yang
 * memiliki tombstone untuk pesan tersebut, node pengirim menerima tombstone
 * tersebut.
 *
 * @author PJ Dillon, University of Pittsburgh
 */
public class DecisionEngineRouter extends ActiveRouter // Mendeklarasikan class DecisionEngineRouter, yang merupakan subclass dari ActiveRouter. Class ini mewarisi fungsionalitas routing dasar dan menambahkan logika berbasis decision engine.
{
     public static final String PUBSUB_NS = "DecisionEngineRouter"; // Mendeklarasikan konstanta string untuk namespace pub/sub (publish/subscribe), digunakan untuk mengidentifikasi konfigurasi router ini.
     public static final String ENGINE_SETTING = "decisionEngine"; // Mendeklarasikan konstanta string untuk kunci setting yang mengidentifikasi decision engine yang digunakan.
     public static final String TOMBSTONE_SETTING = "tombstones"; // Mendeklarasikan konstanta string untuk kunci setting yang mengidentifikasi apakah tombstone diaktifkan atau tidak.
     public static final String CONNECTION_STATE_SETTING = ""; // Mendeklarasikan konstanta string kosong yang tampaknya untuk kunci setting terkait status koneksi, tapi saat ini tidak digunakan.

     protected boolean tombstoning; // Mendeklarasikan variabel instance protected bertipe boolean untuk menentukan apakah mekanisme tombstone diaktifkan. Protected berarti hanya class ini dan subclass-nya yang dapat mengaksesnya.
     protected RoutingDecisionEngine decider; // Mendeklarasikan variabel instance protected bertipe RoutingDecisionEngine, yang memegang objek decision engine yang bertanggung jawab untuk keputusan routing.
     protected List<Tuple<Message, Connection>> outgoingMessages; // Mendeklarasikan variabel instance protected berupa List yang menyimpan Tuple dari Message dan Connection, mewakili pesan yang siap dikirim beserta koneksinya.

     protected Set<String> tombstones; // Mendeklarasikan variabel instance protected berupa Set untuk menyimpan ID pesan yang sudah pernah dikirim/diterima, yaitu tombstone. Menggunakan Set untuk memastikan keunikan ID.

     /**
      * Used to save state machine when new connections are made. See comment in
      * changedConnection()
      */
     protected Map<Connection, Integer> conStates; // Mendeklarasikan variabel instance protected berupa Map yang memetakan Connection ke Integer, digunakan untuk menyimpan status koneksi, terutama untuk pertukaran informasi routing.

     public DecisionEngineRouter(Settings s) // Mendeklarasikan konstruktor DecisionEngineRouter yang menerima objek Settings.
     {
          super(s); // Memanggil konstruktor superclass ActiveRouter dengan objek Settings yang sama, untuk menginisialisasi state ActiveRouter.

          Settings routeSettings = new Settings(PUBSUB_NS); // Membuat objek Settings baru dengan namespace DecisionEngineRouter, untuk membaca konfigurasi khusus untuk router ini.

          outgoingMessages = new LinkedList<Tuple<Message, Connection>>(); // Menginisialisasi list outgoingMessages sebagai LinkedList kosong.

          decider = (RoutingDecisionEngine) routeSettings.createIntializedObject( // Membuat objek decision engine dengan menggunakan objek Settings. Objek Settings digunakan untuk membuat instance class RoutingDecisionEngine sesuai setting.
                  "routing." + routeSettings.getSetting(ENGINE_SETTING)); // Mendapatkan nama class decision engine dari setting dan menginisialisasi objek tersebut.

          if (routeSettings.contains(TOMBSTONE_SETTING)) // Memeriksa apakah setting tombstone ada pada objek routeSettings
               tombstoning = routeSettings.getBoolean(TOMBSTONE_SETTING); // Jika ada, set nilai tombstoning sesuai setting tersebut
          else
               tombstoning = false; // Jika tidak ada, set nilai tombstoning ke false

          if (tombstoning) // Jika tombstone diaktifkan
               tombstones = new HashSet<String>(10); // Inisialisasi set tombstone dengan kapasitas awal 10.
          conStates = new HashMap<Connection, Integer>(4); // Inisialisasi map status koneksi dengan kapasitas awal 4.
     }

     public DecisionEngineRouter(DecisionEngineRouter r) // Mendeklarasikan copy constructor yang menerima objek DecisionEngineRouter lain.
     {
          super(r); // Memanggil copy constructor dari superclass ActiveRouter
          outgoingMessages = new LinkedList<Tuple<Message, Connection>>(); // Inisialisasi list outgoingMessages
          decider = r.decider.replicate(); // Membuat replika dari decision engine dari router lain
          tombstoning = r.tombstoning; // Menyalin setting tombstoning

          if (this.tombstoning) // Jika tombstone diaktifkan
               tombstones = new HashSet<String>(10); // Inisialisasi set tombstone dengan kapasitas awal 10.
          conStates = new HashMap<Connection, Integer>(4); // Inisialisasi map status koneksi dengan kapasitas awal 4.
     }

     //@Override
     public MessageRouter replicate() // Mendeklarasikan method replicate, bertujuan untuk membuat salinan dari router ini.
     {
          return new DecisionEngineRouter(this); // Mengembalikan instance baru dari DecisionEngineRouter dengan memanggil copy constructor.
     }

     @Override // Menandakan bahwa method ini menimpa method dari superclass.
     public boolean createNewMessage(Message m) // Mendeklarasikan method untuk menangani pesan baru yang dibuat oleh node ini
     {
          if (decider.newMessage(m)) // Memanggil method newMessage dari decider untuk menentukan apakah pesan harus disimpan dan diteruskan.
          {
               if (m.getId().equals("M14")) // Memeriksa apakah id pesan adalah M14
                    System.out.println("Host: " + getHost() + "Creating M14"); // Jika iya, mencetak log ke console.
               makeRoomForNewMessage(m.getSize()); // Memastikan ada ruang untuk pesan baru di penyimpanan
               m.setTtl(this.msgTtl); // Menetapkan TTL (Time To Live) pesan sesuai setting
               addToMessages(m, true); // Menambahkan pesan ke daftar pesan, menandai pesan ini sebagai pesan yang dibuat oleh host ini.

               findConnectionsForNewMessage(m, getHost()); // Mencari koneksi yang cocok untuk mengirimkan pesan ini
               return true; // Mengembalikan true jika pesan berhasil ditambahkan dan dicari koneksi.
          }
          return false; // Mengembalikan false jika decider tidak ingin menyimpan pesan ini.
     }


     @Override // Menandakan bahwa method ini menimpa method dari superclass.
     public void connectionUp(Connection con) // Mendeklarasikan method untuk menangani ketika koneksi baru terbentuk.
     {
          DTNHost myHost = getHost(); // Mendapatkan host lokal.
          DTNHost otherNode = con.getOtherNode(myHost); // Mendapatkan host lain yang terhubung.
          DecisionEngineRouter otherRouter = (DecisionEngineRouter) otherNode.getRouter(); // Mendapatkan router dari host lain

          decider.connectionUp(myHost, otherNode); // Memberi tahu decider bahwa koneksi baru terbentuk.

          /*
           * Bagian ini sedikit membingungkan karena ada masalah yang harus kita
           * hindari. Ketika koneksi terbentuk, kita berasumsi bahwa kedua host yang
           * sekarang terhubung akan bertukar beberapa informasi routing dan
           * memperbarui informasi routing mereka sendiri berdasarkan apa yang mereka
           * dapatkan dari peer. Jadi, host A memperbarui tabel routingnya dengan
           * informasi dari host B, dan sebaliknya. Di dunia nyata, A akan mengirim
           * informasi routing *lama*-nya ke B dan menghitung informasi routing baru
           * kemudian setelah menerima informasi routing *lama* dari B. Di ONE,
           * changedConnection() dipanggil dua kali, sekali untuk setiap host A dan
           * B, secara serial. Jika dipanggil untuk A terlebih dahulu, A menggunakan
           * informasi lama B untuk menghitung informasi barunya, tetapi B kemudian
           * menggunakan informasi *baru* A untuk menghitung informasi barunya....
           * dan ini dapat menyebabkan beberapa masalah yang tidak diinginkan.
           *
           * Untuk mengatasi ini, host mana pun yang memanggil changedConnection()
           * pertama kali akan memanggil doExchange() sekali. doExchange()
           * berinteraksi dengan DecisionEngine untuk memulai pertukaran informasi,
           * dan diasumsikan bahwa kode ini akan memperbarui informasi pada kedua
           * peer secara bersamaan menggunakan informasi lama dari kedua peer.
           */
          if (shouldNotifyPeer(con)) // Memeriksa apakah perlu melakukan pertukaran informasi dengan host lain.
          {
               this.doExchange(con, otherNode); // Memulai proses pertukaran informasi dengan host lain.
               otherRouter.didExchange(con); // Memberitahu host lain bahwa pertukaran informasi sudah dilakukan.
          }

          /*
           * Once we have new information computed for the peer, we figure out if
           * there are any messages that should get sent to this peer.
           */
          Collection<Message> msgs = getMessageCollection(); // Mendapatkan semua pesan yang disimpan pada host ini.
          for (Message m : msgs) // Loop melalui semua pesan.
          {
               if (decider.shouldSendMessageToHost(m, otherNode)) // Jika decider memutuskan pesan ini harus dikirim ke host lain
                    outgoingMessages.add(new Tuple<Message, Connection>(m, con)); // Tambahkan pesan dan koneksinya ke list outgoingMessages.
          }
     }


     @Override  // Menandakan bahwa method ini menimpa method dari superclass.
     public void connectionDown(Connection con) // Mendeklarasikan method untuk menangani ketika koneksi terputus.
     {
          DTNHost myHost = getHost(); // Mendapatkan host lokal.
          DTNHost otherNode = con.getOtherNode(myHost); // Mendapatkan host lain yang dulunya terhubung.
          //DecisionEngineRouter otherRouter = (DecisionEngineRouter)otherNode.getRouter();

          decider.connectionDown(myHost, otherNode); // Memberi tahu decider bahwa koneksi terputus.

          conStates.remove(con); // Menghapus status koneksi dari map conStates

          /*
           * If we  were trying to send message to this peer, we need to remove them
           * from the outgoing List.
           */
          for (Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); // Loop melalui semua pesan pada list outgoingMessages
               i.hasNext(); ) {
               Tuple<Message, Connection> t = i.next(); // Mendapatkan tuple pesan dan koneksi
               if (t.getValue() == con) // Jika koneksi tuple sama dengan koneksi yang terputus
                    i.remove(); // Hapus tuple tersebut dari list.
          }
     }

	/*@Override
	public void changedConnection(Connection con)
	{
		DTNHost myHost = getHost();
		DTNHost otherNode = con.getOtherNode(myHost);
		DecisionEngineRouter otherRouter = (DecisionEngineRouter)otherNode.getRouter();
		if(con.isUp())
		{
			decider.connectionUp(myHost, otherNode);

			        /*
         * Bagian ini sedikit membingungkan karena ada masalah yang harus kita
         * hindari. Ketika koneksi terbentuk, kita berasumsi bahwa kedua host yang
         * sekarang terhubung akan bertukar beberapa informasi routing dan
         * memperbarui informasi routing mereka sendiri berdasarkan apa yang mereka
         * dapatkan dari peer. Jadi, host A memperbarui tabel routingnya dengan
         * informasi dari host B, dan sebaliknya. Di dunia nyata, A akan mengirim
         * informasi routing *lama*-nya ke B dan menghitung informasi routing baru
         * kemudian setelah menerima informasi routing *lama* dari B. Di ONE,
         * changedConnection() dipanggil dua kali, sekali untuk setiap host A dan
         * B, secara serial. Jika dipanggil untuk A terlebih dahulu, A menggunakan
         * informasi lama B untuk menghitung informasi barunya, tetapi B kemudian
         * menggunakan informasi *baru* A untuk menghitung informasi barunya....
         * dan ini dapat menyebabkan beberapa masalah yang tidak diinginkan.
         *
         * Untuk mengatasi ini, host mana pun yang memanggil changedConnection()
         * pertama kali akan memanggil doExchange() sekali. doExchange()
         * berinteraksi dengan DecisionEngine untuk memulai pertukaran informasi,
         * dan diasumsikan bahwa kode ini akan memperbarui informasi pada kedua
         * peer secara bersamaan menggunakan informasi lama dari kedua peer.

			if(shouldNotifyPeer(con))
			{
				this.doExchange(con, otherNode);
				otherRouter.didExchange(con);
			}

			/*
			 * Once we have new information computed for the peer, we figure out if
			 * there are any messages that should get sent to this peer.
			 *
			Collection<Message> msgs = getMessageCollection();
			for(Message m : msgs)
			{
				if(decider.shouldSendMessageToHost(m, otherNode))
					outgoingMessages.add(new Tuple<Message,Connection>(m, con));
			}
		}
		else
		{
			decider.connectionDown(myHost, otherNode);

			conStates.remove(con);

			/*
			 * If we  were trying to send message to this peer, we need to remove them
			 * from the outgoing List.
			 *
			for(Iterator<Tuple<Message,Connection>> i = outgoingMessages.iterator();
					i.hasNext();)
			{
				Tuple<Message, Connection> t = i.next();
				if(t.getValue() == con)
					i.remove();
			}
		}
	}*/

     protected void doExchange(Connection con, DTNHost otherHost) // Mendeklarasikan method protected untuk melakukan pertukaran informasi dengan host lain
     {
          conStates.put(con, 1); // Menandai bahwa pertukaran informasi sudah dilakukan
          decider.doExchangeForNewConnection(con, otherHost); // Memanggil method doExchangeForNewConnection pada decider untuk memulai pertukaran informasi.
     }

     /**
      * Dipanggil oleh DecisionEngineRouter peer untuk mengindikasikan bahwa ia
      * telah melakukan pertukaran informasi untuk koneksi yang diberikan.
      *
      * @param con Koneksi di mana pertukaran dilakukan
      */
     protected void didExchange(Connection con) // Mendeklarasikan method protected yang dipanggil oleh host lain untuk menginformasikan bahwa pertukaran informasi sudah selesai.
     {
          conStates.put(con, 1); // Menandai bahwa pertukaran informasi sudah dilakukan
     }

     @Override // Menandakan bahwa method ini menimpa method dari superclass.
     protected int startTransfer(Message m, Connection con) // Mendeklarasikan method protected untuk memulai transfer pesan.
     {
          int retVal; // Mendeklarasikan variabel untuk menyimpan return value.

          if (!con.isReadyForTransfer()) { // Memeriksa apakah koneksi sudah siap untuk melakukan transfer.
               return TRY_LATER_BUSY; // Mengembalikan status TRY_LATER_BUSY jika koneksi belum siap.
          }

          retVal = con.startTransfer(getHost(), m); // Memulai transfer pesan menggunakan method dari objek connection.
          if (retVal == RCV_OK) { // Jika transfer pesan berhasil dimulai
               addToSendingConnections(con); // Tambahkan koneksi ke list koneksi yang sedang mengirim.
          } else if (tombstoning && retVal == DENIED_DELIVERED) // Jika tombstone diaktifkan dan pesan ditolak karena sudah terkirim
          {
               this.deleteMessage(m.getId(), false); // Hapus pesan dari penyimpanan.
               tombstones.add(m.getId()); // Tambahkan id pesan ke daftar tombstone.
          } else if (deleteDelivered && (retVal == DENIED_OLD || retVal == DENIED_DELIVERED) && // Jika deleteDelivered diaktifkan dan pesan ditolak karena sudah terlalu lama atau sudah terkirim dan decider setuju pesan untuk dihapus
                  decider.shouldDeleteOldMessage(m, con.getOtherNode(getHost()))) { // Memanggil decider untuk memutuskan apakah pesan harus dihapus.
               /* final recipient has already received the msg -> delete it */
               if (m.getId().equals("M14")) // Memeriksa apakah pesan adalah M14
                    System.out.println("Host: " + getHost() + " told to delete M14"); // Mencetak log ke console
               this.deleteMessage(m.getId(), false); // Hapus pesan dari penyimpanan.
          }

          return retVal; // Mengembalikan status dari hasil transfer.
     }

     @Override  // Menandakan bahwa method ini menimpa method dari superclass.
     public int receiveMessage(Message m, DTNHost from) // Mendeklarasikan method public untuk menerima pesan dari node lain
     {
          if (isDeliveredMessage(m) || (tombstoning && tombstones.contains(m.getId()))) // Memeriksa apakah pesan sudah pernah diterima atau ada pada list tombstone
               return DENIED_DELIVERED; // Jika iya, tolak pesan.

          return super.receiveMessage(m, from); // Jika belum, proses pesan lebih lanjut menggunakan method dari superclass.
     }

     @Override  // Menandakan bahwa method ini menimpa method dari superclass.
     public Message messageTransferred(String id, DTNHost from) // Mendeklarasikan method public yang dipanggil setelah pesan berhasil diterima.
     {
          Message incoming = removeFromIncomingBuffer(id, from); // Mengambil pesan dari buffer incoming.

          if (incoming == null) { // Jika pesan tidak ada di buffer incoming
               throw new SimError("No message with ID " + id + " in the incoming " + // Melempar error.
                       "buffer of " + getHost());
          }

          incoming.setReceiveTime(SimClock.getTime()); // Menetapkan waktu diterima pada pesan.

          Message outgoing = incoming; // Membuat variabel untuk menyimpan output dari aplikasi
          for (Application app : getApplications(incoming.getAppID())) { // Loop melalui semua aplikasi yang terdaftar untuk pesan ini
               // Note that the order of applications is significant
               // since the next one gets the output of the previous.
               outgoing = app.handle(outgoing, getHost()); // Jalankan aplikasi pada pesan
               if (outgoing == null) break; // Jika aplikasi mengembalikan null, hentikan proses aplikasi
          }

          Message aMessage = (outgoing == null) ? (incoming) : (outgoing); // Mendapatkan pesan yang telah diproses

          boolean isFinalRecipient = decider.isFinalDest(aMessage, getHost()); // Memeriksa apakah host ini adalah tujuan akhir dari pesan ini
          boolean isFirstDelivery = isFinalRecipient &&
                  !isDeliveredMessage(aMessage); // Memeriksa apakah pesan ini baru pertama kali diterima di tujuan akhir

          if (outgoing != null && decider.shouldSaveReceivedMessage(aMessage, getHost())) // Jika pesan tidak null dan decider memutuskan pesan harus disimpan dan diteruskan
          {
               // not the final recipient and app doesn't want to drop the message
               // -> put to buffer
               addToMessages(aMessage, false); // Tambahkan pesan ke daftar pesan.

               // Determine any other connections to which to forward a message
               findConnectionsForNewMessage(aMessage, from); // Cari koneksi lain untuk meneruskan pesan.
          }

          if (isFirstDelivery) // Jika ini adalah penerimaan pertama pesan di tujuan akhir
          {
               this.deliveredMessages.put(id, aMessage); // Tambahkan pesan ke daftar pesan yang sudah diterima.
          }

          for (MessageListener ml : this.mListeners) { // Loop melalui semua listener pesan
               ml.messageTransferred(aMessage, from, getHost(), // Beritahu semua listener pesan bahwa pesan sudah diterima.
                       isFirstDelivery);
          }

          return aMessage; // Mengembalikan pesan yang telah diproses.
     }

     @Override  // Menandakan bahwa method ini menimpa method dari superclass.
     protected void transferDone(Connection con) // Mendeklarasikan method protected yang dipanggil ketika transfer pesan selesai.
     {
          Message transferred = this.getMessage(con.getMessage().getId()); // Mendapatkan pesan yang sudah dikirim.

          for (Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); // Loop melalui semua pesan di list outgoingMessages
               i.hasNext(); ) {
               Tuple<Message, Connection> t = i.next(); // Mendapatkan tuple pesan dan koneksi
               if (t.getKey().getId().equals(transferred.getId()) && // Jika id pesan dan koneksinya cocok dengan pesan yang sudah dikirim.
                       t.getValue().equals(con)) {
                    i.remove(); // Hapus tuple dari list.
                    break; // Hentikan loop.
               }
          }

          if (decider.shouldDeleteSentMessage(transferred, con.getOtherNode(getHost()))) // Jika decider memutuskan pesan yang sudah dikirim harus dihapus
          {
               if (transferred.getId().equals("M14")) // Jika id pesan adalah M14
                    System.out.println("Host: " + getHost() + " deleting M14 after transfer"); // Cetak log ke console.
               this.deleteMessage(transferred.getId(), false); // Hapus pesan dari penyimpanan.

               for (Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); // Loop melalui semua pesan di list outgoingMessages
                    i.hasNext(); ) {
                    Tuple<Message, Connection> t = i.next(); // Mendapatkan tuple pesan dan koneksi
                    if (t.getKey().getId().equals(transferred.getId())) // Jika id pesan sama dengan pesan yang sudah dikirim.
                    {
                         i.remove(); // Hapus tuple dari list.
                    }
               }
          }
     }

     @Override  // Menandakan bahwa method ini menimpa method dari superclass.
     public void update() // Mendeklarasikan method public untuk melakukan update routing berkala.
     {
          super.update(); // Memanggil method update dari superclass.
          if (!canStartTransfer() || isTransferring()) { // Memeriksa apakah node siap melakukan transfer pesan.
               return; // Jika tidak, hentikan proses update.
          }

          tryMessagesForConnected(outgoingMessages); // Mencoba mengirim pesan yang ada di list outgoingMessages melalui koneksi yang aktif.

          for (Iterator<Tuple<Message, Connection>> i = outgoingMessages.iterator(); // Loop melalui semua pesan pada list outgoingMessages.
               i.hasNext(); ) {
               Tuple<Message, Connection> t = i.next(); // Mendapatkan tuple pesan dan koneksi.
               if (!this.hasMessage(t.getKey().getId())) // Jika pesan yang ada pada tuple sudah tidak ada pada node.
               {
                    i.remove(); // Hapus tuple dari list.
               }
          }
     }

     public RoutingDecisionEngine getDecisionEngine() // Mendeklarasikan method public untuk mengembalikan objek RoutingDecisionEngine
     {
          return this.decider; // Mengembalikan objek decider.
     }

     protected boolean shouldNotifyPeer(Connection con) // Mendeklarasikan method protected untuk memeriksa apakah host lain perlu di notifikasi tentang adanya koneksi baru
     {
          Integer i = conStates.get(con); // Mendapatkan status koneksi.
          return i == null || i < 1; // Mengembalikan true jika status koneksi belum pernah disimpan atau kurang dari 1, jika tidak maka return false.
     }

     protected void findConnectionsForNewMessage(Message m, DTNHost from) // Mendeklarasikan method protected untuk mencari koneksi yang sesuai untuk pesan baru.
     {
          for (Connection c : getHost()) // Loop melalui semua koneksi dari host ini.
          //for(Connection c : getConnections())
          {
               DTNHost other = c.getOtherNode(getHost()); // Mendapatkan host lain yang terhubung melalui koneksi.
               if (other != from && decider.shouldSendMessageToHost(m, other)) // Jika host lain bukan host asal pengirim pesan dan jika decider setuju pesan ini untuk dikirim ke host tersebut
               {
                    if (m.getId().equals("M14")) // Jika pesan adalah M14
                         System.out.println("Adding attempt for M14 from: " + getHost() + " to: " + other); // Cetak log ke console.
                    outgoingMessages.add(new Tuple<Message, Connection>(m, c)); // Tambahkan pesan dan koneksi ke list outgoingMessages.
               }
          }
     }
}