import Foundation
import PDFKit
import UIKit

struct ExportedJobBundle {
    let folderURL: URL
    let relativeFolderPath: String
    let packetURLs: [URL]
    let scanSourceCount: Int
    let photoCount: Int
}

struct JobExporter {
    enum ExportError: LocalizedError {
        case emptyJob

        var errorDescription: String? {
            switch self {
            case .emptyJob:
                "Add at least one receiving report before exporting this job."
            }
        }
    }

    private let fileManager = FileManager.default
    private let mediaStore = MaterialMediaStore()
    private let signatureStore = SignatureAssetStore()

    func export(job: Job) throws -> ExportedJobBundle {
        guard !job.materials.isEmpty else {
            throw ExportError.emptyJob
        }

        let relativeFolderPath = "MaterialGuardian/\(sanitize(job.jobNumber, fallback: "JOB"))"
        let folderURL = try folderURL(forRelativePath: relativeFolderPath)
        let packetsURL = folderURL.appendingPathComponent("material_packets", isDirectory: true)

        try fileManager.createDirectory(at: packetsURL, withIntermediateDirectories: true)
        try clearDirectoryContents(at: packetsURL)

        var packetURLs: [URL] = []

        for (index, material) in job.materials.enumerated() {
            let packetURL = packetsURL.appendingPathComponent(packetFileName(for: material, index: index))
            try renderPacketPDF(for: material, job: job, outputURL: packetURL)
            packetURLs.append(packetURL)
        }

        try writeManifest(for: job, relativeFolderPath: relativeFolderPath, packetURLs: packetURLs, at: folderURL)

        return ExportedJobBundle(
            folderURL: folderURL,
            relativeFolderPath: relativeFolderPath,
            packetURLs: packetURLs,
            scanSourceCount: job.materials.reduce(0) { $0 + $1.scanCount },
            photoCount: job.materials.reduce(0) { $0 + $1.photoCount }
        )
    }

    func folderURL(forRelativePath relativePath: String) throws -> URL {
        let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        return relativePath
            .split(separator: "/")
            .reduce(documentsURL) { partial, component in
                partial.appendingPathComponent(String(component), isDirectory: true)
            }
    }

    private func clearDirectoryContents(at url: URL) throws {
        guard fileManager.fileExists(atPath: url.path()) else { return }
        for child in try fileManager.contentsOfDirectory(at: url, includingPropertiesForKeys: nil) {
            try fileManager.removeItem(at: child)
        }
    }

    private func writeManifest(for job: Job, relativeFolderPath: String, packetURLs: [URL], at folderURL: URL) throws {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short

        let lines = [
            "Material Guardian iOS Export",
            "Job: \(job.jobNumber)",
            "Description: \(job.description.isEmpty ? "N/A" : job.description)",
            "Exported: \(formatter.string(from: .now))",
            "Folder: \(relativeFolderPath)",
            "",
            "Packets:",
        ] + packetURLs.map { "- \($0.lastPathComponent)" }

        let manifestURL = folderURL.appendingPathComponent("export_manifest.txt")
        try lines.joined(separator: "\n").write(to: manifestURL, atomically: true, encoding: .utf8)
    }

    private func packetFileName(for material: MaterialRecord, index: Int) -> String {
        let descriptionStem = sanitize(material.description, fallback: "material_\(index + 1)")
        return String(format: "%02d_%@_packet.pdf", index + 1, descriptionStem)
    }

    private func sanitize(_ text: String, fallback: String) -> String {
        let allowed = CharacterSet.alphanumerics.union(.init(charactersIn: "-_"))
        let cleaned = text
            .lowercased()
            .replacingOccurrences(of: " ", with: "_")
            .unicodeScalars
            .map { allowed.contains($0) ? Character($0) : "_" }
        let compact = String(cleaned)
            .replacingOccurrences(of: "__+", with: "_", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "_"))
        return compact.isEmpty ? fallback : compact
    }

    private func renderPacketPDF(for material: MaterialRecord, job: Job, outputURL: URL) throws {
        let bounds = CGRect(x: 0, y: 0, width: 612, height: 792)
        let format = UIGraphicsPDFRendererFormat()
        let renderer = UIGraphicsPDFRenderer(bounds: bounds, format: format)
        let data = renderer.pdfData { context in
            renderReceivingPage(for: material, job: job, bounds: bounds, context: context)
            if !material.scanAttachments.isEmpty {
                renderScansPage(for: material, bounds: bounds, context: context)
            }
            if !material.photoAttachments.isEmpty {
                renderPhotosPage(for: material, bounds: bounds, context: context)
            }
        }
        try data.write(to: outputURL, options: .atomic)
    }

    private func renderReceivingPage(
        for material: MaterialRecord,
        job: Job,
        bounds: CGRect,
        context: UIGraphicsPDFRendererContext
    ) {
        context.beginPage()

        let margin: CGFloat = 36
        let contentWidth = bounds.width - (margin * 2)

        let titleAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 24, weight: .bold),
            .foregroundColor: UIColor.black
        ]
        let subtitleAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11, weight: .semibold),
            .foregroundColor: UIColor.darkGray
        ]
        let headerAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 13, weight: .semibold),
            .foregroundColor: UIColor.black
        ]
        let bodyAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11),
            .foregroundColor: UIColor.black
        ]

        var y = margin
        "Welders Helper".draw(at: CGPoint(x: margin, y: y), withAttributes: subtitleAttributes)
        y += 18
        "Material Guardian Receiving Packet".draw(at: CGPoint(x: margin, y: y), withAttributes: titleAttributes)
        y += 30

        let sections: [(String, [String])] = [
            (
                "Job",
                [
                    "Job number: \(job.jobNumber)",
                    "Job description: \(job.description.isEmpty ? "N/A" : job.description)",
                    "Exported from iOS on \(formattedDate(.now))",
                ]
            ),
            (
                "Receiving",
                [
                    "Material description: \(value(material.description))",
                    "Vendor: \(value(material.vendor))",
                    "Quantity: \(value(material.quantity))",
                    "PO number: \(value(material.poNumber))",
                    "Received on: \(formattedDate(material.receivedAt))",
                ]
            ),
            (
                "Specification",
                [
                    "Product type: \(value(material.productType))",
                    "Specification prefix: \(value(material.specificationPrefix))",
                    "Grade / spec: \(value(material.gradeType))",
                    "B16 selection: \(b16Summary(material))",
                    "Dimensions: \(dimensionSummary(material))",
                    "Markings: \(value(material.markings))",
                ]
            ),
            (
                "Inspection",
                [
                    "Visual inspection acceptable: \(yesNo(material.visualInspectionAcceptable))",
                    "B16 dimensions acceptable: \(value(material.b16DimensionsAcceptable))",
                    "Marking acceptable: \(yesNoNa(yes: material.markingAcceptable, isNa: material.markingAcceptableNa))",
                    "MTR/CoC acceptable: \(yesNoNa(yes: material.mtrAcceptable, isNa: material.mtrAcceptableNa))",
                    "Disposition: \(material.acceptanceStatus.title)",
                    "Material approval: \(material.materialApproval.title)",
                ]
            ),
            (
                "Quality",
                [
                    "Inspector date: \(formattedOptionalDate(material.qcDate))",
                    "Manager date: \(formattedOptionalDate(material.qcManagerDate))",
                    "Comments: \(value(material.comments))",
                ]
            )
        ]

        for section in sections {
            y = drawSection(section.0, lines: section.1, startY: y, x: margin, width: contentWidth, headerAttributes: headerAttributes, bodyAttributes: bodyAttributes)
            y += 8
        }

        y += 12
        let signatureWidth = (contentWidth - 16) / 2
        drawSignatureBox(
            title: "QC signature",
            signature: material.qcInspectorSignature,
            date: material.qcDate,
            frame: CGRect(x: margin, y: y, width: signatureWidth, height: 92),
            headerAttributes: headerAttributes,
            bodyAttributes: bodyAttributes
        )
        drawSignatureBox(
            title: "QC manager signature",
            signature: material.qcManagerSignature,
            date: material.qcManagerDate,
            frame: CGRect(x: margin + signatureWidth + 16, y: y, width: signatureWidth, height: 92),
            headerAttributes: headerAttributes,
            bodyAttributes: bodyAttributes
        )
    }

    private func renderScansPage(
        for material: MaterialRecord,
        bounds: CGRect,
        context: UIGraphicsPDFRendererContext
    ) {
        let renderedStoredPages = material.scanAttachments.reduce(false) { partialResult, attachment in
            renderStoredScanPages(for: attachment, bounds: bounds, context: context) || partialResult
        }

        if renderedStoredPages {
            return
        }

        context.beginPage()

        let margin: CGFloat = 36
        let headerAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 22, weight: .bold),
            .foregroundColor: UIColor.black
        ]
        let bodyAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 12),
            .foregroundColor: UIColor.black
        ]

        "MTR / CoC Scan Pages".draw(at: CGPoint(x: margin, y: margin), withAttributes: headerAttributes)

        let intro = "Current iOS scaffold export includes a packet page for each saved scan attachment. Real scanner capture can plug into this contract without changing downstream packet naming."
        drawParagraph(intro, at: CGRect(x: margin, y: margin + 34, width: bounds.width - (margin * 2), height: 48), attributes: bodyAttributes)

        var y = margin + 104
        for (index, attachment) in material.scanAttachments.enumerated() {
            let box = CGRect(x: margin, y: y, width: bounds.width - (margin * 2), height: 76)
            UIColor(white: 0.96, alpha: 1).setFill()
            UIBezierPath(roundedRect: box, cornerRadius: 12).fill()
            UIColor.darkGray.setStroke()
            UIBezierPath(roundedRect: box, cornerRadius: 12).stroke()

            let lines = [
                "Scan \(index + 1): \(attachment.label)",
                "Source: \(attachment.source == .cameraFallback ? "Camera fallback" : "Document scanner")",
                "Captured: \(formattedDate(attachment.createdAt))",
            ]
            _ = drawSection(nil, lines: lines, startY: y + 14, x: margin + 16, width: box.width - 32, headerAttributes: bodyAttributes, bodyAttributes: bodyAttributes)
            y += 92
        }
    }

    private func renderPhotosPage(
        for material: MaterialRecord,
        bounds: CGRect,
        context: UIGraphicsPDFRendererContext
    ) {
        context.beginPage()

        let margin: CGFloat = 36
        let gutter: CGFloat = 16
        let usableWidth = bounds.width - (margin * 2) - gutter
        let boxWidth = usableWidth / 2
        let boxHeight: CGFloat = 220
        let titleAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 22, weight: .bold),
            .foregroundColor: UIColor.black
        ]
        let bodyAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 11),
            .foregroundColor: UIColor.black
        ]

        "Material Photo Pages".draw(at: CGPoint(x: margin, y: margin), withAttributes: titleAttributes)

        for (index, attachment) in material.photoAttachments.prefix(4).enumerated() {
            let row = CGFloat(index / 2)
            let column = CGFloat(index % 2)
            let x = margin + column * (boxWidth + gutter)
            let y = margin + 44 + row * (boxHeight + gutter)
            let frame = CGRect(x: x, y: y, width: boxWidth, height: boxHeight)

            UIColor(white: 0.95, alpha: 1).setFill()
            UIBezierPath(roundedRect: frame, cornerRadius: 14).fill()
            UIColor.gray.setStroke()
            UIBezierPath(roundedRect: frame, cornerRadius: 14).stroke()

            let inset = frame.insetBy(dx: 18, dy: 18)
            "Photo \(index + 1)".draw(at: CGPoint(x: inset.minX, y: inset.minY), withAttributes: titleAttributes)
            if let image = mediaStore.image(for: attachment) {
                let imageFrame = CGRect(x: inset.minX, y: inset.minY + 34, width: inset.width, height: inset.height - 72)
                drawAspectFit(image: image, in: imageFrame)
                drawParagraph(
                    "Label: \(attachment.label)\nCaptured: \(formattedDate(attachment.createdAt))",
                    at: CGRect(x: inset.minX, y: frame.maxY - 46, width: inset.width, height: 40),
                    attributes: bodyAttributes
                )
            } else {
                drawParagraph(
                    """
                    Label: \(attachment.label)
                    Captured: \(formattedDate(attachment.createdAt))

                    Current scaffold stores attachment metadata and export slot placement. Real camera images will drop into these packet pages next.
                    """,
                    at: CGRect(x: inset.minX, y: inset.minY + 34, width: inset.width, height: inset.height - 34),
                    attributes: bodyAttributes
                )
            }
        }
    }

    private func renderStoredScanPages(
        for attachment: MediaAttachment,
        bounds: CGRect,
        context: UIGraphicsPDFRendererContext
    ) -> Bool {
        if let document = mediaStore.pdfDocument(for: attachment), document.pageCount > 0 {
            for index in 0..<document.pageCount {
                guard let page = document.page(at: index) else { continue }
                context.beginPage()
                render(pdfPage: page, attachment: attachment, bounds: bounds)
            }
            return true
        }

        guard let image = mediaStore.image(for: attachment) else { return false }

        context.beginPage()
        let margin: CGFloat = 24
        let titleAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 16, weight: .semibold),
            .foregroundColor: UIColor.black
        ]
        attachment.label.draw(at: CGPoint(x: margin, y: margin), withAttributes: titleAttributes)
        drawAspectFit(image: image, in: CGRect(x: margin, y: margin + 28, width: bounds.width - (margin * 2), height: bounds.height - (margin * 2) - 28))
        return true
    }

    private func render(pdfPage: PDFPage, attachment: MediaAttachment, bounds: CGRect) {
        let margin: CGFloat = 24
        let titleAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 16, weight: .semibold),
            .foregroundColor: UIColor.black
        ]
        attachment.label.draw(at: CGPoint(x: margin, y: margin), withAttributes: titleAttributes)

        guard let context = UIGraphicsGetCurrentContext() else { return }
        let pageBounds = pdfPage.bounds(for: .mediaBox)
        let drawRect = CGRect(x: margin, y: margin + 28, width: bounds.width - (margin * 2), height: bounds.height - (margin * 2) - 28)
        let scale = min(drawRect.width / pageBounds.width, drawRect.height / pageBounds.height)

        context.saveGState()
        context.translateBy(x: drawRect.minX, y: drawRect.maxY)
        context.scaleBy(x: scale, y: -scale)
        pdfPage.draw(with: .mediaBox, to: context)
        context.restoreGState()
    }

    private func drawAspectFit(image: UIImage, in rect: CGRect) {
        guard image.size.width > 0, image.size.height > 0 else { return }
        let scale = min(rect.width / image.size.width, rect.height / image.size.height)
        let size = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let drawRect = CGRect(
            x: rect.midX - size.width / 2,
            y: rect.midY - size.height / 2,
            width: size.width,
            height: size.height
        )
        image.draw(in: drawRect)
    }

    private func drawSection(
        _ title: String?,
        lines: [String],
        startY: CGFloat,
        x: CGFloat,
        width: CGFloat,
        headerAttributes: [NSAttributedString.Key: Any],
        bodyAttributes: [NSAttributedString.Key: Any]
    ) -> CGFloat {
        var y = startY
        if let title {
            title.draw(at: CGPoint(x: x, y: y), withAttributes: headerAttributes)
            y += 20
        }
        for line in lines {
            let height = drawParagraph(line, at: CGRect(x: x, y: y, width: width, height: 44), attributes: bodyAttributes)
            y += height + 4
        }
        return y
    }

    @discardableResult
    private func drawParagraph(_ text: String, at rect: CGRect, attributes: [NSAttributedString.Key: Any]) -> CGFloat {
        let paragraph = NSMutableParagraphStyle()
        paragraph.lineBreakMode = .byWordWrapping

        let mergedAttributes = attributes.merging([.paragraphStyle: paragraph]) { _, new in new }
        let attributed = NSAttributedString(string: text, attributes: mergedAttributes)
        let measured = attributed.boundingRect(with: CGSize(width: rect.width, height: .greatestFiniteMagnitude), options: [.usesLineFragmentOrigin, .usesFontLeading], context: nil)
        attributed.draw(in: CGRect(x: rect.minX, y: rect.minY, width: rect.width, height: ceil(measured.height)))
        return ceil(measured.height)
    }

    private func drawSignatureBox(
        title: String,
        signature: SignatureCapture?,
        date: Date?,
        frame: CGRect,
        headerAttributes: [NSAttributedString.Key: Any],
        bodyAttributes: [NSAttributedString.Key: Any]
    ) {
        title.draw(at: CGPoint(x: frame.minX + 12, y: frame.minY + 12), withAttributes: headerAttributes)

        let signatureRect = CGRect(x: frame.minX + 12, y: frame.minY + 28, width: frame.width - 24, height: frame.height - 52)
        if let signature, !signature.isEmpty {
            if let storedImage = signatureStore.image(for: signature) {
                drawSignatureImage(storedImage, in: signatureRect)
            } else {
                drawSignature(signature, in: signatureRect)
            }
        }

        let signatureLineY = frame.maxY - 10
        let signatureLineStart = CGPoint(x: frame.minX + 12, y: signatureLineY)
        let signatureLineEnd = CGPoint(x: frame.minX + frame.width * 0.68, y: signatureLineY)
        let dateLineStart = CGPoint(x: frame.minX + frame.width * 0.76, y: signatureLineY)
        let dateLineEnd = CGPoint(x: frame.maxX - 12, y: signatureLineY)

        let signatureLinePath = UIBezierPath()
        signatureLinePath.move(to: signatureLineStart)
        signatureLinePath.addLine(to: signatureLineEnd)
        UIColor.gray.setStroke()
        signatureLinePath.stroke()

        let dateLinePath = UIBezierPath()
        dateLinePath.move(to: dateLineStart)
        dateLinePath.addLine(to: dateLineEnd)
        dateLinePath.stroke()

        drawParagraph("Signature", at: CGRect(x: signatureLineStart.x, y: signatureLineY + 2, width: signatureLineEnd.x - signatureLineStart.x, height: 16), attributes: bodyAttributes)
        if let date {
            let dateText = formattedOptionalDate(date)
            drawParagraph(dateText, at: CGRect(x: dateLineStart.x, y: signatureLineY - 16, width: dateLineEnd.x - dateLineStart.x, height: 16), attributes: bodyAttributes)
        }
        drawParagraph("Date", at: CGRect(x: dateLineStart.x, y: signatureLineY + 2, width: dateLineEnd.x - dateLineStart.x, height: 16), attributes: bodyAttributes)
    }

    private func drawSignatureImage(_ image: UIImage, in rect: CGRect) {
        image.draw(in: aspectFitRect(for: image.size, inside: rect))
    }

    private func drawSignature(_ signature: SignatureCapture, in rect: CGRect) {
        UIColor.black.setStroke()
        for stroke in signature.strokes where stroke.count > 1 {
            let path = UIBezierPath()
            path.lineWidth = 2
            path.lineCapStyle = .round
            path.lineJoinStyle = .round

            for (index, point) in stroke.enumerated() {
                let mapped = CGPoint(
                    x: rect.minX + CGFloat(point.x) * rect.width,
                    y: rect.minY + CGFloat(point.y) * rect.height
                )
                if index == 0 {
                    path.move(to: mapped)
                } else {
                    path.addLine(to: mapped)
                }
            }
            path.stroke()
        }
    }

    private func aspectFitRect(for size: CGSize, inside rect: CGRect) -> CGRect {
        guard size.width > 0, size.height > 0 else { return rect }
        let scale = min(rect.width / size.width, rect.height / size.height)
        let fittedSize = CGSize(width: size.width * scale, height: size.height * scale)
        return CGRect(
            x: rect.midX - fittedSize.width / 2,
            y: rect.midY - fittedSize.height / 2,
            width: fittedSize.width,
            height: fittedSize.height
        )
    }

    private func yesNo(_ value: Bool) -> String {
        value ? "Yes" : "No"
    }

    private func yesNoNa(yes: Bool, isNa: Bool) -> String {
        if isNa { return "N/A" }
        return yes ? "Yes" : "No"
    }

    private func value(_ text: String) -> String {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? "N/A" : trimmed
    }

    private func dimensionSummary(_ material: MaterialRecord) -> String {
        let parts = [
            "Unit: \(material.dimensionUnit.title)",
            "TH1 \(value(material.thickness1))",
            "TH2 \(value(material.thickness2))",
            "TH3 \(value(material.thickness3))",
            "TH4 \(value(material.thickness4))",
            "Width \(value(material.width))",
            "Length \(value(material.length))",
            "Diameter \(value(material.diameter)) \(value(material.diameterType))",
        ]
        return parts.joined(separator: " | ")
    }

    private func formattedDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    private func formattedOptionalDate(_ date: Date?) -> String {
        guard let date else { return "" }
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter.string(from: date)
    }

    private func b16Summary(_ material: MaterialRecord) -> String {
        guard !material.fittingStandard.isEmpty else { return "N/A" }
        if material.fittingSuffix.isEmpty {
            return material.fittingStandard
        }
        return "\(material.fittingStandard)-\(material.fittingSuffix)"
    }
}
