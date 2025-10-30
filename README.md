# Kernel Manager UI (Demo)

UI/UX contoh sederhana untuk aplikasi *Kernel Manager* berbasis **Android Jetpack Compose**.
> Aman untuk dicoba: semua aksi (`Flash`, `Backup`, `Restore`) **simulasi** dan TIDAK menyentuh partisi perangkat.

## Fitur
- Dashboard: info device + progress status.
- Compact CPU card: rata-rata suhu CPU dan core terpanas (real-time).
- Monitor: daftar suhu per thermal zone (CPU dikelompokkan otomatis).
- Tuning CPU (Root): pilih governor dan apply ke semua core.
- UI Material 3 (light), Compose Navigation, MVVM + StateFlow polling.
- Settings: atur polling interval, ambang suhu panas, dan toggle polling (persisten).
 - Preferred governor: pilihan governor disimpan dan dipulihkan saat aplikasi dibuka.

## Build
1. Buka folder ini di **Android Studio Jellyfish+**.
2. Pastikan **Android Gradle Plugin 8.7+** & **Gradle 8.9**.
3. Run pada emulator / device.

## Publish ke GitHub (contoh)
```bash
git init
git branch -M main
git add .
git commit -m "init: Kernel Manager UI demo (Compose)"
git remote add origin https://github.com/<username>/<repo>.git
git push -u origin main
```

## Root & Keamanan
- Fitur Tuning CPU memerlukan akses root via [libsu](https://github.com/topjohnwu/libsu). Saat pertama kali apply governor, aplikasi akan meminta grant root. Jika ditolak, perubahan tidak dijalankan.
- Perintah yang digunakan untuk apply governor:
  - `for f in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo -n <governor> > $f; done`
- Aplikasi melakukan polling thermal dari sysfs (`/sys/class/thermal/thermal_zone*/{type,temp}`) tiap ~1s. Nilai >= 200 dianggap milidegree dan otomatis dibagi 1000.
- Monitor memiliki toggle untuk pause/resume polling agar hemat baterai.
- Di layar Monitor, chip ringkas akan berwarna peringatan jika rata-rata suhu CPU > 70°C.
- Settings menyimpan preferensi (polling enabled, interval, ambang panas) secara persisten.
 - Akses cepat ke Settings dari layar Monitor.
- Berjalan sebagai UI monitoring ringan; tidak menulis ke partisi/system selain operasi tuning yang eksplisit diminta pengguna.

## Catatan Perangkat
- Nama governor dan thermal zone dapat berbeda-beda per device/kernel.
- Beberapa device tidak mengekspos thermal CPU terpisah; kartu ringkas akan menampilkan "-" jika data tidak tersedia.
