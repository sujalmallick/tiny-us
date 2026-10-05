package com.example.shell

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Entry point for SwiftUI: `MainViewControllerKt.SharedComposeViewController()` hosts shared Compose UI. */
fun SharedComposeViewController(): UIViewController = ComposeUIViewController { SharedComposeProbe() }
