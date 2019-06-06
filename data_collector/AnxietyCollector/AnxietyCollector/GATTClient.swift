//
//  GATTClient.swift
//  AnxietyCollector
//
//  Created by Fabian Schnider on 04.06.19.
//  Copyright © 2019 Team2. All rights reserved.
//

import Foundation
import CoreBluetooth

class GattClient : CBCentralManager, CBCentralManagerDelegate, CBPeripheralDelegate {
    
    static let shared = GattClient()

    typealias CharacteristicHandler = (String, Int) -> Void
    private var characteristicHandler: CharacteristicHandler?
    
    var loopIsRunning = false
    var isReady = false
    private var timer: Timer?
    
    private let anxietyServiceCBUUID = CBUUID(string: "0x337D")
    private let heartRateCBUUID = CBUUID(string: "0x1A12")
    private let gsrCBUUID = CBUUID(string: "0x1A11")
    
    private var anxietyDevice: CBPeripheral!
    private var heartRateCharacteristic: CBCharacteristic?
    private var gsrCharacteristic: CBCharacteristic?
    
    convenience init() {
        self.init(delegate: nil, queue: nil, options: nil)
    }
    
    override init(delegate: CBCentralManagerDelegate?, queue: DispatchQueue?, options: [String : Any]? = nil) {
        super.init(delegate: delegate, queue: queue, options: options)
        self.delegate = self
    }
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .unknown:
            print("central.state is .unknown")
        case .resetting:
            print("central.state is .resetting")
        case .unsupported:
            print("central.state is .unsupported")
        case .unauthorized:
            print("central.state is .unauthorized")
        case .poweredOff:
            print("central.state is .poweredOff")
        case .poweredOn:
            print("central.state is .poweredOn")
            self.scanForPeripherals(withServices: [self.anxietyServiceCBUUID], options: nil)
        @unknown default:
            print("central.state is unknown")
        }
    }
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        self.anxietyDevice = peripheral
        self.stopScan()
        connect(self.anxietyDevice, options: nil)
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("Connected with " + (peripheral.name ?? ""))
        self.anxietyDevice.delegate = self
        self.anxietyDevice.discoverServices([anxietyServiceCBUUID])
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let services = peripheral.services else { return }
        for service in services {
            if service.uuid == anxietyServiceCBUUID {
                anxietyDevice.discoverCharacteristics([heartRateCBUUID, gsrCBUUID], for: service)
                break
            }
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let characteristics = service.characteristics else { return }
        for characteristic in characteristics {
            switch characteristic.uuid {
            case heartRateCBUUID:
                heartRateCharacteristic = characteristic
            case gsrCBUUID:
                gsrCharacteristic = characteristic
            default:
                break
            }
        }
        print("Characteristics are ready")
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        var name = ""
        switch characteristic.uuid {
        case heartRateCBUUID:
            name = "HR"
        case gsrCBUUID:
            name = "SR"
        default:
            return
        }
        
        guard let data = characteristic.value else { return }
        let value = [UInt8](data).reversed().reduce(0) { v, byte in
            return v << 8 | Int(byte)
        }
        characteristicHandler?(name, value)
    }
    
    public func startLoop(_ handler: @escaping CharacteristicHandler) {
        guard !loopIsRunning else { return }
        self.loopIsRunning = true
        
        self.characteristicHandler = handler
        self.timer?.invalidate()
        self.timer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { _ in
            if !self.loopIsRunning {
                self.timer?.invalidate()
                return
            }
            
            if let hrChar = self.heartRateCharacteristic {
                self.anxietyDevice.readValue(for: hrChar)
            }
            if let srChar = self.gsrCharacteristic {
                self.anxietyDevice.readValue(for: srChar)
            }
        }
    }
    
    public func stopLoop() {
        self.loopIsRunning = false
    }
}

extension Data {
    struct HexEncodingOptions: OptionSet {
        let rawValue: Int
        static let upperCase = HexEncodingOptions(rawValue: 1 << 0)
    }
    
    func hexEncodedString(options: HexEncodingOptions = []) -> String {
        let format = options.contains(.upperCase) ? "%02hhX" : "%02hhx"
        return map { String(format: format, $0) }.joined()
    }
}
