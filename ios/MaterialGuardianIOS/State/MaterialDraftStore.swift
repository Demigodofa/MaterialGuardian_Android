import Foundation

@MainActor
final class MaterialDraftStore {
    static let shared = MaterialDraftStore()

    private let baseURL: URL

    private init() {
        let supportFolder = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSTemporaryDirectory())
        self.baseURL = supportFolder
            .appendingPathComponent("MaterialGuardianIOS", isDirectory: true)
            .appendingPathComponent("drafts", isDirectory: true)
    }

    func loadDraft(for key: String) -> MaterialRecord? {
        let url = draftURL(for: key)
        guard let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder.materialGuardian.decode(MaterialRecord.self, from: data)
    }

    func saveDraft(_ material: MaterialRecord, for key: String) {
        do {
            try FileManager.default.createDirectory(at: baseURL, withIntermediateDirectories: true)
            let data = try JSONEncoder.materialGuardian.encode(material)
            try data.write(to: draftURL(for: key), options: .atomic)
        } catch {
            assertionFailure("Failed to save draft: \(error)")
        }
    }

    func clearDraft(for key: String) {
        try? FileManager.default.removeItem(at: draftURL(for: key))
    }

    private func draftURL(for key: String) -> URL {
        baseURL.appendingPathComponent("\(sanitize(key)).json")
    }

    private func sanitize(_ key: String) -> String {
        let invalid = CharacterSet.alphanumerics.union(.init(charactersIn: "-_")).inverted
        return key.components(separatedBy: invalid).joined(separator: "_")
    }
}
