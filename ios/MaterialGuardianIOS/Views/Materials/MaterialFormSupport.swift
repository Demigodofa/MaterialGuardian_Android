import SwiftUI

struct PendingReview {
    var attachment: MediaAttachment
    var kind: MediaKind
    var replaceIndex: Int?
    var storedMedia: MaterialMediaStore.StoredMedia
    var retakeAction: PendingRetakeAction

    var message: String {
        switch kind {
        case .photo:
            "Photo ready to keep."
        case .scan:
            attachment.source == .cameraFallback ? "Camera fallback scan ready to keep." : "PDF scan ready to keep."
        }
    }
}

struct ActiveMediaMenu {
    var kind: MediaKind
    var index: Int
}

struct PhotoCaptureRequest: Identifiable {
    enum Kind {
        case photo
        case scanFallback
    }

    let kind: Kind
    let source: ImagePickerSheet.Source?
    let replacingIndex: Int?

    var id: String {
        "\(kindLabel)-\(source?.rawValue ?? "choose")-\(replacingIndex ?? -1)"
    }

    private var kindLabel: String {
        switch kind {
        case .photo: "photo"
        case .scanFallback: "scanFallback"
        }
    }
}

struct ScanCaptureRequest: Identifiable {
    let replacingIndex: Int?

    var id: String {
        "scan-\(replacingIndex ?? -1)"
    }
}

enum PendingRetakeAction {
    case photo(source: ImagePickerSheet.Source, replacingIndex: Int?)
    case scanFallback(source: ImagePickerSheet.Source, replacingIndex: Int?)
    case documentScan(replacingIndex: Int?)
}

enum MediaKind {
    case photo
    case scan
}

enum SignatureTarget: Identifiable {
    case qcInspector
    case qcManager

    var id: String {
        switch self {
        case .qcInspector: "qcInspector"
        case .qcManager: "qcManager"
        }
    }

    var title: String {
        switch self {
        case .qcInspector: "QC Inspector Signature"
        case .qcManager: "QC Manager Signature"
        }
    }
}

struct LabeledField<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if !title.isEmpty {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(Brand.textSecondary)
            } else {
                Spacer()
                    .frame(height: 20)
            }
            content
        }
    }
}

struct MediaThumbnailRow: View {
    let attachments: [MediaAttachment]
    let maxCount: Int
    let symbolName: String
    let onTap: (Int) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(0..<maxCount, id: \.self) { index in
                    if attachments.indices.contains(index) {
                        let attachment = attachments[index]
                        Button {
                            onTap(index)
                        } label: {
                            VStack(spacing: 6) {
                                ZStack {
                                    if let image = MaterialMediaStore().image(for: attachment) {
                                        Image(uiImage: image)
                                            .resizable()
                                            .scaledToFill()
                                            .frame(width: 64, height: 64)
                                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                    } else {
                                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                                            .fill(Color.white)
                                            .frame(width: 64, height: 64)
                                        Image(systemName: iconName(for: attachment))
                                            .font(.system(size: 24, weight: .semibold))
                                            .foregroundStyle(Brand.link)
                                    }
                                }
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .stroke(Brand.border, lineWidth: 1)
                                )
                                Text(shortLabel(for: attachment))
                                    .font(.caption2)
                                    .foregroundStyle(Brand.textSecondary)
                                    .lineLimit(2)
                                    .multilineTextAlignment(.center)
                                    .frame(width: 64)
                            }
                        }
                        .buttonStyle(.plain)
                    } else {
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(Brand.headerButton)
                            .frame(width: 64, height: 64)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .stroke(Brand.border, lineWidth: 1)
                            )
                    }
                }
            }
        }
    }

    private func iconName(for attachment: MediaAttachment) -> String {
        switch attachment.source {
        case .photo:
            symbolName
        case .scan:
            "doc.richtext"
        case .cameraFallback:
            "camera.viewfinder"
        }
    }

    private func shortLabel(for attachment: MediaAttachment) -> String {
        attachment.label.replacingOccurrences(of: "Camera fallback ", with: "Fallback ")
    }
}

struct SignaturePreview: View {
    let strokes: [[SignaturePoint]]

    var body: some View {
        Canvas { context, size in
            for stroke in strokes where stroke.count > 1 {
                var path = Path()
                let first = stroke[0]
                path.move(to: CGPoint(x: first.x * size.width, y: first.y * size.height))
                for point in stroke.dropFirst() {
                    path.addLine(to: CGPoint(x: point.x * size.width, y: point.y * size.height))
                }
                context.stroke(path, with: .color(.black), style: StrokeStyle(lineWidth: 3.5, lineCap: .round, lineJoin: .round))
            }
        }
        .frame(height: 120)
        .frame(maxWidth: .infinity)
        .background(Color.white)
        .overlay(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .stroke(Brand.border, lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

struct SignatureCaptureSheet: View {
    let title: String
    let existing: SignatureCapture?
    let onSave: (SignatureCapture) -> Void
    let onCancel: () -> Void

    @State private var strokes: [[CGPoint]] = []
    @State private var currentStroke: [CGPoint] = []
    @State private var canvasSize: CGSize = .zero

    init(title: String, existing: SignatureCapture?, onSave: @escaping (SignatureCapture) -> Void, onCancel: @escaping () -> Void) {
        self.title = title
        self.existing = existing
        self.onSave = onSave
        self.onCancel = onCancel
        _strokes = State(initialValue: existing?.strokes.map { stroke in
            stroke.map { CGPoint(x: $0.x, y: $0.y) }
        } ?? [])
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text("Draw the signature below, then save or clear it.")
                    .font(.footnote)
                    .foregroundStyle(Brand.textSecondary)

                GeometryReader { proxy in
                    let size = proxy.size
                    Canvas { context, canvasSize in
                        for stroke in renderedStrokes where stroke.count > 1 {
                            var path = Path()
                            path.move(to: stroke[0])
                            for point in stroke.dropFirst() {
                                path.addLine(to: point)
                            }
                            context.stroke(path, with: .color(.black), style: StrokeStyle(lineWidth: 3.5, lineCap: .round, lineJoin: .round))
                        }
                    }
                    .background(Color.white)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .stroke(Brand.border, lineWidth: 1)
                    )
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .gesture(
                        DragGesture(minimumDistance: 0)
                            .onChanged { value in
                                canvasSize = size
                                if currentStroke.isEmpty {
                                    currentStroke = [value.location]
                                } else {
                                    currentStroke.append(value.location)
                                }
                            }
                            .onEnded { _ in
                                if !currentStroke.isEmpty {
                                    strokes.append(currentStroke)
                                    currentStroke.removeAll()
                                }
                            }
                    )
                }
                .frame(height: 240)

                HStack {
                    Button("Clear") {
                        strokes.removeAll()
                        currentStroke.removeAll()
                    }
                    .foregroundStyle(Brand.destructive)

                    Spacer()

                    Button("Save") {
                        let normalized = normalize(strokes: renderedStrokes, in: canvasSize)
                        guard !normalized.isEmpty else { return }
                        onSave(SignatureCapture(strokes: normalized, capturedAt: .now))
                    }
                    .buttonStyle(FilledFormButtonStyle(color: Brand.primaryButton, textColor: Brand.primaryButtonText))
                }
            }
            .padding(20)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onCancel)
                }
            }
        }
    }

    private var renderedStrokes: [[CGPoint]] {
        currentStroke.isEmpty ? strokes : strokes + [currentStroke]
    }

    private func normalize(strokes: [[CGPoint]], in size: CGSize) -> [[SignaturePoint]] {
        guard size.width > 0, size.height > 0 else { return [] }
        return strokes
            .filter { !$0.isEmpty }
            .map { stroke in
                stroke.map { point in
                    SignaturePoint(
                        x: max(0, min(1, point.x / size.width)),
                        y: max(0, min(1, point.y / size.height))
                    )
                }
            }
    }
}

struct FilledFormButtonStyle: ButtonStyle {
    let color: Color
    let textColor: Color

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(textColor)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(color.opacity(configuration.isPressed ? 0.82 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

struct OutlinedMediaButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(Brand.textPrimary)
            .padding(.vertical, 13)
            .padding(.horizontal, 12)
            .background(Color.white)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(Brand.border, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .opacity(configuration.isPressed ? 0.82 : 1)
    }
}
