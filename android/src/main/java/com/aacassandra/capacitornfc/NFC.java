package com.aacassandra.capacitornfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NFC {
    private static final String TAG = "NFCPlugin";
    private NfcAdapter nfcAdapter;
    private Activity activity;
    private IntentFilter[] intentFiltersArray;
    private PendingIntent pendingIntent;
    private String[][] techList;
    private boolean isReading = false;
    private boolean isUIDReading = false;
    private boolean isWriting = false;
    private NdefMessage messageToWrite;
    private NFCCallback nfcCallback;

    public NFC() {
    }

    public interface NFCCallback {
        void onNdefDiscovered(JSObject data);
        void onUIDDiscovered(JSObject data);
        void onError(String error);
        void onWriteSuccess();
    }

    public void setCallback(NFCCallback callback) {
        this.nfcCallback = callback;
    }

    public void init(Activity activity) {
        this.activity = activity;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(activity);

        Intent intent = new Intent(activity, activity.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = PendingIntent.FLAG_MUTABLE;
        }
        this.pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.e(TAG, "Error adding MIME type", e);
        }

        this.intentFiltersArray = new IntentFilter[] { ndef };
        this.techList = new String[][] { 
            new String[] { Ndef.class.getName() },
            new String[] { NdefFormatable.class.getName() }
        };
    }

    public boolean isAvailable() {
        return this.nfcAdapter != null;
    }

    public boolean isEnabled() {
        return this.nfcAdapter != null && this.nfcAdapter.isEnabled();
    }

    public void startReading() {
        if (!isAvailable()) {
            if (nfcCallback != null) {
                nfcCallback.onError("NFC is not available on this device");
            }
            return;
        }

        if (!isEnabled()) {
            if (nfcCallback != null) {
                nfcCallback.onError("NFC is not enabled");
            }
            return;
        }

        // Reset status to start clean reading
        this.isReading = true;
        this.isUIDReading = false;
        this.isWriting = false;
        
        // Log for debug purposes
        Log.d(TAG, "Starting NFC reading mode");
        
        this.enableForegroundDispatch();
    }

    public void startUIDReading() {
        if (!isAvailable()) {
            if (nfcCallback != null) {
                nfcCallback.onError("NFC is not available on this device");
            }
            return;
        }

        if (!isEnabled()) {
            if (nfcCallback != null) {
                nfcCallback.onError("NFC is not enabled");
            }
            return;
        }

        // Reset status to start clean UID reading
        this.isUIDReading = true;
        this.isReading = false;
        this.isWriting = false;
        
        // Log for debug purposes
        Log.d(TAG, "Starting NFC UID reading mode");
        
        this.enableForegroundDispatch();
    }

    public void startWriting(JSONArray records) {
        if (!isAvailable()) {
            if (nfcCallback != null) {
                nfcCallback.onError("NFC is not available on this device");
            }
            return;
        }

        if (!isEnabled()) {
            if (nfcCallback != null) {
                nfcCallback.onError("NFC is not enabled");
            }
            return;
        }

        try {
            // Validasi records terlebih dahulu
            if (records.length() == 0) {
                if (nfcCallback != null) {
                    nfcCallback.onError("No records provided for writing");
                }
                return;
            }
            
            this.messageToWrite = createNdefMessage(records);
            this.isWriting = true;
            this.isReading = false;
            
            // Log for debug purposes
            Log.d(TAG, "Starting NFC writing mode with " + records.length() + " records");
            
            this.enableForegroundDispatch();
        } catch (Exception e) {
            if (nfcCallback != null) {
                nfcCallback.onError("Error creating NDEF message: " + e.getMessage());
            }
        }
    }

    public void stopReading() {
        this.isReading = false;
        this.isUIDReading = false;
        this.disableForegroundDispatch();
    }

    public void stopWriting() {
        this.isWriting = false;
        this.disableForegroundDispatch();
    }

    private void enableForegroundDispatch() {
        if (this.nfcAdapter != null && this.activity != null) {
            this.nfcAdapter.enableForegroundDispatch(
                this.activity,
                this.pendingIntent,
                this.intentFiltersArray,
                this.techList
            );
        }
    }

    private void disableForegroundDispatch() {
        if (this.nfcAdapter != null && this.activity != null) {
            this.nfcAdapter.disableForegroundDispatch(this.activity);
        }
    }

    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            
            // Check for multiple tags
            // Technically Android only sends one tag in a single intent,
            // but we add this check for consistency with iOS
            // and in anticipation of changes to the Android API in the future
            if (tag == null) {
                if (nfcCallback != null) {
                    nfcCallback.onError("No NFC tag detected");
                }
                return;
            }
            
            if (this.isWriting && this.messageToWrite != null) {
                writeNdefMessage(tag, this.messageToWrite);
            } else if (this.isUIDReading) {
                // Process UID reading
                processUIDData(tag);
            } else if (this.isReading) {
                Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                if (rawMessages != null) {
                    NdefMessage[] messages = new NdefMessage[rawMessages.length];
                    for (int i = 0; i < rawMessages.length; i++) {
                        messages[i] = (NdefMessage) rawMessages[i];
                    }
                    
                    // Additional check - if there is an unusual number of messages
                    // This could indicate multiple tags detected simultaneously
                    // In Android, typically one tag = one NDEF message
                    if (rawMessages.length > 1) {
                        Log.w(TAG, "Multiple NDEF messages detected: " + rawMessages.length + ". This might indicate multiple tags.");
                        // We still process all messages, but log a warning
                    }
                    
                    this.processNdefMessages(messages);
                }
            } else if (this.isUIDReading) {
                // Handle UID reading
                String uid = Arrays.toString(tag.getId());
                JSObject uidData = new JSObject();
                uidData.put("uid", uid);
                
                if (nfcCallback != null) {
                    nfcCallback.onUIDDiscovered(uidData);
                }
            }
        }
    }

    private void processNdefMessages(NdefMessage[] messages) {
        if (nfcCallback == null) return;

        try {
            JSObject result = new JSObject();
            JSArray messagesArray = new JSArray();

            for (NdefMessage ndefMessage : messages) {
                JSObject messageObj = new JSObject();
                JSArray recordsArray = new JSArray();

                for (NdefRecord record : ndefMessage.getRecords()) {
                    JSObject recordObj = new JSObject();
                    
                    // Set record type
                    String type = "";
                    if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && 
                        Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                        type = "T"; // Text record
                    } else if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && 
                               Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                        type = "U"; // URI record
                    } else {
                        type = new String(record.getType(), Charset.forName("UTF-8"));
                    }
                    recordObj.put("type", type);
                    
                    // Set record payload
                    byte[] payload = record.getPayload();
                    String payloadText = new String(payload, Charset.forName("UTF-8"));
                    
                    // For TEXT records, remove the language code
                    if (type.equals("T") && payload.length > 0) {
                        int languageCodeLength = payload[0] & 0x3F;
                        if (payload.length > languageCodeLength + 1) {
                            payloadText = new String(
                                Arrays.copyOfRange(payload, languageCodeLength + 1, payload.length),
                                Charset.forName("UTF-8")
                            );
                        }
                    }
                    
                    recordObj.put("payload", payloadText);
                    recordsArray.put(recordObj);
                }

                messageObj.put("records", recordsArray);
                messagesArray.put(messageObj);
            }

            result.put("messages", messagesArray);
            nfcCallback.onNdefDiscovered(result);
        } catch (Exception e) {
            nfcCallback.onError("Error processing NDEF message: " + e.getMessage());
        }
    }

    private void processUIDData(Tag tag) {
        if (nfcCallback == null) return;

        try {
            // Get UID from tag
            byte[] uid = tag.getId();
            if (uid == null || uid.length == 0) {
                nfcCallback.onError("Tag UID tidak dapat dibaca");
                return;
            }

            // Convert UID to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : uid) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            String uidHex = hexString.toString();
            
            // Format UID with spaces for readability
            StringBuilder formattedUID = new StringBuilder();
            for (int i = 0; i < uidHex.length(); i += 2) {
                if (i > 0) formattedUID.append(" ");
                formattedUID.append(uidHex.substring(i, Math.min(i + 2, uidHex.length())));
            }

            // Determine card type based on UID length (similar to desktop version)
            String cardType = "Tidak diketahui";
            int uidLengthBytes = uid.length;
            if (uidLengthBytes == 4) {
                cardType = "Kemungkinan MIFARE Classic 1K/4K, atau kartu 4-byte UID lainnya";
            } else if (uidLengthBytes == 7) {
                cardType = "Kemungkinan MIFARE Ultralight, NTAG, atau kartu 7-byte UID lainnya";
            }

            // Get tech list
            String[] techList = tag.getTechList();
            JSArray techArray = new JSArray();
            for (String tech : techList) {
                techArray.put(tech);
            }

            // Create result object
            JSObject result = new JSObject();
            result.put("uid", uidHex);
            result.put("uidFormatted", formattedUID.toString().toUpperCase());
            result.put("uidLength", uidLengthBytes);
            result.put("cardType", cardType);
            result.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
            result.put("techList", techArray);

            Log.d(TAG, "UID detected: " + uidHex + " (" + uidLengthBytes + " bytes)");
            nfcCallback.onUIDDiscovered(result);
        } catch (Exception e) {
            nfcCallback.onError("Error processing UID data: " + e.getMessage());
        }
    }

    private NdefMessage createNdefMessage(JSONArray recordsArray) throws JSONException, UnsupportedEncodingException {
        List<NdefRecord> records = new ArrayList<>();

        for (int i = 0; i < recordsArray.length(); i++) {
            JSONObject record = recordsArray.getJSONObject(i);
            String type = record.getString("type");
            String payload = record.getString("payload");

            if (type.equals("T")) {
                // Create a TEXT record
                // Format: [status byte][language code][text]
                String languageCode = "en";
                byte[] languageCodeBytes = languageCode.getBytes(Charset.forName("US-ASCII"));
                byte[] textBytes = payload.getBytes(Charset.forName("UTF-8"));
                
                byte[] data = new byte[1 + languageCodeBytes.length + textBytes.length];
                data[0] = (byte) (languageCodeBytes.length & 0x3F); // Language code length
                System.arraycopy(languageCodeBytes, 0, data, 1, languageCodeBytes.length);
                System.arraycopy(textBytes, 0, data, 1 + languageCodeBytes.length, textBytes.length);
                
                records.add(NdefRecord.createTextRecord(languageCode, payload));
            } else if (type.equals("U")) {
                // Create a URI record
                records.add(NdefRecord.createUri(payload));
            } else {
                // Create a custom record (MIME type)
                records.add(NdefRecord.createMime(type, payload.getBytes(Charset.forName("UTF-8"))));
            }
        }

        NdefRecord[] ndefRecordsArray = new NdefRecord[records.size()];
        records.toArray(ndefRecordsArray);
        return new NdefMessage(ndefRecordsArray);
    }

    private void writeNdefMessage(Tag tag, NdefMessage message) {
        try {
            // Additional check to ensure tag is valid and singular
            if (tag == null) {
                if (nfcCallback != null) {
                    nfcCallback.onError("Tag is null or invalid");
                }
                return;
            }

            // Perform check to ensure only one tag technology is available
            // This is an approach to detect multiple tags
            String[] techList = tag.getTechList();
            if (techList.length == 0) {
                if (nfcCallback != null) {
                    nfcCallback.onError("No technologies available on this tag");
                }
                return;
            }

            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (ndef.isWritable()) {
                    ndef.writeNdefMessage(message);
                    if (nfcCallback != null) {
                        nfcCallback.onWriteSuccess();
                    }
                } else {
                    if (nfcCallback != null) {
                        nfcCallback.onError("Tag is read-only");
                    }
                }
                ndef.close();
            } else {
                NdefFormatable ndefFormatable = NdefFormatable.get(tag);
                if (ndefFormatable != null) {
                    try {
                        ndefFormatable.connect();
                        ndefFormatable.format(message);
                        if (nfcCallback != null) {
                            nfcCallback.onWriteSuccess();
                        }
                    } catch (IOException | FormatException e) {
                        if (nfcCallback != null) {
                            nfcCallback.onError("Failed to format tag: " + e.getMessage());
                        }
                    } finally {
                        ndefFormatable.close();
                    }
                } else {
                    if (nfcCallback != null) {
                        nfcCallback.onError("Tag doesn't support NDEF");
                    }
                }
            }
        } catch (Exception e) {
            if (nfcCallback != null) {
                nfcCallback.onError("Error writing to tag: " + e.getMessage());
            }
        }
    }
}
