package net.ednovak.Transceiver;

public class CovertReceiver extends CovertTransceiver {

	/*
	 * @param componentName string name of the android component controlling this device
	 * @param deviceName string name of the device
	 */
	public CovertReceiver(String deviceName){
		super(-1, deviceName, CovertTransceiver.TYPE_RECEIVER);
	}
	
	/*
	 * @param componentName string name of hte android component controlling this device
	 * @param deviceID integer representing the device.  Integers for common devices are provided in GuardServiceHelper
	 */
	public CovertReceiver(int deviceID){
		super(deviceID, null, CovertTransceiver.TYPE_RECEIVER);
	}
}
