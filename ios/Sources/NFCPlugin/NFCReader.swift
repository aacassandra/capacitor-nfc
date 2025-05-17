import Foundation
import CoreNFC

@objc public class NFCReader: NSObject, NFCNDEFReaderSessionDelegate {
    private var readerSession: NFCNDEFReaderSession?

    public var onNDEFMessageReceived: (([NFCNDEFMessage]) -> Void)?
    public var onError: ((Error) -> Void)?
    
    // Adding method to explicitly check NFC status
    @objc public func checkNFCAvailability() -> (available: Bool, message: String) {
        // Check if NFC hardware is available
        if !NFCNDEFReaderSession.readingAvailable {
            return (false, "NFC hardware is not available on this device")
        }
        
        // Technically, iOS doesn't provide a direct API to check if NFC is enabled by the user
        // because iOS handles this through system dialogs. However, we can inform the user that NFC is available.
        return (true, "NFC hardware is available")
    }

    @objc public func startScanning() {
        print("NFCReader startScanning called")

        // Check NFC availability first
        let nfcStatus = checkNFCAvailability()
        guard nfcStatus.available else {
            print(nfcStatus.message)
            if let error = NSError(domain: "NFCReaderError", code: 100, userInfo: [NSLocalizedDescriptionKey: nfcStatus.message]) as Error? {
                onError?(error)
            }
            return
        }
        
        readerSession = NFCNDEFReaderSession(delegate: self, queue: nil, invalidateAfterFirstRead: true)
        readerSession?.alertMessage = "Hold your iPhone near the NFC tag."
        readerSession?.begin()
    }

    // NFCNDEFReaderSessionDelegate methods for reading
    public func readerSession(_ session: NFCNDEFReaderSession, didInvalidateWithError error: Error) {
        print("NFC reader session error: \(error.localizedDescription)")
        onError?(error)
    }

    public func readerSession(_ session: NFCNDEFReaderSession, didDetectNDEFs messages: [NFCNDEFMessage]) {
        onNDEFMessageReceived?(messages)
    }

    public func readerSessionDidBecomeActive(_ session: NFCNDEFReaderSession) {
        
    }

    // Handle detection of NDEF tags (need to connect and read the NDEF message)
    public func readerSession(_ session: NFCNDEFReaderSession, didDetect tags: [NFCNDEFTag]) {
        // Handle multiple tag detection more clearly
        // This is important for consistency with Android which also checks for this case
        if tags.count == 0 {
            session.alertMessage = "No tags detected."
            session.invalidate()
            if let error = NSError(domain: "NFCReaderError", code: 101, userInfo: [NSLocalizedDescriptionKey: "No NFC tag detected"]) as Error? {
                onError?(error)
            }
            return
        }
        
        if tags.count > 1 {
            // Restart polling in 500ms
            let retryInterval = DispatchTimeInterval.milliseconds(500)
            session.alertMessage = "More than one tag detected. Please remove extra tags and try again."
            
            // Log warning message for debugging
            print("Multiple tags detected: \(tags.count). Only one tag can be processed at a time.")
            
            DispatchQueue.global().asyncAfter(deadline: .now() + retryInterval) {
                session.restartPolling()
            }
            return
        }
        
        // Connect to the found tag and perform NDEF message reading
        let tag = tags.first!
        session.connect(to: tag) { (error: Error?) in
            if let error = error {
                session.alertMessage = "Unable to connect to tag."
                session.invalidate()
                self.onError?(error)
                return
            }
            
            tag.queryNDEFStatus { (ndefStatus: NFCNDEFStatus, capacity: Int, error: Error?) in
                if let error = error {
                    session.alertMessage = "Unable to query NDEF status of tag."
                    session.invalidate()
                    self.onError?(error)
                    return
                }
                
                if ndefStatus == .notSupported {
                    session.alertMessage = "Tag is not NDEF compliant."
                    session.invalidate()
                    return
                }
                
                tag.readNDEF { (message: NFCNDEFMessage?, error: Error?) in
                    var statusMessage: String
                    if let error = error {
                        statusMessage = "Failed to read NDEF from tag."
                        session.alertMessage = statusMessage
                        session.invalidate()
                        self.onError?(error)
                        return
                    }
                    
                    if let message = message {
                        print("NDEF message read: \(message)")
                        statusMessage = "Found 1 NDEF message."
                        session.alertMessage = statusMessage
                        session.invalidate()
                        self.onNDEFMessageReceived?([message])
                    } else {
                        statusMessage = "No NDEF message found."
                        session.alertMessage = statusMessage
                        session.invalidate()
                    }
                }
            }
        }
    }
}
