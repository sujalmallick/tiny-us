import SwiftUI
import WidgetKit

private struct TinyUsEntry: TimelineEntry {
    let date: Date
    let payload: TinyWidgetPayload
}

private struct TinyUsProvider: TimelineProvider {
    private let suiteName = "group.com.example.tinyus.shared"
    func placeholder(in context: Context) -> TinyUsEntry { TinyUsEntry(date: .now, payload: .empty) }
    func getSnapshot(in context: Context, completion: @escaping (TinyUsEntry) -> Void) {
        completion(TinyUsEntry(date: .now, payload: load()))
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<TinyUsEntry>) -> Void) {
        let now = Date()
        let entry = TinyUsEntry(date: now, payload: load())
        completion(Timeline(entries: [entry], policy: .after(Calendar.current.date(byAdding: .minute, value: 30, to: now) ?? now.addingTimeInterval(1800))))
    }
    private func load() -> TinyWidgetPayload {
        guard let data = UserDefaults(suiteName: suiteName)?.data(forKey: "tiny-us.widget.payload"),
              let payload = try? JSONDecoder().decode(TinyWidgetPayload.self, from: data) else { return .empty }
        return payload
    }
}

private struct TinyUsWidgetView: View {
    let entry: TinyUsEntry
    @Environment(\.widgetFamily) private var family
    private let cream = Color(red: 1, green: 0.86, blue: 0.67)

    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(red: 0.18, green: 0.2, blue: 0.31), Color(red: 0.35, green: 0.26, blue: 0.34)], startPoint: .topLeading, endPoint: .bottomTrailing)
            VStack(alignment: .leading, spacing: 7) {
                HStack {
                    Text("TINY US").font(.system(size: 10, weight: .black, design: .monospaced)).tracking(1.3).foregroundStyle(cream)
                    Spacer()
                    Text(entry.payload.sharedMoodEmoji).font(.system(size: 16))
                }
                HStack(alignment: .bottom, spacing: 8) {
                    PixelCouple().frame(width: family == .systemSmall ? 57 : 70, height: family == .systemSmall ? 50 : 58)
                    VStack(alignment: .leading, spacing: 3) {
                        Text("\(entry.payload.daysTogether)").font(.system(size: family == .systemSmall ? 23 : 28, weight: .heavy, design: .rounded)).foregroundStyle(cream)
                        Text("days together").font(.system(size: 9, weight: .semibold, design: .rounded)).foregroundStyle(.white.opacity(0.72))
                    }
                    Spacer(minLength: 0)
                }
                Text(entry.payload.coupleNames).font(.system(size: 11, weight: .bold, design: .rounded)).lineLimit(1).foregroundStyle(.white)
                if family == .systemMedium {
                    Text(entry.payload.dailyMomentPrompt).font(.system(size: 10, weight: .medium, design: .rounded)).lineLimit(2).foregroundStyle(.white.opacity(0.8))
                    HStack { Text(entry.payload.sceneName); Spacer(); Text(entry.payload.weatherName) }
                        .font(.system(size: 8, weight: .medium, design: .rounded)).foregroundStyle(cream.opacity(0.9)).lineLimit(1)
                } else {
                    Text("\(entry.payload.sceneName) · \(entry.payload.weatherName)").font(.system(size: 8, weight: .medium, design: .rounded)).foregroundStyle(cream.opacity(0.9)).lineLimit(1)
                }
            }.padding(12)
        }
        .widgetBackground()
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Tiny Us. \(entry.payload.coupleNames), \(entry.payload.daysTogether) days together. \(entry.payload.dailyMomentPrompt)")
    }
}

private extension View {
    @ViewBuilder func widgetBackground() -> some View {
        if #available(iOS 17.0, *) { containerBackground(.clear, for: .widget) }
        else { background(Color.clear) }
    }
}

private struct PixelCouple: View {
    var body: some View {
        Canvas(opaque: false, colorMode: .linear) { context, size in
            let unit = min(size.width / 22, size.height / 18)
            func pixel(_ x: Int, _ y: Int, _ width: Int, _ height: Int, _ color: Color) {
                context.fill(Path(CGRect(x: CGFloat(x) * unit, y: CGFloat(y) * unit, width: CGFloat(width) * unit, height: CGFloat(height) * unit)), with: .color(color))
            }
            for index in 0..<2 {
                let x = index == 0 ? 3 : 12
                pixel(x, 1, 7, 1, Color(red: 0.34, green: 0.25, blue: 0.29))
                pixel(x - 1, 2, 9, 5, Color(red: 0.96, green: 0.81, blue: 0.7))
                pixel(x + 1, 4, 1, 1, Color(red: 0.22, green: 0.2, blue: 0.25))
                pixel(x + 6, 4, 1, 1, Color(red: 0.22, green: 0.2, blue: 0.25))
                pixel(x, 7, 7, 6, index == 0 ? Color(red: 0.48, green: 0.67, blue: 0.82) : Color(red: 0.88, green: 0.53, blue: 0.63))
                pixel(x + 1, 13, 2, 3, Color(red: 0.9, green: 0.76, blue: 0.64))
                pixel(x + 4, 13, 2, 3, Color(red: 0.9, green: 0.76, blue: 0.64))
            }
            pixel(9, 8, 4, 2, Color(red: 1, green: 0.77, blue: 0.72))
        }
        .accessibilityHidden(true)
    }
}

struct TinyUsWidget: Widget {
    let kind = "TinyUsWidget"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: TinyUsProvider()) { entry in TinyUsWidgetView(entry: entry) }
            .configurationDisplayName("Tiny Us")
            .description("A small glimpse of your shared world and the days you’ve spent together.")
            .supportedFamilies([.systemSmall, .systemMedium])
    }
}

@main struct TinyUsWidgetBundle: WidgetBundle {
    var body: some Widget { TinyUsWidget() }
}
