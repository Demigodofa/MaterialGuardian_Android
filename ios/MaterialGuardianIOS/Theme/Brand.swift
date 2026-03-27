import SwiftUI

enum Brand {
    static let screenBackground = Color(hex: 0xF1F3F6)
    static let formBackground = Color(hex: 0xF6F7F9)
    static let cardBackground = Color(hex: 0xF8FAFC)
    static let divider = Color(hex: 0xCDD4DE)
    static let primaryButton = Color(hex: 0x22324A)
    static let primaryButtonText = Color(hex: 0xF2F4F7)
    static let headerButton = Color(hex: 0xE5E7EB)
    static let editButton = Color(hex: 0xE5E7EB)
    static let editButtonText = Color(hex: 0x1F2937)
    static let exportButton = Color(hex: 0x1C3F5B)
    static let exportButtonText = Color.white
    static let destructive = Color(hex: 0xB00020)
    static let success = Color(hex: 0x166534)
    static let warning = Color(hex: 0x9A3412)
    static let textPrimary = Color(hex: 0x1F2937)
    static let textSecondary = Color(hex: 0x566173)
    static let textMuted = Color(hex: 0x7B8794)
    static let title = Color(hex: 0x1C2430)
    static let sectionTitle = Color(hex: 0x4B5563)
    static let link = Color(hex: 0x1E3A5F)
    static let border = Color(hex: 0xCBD5E1)
}

private extension Color {
    init(hex: UInt32) {
        let red = Double((hex >> 16) & 0xFF) / 255.0
        let green = Double((hex >> 8) & 0xFF) / 255.0
        let blue = Double(hex & 0xFF) / 255.0
        self.init(red: red, green: green, blue: blue)
    }
}
