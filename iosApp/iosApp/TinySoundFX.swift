import AVFoundation
import UIKit

/// Every tactile sound in the world. All of them are synthesized on-device — no bundled assets, no network.
enum TinySFX: CaseIterable {
    case bubblePop, heartChime, steamHiss, scooterHorn, catPurr, windChime, waterDrip, starTwinkle
    case shootingStar, fireCrackle, guitarStrum, cafeBell, kettleWhistle, lampClick, vinylSpin
    case cardFlip, gameWin, musicBox
}

/// Tiny PCM-to-WAV encoder so synthesized buffers can be played through AVAudioPlayer.
enum TinyWav {
    static let sampleRate = 22_050

    static func encode(_ samples: [Float], sampleRate: Int = TinyWav.sampleRate) -> Data {
        var pcm = Data(capacity: samples.count * 2)
        for sample in samples {
            var value = Int16(max(-1, min(1, sample)) * Float(Int16.max)).littleEndian
            withUnsafeBytes(of: &value) { pcm.append(contentsOf: $0) }
        }
        var wav = Data(capacity: pcm.count + 44)
        func append<T: FixedWidthInteger>(_ value: T) { var little = value.littleEndian; withUnsafeBytes(of: &little) { wav.append(contentsOf: $0) } }
        wav.append(contentsOf: Array("RIFF".utf8)); append(UInt32(36 + pcm.count))
        wav.append(contentsOf: Array("WAVEfmt ".utf8)); append(UInt32(16)); append(UInt16(1)); append(UInt16(1))
        append(UInt32(sampleRate)); append(UInt32(sampleRate * 2)); append(UInt16(2)); append(UInt16(16))
        wav.append(contentsOf: Array("data".utf8)); append(UInt32(pcm.count)); wav.append(pcm)
        return wav
    }
}

/// Small deterministic noise source so every synthesized effect sounds identical run to run.
private struct TinyNoise {
    private var state: UInt32
    init(seed: UInt32) { state = seed == 0 ? 0x9E37_79B9 : seed }
    mutating func next() -> Float {
        state ^= state << 13; state ^= state >> 17; state ^= state << 5
        return Float(state) / Float(UInt32.max) * 2 - 1
    }
}

/// Procedural 8-bit chiptune synthesizer. Recipes are intentionally short and soft so they sit under the weather music.
enum TinySynth {
    private static let rate = Float(TinyWav.sampleRate)

    // Float-only math: one signature each, so long synthesis expressions type-check quickly.
    private static func fsin(_ x: Float) -> Float { sinf(x) }
    private static func fexp(_ x: Float) -> Float { expf(x) }
    private static func rise1(_ x: Float) -> Float { Swift.min(1, x) }
    private static let tau = Float.pi * 2

    static func render(_ effect: TinySFX) -> [Float] {
        switch effect {
        case .bubblePop: return bubblePop()
        case .heartChime: return notes([(1318.5, 0, 0.22), (1760, 0.11, 0.32)], wave: .softSquare, decay: 9, gain: 0.34)
        case .steamHiss: return steamHiss(duration: 0.85)
        case .scooterHorn: return scooterHorn()
        case .catPurr: return catPurr()
        case .windChime: return chime([1046.5, 1318.5, 1568, 1760, 2093], spacing: 0.13, ring: 1.5)
        case .waterDrip: return waterDrip()
        case .starTwinkle: return notes([(2093, 0, 0.12), (2637, 0.06, 0.12), (3136, 0.12, 0.12), (3951, 0.18, 0.3)], wave: .triangle, decay: 11, gain: 0.26)
        case .shootingStar: return shootingStar()
        case .fireCrackle: return fireCrackle()
        case .guitarStrum: return guitarStrum()
        case .cafeBell: return chime([1046.5, 1568], spacing: 0.16, ring: 1.2)
        case .kettleWhistle: return kettleWhistle()
        case .lampClick: return lampClick()
        case .vinylSpin: return vinylSpin()
        case .cardFlip: return cardFlip()
        case .gameWin: return notes([(523.25, 0, 0.12), (659.25, 0.09, 0.12), (783.99, 0.18, 0.12), (1046.5, 0.27, 0.4)], wave: .softSquare, decay: 6, gain: 0.3)
        case .musicBox: return notes([(528, 0, 0.66)], wave: .musicBox, decay: 4.1, gain: 0.48)
        }
    }

    private enum Wave { case sine, triangle, softSquare, musicBox }

    private static func osc(_ wave: Wave, _ phase: Float) -> Float {
        switch wave {
        case .sine: return fsin(phase)
        case .triangle: return 2 / .pi * asin(fsin(phase))
        // A tanh-rounded square keeps the 8-bit character without harsh aliasing.
        case .softSquare: return tanh(fsin(phase) * 4) * 0.8
        case .musicBox: return fsin(phase) + fsin(phase * 2) * 0.24
        }
    }

    private static func buffer(_ seconds: Float) -> [Float] { [Float](repeating: 0, count: Int(seconds * rate)) }

    /// (frequency, start seconds, length seconds) notes with a short attack and exponential decay.
    private static func notes(_ list: [(Float, Float, Float)], wave: Wave, decay: Float, gain: Float) -> [Float] {
        let total = list.map { $0.1 + $0.2 }.max() ?? 0.2
        var out = buffer(total + 0.02)
        for (freq, start, length) in list {
            let from = Int(start * rate), count = Int(length * rate)
            for i in 0..<count where from + i < out.count {
                let t = Float(i) / rate
                let env = rise1(t * 200) * fexp(-t * decay)
                out[from + i] += osc(wave, tau * freq * t) * env * gain
            }
        }
        return out
    }

    private static func chime(_ freqs: [Float], spacing: Float, ring: Float) -> [Float] {
        var out = buffer(spacing * Float(freqs.count) + ring)
        for (index, freq) in freqs.enumerated() {
            let from = Int(Float(index) * spacing * rate)
            for i in 0..<Int(ring * rate) where from + i < out.count {
                let t = Float(i) / rate
                // A 2.76× inharmonic partial gives the metallic chime colour.
                let tone = fsin(tau * freq * t) + fsin(tau * freq * 2.76 * t) * 0.3 * fexp(-t * 6)
                out[from + i] += tone * rise1(t * 300) * fexp(-t * 3.2) * 0.2
            }
        }
        return out
    }

    private static func bubblePop() -> [Float] {
        var out = buffer(0.12); var phase: Float = 0
        for i in out.indices {
            let t = Float(i) / rate
            phase += tau * (380 + 1100 * (t / 0.12)) / rate
            out[i] = fsin(phase) * fexp(-t * 30) * 0.45
        }
        return out
    }

    private static func steamHiss(duration: Float) -> [Float] {
        var out = buffer(duration); var noise = TinyNoise(seed: 11); var previous: Float = 0
        for i in out.indices {
            let t = Float(i) / rate
            let white = noise.next()
            let bright = white - previous; previous = white // first-difference high-pass
            out[i] = bright * rise1(t / 0.06) * fexp(-t * 3.4) * 0.16
        }
        return out
    }

    private static func scooterHorn() -> [Float] {
        var out = buffer(0.36)
        for beep in 0..<2 {
            let from = Int(Float(beep) * 0.19 * rate)
            for i in 0..<Int(0.13 * rate) where from + i < out.count {
                let t = Float(i) / rate
                let env = rise1(t * 120) * rise1((0.13 - t) * 120)
                out[from + i] += (osc(.softSquare, tau * 440 * t) + osc(.softSquare, tau * 554.4 * t)) * env * 0.17
            }
        }
        return out
    }

    private static func catPurr() -> [Float] {
        var out = buffer(1.0); var noise = TinyNoise(seed: 23); var low: Float = 0
        for i in out.indices {
            let t = Float(i) / rate
            low += (noise.next() - low) * 0.04 // one-pole low-pass rumble
            let flutter = 0.5 + 0.5 * fsin(tau * 24 * t)
            let body = fsin(tau * 52 * t) * 0.6 + low * 3
            let env: Float = rise1(t * 6) * rise1((1.0 - t) * 5)
            out[i] = body * flutter * env * 0.32
        }
        return out
    }

    private static func waterDrip() -> [Float] {
        var out = buffer(0.22); var phase: Float = 0
        for i in out.indices {
            let t = Float(i) / rate
            let freq: Float = t < 0.06 ? 1600 - 1000 * (t / 0.06) : 900
            phase += tau * freq / rate
            out[i] = fsin(phase) * fexp(-t * (t < 0.06 ? 8 : 26)) * 0.38
        }
        return out
    }

    private static func shootingStar() -> [Float] {
        var out = buffer(0.6); var phase: Float = 0
        for i in out.indices {
            let t = Float(i) / rate
            phase += tau * (2400 - 1500 * (t / 0.6)) / rate
            let shimmer: Float = 0.7 + 0.3 * fsin(tau * 30 * t)
            let env: Float = rise1(t * 40) * fexp(-t * 4.2)
            out[i] = fsin(phase) * shimmer * env * 0.22
        }
        return out
    }

    private static func fireCrackle() -> [Float] {
        var out = buffer(0.9); var noise = TinyNoise(seed: 37)
        let pops: [Float] = [0.02, 0.11, 0.17, 0.33, 0.41, 0.58, 0.66, 0.79]
        for (index, start) in pops.enumerated() {
            let from = Int(start * rate), length = Int(Float(0.012 + Float(index % 3) * 0.008) * rate)
            for i in 0..<length where from + i < out.count {
                out[from + i] += noise.next() * fexp(-Float(i) / Float(length) * 4) * 0.42
            }
        }
        var previous: Float = 0 // soft low warmth under the pops
        for i in out.indices { previous += (noise.next() - previous) * 0.02; out[i] += previous * 0.25 }
        return out
    }

    /// Karplus–Strong plucked strings, strummed across an open G major chord.
    private static func guitarStrum() -> [Float] {
        var out = buffer(1.7); var noise = TinyNoise(seed: 53)
        let strings: [Float] = [98, 123.47, 146.83, 196, 246.94, 392]
        for (index, freq) in strings.enumerated() {
            let period = max(2, Int(rate / freq))
            var ring = (0..<period).map { _ in noise.next() * 0.5 }
            let from = Int(Float(index) * 0.028 * rate)
            var cursor = 0
            for i in 0..<(out.count - from) {
                let next = (cursor + 1) % period
                let value = ring[cursor]
                ring[cursor] = (value + ring[next]) * 0.5 * 0.996
                cursor = next
                out[from + i] += value * 0.24
            }
        }
        return out
    }

    private static func kettleWhistle() -> [Float] {
        var out = buffer(1.1); var noise = TinyNoise(seed: 71); var phase: Float = 0
        for i in out.indices {
            let t = Float(i) / rate
            let pitch: Float = 1750 + 40 * fsin(tau * 6 * t) + 120 * t
            phase += tau * pitch / rate
            let swell = rise1(t / 0.45) * rise1((1.1 - t) * 6)
            out[i] = (fsin(phase) * 0.8 + noise.next() * 0.12) * swell * 0.16
        }
        return out
    }

    private static func lampClick() -> [Float] {
        var out = buffer(0.07); var noise = TinyNoise(seed: 89)
        for i in out.indices {
            let t = Float(i) / rate
            out[i] = (noise.next() * 0.6 + fsin(tau * 2100 * t) * 0.4) * fexp(-t * 90) * 0.45
        }
        return out
    }

    private static func vinylSpin() -> [Float] {
        // Needle crackle followed by a soft lofi major-seventh chord.
        var out = notes([(261.63, 0.12, 1.1), (329.63, 0.12, 1.1), (392, 0.12, 1.1), (493.88, 0.12, 1.1)], wave: .triangle, decay: 2.2, gain: 0.09)
        var noise = TinyNoise(seed: 97)
        for i in out.indices where noise.next() > 0.985 { out[i] += noise.next() * 0.3 }
        return out
    }

    private static func cardFlip() -> [Float] {
        var out = buffer(0.09); var noise = TinyNoise(seed: 101)
        for i in out.indices {
            let t = Float(i) / rate
            let swish: Float = noise.next() * 0.5 * fexp(-t * 60)
            let blip: Float = fsin(tau * 700 * t) * fexp(-t * 40) * 0.5
            out[i] = (swish + blip) * 0.4
        }
        return out
    }

    /// Gentle procedural ambience used if a bundled weather track is ever missing.
    static func ambience(rain: Bool, seconds: Float = 8) -> [Float] {
        var out = buffer(seconds); var noise = TinyNoise(seed: rain ? 3 : 5); var low: Float = 0
        let chord: [Float] = [220, 277.18, 329.63, 415.3] // A major seventh, lofi-soft
        for i in out.indices {
            let t = Float(i) / rate
            low += (noise.next() - low) * (rain ? 0.35 : 0.015)
            // Seamless loop: every modulator completes whole cycles over `seconds`.
            let breath = 0.6 + 0.4 * fsin(tau * t / seconds)
            var pad: Float = 0
            for (index, freq) in chord.enumerated() { pad += fsin(tau * freq * t) * (0.5 + 0.5 * fsin(tau * Float(index + 1) * t / seconds)) }
            out[i] = low * (rain ? 0.22 : 0.5) * breath + pad * 0.018
        }
        return out
    }
}

/// Plays synthesized effects with a small voice pool so rapid taps overlap instead of cutting each other off.
@MainActor final class TinySoundBoard {
    static let shared = TinySoundBoard()
    private var cache: [TinySFX: Data] = [:]
    private var voices: [AVAudioPlayer] = []
    private var enabled = true
    private var volume: Float = 0.72

    func configure(enabled: Bool, volume: Double) {
        self.enabled = enabled
        self.volume = Float(min(max(volume, 0), 1))
        if !enabled { voices.forEach { $0.stop() }; voices.removeAll() }
    }

    func play(_ effect: TinySFX, gain: Float = 1) {
        guard enabled, UIApplication.shared.applicationState != .background else { return }
        try? AVAudioSession.sharedInstance().setCategory(.ambient, options: [.mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
        let data: Data
        if let cached = cache[effect] { data = cached }
        else { data = TinyWav.encode(TinySynth.render(effect)); cache[effect] = data }
        guard let player = try? AVAudioPlayer(data: data) else { return }
        voices.removeAll { !$0.isPlaying }
        if voices.count >= 6 { voices.removeFirst().stop() }
        player.volume = volume * gain
        player.prepareToPlay()
        player.play()
        voices.append(player)
    }
}

