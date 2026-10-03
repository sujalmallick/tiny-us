import SwiftUI
import Shared
import AVFoundation
import UserNotifications
import WidgetKit
import PhotosUI
import UIKit

enum TinyScene: String, CaseIterable, Codable, Identifiable {
    case meadow, tree, kitchen, living, nightWalk, cafe, sunroom, loft, momo, scooter, twilight, campfire
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
        case .campfire: return "Starry Campfire"
        }
    }
    /// Bridges to the shared Kotlin scene catalog so both apps describe places identically.
    var sharedScene: SceneType {
        switch self {
        case .meadow: return .flower
        case .tree: return .underTree
        case .kitchen: return .cooking
        case .living: return .sleep
        case .nightWalk: return .walk
        case .cafe: return .rainyCafe
        case .sunroom: return .sunroom
        case .loft: return .cozyLoft
        case .momo: return .momoStall
        case .scooter: return .eveningRide
        case .twilight: return .looking
        case .campfire: return .campfire
        }
    }
    var subtitle: String { sharedScene.subtitle }
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
        case .campfire: return "flame.fill"
        }
    }
    var isIndoor: Bool { [.kitchen, .living, .cafe, .sunroom, .loft].contains(self) }
}

enum TinyWeather: String, CaseIterable, Codable, Identifiable {
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

struct TinySong: Identifiable, Hashable {
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

@MainActor final class TinyAudio: ObservableObject {
    static let shared = TinyAudio()
    @Published private(set) var currentSong: TinySong?
    @Published private(set) var isMusicPlaying = false
    private var weatherPlayer: AVAudioPlayer?
    private var musicPlayer: AVAudioPlayer?
    private var chimePlayer: AVAudioPlayer?
    private var isChimePlaying = false
    private var oldWeatherPlayers: [AVAudioPlayer] = []
    private var weather: TinyWeather = .sunny
    private var currentAmbience: TinyWeather?
    private var isIndoor = false
    private var soundEnabled = true
    private var masterVolume: Float = 0.72
    private var isForeground = true
    private var musicWasPlayingBeforeBackground = false
    private var weatherWasPlayingBeforeBackground = false
    private var musicWasPlayingBeforeInterruption = false
    private var weatherWasPlayingBeforeInterruption = false
    private var chimeWasPlayingBeforeInterruption = false
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

    func update(weather: TinyWeather, indoor: Bool, soundEnabled: Bool, volume: Double = 0.72) {
        self.weather = weather
        self.isIndoor = indoor
        self.soundEnabled = soundEnabled
        masterVolume = Float(min(max(volume,0),1))
        guard soundEnabled else { stopAll(); return }
        guard isForeground else { return }
        configureSession()
        let filename = "bgm_\(weather.rawValue)"
        if let current = weatherPlayer, current.url?.deletingPathExtension().lastPathComponent == filename || (current.url == nil && currentAmbience == weather) {
            weatherPlayer?.setVolume(weatherVolume, fadeDuration: 0.35)
            if weatherPlayer?.isPlaying == false { weatherPlayer?.play() }
            return
        }
        let bundled = Bundle.main.url(forResource: filename, withExtension: "mp3").flatMap { try? AVAudioPlayer(contentsOf: $0) }
        guard let next = bundled ?? (try? AVAudioPlayer(data: TinyWav.encode(TinySynth.ambience(rain: weather == .rain)))) else { return }
        next.numberOfLoops = -1
        next.volume = 0
        next.prepareToPlay()
        let previous = weatherPlayer
        weatherPlayer = next
        currentAmbience = weather
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
        masterVolume * (isIndoor ? 0.4 : 1.0) * ((isMusicPlaying || isChimePlaying) ? 0.25 : 1.0)
    }
    private func refreshWeather() {
        update(weather:weather, indoor:isIndoor, soundEnabled:soundEnabled, volume:Double(masterVolume))
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
            refreshWeather()
            return
        }
        guard let url = Bundle.main.url(forResource: song.resource, withExtension: "mp3"),
              let next = try? AVAudioPlayer(contentsOf: url) else { return }
        configureSession()
        musicPlayer?.stop()
        next.numberOfLoops = 0
        next.volume = 0.9 * masterVolume
        next.prepareToPlay()
        musicPlayer = next
        currentSong = song
        isMusicPlaying = next.play()
        refreshWeather()
        let duration = next.duration
        DispatchQueue.main.asyncAfter(deadline: .now() + duration + 0.2) { [weak self, weak next] in
            guard let self, let next, self.musicPlayer === next, !next.isPlaying else { return }
            self.isMusicPlaying = false
            self.refreshWeather()
        }
    }

    func toggleMusic() {
        guard let musicPlayer, soundEnabled else { return }
        if musicPlayer.isPlaying { musicPlayer.pause(); isMusicPlaying = false }
        else { configureSession(); musicPlayer.play(); isMusicPlaying = true }
        refreshWeather()
    }

    func playChime(_ data: Data) {
        guard soundEnabled, isForeground else { return }
        configureSession()
        guard let player = try? AVAudioPlayer(data: data) else { return }
        chimePlayer?.stop()
        chimePlayer = player
        isChimePlaying = true
        player.volume = 0.8 * masterVolume
        player.prepareToPlay()
        player.play()
        refreshWeather()
        DispatchQueue.main.asyncAfter(deadline: .now() + player.duration + 0.1) { [weak self, weak player] in
            guard let self, let player, self.chimePlayer === player else { return }
            self.isChimePlaying = false
            self.refreshWeather()
        }
    }

    func pauseForBackground() {
        isForeground = false
        musicWasPlayingBeforeBackground = musicPlayer?.isPlaying == true
        weatherWasPlayingBeforeBackground = weatherPlayer?.isPlaying == true
        weatherPlayer?.pause()
        musicPlayer?.pause()
        chimePlayer?.stop(); chimePlayer = nil; isChimePlaying = false
        isMusicPlaying = false
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
    }

    func resumeFromBackground() {
        isForeground = true
        guard soundEnabled else { return }
        configureSession()
        refreshWeather()
        if musicWasPlayingBeforeBackground, let musicPlayer, currentSong != nil {
            musicPlayer.play(); isMusicPlaying = true
            refreshWeather()
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
            chimeWasPlayingBeforeInterruption = chimePlayer?.isPlaying == true
            musicPlayer?.pause(); weatherPlayer?.pause(); chimePlayer?.pause()
            isMusicPlaying = false; isChimePlaying = false
        } else {
            guard let rawOptions = notification.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt,
                  AVAudioSession.InterruptionOptions(rawValue: rawOptions).contains(.shouldResume),
                  soundEnabled, isForeground else { return }
            configureSession()
            if weatherWasPlayingBeforeInterruption { weatherPlayer?.play() }
            if musicWasPlayingBeforeInterruption { musicPlayer?.play(); isMusicPlaying = true }
            if chimeWasPlayingBeforeInterruption { chimePlayer?.play(); isChimePlaying = true }
            refreshWeather()
            musicWasPlayingBeforeInterruption = false
            weatherWasPlayingBeforeInterruption = false
            chimeWasPlayingBeforeInterruption = false
        }
    }

    private func stopAll() {
        weatherPlayer?.stop(); weatherPlayer = nil
        musicPlayer?.stop(); musicPlayer = nil
        chimePlayer?.stop(); chimePlayer = nil; isChimePlaying = false
        oldWeatherPlayers.forEach { $0.stop() }
        oldWeatherPlayers.removeAll()
        currentSong = nil
        isMusicPlaying = false
        musicWasPlayingBeforeBackground = false
        weatherWasPlayingBeforeBackground = false
        musicWasPlayingBeforeInterruption = false
        weatherWasPlayingBeforeInterruption = false
        chimeWasPlayingBeforeInterruption = false
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
    }
}

enum TinyFeature: String, CaseIterable, Identifiable {
    case musicBox = "Music Box", moments = "Daily Moment", memories = "Our Memories", notes = "Love Notes"
    case dreams = "Dream Journal", garden = "Little Garden", wardrobe = "Wardrobe"
    case adventures = "Date Adventures", games = "Couple Arcade", mood = "Shared Mood", calendar = "Special Calendar"
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
        case .calendar: return "calendar"
        }
    }
}

struct TinyEntry: Codable, Identifiable {
    var id = UUID()
    var text: String
    var date = Date()
    var imageFilename: String?
    private enum CodingKeys: String, CodingKey { case id, text, date, imageFilename }
    init(id: UUID = UUID(), text: String, date: Date = Date(), imageFilename: String? = nil) { self.id=id; self.text=text; self.date=date; self.imageFilename=imageFilename }
    init(from decoder: Decoder) throws {
        let values=try decoder.container(keyedBy:CodingKeys.self)
        id=(try? values.decode(UUID.self,forKey:.id)) ?? UUID()
        text=(try? values.decode(String.self,forKey:.text)) ?? ""
        date=(try? values.decode(Date.self,forKey:.date)) ?? Date()
        imageFilename=try? values.decode(String.self,forKey:.imageFilename)
    }
}

struct TinySave: Codable {
    var nameOne = "You"
    var nameTwo = "Your person"
    var anniversary = Calendar.current.startOfDay(for: Date())
    var scene: TinyScene = .meadow
    var weather: TinyWeather = .sunny
    var previousWeather: TinyWeather?
    var weatherChangedAt = Date()
    var weatherDriftStartedAt = Date()
    var entries: [String: [TinyEntry]] = [:]
    var gardenSeeds = 3
    var outfit = 0
    var homeLevel = 1
    var homeKeepsakes: Set<String> = []
    var plantsGrowing = 0
    var mood = "🥰"
    var dailyPromptDate = ""
    var dailyPrompt = "What tiny thing made you smile today?"
    var soundOn = true
    var soundVolume = 0.72
    var notificationsOn = false
    var completedAdventureIDs: Set<String> = []
    var miniGameIndex = 0
    var miniGameTurn = 0
    var miniGameFirstChoice: Int?
    var miniGameSecondChoice: Int?
    var lastWorldMoment = ""
    var mochiPats = 0
    var mochiCollar = 0
    var recentScenes: [TinyScene] = []
    var specialDates: [TinySpecialDate] = []
    /// Two-partner reflections keyed by yyyy-MM-dd: [first partner, second partner].
    var momentAnswers: [String: [String]] = [:]
    /// Tic-tac-toe tally: [hearts wins, stars wins, draws].
    var ticTacToeScore: [Int] = [0, 0, 0]
    var memoryBestMoves: Int?

    private enum CodingKeys: String, CodingKey {
        case nameOne, nameTwo, anniversary, scene, weather, previousWeather, weatherChangedAt, weatherDriftStartedAt, entries, gardenSeeds
        case outfit, homeLevel, homeKeepsakes, plantsGrowing, mood, dailyPromptDate, dailyPrompt, soundOn, soundVolume, notificationsOn
        case completedAdventureIDs, miniGameIndex, miniGameTurn, miniGameFirstChoice, miniGameSecondChoice, lastWorldMoment, mochiPats
        case mochiCollar, recentScenes, specialDates, momentAnswers, ticTacToeScore, memoryBestMoves
    }

    init() { }

    init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        nameOne = (try? values.decode(String.self, forKey: .nameOne)) ?? "You"
        nameTwo = (try? values.decode(String.self, forKey: .nameTwo)) ?? "Your person"
        anniversary = (try? values.decode(Date.self, forKey: .anniversary)) ?? Calendar.current.startOfDay(for: Date())
        scene = (try? values.decode(TinyScene.self, forKey: .scene)) ?? .meadow
        weather = (try? values.decode(TinyWeather.self, forKey: .weather)) ?? .sunny
        previousWeather = try? values.decode(TinyWeather.self, forKey: .previousWeather)
        weatherChangedAt = (try? values.decode(Date.self, forKey: .weatherChangedAt)) ?? Date()
        weatherDriftStartedAt = (try? values.decode(Date.self, forKey: .weatherDriftStartedAt)) ?? Date()
        entries = (try? values.decode([String: [TinyEntry]].self, forKey: .entries))?.mapValues { $0.filter { !$0.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || $0.imageFilename != nil } } ?? [:]
        gardenSeeds = (try? values.decode(Int.self, forKey: .gardenSeeds)) ?? 3
        outfit = (try? values.decode(Int.self, forKey: .outfit)) ?? 0
        homeLevel = (try? values.decode(Int.self, forKey: .homeLevel)) ?? 1
        homeKeepsakes = (try? values.decode(Set<String>.self, forKey: .homeKeepsakes)) ?? []
        plantsGrowing = (try? values.decode(Int.self, forKey: .plantsGrowing)) ?? 0
        mood = (try? values.decode(String.self, forKey: .mood)) ?? "🥰"
        dailyPromptDate = (try? values.decode(String.self, forKey: .dailyPromptDate)) ?? ""
        dailyPrompt = (try? values.decode(String.self, forKey: .dailyPrompt)) ?? "What tiny thing made you smile today?"
        soundOn = (try? values.decode(Bool.self, forKey: .soundOn)) ?? true
        soundVolume = (try? values.decode(Double.self, forKey: .soundVolume)) ?? 0.72
        notificationsOn = (try? values.decode(Bool.self, forKey: .notificationsOn)) ?? false
        completedAdventureIDs = (try? values.decode(Set<String>.self, forKey: .completedAdventureIDs)) ?? []
        miniGameIndex = (try? values.decode(Int.self, forKey: .miniGameIndex)) ?? 0
        miniGameTurn = (try? values.decode(Int.self, forKey: .miniGameTurn)) ?? 0
        miniGameFirstChoice = try? values.decode(Int.self, forKey: .miniGameFirstChoice)
        miniGameSecondChoice = try? values.decode(Int.self, forKey: .miniGameSecondChoice)
        lastWorldMoment = (try? values.decode(String.self, forKey: .lastWorldMoment)) ?? ""
        mochiPats = (try? values.decode(Int.self, forKey: .mochiPats)) ?? 0
        mochiCollar = (try? values.decode(Int.self, forKey: .mochiCollar)) ?? 0
        recentScenes = (try? values.decode([TinyScene].self, forKey: .recentScenes)) ?? []
        specialDates = (try? values.decode([TinySpecialDate].self, forKey: .specialDates)) ?? []
        momentAnswers = (try? values.decode([String: [String]].self, forKey: .momentAnswers)) ?? [:]
        let score = (try? values.decode([Int].self, forKey: .ticTacToeScore)) ?? []
        ticTacToeScore = score.count == 3 ? score : [0, 0, 0]
        memoryBestMoves = try? values.decode(Int.self, forKey: .memoryBestMoves)
    }
}

@MainActor final class TinyWorld: ObservableObject {
    @Published var save: TinySave { didSet { persist() } }
    @Published var showOnboarding = false
    @Published var activeFeature: TinyFeature?
    @Published var toast: String?
    private let key = "tiny-us.ios.world.v1"
    init() {
        if let data = UserDefaults.standard.data(forKey: key) {
            if let restored = try? JSONDecoder().decode(TinySave.self, from: data) {
                save = restored
                showOnboarding = restored.nameOne == "You" && restored.nameTwo == "Your person"
            } else {
                let backup = "tiny-us.ios.world.recovery.\(Int(Date().timeIntervalSince1970))"
                UserDefaults.standard.set(data, forKey: backup)
                save = TinySave()
                showOnboarding = true
            }
        } else {
            save = TinySave()
            showOnboarding = true
        }
        refreshPromptIfNeeded()
    }
    func persist() {
        guard let data = try? JSONEncoder().encode(save) else { return }
        UserDefaults.standard.set(data, forKey: key)
        let sharedStorage = IosUserDefaultsStorage(defaults: TinyAppGroup.defaults)
        sharedStorage.putString(key: "bf_name", value: save.nameOne)
        sharedStorage.putString(key: "gf_name", value: save.nameTwo)
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        sharedStorage.putString(key: "anniversary_date", value: formatter.string(from: save.anniversary))
        publishWidgetSnapshot()
    }
    private func publishWidgetSnapshot() {
        let calendar = Calendar.current
        let days = relationshipDay
        let phase = TimeOfDayPhaseKt.currentPhase(hour: Int32(calendar.component(.hour, from: Date()))).displayName
        let signal = save.entries[TinyFeature.longDistance.rawValue, default: []].first?.text ?? "A little hello from your world."
        let startOfToday=calendar.startOfDay(for:Date())
        _ = startOfToday
        let answeredToday=save.momentAnswers[TinyWorld.dayKey(Date()), default: []].contains { !$0.isEmpty }
        let model = TinyUsWidgetData(
            coupleNames: "\(save.nameOne) ♥ \(save.nameTwo)", daysTogether: days,
            sceneName: save.scene.title, weatherName: save.weather.label, timePhase: phase,
            dailyMomentPrompt: save.dailyPrompt, dailyMomentAnswered: answeredToday,
            latestSignalText: signal, sharedMoodEmoji: save.mood,
            sharedMoodText: "Your shared mood", lastUpdatedTimestamp: Int64(Date().timeIntervalSince1970 * 1000))
        let payload = TinyWidgetPayload(
            coupleNames: model.coupleNames, daysTogether: model.daysTogether, anniversaryDate: save.anniversary,
            sceneName: model.sceneName, weatherName: model.weatherName, timePhase: model.timePhase,
            dailyMomentPrompt: model.dailyMomentPrompt ?? "A little moment together",
            sharedMoodEmoji: model.sharedMoodEmoji ?? "💛",
            latestSignalText: model.latestSignalText ?? "A little hello from your world.", updatedAt: Date())
        guard let encoded = try? JSONEncoder().encode(payload) else { return }
        TinyAppGroup.defaults.set(encoded, forKey: TinyAppGroup.payloadKey)
        WidgetCenter.shared.reloadTimelines(ofKind: TinyAppGroup.widgetKind)
    }
    func add(_ text: String, to feature: TinyFeature, imageData: Data? = nil) {
        let clean = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty || imageData != nil else { return }
        let imageFilename = feature == .memories ? saveMemoryImage(imageData) : nil
        if clean.isEmpty && imageData != nil && imageFilename == nil { showToast("That photo couldn’t be saved. Try a smaller image."); return }
        save.entries[feature.rawValue, default: []].insert(TinyEntry(text: clean, imageFilename: imageFilename), at: 0)
        if feature == .memories { save.homeKeepsakes.insert("A framed memory") }
        if feature == .notes { save.homeKeepsakes.insert("A note on the fridge") }
        showToast("Tucked safely into \(feature.rawValue.lowercased())")
    }
    private func saveMemoryImage(_ data: Data?) -> String? {
        guard let data,
              let base = FileManager.default.urls(for:.applicationSupportDirectory,in:.userDomainMask).first,
              let image=UIImage(data:data) else { return nil }
        let folder=base.appendingPathComponent("TinyUsMemories",isDirectory:true)
        let factor=min(1,1200/max(image.size.width,image.size.height))
        let target=CGSize(width:image.size.width*factor,height:image.size.height*factor)
        let renderer=UIGraphicsImageRenderer(size:target)
        let optimized=renderer.image { _ in image.draw(in:CGRect(origin:.zero,size:target)) }
        guard let jpeg=optimized.jpegData(compressionQuality:0.82) else { return nil }
        let name="\(UUID().uuidString).jpg"
        do {
            try FileManager.default.createDirectory(at:folder,withIntermediateDirectories:true)
            try jpeg.write(to:folder.appendingPathComponent(name),options:[.atomic,.completeFileProtectionUntilFirstUserAuthentication])
            return name
        } catch { return nil }
    }
    func removeMemory(_ entry: TinyEntry) {
        save.entries[TinyFeature.memories.rawValue]?.removeAll { $0.id == entry.id }
        if let name = entry.imageFilename, !name.contains("/"), !name.contains("\\"),
           let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first {
            try? FileManager.default.removeItem(at: base.appendingPathComponent("TinyUsMemories", isDirectory: true).appendingPathComponent(name))
        }
        showToast("Memory removed")
    }
    func memoryImage(named name: String) -> UIImage? {
        guard !name.contains("/"), !name.contains("\\"),
              let base=FileManager.default.urls(for:.applicationSupportDirectory,in:.userDomainMask).first else { return nil }
        let url=base.appendingPathComponent("TinyUsMemories",isDirectory:true).appendingPathComponent(name)
        guard let data=try? Data(contentsOf:url) else { return nil }
        return UIImage(data:data)
    }
    func setWeather(_ weather: TinyWeather, at date: Date = Date()) {
        guard weather != save.weather else { return }
        save.previousWeather = save.weather
        save.weatherChangedAt = date
        save.weatherDriftStartedAt = date
        save.weather = weather
    }
    func resumeWeatherDriftClock(at date: Date = Date()) { save.weatherDriftStartedAt = date }
    func interactWithScene() {
        let moments: [String]
        switch save.scene {
        case .meadow: moments=["A flower tucked behind an ear 🌼","They make a tiny daisy chain 🌸","A shy little hand finds another 💛"]
        case .tree: moments=["A leaf lands softly in their hair 🍃","They share the shady patch beneath their tree 🌳","One tells the other a secret under the branches 🤫"]
        case .kitchen: moments=["A stolen taste, then a giggle 🥄","They make room for one more pinch of cinnamon 🍪","A warm mug is passed across the counter ☕️"]
        case .living: moments=["A blanket is tucked around them both 🧺","They settle into a sleepy shoulder cuddle 💤","Mochi gets the soft middle cushion 🐾"]
        case .nightWalk: moments=["They pause to watch the lanterns sway 🏮","A mittened hand reaches for the other 🧤","They make a wish on the first star ✨"]
        case .cafe: moments=["Two warm cups meet at the window ☕️","They draw a tiny heart in the fogged glass 💗","A pastry is quietly split in half 🥐"]
        case .sunroom: moments=["A little plant gets a careful drink 🌱","They turn a leaf toward the afternoon sun 🌿","A new sprout earns a very proud smile 🌼"]
        case .loft: moments=["They pick a song for the next slow dance 🎶","A sleepy head finds a warm shoulder 🌙","The city lights become a tiny constellation ✨"]
        case .momo: moments=["One last dumpling is split exactly in two 🥟","They blow on each other's too-hot tea 🍵","The cook sneaks them an extra bite 🥢"]
        case .scooter: moments=["They stop to feel the evening breeze 🛵","A scarf flutters between two happy waves 🧣","They take the long way home 🌆"]
        case .twilight: moments=["A gentle head pat says everything 💛","They share a quiet look and a little smile ☺️","A tiny heart appears between them 💕"]
        case .campfire: moments=["Two marshmallows, one shared stick 🍡","Sparks drift up to meet the stars ✨","A plaid blanket around both shoulders 🔥"]
        }
        let options=moments.filter { $0 != save.lastWorldMoment }
        let moment=options.randomElement() ?? moments[0]
        save.lastWorldMoment=moment
        showToast(moment)
    }
    static func dayKey(_ date: Date) -> String {
        let c = Calendar.current.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }
    /// "Day N" of Tiny Us, inclusive of the anniversary itself (shared Kotlin calculator).
    var relationshipDay: Int64 { TinyWorld.relationshipDay(since: save.anniversary) }
    static func relationshipDay(since anniversary: Date, on date: Date = Date()) -> Int64 {
        let cal = Calendar.current
        let a = cal.dateComponents([.year, .month, .day], from: anniversary)
        let b = cal.dateComponents([.year, .month, .day], from: date)
        return RelationshipTimeCalculator.shared.calculateDays(
            startYear: Int32(a.year ?? 2024), startMonth: Int32(a.month ?? 1), startDay: Int32(a.day ?? 1),
            currentYear: Int32(b.year ?? 2026), currentMonth: Int32(b.month ?? 1), currentDay: Int32(b.day ?? 1))
    }
    func select(_ scene: TinyScene) {
        guard scene != save.scene else { return }
        save.recentScenes = Array(([save.scene] + save.recentScenes.filter { $0 != save.scene }).prefix(3))
        save.scene = scene
    }
    /// Random scene that skips the current place and the last three visited, like Android's "Surprise Me".
    func surpriseScene() {
        let fresh = TinyScene.allCases.filter { $0 != save.scene && !save.recentScenes.contains($0) }
        let pick = (fresh.isEmpty ? TinyScene.allCases.filter { $0 != save.scene } : fresh).randomElement() ?? .meadow
        select(pick)
        showToast("Surprise! \(pick.title) ✨")
    }
    func fridgeNote() -> String {
        if let note = save.entries[TinyFeature.notes.rawValue]?.randomElement()?.text, !note.isEmpty { return note }
        return ["You make ordinary days feel special.", "Tea later? I will bring the biscuits.", "Thank you for being you.", "Cannot wait to see you tonight 💕"].randomElement() ?? "💕"
    }
    /// Renders the current scene as a Polaroid and tucks it into Our Memories.
    func capturePolaroid() {
        let image = TinyPolaroid.render(scene: save.scene, weather: save.weather, outfit: save.outfit, collar: save.mochiCollar)
        guard let data = image?.jpegData(compressionQuality: 0.9) else { showToast("Couldn’t capture that moment"); return }
        TinySoundBoard.shared.play(.cardFlip)
        add("📸 \(save.scene.title) • \(Date().formatted(date: .abbreviated, time: .omitted))", to: .memories, imageData: data)
    }
    func interactWithCharacters() {
        let moments=["They lean together for a quiet second 💛","One offers a tiny hand to the other 🫶","Their little people share a shy smile 😊"]
        let choices=moments.filter { $0 != save.lastWorldMoment }
        let moment=choices.randomElement() ?? moments[0]
        save.lastWorldMoment=moment
        showToast(moment)
    }
    private var toastToken = 0
    func showToast(_ value: String) {
        toast = value
        toastToken &+= 1
        let token = toastToken
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.6) { [weak self] in
            guard let self, self.toastToken == token else { return }
            self.toast = nil
        }
    }
    func refreshPromptIfNeeded() {
        let stamp = Calendar.current.startOfDay(for: Date()).formatted(date: .numeric, time: .omitted)
        guard save.dailyPromptDate != stamp else { return }
        let sharedPrompt = DailyPromptCatalog.shared.getPromptForDay(dayIndex: Int32(truncatingIfNeeded: relationshipDay))
        save.dailyPrompt = sharedPrompt.question
        save.dailyPromptDate = stamp
    }
    func playMusicBox() {
        TinyAudio.shared.playChime(TinyWav.encode(TinySynth.render(.musicBox)))
    }
}

struct ContentView: View {
    @StateObject private var world = TinyWorld()
    @StateObject private var stage = TinyStage()
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.scenePhase) private var scenePhase
    @ObservedObject private var audio = TinyAudio.shared
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
                    WorldCanvas(world: world, stage: stage, reducedMotion: reduceMotion)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                        .padding(.horizontal, 14)
                        .overlay(alignment: .bottom) { worldCaption.padding(.bottom, 16) }
                        .overlay(alignment:.bottomTrailing) {
                            Button { stage.petMochi(world: world, now: TinyStage.now) } label: {
                                Image(systemName:"pawprint.fill").font(.system(size:14,weight:.semibold)).foregroundStyle(Color(red:1,green:0.84,blue:0.61))
                                    .frame(width:38,height:38).background(.black.opacity(0.35),in:Circle())
                            }.buttonStyle(.plain).accessibilityLabel("Give Mochi a gentle head pat")
                                .padding(.trailing,25).padding(.bottom,63)
                        }
                        .overlay(alignment:.topTrailing) {
                            Button { world.capturePolaroid() } label: {
                                Image(systemName:"camera.fill").font(.system(size:13,weight:.semibold)).foregroundStyle(Color(red:1,green:0.84,blue:0.61))
                                    .frame(width:34,height:34).background(.black.opacity(0.35),in:Circle())
                            }.buttonStyle(.plain).accessibilityLabel("Capture this moment as a Polaroid")
                                .padding(.trailing,24).padding(.top,10)
                        }
                        .simultaneousGesture(DragGesture(minimumDistance: 30).onEnded { value in
                            if abs(value.translation.width) > abs(value.translation.height) {
                                changeScene(step: value.translation.width < 0 ? 1 : -1)
                            } else if value.translation.height < 0 { showScenes = true }
                        })
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
            .sheet(isPresented: $showScenes) { ScenePickerSheet(world: world) }
            .sheet(isPresented: $showSettings) { SettingsSheet(world: world) }
            .sheet(isPresented: $showFeatures) { FeaturePickerSheet(world: world) }
            .sheet(item: $world.activeFeature) { feature in featureView(feature) }
            .fullScreenCover(isPresented: $world.showOnboarding) { OnboardingSheet(world: world) }
            .onReceive(clock) { date in
                tick = date
                if date.timeIntervalSince(world.save.weatherDriftStartedAt) >= 360 {
                    let choices = TinyWeather.allCases.filter { $0 != world.save.weather }
                    world.setWeather(choices.randomElement() ?? .sunny, at: date)
                }
            }
            .onAppear { updateAudio(); world.resumeWeatherDriftClock() }
            .onChange(of: world.save.weather) { _ in updateAudio() }
            .onChange(of: world.save.scene) { _ in updateAudio() }
            .onChange(of: world.save.soundOn) { _ in updateAudio() }
            .onChange(of: world.save.soundVolume) { _ in updateAudio() }
            .onChange(of: scenePhase) { phase in
                if phase == .background { audio.pauseForBackground(); world.persist() }
                if phase == .active { audio.resumeFromBackground(); world.resumeWeatherDriftClock() }
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
                Label("Day \(world.relationshipDay)", systemImage: "heart.fill")
                    .font(.system(.caption, design: .rounded, weight: .bold))
                    .foregroundStyle(Color(red: 1, green: 0.75, blue: 0.72))
                    .padding(.horizontal, 11).padding(.vertical, 8).background(.white.opacity(0.08), in: Capsule())
            }.accessibilityLabel("Day \(world.relationshipDay) together")
            Button { showSettings = true } label: {
                Image(systemName: "slider.horizontal.3").font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.9)).frame(width: 38, height: 38).background(.white.opacity(0.08), in: Circle())
            }.accessibilityLabel("Settings")
        }.padding(.horizontal, 20).padding(.bottom, 12)
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
        world.select(TinyScene.allCases[new])
        world.showToast(world.save.scene.title)
    }
    private func updateAudio() {
        audio.update(weather: world.save.weather, indoor: world.save.scene.isIndoor, soundEnabled: world.save.soundOn, volume: world.save.soundVolume)
        TinySoundBoard.shared.configure(enabled: world.save.soundOn, volume: world.save.soundVolume)
    }
    @ViewBuilder private func featureView(_ feature: TinyFeature) -> some View {
        switch feature {
        case .moments: DailyMomentSheet(world: world)
        case .games: ArcadeSheet(world: world)
        case .calendar: SpecialCalendarSheet(world: world)
        case .memories: KeepsakeWallSheet(world: world)
        default: FeatureSheet(feature: feature, world: world)
        }
    }

    private var weatherSheet: some View {
        NavigationStack {
            List(TinyWeather.allCases) { weather in
                Button { world.setWeather(weather); showWeather = false; world.showToast("The sky is changing gently") } label: {
                    Label(weather.label, systemImage: weather.icon).foregroundStyle(.primary)
                        .overlay(alignment: .trailing) { if weather == world.save.weather { Image(systemName: "checkmark").foregroundStyle(.tint) } }
                }
            }.navigationTitle("A little weather").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Done") { showWeather = false } } }
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

/// Interactive pixel world: a 30 fps Canvas driven by TinyWorldPainter, with taps routed through TinyStage.
private struct WorldCanvas: View {
    @ObservedObject var world: TinyWorld
    @ObservedObject var stage: TinyStage
    let reducedMotion: Bool

    var body: some View {
        GeometryReader { proxy in
            TimelineView(.animation(minimumInterval: 1.0 / 30.0, paused: reducedMotion)) { context in
                let now = TinyStage.now
                let ambient = reducedMotion ? 0 : context.date.timeIntervalSinceReferenceDate
                let blend = reducedMotion ? 1 : min(1, max(0, (now - world.save.weatherChangedAt.timeIntervalSinceReferenceDate) / 1.6))
                let painter = TinyWorldPainter(scene: world.save.scene, weather: world.save.weather, previousWeather: world.save.previousWeather,
                                               weatherBlend: blend, outfit: world.save.outfit, collar: world.save.mochiCollar,
                                               hour: Calendar.current.component(.hour, from: context.date), t: ambient, now: now,
                                               stage: stage.snapshot(scene: world.save.scene, at: now))
                Canvas(opaque: true) { graphics, size in painter.paint(&graphics, size: size) }
            }
            .contentShape(Rectangle())
            .gesture(SpatialTapGesture().onEnded { value in stage.handleTap(at: value.location, size: proxy.size, world: world) })
            .onLongPressGesture(minimumDuration: 0.45) { stage.handleLongPress(world: world) }
        }
        .onAppear { stage.enter(world.save.scene) }
        .onChange(of: world.save.scene) { scene in stage.enter(scene) }
        .accessibilityElement()
        .accessibilityLabel("Animated pixel-art scene: \(world.save.scene.title), \(world.save.weather.label)")
        .accessibilityHint("Tap objects, the sky, Mochi, or the two of you. Double-tap them for a joy jump, or press and hold for a hug.")
        .accessibilityAddTraits(.allowsDirectInteraction)
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
    @Environment(\.dismiss) private var dismiss
    private var entries: [TinyEntry] { world.save.entries[feature.rawValue, default: []] }
    private var sharedAdventures: [DateAdventure] { DateAdventureCatalog.shared.defaultAdventures }
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
                    } else if feature == .timer {
                        Label("\(world.save.nameOne) & \(world.save.nameTwo)",systemImage:"heart.fill").font(.system(.title2,design:.rounded,weight:.bold)).foregroundStyle(.pink)
                        Text("Day \(world.relationshipDay) of little and big moments together.").font(.system(.body,design:.rounded))
                        Text("Since \(world.save.anniversary.formatted(date:.long,time:.omitted))").foregroundStyle(.secondary)
                    } else if feature == .mood {
                        Text("How are you feeling about us today?").font(.system(.headline,design:.rounded))
                        HStack { ForEach(["🥰","😊","🌱","🫶","🌙"],id:\.self) { emoji in Button(emoji) { world.save.mood=emoji; world.showToast("Your shared mood is \(emoji)") }.font(.largeTitle).buttonStyle(.plain) } }
                        Text("Your mood is kept privately on this device.").font(.footnote).foregroundStyle(.secondary)
                    } else if feature == .garden {
                        Text("A little corner to grow together.").font(.system(.headline,design:.rounded))
                        Text("\(String(repeating:"🌿",count:min(world.save.plantsGrowing,12)))").font(.title2)
                        Text("🌱  \(world.save.gardenSeeds) seeds waiting to bloom").font(.system(.title3,design:.rounded))
                        Button("Plant a seed") { world.save.gardenSeeds=max(0,world.save.gardenSeeds-1); world.save.plantsGrowing+=1; world.save.homeKeepsakes.insert("A windowsill plant"); world.showToast("A new little sprout is growing 🌱") }.buttonStyle(.borderedProminent).disabled(world.save.gardenSeeds==0)
                    } else if feature == .wardrobe {
                        Text("Choose a cozy outfit for your tiny people.").font(.system(.headline,design:.rounded))
                        ForEach(Array(["Soft Sunday","Picnic gingham","Rainy day knits","Starlight pajamas"].enumerated()),id:\.offset) { index,title in
                            Button { world.save.outfit=index; world.showToast("Outfit changed to \(title)") } label: { HStack { Text(["🧺","🌼","☔️","🌙"][index]); Text(title); Spacer(); if index==world.save.outfit { Image(systemName:"checkmark.circle.fill") } }.padding(12).background(.quaternary,in:RoundedRectangle(cornerRadius:12)) }.buttonStyle(.plain)
                        }
                        Text("Mochi’s collar").font(.system(.headline,design:.rounded)).padding(.top,6)
                        ForEach(Array(["Tiny red bowtie","Daisy collar","Blue bowtie","Just Mochi"].enumerated()),id:\.offset) { index,title in
                            Button {
                                world.save.mochiCollar=index
                                world.showToast(index == 0 ? "Mochi looks dapper in a tiny red bowtie!" : index == 1 ? "A little daisy collar for Mochi!" : "Mochi approves 🐾")
                            } label: { HStack { Text(["🎀","🌼","💙","🐈"][index]); Text(title); Spacer(); if index==world.save.mochiCollar { Image(systemName:"checkmark.circle.fill") } }.padding(12).background(.quaternary,in:RoundedRectangle(cornerRadius:12)) }.buttonStyle(.plain)
                        }
                    } else if feature == .home {
                        Text("Your home gathers little reminders of the life you share.").font(.system(.headline,design:.rounded))
                        if world.save.homeKeepsakes.isEmpty { Text("A windowsill plant, a note, or your first memory will find a place here.").foregroundStyle(.secondary) }
                        ForEach(world.save.homeKeepsakes.sorted(),id:\.self) { item in Label(item,systemImage:"sparkle").padding(12).frame(maxWidth:.infinity,alignment:.leading).background(.quaternary,in:RoundedRectangle(cornerRadius:12)) }
                        Button("Tuck a keepsake onto the shelf") {
                            let options=["A cozy knit throw","A copper tea kettle","A little string of lights","An origami heart"]
                            if let item=options.first(where:{ !world.save.homeKeepsakes.contains($0) }) { world.save.homeKeepsakes.insert(item); world.showToast("\(item) found a home 🏡") }
                        }.buttonStyle(.borderedProminent).disabled(["A cozy knit throw","A copper tea kettle","A little string of lights","An origami heart"].allSatisfy(world.save.homeKeepsakes.contains))
                    } else if feature == .adventures {
                        Text("Small real-world ideas from your shared date-adventure collection.").font(.system(.headline,design:.rounded))
                        ForEach(sharedAdventures, id:\.id) { adventure in
                            VStack(alignment:.leading,spacing:9) {
                                Text(adventure.title).font(.system(.body,design:.rounded,weight:.bold))
                                Text(adventure.description).font(.system(.subheadline,design:.rounded)).foregroundStyle(.secondary)
                                Button(world.save.completedAdventureIDs.contains(adventure.id) ? "A little date completed ✓" : "We did this together") {
                                    world.save.completedAdventureIDs.insert(adventure.id)
                                    world.save.gardenSeeds += 1
                                    world.save.homeKeepsakes.insert("A picnic basket from your date")
                                    world.showToast("A new keepsake for your little home 🏡")
                                }.font(.system(.caption,design:.rounded,weight:.semibold)).buttonStyle(.bordered).disabled(world.save.completedAdventureIDs.contains(adventure.id))
                            }.padding(13).frame(maxWidth:.infinity,alignment:.leading).background(.quaternary,in:RoundedRectangle(cornerRadius:14))
                        }
                    } else if feature == .longDistance {
                        Text("Send a little signal when you’re thinking of them.").font(.system(.headline,design:.rounded))
                        HStack { ForEach(["💌","☀️","🫂","🌙"],id:\.self) { emoji in Button(emoji) { world.add("\(emoji) A tiny hello, sent with love.",to:.longDistance) }.font(.largeTitle).buttonStyle(.plain) } }
                        Text("Signals are saved here. Sharing to another device can be added when a sync service is configured.").font(.footnote).foregroundStyle(.secondary)
                    } else if feature == .dreams {
                        Text("Leave a few words for the morning.").font(.system(.headline,design:.rounded))
                        TextField("A dream or a thought…",text:$draft,axis:.vertical).lineLimit(3...7).textFieldStyle(.roundedBorder)
                        Button("Save to journal") { world.add(draft,to:.dreams); draft="" }.buttonStyle(.borderedProminent)
                    } else {
                        Text("Leave a little note for your person.").font(.system(.headline,design:.rounded))
                        TextField("Write a tiny love note…",text:$draft,axis:.vertical).lineLimit(3...7).textFieldStyle(.roundedBorder)
                        Button("Tuck away this note") { world.add(draft,to:feature); draft="" }.buttonStyle(.borderedProminent)
                    }
                    if !entries.isEmpty {
                        Divider().padding(.vertical,5)
                        Text(feature == .notes ? "LITTLE NOTES" : "RECENT")
                            .font(.system(.caption,design:.monospaced,weight:.bold)).tracking(1.2).foregroundStyle(.secondary)
                        ForEach(entries) { entry in
                            VStack(alignment:.leading,spacing:8) {
                                if !entry.text.isEmpty { Text(entry.text).font(.system(.body,design:.rounded)) }
                                Text(entry.date.formatted(date:.abbreviated,time:.shortened)).font(.caption2).foregroundStyle(.secondary)
                            }.frame(maxWidth:.infinity,alignment:.leading).padding(13).background(.quaternary,in:RoundedRectangle(cornerRadius:14))
                        }
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
                    VStack(alignment:.leading,spacing:7) {
                        HStack { Text("World volume"); Spacer(); Text("\(Int(world.save.soundVolume*100))%").foregroundStyle(.secondary) }
                        Slider(value:$world.save.soundVolume,in:0...1).tint(.pink).accessibilityLabel("World volume")
                    }
                    Picker("Weather",selection:Binding(get:{world.save.weather},set:{world.setWeather($0)})) { ForEach(TinyWeather.allCases) { Text($0.label).tag($0) } }
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
