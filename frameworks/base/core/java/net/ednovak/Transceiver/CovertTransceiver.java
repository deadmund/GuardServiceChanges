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
    public long delay; // yytang
	// Should use the taint values defined in the TAINT class
	public int taint = Taint.TAINT_CLEAR;
	public int  flag = 0;
    

    public static final int COVERT_NUMS = 4;

    // These aren't used but they should be instead of the sensor names throughout the project
    public static final int DEV_UNKNOWN = -1;
    public static final int DEV_SPKR = 0;
    public static final int DEV_VIB = 1;
    public static final int DEV_FLASH = 2;
    public static final int DEV_USER = 3;

    public static final int DEV_MIC = 4;
    public static final int DEV_ACCEL = 5;
    public static final int DEV_CAM = 6;
    public static final int DEV_GYRO = 7;
	private final String[] namesTx = {"speaker", "vibrator", "flash", "user"};
    private final String[] namesRx = {"microphone", "Accelerometer", "camera", "Gyroscope"};
	
	final static public int TYPE_SENDER = 1;
	final static public int TYPE_RECEIVER = 2;
	
	public CovertTransceiver(int newDeviceID, String newDeviceName, int newType){

        ts = System.currentTimeMillis();

        type = newType;
        if(newType != TYPE_SENDER && newType != TYPE_RECEIVER){
            throw new IllegalArgumentException("Invalid Type: " + newType);
        }
        String[] names;
        if(type == TYPE_RECEIVER)
            names = namesRx;
        else
            names = namesTx;

        if(newDeviceID >= 0 && newDeviceID < COVERT_NUMS){
            deviceID = newDeviceID;
            deviceName = names[newDeviceID];
        }
        else{
            deviceName = null;
            for(int i = 0; i < COVERT_NUMS; i ++ ){
                if(names[i].equalsIgnoreCase(newDeviceName)){
                    deviceName = newDeviceName;
                    deviceID = i;
                }
            }

            if(deviceName == null){
                deviceName = new String("unknown device");
                deviceID = DEV_UNKNOWN;
            }
        }
	}
/*
	private void init(String newComponentName, int newType){
		ts = System.currentTimeMillis();
		
		type = newType;
		if(newType != TYPE_SENDER && newType != TYPE_RECEIVER){
			throw new IllegalArgumentException("Invalid Type: " + newType);
		}
	}
	
	private void setDeviceID(String devString){
		if(devString.equalsIgnoreCase("unknown device")){
			this.deviceID = DEV_UNKNOWN;
			return;
		}
		
		for(int i = 0; i < names.length; i++){
			if(devString.equals(names[i])){
				this.deviceID = i;
				return;
			}
		}

		this.deviceID = DEV_UNKNOWN;
		return;
			
	}
	*/
	
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
		this.delay = in.readLong();
		this.flag = in.readInt();
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
		dest.writeLong(delay);
		dest.writeInt(flag);
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
		CovertReceiver rx = new CovertReceiver(this.deviceName);
		return rx;
	}
	
	public CovertSender toCovertSender(){
		assert(this.type == TYPE_SENDER);
		CovertSender tx = new CovertSender(this.deviceName);
        tx.delay = this.delay;
        tx.taint = this.taint;
		tx.flag = this.flag;
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

