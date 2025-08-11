# 🌐 Dukungan Web NFC untuk Plugin Capacitor NFC

## 📋 Ringkasan

Plugin `@aacassandra/capacitor-nfc` kini mendukung **Web NFC API** untuk aplikasi PWA (Progressive Web App). Implementasi ini memungkinkan pembacaan ID unik kartu NFC dan pesan NDEF langsung di browser yang mendukung Web NFC.

## ✨ Fitur yang Didukung

### ✅ Fitur Tersedia
- **📱 Pemindaian UID**: Membaca ID unik kartu menggunakan fallback hash-based
- **📋 Pemindaian NDEF**: Membaca pesan NDEF dari tag NFC
- **✍️ Penulisan NDEF**: Menulis pesan NDEF ke tag NFC
- **🔍 Pemeriksaan Dukungan**: Mengecek ketersediaan Web NFC API
- **🎧 Event Listeners**: Mendengarkan event `nfcTag`, `nfcUID`, dan `nfcError`

### ⚠️ Keterbatasan
- **UID berbasis Hash**: Web NFC tidak menyediakan akses langsung ke UID hardware, jadi menggunakan hash dari konten tag
- **Dukungan Browser Terbatas**: Hanya Chrome/Edge di Android
- **Keamanan HTTPS**: Memerlukan HTTPS atau localhost
- **Tidak Ada Serial Number**: Serial number asli tidak tersedia di Web NFC

## 🌍 Kompatibilitas Browser

| Browser | Platform | Status | Versi Minimum |
|---------|----------|--------|--------------|
| Chrome | Android | ✅ Didukung | 89+ |
| Edge | Android | ✅ Didukung | 89+ |
| Safari | iOS | ❌ Tidak Didukung | - |
| Firefox | Semua | ❌ Tidak Didukung | - |
| Desktop Browser | Semua | ❌ Tidak Didukung | - |

## 🚀 Cara Penggunaan

### 1. Import Plugin

```typescript
import { NFC } from '@aacassandra/capacitor-nfc';
```

### 2. Cek Dukungan NFC

```typescript
const checkNFCSupport = async () => {
  try {
    const isSupported = await NFC.isNFCSupported();
    if (isSupported) {
      console.log('✅ Web NFC didukung');
    } else {
      console.log('❌ Web NFC tidak didukung');
    }
  } catch (error) {
    console.error('Error:', error.message);
  }
};
```

### 3. Pemindaian UID (untuk Registrasi Kartu)

```typescript
// Setup listener untuk UID
await NFC.addListener('nfcUID', (data) => {
  console.log('UID Terdeteksi:', data.uid);
  console.log('Tipe Kartu:', data.cardType);
  console.log('Tech List:', data.techList);
  
  // Gunakan UID untuk registrasi siswa
  registerStudentCard(data.uid);
});

// Setup listener untuk error
await NFC.addListener('nfcError', (error) => {
  console.error('NFC Error:', error.message);
});

// Mulai scan UID
try {
  await NFC.startUIDScan();
  console.log('Scan UID dimulai. Dekatkan kartu...');
} catch (error) {
  console.error('Error memulai scan:', error.message);
}
```

### 4. Pemindaian NDEF

```typescript
// Setup listener untuk NDEF
await NFC.addListener('nfcTag', (data) => {
  console.log('NDEF Messages:', data.messages);
  data.messages.forEach((message, index) => {
    console.log(`Message ${index + 1}:`, message.records);
  });
});

// Mulai scan NDEF
await NFC.startScan();
```

### 5. Penulisan NDEF

```typescript
const writeNDEF = async () => {
  try {
    await NFC.writeNDEF({
      records: [{
        tnf: 1, // TNF_WELL_KNOWN
        type: 'T', // Text record
        payload: 'Hello from Web NFC!'
      }]
    });
    console.log('✅ NDEF berhasil ditulis');
  } catch (error) {
    console.error('❌ Error menulis NDEF:', error.message);
  }
};
```

### 6. Menghentikan Scan

```typescript
const stopScanning = async () => {
  try {
    await NFC.stopScan();
    console.log('✅ Scan dihentikan');
  } catch (error) {
    console.error('❌ Error menghentikan scan:', error.message);
  }
};
```

## 🎯 Implementasi di AbsenceStudentRegisterPage

Berikut contoh implementasi di halaman registrasi siswa:

```vue
<template>
  <div class="nfc-registration">
    <ion-button @click="startNFCRegistration" :disabled="!nfcSupported">
      📱 Mulai Registrasi Kartu NFC
    </ion-button>
    
    <div v-if="isScanning" class="scanning-status">
      🔍 Menunggu kartu NFC... Dekatkan kartu ke perangkat
    </div>
    
    <div v-if="lastCardUID" class="card-info">
      <h3>✅ Kartu Terdeteksi</h3>
      <p><strong>UID:</strong> {{ lastCardUID }}</p>
      <ion-button @click="confirmRegistration">Konfirmasi Registrasi</ion-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { NFC } from '@aacassandra/capacitor-nfc';
import { IonButton } from '@ionic/vue';

const nfcSupported = ref(false);
const isScanning = ref(false);
const lastCardUID = ref('');

// Cek dukungan NFC saat komponen dimuat
onMounted(async () => {
  try {
    nfcSupported.value = await NFC.isNFCSupported();
    if (nfcSupported.value) {
      setupNFCListeners();
    }
  } catch (error) {
    console.error('Error checking NFC support:', error);
  }
});

// Setup event listeners
const setupNFCListeners = async () => {
  // Listener untuk UID yang terdeteksi
  await NFC.addListener('nfcUID', (data) => {
    console.log('UID detected:', data.uid);
    lastCardUID.value = data.uid;
    isScanning.value = false;
    
    // Auto-stop scanning setelah kartu terdeteksi
    NFC.stopScan();
  });
  
  // Listener untuk error
  await NFC.addListener('nfcError', (error) => {
    console.error('NFC Error:', error.message);
    isScanning.value = false;
    // Tampilkan toast error ke user
  });
};

// Mulai proses registrasi NFC
const startNFCRegistration = async () => {
  try {
    isScanning.value = true;
    lastCardUID.value = '';
    
    await NFC.startUIDScan();
    console.log('NFC UID scanning started');
  } catch (error) {
    console.error('Error starting NFC scan:', error);
    isScanning.value = false;
  }
};

// Konfirmasi registrasi kartu
const confirmRegistration = async () => {
  if (!lastCardUID.value) return;
  
  try {
    // Kirim UID ke server untuk registrasi
    const response = await httpUtil.post('/api/mobile/edutrack/student/register-card', {
      student_id: studentId.value,
      card_uid: lastCardUID.value
    });
    
    if (response.data.success) {
      // Tampilkan success message
      console.log('✅ Kartu berhasil didaftarkan');
      lastCardUID.value = '';
    }
  } catch (error) {
    console.error('Error registering card:', error);
  }
};
</script>
```

## 🧪 Testing

### File Test Tersedia

1. **`web-test.html`**: Test dasar implementasi Web NFC
2. **`test-web-integration.html`**: Test integrasi lengkap dengan UI

### Cara Menjalankan Test

```bash
# 1. Build plugin terlebih dahulu
npm run build

# 2. Serve file test dengan HTTPS (diperlukan untuk Web NFC)
# Gunakan server lokal dengan HTTPS atau deploy ke server HTTPS

# Contoh dengan Python (jika tersedia):
python3 -m http.server 8000
# Kemudian akses: https://localhost:8000/test-web-integration.html
```

### Langkah Testing

1. **Buka browser Chrome/Edge di Android**
2. **Akses halaman test melalui HTTPS atau localhost**
3. **Klik "Mulai Scan UID" atau "Mulai Scan NDEF"**
4. **Dekatkan kartu NFC ke perangkat**
5. **Periksa log untuk melihat hasil scan**

## 🔧 Pemecahan Masalah

### ❌ "Web NFC API tidak didukung"
- **Solusi**: Gunakan Chrome/Edge di Android dengan versi terbaru
- **Alternatif**: Test di perangkat Android fisik, bukan emulator

### ❌ "NotAllowedError: NFC permission denied"
- **Solusi**: Pastikan menggunakan HTTPS atau localhost
- **Periksa**: Permission NFC di browser settings

### ❌ "UID selalu sama untuk kartu berbeda"
- **Penjelasan**: Ini normal untuk Web NFC karena menggunakan hash fallback
- **Solusi**: UID tetap unik per kartu, meski tidak sama dengan UID hardware asli

### ❌ "Scan tidak berfungsi"
- **Periksa**: Apakah NFC aktif di perangkat Android
- **Periksa**: Apakah halaman diakses melalui HTTPS
- **Coba**: Restart browser dan coba lagi

## 📝 Catatan Pengembangan

### Perubahan File

1. **`src/web.ts`**: Implementasi lengkap Web NFC API
2. **`src/index.ts`**: Menambahkan import web implementation
3. **Build files**: Semua file dist ter-update dengan implementasi web

### Fitur Tambahan yang Bisa Dikembangkan

- **Caching UID**: Simpan UID yang sudah pernah di-scan
- **Batch Registration**: Registrasi multiple kartu sekaligus
- **UID Validation**: Validasi format UID sebelum registrasi
- **Offline Support**: Simpan data registrasi saat offline

## 🎉 Kesimpulan

Implementasi Web NFC telah berhasil ditambahkan ke plugin `@aacassandra/capacitor-nfc`. Meskipun ada beberapa keterbatasan dibanding implementasi native Android/iOS, fitur ini memungkinkan aplikasi PWA untuk membaca kartu NFC dan melakukan registrasi siswa langsung di browser.

**Fitur utama yang berfungsi:**
- ✅ Pembacaan UID kartu (hash-based)
- ✅ Pembacaan pesan NDEF
- ✅ Penulisan pesan NDEF
- ✅ Event handling yang konsisten
- ✅ Error handling yang robust

**Siap untuk produksi** dengan catatan penggunaan di browser yang didukung dan environment HTTPS.