import Foundation
import UIKit

struct SignatureAssetStore {
    private let fileManager = FileManager.default

    func persist(signature: SignatureCapture) throws -> SignatureCapture {
        guard !signature.isEmpty else { return signature }
        let folderURL = try signaturesFolderURL()
        try fileManager.createDirectory(at: folderURL, withIntermediateDirectories: true)

        let filename = "\(ISO8601DateFormatter().string(from: signature.capturedAt).replacingOccurrences(of: ":", with: "-"))-\(UUID().uuidString.lowercased()).png"
        let fileURL = folderURL.appendingPathComponent(filename)

        guard let pngData = renderImage(for: signature)?.pngData() else {
            return signature
        }

        try pngData.write(to: fileURL, options: .atomic)

        var stored = signature
        stored.relativeFilePath = "signatures/\(filename)"
        return stored
    }

    func deleteFile(for signature: SignatureCapture?) {
        guard
            let signature,
            let fileURL = fileURL(for: signature)
        else { return }

        try? fileManager.removeItem(at: fileURL)
    }

    func image(for signature: SignatureCapture) -> UIImage? {
        if let fileURL = fileURL(for: signature), let image = UIImage(contentsOfFile: fileURL.path()) {
            return image
        }
        return renderImage(for: signature)
    }

    private func fileURL(for signature: SignatureCapture) -> URL? {
        guard let relativeFilePath = signature.relativeFilePath else { return nil }
        return try? baseURL().appendingPathComponent(relativeFilePath, isDirectory: false)
    }

    private func signaturesFolderURL() throws -> URL {
        try baseURL().appendingPathComponent("signatures", isDirectory: true)
    }

    private func baseURL() throws -> URL {
        let supportFolder = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        return supportFolder.appendingPathComponent("MaterialGuardianIOS", isDirectory: true)
    }

    private func renderImage(for signature: SignatureCapture) -> UIImage? {
        let filteredStrokes = signature.strokes.filter { !$0.isEmpty }
        guard !filteredStrokes.isEmpty else { return nil }

        let size = CGSize(width: 1200, height: 400)
        let renderer = UIGraphicsImageRenderer(size: size)

        return renderer.image { context in
            UIColor.clear.setFill()
            context.fill(CGRect(origin: .zero, size: size))

            let path = UIBezierPath()
            path.lineWidth = 8
            path.lineCapStyle = .round
            path.lineJoinStyle = .round

            for stroke in filteredStrokes {
                guard let first = stroke.first else { continue }
                path.move(to: CGPoint(x: first.x * size.width, y: first.y * size.height))
                for point in stroke.dropFirst() {
                    path.addLine(to: CGPoint(x: point.x * size.width, y: point.y * size.height))
                }
            }

            UIColor.black.setStroke()
            path.stroke()
        }
    }
}
