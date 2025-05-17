# Capacitor NFC Plugin (@aacassandra/capacitor-nfc)

A Capacitor plugin for reading and writing NFC tags on iOS and Android devices. This plugin allows you to:

- Read NDEF messages from NFC tags.
- Write NDEF messages to NFC tags.

**Note**: NFC functionality is only available on compatible iOS devices running iOS 13.0 or later, and Android devices with NFC hardware.

## Table of Contents

- [Installation](#installation)
- [iOS Setup](#ios-setup)
- [Android Setup](#android-setup)
- [Usage](#usage)
  - [Reading NFC Tags](#reading-nfc-tags)
  - [Writing NFC Tags](#writing-nfc-tags)
- [API](#api)
  - [Methods](#methods)
    - [`startScan()`](#startscan)
    - [`writeNDEF(options)`](#writendefoptions)
  - [Listeners](#listeners)
    - [`addListener('nfcTag', listener)`](#addlistenernfctag-listener)
    - [`addListener('nfcError', listener)`](#addlistenernfcerror-listener)
    - [`addListener('nfcWriteSuccess', listener)`](#addlistenernfcwritesuccess-listener)
  - [Interfaces](#interfaces)
    - [`NDEFWriteOptions`](#ndefwriteoptions)
    - [`NDEFMessages`](#ndefmessages)
    - [`NDEFMessage`](#ndefmessage)
    - [`NDEFRecord`](#ndefrecord)
    - [`NFCError`](#nfcerror)
- [Integration into a Capacitor App](#integration-into-a-capacitor-app)
- [Example](#example)
- [License](#license)

## Installation

Install the plugin using npm:

```bash
npm install @aacassandra/capacitor-nfc
npx cap sync
```

## Capabilities / Platform Support

| Feature                | Android | iOS | Web (Chrome/Edge Android) |
|------------------------|:-------:|:---:|:------------------------:|
| Read NDEF tag          |   ✅    | ✅  |           ✅             |
| Write NDEF tag         |   ✅    | ✅  |           ✅             |
| Listen for tag event   |   ✅    | ✅  |           ✅             |
| Listen for write event |   ✅    | ✅  |           ✅             |
| Error handling         |   ✅    | ✅  |           ✅             |
| Check NFC support      |   ✅    | ✅  |           ✅             |
| Low-level/tag raw      |   ❌    | ❌  |           ❌             |
| Background scan        |   ⚠️*   | ❌  |           ❌             |

- ✅ = Supported
- ❌ = Not supported
- ⚠️* = Android can scan in background if app is in foreground service, but plugin only supports foreground scan by default.
- Web NFC only works on Android (Chrome/Edge), not on iOS/Safari or desktop browsers.

## iOS Setup

To use NFC functionality on iOS, you need to perform some additional setup steps.

### 1. Enable NFC Capability

In Xcode:

1. Open your project (`.xcworkspace` file) in Xcode.
2. Select your project in the Project Navigator.
3. Select your app target.
4. Go to the **Signing & Capabilities** tab.
5. Click the `+ Capability` button.
6. Add **Near Field Communication Tag Reading**.

### 2. Add Usage Description

Add the `NFCReaderUsageDescription` key to your `Info.plist` file to explain why your app needs access to NFC.

In your `Info.plist` file (usually located at `ios/App/App/Info.plist`), add:

```xml
<key>NFCReaderUsageDescription</key>
<string>This app requires access to NFC to read and write NFC tags.</string>
```

Replace the description with a message that explains why your app needs NFC access.

## Android Setup

To use NFC functionality on Android, follow these steps:

### 1. Add NFC Permissions

The plugin automatically adds the required NFC permissions to your AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="true" />
```

If you want to make NFC optional for your app, change `android:required="true"` to `android:required="false"` in your app's main AndroidManifest.xml.

### 2. Check NFC Availability

In your app, you should check if NFC is available and enabled before using NFC functionality:

```typescript
import { NFC } from '@aacassandra/capacitor-nfc';

// Start scanning only if NFC is available
NFC.startScan()
  .then(() => {
    console.log('NFC scanning started');
  })
  .catch((error) => {
    console.error('NFC error:', error);
    // This will be triggered if NFC is not available or not enabled
  });
```

### 3. Handle NFC Detection in Foreground

The plugin automatically handles foreground NFC detection. Make sure your activity handles the NFC intent.

## Platform Specific Details

### iOS and Android Implementation Differences

This plugin provides consistent NFC functionality across both iOS and Android, but there are some platform-specific behaviors to be aware of:

#### Tag Reading

- **iOS**: Uses the `CoreNFC` framework and requires user interaction to initiate scanning. The NFC scanning session will show a system dialog.
- **Android**: Can detect NFC tags automatically when they come in range, even if the app is in the background (if configured).

#### NDEF Record Handling

The plugin normalizes the data format between iOS and Android to provide a consistent developer experience:

- **TEXT Records**: On both platforms, the language code prefix is removed so you only get the actual text content.
- **URI Records**: The URI identifier code is handled appropriately on both platforms.
- **Payload Format**: All payloads are returned as strings to maintain consistency between platforms.

#### Multiple Tag Detection

- **iOS**: Apple's CoreNFC API will detect if multiple tags are present and prompt the user to isolate a single tag.
- **Android**: Android typically handles one tag at a time, but the plugin adds extra verification for consistency.

#### NFC Availability Checking

- **iOS**: The plugin checks if the device has NFC hardware capability.
- **Android**: The plugin verifies both hardware availability and if NFC is enabled in device settings.

### Common Issues and Solutions

1. **NFC Not Detected on Android**
   - Ensure NFC is enabled in the device settings
   - Check if the device has NFC hardware support
   - Verify the tag is compatible with your device

2. **iOS NFC Dialog Not Showing**
   - Make sure you've added the required NFC capability and usage description
   - Verify your app is built for iOS 13.0 or later
   - Ensure you're testing on a physical device (NFC won't work in simulators)

## Usage

Import the plugin into your code:

```typescript
import { NFC } from '@aacassandra/capacitor-nfc';
```

### Reading NFC Tags

To read NFC tags, you need to start a scanning session and listen for `nfcTag` events.

```typescript
import { NFC, NDEFMessages, NFCError } from '@aacassandra/capacitor-nfc';

// Start NFC scanning
NFC.startScan().catch((error) => {
  console.error('Error starting NFC scan:', error);
});

// Listen for NFC tag detection
const nfcTagListener = NFC.addListener('nfcTag', (data: NDEFMessages) => {
  console.log('Received NFC tag:', data);
});

// Handle NFC errors
const nfcErrorListener = NFC.addListener('nfcError', (error: NFCError) => {
  console.error('NFC Error:', error);
});
```

### Writing NFC Tags

To write to NFC tags, you use the `writeNDEF` method and provide an array of NDEF records.

```typescript
import { NFC, NFCError } from '@aacassandra/capacitor-nfc';

// Start NFC writing mode
const recordsToWrite = {
  records: [
    {
      type: 'T', // TEXT record
      payload: 'Hello, NFC!'
    },
    {
      type: 'U', // URI record
      payload: 'https://example.com'
    }
  ]
};

NFC.writeNDEF(recordsToWrite).catch((error) => {
  console.error('Error starting NFC write:', error);
});

// Listen for successful write
const writeSuccessListener = NFC.addListener('nfcWriteSuccess', () => {
  console.log('Successfully wrote to NFC tag');
});

// Handle NFC write errors
const writeErrorListener = NFC.addListener('nfcError', (error: NFCError) => {
  console.error('NFC Write Error:', error);
});
```

### Working with Different NFC Record Types

The plugin supports different types of NDEF records. Here are examples for common record types:

#### 1. TEXT Records

```typescript
// Reading a TEXT record
NFC.addListener('nfcTag', (data) => {
  data.messages.forEach(message => {
    message.records.forEach(record => {
      if (record.type === 'T') {
        console.log('Text content:', record.payload);
      }
    });
  });
});

// Writing a TEXT record
NFC.writeNDEF({
  records: [
    {
      type: 'T',
      payload: 'This is a text record'
    }
  ]
});
```

#### 2. URI Records

```typescript
// Reading a URI record
NFC.addListener('nfcTag', (data) => {
  data.messages.forEach(message => {
    message.records.forEach(record => {
      if (record.type === 'U') {
        console.log('URI content:', record.payload);
        // Could be "https://example.com"
      }
    });
  });
});

// Writing a URI record
NFC.writeNDEF({
  records: [
    {
      type: 'U',
      payload: 'https://example.com'
    }
  ]
});
```

#### 3. Custom MIME Type Records

```typescript
// Reading a custom MIME type record
NFC.addListener('nfcTag', (data) => {
  data.messages.forEach(message => {
    message.records.forEach(record => {
      if (record.type === 'application/json') {
        console.log('JSON data:', record.payload);
        // Could be a JSON string that you need to parse
        try {
          const jsonData = JSON.parse(record.payload);
          console.log('Parsed JSON:', jsonData);
        } catch (e) {
          console.error('Failed to parse JSON data');
        }
      }
    });
  });
});

// Writing a custom MIME type record
NFC.writeNDEF({
  records: [
    {
      type: 'application/json',
      payload: JSON.stringify({ id: 1, name: 'Product Name' })
    }
  ]
});
```

To write NDEF messages to NFC tags, use the `writeNDEF` method and listen for `nfcWriteSuccess` events.

```typescript
import { NFC, NDEFWriteOptions, NFCError } from '@aacassandra/capacitor-nfc';

const message: NDEFWriteOptions = {
  records: [
    {
      type: 'T', // Text record type
      payload: 'Hello, NFC!',
    },
  ],
};

// Write NDEF message to NFC tag
NFC.writeNDEF(message)
  .then(() => {
    console.log('Write initiated');
  })
  .catch((error) => {
    console.error('Error writing to NFC tag:', error);
  });

// Listen for write success
const nfcWriteSuccessListener = NFC.addListener('nfcWriteSuccess', () => {
  console.log('NDEF message written successfully.');
});

// Handle NFC errors
const nfcErrorListener = NFC.addListener('nfcError', (error: NFCError) => {
  console.error('NFC Error:', error);
});
```

## API

### Methods

#### `startScan()`

Starts the NFC scanning session.

**Returns**: `Promise<void>`

```typescript
NFC.startScan()
  .then(() => {
    // Scanning started
  })
  .catch((error) => {
    console.error('Error starting NFC scan:', error);
  });
```

#### `writeNDEF(options: NDEFWriteOptions)`

Writes an NDEF message to an NFC tag.

**Parameters**:

- `options: NDEFWriteOptions` - The NDEF message to write.

**Returns**: `Promise<void>`

```typescript
NFC.writeNDEF(options)
  .then(() => {
    // Write initiated
  })
  .catch((error) => {
    console.error('Error writing NDEF message:', error);
  });
```

### Listeners

#### `addListener('nfcTag', listener: (data: NDEFMessages) => void)`

Adds a listener for NFC tag detection events.

**Parameters**:

- `eventName: 'nfcTag'`
- `listener: (data: NDEFMessages) => void` - The function to call when an NFC tag is detected.

**Returns**: `PluginListenerHandle`

```typescript
const nfcTagListener = NFC.addListener('nfcTag', (data: NDEFMessages) => {
  console.log('Received NFC tag:', data);
});
```

#### `addListener('nfcError', listener: (error: NFCError) => void)`

Adds a listener for NFC error events.

**Parameters**:

- `eventName: 'nfcError'`
- `listener: (error: NFCError) => void` - The function to call when an NFC error occurs.

**Returns**: `PluginListenerHandle`

```typescript
const nfcErrorListener = NFC.addListener('nfcError', (error: NFCError) => {
  console.error('NFC Error:', error);
});
```

#### `addListener('nfcWriteSuccess', listener: () => void)`

Adds a listener for NFC write success events.

**Parameters**:

- `eventName: 'nfcWriteSuccess'`
- `listener: () => void` - The function to call when an NDEF message has been written successfully.

**Returns**: `PluginListenerHandle`

```typescript
const nfcWriteSuccessListener = NFC.addListener('nfcWriteSuccess', () => {
  console.log('NDEF message written successfully.');
});
```

### Interfaces

#### `NDEFWriteOptions`

Options for writing an NDEF message.

```typescript
interface NDEFWriteOptions {
  records: NDEFRecord[];
}
```

#### `NDEFMessages`

Data received from an NFC tag.

```typescript
interface NDEFMessages {
  messages: NDEFMessage[];
}
```

#### `NDEFMessage`

An NDEF message consisting of one or more records.

```typescript
interface NDEFMessage {
  records: NDEFRecord[];
}
```

#### `NDEFRecord`

An NDEF record.

```typescript
interface NDEFRecord {
  /**
   * The type of the record.
   */
  type: string;

  /**
   * The payload of the record.
   */
  payload: string;
}
```

#### `NFCError`

An NFC error.

```typescript
interface NFCError {
  /**
   * The error message.
   */
  error: string;
}
```

## Integration into a Capacitor App

To integrate this plugin into your Capacitor app:

1. **Install the plugin:**

   ```bash
   npm install @aacassandra/capacitor-nfc
   npx cap sync
   ```

2. **Import the plugin in your code:**

   ```typescript
   import { NFC } from '@aacassandra/capacitor-nfc';
   ```

3. **Use the plugin methods as described in the [Usage](#usage) section.**

## Example

Here's a complete example of how to read and write NFC tags in your app:

```typescript
import { NFC, NDEFMessages, NDEFWriteOptions, NFCError } from '@aacassandra/capacitor-nfc';

// Start NFC scanning
NFC.startScan().catch((error) => {
  console.error('Error starting NFC scan:', error);
});

// Listen for NFC tag detection
const nfcTagListener = NFC.addListener('nfcTag', (data: NDEFMessages) => {
  console.log('Received NFC tag:', data);
});

// Handle NFC errors
const nfcErrorListener = NFC.addListener('nfcError', (error: NFCError) => {
  console.error('NFC Error:', error);
});

// Prepare an NDEF message to write
const message: NDEFWriteOptions = {
  records: [
    {
      type: 'T', // Text record type
      payload: 'Hello, NFC!',
    },
  ],
};

// Write NDEF message to NFC tag
NFC.writeNDEF(message)
  .then(() => {
    console.log('Write initiated');
  })
  .catch((error) => {
    console.error('Error writing to NFC tag:', error);
  });

// Listen for write success
const nfcWriteSuccessListener = NFC.addListener('nfcWriteSuccess', () => {
  console.log('NDEF message written successfully.');
});

// Don't forget to remove listeners when they're no longer needed
// For example, in a Vue or React component's onDestroy/componentWillUnmount:
// nfcTagListener.remove();
// nfcErrorListener.remove();
// nfcWriteSuccessListener.remove();
```

### Android-specific Example

For Android, you might want to add a button to check NFC status and prompt users to enable NFC if it's disabled:

```typescript
import { NFC } from '@aacassandra/capacitor-nfc';

function checkNfcStatus() {
  NFC.startScan()
    .then(() => {
      // NFC is available and enabled
      console.log('NFC is ready to use');
    })
    .catch((error) => {
      // NFC might be unavailable or disabled
      console.error('NFC Error:', error);
      // You could show a dialog to prompt the user to enable NFC
      if (error.includes('not enabled')) {
        // Direct user to NFC settings
        // On Android, you could use App.openUrl to send the user to NFC settings
        // or show an instruction to enable NFC from the notification shade
      }
    });
}
```

## Web Support (Web NFC)

### Web NFC Support

This plugin now supports Web NFC on browsers that implement the [Web NFC API](https://developer.mozilla.org/en-US/docs/Web/API/Web_NFC_API), such as Chrome and Edge on Android. On unsupported browsers (including all iOS browsers), NFC features will throw an error or be unavailable.

#### How It Works
- On web, the plugin uses the browser's native Web NFC API (`NDEFReader`) if available.
- If the browser does not support Web NFC, all NFC methods will throw an error indicating NFC is not supported.
- The API and event listeners are the same as on native platforms, so your code can be cross-platform.

#### Checking NFC Support in Browser
You can check if the current browser supports Web NFC:

```typescript
import { NFC } from '@aacassandra/capacitor-nfc';

NFC.isNFCSupported().then((isSupported) => {
  if (!isSupported) {
    alert('Web NFC is not supported in this browser.');
  }
});
```

#### Example Usage (Web)
```typescript
import { NFC } from '@aacassandra/capacitor-nfc';

// Start scanning (will throw if not supported)
NFC.startScan()
  .then(() => console.log('Web NFC scan started'))
  .catch(err => alert('NFC not supported: ' + err.error));

// Listen for NFC tag
NFC.addListener('nfcTag', (data) => {
  console.log('Web NFC tag:', data);
});
```

#### Limitations
- Web NFC is only available on Android (Chrome/Edge) and not on iOS/Safari or desktop browsers.
- Web NFC API is still experimental and may change in the future.
- Only NDEF tags are supported (no low-level tag access).

---

**Support**: If you encounter any issues or have questions, feel free to open an issue.

---
