import { WebPlugin } from '@capacitor/core';

import type { NDEFMessages, NDEFWriteOptions, NFCError, NFCPlugin, NFCUIDData } from './definitions';

export class NFCWeb extends WebPlugin implements NFCPlugin {
  private nfcReader?: any; // NDEFReader type
  private scanActive = false;

  async startScan(): Promise<void> {
    if (!this.isNFCSupported()) {
      throw this.createError('Web NFC is not supported in this browser');
    }
    this.nfcReader = new (window as any).NDEFReader();
    await this.nfcReader.scan();
    this.scanActive = true;
    this.nfcReader.onreading = (event: any) => {
      // Convert Web NFC message to plugin format
      const messages: NDEFMessages = {
        messages: [this.convertNDEFMessage(event.message)],
      };
      this.notifyListeners('nfcTag', messages);
    };
    this.nfcReader.onerror = (event: any) => {
      this.notifyListeners('nfcError', { error: event.error?.message || 'NFC error' });
    };
  }

  async startUIDScan(): Promise<void> {
    // Web NFC API tidak mendukung pembacaan UID langsung
    // Fallback ke NDEF scanning
    throw this.createError('UID scanning tidak didukung di web browser. Gunakan NDEF scanning sebagai alternatif.');
  }

  async stopScan(): Promise<void> {
    this.scanActive = false;
    if (this.nfcReader) {
      // Web NFC API tidak memiliki method stop, tapi kita set flag
      this.nfcReader = undefined;
    }
  }

  async writeNDEF(options: NDEFWriteOptions): Promise<void> {
    if (!this.isNFCSupported()) {
      throw this.createError('Web NFC is not supported in this browser');
    }
    const writer = new (window as any).NDEFReader();
    // Only support writing the first record for simplicity
    const record = options.records[0];
    await writer.write(record.payload);
    this.notifyListeners('nfcWriteSuccess', {});
  }

  // NFCPlugin interface expects these signatures
  addListener(eventName: 'nfcTag', listenerFunc: (data: NDEFMessages) => void): Promise<any> & any;
  addListener(eventName: 'nfcUID', listenerFunc: (data: NFCUIDData) => void): Promise<any> & any;
  addListener(eventName: 'nfcWriteSuccess', listenerFunc: () => void): Promise<any> & any;
  addListener(eventName: 'nfcError', listenerFunc: (error: NFCError) => void): Promise<any> & any;
  addListener(eventName: string, listenerFunc: (data: any) => void): Promise<any> & any {
    return super.addListener(eventName, listenerFunc);
  }

  removeAllListeners(): Promise<void> {
    return super.removeAllListeners();
  }

  // Helper: check if Web NFC is supported
  async isNFCSupported(): Promise<boolean> {
    return typeof window !== 'undefined' && 'NDEFReader' in window;
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

  private createError(msg: string): NFCError {
    return { error: msg };
  }
}

const NFC = new NFCWeb();
export { NFC };
