package net.ednovak.Transceiver;

public class CovertSender extends CovertTransceiver {

	/*
	 * @param componentName string name of the android component controlling this device
	 * @param deviceName string name of the device
	 */
	public CovertSender(String deviceName){
		super(-1, deviceName, CovertTransceiver.TYPE_SENDER);
	}
	
	/*
	 * @param componentName string name of hte android component controlling this device
	 * @param deviceID integer representing the device.  Integers for common devices are provided in GuardServiceHelper
	 */
	public CovertSender(int deviceID){
		super(deviceID, null, CovertTransceiver.TYPE_SENDER);
	}

    public CovertSender(int deviceID, String deviceName){
        super(deviceID, deviceName, CovertTransceiver.TYPE_SENDER);
    }
}
