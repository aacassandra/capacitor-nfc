import { registerPlugin } from '@capacitor/core';
import type { NFCPlugin } from './definitions';

const NFC = registerPlugin<NFCPlugin>('NFC');

export * from './definitions';
export { NFC };
