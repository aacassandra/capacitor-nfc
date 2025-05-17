import type { PluginListenerHandle } from '@capacitor/core';

export interface NFCPlugin {
  /**
   * Starts the NFC scanning session.
   */
  startScan(): Promise<void>;

  /**
   * Writes an NDEF message to an NFC tag.
   * @param options The NDEF message to write.
   */
  writeNDEF(options: NDEFWriteOptions): Promise<void>;

  /**
   * Adds a listener for NFC tag detection events.
   * @param eventName The name of the event ('nfcTag').
   * @param listenerFunc The function to call when an NFC tag is detected.
   */
  addListener(
    eventName: 'nfcTag',
    listenerFunc: (data: NDEFMessages) => void,
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
  removeAllListeners(eventName: 'nfcTag' | 'nfcError'): Promise<void>;

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
