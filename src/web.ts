import { WebPlugin } from '@capacitor/core';

import type { NDEFMessages, NDEFWriteOptions, NFCError, NFCPlugin, NFCUIDData } from './definitions';

/**
 * Implementasi Web NFC untuk plugin Capacitor NFC
 * 
 * CATATAN PENTING:
 * - Web NFC API hanya tersedia di browser yang mendukung (Chrome/Edge di Android)
 * - Memerlukan HTTPS atau localhost untuk keamanan
 * - UID scanning menggunakan fallback karena Web NFC tidak menyediakan akses langsung ke UID
 * - Serial number jarang tersedia di Web NFC, jadi menggunakan hash dari content sebagai UID
 * 
 * KOMPATIBILITAS:
 * - Chrome 89+ di Android
 * - Edge 89+ di Android
 * - Tidak didukung di iOS Safari
 * - Tidak didukung di desktop browser
 */

export class NFCWeb extends WebPlugin implements NFCPlugin {
  private nfcReader?: any; // NDEFReader type
  private scanActive = false;
  private uidScanActive = false; // Flag untuk UID scanning mode

  async startScan(): Promise<void> {
    // Implementasi NDEF scanning untuk Web NFC
    if (!await this.isNFCSupported()) {
      throw this.createError('Web NFC is not supported in this browser');
    }
    
    this.nfcReader = new (window as any).NDEFReader();
    this.scanActive = true;
    this.uidScanActive = false; // Pastikan UID scan mode off
    
    // Mulai scanning untuk NDEF messages
    await this.nfcReader.scan();
    
    // Handler untuk membaca NDEF messages
    this.nfcReader.onreading = (event: any) => {
      if (this.scanActive && !this.uidScanActive) {
        // Convert Web NFC message to plugin format
        const messages: NDEFMessages = {
          messages: [this.convertNDEFMessage(event.message)],
        };
        this.notifyListeners('nfcTag', messages);
      }
    };
    
    this.nfcReader.onerror = (event: any) => {
      this.notifyListeners('nfcError', { error: event.error?.message || 'NFC NDEF scan error' });
    };
  }

  async startUIDScan(): Promise<void> {
    // Implementasi UID scanning untuk Web NFC
    if (!await this.isNFCSupported()) {
      throw this.createError('Web NFC is not supported in this browser');
    }
    
    this.nfcReader = new (window as any).NDEFReader();
    this.uidScanActive = true;
    this.scanActive = true;
    
    // Mulai scanning untuk mendapatkan informasi tag
    await this.nfcReader.scan();
    
    // Handler untuk membaca tag dan mengekstrak UID-like information
    this.nfcReader.onreading = (event: any) => {
      if (this.uidScanActive) {
        // Ekstrak informasi unik dari tag untuk dijadikan UID
        const uidData = this.extractUIDFromTag(event);
        this.notifyListeners('nfcUID', uidData);
      }
    };
    
    this.nfcReader.onerror = (event: any) => {
      this.notifyListeners('nfcError', { error: event.error?.message || 'NFC UID scan error' });
    };
  }

  async stopScan(): Promise<void> {
    this.scanActive = false;
    this.uidScanActive = false; // Reset UID scan flag
    if (this.nfcReader) {
      // Web NFC API tidak memiliki method stop, tapi kita set flag
      this.nfcReader = undefined;
    }
  }

  async writeNDEF(options: NDEFWriteOptions): Promise<void> {
    // Implementasi penulisan NDEF untuk Web NFC
    if (!await this.isNFCSupported()) {
      throw this.createError('Web NFC is not supported in this browser');
    }
    
    const writer = new (window as any).NDEFReader();
    
    try {
      // Konversi records ke format Web NFC
      const ndefMessage = {
        records: options.records.map(record => ({
          recordType: record.type,
          data: record.payload
        }))
      };
      
      // Tulis NDEF message ke tag
      await writer.write(ndefMessage);
      this.notifyListeners('nfcWriteSuccess', {});
    } catch (error: any) {
      this.notifyListeners('nfcError', { error: error.message || 'NFC write error' });
    }
  }

  // NFCPlugin interface expects these signatures
  addListener(eventName: 'nfcTag', listenerFunc: (data: NDEFMessages) => void): Promise<any> & any;
  addListener(eventName: 'nfcUID', listenerFunc: (data: NFCUIDData) => void): Promise<any> & any;
  addListener(eventName: 'nfcWriteSuccess', listenerFunc: () => void): Promise<any> & any;
  addListener(eventName: 'nfcError', listenerFunc: (error: NFCError) => void): Promise<any> & any;
  addListener(eventName: string, listenerFunc: (data: any) => void): Promise<any> & any {
    return super.addListener(eventName, listenerFunc);
  }

  removeAllListeners(_eventName?: 'nfcTag' | 'nfcUID' | 'nfcError'): Promise<void> {
    // WebPlugin.removeAllListeners() tidak menerima parameter eventName
    // Jadi kita hanya panggil method parent tanpa parameter
    return super.removeAllListeners();
  }

  // Helper: check if Web NFC is supported
  async isNFCSupported(): Promise<boolean> {
    // Cek apakah browser mendukung Web NFC API
    if (typeof window === 'undefined' || !('NDEFReader' in window)) {
      return false;
    }
    
    // Cek apakah context aman (HTTPS atau localhost)
    if (location.protocol !== 'https:' && location.hostname !== 'localhost') {
      console.warn('Web NFC requires HTTPS or localhost');
      return false;
    }
    
    return true;
  }

  // Helper: convert Web NFC message to plugin format
  private convertNDEFMessage(message: any): any {
    // Web NFC NDEFMessage.records is an array of NDEFRecord
    return {
      records: (message.records || []).map((rec: any) => ({
        type: rec.recordType,
        payload: rec.data ? rec.data : rec.data === undefined && rec.text ? rec.text : '',
      })),
    };
  }

  // Helper: ekstrak UID-like data dari Web NFC tag
  private extractUIDFromTag(event: any): NFCUIDData {
    // Buat UID unik berdasarkan informasi yang tersedia dari Web NFC
    let uid = '';
    let uidFormatted = '';
    
    // Coba ambil serial number jika tersedia (jarang tersedia di Web NFC)
    if (event.serialNumber) {
      uid = event.serialNumber.replace(/:/g, '').toLowerCase();
      uidFormatted = event.serialNumber.toUpperCase();
    } else {
      // Fallback: buat UID berdasarkan kombinasi informasi tag
      let tagInfo = '';
      
      // Gabungkan informasi yang tersedia untuk membuat identifier unik
      if (event.message && event.message.records) {
        // Gunakan content dari NDEF records
        tagInfo += JSON.stringify(event.message.records);
      }
      
      // Tambahkan timestamp untuk uniqueness jika tidak ada content
      if (!tagInfo) {
        tagInfo = Date.now().toString();
      }
      
      // Generate UID dari informasi tag
      uid = this.generateHashUID(tagInfo);
      uidFormatted = this.formatUID(uid);
    }
    
    // Tentukan tipe kartu berdasarkan panjang UID
    const uidLength = uid.length / 2; // Konversi dari hex string ke byte length
    let cardType = 'Web NFC Tag';
    
    // Klasifikasi berdasarkan panjang UID yang dihasilkan
    if (uidLength === 4) {
      cardType = 'Web NFC - MIFARE Classic';
    } else if (uidLength === 7) {
      cardType = 'Web NFC - MIFARE Ultralight';
    } else {
      cardType = 'Web NFC - Generic Tag';
    }
    
    return {
      uid: uid,
      uidFormatted: uidFormatted,
      uidLength: uidLength,
      cardType: cardType,
      timestamp: new Date().toISOString(),
      techList: ['WebNFC', 'NDEF'] // Teknologi yang tersedia di web
    };
  }
  
  // Helper: generate hash-based UID untuk fallback
  private generateHashUID(content: string): string {
    let hash = 0;
    for (let i = 0; i < content.length; i++) {
      const char = content.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    
    // Konversi ke hex dan pastikan 8 karakter (4 bytes)
    const hexHash = Math.abs(hash).toString(16).padStart(8, '0');
    return hexHash.substring(0, 8); // Ambil 4 bytes pertama
  }
  
  // Helper: format UID dengan spasi
  private formatUID(uid: string): string {
    return uid.match(/.{2}/g)?.join(' ').toUpperCase() || uid.toUpperCase();
  }
  
  private createError(msg: string): NFCError {
    return { error: msg };
  }
}
