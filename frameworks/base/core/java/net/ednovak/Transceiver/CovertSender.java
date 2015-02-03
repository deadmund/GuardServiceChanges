package net.ednovak.Transceiver;

public class CovertSender extends CovertTransceiver {

	/*
	 * @param componentName string name of the android component controlling this device
	 * @param deviceName string name of the device
	 */
	public CovertSender(String componentName, String deviceName){
		super(componentName, deviceName, CovertTransceiver.TYPE_SENDER);
	}
	
	/*
	 * @param componentName string name of hte android component controlling this device
	 * @param deviceID integer representing the device.  Integers for common devices are provided in GuardServiceHelper
	 */
	public CovertSender(String componentName, int deviceID){
		super(componentName, deviceID, CovertTransceiver.TYPE_SENDER);
	}
}
