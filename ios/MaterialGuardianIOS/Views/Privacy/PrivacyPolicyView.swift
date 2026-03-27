import SwiftUI

struct PrivacyPolicyView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                MGHeaderBar(onBack: { dismiss() })

                Text("PRIVACY POLICY")
                    .font(.title2.weight(.bold))
                    .foregroundStyle(Brand.sectionTitle)
                    .frame(maxWidth: .infinity, alignment: .center)

                policySection(
                    title: "Overview",
                    body: "Material Guardian is an offline-first receiving inspection app. The app stores jobs, receiving reports, material photos, scanned PDFs, signatures, and exported packet files on your device."
                )

                policySection(
                    title: "Camera and document scanning",
                    body: "If you choose to capture material photos or scan MTR/CoC documents, the app uses the device camera and document scanner only for that workflow."
                )

                policySection(
                    title: "Data handling",
                    body: "Material Guardian does not require an account. The current release does not upload your jobs, reports, photos, scans, or signatures to a cloud service operated by the developer."
                )

                policySection(
                    title: "Sharing and exports",
                    body: "When you export a job, the app writes packet files to app storage and a copy under a predictable device-accessible location. If you use iOS share actions, the selected files are shared only to the destination you choose."
                )

                policySection(
                    title: "Retention and control",
                    body: "You control local data on the device. You can keep drafts, delete drafts, delete jobs, or remove exported files from device storage."
                )

                policySection(
                    title: "Contact",
                    body: "For support or privacy questions, use the contact details published with the store listing or your company deployment channel."
                )
            }
            .padding(20)
        }
        .background(Brand.formBackground.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }

    private func policySection(title: String, body: String) -> some View {
        MGCard {
            Text(title)
                .font(.headline)
                .foregroundStyle(Brand.textPrimary)
            Text(body)
                .font(.body)
                .foregroundStyle(Brand.textSecondary)
        }
    }
}

#Preview {
    NavigationStack {
        PrivacyPolicyView()
    }
}
