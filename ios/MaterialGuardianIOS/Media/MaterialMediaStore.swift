import Foundation
import PDFKit
import UIKit

struct MaterialMediaStore {
    enum StoredMedia {
        case jpeg(Data)
        case pdf(Data)

        var fileExtension: String {
            switch self {
            case .jpeg: "jpg"
            case .pdf: "pdf"
            }
        }

        var data: Data {
            switch self {
            case .jpeg(let data), .pdf(let data): data
            }
        }
    }

    private let fileManager = FileManager.default

    func persist(media: StoredMedia, for attachment: MediaAttachment) throws -> MediaAttachment {
        let folderURL = try mediaFolderURL()
        try fileManager.createDirectory(at: folderURL, withIntermediateDirectories: true)

        let filename = "\(attachment.id.uuidString.lowercased()).\(media.fileExtension)"
        let fileURL = folderURL.appendingPathComponent(filename)
        try media.data.write(to: fileURL, options: .atomic)

        var stored = attachment
        stored.relativeFilePath = "media/\(filename)"
        return stored
    }

    func deleteFile(for attachment: MediaAttachment) {
        guard let fileURL = fileURL(for: attachment) else { return }
        try? fileManager.removeItem(at: fileURL)
    }

    func fileURL(for attachment: MediaAttachment) -> URL? {
        guard let relativeFilePath = attachment.relativeFilePath else { return nil }
        return try? baseURL().appendingPathComponent(relativeFilePath, isDirectory: false)
    }

    func image(for attachment: MediaAttachment) -> UIImage? {
        guard let fileURL = fileURL(for: attachment) else { return nil }
        if fileURL.pathExtension.lowercased() == "pdf" {
            return pdfThumbnail(for: fileURL)
        }
        return UIImage(contentsOfFile: fileURL.path())
    }

    func pdfDocument(for attachment: MediaAttachment) -> PDFDocument? {
        guard let fileURL = fileURL(for: attachment), fileURL.pathExtension.lowercased() == "pdf" else { return nil }
        return PDFDocument(url: fileURL)
    }

    private func mediaFolderURL() throws -> URL {
        try baseURL().appendingPathComponent("media", isDirectory: true)
    }

    private func baseURL() throws -> URL {
        let supportFolder = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        return supportFolder.appendingPathComponent("MaterialGuardianIOS", isDirectory: true)
    }

    private func pdfThumbnail(for fileURL: URL) -> UIImage? {
        guard let document = PDFDocument(url: fileURL), let page = document.page(at: 0) else { return nil }
        let pageBounds = page.bounds(for: .mediaBox)
        let targetSize = CGSize(width: 240, height: 320)
        let renderer = UIGraphicsImageRenderer(size: targetSize)

        return renderer.image { context in
            UIColor.white.setFill()
            context.fill(CGRect(origin: .zero, size: targetSize))

            context.cgContext.saveGState()
            context.cgContext.translateBy(x: 0, y: targetSize.height)
            context.cgContext.scaleBy(x: 1, y: -1)

            let scale = min(targetSize.width / pageBounds.width, targetSize.height / pageBounds.height)
            let xOffset = (targetSize.width / scale - pageBounds.width) / 2
            let yOffset = (targetSize.height / scale - pageBounds.height) / 2
            context.cgContext.translateBy(x: xOffset, y: yOffset)
            context.cgContext.scaleBy(x: scale, y: scale)
            page.draw(with: .mediaBox, to: context.cgContext)
            context.cgContext.restoreGState()
        }
    }
}
