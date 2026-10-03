import SwiftUI
import Shared
import AVFoundation
import UserNotifications
import WidgetKit

private enum TinyScene: String, CaseIterable, Codable, Identifiable {
    case meadow, tree, kitchen, living, nightWalk, cafe, sunroom, loft, momo, scooter, twilight
    var id: String { rawValue }
    var title: String {
        switch self {
        case .meadow: return "Flower Meadow"
        case .tree: return "Our Old Tree"
        case .kitchen: return "Little Kitchen"
        case .living: return "Couch & Cuddles"
        case .nightWalk: return "Lantern Walk"
        case .cafe: return "Rainy Day Café"
        case .sunroom: return "Cottage Sunroom"
        case .loft: return "Midnight Loft"
        case .momo: return "Momo Stall"
        case .scooter: return "Evening Ride"
        case .twilight: return "Just Us"
        }
    }
    var icon: String {
        switch self {
        case .meadow: return "sun.max.fill"
        case .tree: return "leaf.fill"
        case .kitchen: return "cup.and.saucer.fill"
        case .living: return "sofa.fill"
        case .nightWalk: return "lamp.desk.fill"
        case .cafe: return "cloud.rain.fill"
        case .sunroom: return "camera.macro"
        case .loft: return "moon.stars.fill"
        case .momo: return "takeoutbag.and.cup.and.straw.fill"
        case .scooter: return "bicycle"
        case .twilight: return "heart.fill"
        }
    }
    var isIndoor: Bool { [.kitchen, .living, .cafe, .sunroom, .loft].contains(self) }
}

private enum TinyWeather: String, CaseIterable, Codable, Identifiable {
    case sunny, rain, sakura, autumn, snow
    var id: String { rawValue }
    var label: String {
        switch self {
        case .sunny: return "Sunny breeze"
        case .rain: return "Soft rain"
        case .sakura: return "Sakura petals"
        case .autumn: return "Autumn leaves"
        case .snow: return "Gentle snow"
        }
    }
    var icon: String {
        switch self {
        case .sunny: return "sun.max.fill"
        case .rain: return "cloud.rain.fill"
        case .sakura: return "wind"
        case .autumn: return "leaf.fill"
        case .snow: return "snowflake"
        }
    }
}

private struct TinySong: Identifiable, Hashable {
    let id: String
    let title: String
    let artist: String
    var resource: String { id }
    static let playlist = [
        TinySong(id: "died_in_your_arms", title: "(I Just) Died In Your Arms", artist: "Cutting Crew"),
        TinySong(id: "cant_take_my_eyes_off_you", title: "Can't Take My Eyes Off You", artist: "Frankie Valli"),
        TinySong(id: "wicked_game", title: "Wicked Game", artist: "Chris Isaak"),
        TinySong(id: "golden_brown", title: "Golden Brown", artist: "The Stranglers")
    ]
}

@MainActor private final class TinyAudio: ObservableObject {
    static let shared = TinyAudio()
    @Published private(set) var currentSong: TinySong?
    @Published private(set) var isMusicPlaying = false
    private var weatherPlayer: AVAudioPlayer?
    private var musicPlayer: AVAudioPlayer?
    private var oldWeatherPlayers: [AVAudioPlayer] = []
    private var weather: TinyWeather = .sunny
    private var isIndoor = false
    private var soundEnabled = true
    private var isForeground = true
    private var musicWasPlayingBeforeBackground = false
    private var weatherWasPlayingBeforeBackground = false
    private var musicWasPlayingBeforeInterruption = false
    private var weatherWasPlayingBeforeInterruption = false
    private var interruptionObserver: NSObjectProtocol?

    private init() {
        interruptionObserver = NotificationCenter.default.addObserver(
            forName: AVAudioSession.interruptionNotification,
            object: AVAudioSession.sharedInstance(),
            queue: .main
        ) { [weak self] note in
            self?.handleInterruption(note)
        }
    }

    func update(weather: TinyWeather, indoor: Bool, soundEnabled: Bool) {
        self.weather = weather
        self.isIndoor = indoor
        self.soundEnabled = soundEnabled
        guard soundEnabled else { stopAll(); return }
        guard isForeground else { return }
        configureSession()
        let filename = "bgm_\(weather.rawValue)"
        if weatherPlayer?.url?.deletingPathExtension().lastPathComponent == filename {
            weatherPlayer?.setVolume(weatherVolume, fadeDuration: 0.35)
            if weatherPlayer?.isPlaying == false { weatherPlayer?.play() }
            return
        }
        guard let url = Bundle.main.url(forResource: filename, withExtension: "mp3"),
              let next = try? AVAudioPlayer(contentsOf: url) else { return }
        next.numberOfLoops = -1
        next.volume = 0
        next.prepareToPlay()
        let previous = weatherPlayer
        weatherPlayer = next
        next.play()
        next.setVolume(weatherVolume, fadeDuration: 1.0)
        if let previous {
            oldWeatherPlayers.append(previous)
            previous.setVolume(0, fadeDuration: 1.0)
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.1) { [weak self, weak previous] in
                previous?.stop()
                self?.oldWeatherPlayers.removeAll { $0 === previous }
            }
        }
    }

    private var weatherVolume: Float {
        (isIndoor ? 0.4 : 1.0) * (isMusicPlaying ? 0.25 : 1.0)
    }

    private func configureSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.ambient, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch { }
    }

    func play(_ song: TinySong) {
        guard soundEnabled, isForeground else { return }
        if currentSong == song, let musicPlayer {
            if musicPlayer.isPlaying { musicPlayer.pause(); isMusicPlaying = false }
            else { configureSession(); musicPlayer.play(); isMusicPlaying = true }
            update(weather: weather, indoor: isIndoor, soundEnabled: soundEnabled)
            return
        }
        guard let url = Bundle.main.url(forResource: song.resource, withExtension: "mp3"),
              let next = try? AVAudioPlayer(contentsOf: url) else { return }
        configureSession()
        musicPlayer?.stop()
        next.numberOfLoops = 0
        next.volume = 0.9
        next.prepareToPlay()
        musicPlayer = next
        currentSong = song
        isMusicPlaying = next.play()
        update(weather: weather, indoor: isIndoor, soundEnabled: soundEnabled)
        let duration = next.duration
        DispatchQueue.main.asyncAfter(deadline: .now() + duration + 0.2) { [weak self, weak next] in
            guard let self, let next, self.musicPlayer === next, !next.isPlaying else { return }
            self.isMusicPlaying = false
            self.update(weather: self.weather, indoor: self.isIndoor, soundEnabled: self.soundEnabled)
        }
    }

    func toggleMusic() {
        guard let musicPlayer, soundEnabled else { return }
        if musicPlayer.isPlaying { musicPlayer.pause(); isMusicPlaying = false }
        else { configureSession(); musicPlayer.play(); isMusicPlaying = true }
        update(weather: weather, indoor: isIndoor, soundEnabled: soundEnabled)
    }

    func pauseForBackground() {
        isForeground = false
        musicWasPlayingBeforeBackground = musicPlayer?.isPlaying == true
        weatherWasPlayingBeforeBackground = weatherPlayer?.isPlaying == true
        weatherPlayer?.pause()
        musicPlayer?.pause()
        isMusicPlaying = false
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
    }

    func resumeFromBackground() {
        isForeground = true
        guard soundEnabled else { return }
        configureSession()
        update(weather: weather, indoor: isIndoor, soundEnabled: soundEnabled)
        if musicWasPlayingBeforeBackground, let musicPlayer, currentSong != nil {
            musicPlayer.play(); isMusicPlaying = true
            update(weather: weather, indoor: isIndoor, soundEnabled: soundEnabled)
        }
        musicWasPlayingBeforeBackground = false
        weatherWasPlayingBeforeBackground = false
    }

    private func handleInterruption(_ notification: Notification) {
        guard let rawType = notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
              let type = AVAudioSession.InterruptionType(rawValue: rawType) else { return }
        if type == .began {
            musicWasPlayingBeforeInterruption = musicPlayer?.isPlaying == true
            weatherWasPlayingBeforeInterruption = weatherPlayer?.isPlaying == true
            musicPlayer?.pause(); weatherPlayer?.pause(); isMusicPlaying = false
        } else {
            guard let rawOptions = notification.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt,
                  AVAudioSession.InterruptionOptions(rawValue: rawOptions).contains(.shouldResume),
                  soundEnabled, isForeground else { return }
            configureSession()
            if weatherWasPlayingBeforeInterruption { weatherPlayer?.play() }
            if musicWasPlayingBeforeInterruption { musicPlayer?.play(); isMusicPlaying = true }
            update(weather: weather, indoor: isIndoor, soundEnabled: soundEnabled)
            musicWasPlayingBeforeInterruption = false
            weatherWasPlayingBeforeInterruption = false
        }
    }

    private func stopAll() {
        weatherPlayer?.stop(); weatherPlayer = nil
        musicPlayer?.stop(); musicPlayer = nil
        oldWeatherPlayers.forEach { $0.stop() }
        oldWeatherPlayers.removeAll()
        currentSong = nil
        isMusicPlaying = false
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
    }
}

private enum TinyFeature: String, CaseIterable, Identifiable {
    case musicBox = "Music Box", moments = "Daily Moment", memories = "Our Memories", notes = "Love Notes"
    case dreams = "Dream Journal", garden = "Little Garden", wardrobe = "Wardrobe"
    case adventures = "Date Adventures", games = "Two-Person Games", mood = "Shared Mood"
    case home = "Tiny Home", longDistance = "Long-Distance Mode", timer = "Our Time"
    var id: String { rawValue }
    var icon: String {
        switch self {
        case .musicBox: return "music.note"
        case .moments: return "sparkles"
        case .memories: return "photo.on.rectangle.angled"
        case .notes: return "envelope.heart.fill"
        case .dreams: return "moon.zzz.fill"
        case .garden: return "camera.macro"
        case .wardrobe: return "tshirt.fill"
        case .adventures: return "map.fill"
        case .games: return "gamecontroller.fill"
        case .mood: return "face.smiling"
        case .home: return "house.fill"
        case .longDistance: return "paperplane.fill"
        case .timer: return "heart.circle.fill"
        }
    }
}

private struct TinyEntry: Codable, Identifiable {
    var id = UUID()
    var text: String
    var date = Date()
}

private struct TinySave: Codable {
    var nameOne = "You"
    var nameTwo = "Your person"
    var anniversary = Calendar.current.startOfDay(for: Date())
    var scene: TinyScene = .meadow
    var weather: TinyWeather = .sunny
    var weatherChangedAt = Date()
    var entries: [String: [TinyEntry]] = [:]
    var gardenSeeds = 3
    var outfit = 0
    var homeLevel = 1
    var mood = "🥰"
    var dailyPromptDate = ""
    var dailyPrompt = "What tiny thing made you smile today?"
    var soundOn = true
    var notificationsOn = false
}

@MainActor private final class TinyWorld: ObservableObject {
    @Published var save: TinySave { didSet { persist() } }
    @Published var showOnboarding = false
    @Published var activeFeature: TinyFeature?
    @Published var toast: String?
    private let key = "tiny-us.ios.world.v1"
    private var chimePlayer: AVAudioPlayer?
    init() {
        if let data = UserDefaults.standard.data(forKey: key),
           let restored = try? JSONDecoder().decode(TinySave.self, from: data) {
            save = restored
            showOnboarding = restored.nameOne == "You" && restored.nameTwo == "Your person"
        } else {
            save = TinySave()
            showOnboarding = true
        }
        refreshPromptIfNeeded()
    }
    func persist() {
        guard let data = try? JSONEncoder().encode(save) else { return }
        UserDefaults.standard.set(data, forKey: key)
        let sharedStorage = IosUserDefaultsStorage(defaults: UserDefaults.standard)
        sharedStorage.putString(key: "bf_name", value: save.nameOne)
        sharedStorage.putString(key: "gf_name", value: save.nameTwo)
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        sharedStorage.putString(key: "anniversary_date", value: formatter.string(from: save.anniversary))
        publishWidgetSnapshot()
    }
    private func publishWidgetSnapshot() {
        let calendar = Calendar.current
        let start = calendar.dateComponents([.year, .month, .day], from: save.anniversary)
        let today = calendar.dateComponents([.year, .month, .day, .hour], from: Date())
        let days = RelationshipTimeCalculator.shared.calculateDays(
            startYear: Int32(start.year ?? 2024), startMonth: Int32(start.month ?? 1), startDay: Int32(start.day ?? 1),
            currentYear: Int32(today.year ?? 2026), currentMonth: Int32(today.month ?? 1), currentDay: Int32(today.day ?? 1))
        let phase = TimeOfDayPhaseKt.currentPhase(hour: Int32(today.hour ?? 12)).displayName
        let signal = save.entries[TinyFeature.longDistance.rawValue, default: []].first?.text ?? "A little hello from your world."
        let payload = TinyWidgetPayload(
            coupleNames: "\(save.nameOne) ♥ \(save.nameTwo)", daysTogether: days,
            sceneName: save.scene.title, weatherName: save.weather.label, timePhase: phase,
            dailyMomentPrompt: save.dailyPrompt, sharedMoodEmoji: save.mood,
            latestSignalText: signal, updatedAt: Date())
        guard let encoded = try? JSONEncoder().encode(payload),
              let shared = UserDefaults(suiteName: "group.com.example.tinyus.shared") else { return }
        shared.set(encoded, forKey: "tiny-us.widget.payload")
        WidgetCenter.shared.reloadTimelines(ofKind: "TinyUsWidget")
    }
    func add(_ text: String, to feature: TinyFeature) {
        let clean = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        save.entries[feature.rawValue, default: []].insert(TinyEntry(text: clean), at: 0)
        showToast("Tucked safely into \(feature.rawValue.lowercased())")
    }
    func showToast(_ value: String) {
        toast = value
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) { [weak self] in self?.toast = nil }
    }
    func refreshPromptIfNeeded() {
        let stamp = Calendar.current.startOfDay(for: Date()).formatted(date: .numeric, time: .omitted)
        guard save.dailyPromptDate != stamp else { return }
        let prompts = [
            "What tiny thing made you smile today?", "What would make your person feel loved this week?",
            "Name one ordinary moment you want to remember.", "What is something you admire about each other?",
            "If you could take a little date anywhere, where would you go?"
        ]
        let day = Calendar.current.ordinality(of: .day, in: .era, for: Date()) ?? 0
        save.dailyPrompt = prompts[day % prompts.count]
        save.dailyPromptDate = stamp
    }
    func playMusicBox() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.ambient, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            showToast("Audio is unavailable right now")
            return
        }
        let sampleRate = 22_050
        let frameCount = sampleRate * 2 / 3
        var pcm = Data(capacity: frameCount * 2)
        for frame in 0..<frameCount {
            let time = Double(frame) / Double(sampleRate)
            let fade = exp(-time * 4.1)
            let fundamental = sin(2 * Double.pi * 528 * time)
            let overtone = sin(2 * Double.pi * 1056 * time) * 0.24
            var sample = Int16(max(-1, min(1, (fundamental + overtone) * fade * 0.48)) * Double(Int16.max)).littleEndian
            withUnsafeBytes(of: &sample) { pcm.append(contentsOf: $0) }
        }
        var wav = Data()
        func append<T: FixedWidthInteger>(_ value: T) { var little = value.littleEndian; withUnsafeBytes(of: &little) { wav.append(contentsOf: $0) } }
        wav.append(contentsOf: Array("RIFF".utf8)); append(UInt32(36 + pcm.count))
        wav.append(contentsOf: Array("WAVEfmt ".utf8)); append(UInt32(16)); append(UInt16(1)); append(UInt16(1))
        append(UInt32(sampleRate)); append(UInt32(sampleRate * 2)); append(UInt16(2)); append(UInt16(16))
        wav.append(contentsOf: Array("data".utf8)); append(UInt32(pcm.count)); wav.append(pcm)
        do { chimePlayer = try AVAudioPlayer(data: wav); chimePlayer?.prepareToPlay(); chimePlayer?.play() }
        catch { showToast("The Music Box is resting for a moment") }
    }
    func stopAudioForBackground() {
        chimePlayer?.stop()
        chimePlayer = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
    }
}

struct ContentView: View {
    @StateObject private var world = TinyWorld()
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.scenePhase) private var scenePhase
    @ObservedObject private var audio = TinyAudio.shared
    @State private var momentAnswer = ""
    @State private var showWeather = false
    @State private var showScenes = false
    @State private var showSettings = false
    @State private var showFeatures = false
    @State private var tick = Date.now
    private let clock = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                Color(red: 0.10, green: 0.105, blue: 0.16).ignoresSafeArea()
                VStack(spacing: 0) {
                    topBar
                    WorldCanvas(scene: world.save.scene, weather: world.save.weather,
                                time: tick, reducedMotion: reduceMotion, action: world.showToast)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                        .padding(.horizontal, 14)
                        .overlay(alignment: .bottom) { worldCaption.padding(.bottom, 16) }
                        .contentShape(Rectangle())
                        .gesture(DragGesture(minimumDistance: 30).onEnded { value in
                            if abs(value.translation.width) > abs(value.translation.height) {
                                changeScene(step: value.translation.width < 0 ? 1 : -1)
                            } else if value.translation.height < 0 { showScenes = true }
                        })
                        .onTapGesture { world.showToast(["A tiny wave hello 👋", "They share a little smile 💛", "Mochi is very pleased 🐾"].randomElement() ?? "A tiny wave hello 👋") }
                    worldStrip
                    bottomBar(safeBottom: proxy.safeAreaInsets.bottom)
                }
                .padding(.top, 5)
                if let toast = world.toast {
                    Text(toast).font(.system(.footnote, design: .rounded, weight: .semibold))
                        .foregroundStyle(.white).padding(.horizontal, 16).padding(.vertical, 11)
                        .background(.ultraThinMaterial, in: Capsule()).padding(.bottom, 110)
                        .frame(maxHeight: .infinity, alignment: .bottom).transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: world.toast)
            .sheet(isPresented: $showWeather) { weatherSheet }
            .sheet(isPresented: $showScenes) { sceneSheet }
            .sheet(isPresented: $showSettings) { SettingsSheet(world: world) }
            .sheet(isPresented: $showFeatures) { FeaturePickerSheet(world: world) }
            .sheet(item: $world.activeFeature) { feature in FeatureSheet(feature: feature, world: world) }
            .fullScreenCover(isPresented: $world.showOnboarding) { OnboardingSheet(world: world) }
            .onReceive(clock) { date in
                tick = date
                if date.timeIntervalSince(world.save.weatherChangedAt) >= 360 {
                    let choices = TinyWeather.allCases.filter { $0 != world.save.weather }
                    world.save.weather = choices.randomElement() ?? .sunny
                    world.save.weatherChangedAt = date
                }
            }
            .onAppear { updateAudio() }
            .onChange(of: world.save.weather) { _ in updateAudio() }
            .onChange(of: world.save.scene) { _ in updateAudio() }
            .onChange(of: world.save.soundOn) { _ in updateAudio() }
            .onChange(of: scenePhase) { phase in
                if phase == .background { audio.pauseForBackground(); world.stopAudioForBackground(); world.persist() }
                if phase == .active { audio.resumeFromBackground() }
            }
            .preferredColorScheme(.dark)
        }
    }

    private var topBar: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                Text("TINY US").font(.system(size: 18, weight: .black, design: .monospaced)).tracking(2)
                    .foregroundStyle(Color(red: 1, green: 0.83, blue: 0.58))
                Text("\(world.save.nameOne)  ♥  \(world.save.nameTwo)")
                    .font(.system(.caption, design: .rounded, weight: .medium)).foregroundStyle(.white.opacity(0.72))
                    .lineLimit(1)
            }
            Spacer()
            Button {
                if world.save.soundOn { world.activeFeature = .musicBox }
                else { world.showToast("Music Box is turned off") }
            } label: {
                Image(systemName: "music.note").font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color(red: 1, green: 0.82, blue: 0.58))
                    .frame(width: 36, height: 36).background(.white.opacity(0.08), in: Circle())
            }.accessibilityLabel("Play Music Box")
            Button { world.activeFeature = .timer } label: {
                Label("\(relationshipDays)", systemImage: "heart.fill")
                    .font(.system(.caption, design: .rounded, weight: .bold))
                    .foregroundStyle(Color(red: 1, green: 0.75, blue: 0.72))
                    .padding(.horizontal, 11).padding(.vertical, 8).background(.white.opacity(0.08), in: Capsule())
            }.accessibilityLabel("\(relationshipDays) days together")
            Button { showSettings = true } label: {
                Image(systemName: "slider.horizontal.3").font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.9)).frame(width: 38, height: 38).background(.white.opacity(0.08), in: Circle())
            }.accessibilityLabel("Settings")
        }.padding(.horizontal, 20).padding(.bottom, 12)
    }

    private var relationshipDays: Int64 {
        let cal = Calendar.current
        let a = cal.dateComponents([.year, .month, .day], from: world.save.anniversary)
        let b = cal.dateComponents([.year, .month, .day], from: Date())
        return RelationshipTimeCalculator.shared.calculateDays(
            startYear: Int32(a.year ?? 2024), startMonth: Int32(a.month ?? 1), startDay: Int32(a.day ?? 1),
            currentYear: Int32(b.year ?? 2026), currentMonth: Int32(b.month ?? 1), currentDay: Int32(b.day ?? 1))
    }

    private var worldCaption: some View {
        VStack(spacing: 3) {
            Text(world.save.scene.title.uppercased()).font(.system(.caption2, design: .monospaced, weight: .bold)).tracking(1.4)
            Text("\(phaseName)  ·  \(world.save.weather.label)").font(.system(.caption2, design: .rounded, weight: .medium)).opacity(0.8)
        }.foregroundStyle(.white).padding(.horizontal, 15).padding(.vertical, 9)
            .background(.black.opacity(0.28), in: Capsule())
    }
    private var phaseName: String {
        let h = Calendar.current.component(.hour, from: tick)
        return TimeOfDayPhaseKt.currentPhase(hour: Int32(h)).displayName
    }

    private var worldStrip: some View {
        HStack(spacing: 10) {
            Button { showScenes = true } label: { quickTile("Scenes", icon: "square.grid.2x2.fill") }
            Button { showWeather = true } label: { quickTile(world.save.weather.label, icon: world.save.weather.icon) }
            Button { world.activeFeature = .moments } label: { quickTile("Today", icon: "sparkles") }
        }.buttonStyle(.plain).padding(.horizontal, 14).padding(.top, 12)
    }
    private func quickTile(_ title: String, icon: String) -> some View {
        HStack(spacing: 7) {
            Image(systemName: icon).foregroundStyle(Color(red: 1, green: 0.82, blue: 0.54))
            Text(title).lineLimit(1).minimumScaleFactor(0.75)
        }.font(.system(.caption, design: .rounded, weight: .semibold))
            .foregroundStyle(.white.opacity(0.9)).frame(maxWidth: .infinity).padding(.vertical, 12)
            .background(.white.opacity(0.075), in: RoundedRectangle(cornerRadius: 14))
    }

    private func bottomBar(safeBottom: CGFloat) -> some View {
        VStack(spacing: 10) {
            HStack(spacing: 0) {
                bottomButton("Explore", icon: "sparkles") { world.activeFeature = .adventures }
                bottomButton("Memories", icon: "photo.on.rectangle.angled") { world.activeFeature = .memories }
                bottomButton("Love note", icon: "envelope.heart.fill") { world.activeFeature = .notes }
                bottomButton("More", icon: "ellipsis.circle.fill") { showFeatures = true }
            }
        }.padding(.horizontal, 9).padding(.top, 11).padding(.bottom, max(7, safeBottom == 0 ? 10 : 0))
            .background(.black.opacity(0.20))
    }
    private func bottomButton(_ name: String, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 5) {
                Image(systemName: icon).font(.system(size: 18, weight: .medium))
                Text(name).font(.system(size: 10, weight: .medium, design: .rounded))
            }.foregroundStyle(.white.opacity(0.78)).frame(maxWidth: .infinity).contentShape(Rectangle())
        }.buttonStyle(.plain)
    }
    private func changeScene(step: Int) {
        guard let index = TinyScene.allCases.firstIndex(of: world.save.scene) else { return }
        let new = (index + step + TinyScene.allCases.count) % TinyScene.allCases.count
        world.save.scene = TinyScene.allCases[new]
        world.showToast(world.save.scene.title)
    }
    private func updateAudio() {
        audio.update(weather: world.save.weather, indoor: world.save.scene.isIndoor, soundEnabled: world.save.soundOn)
    }

    private var weatherSheet: some View {
        NavigationStack {
            List(TinyWeather.allCases) { weather in
                Button { world.save.weather = weather; world.save.weatherChangedAt = Date(); showWeather = false; world.showToast("The sky is changing gently") } label: {
                    Label(weather.label, systemImage: weather.icon).foregroundStyle(.primary)
                        .overlay(alignment: .trailing) { if weather == world.save.weather { Image(systemName: "checkmark").foregroundStyle(.tint) } }
                }
            }.navigationTitle("A little weather").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Done") { showWeather = false } } }
        }.presentationDetents([.medium, .large]).presentationDragIndicator(.visible)
    }
    private var sceneSheet: some View {
        NavigationStack {
            List(TinyScene.allCases) { scene in
                Button { world.save.scene = scene; showScenes = false } label: {
                    HStack { Label(scene.title, systemImage: scene.icon).foregroundStyle(.primary); Spacer(); if scene == world.save.scene { Image(systemName: "checkmark").foregroundStyle(.tint) } }
                }
            }.navigationTitle("Places in our world").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Done") { showScenes = false } } }
        }.presentationDetents([.medium, .large]).presentationDragIndicator(.visible)
    }
}

private struct FeaturePickerSheet: View {
    @ObservedObject var world: TinyWorld
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            List(TinyFeature.allCases) { feature in
                Button {
                    dismiss()
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { world.activeFeature = feature }
                } label: {
                    Label(feature.rawValue, systemImage: feature.icon).foregroundStyle(.primary)
                }
            }
            .navigationTitle("Little things for us")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Done") { dismiss() } } }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }
}

// Procedural native Canvas world: stable scene geometry with time-driven movement and no sprite allocations per frame.
private struct WorldCanvas: View {
    let scene: TinyScene
    let weather: TinyWeather
    let time: Date
    let reducedMotion: Bool
    let action: (String) -> Void
    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0, paused: reducedMotion)) { context in
            Canvas(opaque: true, colorMode: .linear) { graphics, size in
                drawWorld(in: &graphics, size: size, seconds: reducedMotion ? 0 : context.date.timeIntervalSinceReferenceDate)
            }
        }.accessibilityLabel("Animated pixel-art scene: \(scene.title), \(weather.label)")
    }
    private func drawWorld(in c: inout GraphicsContext, size: CGSize, seconds t: Double) {
        let w = size.width, h = size.height
        let night = [TinyScene.nightWalk, .loft, .twilight].contains(scene) || (Calendar.current.component(.hour, from: time) < 6 || Calendar.current.component(.hour, from: time) > 19)
        let top = night ? Color(red: 0.18, green: 0.21, blue: 0.37) : Color(red: 0.52, green: 0.69, blue: 0.78)
        let bottom = night ? Color(red: 0.43, green: 0.38, blue: 0.46) : Color(red: 0.96, green: 0.72, blue: 0.57)
        c.fill(Path(CGRect(origin: .zero, size: size)), with: .linearGradient(Gradient(colors: [top, bottom]), startPoint: .zero, endPoint: CGPoint(x: 0, y: h)))
        drawSunMoon(&c, w: w, h: h, night: night, t: t)
        drawClouds(&c, w: w, h: h, t: t)
        drawBackdrop(&c, w: w, h: h, night: night)
        drawSceneProps(&c, w: w, h: h, t: t)
        drawWeather(&c, w: w, h: h, t: t)
        drawCharacters(&c, w: w, h: h, t: t)
    }
    private func rect(_ c: inout GraphicsContext, _ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat, _ color: Color) {
        c.fill(Path(CGRect(x: x, y: y, width: w, height: h)), with: .color(color))
    }
    private func drawSunMoon(_ c: inout GraphicsContext, w: CGFloat, h: CGFloat, night: Bool, t: Double) {
        let x = w * 0.78 + CGFloat(sin(t * 0.08)) * 5, y = h * 0.16 + CGFloat(sin(t * 0.12)) * 3
        let r = CGRect(x: x, y: y, width: 43, height: 43)
        c.fill(Path(ellipseIn: r.insetBy(dx: -8, dy: -8)), with: .color((night ? Color.white : Color(red: 1, green: 0.88, blue: 0.55)).opacity(0.12)))
        c.fill(Path(ellipseIn: r), with: .color(night ? Color(red: 0.93, green: 0.9, blue: 0.78) : Color(red: 1, green: 0.86, blue: 0.56)))
        if night { rect(&c, x + 5, y + 6, 3, 3, .white.opacity(0.7)); rect(&c, w * 0.18, h * 0.15, 2, 2, .white); rect(&c, w * 0.57, h * 0.1, 2, 2, .white.opacity(0.8)) }
    }
    private func drawClouds(_ c: inout GraphicsContext, w: CGFloat, h: CGFloat, t: Double) {
        for i in 0..<3 {
            let base = CGFloat(i) * w * 0.47 - 35
            let x = (base + CGFloat(t * (7 + Double(i))) .truncatingRemainder(dividingBy: w + 90)) - 45
            let y = h * CGFloat(0.24 + Double(i % 2) * 0.11)
            let color = Color.white.opacity(weather == .rain ? 0.48 : 0.46)
            rect(&c, x, y + 8, 48, 10, color); rect(&c, x + 8, y, 25, 18, color); rect(&c, x + 25, y + 3, 22, 15, color)
        }
    }
    private func drawBackdrop(_ c: inout GraphicsContext, w: CGFloat, h: CGFloat, night: Bool) {
        let far = night ? Color(red: 0.35, green: 0.38, blue: 0.49) : Color(red: 0.48, green: 0.61, blue: 0.61)
        let near = night ? Color(red: 0.28, green: 0.34, blue: 0.39) : Color(red: 0.39, green: 0.55, blue: 0.43)
        rect(&c, 0, h * 0.49, w, h * 0.20, far)
        for i in 0..<8 { let x = CGFloat(i) * w / 7 - 12; rect(&c, x, h * 0.44 + CGFloat(i % 3) * 7, w * 0.20, h * 0.25, far.opacity(0.76)) }
        rect(&c, 0, h * 0.61, w, h * 0.39, near)
        // Pixel garden beds and the 2.5D path establish a shared ground plane.
        rect(&c, w * 0.44, h * 0.66, w * 0.15, h * 0.34, Color(red: 0.72, green: 0.57, blue: 0.48).opacity(0.62))
        rect(&c, w * 0.32, h * 0.71, w * 0.36, h * 0.29, Color(red: 0.77, green: 0.64, blue: 0.52).opacity(0.45))
        for i in 0..<13 {
            let x = CGFloat((i * 37 + 11) % 101) / 101 * w
            rect(&c, x, h * 0.64 + CGFloat(i % 4) * 4, 2, 7, Color(red: 0.76, green: 0.82, blue: 0.55).opacity(0.6))
        }
        if scene == .tree {
            rect(&c, w * 0.78, h * 0.3, 22, h * 0.4, Color(red: 0.35, green: 0.25, blue: 0.25))
            for i in 0..<5 { rect(&c, w * (0.67 + CGFloat(i) * 0.045), h * (0.23 + CGFloat(i % 2) * 0.045), 39, 27, Color(red: 0.38, green: 0.55, blue: 0.43)) }
        }
        if [.cafe, .sunroom, .loft, .kitchen, .living].contains(scene) {
            rect(&c, w * 0.07, h * 0.38, w * 0.86, h * 0.29, Color(red: 0.34, green: 0.31, blue: 0.39).opacity(0.78))
            rect(&c, w * 0.12, h * 0.41, w * 0.24, h * 0.19, Color(red: 0.74, green: 0.79, blue: 0.79).opacity(0.72))
            rect(&c, w * 0.4, h * 0.41, w * 0.24, h * 0.19, Color(red: 0.74, green: 0.79, blue: 0.79).opacity(0.62))
            rect(&c, w * 0.68, h * 0.41, w * 0.19, h * 0.19, Color(red: 0.74, green: 0.79, blue: 0.79).opacity(0.7))
            rect(&c, w * 0.1, h * 0.62, w * 0.8, 5, Color(red: 0.52, green: 0.37, blue: 0.36))
        }
    }
    private func drawSceneProps(_ c: inout GraphicsContext, w: CGFloat, h: CGFloat, t: Double) {
        if scene == .kitchen { rect(&c,w*0.72,h*0.56,35,24,Color(red:0.75,green:0.62,blue:0.52)); rect(&c,w*0.75,h*0.51,27,8,Color(red:0.89,green:0.78,blue:0.62)) }
        if scene == .cafe { rect(&c,w*0.76,h*0.53,34,5,Color(red:0.78,green:0.54,blue:0.43)); rect(&c,w*0.79,h*0.48,15,12,Color(red:0.89,green:0.76,blue:0.64)) }
        if scene == .sunroom { for i in 0..<4 { let x=w*(0.13+CGFloat(i)*0.18); rect(&c,x,h*0.59,4,27,Color(red:0.34,green:0.5,blue:0.35)); rect(&c,x-5,h*0.56,14,7,Color(red:0.55,green:0.72,blue:0.48)) } }
        if scene == .momo { rect(&c,w*0.67,h*0.48,74,8,Color(red:0.83,green:0.48,blue:0.36)); rect(&c,w*0.7,h*0.39,67,12,Color(red:0.58,green:0.34,blue:0.35)) }
        if scene == .scooter { rect(&c,w*0.67,h*0.75,62,5,Color(red:0.86,green:0.56,blue:0.37)); rect(&c,w*0.7,h*0.77,6,8,Color(red:0.2,green:0.22,blue:0.29)); rect(&c,w*0.83,h*0.77,6,8,Color(red:0.2,green:0.22,blue:0.29)) }
        if scene == .nightWalk { for i in 0..<3 { let x=w*(0.16+CGFloat(i)*0.34); rect(&c,x,h*0.56,3,39,Color(red:0.36,green:0.3,blue:0.3)); rect(&c,x-5,h*0.55,13,7,Color(red:1,green:0.78,blue:0.47)); c.fill(Path(ellipseIn:CGRect(x:x-10,y:h*0.52,width:23,height:23)),with:.color(Color.orange.opacity(0.13))) } }
        if scene == .living { rect(&c,w*0.22,h*0.66,w*0.56,16,Color(red:0.65,green:0.39,blue:0.45)); rect(&c,w*0.24,h*0.62,w*0.52,13,Color(red:0.77,green:0.52,blue:0.55)) }
    }
    private func drawCharacters(_ c: inout GraphicsContext, w: CGFloat, h: CGFloat, t: Double) {
        let stroll = [TinyScene.nightWalk,.meadow,.tree,.scooter].contains(scene)
        let wander = sin(t * (stroll ? 0.58 : 0.21))
        for i in 0..<2 {
            let direction = i == 0 ? -1.0 : 1.0
            let offset = CGFloat(wander * direction) * (stroll ? 23 : 7)
            let x = w * (i == 0 ? 0.43 : 0.57) + offset
            let stride = sin(t * 6 + Double(i) * 1.5)
            let bob = CGFloat(abs(stride)) * 2.2
            let ground = h * 0.79 + CGFloat(i) * 11 + offset * 0.09 // scale and foot position imply depth
            let scale = CGFloat(0.93 + Double(i) * 0.12)
            let swatch = i == 0 ? Color(red:0.48,green:0.68,blue:0.85) : Color(red:0.92,green:0.61,blue:0.67)
            let shadow = CGRect(x:x-15*scale,y:ground+4,width:30*scale,height:7)
            c.fill(Path(ellipseIn:shadow),with:.color(.black.opacity(0.2)))
            let bodyY=ground-34*scale-bob
            rect(&c,x-12*scale,bodyY+15*scale,24*scale,20*scale,swatch)
            rect(&c,x-10*scale,bodyY+33*scale,8*scale,7*scale,Color(red:0.89,green:0.75,blue:0.63))
            rect(&c,x+2*scale,bodyY+33*scale,8*scale,7*scale,Color(red:0.89,green:0.75,blue:0.63))
            rect(&c,x-14*scale,bodyY,28*scale,21*scale,Color(red:0.96,green:0.83,blue:0.69))
            rect(&c,x-10*scale,bodyY+5*scale,3*scale,3*scale,Color(red:0.20,green:0.18,blue:0.22))
            rect(&c,x+6*scale,bodyY+5*scale,3*scale,3*scale,Color(red:0.20,green:0.18,blue:0.22))
            rect(&c,x-8*scale,bodyY+13*scale,4*scale,2*scale,Color(red:0.88,green:0.48,blue:0.51).opacity(0.8))
            rect(&c,x+4*scale,bodyY+13*scale,4*scale,2*scale,Color(red:0.88,green:0.48,blue:0.51).opacity(0.8))
            rect(&c,x-14*scale,bodyY-2*scale,28*scale,8*scale,i == 0 ? Color(red:0.35,green:0.27,blue:0.32) : Color(red:0.84,green:0.48,blue:0.57))
            if scene == .momo || scene == .kitchen || scene == .cafe { rect(&c,x+13*scale,bodyY+20*scale,8*scale,8*scale,Color(red:0.91,green:0.75,blue:0.51)) }
        }
        // Mochi naps beside the pair, with a gentle breathing loop.
        let mx=w*0.76, my=h*0.82+CGFloat(sin(t*1.5))*1.4
        c.fill(Path(ellipseIn:CGRect(x:mx-13,y:my-8,width:27,height:16)),with:.color(Color(red:0.93,green:0.88,blue:0.79)))
        rect(&c,mx-5,my-5,2,2,Color(red:0.35,green:0.31,blue:0.34)); rect(&c,mx+4,my-5,2,2,Color(red:0.35,green:0.31,blue:0.34))
        rect(&c,mx-2,my,4,2,Color(red:0.82,green:0.53,blue:0.56))
        if Int(t / 7) % 3 == 0 { c.draw(Text("z˙").font(.system(size:11,weight:.bold)).foregroundColor(.white.opacity(0.85)),at:CGPoint(x:mx+15,y:my-14)) }
    }
    private func drawWeather(_ c: inout GraphicsContext, w: CGFloat, h: CGFloat, t: Double) {
        let count = weather == .rain ? 42 : weather == .snow ? 34 : 21
        for i in 0..<count {
            let seed = Double((i * 73 + 17) % 997) / 997
            let speed = weather == .rain ? 0.52 + seed * 0.5 : 0.09 + seed * 0.11
            let phase = (seed + t * speed).truncatingRemainder(dividingBy: 1)
            let drift = sin(t * (0.35 + seed) + seed * 50) * (weather == .rain ? 5 : 25)
            let x = CGFloat((seed * 1.13).truncatingRemainder(dividingBy: 1)) * w + CGFloat(drift)
            let y = CGFloat(phase) * h * 0.91
            switch weather {
            case .sunny:
                if i < 8 { let r=CGRect(x:x,y:y,width:2,height:2); c.fill(Path(ellipseIn:r),with:.color(Color(red:1,green:0.93,blue:0.68).opacity(0.55))) }
            case .rain:
                rect(&c,x,y,1.2,8,Color(red:0.79,green:0.86,blue:0.91).opacity(0.52))
                if phase > 0.97 { c.fill(Path(ellipseIn:CGRect(x:x-2,y:h*0.91-1,width:5,height:2)),with:.color(.white.opacity(0.35))) }
            case .sakura:
                let a=sin(t*2+seed*30)*0.8
                c.rotate(by:.radians(a))
                rect(&c,x,y,5,3,i % 2 == 0 ? Color.pink.opacity(0.75) : Color(red:1,green:0.78,blue:0.82))
                c.rotate(by:.radians(-a))
            case .autumn:
                let spin=sin(t*2+seed*20)
                rect(&c,x,y,4+CGFloat(abs(spin))*2,3,[Color(red:0.92,green:0.56,blue:0.31),Color(red:0.83,green:0.7,blue:0.35),Color(red:0.72,green:0.38,blue:0.3)][i%3].opacity(0.88))
            case .snow:
                let r=CGFloat(1.4 + seed*2.7)
                c.fill(Path(ellipseIn:CGRect(x:x,y:y,width:r,height:r)),with:.color(.white.opacity(0.82)))
            }
        }
    }
}

private struct OnboardingSheet: View {
    @ObservedObject var world: TinyWorld
    @State private var date = Calendar.current.startOfDay(for: Date())
    var body: some View {
        NavigationStack {
            ZStack {
                LinearGradient(colors:[Color(red:0.16,green:0.17,blue:0.26),Color(red:0.3,green:0.22,blue:0.29)],startPoint:.top,endPoint:.bottom).ignoresSafeArea()
                ScrollView {
                    VStack(alignment:.leading,spacing:22) {
                        Text("A little world\nfor the two of you.").font(.system(size:34,weight:.bold,design:.rounded)).foregroundStyle(Color(red:1,green:0.85,blue:0.65))
                        Text("Make yourselves at home. Your moments stay on this device, and the world is here even when you’re offline.").font(.system(.body,design:.rounded)).foregroundStyle(.white.opacity(0.8))
                        TextField("Your name",text:$world.save.nameOne).textContentType(.givenName).textFieldStyle(.roundedBorder)
                        TextField("Your person's name",text:$world.save.nameTwo).textContentType(.name).textFieldStyle(.roundedBorder)
                        DatePicker("Your special day",selection:$world.save.anniversary,in:...Date(),displayedComponents:.date).tint(.pink).foregroundStyle(.white)
                        Button { finish() } label: { Text("Step into our world").font(.system(.headline,design:.rounded)).frame(maxWidth:.infinity).padding().background(Color(red:1,green:0.8,blue:0.57),in:RoundedRectangle(cornerRadius:16)).foregroundStyle(Color(red:0.2,green:0.17,blue:0.22)) }
                            .buttonStyle(.plain)
                    }.padding(26)
                }
            }.toolbar { ToolbarItem(placement:.principal) { Text("WELCOME HOME").font(.system(.caption,design:.monospaced,weight:.bold)).tracking(2).foregroundStyle(.white.opacity(0.8)) } }
        }.interactiveDismissDisabled()
    }
    private func finish() {
        if world.save.nameOne.trimmingCharacters(in:.whitespaces).isEmpty { world.save.nameOne="You" }
        if world.save.nameTwo.trimmingCharacters(in:.whitespaces).isEmpty { world.save.nameTwo="Your person" }
        world.persist(); world.showOnboarding=false
    }
}

private struct FeatureSheet: View {
    let feature: TinyFeature
    @ObservedObject var world: TinyWorld
    @ObservedObject private var audio = TinyAudio.shared
    @State private var draft = ""
    @State private var answered = false
    @State private var gameStep = 0
    @Environment(\.dismiss) private var dismiss
    private var entries: [TinyEntry] { world.save.entries[feature.rawValue, default: []] }
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment:.leading,spacing:17) {
                    if feature == .musicBox {
                        Text("A little soundtrack for your tiny world.").font(.system(.headline,design:.rounded))
                        if audio.isMusicPlaying, let song=audio.currentSong {
                            Label("Now playing · \(song.title)",systemImage:"music.note").font(.system(.subheadline,design:.rounded,weight:.semibold)).foregroundStyle(.pink)
                            Button { audio.toggleMusic() } label: { Label("Pause the Music Box",systemImage:"pause.fill") }.buttonStyle(.borderedProminent)
                        } else if audio.currentSong != nil {
                            Button { audio.toggleMusic() } label: { Label("Resume the Music Box",systemImage:"play.fill") }.buttonStyle(.borderedProminent)
                        }
                        Button { world.playMusicBox() } label: { Label("Play a soft chime",systemImage:"sparkles") }.buttonStyle(.bordered)
                        ForEach(TinySong.playlist) { song in
                            Button { audio.play(song) } label: {
                                HStack(spacing:12) {
                                    Image(systemName: audio.currentSong == song && audio.isMusicPlaying ? "waveform" : "play.circle.fill").foregroundStyle(.pink)
                                    VStack(alignment:.leading,spacing:3) { Text(song.title).foregroundStyle(.primary); Text(song.artist).font(.caption).foregroundStyle(.secondary) }
                                    Spacer()
                                    if audio.currentSong == song { Image(systemName:audio.isMusicPlaying ? "speaker.wave.2.fill" : "pause.fill").foregroundStyle(.secondary) }
                                }.padding(11).background(.quaternary,in:RoundedRectangle(cornerRadius:13))
                            }.buttonStyle(.plain)
                        }
                        Text("Weather music softens while a song plays, and pauses when the app goes to the background.").font(.footnote).foregroundStyle(.secondary)
                    } else if feature == .moments {
                        Text(world.save.dailyPrompt).font(.system(.title3,design:.rounded,weight:.semibold)).foregroundStyle(.primary)
                        TextField("Write a little answer…",text:$draft,axis:.vertical).lineLimit(3...6).textFieldStyle(.roundedBorder)
                        Button("Save today’s moment") { world.add(draft,to:.moments); draft=""; answered=true }
                            .buttonStyle(.borderedProminent).tint(Color(red:0.73,green:0.42,blue:0.48))
                        if answered { Label("Saved for today",systemImage:"checkmark.circle.fill").foregroundStyle(.green) }
                    } else if feature == .timer {
                        Label("\(world.save.nameOne) & \(world.save.nameTwo)",systemImage:"heart.fill").font(.system(.title2,design:.rounded,weight:.bold)).foregroundStyle(.pink)
                        Text("\(max(0,Calendar.current.dateComponents([.day],from:world.save.anniversary,to:Date()).day ?? 0)) days of little and big moments together.").font(.system(.body,design:.rounded))
                        Text("Since \(world.save.anniversary.formatted(date:.long,time:.omitted))").foregroundStyle(.secondary)
                    } else if feature == .mood {
                        Text("How are you feeling about us today?").font(.system(.headline,design:.rounded))
                        HStack { ForEach(["🥰","😊","🌱","🫶","🌙"],id:\.self) { emoji in Button(emoji) { world.save.mood=emoji; world.showToast("Your shared mood is \(emoji)") }.font(.largeTitle).buttonStyle(.plain) } }
                        Text("Your mood is kept privately on this device.").font(.footnote).foregroundStyle(.secondary)
                    } else if feature == .garden {
                        Text("A little corner to grow together.").font(.system(.headline,design:.rounded))
                        Text("🌱  \(world.save.gardenSeeds) seeds waiting to bloom").font(.system(.title3,design:.rounded))
                        Button("Plant a seed") { world.save.gardenSeeds=max(0,world.save.gardenSeeds-1); world.showToast("A new little sprout is growing 🌱") }.buttonStyle(.borderedProminent).disabled(world.save.gardenSeeds==0)
                    } else if feature == .wardrobe {
                        Text("Choose a cozy outfit for your tiny people.").font(.system(.headline,design:.rounded))
                        ForEach(Array(["Soft Sunday","Picnic gingham","Rainy day knits","Starlight pajamas"].enumerated()),id:\.offset) { index,title in
                            Button { world.save.outfit=index; world.showToast("Outfit changed to \(title)") } label: { HStack { Text(["🧺","🌼","☔️","🌙"][index]); Text(title); Spacer(); if index==world.save.outfit { Image(systemName:"checkmark.circle.fill") } }.padding(12).background(.quaternary,in:RoundedRectangle(cornerRadius:12)) }.buttonStyle(.plain)
                        }
                    } else if feature == .home {
                        Text("Your tiny home grows with your moments together.").font(.system(.headline,design:.rounded))
                        Text("🏡 Home level \(world.save.homeLevel)").font(.system(.title,design:.rounded,weight:.bold))
                        Button("Add a little keepsake") { world.save.homeLevel+=1; world.showToast("Your home feels a little more yours 🏡") }.buttonStyle(.borderedProminent)
                    } else if feature == .adventures {
                        Text("A small date idea for the two of you.").font(.system(.headline,design:.rounded))
                        ForEach(["Make a tiny picnic, even indoors.","Take a slow walk and collect a color.","Cook something neither of you has tried.","Watch the sky change for ten quiet minutes."],id:\.self) { idea in Label(idea,systemImage:"sparkle").padding(12).frame(maxWidth:.infinity,alignment:.leading).background(.quaternary,in:RoundedRectangle(cornerRadius:12)) }
                    } else if feature == .games {
                        Text("A little turn-taking game").font(.system(.headline,design:.rounded))
                        Text(gameStep % 2 == 0 ? "Your turn: name a favorite shared place." : "Their turn: what would you order there?").font(.system(.title3,design:.rounded))
                        Button("Pass the turn") { gameStep+=1 }.buttonStyle(.borderedProminent)
                    } else if feature == .longDistance {
                        Text("Send a little signal when you’re thinking of them.").font(.system(.headline,design:.rounded))
                        HStack { ForEach(["💌","☀️","🫂","🌙"],id:\.self) { emoji in Button(emoji) { world.add("\(emoji) A tiny hello, sent with love.",to:.longDistance) }.font(.largeTitle).buttonStyle(.plain) } }
                        Text("Signals are saved here. Sharing to another device can be added when a sync service is configured.").font(.footnote).foregroundStyle(.secondary)
                    } else if feature == .dreams {
                        Text("Leave a few words for the morning.").font(.system(.headline,design:.rounded))
                        TextField("A dream or a thought…",text:$draft,axis:.vertical).lineLimit(3...7).textFieldStyle(.roundedBorder)
                        Button("Save to journal") { world.add(draft,to:.dreams); draft="" }.buttonStyle(.borderedProminent)
                    } else {
                        Text(feature == .memories ? "Keep the small things close." : "Leave a little note for your person.").font(.system(.headline,design:.rounded))
                        TextField(feature == .memories ? "What do you want to remember?" : "Write a tiny love note…",text:$draft,axis:.vertical).lineLimit(3...7).textFieldStyle(.roundedBorder)
                        Button(feature == .memories ? "Save memory" : "Tuck away this note") { world.add(draft,to:feature); draft="" }.buttonStyle(.borderedProminent)
                    }
                    if !entries.isEmpty {
                        Divider().padding(.vertical,5)
                        Text(feature == .memories ? "KEEPSAKES" : feature == .notes ? "LITTLE NOTES" : "RECENT")
                            .font(.system(.caption,design:.monospaced,weight:.bold)).tracking(1.2).foregroundStyle(.secondary)
                        ForEach(entries) { entry in VStack(alignment:.leading,spacing:5) { Text(entry.text).font(.system(.body,design:.rounded)); Text(entry.date.formatted(date:.abbreviated,time:.shortened)).font(.caption2).foregroundStyle(.secondary) }.frame(maxWidth:.infinity,alignment:.leading).padding(13).background(.quaternary,in:RoundedRectangle(cornerRadius:14)) }
                    }
                }.padding(20)
            }
            .navigationTitle(feature.rawValue).navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement:.topBarTrailing) { Button("Done") { dismiss() } } }
        }.presentationDetents([.medium,.large]).presentationDragIndicator(.visible)
    }
}

private struct SettingsSheet: View {
    @ObservedObject var world: TinyWorld
    @State private var nameOne = ""
    @State private var nameTwo = ""
    @State private var date = Date()
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            Form {
                Section("Your little family") {
                    TextField("Your name",text:$nameOne)
                    TextField("Your person's name",text:$nameTwo)
                    DatePicker("Special day",selection:$date,in:...Date(),displayedComponents:.date)
                }
                Section("Sound & atmosphere") {
                    Toggle("World music and sounds",isOn:$world.save.soundOn)
            Picker("Weather",selection:$world.save.weather) { ForEach(TinyWeather.allCases) { Text($0.label).tag($0) } }
                }
                Section("Gentle reminders") {
                    Toggle("Daily tiny moment reminder",isOn:Binding(get:{world.save.notificationsOn},set:{ value in Task { await setReminders(value) } }))
                    Text("Optional, once a day at 7 pm. Notifications stay on this device.").font(.footnote).foregroundStyle(.secondary)
                }
                Section { Text("Your world, notes, and memories are stored on this device. The core experience works offline.").font(.footnote).foregroundStyle(.secondary) }
            }
            .navigationTitle("Settings").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement:.topBarLeading) { Button("Cancel") { dismiss() } }; ToolbarItem(placement:.topBarTrailing) { Button("Save") { save(); dismiss() } } }
            .onAppear { nameOne=world.save.nameOne; nameTwo=world.save.nameTwo; date=world.save.anniversary }
            .onChange(of: world.save.weather) { _ in world.save.weatherChangedAt=Date() }
        }
    }
    private func save() {
        world.save.nameOne=nameOne.trimmingCharacters(in:.whitespaces).isEmpty ? "You" : nameOne
        world.save.nameTwo=nameTwo.trimmingCharacters(in:.whitespaces).isEmpty ? "Your person" : nameTwo
        world.save.anniversary=date; world.persist()
    }
    @MainActor private func setReminders(_ enabled: Bool) async {
        if !enabled {
            UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers:["tiny-us.daily-moment"])
            world.save.notificationsOn=false
            return
        }
        do {
            let allowed=try await UNUserNotificationCenter.current().requestAuthorization(options:[.alert,.sound,.badge])
            guard allowed else { world.save.notificationsOn=false; world.showToast("Reminders are off in iPhone Settings"); return }
            let content=UNMutableNotificationContent(); content.title="A little moment for the two of you"; content.body=world.save.dailyPrompt; content.sound = .default
            var components=DateComponents(); components.hour=19; components.minute=0
            let request=UNNotificationRequest(identifier:"tiny-us.daily-moment",content:content,trigger:UNCalendarNotificationTrigger(dateMatching:components,repeats:true))
            try await UNUserNotificationCenter.current().add(request); world.save.notificationsOn=true
        } catch { world.save.notificationsOn=false; world.showToast("Couldn’t schedule the reminder") }
    }
}
