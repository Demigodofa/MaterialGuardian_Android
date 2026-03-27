import Foundation

struct MaterialRecord: Identifiable, Hashable, Codable {
    let id: UUID
    var description: String
    var vendor: String
    var quantity: String
    var poNumber: String
    var productType: String
    var specificationPrefix: String
    var gradeType: String
    var fittingStandard: String
    var fittingSuffix: String
    var dimensionUnit: DimensionUnit
    var thickness1: String
    var thickness2: String
    var thickness3: String
    var thickness4: String
    var width: String
    var length: String
    var diameter: String
    var diameterType: String
    var b16DimensionsAcceptable: String
    var specificationNumbers: String
    var markings: String
    var comments: String
    var qcInitials: String
    var qcDate: Date?
    var qcInspectorSignature: SignatureCapture?
    var visualInspectionAcceptable: Bool
    var markingAcceptable: Bool
    var markingAcceptableNa: Bool
    var mtrAcceptable: Bool
    var mtrAcceptableNa: Bool
    var acceptanceStatus: AcceptanceStatus
    var materialApproval: MaterialApproval
    var qcManager: String
    var qcManagerInitials: String
    var qcManagerDate: Date?
    var qcManagerSignature: SignatureCapture?
    var offloadStatus: String
    var pdfStatus: String
    var pdfStoragePath: String
    var photoAttachments: [MediaAttachment]
    var scanAttachments: [MediaAttachment]
    var receivedAt: Date

    var photoCount: Int {
        photoAttachments.count
    }

    var scanCount: Int {
        scanAttachments.count
    }
}

struct MediaAttachment: Identifiable, Hashable, Codable {
    let id: UUID
    var label: String
    var createdAt: Date
    var source: Source
    var relativeFilePath: String?

    enum Source: String, Hashable, Codable, CaseIterable {
        case photo
        case scan
        case cameraFallback
    }
}

struct SignatureCapture: Hashable, Codable {
    var strokes: [[SignaturePoint]]
    var capturedAt: Date
    var relativeFilePath: String?

    var isEmpty: Bool {
        strokes.allSatisfy(\.isEmpty)
    }
}

struct SignaturePoint: Hashable, Codable {
    var x: Double
    var y: Double
}

extension MaterialRecord {
    enum DimensionUnit: String, CaseIterable, Identifiable, Codable {
        case imperial
        case metric

        var id: String { rawValue }

        var title: String {
            switch self {
            case .imperial: "Imperial"
            case .metric: "Metric"
            }
        }
    }

    enum AcceptanceStatus: String, CaseIterable, Identifiable, Codable {
        case accept
        case hold
        case reject

        var id: String { rawValue }

        var title: String {
            switch self {
            case .accept: "Accept"
            case .hold: "Hold"
            case .reject: "Reject"
            }
        }
    }

    enum MaterialApproval: String, CaseIterable, Identifiable, Codable {
        case approved
        case reviewRequired
        case rejected

        var id: String { rawValue }

        var title: String {
            switch self {
            case .approved: "Approved"
            case .reviewRequired: "Review Required"
            case .rejected: "Rejected"
            }
        }
    }

    static func mock(
        description: String,
        vendor: String,
        quantity: String,
        acceptanceStatus: AcceptanceStatus = .accept
    ) -> MaterialRecord {
        MaterialRecord(
            id: UUID(),
            description: description,
            vendor: vendor,
            quantity: quantity,
            poNumber: "PO-123",
            productType: "Tube",
            specificationPrefix: "A",
            gradeType: "B",
            fittingStandard: "N/A",
            fittingSuffix: "",
            dimensionUnit: .imperial,
            thickness1: "",
            thickness2: "",
            thickness3: "",
            thickness4: "",
            width: "",
            length: "20",
            diameter: "6",
            diameterType: "O.D.",
            b16DimensionsAcceptable: "Yes",
            specificationNumbers: "A106 / B36.10",
            markings: "Heat # 2478",
            comments: "",
            qcInitials: "KP",
            qcDate: .now,
            qcInspectorSignature: nil,
            visualInspectionAcceptable: true,
            markingAcceptable: true,
            markingAcceptableNa: false,
            mtrAcceptable: true,
            mtrAcceptableNa: false,
            acceptanceStatus: acceptanceStatus,
            materialApproval: .approved,
            qcManager: "QC Manager",
            qcManagerInitials: "",
            qcManagerDate: .now,
            qcManagerSignature: nil,
            offloadStatus: "",
            pdfStatus: "",
            pdfStoragePath: "",
            photoAttachments: [
                MediaAttachment(id: UUID(), label: "Arrival condition", createdAt: .now, source: .photo, relativeFilePath: nil),
                MediaAttachment(id: UUID(), label: "Markings", createdAt: .now, source: .photo, relativeFilePath: nil)
            ],
            scanAttachments: [
                MediaAttachment(id: UUID(), label: "MTR Packet", createdAt: .now, source: .scan, relativeFilePath: nil)
            ],
            receivedAt: .now
        )
    }
}
