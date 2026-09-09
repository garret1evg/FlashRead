import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @AppStorage("flashread_app_theme") private var appTheme = "system"

    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .preferredColorScheme(preferredScheme)
            .background(canvasColor)
    }

    private var preferredScheme: ColorScheme? {
        switch appTheme.lowercased() {
        case "dark": return .dark
        case "light", "sepia": return .light
        default: return nil
        }
    }

    private var canvasColor: Color {
        switch appTheme.lowercased() {
        case "dark":
            return Color(red: 18 / 255, green: 16 / 255, blue: 22 / 255)
        case "sepia":
            return Color(red: 244 / 255, green: 236 / 255, blue: 216 / 255)
        case "light":
            return Color(red: 248 / 255, green: 247 / 255, blue: 252 / 255)
        default:
            return Color("LaunchBackground")
        }
    }
}



