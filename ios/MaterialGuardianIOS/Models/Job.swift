import Foundation

struct Job: Identifiable, Hashable, Codable {
    let id: UUID
    var jobNumber: String
    var description: String
    var notes: String
    var createdAt: Date
    var exportedAt: Date?
    var exportPath: String
    var materials: [MaterialRecord]

    var statusText: String {
        exportedAt == nil ? "Not exported" : "Exported"
    }
}

extension Job {
    static func mock(
        jobNumber: String,
        description: String,
        exportedAt: Date? = nil,
        materials: [MaterialRecord] = []
    ) -> Job {
        Job(
            id: UUID(),
            jobNumber: jobNumber,
            description: description,
            notes: "",
            createdAt: .now,
            exportedAt: exportedAt,
            exportPath: exportedAt == nil ? "" : "Files/MaterialGuardian/\(jobNumber)",
            materials: materials
        )
    }
}
