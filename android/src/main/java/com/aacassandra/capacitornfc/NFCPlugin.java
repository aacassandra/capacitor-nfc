package com.aacassandra.capacitornfc;

import android.content.Intent;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONException;

@CapacitorPlugin(name = "NFC")
public class NFCPlugin extends Plugin {

    private NFC implementation;

    @Override
    public void load() {
        implementation = new NFC();
        implementation.init(getActivity());
        implementation.setCallback(new NFC.NFCCallback() {
            @Override
            public void onNdefDiscovered(JSObject data) {
                notifyListeners("nfcTag", data);
            }

            @Override
            public void onUIDDiscovered(JSObject data) {
                notifyListeners("nfcUID", data);
            }

            @Override
            public void onError(String error) {
                JSObject errorObj = new JSObject();
                errorObj.put("error", error);
                notifyListeners("nfcError", errorObj);
            }

            @Override
            public void onWriteSuccess() {
                notifyListeners("nfcWriteSuccess", new JSObject());
            }
        });
    }

    @PluginMethod
    public void startScan(PluginCall call) {
        if (!implementation.isAvailable()) {
            call.reject("NFC is not available on this device");
            return;
        }

        if (!implementation.isEnabled()) {
            call.reject("NFC is not enabled");
            return;
        }

        implementation.startReading();
        call.resolve();
    }

    @PluginMethod
    public void startUIDScan(PluginCall call) {
        if (!implementation.isAvailable()) {
            call.reject("NFC is not available on this device");
            return;
        }

        if (!implementation.isEnabled()) {
            call.reject("NFC is not enabled");
            return;
        }

        implementation.startUIDReading();
        call.resolve();
    }

    @PluginMethod
    public void stopScan(PluginCall call) {
        implementation.stopReading();
        implementation.stopWriting();
        call.resolve();
    }

    @PluginMethod
    public void writeNDEF(PluginCall call) {
        if (!implementation.isAvailable()) {
            call.reject("NFC is not available on this device");
            return;
        }

        if (!implementation.isEnabled()) {
            call.reject("NFC is not enabled");
            return;
        }

        try {
            JSArray jsArray = call.getArray("records");
            JSONArray records = new JSONArray();
            for (int i = 0; i < jsArray.length(); i++) {
                records.put(jsArray.get(i));
            }
            implementation.startWriting(records);
            call.resolve();
        } catch (JSONException e) {
            call.reject("Error writing NDEF message: " + e.getMessage());
        }
    }

    @PluginMethod
    public void isNFCSupported(PluginCall call) {
        boolean supported = implementation.isAvailable();
        JSObject ret = new JSObject();
        ret.put("value", supported);
        call.resolve(ret);
    }

    @Override
    protected void handleOnNewIntent(Intent intent) {
        super.handleOnNewIntent(intent);
        implementation.onNewIntent(intent);
    }

    @Override
    protected void handleOnDestroy() {
        implementation.stopReading();
        implementation.stopWriting();
        super.handleOnDestroy();
    }
}
