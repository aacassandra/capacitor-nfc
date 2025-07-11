import type { PluginListenerHandle } from '@capacitor/core';

export interface NFCPlugin {
  /**
   * Starts the NFC scanning session.
   */
  startScan(): Promise<void>;

  /**
   * Starts scanning for NFC tags to read their UID (unique identifier).
   * This is useful for card identification without requiring NDEF data.
   */
  startUIDScan(): Promise<void>;

  /**
   * Stops the current NFC scanning session.
   */
  stopScan(): Promise<void>;

  /**
   * Writes an NDEF message to an NFC tag.
   * @param options The NDEF message to write.
   */
  writeNDEF(options: NDEFWriteOptions): Promise<void>;

  /**
   * Adds a listener for NFC tag detection events (NDEF format).
   */
  addListener(
    eventName: 'nfcTag',
    listenerFunc: (data: NDEFMessages) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Adds a listener for NFC UID detection events.
   * This event is triggered when a card UID is detected during UID scanning.
   */
  addListener(
    eventName: 'nfcUID',
    listenerFunc: (data: NFCUIDData) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Adds a listener for NFC tag write events.
   * @param eventName The name of the event ('nfcWriteSuccess').
   * @param listenerFunc The function to call when an NFC tag is written.
   */
  addListener(
    eventName: 'nfcWriteSuccess',
    listenerFunc: () => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Adds a listener for NFC error events.
   * @param eventName The name of the event ('nfcError').
   * @param listenerFunc The function to call when an NFC error occurs.
   */
  addListener(
    eventName: 'nfcError',
    listenerFunc: (error: NFCError) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  /**
   * Removes all listeners for the specified event.
   * @param eventName The name of the event.
   */
  removeAllListeners(eventName: 'nfcTag' | 'nfcUID' | 'nfcError'): Promise<void>;

  /**
   * Checks if NFC is supported on the current device/platform.
   * Returns a boolean for all platforms.
   */
  isNFCSupported(): Promise<boolean>;
}

export interface NDEFMessages {
  messages: NDEFMessage[];
}

export interface NDEFMessage {
  records: NDEFRecord[];
}

export interface NDEFRecord {
  /**
   * The type of the record.
   */
  type: string;

  /**
   * The payload of the record.
   */
  payload: string;
}

export interface NFCError {
  /**
   * The error message.
   */
  error: string;
}

export interface NDEFWriteOptions {
  records: NDEFRecord[];
}

export interface NFCUIDData {
  /**
   * The unique identifier of the NFC card in hexadecimal format.
   */
  uid: string;

  /**
   * The UID formatted with spaces (e.g., "04 A1 23 45").
   */
  uidFormatted: string;

  /**
   * The length of the UID in bytes.
   */
  uidLength: number;

  /**
   * The detected card type based on UID length.
   */
  cardType: string;

  /**
   * Timestamp when the card was detected.
   */
  timestamp: string;

  /**
   * Additional technical information about the card.
   */
  techList?: string[];
}
