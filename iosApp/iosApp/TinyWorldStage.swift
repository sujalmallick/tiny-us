import SwiftUI

// Live, session-only state of the pixel world: tactile prop toggles, poses, Mochi, particles and sky events.
// Everything here is cosmetic and transient; durable progress lives in TinySave.

enum TinyPose { case idle, walk, sitSnuggle, sitLog, joyJump, cook, eatSneak, hug, ride }

enum TinyCatState { case sleeping, sittingPurr, bellyRoll, playfulPounce, walkFollow, boxNap }

enum TinyParticleKind { case heart, ember, steam, note, petal, sparkle, leaf, bubble, flower, drop, mist, marshmallow, text }

struct TinyParticle {
    var kind: TinyParticleKind
    var x: CGFloat, y: CGFloat          // normalized spawn point
    var vx: CGFloat, vy: CGFloat        // normalized units per second
    var born: Double, life: Double
    var size: CGFloat
    var seed: Double
    var label = ""
}

struct TinyShootingStar { var x: CGFloat, y: CGFloat, born: Double, angle: CGFloat }

struct TinyConstellation {
    let name: String
    let detail: String
    let zone: CGRect
    let stars: [CGPoint]

    // Star positions mirror the Android night sky so both apps share the same constellations.
    static let all: [TinyConstellation] = [
        TinyConstellation(name: "The Two Hearts", detail: "Two shining stars linked across the sky",
                          zone: CGRect(x: 0.08, y: 0.05, width: 0.24, height: 0.23),
                          stars: [CGPoint(x: 0.14, y: 0.11), CGPoint(x: 0.17, y: 0.08), CGPoint(x: 0.20, y: 0.16), CGPoint(x: 0.23, y: 0.22), CGPoint(x: 0.11, y: 0.18), CGPoint(x: 0.14, y: 0.11)]),
        TinyConstellation(name: "The Celestial Teapot", detail: "Pouring warmth and sweet tea over our world",
                          zone: CGRect(x: 0.33, y: 0.05, width: 0.19, height: 0.29),
                          stars: [CGPoint(x: 0.36, y: 0.09), CGPoint(x: 0.49, y: 0.12), CGPoint(x: 0.47, y: 0.19), CGPoint(x: 0.40, y: 0.20), CGPoint(x: 0.34, y: 0.17), CGPoint(x: 0.36, y: 0.09)]),
        TinyConstellation(name: "Starlight Trail", detail: "Guiding our evening ride through gentle breezes",
                          zone: CGRect(x: 0.58, y: 0.06, width: 0.34, height: 0.22),
                          stars: [CGPoint(x: 0.62, y: 0.10), CGPoint(x: 0.68, y: 0.08), CGPoint(x: 0.73, y: 0.12), CGPoint(x: 0.82, y: 0.11), CGPoint(x: 0.89, y: 0.14)])
    ]
}

/// Every tappable prop. Rects are normalized to the canvas and shared by hit-testing and drawing, so they always line up.
enum TinyHotspot: CaseIterable {
    case windChime, flowers, sakura
    case treeCanopy, carvedHeart
    case kettle, fridge, stewPot, sink, cuttingBoard
    case blanket, candle, floorLamp, featherWand, catBox
    case streetLamp, telescope
    case barista, chalkboard, boba, latte, foggyWindow
    case wateringCan, terrarium
    case turntable, moon, nightlight, fairyLights
    case momoCart, scooter
    case firePit, guitar, lantern, plaidBlanket

    var rect: CGRect {
        switch self {
        case .windChime: return CGRect(x: 0.25, y: 0.48, width: 0.10, height: 0.14)
        case .flowers: return CGRect(x: 0.02, y: 0.70, width: 0.30, height: 0.12)
        case .sakura: return CGRect(x: 0.68, y: 0.14, width: 0.30, height: 0.36)
        case .treeCanopy: return CGRect(x: 0.62, y: 0.12, width: 0.36, height: 0.24)
        case .carvedHeart: return CGRect(x: 0.74, y: 0.38, width: 0.14, height: 0.22)
        case .kettle: return CGRect(x: 0.06, y: 0.42, width: 0.14, height: 0.13)
        case .stewPot: return CGRect(x: 0.20, y: 0.43, width: 0.13, height: 0.12)
        case .cuttingBoard: return CGRect(x: 0.36, y: 0.50, width: 0.16, height: 0.07)
        case .sink: return CGRect(x: 0.54, y: 0.46, width: 0.16, height: 0.11)
        case .fridge: return CGRect(x: 0.76, y: 0.22, width: 0.20, height: 0.46)
        case .blanket: return CGRect(x: 0.24, y: 0.52, width: 0.52, height: 0.20)
        case .candle: return CGRect(x: 0.04, y: 0.54, width: 0.12, height: 0.12)
        case .floorLamp: return CGRect(x: 0.84, y: 0.26, width: 0.14, height: 0.44)
        case .featherWand: return CGRect(x: 0.56, y: 0.76, width: 0.14, height: 0.14)
        case .catBox: return CGRect(x: 0.74, y: 0.76, width: 0.18, height: 0.13)
        case .streetLamp: return CGRect(x: 0.04, y: 0.30, width: 0.14, height: 0.44)
        case .telescope: return CGRect(x: 0.76, y: 0.52, width: 0.16, height: 0.20)
        case .barista: return CGRect(x: 0.66, y: 0.30, width: 0.20, height: 0.24)
        case .chalkboard: return CGRect(x: 0.04, y: 0.12, width: 0.22, height: 0.20)
        case .boba: return CGRect(x: 0.04, y: 0.74, width: 0.16, height: 0.14)
        case .latte: return CGRect(x: 0.44, y: 0.60, width: 0.12, height: 0.08)
        case .foggyWindow: return CGRect(x: 0.30, y: 0.10, width: 0.32, height: 0.28)
        case .wateringCan: return CGRect(x: 0.04, y: 0.72, width: 0.16, height: 0.13)
        case .terrarium: return CGRect(x: 0.78, y: 0.50, width: 0.16, height: 0.16)
        case .turntable: return CGRect(x: 0.04, y: 0.50, width: 0.20, height: 0.16)
        case .moon: return CGRect(x: 0.56, y: 0.08, width: 0.16, height: 0.14)
        case .nightlight: return CGRect(x: 0.84, y: 0.54, width: 0.12, height: 0.12)
        case .fairyLights: return CGRect(x: 0.30, y: 0.02, width: 0.66, height: 0.07)
        case .momoCart: return CGRect(x: 0.62, y: 0.40, width: 0.34, height: 0.32)
        case .scooter: return CGRect(x: 0.26, y: 0.58, width: 0.48, height: 0.30)
        case .firePit: return CGRect(x: 0.39, y: 0.64, width: 0.22, height: 0.20)
        case .guitar: return CGRect(x: 0.00, y: 0.60, width: 0.10, height: 0.24)
        case .lantern: return CGRect(x: 0.84, y: 0.42, width: 0.12, height: 0.16)
        case .plaidBlanket: return CGRect(x: 0.62, y: 0.80, width: 0.32, height: 0.12)
        }
    }

    static func available(in scene: TinyScene) -> [TinyHotspot] {
        switch scene {
        case .meadow: return [.windChime, .sakura, .flowers]
        case .tree: return [.carvedHeart, .treeCanopy]
        case .kitchen: return [.kettle, .stewPot, .cuttingBoard, .sink, .fridge]
        case .living: return [.candle, .floorLamp, .featherWand, .catBox, .blanket]
        case .nightWalk: return [.streetLamp, .telescope]
        case .cafe: return [.chalkboard, .barista, .boba, .latte, .foggyWindow]
        case .sunroom: return [.wateringCan, .terrarium]
        case .loft: return [.turntable, .nightlight, .moon, .fairyLights]
        case .momo: return [.momoCart]
        case .scooter: return [.scooter]
        case .twilight: return []
        case .campfire: return [.guitar, .lantern, .plaidBlanket, .firePit]
        }
    }
}

extension TinyScene {
    /// Normalized foot positions of the two characters for this place.
    var characterAnchors: (CGPoint, CGPoint) {
        switch self {
        case .kitchen: return (CGPoint(x: 0.36, y: 0.80), CGPoint(x: 0.56, y: 0.82))
        case .living: return (CGPoint(x: 0.42, y: 0.70), CGPoint(x: 0.58, y: 0.70))
        case .cafe: return (CGPoint(x: 0.33, y: 0.84), CGPoint(x: 0.67, y: 0.84))
        case .loft: return (CGPoint(x: 0.40, y: 0.80), CGPoint(x: 0.56, y: 0.81))
        case .momo: return (CGPoint(x: 0.30, y: 0.82), CGPoint(x: 0.46, y: 0.83))
        case .scooter: return (CGPoint(x: 0.42, y: 0.78), CGPoint(x: 0.52, y: 0.78))
        case .campfire: return (CGPoint(x: 0.17, y: 0.76), CGPoint(x: 0.29, y: 0.76))
        default: return (CGPoint(x: 0.43, y: 0.79), CGPoint(x: 0.57, y: 0.81))
        }
    }

    var basePose: TinyPose {
        switch self {
        case .meadow, .tree, .nightWalk: return .walk
        case .scooter: return .ride
        case .campfire: return .sitLog
        case .living: return .sitSnuggle
        default: return .idle
        }
    }

    var mochiHome: CGPoint {
        switch self {
        case .campfire: return CGPoint(x: 0.78, y: 0.86)
        case .living: return CGPoint(x: 0.83, y: 0.80)
        case .cafe: return CGPoint(x: 0.86, y: 0.88)
        case .scooter: return CGPoint(x: 0.645, y: 0.70)
        default: return CGPoint(x: 0.76, y: 0.84)
        }
    }

    /// Scenes that show the full starfield (and constellations) after dark.
    var hasStarfield: Bool { [.meadow, .tree, .nightWalk, .momo, .twilight, .campfire].contains(self) }

    func isNight(at date: Date) -> Bool {
        if [.nightWalk, .loft, .twilight, .campfire].contains(self) { return true }
        let hour = Calendar.current.component(.hour, from: date)
        return hour < 6 || hour > 19
    }
}

struct TinyStageSnapshot {
    var poses: (TinyPose, TinyPose)
    var gaze: CGPoint?
    var blushing: Bool
    var cat: CGPoint
    var catState: TinyCatState
    var catWalkingLeft: Bool
    var floorLampOn, candleLit, vinylSpinning, nightlightOn, fairyLightsOn, lanternOn, streetLampsOn, blanketSnuggle: Bool
    var sproutGrowth: Int
    var chimeSwingAt, kettleSteamAt, baristaBrewAt, latteHeartAt, bobaWagAt, fireStokedAt, guitarStrumAt, hornAt, fridgeNoteAt: Double
    var windowHearts: [(CGPoint, Double)]
    var particles: [TinyParticle]
    var shootingStars: [TinyShootingStar]
    /// (constellation index, progress 0…1) while a constellation is being traced.
    var constellation: (Int, Double)?

    /// A calm, default world used for Polaroid snapshots and previews.
    static func resting(_ scene: TinyScene) -> TinyStageSnapshot {
        TinyStageSnapshot(poses: (scene.basePose, scene == .kitchen ? .idle : scene.basePose), gaze: nil, blushing: false,
                          cat: scene.mochiHome, catState: scene == .living ? .boxNap : .sleeping, catWalkingLeft: false,
                          floorLampOn: true, candleLit: true, vinylSpinning: true, nightlightOn: true, fairyLightsOn: true,
                          lanternOn: true, streetLampsOn: true, blanketSnuggle: true, sproutGrowth: 2,
                          chimeSwingAt: -10, kettleSteamAt: -10, baristaBrewAt: -10, latteHeartAt: -10, bobaWagAt: -10,
                          fireStokedAt: -10, guitarStrumAt: -10, hornAt: -10, fridgeNoteAt: -10,
                          windowHearts: [], particles: [], shootingStars: [], constellation: nil)
    }
}

@MainActor final class TinyStage: ObservableObject {
    /// Bumped on every interaction so the canvas redraws even when Reduce Motion pauses the timeline.
    @Published private(set) var revision = 0

    // Prop state
    var floorLampOn = true
    var candleLit = false
    var vinylSpinning = false
    var nightlightOn = true
    var fairyLightsOn = true
    var lanternOn = true
    var streetLampsOn = true
    var blanketSnuggle = true
    var sproutGrowth = 0
    var marshmallowsRoasted = 0
    var flowersPicked = 0
    var chimeSwingAt: Double = -10
    var kettleSteamAt: Double = -10
    var baristaBrewAt: Double = -10
    var latteHeartAt: Double = -10
    var bobaWagAt: Double = -10
    var fireStokedAt: Double = -10
    var guitarStrumAt: Double = -10
    var hornAt: Double = -10
    var fridgeNote = ""
    var fridgeNoteAt: Double = -10
    var windowHearts: [(CGPoint, Double)] = []

    // Characters
    var poseOverride: TinyPose?
    var poseUntil: Double = 0
    var gazeTarget: CGPoint?
    var gazeUntil: Double = 0
    var blushUntil: Double = 0

    // Mochi
    var catState: TinyCatState = .sleeping
    var catStateUntil: Double = 0
    var catX: CGFloat = 0.76
    var catY: CGFloat = 0.84
    var catTargetX: CGFloat?
    var catMoveStart: (x: CGFloat, at: Double)?

    // Sky
    var shootingStars: [TinyShootingStar] = []
    var activeConstellation: Int?
    var constellationAt: Double = -10

    private(set) var particles: [TinyParticle] = []
    private var lastCharacterTap: Double = -10
    private var scene: TinyScene?

    static var now: Double { Date().timeIntervalSinceReferenceDate }

    func enter(_ scene: TinyScene) {
        guard scene != self.scene else { return }
        self.scene = scene
        particles.removeAll(); shootingStars.removeAll(); windowHearts.removeAll()
        poseOverride = nil; activeConstellation = nil
        catState = scene == .living ? .boxNap : .sleeping
        catX = scene.mochiHome.x; catY = scene.mochiHome.y; catTargetX = nil; catMoveStart = nil
        bump()
    }

    func pose(for index: Int, scene: TinyScene, at time: Double) -> TinyPose {
        if let poseOverride, time < poseUntil {
            if poseOverride == .cook || poseOverride == .eatSneak { return index == 0 ? poseOverride : scene.basePose }
            return poseOverride
        }
        if scene == .living { return blanketSnuggle ? .sitSnuggle : .idle }
        if scene == .kitchen && index == 0 { return .cook }
        return scene.basePose
    }

    /// Mochi's live position, advancing any walk toward a tapped floor point.
    func catPosition(at time: Double) -> CGPoint {
        guard let target = catTargetX, let start = catMoveStart else { return CGPoint(x: catX, y: catY) }
        let distance = target - start.x
        let duration = max(0.4, Double(abs(distance)) / 0.22)
        let progress = min(1, (time - start.at) / duration)
        let x = start.x + distance * CGFloat(progress)
        if progress >= 1 {
            catX = target; catTargetX = nil; catMoveStart = nil
            catState = .sittingPurr; catStateUntil = time + 3
        }
        return CGPoint(x: x, y: catY)
    }

    func currentCatState(at time: Double) -> TinyCatState {
        if catTargetX != nil { return .walkFollow }
        if catState != .sleeping && catState != .boxNap && time > catStateUntil {
            catState = scene == .living ? .boxNap : .sleeping
        }
        return catState
    }

    /// Immutable copy of everything the painter needs for one frame (the Canvas renderer stays actor-free).
    func snapshot(scene: TinyScene, at time: Double) -> TinyStageSnapshot {
        var tracing: (Int, Double)? = nil
        if let index = activeConstellation, time - constellationAt < 2.4 { tracing = (index, (time - constellationAt) / 2.4) }
        return TinyStageSnapshot(
            poses: (pose(for: 0, scene: scene, at: time), pose(for: 1, scene: scene, at: time)),
            gaze: time < gazeUntil ? gazeTarget : nil, blushing: time < blushUntil,
            cat: catPosition(at: time), catState: currentCatState(at: time), catWalkingLeft: (catTargetX ?? catX) < catX,
            floorLampOn: floorLampOn, candleLit: candleLit, vinylSpinning: vinylSpinning, nightlightOn: nightlightOn,
            fairyLightsOn: fairyLightsOn, lanternOn: lanternOn, streetLampsOn: streetLampsOn, blanketSnuggle: blanketSnuggle,
            sproutGrowth: sproutGrowth, chimeSwingAt: chimeSwingAt, kettleSteamAt: kettleSteamAt, baristaBrewAt: baristaBrewAt,
            latteHeartAt: latteHeartAt, bobaWagAt: bobaWagAt, fireStokedAt: fireStokedAt, guitarStrumAt: guitarStrumAt,
            hornAt: hornAt, fridgeNoteAt: fridgeNoteAt, windowHearts: windowHearts,
            particles: particles, shootingStars: shootingStars,
            constellation: tracing)
    }

    // MARK: - Gestures

    func handleTap(at point: CGPoint, size: CGSize, world: TinyWorld, date: Date = Date()) {
        guard size.width > 0, size.height > 0 else { return }
        let p = CGPoint(x: point.x / size.width, y: point.y / size.height)
        let scene = world.save.scene
        let now = Self.now
        gazeTarget = p; gazeUntil = now + 3
        defer { prune(now); bump() }

        if hitsCat(p, now: now) { petMochi(world: world, now: now); return }
        if hitsCharacters(p, scene: scene) {
            if now - lastCharacterTap < 0.35 { joyJump(world: world, now: now) }
            else { blushUntil = now + 2; world.interactWithCharacters() }
            lastCharacterTap = now
            return
        }
        if let hotspot = TinyHotspot.available(in: scene).first(where: { $0.rect.insetBy(dx: -0.02, dy: -0.02).contains(p) }) {
            activate(hotspot, at: p, world: world, now: now)
            return
        }
        if p.y < 0.48 && !scene.isIndoor {
            if scene.isNight(at: date) && scene.hasStarfield, let index = TinyConstellation.all.firstIndex(where: { $0.zone.contains(p) }) {
                traceConstellation(index, at: p, world: world, now: now)
            } else {
                skyWish(at: p, night: scene.isNight(at: date), world: world, now: now)
            }
            return
        }
        if p.y > 0.66 && scene != .scooter {
            // Floor tap: Mochi trots over to see what happened.
            let target = min(0.92, max(0.08, p.x))
            let current = catPosition(at: now)
            catX = current.x; catTargetX = target; catMoveStart = (current.x, now)
            burst(.sparkle, at: p, count: 4, now: now, spread: 0.03, rise: 0.05)
            TinySoundBoard.shared.play(.bubblePop, gain: 0.5)
            return
        }
        world.interactWithScene()
    }

    func handleLongPress(world: TinyWorld) {
        let now = Self.now
        poseOverride = .hug; poseUntil = now + 2.6
        let (a, b) = world.save.scene.characterAnchors
        burst(.heart, at: CGPoint(x: (a.x + b.x) / 2, y: min(a.y, b.y) - 0.16), count: 7, now: now, spread: 0.06, rise: 0.12)
        TinySoundBoard.shared.play(.heartChime)
        world.showToast("Holding you close.")
        bump()
    }

    // MARK: - Hit testing

    private func hitsCat(_ p: CGPoint, now: Double) -> Bool {
        let cat = catPosition(at: now)
        return abs(p.x - cat.x) < 0.07 && p.y > cat.y - 0.07 && p.y < cat.y + 0.04
    }

    private func hitsCharacters(_ p: CGPoint, scene: TinyScene) -> Bool {
        let (a, b) = scene.characterAnchors
        return [a, b].contains { abs(p.x - $0.x) < 0.07 && p.y > $0.y - 0.17 && p.y < $0.y + 0.02 }
    }

    // MARK: - Interactions

    private func activate(_ hotspot: TinyHotspot, at p: CGPoint, world: TinyWorld, now: Double) {
        let sound = TinySoundBoard.shared
        let r = hotspot.rect
        let center = CGPoint(x: r.midX, y: r.minY + r.height * 0.3)
        switch hotspot {
        case .windChime:
            chimeSwingAt = now; sound.play(.windChime)
            burst(.sparkle, at: center, count: 5, now: now, spread: 0.04, rise: 0.04)
            world.showToast("The crystalline porch wind chime sings in the breeze 🎐")
        case .flowers:
            flowersPicked += 1; sound.play(.bubblePop)
            burst(.flower, at: p, count: 3, now: now, spread: 0.03, rise: 0.1)
            world.showToast(TinyStage.flowerLines.pick())
        case .sakura:
            sound.play(.windChime, gain: 0.6)
            burst(.petal, at: center, count: 14, now: now, spread: 0.12, rise: -0.08)
            world.showToast("Sakura petals swirl down around you 🌸")
        case .treeCanopy:
            sound.play(.bubblePop, gain: 0.6)
            burst(.leaf, at: center, count: 10, now: now, spread: 0.12, rise: -0.07)
            world.showToast("A leaf lands softly in their hair 🍃")
        case .carvedHeart:
            sound.play(.heartChime)
            burst(.heart, at: center, count: 6, now: now, spread: 0.04, rise: 0.1)
            world.showToast("Your initials, still carved in the old bark 💕")
        case .kettle:
            kettleSteamAt = now; sound.play(.kettleWhistle)
            burst(.steam, at: CGPoint(x: r.midX + 0.03, y: r.minY), count: 8, now: now, spread: 0.02, rise: 0.12)
            world.showToast("Whistling teakettle! Fresh hot tea steeping for both of us ☕")
        case .stewPot:
            sound.play(.bubblePop)
            burst(.bubble, at: CGPoint(x: r.midX, y: r.minY + 0.02), count: 6, now: now, spread: 0.03, rise: 0.06)
            burst(.steam, at: CGPoint(x: r.midX, y: r.minY), count: 4, now: now, spread: 0.02, rise: 0.1)
            world.showToast("The stew bubbles away, smelling like home 🍲")
        case .cuttingBoard:
            poseOverride = .cook; poseUntil = now + 2.5; sound.play(.cardFlip)
            world.showToast("Chopping sweet carrots and fresh herbs together.")
        case .sink:
            sound.play(.waterDrip)
            burst(.drop, at: CGPoint(x: r.midX, y: r.minY + 0.02), count: 5, now: now, spread: 0.02, rise: -0.05)
            world.showToast("Splashing fresh water — washing veggies and tea cups!")
        case .fridge:
            fridgeNote = world.fridgeNote(); fridgeNoteAt = now; sound.play(.heartChime)
            burst(.heart, at: center, count: 4, now: now, spread: 0.04, rise: 0.08)
            world.showToast("On the fridge: “\(fridgeNote)”")
        case .blanket:
            blanketSnuggle.toggle(); sound.play(blanketSnuggle ? .heartChime : .bubblePop)
            if blanketSnuggle { burst(.heart, at: center, count: 5, now: now, spread: 0.06, rise: 0.08) }
            world.showToast(blanketSnuggle ? "Snuggling warm under the chunky knit throw together 💕" : "They stretch and fold the blanket")
        case .candle:
            candleLit.toggle(); sound.play(.lampClick)
            if candleLit { burst(.ember, at: CGPoint(x: r.midX, y: r.minY), count: 6, now: now, spread: 0.01, rise: 0.08) }
            world.showToast(candleLit ? "Lit the lavender soy candle... warm, calming scent fills the room 🕯️" : "Blew out the candle with a gentle breath. Time to rest.")
        case .floorLamp:
            floorLampOn.toggle(); sound.play(.lampClick)
            world.showToast(floorLampOn ? "Lamp on — warm amber light fills the room." : "Lamp off — time to drift away together.")
        case .featherWand:
            catState = .playfulPounce; catStateUntil = now + 1.8
            catTargetX = nil; catMoveStart = nil; catX = 0.64
            sound.play(.bubblePop)
            burst(.sparkle, at: center, count: 5, now: now, spread: 0.04, rise: 0.06)
            world.showToast("Mochi pounces on the feather wand with pure joy! 🐾")
        case .catBox:
            catTargetX = nil; catMoveStart = nil
            catX = TinyHotspot.catBox.rect.midX; catY = 0.84
            catState = catState == .boxNap ? .sittingPurr : .boxNap; catStateUntil = now + 4
            sound.play(.catPurr)
            world.showToast(catState == .boxNap ? "Mochi curls up in the cardboard box 📦" : "Mochi peeks out of the box 🐈")
        case .streetLamp:
            streetLampsOn.toggle(); sound.play(.lampClick)
            world.showToast(streetLampsOn ? "The vintage streetlamp glows again 🏮" : "The lamp dims so the stars can shine ✨")
        case .telescope:
            spawnShootingStar(at: CGPoint(x: 0.3 + CGFloat.random(in: 0...0.4), y: 0.08), now: now)
            sound.play(.starTwinkle)
            world.showToast("A shooting star crossed the night sky! Made a quiet wish for us 🌠")
        case .barista:
            baristaBrewAt = now; sound.play(.steamHiss)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.85) { sound.play(.cafeBell) }
            burst(.steam, at: CGPoint(x: r.minX + 0.04, y: r.minY + 0.08), count: 7, now: now, spread: 0.02, rise: 0.1)
            world.showToast("Barista Leo: 'Fresh espresso brewing! Extra warm love for you two.' ☕✨")
        case .chalkboard:
            sound.play(.cardFlip)
            world.showToast("Today’s special: \(TinyStage.cafeSpecial(for: Date()))")
        case .boba:
            bobaWagAt = now; sound.play(.bubblePop)
            burst(.heart, at: center, count: 3, now: now, spread: 0.03, rise: 0.08)
            world.showToast("Boba wags so hard the whole pup wiggles 🐶")
        case .latte:
            latteHeartAt = now; sound.play(.bubblePop, gain: 0.7)
            burst(.heart, at: CGPoint(x: r.midX, y: r.minY), count: 2, now: now, spread: 0.01, rise: 0.06)
            world.showToast("A tiny heart in the latte foam, made just for you.")
        case .foggyWindow:
            windowHearts.append((p, now)); if windowHearts.count > 6 { windowHearts.removeFirst() }
            sound.play(.heartChime, gain: 0.7)
            world.showToast("A little heart fogs the rainy window.")
        case .wateringCan:
            sproutGrowth = min(3, sproutGrowth + 1); sound.play(.waterDrip)
            burst(.mist, at: CGPoint(x: r.maxX, y: r.minY), count: 10, now: now, spread: 0.05, rise: -0.03)
            world.showToast(sproutGrowth >= 3 ? "Tiny flowers open among the succulents!" : "A cool morning mist curls through the greenhouse.")
        case .terrarium:
            sound.play(.starTwinkle, gain: 0.7)
            burst(.sparkle, at: center, count: 8, now: now, spread: 0.05, rise: 0.05)
            world.showToast("A tiny world inside a tiny world 🪴")
        case .turntable:
            vinylSpinning.toggle(); sound.play(vinylSpinning ? .vinylSpin : .lampClick)
            if vinylSpinning { burst(.note, at: center, count: 4, now: now, spread: 0.04, rise: 0.1) }
            world.showToast(vinylSpinning ? "Vinyl needle drops... warm cozy melodies play." : "Turntable paused.")
        case .moon:
            spawnShootingStar(at: CGPoint(x: r.midX - 0.1, y: r.minY), now: now)
            spawnShootingStar(at: CGPoint(x: r.midX + 0.05, y: r.minY + 0.03), now: now + 0.25)
            sound.play(.shootingStar)
            world.showToast("Shooting stars streak past the crescent moon 🌙")
        case .nightlight:
            nightlightOn.toggle(); sound.play(.lampClick)
            world.showToast(nightlightOn ? "The bedside nightlight glows soft pink 🌸" : "Nightlight off — the city twinkles instead")
        case .fairyLights:
            fairyLightsOn.toggle(); sound.play(.starTwinkle, gain: 0.7)
            world.showToast(fairyLightsOn ? "Balcony fairy lights twinkle on ✨" : "The fairy lights rest for the night")
        case .momoCart:
            poseOverride = .eatSneak; poseUntil = now + 2.6; sound.play(.steamHiss, gain: 0.8)
            TinySoundBoard.shared.play(.bubblePop, gain: 0.5)
            burst(.steam, at: CGPoint(x: r.midX, y: r.minY + 0.06), count: 8, now: now, spread: 0.04, rise: 0.1)
            world.showToast("Fresh momos! One is quietly stolen 🥟")
        case .scooter:
            hornAt = now; sound.play(.scooterHorn)
            burst(.text, at: CGPoint(x: r.midX, y: r.minY), count: 1, now: now, spread: 0, rise: 0.08, label: "beep beep!")
            world.showToast("Scooter road trip with my favourite person 🛵")
        case .firePit:
            fireStokedAt = now; marshmallowsRoasted += 1; sound.play(.fireCrackle)
            burst(.ember, at: CGPoint(x: r.midX, y: r.minY + 0.05), count: 12, now: now, spread: 0.04, rise: 0.16)
            burst(.marshmallow, at: CGPoint(x: r.midX, y: r.minY), count: 1, now: now, spread: 0, rise: 0.05)
            world.showToast("Roasting sweet golden marshmallows over the crackling campfire embers 🪵🔥")
        case .guitar:
            guitarStrumAt = now; sound.play(.guitarStrum)
            burst(.note, at: CGPoint(x: r.midX + 0.03, y: r.minY), count: 5, now: now, spread: 0.04, rise: 0.12)
            world.showToast("Strumming a quiet acoustic melody beneath the pine trees and starlight 🎸✨")
        case .lantern:
            lanternOn.toggle(); sound.play(.lampClick)
            world.showToast(lanternOn ? "The warm camp lantern glows bright beside our tent ⛺💡" : "Dimmed the lantern for better stargazing 🌌")
        case .plaidBlanket:
            petMochi(world: world, now: now)
        }
    }

    /// Mirrors Android's tap cycle: sleeping → purring → belly roll → back to a snooze.
    func petMochi(world: TinyWorld, now: Double) {
        let cat = catPosition(at: now)
        let wasWalking = catTargetX != nil
        catTargetX = nil; catMoveStart = nil; catX = cat.x
        world.save.mochiPats += 1
        let message: String
        if wasWalking { catState = .sittingPurr; message = "Mochi stopped to get your love!" }
        else {
            switch currentCatState(at: now) {
            case .sittingPurr: catState = .bellyRoll; message = "Mochi wants gentle belly rubs!"
            case .bellyRoll: catState = .sleeping; message = "Mochi curled up for a warm snooze."
            default: catState = .sittingPurr; message = "Mochi is purring happily!"
            }
        }
        catStateUntil = catState == .sleeping ? 0 : now + 6
        TinySoundBoard.shared.play(.catPurr)
        burst(.heart, at: CGPoint(x: cat.x, y: cat.y - 0.06), count: 3, now: now, spread: 0.02, rise: 0.08)
        world.showToast(message)
    }

    private func joyJump(world: TinyWorld, now: Double) {
        poseOverride = .joyJump; poseUntil = now + 1.4
        let (a, b) = world.save.scene.characterAnchors
        burst(.sparkle, at: CGPoint(x: (a.x + b.x) / 2, y: a.y - 0.14), count: 8, now: now, spread: 0.08, rise: 0.08)
        TinySoundBoard.shared.play(.gameWin, gain: 0.7)
        world.showToast("A little jump for joy! ✨")
    }

    private func traceConstellation(_ index: Int, at p: CGPoint, world: TinyWorld, now: Double) {
        if activeConstellation == index && now - constellationAt < 2.4 { return }
        activeConstellation = index; constellationAt = now
        let constellation = TinyConstellation.all[index]
        TinySoundBoard.shared.play(.starTwinkle)
        burst(.sparkle, at: p, count: 7, now: now, spread: 0.04, rise: 0.02)
        burst(.heart, at: CGPoint(x: p.x, y: p.y - 0.02), count: 1, now: now, spread: 0, rise: 0.05)
        gazeTarget = p; gazeUntil = now + 4; blushUntil = now + 2.2
        world.showToast("Constellation: \(constellation.name) - \(constellation.detail)")
    }

    private func skyWish(at p: CGPoint, night: Bool, world: TinyWorld, now: Double) {
        if night {
            spawnShootingStar(at: CGPoint(x: max(0.1, p.x - 0.1), y: max(0.04, p.y - 0.05)), now: now)
            TinySoundBoard.shared.play(.shootingStar)
            world.showToast(TinyStage.skyLines.pick())
        } else {
            burst(.sparkle, at: p, count: 6, now: now, spread: 0.05, rise: 0.02)
            TinySoundBoard.shared.play(.windChime, gain: 0.5)
        }
    }

    private func spawnShootingStar(at p: CGPoint, now: Double) {
        shootingStars.append(TinyShootingStar(x: p.x, y: p.y, born: now, angle: CGFloat.random(in: 0.35...0.6)))
        if shootingStars.count > 5 { shootingStars.removeFirst() }
    }

    func burst(_ kind: TinyParticleKind, at p: CGPoint, count: Int, now: Double, spread: CGFloat, rise: CGFloat, label: String = "") {
        for i in 0..<count {
            let seed = Double.random(in: 0...1)
            let life: Double
            switch kind {
            case .steam, .mist: life = 1.6
            case .petal, .leaf: life = 3.2
            case .text, .marshmallow: life = 1.8
            case .ember: life = 1.4
            default: life = 1.5
            }
            particles.append(TinyParticle(
                kind: kind, x: p.x + CGFloat.random(in: -spread...max(spread, 0.0001)), y: p.y,
                vx: CGFloat.random(in: -0.04...0.04) + (kind == .petal || kind == .leaf ? 0.03 : 0),
                vy: -rise * CGFloat.random(in: 0.7...1.2),
                born: now + Double(i) * (kind == .steam ? 0.08 : 0.02), life: life,
                size: CGFloat.random(in: 0.8...1.3), seed: seed, label: label))
        }
        if particles.count > 180 { particles.removeFirst(particles.count - 180) }
    }

    private func prune(_ now: Double) {
        particles.removeAll { now - $0.born > $0.life }
        shootingStars.removeAll { now - $0.born > 1.4 }
        windowHearts.removeAll { now - $0.1 > 10 }
    }

    private func bump() { revision &+= 1 }

    static let flowerLines = TinyLinePicker(["This reminded me of you.", "Our garden is growing with our days together.",
                                              "Every petal holds a sweet memory of us.", "Blooming more beautifully every single day."])
    static let skyLines = TinyLinePicker(["A quiet moment under the endless starry sky.", "Every star shines a little brighter with you here.",
                                          "Make a wish on the shooting star!", "Wrapped in starlight and gentle evening whispers."])

    static func cafeSpecial(for date: Date) -> String {
        let specials = ["Honey oat latte & a warm cinnamon roll 🥐", "Rainy-day hot cocoa with tiny marshmallows ☕️",
                        "Matcha cloud latte & strawberry shortcake 🍰", "Caramel cortado & a buttery croissant 🥐",
                        "Chai for two & ginger snap cookies 🍪", "Lavender mocha & a blueberry muffin 🫐",
                        "Iced vanilla cold brew & lemon tart 🍋"]
        let day = Calendar.current.ordinality(of: .day, in: .era, for: date) ?? 0
        return specials[day % specials.count]
    }
}

/// Never repeats the previous line back to back, like the shared AntiRepeatRandomPicker.
final class TinyLinePicker {
    private let lines: [String]
    private var last: Int?
    init(_ lines: [String]) { self.lines = lines }
    func pick() -> String {
        let choices = lines.indices.filter { $0 != last }
        let index = choices.randomElement() ?? 0
        last = index
        return lines[index]
    }
}
