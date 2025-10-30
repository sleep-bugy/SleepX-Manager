# Kernel Manager UI (Demo)

UI/UX contoh sederhana untuk aplikasi *Kernel Manager* berbasis **Android Jetpack Compose**.
> Aman untuk dicoba: semua aksi (`Flash`, `Backup`, `Restore`) **simulasi** dan TIDAK menyentuh partisi perangkat.

## Fitur
- Dashboard: info device + progress status.
- Tab navigasi: Dashboard / Flash / Backup / Restore.
- Arsitektur sederhana: ViewModel + StateFlow, tanpa dependency berat.

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
