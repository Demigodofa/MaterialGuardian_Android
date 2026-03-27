import Foundation

final class AppStore: ObservableObject {
    @Published var jobs: [Job] {
        didSet {
            persistJobs()
        }
    }

    private let jobsURL: URL

    init() {
        self.jobsURL = Self.makeJobsURL()
        self.jobs = Self.loadJobs(from: jobsURL) ?? [
            .mock(
                jobNumber: "JOB-1042",
                description: "Structural tube receiving",
                exportedAt: nil,
                materials: [
                    .mock(description: "6 in Sch 40 pipe", vendor: "SteelCo", quantity: "12"),
                    .mock(description: "2 in socket weld fittings", vendor: "ForgeWorks", quantity: "24", acceptanceStatus: .hold)
                ]
            ),
            .mock(
                jobNumber: "JOB-1043",
                description: "Valve skid package",
                exportedAt: .now,
                materials: [
                    .mock(description: "150# flanges", vendor: "North Foundry", quantity: "8")
                ]
            )
        ]
    }

    func addJob(jobNumber: String, description: String, notes: String) {
        let trimmedNumber = jobNumber.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedNumber.isEmpty else { return }

        jobs.insert(
            Job(
                id: UUID(),
                jobNumber: trimmedNumber,
                description: description.trimmingCharacters(in: .whitespacesAndNewlines),
                notes: notes.trimmingCharacters(in: .whitespacesAndNewlines),
                createdAt: .now,
                exportedAt: nil,
                exportPath: "",
                materials: []
            ),
            at: 0
        )
    }

    func deleteJob(id: UUID) {
        jobs.removeAll { $0.id == id }
    }

    func update(job: Job) {
        guard let index = jobs.firstIndex(where: { $0.id == job.id }) else { return }
        jobs[index] = job
    }

    func updateJobDescription(jobID: UUID, description: String) {
        guard let index = jobs.firstIndex(where: { $0.id == jobID }) else { return }
        jobs[index].description = description.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    func renameJob(jobID: UUID, to newJobNumber: String) -> Bool {
        let trimmed = newJobNumber.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return false }
        guard !jobs.contains(where: { $0.id != jobID && $0.jobNumber.caseInsensitiveCompare(trimmed) == .orderedSame }) else {
            return false
        }
        guard let index = jobs.firstIndex(where: { $0.id == jobID }) else { return false }
        jobs[index].jobNumber = trimmed
        return true
    }

    func markExported(jobID: UUID, exportPath: String) {
        guard let index = jobs.firstIndex(where: { $0.id == jobID }) else { return }
        jobs[index].exportedAt = .now
        jobs[index].exportPath = exportPath
    }

    func addMaterial(_ material: MaterialRecord, to jobID: UUID) {
        guard let index = jobs.firstIndex(where: { $0.id == jobID }) else { return }
        jobs[index].materials.insert(material, at: 0)
    }

    func updateMaterial(_ material: MaterialRecord, in jobID: UUID) {
        guard let jobIndex = jobs.firstIndex(where: { $0.id == jobID }) else { return }
        guard let materialIndex = jobs[jobIndex].materials.firstIndex(where: { $0.id == material.id }) else { return }
        jobs[jobIndex].materials[materialIndex] = material
    }

    private func persistJobs() {
        do {
            let parent = jobsURL.deletingLastPathComponent()
            try FileManager.default.createDirectory(at: parent, withIntermediateDirectories: true)
            let data = try JSONEncoder.materialGuardian.encode(jobs)
            try data.write(to: jobsURL, options: .atomic)
        } catch {
            assertionFailure("Failed to persist jobs: \(error)")
        }
    }

    private static func loadJobs(from url: URL) -> [Job]? {
        guard let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder.materialGuardian.decode([Job].self, from: data)
    }

    private static func makeJobsURL() -> URL {
        let supportFolder = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        return supportFolder
            .appendingPathComponent("MaterialGuardianIOS", isDirectory: true)
            .appendingPathComponent("jobs.json")
    }
}

extension JSONEncoder {
    static let materialGuardian: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }()
}

extension JSONDecoder {
    static let materialGuardian: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }()
}
