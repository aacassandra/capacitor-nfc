// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorNfc",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "CapacitorNfc",
            targets: ["NFCPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "NFCPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/NFCPlugin"),
        .testTarget(
            name: "NFCPluginTests",
            dependencies: ["NFCPlugin"],
            path: "ios/Tests/NFCPluginTests")
    ]
)
