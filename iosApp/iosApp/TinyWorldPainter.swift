import SwiftUI

/// Shared pixel palette for the world painter.
private enum Pal {
    static let ink = Color(red: 0.20, green: 0.18, blue: 0.22)
    static let skin = Color(red: 0.96, green: 0.83, blue: 0.69)
    static let skinShade = Color(red: 0.89, green: 0.75, blue: 0.63)
    static let blush = Color(red: 0.93, green: 0.52, blue: 0.56)
    static let hairBoy = Color(red: 0.30, green: 0.23, blue: 0.24)
    static let hairGirl = Color(red: 0.42, green: 0.27, blue: 0.24)
    static let bow = Color(red: 0.96, green: 0.55, blue: 0.64)
    static let wood = Color(red: 0.55, green: 0.38, blue: 0.29)
    static let woodDark = Color(red: 0.38, green: 0.26, blue: 0.22)
    static let woodLight = Color(red: 0.72, green: 0.54, blue: 0.40)
    static let cream = Color(red: 0.98, green: 0.92, blue: 0.80)
    static let warm = Color(red: 1.00, green: 0.80, blue: 0.47)
    static let glow = Color(red: 1.00, green: 0.74, blue: 0.40)
    static let pink = Color(red: 1.00, green: 0.52, blue: 0.63)
    static let leaf = Color(red: 0.42, green: 0.62, blue: 0.40)
    static let leafDark = Color(red: 0.29, green: 0.46, blue: 0.33)
    static let stone = Color(red: 0.55, green: 0.55, blue: 0.58)
    static let fur = Color(red: 0.93, green: 0.88, blue: 0.79)
    static let golden = Color(red: 0.90, green: 0.70, blue: 0.40)
    static let steel = Color(red: 0.70, green: 0.72, blue: 0.76)
    static let glass = Color(red: 0.74, green: 0.84, blue: 0.88)
    static let cyan = Color(red: 0.39, green: 0.87, blue: 0.87)
    static let collars: [Color?] = [Color(red: 0.86, green: 0.25, blue: 0.30), Color(red: 1, green: 0.93, blue: 0.55),
                                    Color(red: 0.40, green: 0.58, blue: 0.88), nil]
    static let outfits: [[Color]] = [
        [Color(red: 0.48, green: 0.68, blue: 0.85), Color(red: 0.92, green: 0.61, blue: 0.67)],
        [Color(red: 0.59, green: 0.66, blue: 0.50), Color(red: 0.93, green: 0.72, blue: 0.52)],
        [Color(red: 0.48, green: 0.58, blue: 0.69), Color(red: 0.76, green: 0.63, blue: 0.69)],
        [Color(red: 0.38, green: 0.42, blue: 0.64), Color(red: 0.66, green: 0.53, blue: 0.72)]
    ]
    static let autumn = [Color(red: 0.92, green: 0.56, blue: 0.31), Color(red: 0.83, green: 0.70, blue: 0.35), Color(red: 0.72, green: 0.38, blue: 0.30)]
}

private let heartPattern: [[Int]] = [[0, 1, 0, 1, 0], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1], [0, 1, 1, 1, 0], [0, 0, 1, 0, 0]]

private func fract(_ v: Double) -> Double { v - floor(v) }
private func hash(_ i: Int, _ salt: Double) -> Double { fract(sin(Double(i) * 12.9898 + salt) * 43758.5453) }

/// Pure procedural painter for the pixel world. Takes only values, so it can render live frames and Polaroid snapshots alike.
struct TinyWorldPainter {
    let scene: TinyScene
    let weather: TinyWeather
    let previousWeather: TinyWeather?
    /// 0…1 cross-fade from `previousWeather` into `weather`.
    let weatherBlend: Double
    let outfit: Int
    let collar: Int
    let hour: Int
    /// Ambient animation clock (frozen at 0 when Reduce Motion is on).
    let t: Double
    /// Real clock used to age tap-spawned particles and events.
    let now: Double
    let stage: TinyStageSnapshot

    private var night: Bool { [.nightWalk, .loft, .twilight, .campfire].contains(scene) || hour < 6 || hour > 19 }
    private var sunset: Bool { scene == .twilight || (!night && hour >= 17) }

    func paint(_ c: inout GraphicsContext, size: CGSize) {
        let g = Geo(w: size.width, h: size.height)
        switch scene {
        case .kitchen: kitchen(&c, g)
        case .living: living(&c, g)
        case .cafe: cafe(&c, g)
        case .sunroom: sunroom(&c, g)
        case .loft: loft(&c, g)
        default: outdoor(&c, g)
        }
        if scene.isIndoor {
            for window in windows { weatherLayer(&c, g, clip: window) }
        } else {
            weatherLayer(&c, g, clip: nil)
        }
        if scene == .living && night && !stage.floorLampOn { g.rect(&c, 0, 0, 1, 1, Color(red: 0.05, green: 0.05, blue: 0.16).opacity(0.32)) }
        if scene != .scooter { drawCat(&c, g) }
        drawCharacters(&c, g)
        if scene == .scooter { scooterForeground(&c, g) }
        drawParticles(&c, g)
    }

    // MARK: - Geometry helpers

    struct Geo {
        let w: CGFloat, h: CGFloat
        var u: CGFloat { max(0.7, w / 360) }
        func rect(_ c: inout GraphicsContext, _ x: CGFloat, _ y: CGFloat, _ rw: CGFloat, _ rh: CGFloat, _ color: Color) {
            c.fill(Path(CGRect(x: x * w, y: y * h, width: rw * w, height: rh * h)), with: .color(color))
        }
        func abs(_ c: inout GraphicsContext, _ x: CGFloat, _ y: CGFloat, _ rw: CGFloat, _ rh: CGFloat, _ color: Color) {
            c.fill(Path(CGRect(x: x, y: y, width: rw, height: rh)), with: .color(color))
        }
        func oval(_ c: inout GraphicsContext, _ x: CGFloat, _ y: CGFloat, _ rw: CGFloat, _ rh: CGFloat, _ color: Color) {
            c.fill(Path(ellipseIn: CGRect(x: x * w, y: y * h, width: rw * w, height: rh * h)), with: .color(color))
        }
        func glow(_ c: inout GraphicsContext, _ x: CGFloat, _ y: CGFloat, radius: CGFloat, _ color: Color, _ alpha: Double) {
            let center = CGPoint(x: x * w, y: y * h)
            let r = radius * w
            c.fill(Path(ellipseIn: CGRect(x: center.x - r, y: center.y - r, width: r * 2, height: r * 2)),
                   with: .radialGradient(Gradient(colors: [color.opacity(alpha), color.opacity(0)]), center: center, startRadius: 0, endRadius: r))
        }
        func poly(_ c: inout GraphicsContext, _ points: [CGPoint], _ color: Color) {
            var path = Path()
            path.addLines(points.map { CGPoint(x: $0.x * w, y: $0.y * h) })
            path.closeSubpath()
            c.fill(path, with: .color(color))
        }
        func line(_ c: inout GraphicsContext, _ a: CGPoint, _ b: CGPoint, _ color: Color, width: CGFloat) {
            var path = Path()
            path.move(to: CGPoint(x: a.x * w, y: a.y * h)); path.addLine(to: CGPoint(x: b.x * w, y: b.y * h))
            c.stroke(path, with: .color(color), lineWidth: width)
        }
        func heart(_ c: inout GraphicsContext, _ cx: CGFloat, _ cy: CGFloat, cell: CGFloat, _ color: Color) {
            for (row, cells) in heartPattern.enumerated() {
                for (col, on) in cells.enumerated() where on == 1 {
                    c.fill(Path(CGRect(x: cx - cell * 2.5 + CGFloat(col) * cell, y: cy - cell * 2.5 + CGFloat(row) * cell, width: cell, height: cell)), with: .color(color))
                }
            }
        }
    }

    private var windows: [CGRect] {
        switch scene {
        case .kitchen: return [CGRect(x: 0.36, y: 0.14, width: 0.24, height: 0.22)]
        case .living: return [CGRect(x: 0.36, y: 0.12, width: 0.28, height: 0.22)]
        case .cafe: return [CGRect(x: 0.30, y: 0.10, width: 0.32, height: 0.28)]
        case .sunroom: return [CGRect(x: 0, y: 0, width: 1, height: 0.60)]
        case .loft: return [CGRect(x: 0.30, y: 0.08, width: 0.68, height: 0.44)]
        default: return []
        }
    }

    // MARK: - Sky

    private func sky(_ c: inout GraphicsContext, _ g: Geo, in frame: CGRect) {
        let colors: [Color]
        if scene == .twilight {
            colors = [Color(red: 0.27, green: 0.24, blue: 0.45), Color(red: 0.86, green: 0.50, blue: 0.62), Color(red: 1, green: 0.74, blue: 0.56)]
        } else if night {
            colors = [Color(red: 0.08, green: 0.10, blue: 0.24), Color(red: 0.18, green: 0.21, blue: 0.38), Color(red: 0.36, green: 0.33, blue: 0.47)]
        } else if sunset {
            colors = [Color(red: 0.45, green: 0.47, blue: 0.70), Color(red: 0.96, green: 0.62, blue: 0.55), Color(red: 1, green: 0.82, blue: 0.56)]
        } else {
            colors = [Color(red: 0.47, green: 0.68, blue: 0.86), Color(red: 0.66, green: 0.82, blue: 0.90), Color(red: 0.98, green: 0.86, blue: 0.70)]
        }
        let r = CGRect(x: frame.minX * g.w, y: frame.minY * g.h, width: frame.width * g.w, height: frame.height * g.h)
        c.fill(Path(r), with: .linearGradient(Gradient(colors: colors), startPoint: CGPoint(x: 0, y: r.minY), endPoint: CGPoint(x: 0, y: r.maxY)))
        if night || scene == .twilight { stars(&c, g, frame: frame) }
        if night && scene != .twilight { meteor(&c, g) }
        if night && scene != .loft {
            g.glow(&c, 0.80, 0.14, radius: 0.12, .white, 0.10)
            g.oval(&c, 0.765, 0.105, 0.07, 0.07 * g.w / g.h, Color(red: 0.95, green: 0.92, blue: 0.80))
            g.oval(&c, 0.785, 0.095, 0.06, 0.06 * g.w / g.h, colors[0].opacity(0.9)) // crescent bite
        } else if !night {
            let sunY: CGFloat = sunset ? 0.30 : 0.12
            g.glow(&c, 0.80, sunY + 0.03, radius: 0.14, Pal.warm, 0.25)
            g.oval(&c, 0.76, sunY, 0.08, 0.08 * g.w / g.h, sunset ? Color(red: 1, green: 0.66, blue: 0.45) : Color(red: 1, green: 0.88, blue: 0.56))
            clouds(&c, g)
        }
        if let tracing = stage.constellation, scene.hasStarfield { constellation(&c, g, index: tracing.0, progress: tracing.1) }
        for star in stage.shootingStars { shootingStar(&c, g, star) }
    }

    private func stars(_ c: inout GraphicsContext, _ g: Geo, frame: CGRect) {
        let fade: Double = scene == .twilight ? 0.45 : 1
        for i in 0..<64 {
            let x = CGFloat(hash(i, 1.7)), y = CGFloat(hash(i, 9.1)) * 0.46
            guard frame.contains(CGPoint(x: x, y: y)) else { continue }
            let twinkle = 0.55 + 0.45 * sin(t * (0.8 + hash(i, 3.3) * 2) + Double(i))
            let s = (1 + CGFloat(hash(i, 5.5)) * 1.4) * g.u
            g.abs(&c, x * g.w, y * g.h, s, s, Color.white.opacity(twinkle * 0.85 * fade))
        }
        if scene.hasStarfield {
            // Bright anchor stars of the three constellations, like the Android sky.
            for constellation in TinyConstellation.all {
                for star in constellation.stars {
                    let s = 2.4 * g.u
                    g.abs(&c, star.x * g.w - s / 2, star.y * g.h - s / 2, s, s, Color(red: 1, green: 0.95, blue: 0.85).opacity(0.95 * fade))
                    g.abs(&c, star.x * g.w - s * 1.5, star.y * g.h - 0.5, s * 3, 1, Color.white.opacity(0.35 * fade))
                }
            }
        }
    }

    private func meteor(_ c: inout GraphicsContext, _ g: Geo) {
        let phase = (t + 19.7).truncatingRemainder(dividingBy: 78)
        guard phase < 0.75 else { return }
        let p = CGFloat(phase / 0.75)
        let head = CGPoint(x: 0.20 + 0.24 * p, y: 0.06 + 0.10 * p)
        g.line(&c, CGPoint(x: head.x - 0.06, y: head.y - 0.025), head, Color.white.opacity(0.7 * Double(1 - p)), width: 1.5 * g.u)
    }

    private func clouds(_ c: inout GraphicsContext, _ g: Geo) {
        for i in 0..<3 {
            let speed = 7 + Double(i) * 2
            let x = CGFloat(fract((Double(i) * 0.37) + t * speed / Double(g.w + 120))) * (g.w + 120) - 90
            let y = g.h * (0.16 + CGFloat(i % 2) * 0.10)
            let color = Color.white.opacity(weather == .rain ? 0.42 : 0.5)
            let s = g.u
            g.abs(&c, x, y + 8 * s, 52 * s, 10 * s, color); g.abs(&c, x + 9 * s, y, 26 * s, 18 * s, color); g.abs(&c, x + 26 * s, y + 3 * s, 22 * s, 15 * s, color)
        }
    }

    private func constellation(_ c: inout GraphicsContext, _ g: Geo, index: Int, progress: Double) {
        let alpha = sin(Double.pi * progress)
        let points = TinyConstellation.all[index].stars
        for (a, b) in zip(points, points.dropFirst()) {
            g.line(&c, a, b, Pal.cyan.opacity(0.35 * alpha), width: 3.5 * g.u)
            g.line(&c, a, b, Color.white.opacity(0.85 * alpha), width: 1.2 * g.u)
        }
        for p in points {
            c.fill(Path(ellipseIn: CGRect(x: p.x * g.w - 3.5 * g.u, y: p.y * g.h - 3.5 * g.u, width: 7 * g.u, height: 7 * g.u)), with: .color(Pal.warm.opacity(alpha)))
            c.fill(Path(ellipseIn: CGRect(x: p.x * g.w - 1.5 * g.u, y: p.y * g.h - 1.5 * g.u, width: 3 * g.u, height: 3 * g.u)), with: .color(Color.white.opacity(alpha)))
        }
    }

    private func shootingStar(_ c: inout GraphicsContext, _ g: Geo, _ star: TinyShootingStar) {
        let age = now - star.born
        guard age >= 0, age < 1.1 else { return }
        let p = CGFloat(age / 1.1)
        let dx = cos(star.angle) * 0.42 * p, dy = sin(star.angle) * 0.42 * p * g.w / g.h
        let head = CGPoint(x: star.x + dx, y: star.y + dy)
        let tail = CGPoint(x: head.x - cos(star.angle) * 0.12, y: head.y - sin(star.angle) * 0.12 * g.w / g.h)
        let fade = Double(1 - p)
        g.line(&c, tail, head, Color(red: 1, green: 0.98, blue: 0.86).opacity(0.75 * fade), width: 2 * g.u)
        g.glow(&c, head.x, head.y, radius: 0.025, .white, 0.9 * fade)
    }

    // MARK: - Outdoor scenes

    private func outdoor(_ c: inout GraphicsContext, _ g: Geo) {
        sky(&c, g, in: CGRect(x: 0, y: 0, width: 1, height: 1))
        let dim: Double = night ? 0.55 : 1
        let far = night ? Color(red: 0.24, green: 0.28, blue: 0.38) : Color(red: 0.48, green: 0.63, blue: 0.60)
        let near = night ? Color(red: 0.20, green: 0.29, blue: 0.30) : Color(red: 0.43, green: 0.60, blue: 0.43)
        if scene == .campfire {
            for i in 0..<9 { pine(&c, g, x: CGFloat(i) * 0.12 - 0.02, base: 0.62, height: 0.18 + CGFloat(i % 3) * 0.05, color: Color(red: 0.11, green: 0.18, blue: 0.20)) }
        } else if scene == .scooter || scene == .momo {
            skyline(&c, g)
        } else {
            g.rect(&c, 0, 0.50, 1, 0.16, far)
            for i in 0..<8 { g.oval(&c, CGFloat(i) * 0.15 - 0.08, 0.45 + CGFloat(i % 3) * 0.02, 0.26, 0.16, far) }
        }
        g.rect(&c, 0, 0.61, 1, 0.39, near)
        if weather == .snow { g.rect(&c, 0, 0.61, 1, 0.012, Color.white.opacity(0.55)) }
        for i in 0..<18 {
            let x = CGFloat(hash(i, 2.2)), y = 0.63 + CGFloat(hash(i, 4.4)) * 0.34
            let sway = CGFloat(sin(t * 0.9 + Double(i))) * 1.2
            g.abs(&c, x * g.w + sway, y * g.h, 2 * g.u, 6 * g.u, Color(red: 0.70, green: 0.82, blue: 0.52).opacity(0.55 * dim))
        }
        switch scene {
        case .meadow: meadow(&c, g)
        case .tree: oldTree(&c, g)
        case .nightWalk: lanternStroll(&c, g)
        case .momo: momoStall(&c, g)
        case .scooter: eveningRoad(&c, g)
        case .twilight: twilightHill(&c, g)
        case .campfire: campfire(&c, g)
        default: break
        }
    }

    private func pine(_ c: inout GraphicsContext, _ g: Geo, x: CGFloat, base: CGFloat, height: CGFloat, color: Color) {
        g.rect(&c, x + 0.045, base - 0.03, 0.012, 0.03, Pal.woodDark)
        for tier in 0..<3 {
            let top = base - height + CGFloat(tier) * height * 0.28
            let half = 0.035 + CGFloat(tier) * 0.017
            g.poly(&c, [CGPoint(x: x + 0.05, y: top), CGPoint(x: x + 0.05 + half, y: top + height * 0.42), CGPoint(x: x + 0.05 - half, y: top + height * 0.42)], color)
        }
    }

    private func skyline(_ c: inout GraphicsContext, _ g: Geo) {
        let tone = night ? Color(red: 0.16, green: 0.17, blue: 0.29) : Color(red: 0.55, green: 0.55, blue: 0.68)
        for i in 0..<12 {
            let x = CGFloat(i) * 0.09 - 0.02, height = 0.08 + CGFloat(hash(i, 7.7)) * 0.12
            g.rect(&c, x, 0.61 - height, 0.07, height, tone)
            if night { for row in 0..<3 where hash(i * 3 + row, 1.3) > 0.4 { g.rect(&c, x + 0.015, 0.61 - height + 0.02 + CGFloat(row) * 0.03, 0.012, 0.012, Pal.warm.opacity(0.8)) } }
        }
        if scene == .scooter {
            // The distant temple spire glows in the evening.
            g.glow(&c, 0.82, 0.38, radius: 0.10, Pal.warm, night ? 0.35 : 0.18)
            g.poly(&c, [CGPoint(x: 0.82, y: 0.30), CGPoint(x: 0.86, y: 0.44), CGPoint(x: 0.78, y: 0.44)], tone.opacity(0.95))
            g.rect(&c, 0.775, 0.44, 0.09, 0.17, tone)
        }
    }

    private func meadow(_ c: inout GraphicsContext, _ g: Geo) {
        // Cottage with porch, chimney smoke and a glowing window after dark.
        g.rect(&c, 0.04, 0.50, 0.22, 0.16, Color(red: 0.93, green: 0.84, blue: 0.72))
        g.poly(&c, [CGPoint(x: 0.02, y: 0.50), CGPoint(x: 0.15, y: 0.39), CGPoint(x: 0.28, y: 0.50)], Color(red: 0.70, green: 0.36, blue: 0.34))
        g.rect(&c, 0.20, 0.40, 0.03, 0.06, Pal.woodDark)
        for i in 0..<3 {
            let rise = CGFloat(fract(t * 0.18 + Double(i) / 3))
            g.oval(&c, 0.205 + rise * 0.03, 0.39 - rise * 0.10, 0.025 + rise * 0.02, 0.018 + rise * 0.012, Color.white.opacity(0.35 * Double(1 - rise)))
        }
        g.rect(&c, 0.07, 0.55, 0.05, 0.11, Pal.wood)
        g.rect(&c, 0.15, 0.54, 0.07, 0.05, night ? Pal.warm : Pal.glass)
        if night { g.glow(&c, 0.185, 0.565, radius: 0.06, Pal.glow, 0.35) }
        g.rect(&c, 0.24, 0.495, 0.10, 0.012, Pal.woodDark)
        // Porch wind chimes swing harder right after a tap.
        let r = TinyHotspot.windChime.rect
        let energy = max(0.15, 1 - (now - stage.chimeSwingAt) / 2.5)
        for i in 0..<4 {
            let x = r.minX + 0.02 + CGFloat(i) * 0.018
            let swing = CGFloat(sin(t * 3 + Double(i)) * 0.006 * energy * 3)
            g.line(&c, CGPoint(x: x, y: 0.50), CGPoint(x: x + swing, y: 0.53 + CGFloat(i % 2) * 0.02), Pal.ink.opacity(0.5), width: 1)
            g.rect(&c, x + swing - 0.003, 0.53 + CGFloat(i % 2) * 0.02, 0.006, 0.035, Color(red: 0.78, green: 0.86, blue: 0.92))
        }
        // Sakura tree.
        g.rect(&c, 0.80, 0.34, 0.035, 0.30, Pal.woodDark)
        for i in 0..<7 {
            let x = 0.68 + CGFloat(i % 4) * 0.075, y = 0.16 + CGFloat(i / 4) * 0.10 + CGFloat(i % 2) * 0.02
            g.oval(&c, x, y, 0.13, 0.11, Color(red: 0.98, green: 0.74, blue: 0.80).opacity(0.95))
        }
        for i in 0..<10 { g.rect(&c, 0.70 + CGFloat(hash(i, 6.1)) * 0.26, 0.18 + CGFloat(hash(i, 8.3)) * 0.18, 0.012, 0.01, Color(red: 1, green: 0.88, blue: 0.91)) }
        flowerBed(&c, g, TinyHotspot.flowers.rect)
        // Picnic basket.
        g.rect(&c, 0.85, 0.73, 0.09, 0.05, Pal.woodLight); g.rect(&c, 0.85, 0.73, 0.09, 0.012, Color(red: 0.86, green: 0.36, blue: 0.40))
    }

    private func flowerBed(_ c: inout GraphicsContext, _ g: Geo, _ r: CGRect) {
        let colors = [Color(red: 0.98, green: 0.78, blue: 0.55), Color(red: 0.95, green: 0.60, blue: 0.70), Color(red: 0.80, green: 0.70, blue: 0.95), Color.white]
        for i in 0..<9 {
            let x = r.minX + CGFloat(i) / 9 * r.width + 0.01
            let y = r.minY + CGFloat(i % 3) * 0.03 + 0.02
            let sway = CGFloat(sin(t * 0.8 + Double(i) * 1.7)) * 0.004
            g.rect(&c, x + sway, y, 0.006, 0.04, Pal.leaf)
            let petal = colors[i % colors.count]
            g.rect(&c, x + sway - 0.008, y - 0.008, 0.022, 0.012, petal)
            g.rect(&c, x + sway - 0.002, y - 0.016, 0.01, 0.028, petal)
            g.rect(&c, x + sway, y - 0.006, 0.006, 0.008, Color(red: 1, green: 0.88, blue: 0.4))
        }
    }

    private func oldTree(_ c: inout GraphicsContext, _ g: Geo) {
        g.rect(&c, 0.77, 0.30, 0.08, 0.36, Pal.woodDark)
        g.rect(&c, 0.66, 0.33, 0.12, 0.02, Pal.woodDark)
        for i in 0..<8 {
            let x = 0.62 + CGFloat(i % 4) * 0.09, y = 0.12 + CGFloat(i / 4) * 0.11
            let sway = CGFloat(sin(t * 0.5 + Double(i))) * 0.004
            g.oval(&c, x + sway, y, 0.14, 0.12, i % 2 == 0 ? Pal.leaf : Pal.leafDark)
        }
        g.heart(&c, 0.81 * g.w, 0.47 * g.h, cell: 2.2 * g.u, Color(red: 0.85, green: 0.62, blue: 0.52))
        // Rope swing.
        let swing = CGFloat(sin(t * 1.1)) * 0.012
        g.line(&c, CGPoint(x: 0.67, y: 0.345), CGPoint(x: 0.67 + swing, y: 0.58), Pal.woodLight, width: 1.2 * g.u)
        g.line(&c, CGPoint(x: 0.73, y: 0.345), CGPoint(x: 0.73 + swing, y: 0.58), Pal.woodLight, width: 1.2 * g.u)
        g.rect(&c, 0.66 + swing, 0.58, 0.08, 0.012, Pal.wood)
        flowerBed(&c, g, CGRect(x: 0.04, y: 0.72, width: 0.26, height: 0.10))
    }

    private func streetLamp(_ c: inout GraphicsContext, _ g: Geo, x: CGFloat, top: CGFloat, base: CGFloat) {
        g.rect(&c, x - 0.006, top, 0.012, base - top, Color(red: 0.25, green: 0.24, blue: 0.27))
        g.rect(&c, x - 0.025, top - 0.05, 0.05, 0.05, Color(red: 0.30, green: 0.28, blue: 0.30))
        g.rect(&c, x - 0.018, top - 0.044, 0.036, 0.038, stage.streetLampsOn ? Pal.warm : Color(red: 0.45, green: 0.45, blue: 0.48))
        if stage.streetLampsOn {
            let flicker = 0.9 + 0.1 * sin(t * 13 + Double(x) * 40)
            g.glow(&c, x, top - 0.02, radius: 0.13, Pal.glow, 0.32 * flicker)
            g.oval(&c, x - 0.08, base - 0.01, 0.16, 0.03, Pal.glow.opacity(0.18))
        }
    }

    private func lanternStroll(_ c: inout GraphicsContext, _ g: Geo) {
        // Cobblestone path.
        g.poly(&c, [CGPoint(x: 0.38, y: 0.61), CGPoint(x: 0.62, y: 0.61), CGPoint(x: 0.90, y: 1), CGPoint(x: 0.10, y: 1)], Color(red: 0.42, green: 0.40, blue: 0.44))
        for i in 0..<22 {
            let y = 0.63 + CGFloat(i / 4) * 0.065
            let spread = (y - 0.61) * 1.3
            let x = 0.5 - 0.12 - spread * 0.5 + CGFloat(i % 4) * (0.06 + spread * 0.25)
            g.rect(&c, x, y, 0.04 + spread * 0.1, 0.018, Color(red: 0.52, green: 0.50, blue: 0.54))
        }
        let lamp = TinyHotspot.streetLamp.rect
        streetLamp(&c, g, x: lamp.midX, top: lamp.minY + 0.06, base: lamp.maxY)
        streetLamp(&c, g, x: 0.64, top: 0.40, base: 0.62)
        // Paper lantern string between the lamps.
        for i in 0..<6 {
            let p = CGFloat(i + 1) / 7
            let x = lamp.midX + (0.64 - lamp.midX) * p
            let y = 0.33 + sin(p * .pi) * 0.05 + CGFloat(sin(t * 1.2 + Double(i))) * 0.003
            g.oval(&c, x - 0.015, y, 0.03, 0.035, Color(red: 0.92, green: 0.40, blue: 0.36))
            g.glow(&c, x, y + 0.017, radius: 0.03, Pal.glow, 0.35)
        }
        // Telescope on the overlook.
        let r = TinyHotspot.telescope.rect
        g.line(&c, CGPoint(x: r.midX, y: r.minY + 0.08), CGPoint(x: r.minX + 0.02, y: r.maxY), Pal.woodDark, width: 1.5 * g.u)
        g.line(&c, CGPoint(x: r.midX, y: r.minY + 0.08), CGPoint(x: r.maxX - 0.02, y: r.maxY), Pal.woodDark, width: 1.5 * g.u)
        var scope = c
        scope.translateBy(x: r.midX * g.w, y: (r.minY + 0.07) * g.h)
        scope.rotate(by: .degrees(-28))
        scope.fill(Path(CGRect(x: -0.07 * g.w, y: -4 * g.u, width: 0.12 * g.w, height: 8 * g.u)), with: .color(Color(red: 0.78, green: 0.62, blue: 0.38)))
        scope.fill(Path(CGRect(x: 0.04 * g.w, y: -5 * g.u, width: 0.015 * g.w, height: 10 * g.u)), with: .color(Pal.woodDark))
        fireflies(&c, g, count: 8, region: CGRect(x: 0.05, y: 0.55, width: 0.9, height: 0.3))
    }

    private func fireflies(_ c: inout GraphicsContext, _ g: Geo, count: Int, region: CGRect) {
        guard night else { return }
        for i in 0..<count {
            let x = region.minX + CGFloat(fract(hash(i, 1.1) + sin(t * 0.21 + Double(i)) * 0.05)) * region.width
            let y = region.minY + CGFloat(fract(hash(i, 2.9) + cos(t * 0.17 + Double(i)) * 0.05)) * region.height
            let pulse = 0.4 + 0.6 * max(0, sin(t * 2 + Double(i) * 1.7))
            g.glow(&c, x, y, radius: 0.018, Color(red: 0.95, green: 1, blue: 0.6), 0.6 * pulse)
        }
    }

    private func momoStall(_ c: inout GraphicsContext, _ g: Geo) {
        g.rect(&c, 0, 0.62, 1, 0.05, Color(red: 0.36, green: 0.35, blue: 0.40))
        streetLamp(&c, g, x: 0.08, top: 0.38, base: 0.63)
        let r = TinyHotspot.momoCart.rect
        // Striped canopy.
        for i in 0..<8 { g.rect(&c, r.minX + CGFloat(i) * r.width / 8, r.minY, r.width / 8, 0.06, i % 2 == 0 ? Color(red: 0.86, green: 0.36, blue: 0.36) : Pal.cream) }
        g.rect(&c, r.minX + 0.01, r.minY + 0.06, 0.012, 0.16, Pal.woodDark); g.rect(&c, r.maxX - 0.022, r.minY + 0.06, 0.012, 0.16, Pal.woodDark)
        // Neon sign.
        let buzz = 0.75 + 0.25 * sin(t * 9)
        g.glow(&c, r.midX, r.minY + 0.095, radius: 0.08, Pal.pink, 0.35 * buzz)
        c.draw(Text("MOMO").font(.system(size: 11 * g.u, weight: .heavy, design: .monospaced)).foregroundColor(Pal.pink.opacity(buzz)), at: CGPoint(x: r.midX * g.w, y: (r.minY + 0.095) * g.h))
        g.rect(&c, r.minX, r.minY + 0.16, r.width, 0.03, Pal.woodLight)
        g.rect(&c, r.minX + 0.01, r.minY + 0.19, r.width - 0.02, 0.10, Pal.wood)
        g.oval(&c, r.minX + 0.03, r.maxY - 0.03, 0.05, 0.05 * g.w / g.h, Pal.ink); g.oval(&c, r.maxX - 0.08, r.maxY - 0.03, 0.05, 0.05 * g.w / g.h, Pal.ink)
        // Bamboo steamers with constant gentle steam.
        for i in 0..<2 { g.rect(&c, r.minX + 0.05 + CGFloat(i) * 0.09, r.minY + 0.115 - CGFloat(i) * 0.0, 0.07, 0.045, Color(red: 0.84, green: 0.70, blue: 0.46)) }
        for i in 0..<3 {
            let rise = CGFloat(fract(t * 0.5 + Double(i) / 3))
            g.oval(&c, r.minX + 0.07 + CGFloat(i) * 0.04, r.minY + 0.10 - rise * 0.08, 0.025, 0.02, Color.white.opacity(0.4 * Double(1 - rise)))
        }
        fireflies(&c, g, count: 5, region: CGRect(x: 0.05, y: 0.4, width: 0.5, height: 0.3))
    }

    private func eveningRoad(_ c: inout GraphicsContext, _ g: Geo) {
        g.rect(&c, 0, 0.70, 1, 0.22, Color(red: 0.26, green: 0.26, blue: 0.31))
        let scroll = CGFloat(fract(t * 0.9))
        for i in 0..<7 { g.rect(&c, CGFloat(i) * 0.18 - scroll * 0.18, 0.805, 0.08, 0.01, Pal.cream.opacity(0.75)) }
        // Roadside lamps drift past.
        for i in 0..<3 {
            let x = CGFloat(fract(Double(i) / 3 - t * 0.12)) * 1.3 - 0.15
            g.rect(&c, x, 0.46, 0.008, 0.24, Color(red: 0.22, green: 0.22, blue: 0.26))
            g.rect(&c, x - 0.01, 0.45, 0.028, 0.012, Pal.warm)
            if night || sunset { g.glow(&c, x + 0.004, 0.46, radius: 0.08, Pal.glow, 0.3) }
        }
    }

    private func twilightHill(_ c: inout GraphicsContext, _ g: Geo) {
        g.oval(&c, -0.1, 0.58, 1.2, 0.3, Color(red: 0.36, green: 0.44, blue: 0.38))
        fireflies(&c, g, count: 10, region: CGRect(x: 0.05, y: 0.45, width: 0.9, height: 0.4))
        flowerBed(&c, g, CGRect(x: 0.70, y: 0.74, width: 0.26, height: 0.1))
    }

    private func campfire(_ c: inout GraphicsContext, _ g: Geo) {
        // A-frame canvas tent with its lantern.
        g.poly(&c, [CGPoint(x: 0.62, y: 0.66), CGPoint(x: 0.76, y: 0.42), CGPoint(x: 0.92, y: 0.66)], Color(red: 0.84, green: 0.76, blue: 0.60))
        g.poly(&c, [CGPoint(x: 0.71, y: 0.66), CGPoint(x: 0.76, y: 0.52), CGPoint(x: 0.81, y: 0.66)], Color(red: 0.30, green: 0.24, blue: 0.24))
        let lantern = TinyHotspot.lantern.rect
        g.line(&c, CGPoint(x: lantern.midX, y: lantern.minY), CGPoint(x: lantern.midX, y: lantern.minY + 0.04), Pal.ink, width: 1)
        g.rect(&c, lantern.midX - 0.018, lantern.minY + 0.04, 0.036, 0.05, stage.lanternOn ? Pal.warm : Color(red: 0.45, green: 0.45, blue: 0.48))
        g.rect(&c, lantern.midX - 0.022, lantern.minY + 0.035, 0.044, 0.008, Pal.woodDark)
        if stage.lanternOn { g.glow(&c, lantern.midX, lantern.minY + 0.065, radius: 0.14, Pal.glow, 0.4) }
        // Log bench and guitar.
        g.rect(&c, 0.06, 0.745, 0.32, 0.04, Pal.wood); g.rect(&c, 0.06, 0.745, 0.32, 0.01, Pal.woodLight)
        let guitar = TinyHotspot.guitar.rect
        let strum = max(0, 1 - (now - stage.guitarStrumAt) / 0.6)
        var neck = c
        neck.translateBy(x: guitar.midX * g.w, y: guitar.maxY * g.h)
        neck.rotate(by: .degrees(-12 + 3 * strum * sin(now * 40)))
        neck.fill(Path(CGRect(x: -2 * g.u, y: -0.22 * g.h, width: 4 * g.u, height: 0.12 * g.h)), with: .color(Pal.woodDark))
        neck.fill(Path(ellipseIn: CGRect(x: -11 * g.u, y: -0.12 * g.h, width: 22 * g.u, height: 0.07 * g.h)), with: .color(Color(red: 0.82, green: 0.55, blue: 0.32)))
        neck.fill(Path(ellipseIn: CGRect(x: -9 * g.u, y: -0.07 * g.h, width: 18 * g.u, height: 0.07 * g.h)), with: .color(Color(red: 0.82, green: 0.55, blue: 0.32)))
        neck.fill(Path(ellipseIn: CGRect(x: -3 * g.u, y: -0.08 * g.h, width: 6 * g.u, height: 6 * g.u)), with: .color(Pal.ink))
        // Plaid fleece blanket.
        let blanket = TinyHotspot.plaidBlanket.rect
        g.rect(&c, blanket.minX, blanket.minY + 0.02, blanket.width, blanket.height - 0.03, Color(red: 0.72, green: 0.25, blue: 0.27))
        for i in 0..<5 { g.rect(&c, blanket.minX + CGFloat(i) * blanket.width / 5 + 0.02, blanket.minY + 0.02, 0.012, blanket.height - 0.03, Color(red: 0.25, green: 0.22, blue: 0.30).opacity(0.5)) }
        g.rect(&c, blanket.minX, blanket.minY + 0.05, blanket.width, 0.01, Color(red: 0.25, green: 0.22, blue: 0.30).opacity(0.5))
        // Stone fire pit with animated flames.
        let pit = TinyHotspot.firePit.rect
        let stoked = max(0, 1 - (now - stage.fireStokedAt) / 1.5)
        g.glow(&c, pit.midX, pit.midY, radius: 0.30 + CGFloat(stoked) * 0.08, Pal.glow, 0.28 + 0.06 * sin(t * 7))
        for i in 0..<7 {
            let a = Double(i) / 7 * .pi * 2
            g.oval(&c, pit.midX + CGFloat(cos(a)) * 0.08 - 0.02, pit.maxY - 0.04 + CGFloat(sin(a)) * 0.02, 0.04, 0.025, Pal.stone)
        }
        g.rect(&c, pit.midX - 0.06, pit.maxY - 0.05, 0.12, 0.015, Pal.woodDark)
        for i in 0..<5 {
            let flicker = CGFloat(0.6 + 0.4 * sin(t * (9 + Double(i)) + Double(i) * 2)) * (1 + CGFloat(stoked) * 0.6)
            let x = pit.midX - 0.045 + CGFloat(i) * 0.022
            let height = 0.05 * flicker * (i == 2 ? 1.5 : 1)
            g.rect(&c, x, pit.maxY - 0.05 - height, 0.02, height, Color(red: 1, green: 0.52, blue: 0.22))
            g.rect(&c, x + 0.004, pit.maxY - 0.05 - height * 0.6, 0.012, height * 0.6, Color(red: 1, green: 0.84, blue: 0.40))
        }
        for i in 0..<5 {
            let rise = CGFloat(fract(t * 0.6 + Double(i) / 5))
            let drift: CGFloat = CGFloat(sin(t + Double(i) * 2)) * 0.04
            let emberX: CGFloat = (pit.midX + drift) * g.w
            let emberY: CGFloat = (pit.maxY - 0.08 - rise * 0.22) * g.h
            g.abs(&c, emberX, emberY, 2 * g.u, 2 * g.u, Color(red: 1, green: 0.68, blue: 0.3).opacity(Double(1 - rise)))
        }
    }

    private func scooterForeground(_ c: inout GraphicsContext, _ g: Geo) {
        let r = TinyHotspot.scooter.rect
        let bob = CGFloat(sin(t * 8)) * 0.003
        let honk = max(0, 1 - (now - stage.hornAt) / 0.8)
        let body = Color(red: 0.95, green: 0.62, blue: 0.40)
        g.rect(&c, r.minX + 0.08, 0.76 + bob, 0.30, 0.05, body)
        g.rect(&c, r.minX + 0.30, 0.66 + bob, 0.05, 0.11, body)
        g.rect(&c, r.minX + 0.30, 0.65 + bob, 0.09, 0.012, Pal.ink)
        g.rect(&c, r.minX + 0.36, 0.66 + bob, 0.04, 0.03, Pal.warm.opacity(0.9 + 0.1 * honk))
        g.glow(&c, r.minX + 0.40, 0.675 + bob, radius: 0.08 + CGFloat(honk) * 0.04, Pal.warm, 0.35)
        let wheel = 0.075 * g.w / g.h
        g.oval(&c, r.minX + 0.07, 0.80, 0.075, wheel, Pal.ink); g.oval(&c, r.minX + 0.32, 0.80, 0.075, wheel, Pal.ink)
        g.oval(&c, r.minX + 0.09, 0.80 + wheel * 0.25, 0.035, wheel * 0.5, Pal.steel); g.oval(&c, r.minX + 0.34, 0.80 + wheel * 0.25, 0.035, wheel * 0.5, Pal.steel)
        // Mochi rides in the front basket.
        g.rect(&c, r.minX + 0.35, 0.69 + bob, 0.07, 0.04, Pal.woodLight)
        drawCatHead(&c, g, x: (r.minX + 0.385) * g.w, y: (0.68 + bob) * g.h, s: g.u * 0.9, sleepy: false)
    }

    // MARK: - Indoor scenes

    private func room(_ c: inout GraphicsContext, _ g: Geo, wall: Color, floor: Color, floorTop: CGFloat = 0.66) {
        g.rect(&c, 0, 0, 1, floorTop, wall)
        g.rect(&c, 0, floorTop, 1, 1 - floorTop, floor)
        for i in 0..<8 { g.rect(&c, 0, floorTop + CGFloat(i) * 0.045, 1, 0.003, Color.black.opacity(0.08)) }
        g.rect(&c, 0, floorTop - 0.012, 1, 0.012, Pal.woodDark.opacity(0.7))
    }

    private func window(_ c: inout GraphicsContext, _ g: Geo, _ r: CGRect) {
        g.rect(&c, r.minX - 0.012, r.minY - 0.012, r.width + 0.024, r.height + 0.024, Pal.woodDark)
        var pane = c
        pane.clip(to: Path(CGRect(x: r.minX * g.w, y: r.minY * g.h, width: r.width * g.w, height: r.height * g.h)))
        sky(&pane, g, in: r)
        g.rect(&pane, r.minX, r.maxY - r.height * 0.25, r.width, r.height * 0.25, (night ? Color(red: 0.20, green: 0.28, blue: 0.30) : Color(red: 0.50, green: 0.66, blue: 0.48)))
        g.rect(&c, r.midX - 0.004, r.minY, 0.008, r.height, Pal.woodDark)
        g.rect(&c, r.minX, r.midY - 0.004, r.width, 0.008, Pal.woodDark)
    }

    private func kitchen(_ c: inout GraphicsContext, _ g: Geo) {
        room(&c, g, wall: Color(red: 0.96, green: 0.90, blue: 0.80), floor: Color(red: 0.78, green: 0.50, blue: 0.40))
        for i in 0..<14 { for j in 0..<4 where (i + j) % 2 == 0 { g.rect(&c, CGFloat(i) * 0.075, 0.66 + CGFloat(j) * 0.085, 0.075, 0.085, Color(red: 0.86, green: 0.62, blue: 0.50)) } }
        g.rect(&c, 0, 0.48, 1, 0.18, Color(red: 0.62, green: 0.72, blue: 0.60))
        let win = windows[0]
        window(&c, g, win)
        g.rect(&c, win.minX - 0.03, win.minY - 0.02, 0.05, win.height + 0.04, Color(red: 0.93, green: 0.55, blue: 0.58).opacity(0.8))
        g.rect(&c, win.maxX - 0.02, win.minY - 0.02, 0.05, win.height + 0.04, Color(red: 0.93, green: 0.55, blue: 0.58).opacity(0.8))
        // Pendant lamp.
        g.rect(&c, 0.495, 0, 0.006, 0.06, Pal.ink); g.poly(&c, [CGPoint(x: 0.46, y: 0.09), CGPoint(x: 0.54, y: 0.09), CGPoint(x: 0.51, y: 0.06), CGPoint(x: 0.49, y: 0.06)], Color(red: 0.36, green: 0.52, blue: 0.48))
        g.glow(&c, 0.5, 0.10, radius: 0.12, Pal.glow, 0.25)
        // Counter run with stove, cutting board and farmhouse sink.
        g.rect(&c, 0.03, 0.555, 0.70, 0.11, Color(red: 0.88, green: 0.82, blue: 0.72))
        g.rect(&c, 0.03, 0.545, 0.70, 0.015, Pal.woodLight)
        for i in 0..<5 { g.rect(&c, 0.05 + CGFloat(i) * 0.14, 0.58, 0.11, 0.07, Color(red: 0.80, green: 0.74, blue: 0.64)) }
        g.rect(&c, 0.05, 0.54, 0.28, 0.01, Pal.ink)
        let kettle = TinyHotspot.kettle.rect
        g.oval(&c, kettle.minX + 0.02, kettle.minY + 0.04, 0.10, 0.08, Color(red: 0.80, green: 0.48, blue: 0.32))
        g.rect(&c, kettle.minX + 0.05, kettle.minY + 0.03, 0.04, 0.015, Pal.woodDark)
        g.rect(&c, kettle.maxX - 0.02, kettle.minY + 0.06, 0.03, 0.012, Color(red: 0.80, green: 0.48, blue: 0.32))
        let whistle = now - stage.kettleSteamAt < 2.2
        steamPlume(&c, g, x: kettle.maxX + 0.01, y: kettle.minY + 0.05, strength: whistle ? 1 : 0.35)
        let pot = TinyHotspot.stewPot.rect
        g.rect(&c, pot.minX + 0.01, pot.minY + 0.04, 0.11, 0.07, Pal.steel); g.rect(&c, pot.minX, pot.minY + 0.035, 0.13, 0.012, Color(red: 0.55, green: 0.57, blue: 0.62))
        steamPlume(&c, g, x: pot.midX, y: pot.minY + 0.03, strength: 0.4)
        let board = TinyHotspot.cuttingBoard.rect
        g.rect(&c, board.minX, board.minY + 0.03, board.width, 0.02, Pal.woodLight)
        g.rect(&c, board.minX + 0.02, board.minY + 0.02, 0.03, 0.012, Color(red: 0.96, green: 0.56, blue: 0.24))
        g.rect(&c, board.minX + 0.07, board.minY + 0.02, 0.02, 0.012, Pal.leaf)
        let sink = TinyHotspot.sink.rect
        g.rect(&c, sink.minX, sink.minY + 0.05, sink.width, 0.06, Color.white.opacity(0.92))
        g.rect(&c, sink.midX - 0.004, sink.minY, 0.008, 0.05, Pal.steel); g.rect(&c, sink.midX - 0.004, sink.minY, 0.03, 0.008, Pal.steel)
        // Pastel fridge covered in love notes.
        let fridge = TinyHotspot.fridge.rect
        g.rect(&c, fridge.minX, fridge.minY, fridge.width, fridge.height, Color(red: 0.70, green: 0.88, blue: 0.82))
        g.rect(&c, fridge.minX, fridge.minY + fridge.height * 0.36, fridge.width, 0.008, Color(red: 0.55, green: 0.72, blue: 0.67))
        g.rect(&c, fridge.maxX - 0.03, fridge.minY + 0.05, 0.01, 0.08, Pal.steel); g.rect(&c, fridge.maxX - 0.03, fridge.minY + 0.22, 0.01, 0.12, Pal.steel)
        let noteColors = [Color(red: 1, green: 0.93, blue: 0.60), Color(red: 1, green: 0.78, blue: 0.82), Color(red: 0.80, green: 0.90, blue: 1)]
        let noteLift = now - stage.fridgeNoteAt < 2.5 ? 0.01 : 0
        for i in 0..<4 {
            let x = fridge.minX + 0.025 + CGFloat(i % 2) * 0.07, y = fridge.minY + 0.21 + CGFloat(i / 2) * 0.09 - CGFloat(noteLift)
            g.rect(&c, x, y, 0.055, 0.05, noteColors[i % 3])
            g.heart(&c, (x + 0.0275) * g.w, (y + 0.025) * g.h, cell: 1.4 * g.u, Pal.pink)
        }
        // Mochi's dish.
        g.oval(&c, 0.66, 0.89, 0.07, 0.025, Color(red: 0.96, green: 0.72, blue: 0.40))
    }

    private func steamPlume(_ c: inout GraphicsContext, _ g: Geo, x: CGFloat, y: CGFloat, strength: Double) {
        for i in 0..<4 {
            let rise = CGFloat(fract(t * 0.6 + Double(i) / 4))
            let sway = CGFloat(sin(t * 2 + Double(i))) * 0.01
            g.oval(&c, x + sway - 0.012, y - rise * 0.12, 0.024 + rise * 0.02, 0.018 + rise * 0.015, Color.white.opacity(0.45 * strength * Double(1 - rise)))
        }
    }

    private func living(_ c: inout GraphicsContext, _ g: Geo) {
        room(&c, g, wall: Color(red: 0.86, green: 0.74, blue: 0.68), floor: Color(red: 0.62, green: 0.45, blue: 0.36))
        g.rect(&c, 0, 0.54, 1, 0.12, Color(red: 0.78, green: 0.62, blue: 0.58))
        window(&c, g, windows[0])
        // Rug and couch with the snuggle throw.
        g.oval(&c, 0.16, 0.76, 0.68, 0.14, Color(red: 0.86, green: 0.70, blue: 0.56))
        let couch = TinyHotspot.blanket.rect
        g.rect(&c, couch.minX, couch.minY, couch.width, 0.10, Color(red: 0.55, green: 0.42, blue: 0.62))
        g.rect(&c, couch.minX - 0.03, couch.minY + 0.05, 0.05, 0.14, Color(red: 0.48, green: 0.36, blue: 0.55))
        g.rect(&c, couch.maxX - 0.02, couch.minY + 0.05, 0.05, 0.14, Color(red: 0.48, green: 0.36, blue: 0.55))
        g.rect(&c, couch.minX, couch.minY + 0.10, couch.width, 0.08, Color(red: 0.62, green: 0.49, blue: 0.70))
        // Monstera planter beside the couch.
        g.rect(&c, 0.18, 0.62, 0.05, 0.05, Color(red: 0.82, green: 0.56, blue: 0.42))
        for i in 0..<4 { g.oval(&c, 0.15 + CGFloat(i % 2) * 0.05, 0.53 + CGFloat(i / 2) * 0.04, 0.06, 0.05, Pal.leafDark) }
        // Side table and aromatherapy candle.
        let candle = TinyHotspot.candle.rect
        g.rect(&c, candle.minX, candle.minY + 0.06, candle.width, 0.012, Pal.wood); g.rect(&c, candle.midX - 0.006, candle.minY + 0.07, 0.012, 0.06, Pal.wood)
        g.rect(&c, candle.midX - 0.018, candle.minY + 0.025, 0.036, 0.035, Color(red: 0.86, green: 0.80, blue: 0.95))
        if stage.candleLit {
            let flick = 0.8 + 0.2 * sin(t * 15)
            g.rect(&c, candle.midX - 0.004, candle.minY + 0.005, 0.008, 0.02, Pal.warm.opacity(flick))
            g.glow(&c, candle.midX, candle.minY + 0.015, radius: 0.09, Pal.glow, 0.4 * flick)
        }
        // Floor lamp.
        let lamp = TinyHotspot.floorLamp.rect
        g.rect(&c, lamp.midX - 0.005, lamp.minY + 0.06, 0.01, lamp.height - 0.06, Pal.ink)
        g.rect(&c, lamp.midX - 0.03, lamp.maxY - 0.01, 0.06, 0.01, Pal.ink)
        g.poly(&c, [CGPoint(x: lamp.midX - 0.035, y: lamp.minY + 0.08), CGPoint(x: lamp.midX + 0.035, y: lamp.minY + 0.08), CGPoint(x: lamp.midX + 0.02, y: lamp.minY), CGPoint(x: lamp.midX - 0.02, y: lamp.minY)],
               stage.floorLampOn ? Pal.warm : Color(red: 0.82, green: 0.78, blue: 0.70))
        if stage.floorLampOn { g.glow(&c, lamp.midX, lamp.minY + 0.08, radius: 0.24, Pal.glow, 0.35) }
        // Feather wand and Mochi's cardboard box.
        let wand = TinyHotspot.featherWand.rect
        g.line(&c, CGPoint(x: wand.minX, y: wand.maxY - 0.02), CGPoint(x: wand.maxX - 0.03, y: wand.minY + 0.04), Pal.wood, width: 1.4 * g.u)
        let flutter = CGFloat(sin(t * 6)) * 0.006
        g.oval(&c, wand.maxX - 0.05 + flutter, wand.minY + 0.01, 0.04, 0.03, Color(red: 0.96, green: 0.55, blue: 0.62))
        let box = TinyHotspot.catBox.rect
        g.rect(&c, box.minX, box.minY + 0.03, box.width, box.height - 0.03, Color(red: 0.80, green: 0.62, blue: 0.40))
        g.poly(&c, [CGPoint(x: box.minX, y: box.minY + 0.03), CGPoint(x: box.minX - 0.03, y: box.minY), CGPoint(x: box.minX + 0.03, y: box.minY + 0.03)], Color(red: 0.72, green: 0.55, blue: 0.35))
        g.poly(&c, [CGPoint(x: box.maxX, y: box.minY + 0.03), CGPoint(x: box.maxX + 0.03, y: box.minY), CGPoint(x: box.maxX - 0.03, y: box.minY + 0.03)], Color(red: 0.72, green: 0.55, blue: 0.35))
    }

    private func cafe(_ c: inout GraphicsContext, _ g: Geo) {
        room(&c, g, wall: Color(red: 0.62, green: 0.34, blue: 0.30), floor: Color(red: 0.50, green: 0.36, blue: 0.28))
        for row in 0..<16 {
            for col in 0..<10 {
                let x = CGFloat(col) * 0.1 + (row % 2 == 0 ? 0 : 0.05)
                g.rect(&c, x, CGFloat(row) * 0.04, 0.094, 0.034, Color(red: 0.70, green: 0.40, blue: 0.34).opacity(0.6 + 0.4 * hash(row * 10 + col, 0.5)))
            }
        }
        g.rect(&c, 0, 0.50, 1, 0.16, Color(red: 0.46, green: 0.32, blue: 0.24))
        // Rainy window with an umbrella passerby and foggy hearts.
        let win = windows[0]
        window(&c, g, win)
        var outside = c
        outside.clip(to: Path(CGRect(x: win.minX * g.w, y: win.minY * g.h, width: win.width * g.w, height: win.height * g.h)))
        let walker = win.minX - 0.06 + CGFloat(fract(t * 0.04)) * (win.width + 0.12)
        g.rect(&outside, walker, win.maxY - 0.10, 0.025, 0.08, Color(red: 0.25, green: 0.28, blue: 0.40))
        g.poly(&outside, [CGPoint(x: walker - 0.035, y: win.maxY - 0.10), CGPoint(x: walker + 0.0125, y: win.maxY - 0.15), CGPoint(x: walker + 0.06, y: win.maxY - 0.10)], Color(red: 0.95, green: 0.70, blue: 0.30))
        g.rect(&c, win.minX, win.minY, win.width, win.height, Color.white.opacity(0.14))
        for (point, born) in stage.windowHearts {
            let age = now - born
            let alpha = max(0, 1 - age / 10)
            g.glow(&c, point.x, point.y, radius: 0.05, Pal.pink, 0.45 * alpha)
            g.heart(&c, point.x * g.w, point.y * g.h, cell: 2.4 * g.u, Color.white.opacity(0.85 * alpha))
        }
        // Edison pendants.
        for x: CGFloat in [0.18, 0.46, 0.74] {
            g.rect(&c, x - 0.002, 0, 0.004, 0.05, Pal.ink)
            g.oval(&c, x - 0.012, 0.05, 0.024, 0.03, Pal.warm)
            g.glow(&c, x, 0.065, radius: 0.07, Pal.glow, 0.45 + 0.05 * sin(t * 3 + Double(x) * 9))
        }
        // Chalkboard specials.
        let board = TinyHotspot.chalkboard.rect
        g.rect(&c, board.minX, board.minY, board.width, board.height, Pal.woodDark)
        g.rect(&c, board.minX + 0.01, board.minY + 0.012, board.width - 0.02, board.height - 0.024, Color(red: 0.18, green: 0.24, blue: 0.22))
        c.draw(Text("SPECIALS").font(.system(size: 8 * g.u, weight: .bold, design: .monospaced)).foregroundColor(.white.opacity(0.85)), at: CGPoint(x: board.midX * g.w, y: (board.minY + 0.04) * g.h))
        for i in 0..<3 { g.rect(&c, board.minX + 0.025, board.minY + 0.08 + CGFloat(i) * 0.03, board.width * (0.5 + CGFloat(i % 2) * 0.2), 0.006, Color.white.opacity(0.55)) }
        // Espresso bar with Barista Leo.
        let bar = TinyHotspot.barista.rect
        let brewing = now - stage.baristaBrewAt < 1.6
        drawPerson(&c, g, x: (bar.midX + 0.04) * g.w, feet: (bar.maxY + 0.02) * g.h, scale: g.u * 0.95, shirt: Color(red: 0.30, green: 0.36, blue: 0.32),
                   hair: Pal.hairBoy, isGirl: false, pose: brewing ? .cook : .idle, eyeShift: .zero, blush: false, apron: true)
        g.rect(&c, 0.62, 0.50, 0.38, 0.16, Pal.wood); g.rect(&c, 0.62, 0.495, 0.38, 0.012, Pal.woodLight)
        g.rect(&c, bar.minX, bar.minY + 0.12, 0.09, 0.08, Pal.steel)
        g.rect(&c, bar.minX + 0.01, bar.minY + 0.17, 0.02, 0.02, Pal.ink)
        steamPlume(&c, g, x: bar.minX + 0.04, y: bar.minY + 0.12, strength: brewing ? 1 : 0.2)
        // Couple table with latte and croissant.
        g.rect(&c, 0.40, 0.68, 0.20, 0.014, Pal.woodLight); g.rect(&c, 0.495, 0.694, 0.012, 0.12, Pal.woodDark)
        let latte = TinyHotspot.latte.rect
        g.rect(&c, latte.midX - 0.025, latte.maxY - 0.04, 0.05, 0.035, Pal.cream)
        g.oval(&c, latte.midX - 0.022, latte.maxY - 0.046, 0.044, 0.016, Color(red: 0.80, green: 0.62, blue: 0.45))
        let pour = min(1, (now - stage.latteHeartAt) / 0.8)
        g.heart(&c, latte.midX * g.w, (latte.maxY - 0.038) * g.h, cell: 0.9 * g.u * CGFloat(max(0.4, pour)), Color.white.opacity(0.9))
        g.oval(&c, 0.56, 0.665, 0.03, 0.015, Color(red: 0.92, green: 0.70, blue: 0.40))
        // Boba the golden pup with a happy tail.
        let pup = TinyHotspot.boba.rect
        let wag = now - stage.bobaWagAt < 2 ? 18.0 : 4.0
        g.oval(&c, pup.minX + 0.02, pup.minY + 0.06, 0.11, 0.06, Pal.golden)
        g.oval(&c, pup.minX + 0.10, pup.minY + 0.03, 0.06, 0.06, Pal.golden)
        g.rect(&c, pup.minX + 0.11, pup.minY + 0.045, 0.006, 0.006, Pal.ink); g.rect(&c, pup.minX + 0.135, pup.minY + 0.045, 0.006, 0.006, Pal.ink)
        g.oval(&c, pup.minX + 0.095, pup.minY + 0.03, 0.02, 0.035, Color(red: 0.75, green: 0.52, blue: 0.30))
        var tail = c
        tail.translateBy(x: (pup.minX + 0.025) * g.w, y: (pup.minY + 0.07) * g.h)
        tail.rotate(by: .degrees(-30 + 25 * sin(t * wag)))
        tail.fill(Path(CGRect(x: -0.04 * g.w, y: -2 * g.u, width: 0.04 * g.w, height: 4 * g.u)), with: .color(Pal.golden))
    }

    private func sunroom(_ c: inout GraphicsContext, _ g: Geo) {
        sky(&c, g, in: CGRect(x: 0, y: 0, width: 1, height: 0.62))
        for i in 0...6 { g.rect(&c, CGFloat(i) / 6 - 0.004, 0, 0.008, 0.62, Color.white.opacity(0.85)) }
        for y: CGFloat in [0.2, 0.42] { g.rect(&c, 0, y, 1, 0.008, Color.white.opacity(0.85)) }
        g.rect(&c, 0, 0.60, 1, 0.40, Color(red: 0.84, green: 0.80, blue: 0.72))
        for i in 0..<10 { for j in 0..<4 where (i + j) % 2 == 0 { g.rect(&c, CGFloat(i) * 0.1, 0.62 + CGFloat(j) * 0.1, 0.1, 0.1, Color(red: 0.78, green: 0.72, blue: 0.64)) } }
        if !night { for i in 0..<3 { g.poly(&c, [CGPoint(x: 0.15 + CGFloat(i) * 0.3, y: 0.62), CGPoint(x: 0.27 + CGFloat(i) * 0.3, y: 0.62), CGPoint(x: 0.32 + CGFloat(i) * 0.3, y: 0.9), CGPoint(x: 0.2 + CGFloat(i) * 0.3, y: 0.9)], Pal.warm.opacity(0.12)) } }
        // Potting shelf with plants that grow as they're misted.
        g.rect(&c, 0.24, 0.50, 0.50, 0.012, Pal.woodLight); g.rect(&c, 0.26, 0.512, 0.01, 0.15, Pal.wood); g.rect(&c, 0.72, 0.512, 0.01, 0.15, Pal.wood)
        for i in 0..<4 {
            let x = 0.28 + CGFloat(i) * 0.12
            g.rect(&c, x, 0.465, 0.06, 0.035, Color(red: 0.82, green: 0.52, blue: 0.40))
            let height = 0.03 + CGFloat(stage.sproutGrowth) * 0.018
            let sway = CGFloat(sin(t * 0.9 + Double(i))) * 0.005
            g.rect(&c, x + 0.027 + sway, 0.465 - height, 0.006, height, Pal.leaf)
            g.oval(&c, x + 0.008 + sway, 0.465 - height - 0.01, 0.024, 0.02, Pal.leaf)
            g.oval(&c, x + 0.028 + sway, 0.465 - height - 0.015, 0.024, 0.02, Pal.leafDark)
            if stage.sproutGrowth >= 3 { g.rect(&c, x + 0.024 + sway, 0.465 - height - 0.022, 0.012, 0.012, Color(red: 1, green: 0.66, blue: 0.75)) }
        }
        // Ferns.
        for i in 0..<2 {
            let x: CGFloat = i == 0 ? 0.02 : 0.88
            g.rect(&c, x + 0.02, 0.66, 0.07, 0.06, Color(red: 0.70, green: 0.46, blue: 0.36))
            for f in 0..<5 {
                var frond = c
                frond.translateBy(x: (x + 0.055) * g.w, y: 0.66 * g.h)
                frond.rotate(by: .degrees(-70 + Double(f) * 35 + 4 * sin(t + Double(f))))
                frond.fill(Path(CGRect(x: 0, y: -2 * g.u, width: 0.09 * g.w, height: 4 * g.u)), with: .color(Pal.leaf))
            }
        }
        // Terrarium dome.
        let terr = TinyHotspot.terrarium.rect
        g.rect(&c, terr.minX + 0.02, terr.maxY - 0.02, terr.width - 0.04, 0.02, Pal.woodDark)
        g.oval(&c, terr.minX + 0.01, terr.minY + 0.02, terr.width - 0.02, terr.height - 0.03, Pal.glass.opacity(0.55))
        g.oval(&c, terr.minX + 0.04, terr.maxY - 0.07, 0.04, 0.04, Pal.leaf); g.oval(&c, terr.minX + 0.07, terr.maxY - 0.06, 0.04, 0.03, Pal.leafDark)
        g.rect(&c, terr.minX + 0.03, terr.minY + 0.04, 0.01, 0.04, Color.white.opacity(0.6))
        // Watering can.
        let can = TinyHotspot.wateringCan.rect
        g.rect(&c, can.minX + 0.02, can.minY + 0.04, 0.09, 0.07, Color(red: 0.55, green: 0.70, blue: 0.80))
        g.line(&c, CGPoint(x: can.minX + 0.11, y: can.minY + 0.07), CGPoint(x: can.maxX, y: can.minY + 0.02), Color(red: 0.55, green: 0.70, blue: 0.80), width: 2.5 * g.u)
        g.line(&c, CGPoint(x: can.minX + 0.03, y: can.minY + 0.04), CGPoint(x: can.minX + 0.07, y: can.minY + 0.01), Color(red: 0.45, green: 0.58, blue: 0.68), width: 1.5 * g.u)
    }

    private func loft(_ c: inout GraphicsContext, _ g: Geo) {
        room(&c, g, wall: Color(red: 0.30, green: 0.27, blue: 0.40), floor: Color(red: 0.40, green: 0.30, blue: 0.30))
        g.poly(&c, [CGPoint(x: 0, y: 0), CGPoint(x: 0.28, y: 0), CGPoint(x: 0, y: 0.30)], Color(red: 0.24, green: 0.21, blue: 0.32))
        let win = windows[0]
        g.rect(&c, win.minX - 0.012, win.minY - 0.012, win.width + 0.024, win.height + 0.024, Pal.woodDark)
        var pane = c
        pane.clip(to: Path(CGRect(x: win.minX * g.w, y: win.minY * g.h, width: win.width * g.w, height: win.height * g.h)))
        sky(&pane, g, in: win)
        for i in 0..<10 {
            let x = win.minX + CGFloat(i) * win.width / 10, height = 0.06 + CGFloat(hash(i, 4.4)) * 0.14
            g.rect(&pane, x, win.maxY - height, win.width / 10 - 0.006, height, Color(red: 0.14, green: 0.15, blue: 0.26))
            for row in 0..<4 where hash(i * 7 + row, 2.2) > 0.45 { g.rect(&pane, x + 0.012, win.maxY - height + 0.015 + CGFloat(row) * 0.03, 0.01, 0.01, Pal.warm.opacity(0.75)) }
        }
        let moon = TinyHotspot.moon.rect
        g.glow(&pane, moon.midX, moon.midY, radius: 0.10, .white, 0.15)
        g.oval(&pane, moon.midX - 0.04, moon.midY - 0.04 * g.w / g.h, 0.08, 0.08 * g.w / g.h, Color(red: 0.97, green: 0.94, blue: 0.82))
        g.oval(&pane, moon.midX - 0.015, moon.midY - 0.05 * g.w / g.h, 0.08, 0.08 * g.w / g.h, Color(red: 0.08, green: 0.10, blue: 0.24))
        for i in 1..<4 { g.rect(&c, win.minX + CGFloat(i) * win.width / 4 - 0.003, win.minY, 0.006, win.height, Pal.woodDark) }
        // Balcony fairy lights strung under the beam.
        let lights = TinyHotspot.fairyLights.rect
        for i in 0..<16 {
            let p = CGFloat(i) / 15
            let x = lights.minX + p * lights.width, y = lights.minY + 0.03 + sin(p * .pi * 3) * 0.015
            let on = stage.fairyLightsOn ? 0.55 + 0.45 * sin(t * 2.5 + Double(i) * 1.3) : 0.15
            let color = [Pal.warm, Pal.pink, Color(red: 0.7, green: 0.9, blue: 1)][i % 3]
            g.rect(&c, x - 0.004, y, 0.008, 0.012, color.opacity(on))
            if stage.fairyLightsOn { g.glow(&c, x, y + 0.006, radius: 0.025, color, 0.4 * on) }
        }
        // Daybed with patchwork blanket.
        g.rect(&c, 0.28, 0.58, 0.54, 0.10, Color(red: 0.36, green: 0.42, blue: 0.58))
        for i in 0..<6 { g.rect(&c, 0.30 + CGFloat(i) * 0.085, 0.62, 0.08, 0.05, [Color(red: 0.86, green: 0.62, blue: 0.66), Color(red: 0.94, green: 0.84, blue: 0.62), Color(red: 0.62, green: 0.74, blue: 0.86)][i % 3]) }
        // Turntable with a spinning record and floating notes.
        let deck = TinyHotspot.turntable.rect
        g.rect(&c, deck.minX, deck.minY + 0.07, deck.width, 0.14, Pal.wood)
        g.rect(&c, deck.minX, deck.minY + 0.04, deck.width, 0.035, Pal.woodDark)
        let recordH = 0.03
        g.oval(&c, deck.minX + 0.02, deck.minY + 0.015, 0.13, CGFloat(recordH) * 1.6, Pal.ink)
        if stage.vinylSpinning {
            let angle = t * 4
            let hx = deck.minX + 0.085 + CGFloat(cos(angle)) * 0.045, hy = deck.minY + 0.039 + CGFloat(sin(angle)) * 0.012
            g.rect(&c, hx, hy, 0.01, 0.004, Color.white.opacity(0.5))
            for i in 0..<3 {
                let rise = CGFloat(fract(t * 0.35 + Double(i) / 3))
                c.draw(Text(Image(systemName: i % 2 == 0 ? "music.note" : "music.quarternote.3")).font(.system(size: 12 * g.u, weight: .bold)).foregroundColor(Pal.warm.opacity(Double(1 - rise))),
                       at: CGPoint(x: (deck.midX + CGFloat(sin(t + Double(i) * 2)) * 0.04) * g.w, y: (deck.minY - rise * 0.18) * g.h))
            }
        }
        g.oval(&c, deck.minX + 0.075, deck.minY + 0.032, 0.02, 0.014, Color(red: 0.92, green: 0.40, blue: 0.40))
        g.line(&c, CGPoint(x: deck.maxX - 0.02, y: deck.minY + 0.01), CGPoint(x: deck.maxX - 0.05, y: deck.minY + 0.04), Pal.steel, width: 1.5 * g.u)
        // Bedside nightlight.
        let light = TinyHotspot.nightlight.rect
        g.rect(&c, light.minX, light.minY + 0.06, light.width, 0.12, Pal.wood)
        g.oval(&c, light.midX - 0.025, light.minY + 0.015, 0.05, 0.05, stage.nightlightOn ? Color(red: 1, green: 0.76, blue: 0.82) : Color(red: 0.55, green: 0.50, blue: 0.56))
        if stage.nightlightOn { g.glow(&c, light.midX, light.minY + 0.04, radius: 0.12, Color(red: 1, green: 0.66, blue: 0.76), 0.4) }
    }

    // MARK: - Weather

    private func weatherLayer(_ c: inout GraphicsContext, _ g: Geo, clip: CGRect?) {
        var layer = c
        if let clip { layer.clip(to: Path(CGRect(x: clip.minX * g.w, y: clip.minY * g.h, width: clip.width * g.w, height: clip.height * g.h))) }
        let floor = clip?.maxY ?? 0.91
        if let previousWeather, weatherBlend < 1 { drawWeather(&layer, g, type: previousWeather, opacity: 1 - weatherBlend, floor: floor) }
        drawWeather(&layer, g, type: weather, opacity: previousWeather == nil ? 1 : weatherBlend, floor: floor)
    }

    private func drawWeather(_ c: inout GraphicsContext, _ g: Geo, type: TinyWeather, opacity: Double, floor: CGFloat) {
        guard opacity > 0 else { return }
        let count = type == .rain ? 44 : type == .snow ? 34 : 22
        for i in 0..<count {
            let seed = Double((i * 73 + 17) % 997) / 997
            let speed = type == .rain ? 0.52 + seed * 0.5 : 0.09 + seed * 0.11
            let phase = fract(seed + t * speed)
            let alpha = opacity * min(1, phase * 9, (1 - phase) * 9)
            let drift = sin(t * (0.35 + seed) + seed * 50) * (type == .rain ? 5 : 25)
            let x = CGFloat(fract(seed * 1.13)) * g.w + CGFloat(drift)
            let y = CGFloat(phase) * g.h * floor
            switch type {
            case .sunny:
                if i < 8 { c.fill(Path(ellipseIn: CGRect(x: x, y: y, width: 2 * g.u, height: 2 * g.u)), with: .color(Color(red: 1, green: 0.93, blue: 0.68).opacity(0.55 * alpha))) }
            case .rain:
                g.abs(&c, x, y, 1.2 * g.u, 8 * g.u, Color(red: 0.79, green: 0.86, blue: 0.91).opacity(0.52 * alpha))
                if phase > 0.95 { c.fill(Path(ellipseIn: CGRect(x: x - 2, y: g.h * floor - 1, width: 5 * g.u, height: 2 * g.u)), with: .color(Color.white.opacity(0.35 * alpha))) }
            case .sakura, .autumn:
                var piece = c
                piece.translateBy(x: x, y: y)
                piece.rotate(by: .radians(sin(t * 2 + seed * 30) * 0.9))
                let color = type == .sakura ? (i % 2 == 0 ? Color(red: 1, green: 0.66, blue: 0.76) : Color(red: 1, green: 0.80, blue: 0.84)) : Pal.autumn[i % Pal.autumn.count]
                piece.fill(Path(CGRect(x: -2.5 * g.u, y: -1.5 * g.u, width: 5 * g.u, height: 3 * g.u)), with: .color(color.opacity(0.85 * alpha)))
            case .snow:
                let r = CGFloat(1.4 + seed * 2.7) * g.u
                c.fill(Path(ellipseIn: CGRect(x: x, y: y, width: r, height: r)), with: .color(Color.white.opacity(0.82 * alpha)))
            }
        }
    }

    // MARK: - Characters

    private func drawCharacters(_ c: inout GraphicsContext, _ g: Geo) {
        let (a, b) = scene.characterAnchors
        let anchors = [a, b]
        let outfit = Pal.outfits[min(max(self.outfit, 0), Pal.outfits.count - 1)]
        let center = (a.x + b.x) / 2
        for i in 0..<2 {
            let pose = i == 0 ? stage.poses.0 : stage.poses.1
            var x = anchors[i].x
            var feet = anchors[i].y
            if pose == .walk {
                let wander = CGFloat(sin(t * 0.58)) * 0.06 * (i == 0 ? -1 : 1)
                x += wander
                feet += CGFloat(Swift.abs(sin(t * 6 + Double(i) * 1.5))) * -0.006
            }
            if pose == .hug { x = center + (i == 0 ? -0.035 : 0.035) }
            if pose == .joyJump { feet -= CGFloat(Swift.abs(sin(now * 9 + Double(i)))) * 0.05 }
            var shift = CGSize.zero
            if let gaze = stage.gaze {
                let dx = gaze.x - x, dy = gaze.y - (feet - 0.12)
                let length = max(0.001, sqrt(dx * dx + dy * dy))
                shift = CGSize(width: dx / length * 1.6, height: dy / length * 1.2)
            }
            drawPerson(&c, g, x: x * g.w, feet: feet * g.h, scale: g.u * (1 + CGFloat(i) * 0.04), shirt: outfit[i],
                       hair: i == 0 ? Pal.hairBoy : Pal.hairGirl, isGirl: i == 1, pose: pose, eyeShift: shift,
                       blush: stage.blushing || pose == .sitSnuggle || pose == .hug, leanToward: i == 0 ? 1 : -1)
        }
        if stage.poses.0 == .hug || stage.poses.0 == .sitSnuggle {
            let lift = CGFloat(fract(now * 0.5))
            g.heart(&c, center * g.w, (min(a.y, b.y) - 0.18 - lift * 0.04) * g.h, cell: 1.8 * g.u, Pal.pink.opacity(Double(1 - lift)))
        }
    }

    /// One tiny pixel person. Coordinates are absolute; `scale` is points per pixel-art unit.
    private func drawPerson(_ c: inout GraphicsContext, _ g: Geo, x: CGFloat, feet: CGFloat, scale s: CGFloat, shirt: Color, hair: Color,
                            isGirl: Bool, pose: TinyPose, eyeShift: CGSize, blush: Bool, leanToward: CGFloat = 0, apron: Bool = false) {
        let seated = pose == .sitSnuggle || pose == .sitLog || pose == .ride
        let breathe = CGFloat(sin(t * 2.2 + (isGirl ? 1 : 0))) * 0.6 * s
        let lean = pose == .sitSnuggle || pose == .hug ? leanToward * 3 * s : 0
        let bodyH: CGFloat = 20 * s
        let legH: CGFloat = seated ? 4 * s : 7 * s
        let bodyTop = feet - legH - bodyH + breathe
        let headTop = bodyTop - 20 * s
        if pose != .ride { c.fill(Path(ellipseIn: CGRect(x: x - 15 * s, y: feet - 2 * s, width: 30 * s, height: 6 * s)), with: .color(.black.opacity(0.18))) }
        // Legs.
        if seated {
            g.abs(&c, x - 10 * s, feet - legH, 9 * s, legH, Pal.skinShade); g.abs(&c, x + 1 * s, feet - legH, 9 * s, legH, Pal.skinShade)
        } else {
            let step = pose == .walk ? CGFloat(sin(t * 6 + (isGirl ? 1.5 : 0))) * 2 * s : 0
            g.abs(&c, x - 9 * s, feet - legH + step, 7 * s, legH - step, Pal.skinShade); g.abs(&c, x + 2 * s, feet - legH - step, 7 * s, legH + step, Pal.skinShade)
        }
        // Body (girls get a little skirt flare).
        g.abs(&c, x - 12 * s + lean * 0.5, bodyTop, 24 * s, bodyH, shirt)
        if isGirl { g.abs(&c, x - 14 * s + lean * 0.5, bodyTop + bodyH - 6 * s, 28 * s, 6 * s, shirt) }
        if apron { g.abs(&c, x - 8 * s, bodyTop + 6 * s, 16 * s, 14 * s, Pal.cream) }
        // Arms per pose.
        let armY = bodyTop + 3 * s
        switch pose {
        case .joyJump:
            g.abs(&c, x - 17 * s, armY - 12 * s, 5 * s, 14 * s, Pal.skin); g.abs(&c, x + 12 * s, armY - 12 * s, 5 * s, 14 * s, Pal.skin)
        case .hug, .sitSnuggle:
            g.abs(&c, x + leanToward * 8 * s - 6 * s, armY + 4 * s, 16 * s, 5 * s, Pal.skin)
        case .cook:
            let stir = CGFloat(sin(t * 7)) * 2 * s
            g.abs(&c, x + 10 * s, armY + 6 * s + stir, 10 * s, 4 * s, Pal.skin)
            g.abs(&c, x + 18 * s, armY - 4 * s + stir, 2 * s, 14 * s, Pal.woodLight)
        case .eatSneak:
            g.abs(&c, x + 4 * s, armY - 4 * s, 5 * s, 10 * s, Pal.skin)
            c.fill(Path(ellipseIn: CGRect(x: x + 2 * s, y: armY - 9 * s, width: 8 * s, height: 6 * s)), with: .color(Pal.cream))
        case .ride:
            g.abs(&c, x + 6 * s, armY + 2 * s, 12 * s, 4 * s, Pal.skin)
        default:
            g.abs(&c, x - 15 * s, armY, 4 * s, 13 * s, Pal.skin); g.abs(&c, x + 11 * s, armY, 4 * s, 13 * s, Pal.skin)
        }
        // Head.
        let hx = x + lean
        if isGirl { g.abs(&c, hx - 16 * s, headTop + 2 * s, 32 * s, 24 * s, hair) }
        g.abs(&c, hx - 14 * s, headTop, 28 * s, 21 * s, Pal.skin)
        g.abs(&c, hx - 14 * s, headTop - 2 * s, 28 * s, isGirl ? 8 * s : 7 * s, hair)
        if !isGirl { g.abs(&c, hx - 14 * s, headTop + 4 * s, 4 * s, 5 * s, hair) }
        if isGirl {
            g.abs(&c, hx + 8 * s, headTop - 6 * s, 5 * s, 5 * s, Pal.bow); g.abs(&c, hx + 14 * s, headTop - 6 * s, 5 * s, 5 * s, Pal.bow)
            g.abs(&c, hx + 12 * s, headTop - 5 * s, 3 * s, 3 * s, Color(red: 0.86, green: 0.40, blue: 0.50))
        }
        // Eyes follow the last touch; happy poses squint into little arcs.
        let happy = pose == .joyJump || pose == .hug || pose == .sitSnuggle
        let ex = eyeShift.width * s, ey = eyeShift.height * s
        if happy {
            g.abs(&c, hx - 10 * s, headTop + 9 * s, 4 * s, 1.5 * s, Pal.ink); g.abs(&c, hx + 6 * s, headTop + 9 * s, 4 * s, 1.5 * s, Pal.ink)
        } else {
            let blink = fract(t / 4.3 + (isGirl ? 0.3 : 0)) < 0.03
            let eh = blink ? 1 * s : 3 * s
            g.abs(&c, hx - 10 * s + ex, headTop + 8 * s + ey, 3 * s, eh, Pal.ink); g.abs(&c, hx + 6 * s + ex, headTop + 8 * s + ey, 3 * s, eh, Pal.ink)
        }
        let cheek = blush ? 0.95 : 0.6
        g.abs(&c, hx - 11 * s, headTop + 13 * s, 4 * s, 2 * s, Pal.blush.opacity(cheek)); g.abs(&c, hx + 7 * s, headTop + 13 * s, 4 * s, 2 * s, Pal.blush.opacity(cheek))
        if pose == .eatSneak { g.abs(&c, hx - 2 * s, headTop + 15 * s, 5 * s, 3 * s, Pal.ink.opacity(0.7)) }
        else { g.abs(&c, hx - 1 * s, headTop + 15 * s, 3 * s, 1 * s, Pal.ink.opacity(0.6)) }
    }

    // MARK: - Mochi

    private func drawCat(_ c: inout GraphicsContext, _ g: Geo) {
        let s = g.u
        let x = stage.cat.x * g.w, y = stage.cat.y * g.h
        let bow = Pal.collars[min(max(collar, 0), Pal.collars.count - 1)]
        switch stage.catState {
        case .boxNap:
            let box = TinyHotspot.catBox.rect
            drawCatHead(&c, g, x: box.midX * g.w, y: (box.minY + 0.02) * g.h, s: s, sleepy: true)
            sleepZ(&c, g, x: box.midX * g.w + 14 * s, y: (box.minY - 0.02) * g.h)
            return
        case .sleeping:
            let breathe = CGFloat(sin(t * 1.5)) * 1.2 * s
            c.fill(Path(ellipseIn: CGRect(x: x - 14 * s, y: y - 9 * s - breathe, width: 28 * s, height: 16 * s + breathe)), with: .color(Pal.fur))
            g.abs(&c, x + 6 * s, y - 2 * s, 12 * s, 4 * s, Pal.fur)
            drawCatHead(&c, g, x: x - 10 * s, y: y - 8 * s, s: s * 0.85, sleepy: true)
            sleepZ(&c, g, x: x + 10 * s, y: y - 18 * s)
        case .sittingPurr, .playfulPounce:
            let hop = stage.catState == .playfulPounce ? CGFloat(Swift.abs(sin(now * 7))) * 10 * s : 0
            c.fill(Path(ellipseIn: CGRect(x: x - 9 * s, y: y - 14 * s - hop, width: 18 * s, height: 20 * s)), with: .color(Pal.fur))
            let tailWag = CGFloat(sin(t * 5)) * 3 * s
            g.abs(&c, x + 8 * s, y - 2 * s - hop + tailWag * 0.3, 10 * s, 3 * s, Pal.fur)
            drawCatHead(&c, g, x: x, y: y - 18 * s - hop, s: s, sleepy: stage.catState == .sittingPurr)
            if stage.catState == .sittingPurr { c.draw(Text("purr").font(.system(size: 8 * s, weight: .bold, design: .rounded)).foregroundColor(.white.opacity(0.7 + 0.3 * sin(t * 6))), at: CGPoint(x: x + 18 * s, y: y - 30 * s)) }
        case .bellyRoll:
            let wiggle = CGFloat(sin(t * 8)) * 2 * s
            c.fill(Path(ellipseIn: CGRect(x: x - 14 * s, y: y - 10 * s, width: 28 * s, height: 14 * s)), with: .color(Pal.fur))
            c.fill(Path(ellipseIn: CGRect(x: x - 7 * s, y: y - 8 * s, width: 14 * s, height: 8 * s)), with: .color(Color.white))
            for i in 0..<4 { g.abs(&c, x - 10 * s + CGFloat(i) * 6 * s, y - 15 * s + (i % 2 == 0 ? wiggle : -wiggle), 3 * s, 6 * s, Pal.fur) }
            drawCatHead(&c, g, x: x - 16 * s, y: y - 8 * s, s: s * 0.85, sleepy: true)
        case .walkFollow:
            let step = CGFloat(sin(now * 12)) * 2 * s
            let facing: CGFloat = stage.catWalkingLeft ? -1 : 1
            c.fill(Path(ellipseIn: CGRect(x: x - 13 * s, y: y - 12 * s, width: 26 * s, height: 12 * s)), with: .color(Pal.fur))
            for i in 0..<4 { g.abs(&c, x - 10 * s + CGFloat(i) * 6 * s, y - 2 * s + (i % 2 == 0 ? step : -step) * 0.5, 3 * s, 5 * s, Pal.fur) }
            g.abs(&c, x - facing * 14 * s - 4 * s, y - 14 * s, 8 * s, 3 * s, Pal.fur)
            drawCatHead(&c, g, x: x + facing * 13 * s, y: y - 16 * s, s: s, sleepy: false)
        }
        if let bow, stage.catState != .bellyRoll {
            let neck = CGPoint(x: x + (stage.catState == .sleeping ? -10 * s : 0), y: y - (stage.catState == .sleeping ? 2 : 10) * s)
            g.abs(&c, neck.x - 4 * s, neck.y - 1.5 * s, 3 * s, 3 * s, bow); g.abs(&c, neck.x + 1 * s, neck.y - 1.5 * s, 3 * s, 3 * s, bow)
            g.abs(&c, neck.x - 1 * s, neck.y - 1 * s, 2 * s, 2 * s, bow.opacity(0.7))
        }
    }

    private func drawCatHead(_ c: inout GraphicsContext, _ g: Geo, x: CGFloat, y: CGFloat, s: CGFloat, sleepy: Bool) {
        g.abs(&c, x - 8 * s, y - 9 * s, 4 * s, 4 * s, Pal.fur); g.abs(&c, x + 4 * s, y - 9 * s, 4 * s, 4 * s, Pal.fur)
        c.fill(Path(ellipseIn: CGRect(x: x - 9 * s, y: y - 7 * s, width: 18 * s, height: 14 * s)), with: .color(Pal.fur))
        if sleepy {
            g.abs(&c, x - 6 * s, y - 1 * s, 4 * s, 1 * s, Pal.ink); g.abs(&c, x + 2 * s, y - 1 * s, 4 * s, 1 * s, Pal.ink)
        } else {
            g.abs(&c, x - 5 * s, y - 2 * s, 2 * s, 2 * s, Pal.ink); g.abs(&c, x + 3 * s, y - 2 * s, 2 * s, 2 * s, Pal.ink)
        }
        g.abs(&c, x - 1 * s, y + 1 * s, 2 * s, 1.5 * s, Color(red: 0.86, green: 0.55, blue: 0.58))
        g.abs(&c, x - 7 * s, y + 2 * s, 3 * s, 1.5 * s, Pal.blush.opacity(0.5)); g.abs(&c, x + 4 * s, y + 2 * s, 3 * s, 1.5 * s, Pal.blush.opacity(0.5))
    }

    private func sleepZ(_ c: inout GraphicsContext, _ g: Geo, x: CGFloat, y: CGFloat) {
        let rise = CGFloat(fract(t / 2.5))
        c.draw(Text("z").font(.system(size: (8 + rise * 4) * g.u, weight: .bold, design: .rounded)).foregroundColor(.white.opacity(0.8 * Double(1 - rise))),
               at: CGPoint(x: x + rise * 6 * g.u, y: y - rise * 12 * g.u))
    }

    // MARK: - Particles

    private func drawParticles(_ c: inout GraphicsContext, _ g: Geo) {
        for p in stage.particles {
            let age = now - p.born
            guard age >= 0, age < p.life else { continue }
            let a = age / p.life
            let alpha = 1 - a * a
            var x = (p.x + p.vx * CGFloat(age)) * g.w
            var y = (p.y + p.vy * CGFloat(age)) * g.h
            let s = p.size * g.u
            switch p.kind {
            case .heart:
                g.heart(&c, x, y, cell: 1.6 * s, Pal.pink.opacity(alpha))
            case .ember:
                g.abs(&c, x + CGFloat(sin(age * 8 + p.seed * 9)) * 3 * s, y, 2.5 * s, 2.5 * s, Color(red: 1, green: 0.62 - 0.3 * a, blue: 0.25).opacity(alpha))
            case .steam, .mist:
                let r = (5 + CGFloat(a) * 10) * s * (p.kind == .mist ? 0.5 : 1)
                c.fill(Path(ellipseIn: CGRect(x: x - r / 2, y: y - r / 2, width: r, height: r)), with: .color(Color.white.opacity(0.4 * alpha)))
            case .note:
                c.draw(Text(Image(systemName: p.seed > 0.5 ? "music.note" : "music.quarternote.3")).font(.system(size: 13 * s, weight: .bold)).foregroundColor(Pal.warm.opacity(alpha)),
                       at: CGPoint(x: x + CGFloat(sin(age * 4 + p.seed * 6)) * 6 * s, y: y))
            case .petal, .leaf:
                y += CGFloat(age * age) * 6 * s
                x += CGFloat(sin(age * 3 + p.seed * 10)) * 8 * s
                var piece = c
                piece.translateBy(x: x, y: y)
                piece.rotate(by: .radians(age * 3 + p.seed * 6))
                let color = p.kind == .petal ? Color(red: 1, green: 0.70, blue: 0.78) : Pal.autumn[Int(p.seed * 3) % 3]
                piece.fill(Path(CGRect(x: -3 * s, y: -2 * s, width: 6 * s, height: 4 * s)), with: .color(color.opacity(alpha)))
            case .sparkle:
                let twinkle = 0.5 + 0.5 * sin(age * 18 + p.seed * 9)
                let color = Color(red: 1, green: 0.95, blue: 0.75).opacity(alpha * twinkle)
                g.abs(&c, x - 4 * s, y - 0.75 * s, 8 * s, 1.5 * s, color); g.abs(&c, x - 0.75 * s, y - 4 * s, 1.5 * s, 8 * s, color)
            case .bubble:
                let r = 4 * s
                c.stroke(Path(ellipseIn: CGRect(x: x - r, y: y - r, width: r * 2, height: r * 2)), with: .color(Color.white.opacity(0.7 * alpha)), lineWidth: 1)
            case .flower:
                let color = [Color(red: 1, green: 0.78, blue: 0.55), Color(red: 0.95, green: 0.60, blue: 0.70), Color.white][Int(p.seed * 3) % 3]
                g.abs(&c, x - 3 * s, y - 1 * s, 6 * s, 2 * s, color.opacity(alpha)); g.abs(&c, x - 1 * s, y - 3 * s, 2 * s, 6 * s, color.opacity(alpha))
                g.abs(&c, x - 0.75 * s, y - 0.75 * s, 1.5 * s, 1.5 * s, Color(red: 1, green: 0.88, blue: 0.4).opacity(alpha))
            case .drop:
                y += CGFloat(age * age) * 0.3 * g.h
                g.abs(&c, x, y, 2 * s, 4 * s, Color(red: 0.60, green: 0.80, blue: 1).opacity(alpha))
            case .marshmallow:
                let toast = min(1, a * 2)
                g.line(&c, CGPoint(x: x / g.w - 0.06, y: y / g.h + 0.05), CGPoint(x: x / g.w, y: y / g.h), Pal.woodLight.opacity(alpha), width: 1.5 * s)
                c.fill(Path(roundedRect: CGRect(x: x - 4 * s, y: y - 5 * s, width: 8 * s, height: 7 * s), cornerRadius: 2 * s),
                       with: .color(Color(red: 1 - 0.1 * toast, green: 0.95 - 0.3 * toast, blue: 0.85 - 0.5 * toast).opacity(alpha)))
            case .text:
                c.draw(Text(p.label).font(.system(size: 14 * s, weight: .heavy, design: .rounded)).foregroundColor(Pal.warm.opacity(alpha)), at: CGPoint(x: x, y: y))
            }
        }
    }
}
