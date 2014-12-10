#**Seyhan Projesi' ne Hoşgeldiniz** 

####Seyhan, küçük ve orta ölçekli firmalara yönelik bir ön muhasebe projesidir. 

###Genel özellikleri
* Açık kaynaklı kodludur [^1]
* Çok kullanıcıdır
* Çoklu dil desteğine sahiptir (şimdilik Türkçe ve İngilizce)
* İşletim sistemi bağımsızdır (linux, windows, mac)
* Veritabanı bağımsızdır (h2, mysql, postgresql, ms-sqlserver)
* Tarayıcı bağımsızdır (firefox, ie, chrome, safari)
* Uzak sunucularda ve kişisel bilgisayarlarda çalışır [^2]

[^1]: https://bitbucket.org/mdpinar/seyhan

[^2]: Projeyi uzak ya da yerel bir sunucuda tutmanız, verilerinizin internete açılacağı anlamına gelmez. Aksine bunu yapmak için ekstra uğraşmanız gerekir.

###Sahip olduğu modüller
* Cari
* Stok
* Sipariş
* İrsaliye
* Fatura
* Çek
* Senet
* Kasa
* Banka
* Satış
* Genel
* Admin 

###Teşekkürler
|Adı|Sürüm|Kullanım Amacı|
|-|:-:|-|
|[Java](http://www.java.com)|1.6|Backend geliştirme platformu|
|[Eclipse](http://www.eclipse.org)|Kepler|Java platformu için bütünleşik geliştirme ortamı|
|[Linux](http://www.linuxmint.com/)|Mint|Açık kaynak linux işletim sistemi|
|[Scala](http://www.scala-lang.org)|2.10|Template hazırlama dili|
|[Play Framework](http://www.playframework.com)|2.1|RestFUL WEB uygulama çatısı|
|[Twitter Bootstrap](http://getbootstrap.com)|2.3.2|Frontend WEB arayüz hazırlama çatısı|
|[H2](http://www.h2database.com)|1.3|Demo projesi için veritabanı|
|[MySql](http://www.mysql.com)|5.2|Geliştirme veritabanı|
|[Flyway](http://flywaydb.org)|2.2|Versiyonlar arası veritabanı geçiş sistemi|
|[Ant](http://ant.apache.org)|1.8.2|Betik tabanlı derleme sistemi|
|[jQuery](http://www.jquery.com)|1.9|Temel Javascript kütüphanesi|
|[Bitbucket](https://bitbucket.org)|-|Kod reposu (git) ve talep yönetimi|

#Kurulum işlemleri

###Kurulumdan önce yapılması gerekenler
1. Sisteminizde Java 6 SDK kurulu olması gerekiyor. Daha önceden kurmadıysanız [şu adresten](http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase6-419409.html#jdk-6u45-oth-JPR) indirebilirsiniz.

2. Seyhan ilk olarak H2 veritabanı ile çalışacak şekilde ayarlanmıştır ve H2 db, daha çok demo ve test işlemlerinde kullanım için uygundur.  Farklı bir veritabanı kullanmak istiyorsanız ve sisteminizde de yüklü değilse bu aşamada kurmanız gerekiyor. 

3. Dağıtımlar zip halinde olduğu için sisteminizde Zip Açıcı (winzip, unzip...) olmalı. Sisteminizde yoksa, kullandığınız işletim sistemine uygun olan bir açıcı indirip kullanabilirsiniz.

###Kurulum aşaması
1. Seyhan projesini şu [ftp adresinden](ftp://seyhanproject.com) indirin.

2. seyhan-x.x.x.zip dosyasını sisteminizde uygun olan bir yere açın. Daha sonraki anlatımlarda referans olması için bu dizine APP_DIR diyeceğiz.

APP_DIR (1)
├─ conf (2)
│   └── evolutions (3)
├── lib (4)
├── logs (5)
├── reports (6)
└── share (7)

(1) Seyhan Project'in ana dizinidir. Projeyi başlatmak için içerisinde iki adet script dosyası bulunur. ***seyhan.bat***, Windows işletim sistemleri için, ***seyhan*** ise Mac ve Linux sistemleri için ana script dosyalarıdır. Linux ve Mac kullanıcılarının öncelikle ***seyhan*** script dosyasını executable hale getirmeleri gerekiyor.
<pre>
$ chmod +x seyhan
</pre>

(2) Tüm konfigurasyon dosyalarının bulunduğu dizindir. İçerisinde bulunan dosyalar ve temel görevleri:

* `application.conf`: projenin temel ayarlar dosyasıdır.
* `messages.tr`: Türkçe metinlerin bulunduğu dosyadır.
* `messages.en`: İngilizce metinlerin bulunduğu dosyadır.
* `logger.xml`: log çıktısı formatlarının ve çıkış yerlerinin ayarlandığı dosyadır.

(3) Proje kullanılacağı ilk sefer için veritabanı şema kurulumu *seyhan* tarafından otomatik olarak yapılır. Bu dizinde her bir veritabanı için ana şemayı oluşturan toplu sql scriptleri yer alır. *seyhan* şema kurulumlarını ve güncellemelerin otomatik olarak yapar isterseniz bunu kendiniz de yapabilirsiniz.

(4) Projenin temel jar dosyası ile kullandığı kütüphaneler bu dizinde yer alır.

(5) log dosyaları bu dizinde yer alır.

(6) Projedeki tüm raporlar bu dizinde açık olarak yer alır.

(7) Projenin kod dökümanları bu dizinde yer alır (geliştiricilere yönelik içeriğe sahip dökümanlardır)