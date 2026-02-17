# Port 8083 Nasıl Bulunur ve Kapatılır

`APPLICATION FAILED TO START` ve `Port 8083 was already in use` hatası alırsan, 8083 portunu kullanan süreci bulup kapatman gerekir.

## 1) Portu Kullanan Süreci Bul

```bash
ss -ltnp | rg ':8083'
```

Alternatif:

```bash
lsof -i :8083 -sTCP:LISTEN -n -P
```

Beklenen çıktıda `PID` görünür. Örnek:

```text
java    32191 ... TCP *:8083 (LISTEN)
```

## 2) Süreci Kapat

Normal kapatma:

```bash
kill 32191
```

Genel kullanım:

```bash
kill <PID>
```

Yanıt vermezse zorla kapatma:

```bash
kill -9 <PID>
```

## 3) Tek Satır Kısa Yol

```bash
kill $(lsof -ti :8083)
```

## 4) Portun Boşaldığını Doğrula

```bash
ss -ltnp | rg ':8083'
```

Çıktı gelmiyorsa port boştur ve uygulamayı tekrar başlatabilirsin.

## 5) Uygulamayı Tekrar Başlat

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
