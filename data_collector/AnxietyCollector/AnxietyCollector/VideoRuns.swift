//
//  VideoRuns.swift
//  AnxietyCollector
//
//  Created by Fabian Schnider on 04.06.19.
//  Copyright © 2019 Team2. All rights reserved.
//

import Foundation

class VideoRuns {
    
    static let shared = VideoRuns()
    
    private var videoFolderURL: URL!
    private var videoURLs = [URL]()
    var runs = [[URL]]()
    
    init() {
        let fileManager = FileManager.default
        let userDIR = Bundle.main.bundleURL
        videoFolderURL = userDIR.appendingPathComponent("Contents/Resources")
        
        do {
            let contents = try fileManager.contentsOfDirectory(atPath: videoFolderURL.path)
            videoURLs = contents.map { return videoFolderURL.appendingPathComponent($0) }
        } catch {
            videoURLs = []
        }
        
        videoURLs.removeAll { (url) -> Bool in
            return url.pathExtension != "mp4"
        }
        
        videoURLs.sort { (first, second) -> Bool in
            first.absoluteString.compare(second.absoluteString,
                                         options: .numeric
                ) == .orderedAscending
        }
        
        var currentIndex = 0
        var urlArray = [URL]()
        for url in videoURLs {
            guard let index = Int(url.lastPathComponent.prefix(1)) else { continue }
            if currentIndex != index {
                currentIndex = index
                if !urlArray.isEmpty {
                    self.runs.append(urlArray)
                }
                urlArray.removeAll()
            }
            urlArray.append(url)
        }
    }
}
