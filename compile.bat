@echo off                                           :: Menonaktifkan tampilan perintah di command prompt.

:: Set classpath to include all jars in the lib directory
set CLASSPATH=lib/*;                              :: Mendefinisikan variabel lingkungan CLASSPATH yang berisi semua file jar di direktori lib/.

:: Create output directory if not exists
if not exist bin mkdir bin                         :: Membuat direktori bin jika belum ada.

:: Check Java version
java -version                                      :: Menjalankan perintah java -version untuk mengecek versi Java.
if %errorlevel% neq 0 (                             :: Memeriksa kode error dari perintah java -version. Jika tidak 0 (berarti error), maka:
    echo Java is not installed or not in PATH.      :: Menampilkan pesan error bahwa Java tidak terinstal atau tidak ada di PATH.
    pause                                          :: Menghentikan eksekusi dan menunggu tombol ditekan (untuk melihat pesan error).
    goto :end                                       :: Melompat ke label :end untuk menghentikan eksekusi skrip.
)

echo Compiling core files...                        :: Menampilkan pesan di layar bahwa kompilasi file core akan dimulai.
javac -cp %CLASSPATH% -d bin -Xlint:-deprecation,unchecked core/*.java  :: Mengkompilasi file Java di direktori core, menggunakan classpath, output ke bin, dan menonaktifkan beberapa lint warning.
if %errorlevel% neq 0 goto :compilation_error        :: Memeriksa kode error dari perintah javac. Jika tidak 0, maka lompat ke label :compilation_error.

echo Compiling movement files...                    :: Menampilkan pesan di layar bahwa kompilasi file movement akan dimulai.
javac -cp %CLASSPATH% -d bin -Xlint:-deprecation,unchecked movement/*.java :: Mengkompilasi file Java di direktori movement, menggunakan classpath, output ke bin, dan menonaktifkan beberapa lint warning.
if %errorlevel% neq 0 goto :compilation_error        :: Memeriksa kode error dari perintah javac. Jika tidak 0, maka lompat ke label :compilation_error.

echo Compiling report files...                      :: Menampilkan pesan di layar bahwa kompilasi file report akan dimulai.
javac -cp %CLASSPATH% -d bin -Xlint:-deprecation,unchecked report/*.java  :: Mengkompilasi file Java di direktori report, menggunakan classpath, output ke bin, dan menonaktifkan beberapa lint warning.
if %errorlevel% neq 0 goto :compilation_error        :: Memeriksa kode error dari perintah javac. Jika tidak 0, maka lompat ke label :compilation_error.

echo Compiling routing files...                     :: Menampilkan pesan di layar bahwa kompilasi file routing akan dimulai.
javac -cp %CLASSPATH% -d bin -Xlint:-deprecation,unchecked routing/*.java :: Mengkompilasi file Java di direktori routing, menggunakan classpath, output ke bin, dan menonaktifkan beberapa lint warning.
if %errorlevel% neq 0 goto :compilation_error        :: Memeriksa kode error dari perintah javac. Jika tidak 0, maka lompat ke label :compilation_error.

echo Compiling gui files...                        :: Menampilkan pesan di layar bahwa kompilasi file gui akan dimulai.
javac -cp %CLASSPATH% -d bin -Xlint:-deprecation,unchecked gui/*.java  :: Mengkompilasi file Java di direktori gui, menggunakan classpath, output ke bin, dan menonaktifkan beberapa lint warning.
if %errorlevel% neq 0 goto :compilation_error        :: Memeriksa kode error dari perintah javac. Jika tidak 0, maka lompat ke label :compilation_error.

echo Compiling input files...                      :: Menampilkan pesan di layar bahwa kompilasi file input akan dimulai.
javac -cp %CLASSPATH% -d bin -Xlint:-deprecation,unchecked input/*.java  :: Mengkompilasi file Java di direktori input, menggunakan classpath, output ke bin, dan menonaktifkan beberapa lint warning.
if %errorlevel% neq 0 goto :compilation_error        :: Memeriksa kode error dari perintah javac. Jika tidak 0, maka lompat ke label :compilation_error.

echo Compiling applications files...               :: Menampilkan pesan di layar bahwa kompilasi file applications akan dimulai.
javac -cp %CLASSPATH% -d bin -Xlint:-deprecation,unchecked applications/*.java :: Mengkompilasi file Java di direktori applications, menggunakan classpath, output ke bin, dan menonaktifkan beberapa lint warning.
if %errorlevel% neq 0 goto :compilation_error        :: Memeriksa kode error dari perintah javac. Jika tidak 0, maka lompat ke label :compilation_error.

echo Compiling interfaces files...                :: Menampilkan pesan di layar bahwa kompilasi file interfaces akan dimulai.
javac -cp %CLASSPATH% -d bin -Xlint:-deprecation,unchecked interfaces/*.java :: Mengkompilasi file Java di direktori interfaces, menggunakan classpath, output ke bin, dan menonaktifkan beberapa lint warning.
if %errorlevel% neq 0 goto :compilation_error        :: Memeriksa kode error dari perintah javac. Jika tidak 0, maka lompat ke label :compilation_error.

echo Compilation complete.                       :: Menampilkan pesan di layar bahwa kompilasi telah selesai.
goto :end                                          :: Melompat ke label :end.

:compilation_error                                 :: Label yang menjadi tujuan jika terjadi error kompilasi.
echo Compilation failed. See compile_log.txt for details. :: Menampilkan pesan bahwa kompilasi gagal dan log tersedia di compile_log.txt.
pause                                             :: Menghentikan eksekusi dan menunggu tombol ditekan (untuk melihat pesan error).

:end                                               :: Label akhir dari skrip.