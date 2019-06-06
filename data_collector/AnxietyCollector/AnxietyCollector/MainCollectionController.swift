//
//  MainCollectionController.swift
//  AnxietyCollector
//
//  Created by Fabian Schnider on 04.06.19.
//  Copyright © 2019 Team2. All rights reserved.
//

import Cocoa
import AVKit
import NotificationCenter

class MainCollectionController: NSViewController {
    
    @IBOutlet weak var clipLabel: NSTextField!
    @IBOutlet weak var arouselSelection: NSPopUpButton!
    @IBOutlet weak var valenceSelection: NSPopUpButton!
    @IBOutlet var nextButton: NSButton!
    
    private var playerView: AVPlayerView?
    
    var runs: [[URL]]
    var currentRun = 0
    var currentClip = 0
    
    init() {
        runs = VideoRuns.shared.runs
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        runs = VideoRuns.shared.runs
        super.init(coder: coder)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setup()
    }
    
    private func setup() {
        arouselSelection.removeAllItems()
        valenceSelection.removeAllItems()
        
        arouselSelection.addItems(withTitles:
            [
                "-2 Exited (Good or Bad)",
                "-1 Wide-awake",
                "0 Neutral",
                "1 Dull",
                "2 Calm"
            ]
        )
        
        valenceSelection.addItems(withTitles:
            [
                "-2 Unpleasant",
                "-1 Unsatisfied",
                "0 Neutral",
                "1 Pleased",
                "2 Pleasant"
            ]
        )
        
        resetViews()
    }
    
    public func start() {
        let presOptions: NSApplication.PresentationOptions = [.fullScreen, .autoHideMenuBar]
        let optionsDictionary = [NSView.FullScreenModeOptionKey.fullScreenModeApplicationPresentationOptions: presOptions]
        view.enterFullScreenMode(NSScreen.main!, withOptions: optionsDictionary)
        view.wantsLayer = true
        
        data.removeAll()
        currentRun = 0
        currentClip = 0
        nextRun()
    }
    
    private func resetViews() {
        arouselSelection.selectItem(at: 2)
        valenceSelection.selectItem(at: 2)
        clipLabel.stringValue = "Run \(currentRun) - Clip \(currentClip)"
        
        self.nextButton.target = self
        self.nextButton.action = #selector(nextPressed(_:))
    }
    
    private func nextRun() {
        currentRun += 1
        currentClip = 1
        
        if self.runs.count < currentRun {
            self.finishCollection()
            return
        }
        
        startClip(url: self.runs[currentRun - 1][currentClip - 1])
        resetViews()
    }
    
    @objc func nextPressed(_ sender: NSButton) {
        saveData()
        
        currentClip += 1
        
        if self.runs[currentRun - 1].count < currentClip {
            nextRun()
            return
        }
        
        startClip(url: self.runs[currentRun - 1][currentClip - 1])
        resetViews()
    }
    
    
    // MARK: DATA
    
    private var heartrateData = [Int]()
    private var gsrData = [Int]()
    private var data = [String: [String:Any]]()
    
    private func startSensorCollection() {
        heartrateData.removeAll()
        gsrData.removeAll()
        GattClient.shared.startLoop { (name, value) in
            switch name {
            case "HR":
                self.heartrateData.append(value)
            case "SR":
                self.gsrData.append(value)
            default:
                break
            }
        }
    }
    
    private func saveData() {
        guard   let arouselString = self.arouselSelection.selectedItem?.title.split(separator: " ").first,
                let valenceString = self.valenceSelection.selectedItem?.title.split(separator: " ").first,
                let arousel = Int(arouselString),
                let valence = Int(valenceString)
        else { return }
        data["run\(self.currentRun)_clip\(self.currentClip)"] = [
            "valence": valence,
            "arousel": arousel,
            "heartrate": heartrateData,
            "gsr": gsrData
        ]
        debugPrint(data)
    }
    
    private func finishCollection() {
        debugPrint(data)
        if let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            let fileUrl = documentsURL.appendingPathComponent("dump_\(UUID().uuidString).json")
            try? data.json.write(to: fileUrl, atomically: true, encoding: .utf8)
        }
        NSApplication.shared.terminate(self)
    }
    
    
    // MARK: Video Player
    
    private func startClip(url: URL) {
        startSensorCollection()
        
        self.playerView = AVPlayerView()
        self.view.addSubview(self.playerView!)
        
        playerView?.translatesAutoresizingMaskIntoConstraints = false
        playerView?.topAnchor.constraint(equalTo: self.view.topAnchor).isActive = true
        playerView?.bottomAnchor.constraint(equalTo: self.view.bottomAnchor).isActive = true
        playerView?.leadingAnchor.constraint(equalTo: self.view.leadingAnchor).isActive = true
        playerView?.trailingAnchor.constraint(equalTo: self.view.trailingAnchor).isActive = true
        
        playerView?.player = AVPlayer(url: url)
        // playerView?.controlsStyle = .none
        playerView?.player?.play()
        
        NotificationCenter.default.addObserver(forName: .AVPlayerItemDidPlayToEndTime, object: nil, queue: nil, using: self.playerDidFinishPlaying(_:))
    }
    
    @objc private func playerDidFinishPlaying(_ notification: Notification) {
        GattClient.shared.stopLoop()
        NotificationCenter.default.removeObserver(self)
        playerView?.removeFromSuperview()
        playerView = nil
        resetViews()
    }
}

extension Dictionary {
    
    var json: String {
        let invalidJson = "Not a valid JSON"
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: self, options: .prettyPrinted)
            return String(bytes: jsonData, encoding: String.Encoding.utf8) ?? invalidJson
        } catch {
            return invalidJson
        }
    }
    
}
