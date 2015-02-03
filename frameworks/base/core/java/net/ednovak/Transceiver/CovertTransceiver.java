package net.ednovak.Transceiver;

import dalvik.system.Taint;
import net.ednovak.GuardServiceHelper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class CovertTransceiver implements Parcelable {
	final private static String TAG = CovertTransceiver.class.getName();

	private String componentName;
	private String deviceName;
	private int deviceID;
	private int type;
	private long ts;
	// Should use the taint values defined in the TAINT class
	public int taint = Taint.TAINT_CLEAR;
	
    // These aren't used but they should be instead of the sensor names throughout the project
    public static final int DEV_UNKNOWN = -1;
    public static final int DEV_SPKR = 0;
    public static final int DEV_MIC = 1;
    public static final int DEV_VIB = 2;
    public static final int DEV_ACCEL = 3;
    public static final int DEV_FLASH = 4;
    public static final int DEV_CAM = 5;
    public static final int DEV_USER = 6;
    public static final int DEV_GYRO = 7;
	private final String[] names = {"speaker", "microphone", "vibration motor", "MPL Accelerometer", 
			"flash", "camera", "user", "MPL Gyroscope"};
	
	final static public int TYPE_SENDER = 1;
	final static public int TYPE_RECEIVER = 2;
	
	public CovertTransceiver(String newComponentName, int newDeviceID, int newType){
		init(newComponentName, newType);
		deviceID = newDeviceID;
		if(deviceID != -1){
			deviceName = names[newDeviceID];
		}
		else{
			deviceName = "unknown device";
		}
	}
	
	public CovertTransceiver(String newComponentName, String newDeviceName, int newType){
		init(newComponentName, newType);

		deviceName = newDeviceName;
		if (deviceName == "") { deviceName = null; }
		setDeviceID(deviceName);
		
	}
	
	
	private void init(String newComponentName, int newType){
		ts = System.currentTimeMillis();
		
		componentName = newComponentName;
		
		type = newType;
		if(newType != TYPE_SENDER && newType != TYPE_RECEIVER){
			throw new IllegalArgumentException("Invalid Type: " + newType);
		}
	}
	
	private void setDeviceID(String devString){
		if(devString == null){
			this.deviceID = GuardServiceHelper.DEV_UNKNOWN;
			return;
		}
		
		for(int i = 0; i < names.length; i++){
			if(devString.equals(names[i])){
				this.deviceID = i;
				return;
			}
		}
		
		this.deviceID = GuardServiceHelper.DEV_UNKNOWN;
		return;
			
	}
	
	public int getDeviceID() { return deviceID; }
	
	public String getComponentName(){ return componentName; }
	
	public String getDeviceName(){ return deviceName; }
	
	public long getTS(){ return ts; }
	
	public String getTypeString(){
		String res = "";
		switch(type){
		case TYPE_SENDER:
			res = "sender";
		case TYPE_RECEIVER:
			res = "receiver";
		}
		assert(res!="");
		return res;
	}
	
	public String toString(){ return getTypeString() + ":" + componentName + ":" + deviceName; }
	
	// Returns true if devices are matching in all aspects.
	// Also returns true (lazy) if all components are matching and the deviceID of either object is -1
	// This allows for unknown / new devices, as well as remove(component, null)
	// to function properly.  This is useful when removing from the active* lists
	public boolean lazyMatches(CovertTransceiver other){
		// If the other's name is null or "" then it matches
		// Android sometimes called unregisterlistener(listener)
		// In this case the sensor value is null and the listener should be turned off
		// for all sensors.
		// There is an optional unregisterlistener(listener, sensor) that removes
		// the listener for the given sensor only
		//boolean checkComp = this.componentName.equals(other.componentName);
		// I don't want to check the component name because
		// getting the component name is very buggy in many places
		
		
		boolean checkType = (this.type == other.type);
		boolean checkID = (other.deviceID == this.deviceID);
		boolean ans = checkType && checkID;
		//Log.d(TAG, "They're the same: " + ans);
		return ans;
	}
	

	public boolean equals(CovertTransceiver other){
		boolean checkComp = (this.componentName.equals(other.componentName));
		boolean checkType = (this.type == other.type);
		boolean checkID = (this.deviceID == other.deviceID);
		boolean checkDeviceName = (this.deviceName.equals(other.deviceName));
		return checkComp && checkType && checkID && checkDeviceName;
	}

	
	
	// Parcelable Stuff
	public CovertTransceiver(Parcel in){
		String[] stringInputs = new String[2];
		in.readStringArray(stringInputs);
		this.componentName = stringInputs[0];
		this.deviceName = stringInputs[1];
		this.deviceID = in.readInt();
		this.type = in.readInt();
		this.ts = in.readLong();
		this.taint = in.readInt();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		String[] stringOutputs = new String[] {this.componentName, this.deviceName};
		dest.writeStringArray(stringOutputs);
		dest.writeInt(deviceID);
		dest.writeInt(type);
		dest.writeLong(ts);
		dest.writeInt(taint);
	}
	
	public static Parcelable.Creator<CovertTransceiver> CREATOR = new Parcelable.Creator<CovertTransceiver>() {
		public CovertTransceiver createFromParcel(Parcel in){
			return new CovertTransceiver(in);
		}
		
		public CovertTransceiver[] newArray(int size){
			return new CovertTransceiver[size];
		}
	};
	
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	// This is necessary because if you create a CovertTransceiver, you 
	// cannot downcast it to CovertReciever.  So this easily allows that
	public CovertReceiver toCovertReceiver(){
		assert(this.type == TYPE_RECEIVER);
		CovertReceiver rx = new CovertReceiver(this.componentName, this.deviceName);
		return rx;
	}
	
	public CovertSender toCovertSender(){
		assert(this.type == TYPE_SENDER);
		CovertSender tx = new CovertSender(this.componentName, this.deviceName);
		return tx;
	}
	
	/**
	 * 
	 * @return an integer representing the type of this CovertTransceiver, should be either TYPE_SENDER or TYPE_RECEIVER
	 */
	public int getType(){
		return type;
	}
}

