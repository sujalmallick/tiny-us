import SwiftUI
import Shared

struct ContentView: View {
    @State private var boyName: String = "Him"
    @State private var girlName: String = "Her"
    @State private var anniversaryDate: Date = Calendar.current.date(from: DateComponents(year: 2024, month: 1, day: 1)) ?? Date()
    @State private var relationshipDays: Int64 = 1
    @State private var timeOfDayText: String = "Golden Hour"
    @State private var dailyMessage: String = "Take a deep breath. You are doing wonderfully today."
    @State private var showSettings: Bool = false
    @State private var soundEnabled: Bool = true
    
    let timer = Timer.publish(every: 1.0, on: .main, in: .common).autoconnect()
    
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.12, green: 0.11, blue: 0.18), Color(red: 0.20, green: 0.18, blue: 0.28)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            VStack(spacing: 24) {
                // Header Bar
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("TINY US")
                            .font(.system(size: 20, weight: .black, design: .monospaced))
                            .foregroundColor(Color(red: 1.0, green: 0.85, blue: 0.55))
                        
                        Text(timeOfDayText.uppercased())
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                            .foregroundColor(Color(red: 0.8, green: 0.8, blue: 0.85))
                    }
                    
                    Spacer()
                    
                    Button(action: {
                        showSettings = true
                    }) {
                        Image(systemName: "heart.fill")
                            .font(.system(size: 18))
                            .foregroundColor(Color(red: 1.0, green: 0.45, blue: 0.55))
                            .padding(10)
                            .background(Color.white.opacity(0.1))
                            .clipShape(Circle())
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 16)
                
                Spacer()
                
                // Days Counter Card
                VStack(spacing: 8) {
                    Text("TOGETHER FOR")
                        .font(.system(size: 12, weight: .bold, design: .monospaced))
                        .foregroundColor(Color(red: 0.7, green: 0.7, blue: 0.8))
                    
                    Text("\(relationshipDays)")
                        .font(.system(size: 64, weight: .heavy, design: .rounded))
                        .foregroundColor(Color(red: 1.0, green: 0.85, blue: 0.45))
                    
                    Text("DAYS OF LOVE")
                        .font(.system(size: 13, weight: .black, design: .monospaced))
                        .foregroundColor(Color(red: 1.0, green: 0.6, blue: 0.7))
                    
                    HStack(spacing: 12) {
                        Text(boyName)
                            .font(.system(size: 15, weight: .bold, design: .monospaced))
                            .foregroundColor(Color(red: 0.6, green: 0.85, blue: 1.0))
                        
                        Image(systemName: "heart.fill")
                            .font(.system(size: 11))
                            .foregroundColor(Color(red: 1.0, green: 0.45, blue: 0.55))
                        
                        Text(girlName)
                            .font(.system(size: 15, weight: .bold, design: .monospaced))
                            .foregroundColor(Color(red: 1.0, green: 0.75, blue: 0.85))
                    }
                    .padding(.top, 6)
                }
                .padding(.vertical, 28)
                .padding(.horizontal, 32)
                .background(
                    RoundedRectangle(cornerRadius: 24)
                        .fill(Color(red: 0.16, green: 0.15, blue: 0.24))
                        .overlay(
                            RoundedRectangle(cornerRadius: 24)
                                .stroke(Color(red: 0.3, green: 0.28, blue: 0.4), lineWidth: 2)
                        )
                )
                .padding(.horizontal, 24)
                
                // Daily Tiny Care Message Card
                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        Image(systemName: "sparkles")
                            .foregroundColor(Color(red: 1.0, green: 0.85, blue: 0.45))
                        Text("TINY CARE")
                            .font(.system(size: 12, weight: .black, design: .monospaced))
                            .foregroundColor(Color(red: 1.0, green: 0.85, blue: 0.45))
                        Spacer()
                    }
                    
                    Text(dailyMessage)
                        .font(.system(size: 14, weight: .medium, design: .serif))
                        .foregroundColor(Color(red: 0.95, green: 0.95, blue: 0.98))
                        .lineSpacing(4)
                }
                .padding(20)
                .background(
                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color(red: 0.18, green: 0.17, blue: 0.27))
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(Color(red: 0.35, green: 0.32, blue: 0.48), lineWidth: 1)
                        )
                )
                .padding(.horizontal, 24)
                
                Spacer()
                
                // Bottom Interactive Bar
                HStack(spacing: 16) {
                    Button(action: {
                        refreshCareMessage()
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "arrow.clockwise")
                            Text("New Whisper")
                                .font(.system(size: 13, weight: .bold, design: .monospaced))
                        }
                        .foregroundColor(.white)
                        .padding(.vertical, 12)
                        .padding(.horizontal, 20)
                        .background(Color(red: 0.3, green: 0.25, blue: 0.45))
                        .cornerRadius(14)
                    }
                    
                    Button(action: {
                        playChime()
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "music.note")
                            Text("Play Chime")
                                .font(.system(size: 13, weight: .bold, design: .monospaced))
                        }
                        .foregroundColor(Color(red: 0.15, green: 0.1, blue: 0.2))
                        .padding(.vertical, 12)
                        .padding(.horizontal, 20)
                        .background(Color(red: 1.0, green: 0.85, blue: 0.45))
                        .cornerRadius(14)
                    }
                }
                .padding(.bottom, 24)
            }
        }
        .onAppear {
            loadInitialData()
        }
        .onReceive(timer) { _ in
            updateLiveCalculations()
        }
        .sheet(isPresented: $showSettings) {
            SettingsSheet(
                boyName: $boyName,
                girlName: $girlName,
                anniversaryDate: $anniversaryDate,
                onSave: {
                    saveProfile()
                    updateLiveCalculations()
                }
            )
        }
    }
    
    private func loadInitialData() {
        let storage = IosUserDefaultsStorage(defaults: UserDefaults.standard)
        boyName = storage.getString(key: "bf_name", defaultValue: "Him") ?? "Him"
        girlName = storage.getString(key: "gf_name", defaultValue: "Her") ?? "Her"
        
        if let savedDateStr = storage.getString(key: "anniversary_date", defaultValue: nil) {
            let formatter = ISO8601DateFormatter()
            formatter.formatOptions = [.withFullDate]
            if let d = formatter.date(from: savedDateStr) {
                anniversaryDate = d
            }
        }
        
        refreshCareMessage()
        updateLiveCalculations()
    }
    
    private func saveProfile() {
        let storage = IosUserDefaultsStorage(defaults: UserDefaults.standard)
        storage.putString(key: "bf_name", value: boyName)
        storage.putString(key: "gf_name", value: girlName)
        
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        storage.putString(key: "anniversary_date", value: formatter.string(from: anniversaryDate))
    }
    
    private func updateLiveCalculations() {
        let calendar = Calendar.current
        let startComponents = calendar.dateComponents([.year, .month, .day], from: anniversaryDate)
        let nowComponents = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: Date())
        
        if let sY = startComponents.year, let sM = startComponents.month, let sD = startComponents.day,
           let nY = nowComponents.year, let nM = nowComponents.month, let nD = nowComponents.day {
            
            relationshipDays = RelationshipTimeCalculator.shared.calculateDays(
                startYear: Int32(sY),
                startMonth: Int32(sM),
                startDay: Int32(sD),
                currentYear: Int32(nY),
                currentMonth: Int32(nM),
                currentDay: Int32(nD)
            )
        }
        
        let hour = Int32(nowComponents.hour ?? 12)
        let phase = TimeOfDayPhaseKt.currentPhase(hour: hour)
        timeOfDayText = phase.displayName
    }
    
    private func refreshCareMessage() {
        let allMessages = TinyCareMessagePool.shared.allMessages
        if !allMessages.isEmpty {
            let randomIndex = Int.random(in: 0..<allMessages.count)
            dailyMessage = allMessages[randomIndex].body
        }
    }
    
    private func playChime() {
        let sink = IosAudioSink()
        let synthesizer = ProceduralAudioSynthesizer.shared
        let buffer = synthesizer.synthesizeChime(frequency: 528.0, durationMs: 600, sampleRate: 22050, volume: 0.8)
        sink.playStaticBuffer(buffer: buffer, sampleRate: 22050, volume: 0.8)
    }
}

struct SettingsSheet: View {
    @Binding var boyName: String
    @Binding var girlName: String
    @Binding var anniversaryDate: Date
    let onSave: () -> Void
    
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Partners")) {
                    TextField("Boyfriend Name", text: $boyName)
                    TextField("Girlfriend Name", text: $girlName)
                }
                
                Section(header: Text("Anniversary Date")) {
                    DatePicker("Start Date", selection: $anniversaryDate, displayedComponents: .date)
                }
            }
            .navigationTitle("Tiny Us Settings")
            .navigationBarItems(
                trailing: Button("Done") {
                    onSave()
                    presentationMode.wrappedValue.dismiss()
                }
            )
        }
    }
}
