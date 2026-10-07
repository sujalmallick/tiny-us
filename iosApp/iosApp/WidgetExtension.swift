import SwiftUI
import WidgetKit

private struct TinyUsEntry: TimelineEntry {
    let date: Date
    let payload: TinyWidgetPayload
    /// The couple's scene for each widget shape, once the app has drawn it.
    var smallScene: UIImage? = nil
    var mediumScene: UIImage? = nil
}

private struct TinyUsProvider: TimelineProvider {
    func placeholder(in context: Context) -> TinyUsEntry { TinyUsEntry(date: .now, payload: .empty) }
    func getSnapshot(in context: Context, completion: @escaping (TinyUsEntry) -> Void) {
        completion(TinyUsEntry(date: .now, payload: load(), smallScene: scene(TinyAppGroup.smallSceneKey), mediumScene: scene(TinyAppGroup.mediumSceneKey)))
    }
    func getTimeline(in context: Context, completion: @escaping (Timeline<TinyUsEntry>) -> Void) {
        let now = Date()
        let current = refreshed(load(), at: now)
        let today = Calendar.current.startOfDay(for: now)
        let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: today) ?? now.addingTimeInterval(86_400)
        var tomorrowPayload = current
        tomorrowPayload.daysTogether = refreshed(current, at: tomorrow).daysTogether
        let small = scene(TinyAppGroup.smallSceneKey)
        let medium = scene(TinyAppGroup.mediumSceneKey)
        let entries = [
            TinyUsEntry(date: now, payload: current, smallScene: small, mediumScene: medium),
            TinyUsEntry(date: tomorrow, payload: tomorrowPayload, smallScene: small, mediumScene: medium)
        ]
        completion(Timeline(entries: entries, policy: .after(tomorrow.addingTimeInterval(300))))
    }
    private func load() -> TinyWidgetPayload {
        guard let data = TinyAppGroup.defaults.data(forKey: TinyAppGroup.payloadKey),
              let payload = try? JSONDecoder().decode(TinyWidgetPayload.self, from: data) else { return .empty }
        return payload
    }
    private func scene(_ key: String) -> UIImage? {
        guard let data = TinyAppGroup.defaults.data(forKey: key) else { return nil }
        return UIImage(data: data)
    }
    private func refreshed(_ payload: TinyWidgetPayload, at date: Date) -> TinyWidgetPayload {
        var result = payload
        let anniversary = Calendar.current.startOfDay(for: result.anniversaryDate)
        let currentDay = Calendar.current.startOfDay(for: date)
        // Inclusive count, matching RelationshipTimeCalculator: the anniversary itself is Day 1.
        let elapsed = Calendar.current.dateComponents([.day], from: anniversary, to: currentDay).day ?? 0
        result.daysTogether = Int64(max(1, elapsed + 1))
        return result
    }
}

private struct TinyUsWidgetView: View {
    let entry: TinyUsEntry
    @Environment(\.widgetFamily) private var family
    private let cream = Color(red: 1, green: 0.86, blue: 0.67)

    var body: some View {
        if let picture = family == .systemSmall ? entry.smallScene : entry.mediumScene {
            SceneWidgetView(payload: entry.payload, picture: picture, small: family == .systemSmall)
        } else {
            classic
        }
    }

    /// Before the app has drawn a scene: the gradient card with the little pixel couple.
    private var classic: some View {
        ZStack {
            LinearGradient(colors: [Color(red: 0.18, green: 0.2, blue: 0.31), Color(red: 0.35, green: 0.26, blue: 0.34)], startPoint: .topLeading, endPoint: .bottomTrailing)
            VStack(alignment: .leading, spacing: 7) {
                HStack {
                    Text("TINY US").font(.system(size: 10, weight: .black, design: .monospaced)).tracking(1.3).foregroundStyle(cream)
                    Spacer()
                    // SF Symbol name; an unknown name (e.g. an older payload) simply renders nothing.
                    Image(systemName: entry.payload.sharedMoodEmoji).font(.system(size: 14, weight: .semibold)).foregroundStyle(cream)
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
                    Text(entry.payload.dailyMomentPrompt).font(.system(size: 10, weight: .medium, design: .rounded)).lineLimit(1).foregroundStyle(.white.opacity(0.8))
                    Text(entry.payload.latestSignalText).font(.system(size:9,weight:.medium,design:.rounded)).lineLimit(1).foregroundStyle(.white.opacity(0.67))
                    HStack { Text(entry.payload.sceneName); Spacer(); Text(entry.payload.timePhase); Spacer(); Text(entry.payload.weatherName) }
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

/// The couple's real scene (as on Android): the picture fills the widget, with the names, a status
/// line and the day count on a strip along the top, which is usually sky or ceiling.
private struct SceneWidgetView: View {
    let payload: TinyWidgetPayload
    let picture: UIImage
    let small: Bool
    private let dayPink = Color(red: 0.79, green: 0.09, blue: 0.29)

    private var status: String {
        payload.latestSignalText == TinyWidgetPayload.empty.latestSignalText ? payload.dailyMomentPrompt : payload.latestSignalText
    }

    var body: some View {
        ZStack(alignment: .top) {
            GeometryReader { geo in
                Image(uiImage: picture)
                    .resizable()
                    .interpolation(.none)
                    .scaledToFill()
                    .frame(width: geo.size.width, height: geo.size.height, alignment: .bottom)
                    .clipped()
            }
            HStack(alignment: .center, spacing: 8) {
                VStack(alignment: .leading, spacing: 1) {
                    Text(payload.coupleNames).font(.system(size: 13, weight: .bold, design: .rounded)).lineLimit(1)
                    Text(status).font(.system(size: 11, weight: .medium, design: .rounded)).lineLimit(1).opacity(0.95)
                }
                .foregroundStyle(.white)
                .shadow(color: .black.opacity(0.6), radius: 2, y: 1)
                Spacer(minLength: 0)
                if !small {
                    dayPill
                }
            }
            .padding(.horizontal, 12)
            .padding(.top, 9)
            .padding(.bottom, 14)
            .frame(maxWidth: .infinity)
            .background(LinearGradient(colors: [.black.opacity(0.45), .clear], startPoint: .top, endPoint: .bottom))
            if small {
                VStack { Spacer(); HStack { Spacer(); dayPill } }.padding(8)
            }
        }
        .widgetBackground()
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Tiny Us. \(payload.coupleNames) in the \(payload.sceneName), day \(payload.daysTogether). \(status)")
    }

    private var dayPill: some View {
        Text("Day \(payload.daysTogether)")
            .font(.system(size: 11, weight: .bold, design: .rounded))
            .foregroundStyle(dayPink)
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(Capsule().fill(Color.white.opacity(0.92)))
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
            for index in 0..<2 {
                let x = index == 0 ? 3 : 12
                drawWidgetPixel(&context, unit:unit, x:x, y:1, width:7, height:1, color:Color(red:0.34,green:0.25,blue:0.29))
                drawWidgetPixel(&context, unit:unit, x:x-1, y:2, width:9, height:5, color:Color(red:0.96,green:0.81,blue:0.7))
                drawWidgetPixel(&context, unit:unit, x:x+1, y:4, width:1, height:1, color:Color(red:0.22,green:0.2,blue:0.25))
                drawWidgetPixel(&context, unit:unit, x:x+6, y:4, width:1, height:1, color:Color(red:0.22,green:0.2,blue:0.25))
                drawWidgetPixel(&context, unit:unit, x:x, y:7, width:7, height:6, color:index == 0 ? Color(red:0.48,green:0.67,blue:0.82) : Color(red:0.88,green:0.53,blue:0.63))
                drawWidgetPixel(&context, unit:unit, x:x+1, y:13, width:2, height:3, color:Color(red:0.9,green:0.76,blue:0.64))
                drawWidgetPixel(&context, unit:unit, x:x+4, y:13, width:2, height:3, color:Color(red:0.9,green:0.76,blue:0.64))
            }
            drawWidgetPixel(&context, unit:unit, x:9, y:8, width:4, height:2, color:Color(red:1,green:0.77,blue:0.72))
        }
        .accessibilityHidden(true)
    }
}

private func drawWidgetPixel(_ context: inout GraphicsContext, unit: CGFloat, x: Int, y: Int, width: Int, height: Int, color: Color) {
    let rect = CGRect(x:CGFloat(x)*unit, y:CGFloat(y)*unit, width:CGFloat(width)*unit, height:CGFloat(height)*unit)
    context.fill(Path(rect), with:.color(color))
}

struct TinyUsWidget: Widget {
    let kind = TinyAppGroup.widgetKind
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
