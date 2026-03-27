import SwiftUI

struct JobsListView: View {
    @EnvironmentObject private var store: AppStore
    @State private var showingNewJobSheet = false
    @State private var jobToDelete: Job?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    header
                    createButton
                    policyNote
                    jobsSection
                }
                .padding(20)
            }
            .background(Brand.screenBackground.ignoresSafeArea())
            .navigationTitle("Material Guardian")
            .navigationBarTitleDisplayMode(.inline)
            .sheet(isPresented: $showingNewJobSheet) {
                NewJobSheet()
                    .environmentObject(store)
            }
            .alert("Delete job?", isPresented: deleteAlertBinding) {
                Button("Cancel", role: .cancel) {
                    jobToDelete = nil
                }
                Button("Delete", role: .destructive) {
                    if let jobToDelete {
                        store.deleteJob(id: jobToDelete.id)
                    }
                    jobToDelete = nil
                }
            } message: {
                Text(deleteMessage)
            }
        }
    }

    private var header: some View {
        VStack(spacing: 14) {
            HStack(alignment: .center, spacing: 12) {
                Image("MaterialGuardianLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 96, height: 96)
                    .background(
                        RoundedRectangle(cornerRadius: 22, style: .continuous)
                            .fill(Brand.cardBackground)
                    )

                VStack(alignment: .leading, spacing: 6) {
                    Text("Material Guardian")
                        .font(.title2.weight(.bold))
                        .foregroundStyle(Brand.title)
                    Text("Receiving inspection workflow")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Brand.sectionTitle)
                }

                Spacer(minLength: 0)
            }

            MGCard {
                HStack(spacing: 12) {
                    Image("WeldersHelperLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 42, height: 42)

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Brought to you by Welders Helper")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(Brand.title)
                        Text("Material Guardian is part of the Welders Helper suite of field-use apps.")
                            .font(.caption)
                            .foregroundStyle(Brand.textSecondary)
                    }

                    Spacer(minLength: 0)
                }
            }

            Text("Offline receiving inspection reports with photos, scans, signatures, and export planning.")
                .font(.subheadline)
                .multilineTextAlignment(.center)
                .foregroundStyle(Brand.textSecondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 8)
    }

    private var createButton: some View {
        Button {
            showingNewJobSheet = true
        } label: {
            Text("Create Job")
                .font(.headline)
                .foregroundStyle(Brand.primaryButtonText)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(Brand.primaryButton)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
    }

    private var policyNote: some View {
        NavigationLink {
            PrivacyPolicyView()
        } label: {
            MGCard {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Privacy Policy")
                            .font(.headline)
                            .foregroundStyle(Brand.sectionTitle)
                        Text("The iOS app should inherit the same public privacy policy and Welders Helper suite standards that already exist in the Android repo.")
                            .font(.subheadline)
                            .foregroundStyle(Brand.textSecondary)
                    }

                    Spacer(minLength: 12)

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Brand.textMuted)
                        .padding(.top, 4)
                }
            }
        }
        .buttonStyle(.plain)
    }

    private var jobsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Current Jobs")
                .font(.title3.weight(.semibold))
                .foregroundStyle(Brand.title)

            if store.jobs.isEmpty {
                MGCard {
                    Text("No jobs yet. Create your first job above.")
                        .foregroundStyle(Brand.textMuted)
                }
            } else {
                ForEach(store.jobs) { job in
                    NavigationLink(value: job) {
                        JobRow(job: job) {
                            jobToDelete = job
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .navigationDestination(for: Job.self) { job in
            JobDetailView(jobID: job.id)
                .environmentObject(store)
        }
    }

    private var deleteAlertBinding: Binding<Bool> {
        Binding(
            get: { jobToDelete != nil },
            set: { if !$0 { jobToDelete = nil } }
        )
    }

    private var deleteMessage: String {
        guard let jobToDelete else { return "" }
        return jobToDelete.exportedAt == nil
            ? "This job has not been exported yet. Deleting will remove it and its materials from this device."
            : "This job was already exported. Delete the local copy?"
    }
}

private struct JobRow: View {
    let job: Job
    let onDelete: () -> Void

    var body: some View {
        MGCard {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Job# \(job.jobNumber)")
                        .font(.headline)
                        .foregroundStyle(Brand.link)
                        .underline()
                    if !job.description.isEmpty {
                        Text(job.description)
                            .foregroundStyle(Brand.textPrimary)
                            .underline()
                    }
                    Text(job.statusText)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(job.exportedAt == nil ? Brand.warning : Brand.success)
                }

                Spacer(minLength: 12)

                VStack(alignment: .trailing, spacing: 10) {
                    Button("Delete", action: onDelete)
                        .buttonStyle(RowDeleteButtonStyle())

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Brand.textMuted)
                        .padding(.top, 4)
                }
            }
        }
    }
}

private struct RowDeleteButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.caption.weight(.semibold))
            .foregroundStyle(Color.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .background(Brand.destructive.opacity(configuration.isPressed ? 0.82 : 1))
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}

#Preview {
    JobsListView()
        .environmentObject(AppStore())
}
