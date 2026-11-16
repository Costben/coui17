import UIKit
import SwiftUI
import shared

final class IosSwiftBackend: NSObject, NativeHostBackend {
    private var actionCount: Int = 0

    var hostPlatformName: String {
        let device = UIDevice.current
        return "Swift / UIKit (\(device.systemName) \(device.systemVersion), \(device.model))"
    }

    func fetchHostGreeting(topic: String) -> String {
        let trimmed = topic.trimmingCharacters(in: .whitespacesAndNewlines)
        let resolved = trimmed.isEmpty ? "COUI" : trimmed
        let device = UIDevice.current
        return "Hello from Swift on \(device.name) (\(device.systemName) \(device.systemVersion)) — topic: \(resolved)"
    }

    func dispatchHostAction(action: String) -> String {
        actionCount += 1
        let trimmed = action.trimmingCharacters(in: .whitespacesAndNewlines)
        let resolved = trimmed.isEmpty ? "ping" : trimmed
        return "Swift handled \"\(resolved)\" (#\(actionCount))"
    }
}

private final class NativeInteropCardView: UIView {
    private let titleLabel = UILabel()
    private let actionButton = UIButton(type: .system)
    private var tapCount: Int = 0

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }

    private func setupView() {
        backgroundColor = .secondarySystemBackground
        layer.cornerRadius = 14
        layer.masksToBounds = true

        titleLabel.font = .preferredFont(forTextStyle: .subheadline)
        titleLabel.textColor = .label
        titleLabel.numberOfLines = 2
        titleLabel.text = "UIKit UIView • \(UIDevice.current.systemName) \(UIDevice.current.systemVersion) (taps: 0)"

        actionButton.setTitle("Tap UIButton", for: .normal)
        actionButton.titleLabel?.font = .preferredFont(forTextStyle: .headline)
        actionButton.contentHorizontalAlignment = .leading
        actionButton.addTarget(self, action: #selector(handleTap), for: .touchUpInside)

        let stack = UIStackView(arrangedSubviews: [titleLabel, actionButton])
        stack.axis = .vertical
        stack.spacing = 6
        stack.translatesAutoresizingMaskIntoConstraints = false
        addSubview(stack)

        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -16),
            stack.centerYAnchor.constraint(equalTo: centerYAnchor),
        ])
    }

    @objc private func handleTap() {
        tapCount += 1
        titleLabel.text = "UIKit UIView • \(UIDevice.current.systemName) \(UIDevice.current.systemVersion) (taps: \(tapCount))"
    }
}

private final class NativeInteropViewController: UIViewController {
    private let badgeLabel = UILabel()
    private let detailLabel = UILabel()
    private var counter: Int = 0

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .tertiarySystemBackground
        view.layer.cornerRadius = 14
        view.layer.masksToBounds = true

        badgeLabel.font = .monospacedDigitSystemFont(ofSize: 13, weight: .semibold)
        badgeLabel.textColor = .white
        badgeLabel.backgroundColor = .systemBlue
        badgeLabel.textAlignment = .center
        badgeLabel.layer.cornerRadius = 10
        badgeLabel.layer.masksToBounds = true
        badgeLabel.text = "  VC Counter: 0  "

        detailLabel.font = .preferredFont(forTextStyle: .footnote)
        detailLabel.textColor = .secondaryLabel
        detailLabel.numberOfLines = 2
        detailLabel.text = "Hosted UIKit UIViewController (\(UIDevice.current.model))"

        let incrementButton = UIButton(type: .system)
        incrementButton.setTitle("Increment VC Counter", for: .normal)
        incrementButton.contentHorizontalAlignment = .leading
        incrementButton.addTarget(self, action: #selector(incrementCounter), for: .touchUpInside)

        let topRow = UIStackView(arrangedSubviews: [badgeLabel, UIView()])
        topRow.axis = .horizontal

        let stack = UIStackView(arrangedSubviews: [topRow, detailLabel, incrementButton])
        stack.axis = .vertical
        stack.spacing = 6
        stack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(stack)

        NSLayoutConstraint.activate([
            badgeLabel.heightAnchor.constraint(equalToConstant: 24),
            stack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            stack.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
    }

    @objc private func incrementCounter() {
        counter += 1
        badgeLabel.text = "  VC Counter: \(counter)  "
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.mainWithNativeHost(
            backend: IosSwiftBackend(),
            uiViewFactory: { NativeInteropCardView() },
            uiViewControllerFactory: { NativeInteropViewController() }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
                .edgesIgnoringSafeArea(.all) // edge to edge
    }
}
