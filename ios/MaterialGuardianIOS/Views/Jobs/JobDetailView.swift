import SwiftUI
import UIKit

struct JobDetailView: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss
    let jobID: UUID

    @State private var draftMaterial = MaterialRecord.mock(description: "", vendor: "", quantity: "")
    @State private var showingMaterialSheet = false
    @State private var showingEditDescription = false
    @State private var showingEditJobNumber = false
    @State private var showingExportConfirm = false
    @State private var exportError: String?
    @State private var exportSuccess: ExportSummary?
    @State private var shareItems: [Any] = []
    @State private var descriptionDraft = ""
    @State private var jobNumberDraft = ""

    private let exporter = JobExporter()

    private var job: Job? {
        store.jobs.first(where: { $0.id == jobID })
    }

    var body: some View {
        Group {
            if let job {
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        MGHeaderBar(onBack: { dismiss() })
                        summaryCard(job: job)
                        primaryActions(job: job)
                        materialsSection(job: job)
                    }
                    .padding(20)
                }
                .background(Brand.formBackground.ignoresSafeArea())
                .navigationBarBackButtonHidden(true)
                .toolbar(.hidden, for: .navigationBar)
                .sheet(isPresented: $showingMaterialSheet) {
                    NavigationStack {
                        MaterialFormView(material: draftMaterial, draftKey: materialDraftKey(for: draftMaterial)) { material in
                            store.addMaterial(material, to: jobID)
                        }
                    }
                }
                .sheet(isPresented: $showingEditDescription) {
                    textEditSheet(
                        title: "Edit job description",
                        text: $descriptionDraft,
                        limit: 120
                    ) {
                        store.updateJobDescription(jobID: job.id, description: descriptionDraft)
                        showingEditDescription = false
                    }
                }
                .sheet(isPresented: $showingEditJobNumber) {
                    textEditSheet(
                        title: "Edit job number",
                        text: $jobNumberDraft,
                        limit: 30
                    ) {
                        let success = store.renameJob(jobID: job.id, to: jobNumberDraft)
                        if !success {
                            exportError = "Could not rename the job. That number may already exist."
                        }
                        showingEditJobNumber = false
                    }
                }
                .alert("Export job", isPresented: $showingExportConfirm) {
                    Button("Cancel", role: .cancel) {}
                    Button("Export") {
                        runMockExport()
                    }
                } message: {
                    Text(exportPrompt)
                }
                .alert("Export failed", isPresented: exportErrorAlertBinding) {
                    Button("OK", role: .cancel) {
                        exportError = nil
                    }
                } message: {
                    Text(exportError ?? "Export failed.")
                }
                .alert("Export complete", isPresented: exportSuccessAlertBinding) {
                    Button("Open Folder") {
                        openExportFolder()
                    }
                    Button("Done", role: .cancel) {
                        exportSuccess = nil
                    }
                } message: {
                    Text(exportSuccess?.message ?? "")
                }
                .sheet(isPresented: shareSheetBinding) {
                    ShareSheet(items: shareItems)
                }
            } else {
                ContentUnavailableView("Job Not Found", systemImage: "tray")
            }
        }
    }

    private func summaryCard(job: Job) -> some View {
        MGCard {
            VStack(alignment: .leading, spacing: 8) {
                Text("JOB DETAILS")
                    .font(.title2.weight(.bold))
                    .foregroundStyle(Brand.sectionTitle)
                    .frame(maxWidth: .infinity, alignment: .center)

                Text("Job# \(job.jobNumber)")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Brand.link)
                    .underline()
                    .onTapGesture {
                        jobNumberDraft = job.jobNumber
                        showingEditJobNumber = true
                    }

                if !job.description.isEmpty {
                    Text(job.description)
                        .foregroundStyle(Brand.textPrimary)
                        .underline()
                        .onTapGesture {
                            descriptionDraft = job.description
                            showingEditDescription = true
                        }
                } else {
                    Text("Add job description")
                        .foregroundStyle(Brand.link)
                        .underline()
                        .onTapGesture {
                            descriptionDraft = job.description
                            showingEditDescription = true
                        }
                }

                Text(job.statusText)
                    .font(.caption.weight(.bold))
                    .foregroundStyle(job.exportedAt == nil ? Brand.warning : Brand.success)

                if !job.exportPath.isEmpty {
                    Divider()
                    Text("Latest export folder")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Brand.textSecondary)
                    Text(job.exportPath)
                        .font(.footnote)
                        .foregroundStyle(Brand.textSecondary)
                }
            }
        }
    }

    private func primaryActions(job: Job) -> some View {
        HStack(spacing: 12) {
            Button {
                draftMaterial = MaterialRecord.mock(description: "", vendor: "", quantity: "")
                showingMaterialSheet = true
            } label: {
                Text("Add Receiving Report")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(FilledCapsuleButtonStyle(color: Brand.primaryButton))

            Button {
                showingExportConfirm = true
            } label: {
                Text("Export Job")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(FilledCapsuleButtonStyle(color: Brand.exportButton))
        }
    }

    private func materialsSection(job: Job) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Materials Received")
                .font(.title3.weight(.semibold))
                .foregroundStyle(Brand.title)

            if job.materials.isEmpty {
                MGCard {
                    Text("No receiving reports yet.")
                        .foregroundStyle(Brand.textMuted)
                }
            } else {
                ForEach(job.materials) { material in
                    NavigationLink {
                        MaterialFormView(material: material, draftKey: materialDraftKey(for: material)) { updated in
                            store.updateMaterial(updated, in: jobID)
                        }
                    } label: {
                        MaterialSummaryRow(material: material)
                    }
                    .buttonStyle(.plain)
                }
            }

            if !job.exportPath.isEmpty {
                MGCard {
                    VStack(alignment: .center, spacing: 10) {
                        Text("Latest export folder")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(Brand.textSecondary)
                        Text(job.exportPath)
                            .font(.footnote)
                            .foregroundStyle(Brand.textSecondary)
                            .multilineTextAlignment(.center)

                        Button("Open Export Folder") {
                            openExportFolder()
                        }
                        .buttonStyle(FilledCapsuleButtonStyle(color: Brand.editButton, textColor: Brand.editButtonText))

                        Button("Share Latest Export") {
                            shareLatestExport()
                        }
                        .buttonStyle(FilledCapsuleButtonStyle(color: Brand.primaryButton))
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private var exportPrompt: String {
        guard let job else { return "Export job files?" }
        return job.exportedAt == nil
            ? "Export job files to local storage?"
            : "This job was already exported. Export again?"
    }

    private var exportErrorAlertBinding: Binding<Bool> {
        Binding(
            get: { exportError != nil },
            set: { if !$0 { exportError = nil } }
        )
    }

    private var exportSuccessAlertBinding: Binding<Bool> {
        Binding(
            get: { exportSuccess != nil },
            set: { if !$0 { exportSuccess = nil } }
        )
    }

    private var shareSheetBinding: Binding<Bool> {
        Binding(
            get: { !shareItems.isEmpty },
            set: { if !$0 { shareItems = [] } }
        )
    }

    private func runMockExport() {
        guard let job else { return }

        do {
            let bundle = try exporter.export(job: job)
            store.markExported(jobID: job.id, exportPath: bundle.relativeFolderPath)
            exportSuccess = ExportSummary(
                downloadsFolder: bundle.relativeFolderPath,
                materialPacketCount: bundle.packetURLs.count,
                scanSourceCount: bundle.scanSourceCount,
                photoCount: bundle.photoCount
            )
        } catch {
            exportError = error.localizedDescription
        }
    }

    private func openExportFolder() {
        guard let job, !job.exportPath.isEmpty else {
            exportError = "Export this job first."
            return
        }

        do {
            let folderURL = try exporter.folderURL(forRelativePath: job.exportPath)
            guard FileManager.default.fileExists(atPath: folderURL.path()) else {
                exportError = "The latest export folder could not be found on this device."
                return
            }

            if UIApplication.shared.canOpenURL(folderURL) {
                UIApplication.shared.open(folderURL)
            } else {
                shareItems = [folderURL]
            }
            exportSuccess = nil
        } catch {
            exportError = "Unable to open the latest export folder."
        }
    }

    private func shareLatestExport() {
        guard let job, !job.exportPath.isEmpty else {
            exportError = "Export this job first."
            return
        }

        do {
            let folderURL = try exporter.folderURL(forRelativePath: job.exportPath)
            guard FileManager.default.fileExists(atPath: folderURL.path()) else {
                exportError = "The latest export folder could not be found on this device."
                return
            }
            shareItems = [folderURL]
        } catch {
            exportError = "Unable to share the latest export."
        }
    }

    private func materialDraftKey(for material: MaterialRecord) -> String {
        "job-\(jobID.uuidString)-material-\(material.id.uuidString)"
    }
}

@MainActor
private func textEditSheet(
    title: String,
    text: Binding<String>,
    limit: Int,
    onSave: @escaping () -> Void
) -> some View {
    NavigationStack {
        Form {
            Section {
                TextField(title, text: Binding(
                    get: { text.wrappedValue },
                    set: { text.wrappedValue = String($0.prefix(limit)) }
                ))
            }
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Save", action: onSave)
            }
        }
    }
}

private struct ExportSummary {
    let downloadsFolder: String
    let materialPacketCount: Int
    let scanSourceCount: Int
    let photoCount: Int

    var message: String {
        var parts = ["\(materialPacketCount) material packet PDF" + (materialPacketCount == 1 ? "" : "s")]
        if scanSourceCount > 0 {
            parts.append("\(scanSourceCount) scan source" + (scanSourceCount == 1 ? "" : "s"))
        }
        if photoCount > 0 {
            parts.append("\(photoCount) photo" + (photoCount == 1 ? "" : "s"))
        }
        return "Exported \(parts.joined(separator: ", ")).\n\nPhone-accessible folder:\n\(downloadsFolder)"
    }
}

private struct MaterialSummaryRow: View {
    let material: MaterialRecord

    var body: some View {
        MGCard {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(material.description.isEmpty ? "Untitled material" : material.description)
                        .font(.headline)
                        .foregroundStyle(Brand.textPrimary)
                    Text("Vendor: \(material.vendor.isEmpty ? "Unassigned" : material.vendor)")
                        .font(.subheadline)
                        .foregroundStyle(Brand.textSecondary)
                    Text("Qty: \(material.quantity.isEmpty ? "0" : material.quantity)")
                        .font(.subheadline)
                        .foregroundStyle(Brand.textSecondary)
                    Text(material.acceptanceStatus.title)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(statusColor)
                }

                Spacer()

                VStack(alignment: .trailing, spacing: 8) {
                    Label("\(material.photoCount)", systemImage: "camera")
                    Label("\(material.scanCount)", systemImage: "doc.viewfinder")
                }
                .font(.caption)
                .foregroundStyle(Brand.textMuted)
            }
        }
    }

    private var statusColor: Color {
        switch material.acceptanceStatus {
        case .accept: Brand.success
        case .hold: Brand.warning
            case .reject: Brand.destructive
        }
    }
}

private struct FilledCapsuleButtonStyle: ButtonStyle {
    let color: Color
    var textColor: Color? = nil

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(textColor ?? (color == Brand.exportButton ? Brand.exportButtonText : Brand.primaryButtonText))
            .padding(.vertical, 14)
            .padding(.horizontal, 12)
            .background(color.opacity(configuration.isPressed ? 0.82 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

#Preview {
    NavigationStack {
        JobDetailView(jobID: AppStore().jobs[0].id)
            .environmentObject(AppStore())
    }
}
