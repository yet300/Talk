import SwiftUI
import VisionKit

/// Scans a QR code with the camera (VisionKit's data scanner) and hands over the first payload it reads.
struct QrScanner: UIViewControllerRepresentable {
    let onScanned: (String) -> Void

    /// The device has a camera and the scanner is allowed.
    @MainActor static var isAvailable: Bool {
        DataScannerViewController.isSupported && DataScannerViewController.isAvailable
    }

    func makeUIViewController(context: Context) -> DataScannerViewController {
        let scanner = DataScannerViewController(
            recognizedDataTypes: [.barcode(symbologies: [.qr])],
            qualityLevel: .balanced,
            isHighlightingEnabled: true
        )
        scanner.delegate = context.coordinator
        try? scanner.startScanning()
        return scanner
    }

    func updateUIViewController(_ controller: DataScannerViewController, context: Context) {}

    static func dismantleUIViewController(_ controller: DataScannerViewController, coordinator: Coordinator) {
        controller.stopScanning()
    }

    func makeCoordinator() -> Coordinator { Coordinator(onScanned: onScanned) }

    final class Coordinator: NSObject, DataScannerViewControllerDelegate {
        private let onScanned: (String) -> Void
        private var done = false

        init(onScanned: @escaping (String) -> Void) { self.onScanned = onScanned }

        func dataScanner(_ scanner: DataScannerViewController, didAdd items: [RecognizedItem], allItems: [RecognizedItem]) {
            guard !done else { return }
            for case .barcode(let code) in items {
                if let payload = code.payloadStringValue {
                    done = true
                    onScanned(payload)
                    return
                }
            }
        }
    }
}
