<h1 align="center">🚀 Config ConnectivityDtnsim2Report 📡</h1>


## Scenario Settings

Penjelasan konfigurasi untuk pengaturan skenario.

### 1. Scenario.name = `connectivity_dtn_scenario`
- **Deskripsi**: Memberi nama skenario "connectivity_dtn_scenario". Nama ini akan digunakan dalam nama file laporan (jika ada).

### 2. Scenario.simulateConnections = `true`
- **Deskripsi**: Mengaktifkan simulasi koneksi. Jika disetel ke `false`, koneksi tidak akan dideteksi.

### 3. Scenario.updateInterval = `0.1`
- **Deskripsi**: Menentukan bahwa ONE akan memperbarui simulasi setiap 0.1 detik simulasi. Semakin kecil interval ini, semakin akurat tetapi simulasi juga akan berjalan lebih lambat.

### 4. Scenario.endTime = `43200`
- **Deskripsi**: Durasi simulasi selama 43200 detik (12 jam).




## "Bluetooth" interface for all nodes
### 5. btInterface.type = SimpleBroadcastInterface
- **Deskripsi**: Menentukan bahwa interface menggunakan class SimpleBroadcastInterface.
## Transmit speed of 2 Mbps = 250kBps
### 6. btInterface.transmitSpeed = 250k
- **Deskripsi**: Kecepatan transmisi interface adalah 250 kilobyte per detik.
### 7. btInterface.transmitRange = 10
- **Deskripsi**: Jangkauan transmisi interface adalah 10 meter.
## Define 2 different node groups
### 8. Scenario.nrofHostGroups = 2
- **Deskripsi**: Menentukan bahwa akan ada dua grup node yang berpartisipasi dalam simulasi.
## Common settings for all groups
### 9. Group.movementModel = RandomWaypoint
- **Deskripsi**: Model pergerakan setiap node adalah RandomWaypoint, setiap node berpindah secara acak ke titik tujuan.
### 10. Group.router = DirectDeliveryRouter
- **Deskripsi**: Jadi, node hanya mengirim pesan langsung ke tujuan dan tanpa forward.
### 11. Group.bufferSize = 5M
- **Deskripsi**: Ukuran buffer setiap node adalah 5 Megabyte.
### 12. Group.waitTime = 0, 60
- **Deskripsi**: Waktu tunggu ketika node berhenti setelah mencapai titik tujuan adalah acak antara 0 hingga 60 detik.
## All nodes have the bluetooth interface
### 13.Group.nrofInterfaces = 1
- **Deskripsi**: Setiap node hanya memiliki satu interface
### 14. Group.interface1 = btInterface
- **Deskripsi**: Setiap node menggunakan interface btInterface yang telah didefinisikan.
## Walking speeds
### 15. Group.speed = 0.5, 1.5
- **Deskripsi**: Kecepatan pergerakan setiap node acak antara 0.5 hingga 1.5 meter per detik.

### 16. Group.nrofHosts = 20
- **Deskripsi**: Jumlah node dalam grup adalah 20.
## group1 specific settings
### 17. Group1.groupID = a
- **Deskripsi**: Grup pertama memiliki ID "a".

## group2 specific settings
### 18.Group2.groupID = b
- **Deskripsi**: Grup kedua memiliki ID "b".

## How many event generators
### 19. Events.nrof = 0
- **Deskripsi**: Menonaktifkan event generator sehingga tidak ada pesan yang dibuat.

## Movement model settings
### seed for movement models' pseudo random number generator (default = 0)
### 20. MovementModel.rngSeed = 1
- **Deskripsi**: Seed untuk generator angka acak, yang digunakan untuk mengontrol posisi dan arah node.
### World's size for Movement Models without implicit size (width, height; meters)
### 21. MovementModel.worldSize = 1000, 1000
- **Deskripsi**: Ukuran dunia simulasi (1000 x 1000 meter).
### How long time to move hosts in the world before real simulation
### 22. MovementModel.warmup = 1000
- **Deskripsi**: Waktu warmup model pergerakan selama 1000 detik.

## Reports - all report names have to be valid report classes

### how many reports to load
### 23. Report.nrofReports = 1
- **Deskripsi**: Hanya ada satu laporan.

### length of the warm up period (simulated seconds)
### 24. Report.warmup = 600
- **Deskripsi**: Periode warmup laporan (600 detik). Event sebelum waktu ini akan diabaikan.

### default directory of reports (can be overridden per Report with output setting)
### 25.Report.reportDir = reports/
- **Deskripsi**: Direktori penyimpanan laporan.


### Report classes to load
### 26. Report.report1 = ConnectivityDtnsim2Report
- **Deskripsi**:  Laporan yang digunakan adalah ConnectivityDtnsim2Report.
### Settings for ConnectivityDtnsim2Report
### 27.ConnectivityDtnsim2Report.output = reports/connectivity_dtn_report.txt
- **Deskripsi**:  Nama file output laporan.

## Optimization settings -- these affect the speed of the simulation
### see World class for details.
### 28. Optimization.cellSizeMult = 5
- **Deskripsi**: Mengatur ukuran cell grid untuk optimasi.
### 29. Optimization.randomizeUpdateOrder = true
- **Deskripsi**: Mengacak urutan update setiap node setiap time step.

## how many events to show in the log panel (default = 30)
### 30. GUI.EventLogPanel.nrofEvents = 100
- **Deskripsi**: Jumlah event yang akan ditampilkan pada log panel GUI.