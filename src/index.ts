// ========================================
// 2. index.ts - FIXED REGISTRATION
// ========================================

import { registerPlugin } from '@capacitor/core';
import type { NFCPlugin } from './definitions';

const NFC = registerPlugin<NFCPlugin>('NFC', {
  // ✅ KUNCI: Explicit web implementation
  web: () => import('./web').then(m => new m.NFCWeb()),
});

export * from './definitions';
export { NFC };