import SwiftUI
import PhotosUI
import Shared

private let roseTint = Color(red: 0.93, green: 0.45, blue: 0.56)
private let creamTint = Color(red: 1, green: 0.85, blue: 0.62)

/// A user-added date on the Special Calendar. Recurs every year on its month and day.
struct TinySpecialDate: Codable, Identifiable, Hashable {
    var id = UUID()
    var title: String
    var date: Date
    var emoji: String
}

// MARK: - Scene picker

struct ScenePickerSheet: View {
    @ObservedObject var world: TinyWorld
    @Environment(\.dismiss) private var dismiss
    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    Button { world.surpriseScene(); dismiss() } label: {
                        Label("Surprise Me (Random Scene)", systemImage: "dice.fill")
                            .font(.system(.headline, design: .rounded)).frame(maxWidth: .infinity).padding(.vertical, 14)
                            .background(roseTint.opacity(0.85), in: RoundedRectangle(cornerRadius: 16)).foregroundStyle(.white)
                    }.buttonStyle(.plain)
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(TinyScene.allCases) { scene in
                            Button { world.select(scene); dismiss() } label: { tile(scene) }.buttonStyle(.plain)
                        }
                    }
                }.padding(16)
            }
            .navigationTitle("Choose a Scene").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Done") { dismiss() } } }
        }.presentationDetents([.medium, .large]).presentationDragIndicator(.visible)
    }

    private func tile(_ scene: TinyScene) -> some View {
        let selected = scene == world.save.scene
        return VStack(alignment: .leading, spacing: 6) {
            Image(systemName: scene.icon).font(.system(size: 22, weight: .semibold)).foregroundStyle(creamTint)
            Text(scene.title).font(.system(.subheadline, design: .rounded, weight: .bold)).foregroundStyle(.primary).lineLimit(1)
            Text(scene.subtitle).font(.system(.caption2, design: .rounded)).foregroundStyle(.secondary).lineLimit(2, reservesSpace: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading).padding(12)
        .background(.quaternary, in: RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(selected ? roseTint : .clear, lineWidth: 2))
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

// MARK: - Daily Tiny Moment & anniversary

struct DailyMomentSheet: View {
    @ObservedObject var world: TinyWorld
    @Environment(\.dismiss) private var dismiss
    @State private var answerOne = ""
    @State private var answerTwo = ""

    private var todayKey: String { TinyWorld.dayKey(Date()) }
    private var saved: [String] { world.save.momentAnswers[todayKey] ?? ["", ""] }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(spacing: 8) {
                        Image(systemName: "heart.circle.fill").font(.system(size: 38)).foregroundStyle(roseTint)
                        Text("\(world.save.nameOne) ♥ \(world.save.nameTwo)").font(.system(.headline, design: .rounded))
                        Text("✦ Day \(world.relationshipDay) of Tiny Us ✦")
                            .font(.system(.subheadline, design: .monospaced, weight: .bold)).foregroundStyle(creamTint)
                            .padding(.horizontal, 14).padding(.vertical, 7).background(.black.opacity(0.25), in: Capsule())
                        loveClock
                        Text(milestoneLine).font(.system(.footnote, design: .rounded)).foregroundStyle(.secondary).multilineTextAlignment(.center)
                    }.frame(maxWidth: .infinity)

                    VStack(alignment: .leading, spacing: 10) {
                        Text("DAILY TINY MOMENT").font(.system(.caption, design: .monospaced, weight: .bold)).tracking(1.2).foregroundStyle(.secondary)
                        Text(world.save.dailyPrompt).font(.system(.title3, design: .rounded, weight: .semibold))
                        Text("One gentle reflection for both of you today").font(.footnote).foregroundStyle(.secondary)
                        TextField("\(world.save.nameOne)'s reflection", text: $answerOne, axis: .vertical).lineLimit(2...5).textFieldStyle(.roundedBorder)
                        TextField("\(world.save.nameTwo)'s reflection", text: $answerTwo, axis: .vertical).lineLimit(2...5).textFieldStyle(.roundedBorder)
                        Button("Save Our Daily Moment ✨") { saveAnswers() }
                            .buttonStyle(.borderedProminent).tint(roseTint)
                        revealCard
                    }.padding(14).background(.quaternary, in: RoundedRectangle(cornerRadius: 16))

                    let history = world.save.momentAnswers.keys.filter { $0 != todayKey }.sorted(by: >).prefix(5)
                    if !history.isEmpty {
                        Text("EARLIER MOMENTS").font(.system(.caption, design: .monospaced, weight: .bold)).tracking(1.2).foregroundStyle(.secondary)
                        ForEach(Array(history), id: \.self) { key in
                            let answers = world.save.momentAnswers[key] ?? []
                            VStack(alignment: .leading, spacing: 5) {
                                Text(key).font(.caption2).foregroundStyle(.secondary)
                                ForEach(Array(answers.enumerated()), id: \.offset) { index, text in
                                    if !text.isEmpty { Text("\(index == 0 ? world.save.nameOne : world.save.nameTwo): \(text)").font(.system(.subheadline, design: .rounded)) }
                                }
                            }.frame(maxWidth: .infinity, alignment: .leading).padding(12).background(.quaternary, in: RoundedRectangle(cornerRadius: 12))
                        }
                    }
                }.padding(20)
            }
            .navigationTitle("Today's Tiny Moment").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Close") { dismiss() } } }
            .onAppear { world.refreshPromptIfNeeded(); answerOne = saved.first ?? ""; answerTwo = saved.count > 1 ? saved[1] : "" }
        }.presentationDetents([.large]).presentationDragIndicator(.visible)
    }

    /// Live "Our Time" clock: years, months, days and a ticking hh:mm:ss since the special day.
    private var loveClock: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            let parts = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute, .second],
                                                        from: Calendar.current.startOfDay(for: world.save.anniversary), to: context.date)
            VStack(spacing: 3) {
                Text("\(parts.year ?? 0) years · \(parts.month ?? 0) months · \(parts.day ?? 0) days").font(.system(.subheadline, design: .rounded, weight: .semibold))
                Text(String(format: "%02d hours %02d mins %02d secs", parts.hour ?? 0, parts.minute ?? 0, parts.second ?? 0))
                    .font(.system(.caption, design: .monospaced)).foregroundStyle(.secondary)
            }
        }
    }

    private var milestoneLine: String {
        let day = Int(world.relationshipDay)
        let nextHundred = (day / 100 + 1) * 100
        let cal = Calendar.current
        let today = cal.startOfDay(for: Date())
        var parts = cal.dateComponents([.month, .day], from: world.save.anniversary)
        parts.year = cal.component(.year, from: today)
        var next = cal.date(from: parts) ?? today
        if next < today { next = cal.date(byAdding: .year, value: 1, to: next) ?? next }
        let untilAnniversary = cal.dateComponents([.day], from: today, to: next).day ?? 0
        let anniversaryText = untilAnniversary == 0 ? "Happy anniversary today! 🎉" : "\(untilAnniversary) days until your anniversary."
        return "\(nextHundred - day) days until Day \(nextHundred). \(anniversaryText)"
    }

    @ViewBuilder private var revealCard: some View {
        let both = saved.count > 1 && !saved[0].isEmpty && !saved[1].isEmpty
        if both {
            VStack(alignment: .leading, spacing: 6) {
                Label("Both reflections shared & unlocked 📝", systemImage: "lock.open.fill").font(.system(.footnote, design: .rounded, weight: .semibold)).foregroundStyle(.green)
                Text("\(world.save.nameOne): \(saved[0])").font(.system(.subheadline, design: .rounded))
                Text("\(world.save.nameTwo): \(saved[1])").font(.system(.subheadline, design: .rounded))
            }
        } else if saved.contains(where: { !$0.isEmpty }) {
            Label("Reflections preserved privately until both share 🔒", systemImage: "lock.fill").font(.footnote).foregroundStyle(.secondary)
        }
    }

    private func saveAnswers() {
        let one = answerOne.trimmingCharacters(in: .whitespacesAndNewlines)
        let two = answerTwo.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !one.isEmpty || !two.isEmpty else { return }
        world.save.momentAnswers[todayKey] = [one, two]
        world.save.homeKeepsakes.insert("A bedside notepad of reflections")
        TinySoundBoard.shared.play(.heartChime)
        world.showToast("Today’s tiny moment is saved 💛")
    }
}

// MARK: - Couple Arcade

struct ArcadeSheet: View {
    enum Game: String, CaseIterable, Identifiable { case ticTacToe = "Tic-Tac-Toe", memory = "Memory Match", questions = "Questions"; var id: String { rawValue } }
    @ObservedObject var world: TinyWorld
    @Environment(\.dismiss) private var dismiss
    @State private var game: Game = .ticTacToe

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Picker("Game", selection: $game) { ForEach(Game.allCases) { Text($0.rawValue).tag($0) } }.pickerStyle(.segmented)
                    switch game {
                    case .ticTacToe: TicTacToeBoard(world: world)
                    case .memory: MemoryMatchBoard(world: world)
                    case .questions: LittleQuestionsGame(world: world)
                    }
                }.padding(20)
            }
            .navigationTitle("Couple Arcade").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Done") { dismiss() } } }
        }.presentationDetents([.large]).presentationDragIndicator(.visible)
    }
}

/// Pass-and-play tic-tac-toe: hearts for the first partner, stars for the second.
private struct TicTacToeBoard: View {
    @ObservedObject var world: TinyWorld
    @State private var cells: [Int] = Array(repeating: 0, count: 9) // 0 empty, 1 heart, 2 star
    @State private var turn = 1
    @State private var winningLine: [Int] = []
    private static let lines = [[0, 1, 2], [3, 4, 5], [6, 7, 8], [0, 3, 6], [1, 4, 7], [2, 5, 8], [0, 4, 8], [2, 4, 6]]

    private var finished: Bool { !winningLine.isEmpty || !cells.contains(0) }

    var body: some View {
        VStack(spacing: 14) {
            HStack {
                score("💖 \(world.save.nameOne)", world.save.ticTacToeScore[0])
                Spacer()
                score("Draws", world.save.ticTacToeScore[2])
                Spacer()
                score("⭐️ \(world.save.nameTwo)", world.save.ticTacToeScore[1])
            }
            Text(status).font(.system(.headline, design: .rounded)).frame(maxWidth: .infinity)
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3), spacing: 8) {
                ForEach(0..<9, id: \.self) { index in
                    Button { play(index) } label: {
                        Text(cells[index] == 1 ? "💖" : cells[index] == 2 ? "⭐️" : " ")
                            .font(.system(size: 40)).frame(maxWidth: .infinity).aspectRatio(1, contentMode: .fit)
                            .background(winningLine.contains(index) ? roseTint.opacity(0.35) : Color.white.opacity(0.08), in: RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain).disabled(cells[index] != 0 || finished)
                    .accessibilityLabel(cells[index] == 1 ? "Heart" : cells[index] == 2 ? "Star" : "Empty square \(index + 1)")
                }
            }
            Button(finished ? "Play again" : "Restart round") { reset() }.buttonStyle(.borderedProminent).tint(roseTint)
        }
    }

    private func score(_ title: String, _ value: Int) -> some View {
        VStack(spacing: 2) { Text("\(value)").font(.system(.title3, design: .rounded, weight: .bold)); Text(title).font(.caption2).lineLimit(1) }
    }

    private var status: String {
        if let first = winningLine.first { return "\(cells[first] == 1 ? world.save.nameOne : world.save.nameTwo) wins this round! 🎉" }
        if !cells.contains(0) { return "A cozy draw — everyone wins 🤝" }
        return "\(turn == 1 ? "💖 " + world.save.nameOne : "⭐️ " + world.save.nameTwo)’s turn"
    }

    private func play(_ index: Int) {
        guard cells[index] == 0, !finished else { return }
        cells[index] = turn
        TinySoundBoard.shared.play(turn == 1 ? .bubblePop : .starTwinkle, gain: 0.7)
        if let line = Self.lines.first(where: { $0.allSatisfy { cells[$0] == turn } }) {
            winningLine = line
            world.save.ticTacToeScore[turn - 1] += 1
            TinySoundBoard.shared.play(.gameWin)
        } else if !cells.contains(0) {
            world.save.ticTacToeScore[2] += 1
        } else {
            turn = turn == 1 ? 2 : 1
        }
    }

    private func reset() {
        cells = Array(repeating: 0, count: 9); winningLine = []
        turn = turn == 1 ? 2 : 1 // alternate who starts
    }
}

/// Flip two cards at a time and find all six pairs.
private struct MemoryMatchBoard: View {
    @ObservedObject var world: TinyWorld
    @State private var deck: [String] = MemoryMatchBoard.shuffled()
    @State private var faceUp: Set<Int> = []
    @State private var matched: Set<Int> = []
    @State private var moves = 0
    @State private var busy = false
    private static let symbols = ["💖", "⭐️", "🌙", "🌸", "☕️", "🐾"]
    private static func shuffled() -> [String] { (symbols + symbols).shuffled() }

    var body: some View {
        VStack(spacing: 14) {
            HStack {
                Text("Moves: \(moves)").font(.system(.subheadline, design: .rounded, weight: .semibold))
                Spacer()
                if let best = world.save.memoryBestMoves { Text("Best: \(best)").font(.system(.subheadline, design: .rounded)).foregroundStyle(.secondary) }
            }
            if matched.count == deck.count { Text("All pairs found together! 🎉").font(.system(.headline, design: .rounded)).frame(maxWidth: .infinity) }
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
                ForEach(deck.indices, id: \.self) { index in
                    let shown = faceUp.contains(index) || matched.contains(index)
                    Button { flip(index) } label: {
                        Text(shown ? deck[index] : "?").font(.system(size: shown ? 30 : 22, weight: .bold, design: .rounded))
                            .foregroundStyle(shown ? Color.primary : creamTint)
                            .frame(maxWidth: .infinity).aspectRatio(0.8, contentMode: .fit)
                            .background(matched.contains(index) ? roseTint.opacity(0.3) : shown ? Color.white.opacity(0.18) : roseTint.opacity(0.65), in: RoundedRectangle(cornerRadius: 12))
                    }
                    .buttonStyle(.plain).disabled(shown || busy)
                    .accessibilityLabel(shown ? deck[index] : "Face-down card \(index + 1)")
                }
            }
            Button("Shuffle a new game") { deck = Self.shuffled(); faceUp = []; matched = []; moves = 0 }.buttonStyle(.borderedProminent).tint(roseTint)
        }
    }

    private func flip(_ index: Int) {
        guard !busy, !faceUp.contains(index), !matched.contains(index) else { return }
        TinySoundBoard.shared.play(.cardFlip)
        faceUp.insert(index)
        guard faceUp.count == 2 else { return }
        moves += 1
        let pair = Array(faceUp)
        if deck[pair[0]] == deck[pair[1]] {
            matched.formUnion(pair); faceUp = []
            TinySoundBoard.shared.play(.heartChime, gain: 0.7)
            if matched.count == deck.count {
                if moves < (world.save.memoryBestMoves ?? .max) { world.save.memoryBestMoves = moves }
                TinySoundBoard.shared.play(.gameWin)
                world.save.homeKeepsakes.insert("A tiny game board")
            }
        } else {
            busy = true
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) { faceUp = []; busy = false }
        }
    }
}

/// The shared Kotlin question catalog, answered privately by each partner in turn.
private struct LittleQuestionsGame: View {
    @ObservedObject var world: TinyWorld
    private var questions: [MiniGameQuestion] { MiniGameCatalog.shared.questions }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            if questions.isEmpty {
                Text("Your little game collection is resting. Try again in a moment.").foregroundStyle(.secondary)
            } else {
                let question = questions[world.save.miniGameIndex % questions.count]
                Text(question.type.title).font(.system(.caption, design: .monospaced, weight: .bold)).tracking(1).foregroundStyle(.secondary)
                Text(question.prompt).font(.system(.title3, design: .rounded, weight: .semibold))
                if world.save.miniGameTurn == 1 { Text("Pass the phone to \(world.save.nameTwo), then choose privately.").font(.footnote).foregroundStyle(.secondary) }
                if world.save.miniGameTurn < 2 {
                    ForEach(Array(question.options.enumerated()), id: \.offset) { choice in
                        Button { choose(choice.offset) } label: {
                            Text(choice.element).frame(maxWidth: .infinity, alignment: .leading).padding(12).background(.quaternary, in: RoundedRectangle(cornerRadius: 12))
                        }.buttonStyle(.plain)
                    }
                } else {
                    let first = world.save.miniGameFirstChoice ?? 0
                    let second = world.save.miniGameSecondChoice ?? 0
                    Text("\(world.save.nameOne): \(question.options.indices.contains(first) ? question.options[first] : "—")").font(.system(.subheadline, design: .rounded))
                    Text("\(world.save.nameTwo): \(question.options.indices.contains(second) ? question.options[second] : "—")").font(.system(.subheadline, design: .rounded))
                    Text(first == second ? "Match Made in Heaven! 💕" : "Playful Perspectives! 🌟").font(.system(.headline, design: .rounded)).foregroundStyle(roseTint)
                    Button("Next Question") {
                        world.save.miniGameIndex = (world.save.miniGameIndex + 1) % max(1, questions.count)
                        world.save.miniGameTurn = 0; world.save.miniGameFirstChoice = nil; world.save.miniGameSecondChoice = nil
                    }.buttonStyle(.borderedProminent).tint(roseTint)
                }
            }
        }
    }

    private func choose(_ index: Int) {
        TinySoundBoard.shared.play(.bubblePop, gain: 0.6)
        if world.save.miniGameTurn == 0 { world.save.miniGameFirstChoice = index; world.save.miniGameTurn = 1 }
        else { world.save.miniGameSecondChoice = index; world.save.miniGameTurn = 2; world.save.homeKeepsakes.insert("A tiny game board") }
    }
}

// MARK: - Special Calendar

struct SpecialCalendarSheet: View {
    struct DayEvent: Hashable { let emoji: String; let title: String }

    @ObservedObject var world: TinyWorld
    @Environment(\.dismiss) private var dismiss
    @State private var month = Calendar.current.date(from: Calendar.current.dateComponents([.year, .month], from: Date())) ?? Date()
    @State private var selected = Calendar.current.startOfDay(for: Date())
    @State private var newTitle = ""
    @State private var newDate = Date()
    @State private var newEmoji = "🌸"
    private var cal: Calendar { var c = Calendar.current; c.firstWeekday = 2; return c } // weeks start on Monday, like Android

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("OUR SPECIAL CALENDAR").font(.system(.caption, design: .monospaced, weight: .bold)).tracking(1.4).foregroundStyle(creamTint)
                        Text("\(world.save.nameOne) & \(world.save.nameTwo) • Precious Moments").font(.system(.subheadline, design: .rounded)).foregroundStyle(.secondary)
                    }
                    if let today = events(on: Date()).first {
                        Text("TODAY • \(today.title.uppercased()) \(today.emoji)").font(.system(.footnote, design: .rounded, weight: .bold))
                            .frame(maxWidth: .infinity).padding(10).background(roseTint.opacity(0.3), in: RoundedRectangle(cornerRadius: 12))
                    }
                    monthHeader
                    grid
                    Text("❤️ Moments  🎂 Birthdays  ✨ Next Meet  🌸 Special  📸 Memories").font(.caption2).foregroundStyle(.secondary)
                    detailCard
                    addForm
                }.padding(20)
            }
            .navigationTitle("Special Calendar").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Close Scrapbook") { dismiss() } } }
        }.presentationDetents([.large]).presentationDragIndicator(.visible)
    }

    private var monthHeader: some View {
        HStack {
            Button { shiftMonth(-1) } label: { Image(systemName: "chevron.left") }.accessibilityLabel("Previous month")
            Spacer()
            Text(month.formatted(.dateTime.month(.wide).year())).font(.system(.headline, design: .rounded))
            Spacer()
            Button { shiftMonth(1) } label: { Image(systemName: "chevron.right") }.accessibilityLabel("Next month")
        }
        .overlay(alignment: .bottom) {
            Button("Jump to Today") {
                month = cal.date(from: cal.dateComponents([.year, .month], from: Date())) ?? Date()
                selected = cal.startOfDay(for: Date())
            }.font(.caption).offset(y: 22)
        }.padding(.bottom, 18)
    }

    private var grid: some View {
        let days = monthDays()
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 7), spacing: 4) {
            ForEach(["M", "T", "W", "T", "F", "S", "S"].indices, id: \.self) { i in
                Text(["M", "T", "W", "T", "F", "S", "S"][i]).font(.caption2.weight(.bold)).foregroundStyle(.secondary)
            }
            ForEach(days.indices, id: \.self) { i in
                if let day = days[i] {
                    let marks = events(on: day)
                    let isToday = cal.isDateInToday(day)
                    Button { selected = day } label: {
                        VStack(spacing: 1) {
                            Text("\(cal.component(.day, from: day))").font(.system(.footnote, design: .rounded, weight: isToday ? .bold : .regular))
                            Text(marks.first?.emoji ?? " ").font(.system(size: 9))
                        }
                        .frame(maxWidth: .infinity, minHeight: 40)
                        .background((marks.isEmpty ? (isToday ? roseTint.opacity(0.14) : Color.clear) : roseTint.opacity(0.22)), in: RoundedRectangle(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(cal.isDate(day, inSameDayAs: selected) ? creamTint : (isToday ? roseTint : .clear), lineWidth: 1.5))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("\(day.formatted(date: .long, time: .omitted))\(marks.isEmpty ? "" : ", " + marks.map(\.title).joined(separator: ", "))")
                } else {
                    Color.clear.frame(minHeight: 40)
                }
            }
        }
    }

    @ViewBuilder private var detailCard: some View {
        let list = events(on: selected)
        VStack(alignment: .leading, spacing: 8) {
            Text(selected.formatted(.dateTime.day().month(.wide).year())).font(.system(.headline, design: .rounded))
            if list.isEmpty {
                Text("A quiet, ordinary day — the best kind to spend together.").font(.footnote).foregroundStyle(.secondary)
            } else {
                ForEach(list, id: \.self) { event in Text("\(event.emoji)  \(event.title)").font(.system(.subheadline, design: .rounded)) }
                let until = cal.dateComponents([.day], from: cal.startOfDay(for: Date()), to: selected).day ?? 0
                if until == 0 { Text("TODAY ❤️").font(.system(.footnote, design: .rounded, weight: .bold)).foregroundStyle(roseTint) }
                else if until > 0 { Text("Coming in \(until) DAYS").font(.system(.footnote, design: .monospaced, weight: .bold)).foregroundStyle(creamTint) }
            }
            ForEach(world.save.specialDates.filter { sameMonthDay($0.date, selected) }) { special in
                Button(role: .destructive) { world.save.specialDates.removeAll { $0.id == special.id } } label: {
                    Label("Remove “\(special.title)”", systemImage: "trash")
                }.font(.caption)
            }
        }.frame(maxWidth: .infinity, alignment: .leading).padding(14).background(.quaternary, in: RoundedRectangle(cornerRadius: 14))
    }

    private var addForm: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("ADD A SPECIAL DATE").font(.system(.caption, design: .monospaced, weight: .bold)).tracking(1.2).foregroundStyle(.secondary)
            TextField("Birthday, first trip, next meet…", text: $newTitle).textFieldStyle(.roundedBorder)
            DatePicker("Date", selection: $newDate, displayedComponents: .date)
            Picker("Kind", selection: $newEmoji) {
                Text("❤️ Moment").tag("❤️"); Text("🎂 Birthday").tag("🎂"); Text("✨ Next meet").tag("✨"); Text("🌸 Special").tag("🌸")
            }.pickerStyle(.segmented)
            Button("Add to our calendar") {
                let title = newTitle.trimmingCharacters(in: .whitespacesAndNewlines)
                guard !title.isEmpty else { return }
                world.save.specialDates.append(TinySpecialDate(title: title, date: newDate, emoji: newEmoji))
                newTitle = ""; selected = cal.startOfDay(for: newDate)
                month = cal.date(from: cal.dateComponents([.year, .month], from: newDate)) ?? month
                TinySoundBoard.shared.play(.heartChime)
            }.buttonStyle(.borderedProminent).tint(roseTint)
        }
    }

    private func sameMonthDay(_ a: Date, _ b: Date) -> Bool {
        cal.component(.month, from: a) == cal.component(.month, from: b) && cal.component(.day, from: a) == cal.component(.day, from: b)
    }

    /// Events recur yearly by month/day, like Android's calendar; memories are shown on the exact day they were saved.
    private func events(on day: Date) -> [DayEvent] {
        var list: [DayEvent] = []
        let anniversary = world.save.anniversary
        if sameMonthDay(anniversary, day) && day >= cal.startOfDay(for: anniversary) { list.append(DayEvent(emoji: "❤️", title: "Our Beginning")) }
        else if cal.component(.day, from: anniversary) == cal.component(.day, from: day) && day > anniversary { list.append(DayEvent(emoji: "💕", title: "Monthiversary")) }
        for special in world.save.specialDates where sameMonthDay(special.date, day) { list.append(DayEvent(emoji: special.emoji, title: special.title)) }
        let memories = world.save.entries[TinyFeature.memories.rawValue, default: []].filter { cal.isDate($0.date, inSameDayAs: day) }
        if !memories.isEmpty { list.append(DayEvent(emoji: "📸", title: memories.count == 1 ? "A saved memory" : "\(memories.count) saved memories")) }
        return list
    }

    private func monthDays() -> [Date?] {
        guard let range = cal.range(of: .day, in: .month, for: month) else { return [] }
        let weekday = cal.component(.weekday, from: month) // 1 = Sunday
        let leading = (weekday + 5) % 7 // Monday-first offset
        let days = range.compactMap { cal.date(byAdding: .day, value: $0 - 1, to: month) }
        return Array(repeating: nil, count: leading) + days.map { Optional($0) }
    }

    private func shiftMonth(_ step: Int) {
        month = cal.date(byAdding: .month, value: step, to: month) ?? month
        TinySoundBoard.shared.play(.cardFlip, gain: 0.5)
    }
}

// MARK: - Keepsake memory wall

struct KeepsakeWallSheet: View {
    @ObservedObject var world: TinyWorld
    @Environment(\.dismiss) private var dismiss
    @State private var draft = ""
    @State private var pickedPhoto: PhotosPickerItem?
    @State private var inspected: TinyEntry?
    private var memories: [TinyEntry] { world.save.entries[TinyFeature.memories.rawValue, default: []] }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Keep a small moment close, with a photo, a Polaroid of your world, or a few words.").font(.system(.headline, design: .rounded))
                    HStack {
                        Button { world.capturePolaroid() } label: { Label("Capture this scene", systemImage: "camera.fill") }.buttonStyle(.borderedProminent).tint(roseTint)
                        PhotosPicker(selection: $pickedPhoto, matching: .images) { Label(pickedPhoto == nil ? "Add a photo" : "Change photo", systemImage: "photo.badge.plus") }.buttonStyle(.bordered)
                    }
                    TextField("What do you want to remember?", text: $draft, axis: .vertical).lineLimit(2...5).textFieldStyle(.roundedBorder)
                    Button("Save memory") {
                        Task {
                            var imageData: Data?
                            if let pickedPhoto { imageData = try? await pickedPhoto.loadTransferable(type: Data.self) }
                            world.add(draft, to: .memories, imageData: imageData)
                            draft = ""; pickedPhoto = nil
                        }
                    }.buttonStyle(.bordered)
                    Text(memories.isEmpty ? "Your album is waiting" : "\(memories.count) moments captured")
                        .font(.system(.caption, design: .monospaced, weight: .bold)).tracking(1.2).foregroundStyle(.secondary)
                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)], spacing: 18) {
                        ForEach(Array(memories.enumerated()), id: \.element.id) { index, entry in
                            Button { inspected = entry } label: { polaroid(entry, tilt: index % 2 == 0 ? -2.5 : 2) }.buttonStyle(.plain)
                        }
                    }
                }.padding(20)
            }
            .navigationTitle("Our Memories").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Done") { dismiss() } } }
            .sheet(item: $inspected) { entry in inspector(entry) }
        }.presentationDetents([.large]).presentationDragIndicator(.visible)
    }

    private func polaroid(_ entry: TinyEntry, tilt: Double) -> some View {
        VStack(spacing: 6) {
            Group {
                if let name = entry.imageFilename, let image = world.memoryImage(named: name) {
                    Image(uiImage: image).resizable().scaledToFill()
                } else {
                    LinearGradient(colors: [roseTint.opacity(0.5), creamTint.opacity(0.6)], startPoint: .topLeading, endPoint: .bottomTrailing)
                        .overlay(Image(systemName: "heart.fill").font(.title).foregroundStyle(.white.opacity(0.8)))
                }
            }
            .frame(height: 130).frame(maxWidth: .infinity).clipped()
            Text(entry.text.isEmpty ? " " : entry.text).font(.system(.caption, design: .serif)).italic().foregroundStyle(Color(white: 0.25)).lineLimit(2)
            Text(entry.date.formatted(date: .abbreviated, time: .omitted)).font(.system(size: 9, design: .rounded)).foregroundStyle(Color(white: 0.45))
        }
        .padding(8).padding(.bottom, 6).background(Color(white: 0.98))
        .shadow(color: .black.opacity(0.3), radius: 4, y: 3)
        .rotationEffect(.degrees(tilt))
        .accessibilityElement(children: .combine)
    }

    private func inspector(_ entry: TinyEntry) -> some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    polaroid(entry, tilt: 0).frame(maxWidth: 320)
                    if let name = entry.imageFilename, let image = world.memoryImage(named: name) {
                        ShareLink(item: Image(uiImage: image), preview: SharePreview(entry.text.isEmpty ? "Tiny Us memory" : entry.text, image: Image(uiImage: image))) {
                            Label("Save or share", systemImage: "square.and.arrow.up")
                        }.buttonStyle(.bordered)
                    }
                    Button(role: .destructive) { world.removeMemory(entry); inspected = nil } label: { Label("Delete this memory", systemImage: "trash") }
                }.padding(24)
            }
            .navigationTitle("Memory").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .topBarLeading) { Button("Back") { inspected = nil } } }
        }.presentationDetents([.large])
    }
}

// MARK: - Polaroid capture

/// Renders a calm, still frame of the current place into a Polaroid card image.
@MainActor enum TinyPolaroid {
    static func render(scene: TinyScene, weather: TinyWeather, outfit: Int, collar: Int) -> UIImage? {
        let painter = TinyWorldPainter(scene: scene, weather: weather, previousWeather: nil, weatherBlend: 1, outfit: outfit, collar: collar,
                                       hour: Calendar.current.component(.hour, from: Date()), t: 12, now: TinyStage.now,
                                       stage: .resting(scene))
        let card = VStack(spacing: 10) {
            Canvas(opaque: true) { graphics, size in painter.paint(&graphics, size: size) }
                .frame(width: 320, height: 360)
            Text(scene.title).font(.system(size: 18, weight: .semibold, design: .serif)).italic().foregroundColor(Color(white: 0.25))
            Text(Date().formatted(date: .long, time: .omitted)).font(.system(size: 11, design: .rounded)).foregroundColor(Color(white: 0.5))
        }
        .padding(16).padding(.bottom, 14).background(Color(white: 0.98))
        let renderer = ImageRenderer(content: card)
        renderer.scale = 2
        return renderer.uiImage
    }
}
