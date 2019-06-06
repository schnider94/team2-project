//
//  ViewController.swift
//  AnxietyCollector
//
//  Created by Fabian Schnider on 04.06.19.
//  Copyright © 2019 Team2. All rights reserved.
//

import Cocoa
import AVKit

class ViewController: NSViewController {
        
    override func viewDidLoad() {
        super.viewDidLoad()
        _ = GattClient.shared
        // Do any additional setup after loading the view.
    }

    override var representedObject: Any? {
        didSet {
        // Update the view, if already loaded.
        }
    }

    @IBAction func startCollection(_ sender: NSButton) {
        let storyboard = NSStoryboard(name: "Main", bundle: nil)
        guard let vc = storyboard.instantiateController(withIdentifier: "MainCollection") as? MainCollectionController else { return }
        self.view = vc.view
        vc.start()
    }
}
