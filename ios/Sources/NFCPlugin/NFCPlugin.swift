import Foundation
import Capacitor
import CoreNFC

@objc(NFCPlugin)
public class NFCPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "NFCPlugin"
    public let jsName = "NFC"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startScan", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "writeNDEF", returnType: CAPPluginReturnPromise)
    ]

    private let reader = NFCReader()
    private let writer = NFCWriter()

    @objc func startScan(_ call: CAPPluginCall) {
        print("startScan called")
        
        // Check NFC availability explicitly before starting scan
        let nfcStatus = reader.checkNFCAvailability()
        if !nfcStatus.available {
            call.reject(nfcStatus.message)
            return
        }
        
        reader.onNDEFMessageReceived = { messages in
            var ndefMessages = [[String: Any]]()
            for message in messages {
                var records = [[String: Any]]()
                for record in message.records {
                    let recordType = String(data: record.type, encoding: .utf8) ?? ""
                    
                    // Convert payload to string for consistency with Android
                    var payloadText = ""
                    
                    // Handle TEXT record type specially
                    if recordType == "T" && record.payload.count > 0 {
                        // TEXT record format in NDEF: [status byte][language code][text]
                        let statusByte = record.payload[0]
                        let languageCodeLength = Int(statusByte & 0x3F)
                        
                        if record.payload.count > languageCodeLength + 1 {
                            let range = (languageCodeLength + 1)..<record.payload.count
                            let textPayload = record.payload.subdata(in: range)
                            payloadText = String(data: textPayload, encoding: .utf8) ?? ""
                        }
                    } else {
                        // For other record types
                        payloadText = String(data: record.payload, encoding: .utf8) ?? ""
                    }
                    
                    records.append([
                        "type": recordType,
                        "payload": payloadText
                    ])
                }
                ndefMessages.append([
                    "records": records
                ])
            }
            self.notifyListeners("nfcTag", data: ["messages": ndefMessages])
        }

        reader.onError = { error in
            if let nfcError = error as? NFCReaderError {
                if nfcError.code != .readerSessionInvalidationErrorUserCanceled {
                    self.notifyListeners("nfcError", data: ["error": nfcError.localizedDescription])
                }
            }
        }

        reader.startScanning()
        call.resolve()
    }

    @objc func writeNDEF(_ call: CAPPluginCall) {
        print("writeNDEF called")
        
        // Check NFC availability explicitly before starting write
        let nfcStatus = writer.checkNFCAvailability()
        if !nfcStatus.available {
            call.reject(nfcStatus.message)
            return
        }

        guard let recordsData = call.getArray("records") as? [[String: Any]] else {
            call.reject("Records are required")
            return
        }

        var ndefRecords = [NFCNDEFPayload]()
        for recordData in recordsData {
            guard let type = recordData["type"] as? String,
                  let payload = recordData["payload"] as? String else {
                continue
            }
            
            var finalPayloadData: Data
            var format: NFCTypeNameFormat = .nfcWellKnown
            var typeData: Data
            
            if type == "T" {
                // For TEXT type, need to add status byte and language code
                let languageCode = "en"
                let languageCodeData = languageCode.data(using: .ascii)!
                let statusByte: UInt8 = UInt8(languageCodeData.count & 0x3F)
                
                // Combine status byte + language code + text payload
                var textPayloadData = Data([statusByte])
                textPayloadData.append(languageCodeData)
                if let payloadTextData = payload.data(using: .utf8) {
                    textPayloadData.append(payloadTextData)
                }
                
                finalPayloadData = textPayloadData
                typeData = Data([0x54]) // "T" in ASCII
            } else if type == "U" {
                // For URI records
                // URI Prefix identifier byte (0x00 for "no prefix")
                // Documentation: https://developer.apple.com/documentation/corenfc/nfcndefuripayload
                var uriPayloadData = Data([0x00])
                if let uriData = payload.data(using: .utf8) {
                    uriPayloadData.append(uriData)
                }
                
                finalPayloadData = uriPayloadData
                typeData = Data([0x55]) // "U" in ASCII
            } else {
                // For custom or other types
                finalPayloadData = payload.data(using: .utf8) ?? Data()
                typeData = type.data(using: .utf8) ?? Data()
                format = .media
            }

            let ndefRecord = NFCNDEFPayload(
                format: format,
                type: typeData,
                identifier: Data(),
                payload: finalPayloadData
            )
            ndefRecords.append(ndefRecord)
        }

        let ndefMessage = NFCNDEFMessage(records: ndefRecords)

        writer.onWriteSuccess = {
            self.notifyListeners("nfcWriteSuccess", data: ["success": true])
        }

        writer.onError = { error in
            if let nfcError = error as? NFCReaderError {
                if nfcError.code != .readerSessionInvalidationErrorUserCanceled {
                    self.notifyListeners("nfcError", data: ["error": nfcError.localizedDescription])
                }
            }
        }

        writer.startWriting(message: ndefMessage)
        call.resolve()
    }

    @objc func isNFCSupported(_ call: CAPPluginCall) {
        let supported = NFCNDEFReaderSession.readingAvailable
        call.resolve(["value": supported]) // Capacitor expects a dictionary, but we can return the boolean as the main value
    }
}
