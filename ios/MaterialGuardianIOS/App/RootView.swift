import SwiftUI

struct RootView: View {
    @State private var showingSplash = true

    var body: some View {
        ZStack {
            JobsListView()
                .opacity(showingSplash ? 0 : 1)

            if showingSplash {
                SuiteSplashView {
                    withAnimation(.easeInOut(duration: 0.35)) {
                        showingSplash = false
                    }
                }
                .transition(.opacity)
            }
        }
    }
}

#Preview {
    RootView()
        .environmentObject(AppStore())
}

private struct SuiteSplashView: View {
    let onFinish: () -> Void

    @State private var phase = 0
    @State private var visible = false

    var body: some View {
        ZStack {
            Color.white.ignoresSafeArea()

            if phase == 0 {
                Text("Brought to you by:")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(Brand.title)
                    .opacity(visible ? 1 : 0)
            } else {
                VStack(spacing: 12) {
                    Image("WeldersHelperLogo")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 180, height: 180)

                    Text("Welders Helper")
                        .font(.title3.weight(.bold))
                        .foregroundStyle(Brand.title)
                }
                .opacity(visible ? 1 : 0)
            }
        }
        .task {
            visible = true
            try? await Task.sleep(for: .milliseconds(900))
            withAnimation(.easeInOut(duration: 0.35)) {
                visible = false
            }
            try? await Task.sleep(for: .milliseconds(400))
            phase = 1
            withAnimation(.easeInOut(duration: 0.45)) {
                visible = true
            }
            try? await Task.sleep(for: .milliseconds(1300))
            withAnimation(.easeInOut(duration: 0.35)) {
                visible = false
            }
            try? await Task.sleep(for: .milliseconds(250))
            onFinish()
        }
    }
}
