import SwiftUI

struct MGHeaderBar: View {
    let onBack: (() -> Void)?

    var body: some View {
        HStack {
            if let onBack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Brand.textPrimary)
                        .frame(width: 60, height: 60)
                        .background(Brand.headerButton)
                        .clipShape(Circle())
                        .shadow(color: Color.black.opacity(0.08), radius: 8, y: 3)
                }
                .buttonStyle(.plain)
            } else {
                Color.clear
                    .frame(width: 60, height: 60)
            }

            Spacer()

            Image("MaterialGuardianLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 72, height: 72)
                .padding(.trailing, 4)
        }
        .frame(maxWidth: .infinity)
    }
}

#Preview {
    MGHeaderBar(onBack: {})
        .padding()
        .background(Brand.formBackground)
}
