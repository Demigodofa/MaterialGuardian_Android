import SwiftUI

struct NewJobSheet: View {
    @EnvironmentObject private var store: AppStore
    @Environment(\.dismiss) private var dismiss

    @State private var jobNumber = ""
    @State private var description = ""
    @State private var notes = ""
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Form {
                    Section {
                        TextField("Job Number", text: $jobNumber)
                            .textInputAutocapitalization(.characters)
                        TextField("Description", text: $description)
                        TextField("Notes", text: $notes, axis: .vertical)
                            .lineLimit(3...5)
                        if let errorMessage {
                            Text(errorMessage)
                                .font(.footnote)
                                .foregroundStyle(Brand.destructive)
                        }
                    }
                }
            }
            .navigationTitle("Create New Job")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create") {
                        let trimmed = jobNumber.trimmingCharacters(in: .whitespacesAndNewlines)
                        if trimmed.isEmpty {
                            errorMessage = "Job number is required."
                            return
                        }
                        if store.jobs.contains(where: { $0.jobNumber.caseInsensitiveCompare(trimmed) == .orderedSame }) {
                            errorMessage = "A job with that number already exists."
                            return
                        }
                        store.addJob(jobNumber: jobNumber, description: description, notes: notes)
                        dismiss()
                    }
                    .disabled(jobNumber.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}

#Preview {
    NewJobSheet()
        .environmentObject(AppStore())
}
