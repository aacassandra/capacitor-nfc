import Foundation
import CoreNFC

@objc public class NFCWriter: NSObject, NFCNDEFReaderSessionDelegate {
    private var writerSession: NFCNDEFReaderSession?
    private var messageToWrite: NFCNDEFMessage?

    public var onWriteSuccess: (() -> Void)?
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

    @objc public func startWriting(message: NFCNDEFMessage) {
        print("NFCWriter startWriting called")
        self.messageToWrite = message

        // Check NFC availability first
        let nfcStatus = checkNFCAvailability()
        guard nfcStatus.available else {
            print(nfcStatus.message)
            if let error = NSError(domain: "NFCWriterError", code: 100, userInfo: [NSLocalizedDescriptionKey: nfcStatus.message]) as Error? {
                onError?(error)
            }
            return
        }
        
        writerSession = NFCNDEFReaderSession(delegate: self, queue: nil, invalidateAfterFirstRead: false)
        writerSession?.alertMessage = "Hold your iPhone near the NFC tag to write."
        writerSession?.begin()
    }

    public func readerSession(_ session: NFCNDEFReaderSession, didDetectNDEFs messages: [NFCNDEFMessage]) {
    }

    // NFCNDEFReaderSessionDelegate methods for writing
    public func readerSession(_ session: NFCNDEFReaderSession, didInvalidateWithError error: Error) {
        print("NFC writer session error: \(error.localizedDescription)")
        onError?(error)
    }

    public func readerSessionDidBecomeActive(_ session: NFCNDEFReaderSession) {
        
    }

    public func readerSession(_ session: NFCNDEFReaderSession, didDetect tags: [NFCNDEFTag]) {
        // Handle multiple tag detection more clearly
        // This is important for consistency with Android which also checks for this case
        if tags.count == 0 {
            session.alertMessage = "No tags detected."
            session.invalidate()
            if let error = NSError(domain: "NFCWriterError", code: 101, userInfo: [NSLocalizedDescriptionKey: "No NFC tag detected"]) as Error? {
                onError?(error)
            }
            return
        }
        
        if tags.count > 1 {
            let retryInterval = DispatchTimeInterval.milliseconds(500)
            session.alertMessage = "More than one tag detected. Please remove extra tags and try again."
            
            // Log warning message for debugging
            print("Multiple tags detected: \(tags.count). Only one tag can be written at a time.")
            
            DispatchQueue.global().asyncAfter(deadline: .now() + retryInterval) {
                session.restartPolling()
            }
            return
        }

        guard let tag = tags.first else { return }

        session.connect(to: tag) { (error) in
            if let error = error {
                session.alertMessage = "Unable to connect to tag."
                session.invalidate()
                self.onError?(error)
                return
            }

            tag.queryNDEFStatus { (ndefStatus, capacity, error) in
                if let error = error {
                    session.alertMessage = "Unable to query the NDEF status of tag."
                    session.invalidate()
                    self.onError?(error)
                    return
                }

                switch ndefStatus {
                case .notSupported:
                    session.alertMessage = "Tag is not NDEF compliant."
                    session.invalidate()
                case .readOnly:
                    session.alertMessage = "Tag is read-only."
                    session.invalidate()
                case .readWrite:
                    if let message = self.messageToWrite {
                        tag.writeNDEF(message) { (error) in
                            if let error = error {
                                session.alertMessage = "Failed to write NDEF message."
                                session.invalidate()
                                self.onError?(error)
                                return
                            }
                            session.alertMessage = "NDEF message written successfully."
                            session.invalidate()
                            self.onWriteSuccess?()
                        }
                    } else {
                        session.alertMessage = "No message to write."
                        session.invalidate()
                    }
                @unknown default:
                    session.alertMessage = "Unknown NDEF tag status."
                    session.invalidate()
                }
            }
        }
    }
}
