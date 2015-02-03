package com.android.server;

import java.util.ArrayList;

import dalvik.system.Taint;

import net.ednovak.CameraTimer;
import net.ednovak.GuardServiceHelper;
import net.ednovak.Transceiver.CovertReceiver;
import net.ednovak.Transceiver.CovertSender;
import net.ednovak.Transceiver.CovertTransceiver;
import android.content.Context;
import android.os.Handler;
import android.os.IGuardService;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.util.Log;
import android.widget.Toast;

public class GuardService extends IGuardService.Stub{
    private static final String TAG = GuardService.class.getName();
    
    private Context ctx;
    private Thread t;
    private Handler handler;
    
    protected ArrayList<CovertReceiver> activeRx = new ArrayList<CovertReceiver>();
    protected ArrayList<CovertSender> activeTx = new ArrayList<CovertSender>();
    private ArrayList<GSTimer> gsTimers = new ArrayList<GSTimer>();
    
    private boolean[] channelDefenseState = new boolean[5];
    
    
	/**
	* @hide
	* */
    public GuardService(Context newCTX){
        super();
        Log.d(TAG, "Constructing GuardService");
        ctx = newCTX;
        t = new GuardServiceThread("Guard Service Thread");
        t.start();
    }
    
    public static IGuardService getInstance(){
	    try{
	    	return IGuardService.Stub.asInterface(ServiceManager.getService("GuardService"));
	    } catch (Exception e){
	    	e.printStackTrace();
	    	throw new IllegalStateException("Exception creating igs");
	    }
    }
    
    private class GuardServiceThread extends Thread {
        public GuardServiceThread(String name){
            super(name);
        }
        
        public void run(){
            Log.d(TAG, "GuardServiceThread Started");
            Looper.prepare();
            handler = new GuardServiceHandler();
            Looper.loop();
        }
    }
    
    private class GuardServiceHandler extends Handler {
    	
    	private void printInfo(int type, CovertTransceiver trans){
        	Log.d(TAG, GuardServiceHelper.getTypeString(type) + "  device ID: " + trans.getDeviceID() + "  device name: " + trans.getDeviceName());
    	}
    	
        
        @Override
        public void handleMessage(Message msg){
        	
        	printInfo(msg.what, (CovertTransceiver)msg.obj);
        	
        	CovertReceiver rx;
        	CovertSender tx;
            switch(msg.what){
                    
                case GuardServiceHelper.ADD_ACTIVE_RX: // add a receiver
                    rx = (CovertReceiver)msg.obj;
                    activeRx.add(rx);
                    Log.d(TAG, getActiveReceivers());
                    updateChannels();
                    break;
                    
                case GuardServiceHelper.REMOVE_ACTIVE_RX: // remote receiver
                	rx = (CovertReceiver)msg.obj;
                	removeRxInstances(rx);
                	Log.d(TAG, getActiveReceivers());
                	updateChannels();
                	break;
                	
                case GuardServiceHelper.ADD_ACTIVE_TX: // add sender
                	tx = (CovertSender)msg.obj;
                	activeTx.add(tx);
                	Log.d(TAG, getActiveSenders());
                	updateChannels();
                	break;
                	
                case GuardServiceHelper.REMOVE_ACTIVE_TX: // remove sender
                	tx = (CovertSender)msg.obj;
                	removeTxInstances(tx);
                	Log.d(TAG, getActiveSenders());
                	updateChannels();
                	break;
            }
        }
    }
    
    private CovertSender getActiveSender(int sensorID){
		for(int i = 0; i < activeTx.size(); i++){
			//Log.d(TAG, "Looking for sensorType: " + sensorType + "  comparing to: " + list.get(i).getDeviceID() + " - " + list.get(i).getDeviceID() == sensorType);
			if(activeTx.get(i).getDeviceID() == sensorID){
				return  activeTx.get(i);
			}
		}
		return null;
    }
    
    private CovertReceiver getActiveReceiver(int sensorID){
		for(int i = 0; i < activeRx.size(); i++){
			//Log.d(TAG, "Looking for sensorType: " + sensorType + "  comparing to: " + list.get(i).getDeviceID() + " - " + list.get(i).getDeviceID() == sensorType);
			if(activeRx.get(i).getDeviceID() == sensorID){
				return  activeRx.get(i);
			}
		}
		return null;
    }
    
    
    private boolean isPresent(int sensorID, int transceiverType){
    	//ArrayList<? extends CovertTransceiver> list;
    	if(transceiverType == CovertTransceiver.TYPE_SENDER){
    		if(getActiveSender(sensorID) != null){
    			return true;
    		}
    	}
    	else if (transceiverType == CovertTransceiver.TYPE_RECEIVER){
    		if(getActiveReceiver(sensorID) != null){
    			return true;
    		}
    	}
    	else{
    		throw new IllegalArgumentException("Invalid Transceiver Type: " + transceiverType);
    	}
		return false;
    }
    
    private void updateChannels(){
    	// We are concerned with five channels:
    	// 1.) Ultrasound
    	// 2.) Vib + Accel
    	// 3.) Speaker + Accel
    	// 4.) Flash + Cam
    	// 5.) User + Gryo
    	int[] channels = {GuardServiceHelper.DEV_SPKR, GuardServiceHelper.DEV_MIC, 
    			GuardServiceHelper.DEV_VIB, GuardServiceHelper.DEV_ACCEL,
    			GuardServiceHelper.DEV_SPKR, GuardServiceHelper.DEV_ACCEL,
    			GuardServiceHelper.DEV_FLASH, GuardServiceHelper.DEV_CAM,
    			GuardServiceHelper.DEV_USER, GuardServiceHelper.DEV_GYRO};
    	
    	// Check for each channel
    	for(int i = 0; i < channels.length/2; i++){
    		int sender = channels[i*2];
    		int receiver = channels[(i*2) +1];
    		
    		if(isPresent(sender, CovertTransceiver.TYPE_SENDER) && isPresent(receiver, CovertTransceiver.TYPE_RECEIVER)){
    			// This is a bit hacky
    			// It works because the channels list above, 
    			// and the channel ints in GuardServiceHelper,
    			// and the channelDefenseState array all align
    			channelDefenseState[i] = true;
    			Log.d(TAG, "Channel present!  Index: " + i + "  sender int: " + sender + "  reciever int: " + receiver);
    		}
    		else{
    			channelDefenseState[i] = false; // erase the channels that aren't present
    		}
    	}
    	
    }
    
    
    /**
     * @hide
     */
    public void addActiveRx(CovertTransceiver trans){
    	//Log.d(TAG, "addActiveRx called");
    	CovertReceiver rx = trans.toCovertReceiver();
    	activeRxChangeMessage(rx, GuardServiceHelper.ADD_ACTIVE_RX);
    }
    
    /**
     * @hide
     */
    public void removeActiveRx(CovertTransceiver trans){
    	//Log.d(TAG, "removeActiveRx called");
    	// Downcasting!
    	CovertReceiver rx = trans.toCovertReceiver();
    	activeRxChangeMessage(rx, GuardServiceHelper.REMOVE_ACTIVE_RX);
    }
    
    private void activeRxChangeMessage(CovertReceiver rx, int msgType){
    	Message msg = Message.obtain();
    	msg.what = msgType;
    	msg.obj = rx;
    	handler.dispatchMessage(msg);
    }
    
    
    /**
     * @hide
     */
    public void addActiveTx(CovertTransceiver trans){
    	//Log.d(TAG, "addActiveSink called");
    	CovertSender tx = trans.toCovertSender();
    	activeTxChangeMessage(tx, GuardServiceHelper.ADD_ACTIVE_TX);
    }
    
    /**
     * @hide 
     */
    public void removeActiveTx(CovertTransceiver trans){
    	//Log.d(TAG, "removeActiveSink called");
    	CovertSender tx = trans.toCovertSender();
    	activeTxChangeMessage(tx, GuardServiceHelper.REMOVE_ACTIVE_TX);
    }
    
    private void activeTxChangeMessage(CovertSender tx, int msgType){
    	Message msg = Message.obtain();
    	msg.what = msgType;
    	msg.obj = tx;
    	handler.dispatchMessage(msg);
    }
    
    /**
     * @hide
     */
    public String getActiveReceivers(){
    	return getActiveList(CovertTransceiver.TYPE_RECEIVER);
    }
    
    /**
     * @hide
     */
    public String getActiveSenders(){
    	return getActiveList(CovertTransceiver.TYPE_SENDER);
    }
    
    
    /**
     * @hide
     */
    public boolean checkChannels(int[] chans){
    	//Log.d(TAG, "Checking for channels");
    	// Returns "true" if any of the the channels are currently active;
    	for(int i = 0; i < chans.length; i++){
    		// This is a little bit "hacky" but it works
    		// because the ints defined in the GuardServiceHelper
    		// happen to align nicely with the channelDefenseState arr
    		int val = chans[i];
    		if(val < 0 || val > GuardServiceHelper.CHAN_USER_GYRO){
    			throw new IllegalArgumentException("Invalid channel int: " + val + "  Please use final static int values defined in GuardServiceHelper");
    		}
    		//Log.d(TAG, "Checking for channel number " + i + "  channel_ID: " + chans[i] + "  state: " + channelDefenseState[chans[i]]);
    		if(channelDefenseState[chans[i]]){
    			return true;
    		}
    	}
    	return false;
    }
    
    
    private String getActiveList(int LIST_TYPE){
    	if(LIST_TYPE == CovertTransceiver.TYPE_SENDER){
    		return "Active Sender List[" + activeTx.size() +"]: " + activeTx;
    	}
    	else if(LIST_TYPE == CovertTransceiver.TYPE_RECEIVER){
    		return "Active Receiver List[" + activeRx.size() +"]: " + activeRx;
    	}
    	else{
    		throw new IllegalArgumentException("Invalid type: " + LIST_TYPE);
    	}
    	
    }
    
    private static void removeInstances(ArrayList<? extends CovertTransceiver> l, CovertTransceiver trans){
    	// Some assert statement about the objects in l 
    	// matching the actual type of trans would probably
    	// be prudent here
    	
    	for(int i = 0; i < l.size(); i++){
    		CovertTransceiver item = (CovertTransceiver) l.get(i);
    		if(item.lazyMatches(trans)){
    			l.remove(i);
    			i--;
    		}
    	}
    }
    
    private void removeRxInstances(CovertReceiver rx){
    	removeInstances(activeRx, (CovertTransceiver)rx);
    }
    
    private void removeTxInstances(CovertSender tx){
    	removeInstances(activeTx, (CovertTransceiver)tx);
    }
    
  /**
   * @hide 
   */
    public void clearList(int type){
    	if(type == CovertTransceiver.TYPE_SENDER){
    		clearTxList();
    	}
    	else if(type == CovertTransceiver.TYPE_RECEIVER){
    		clearRxList();
    	}
    	else{
    		throw new IllegalArgumentException("Invalid Type: " + type);
    	}
    	
    	updateChannels();
    }
    
    private void clearRxList(){
    	activeRx = new ArrayList<CovertReceiver>();
    }
    
    private void clearTxList(){
    	activeTx = new ArrayList<CovertSender>();
    }
    
    
    /**
     * 
     * @param tag the new taint value to be added (bitwise OR)
     * @param dev the ID of the device to store the taint value for
     * @hide
     */
    public void combineTaint(int tag, CovertTransceiver trans){
    	CovertTransceiver newTrans = null;
    	switch(trans.getType()){
    	case CovertTransceiver.TYPE_SENDER:
    		newTrans = getActiveSender(trans.getDeviceID());
    		break;
    	case CovertTransceiver.TYPE_RECEIVER:
    		newTrans = getActiveReceiver(trans.getDeviceID());
    		break;
    	}

    	if(newTrans == null){
    		Log.w(TAG, "Device (" + trans.getDeviceName() + ") is not active.  Cannot combine taint: " + tag);
    		return;
    	}
    	newTrans.taint = (newTrans.taint | tag);
    }
    
    /**
     * 
     * @param tag the new taint value for the device (replaces old value for device)
     * @param dev the ID of the device to store the taint value for
     * @hide
     */
    public void setTaint(int tag, CovertTransceiver trans){
    	CovertTransceiver newTrans = null;
    	
    	switch(trans.getType()){
    	case CovertTransceiver.TYPE_SENDER:
    		newTrans = getActiveSender(trans.getDeviceID());
    		break;
    	
    	case CovertTransceiver.TYPE_RECEIVER:
    		newTrans = getActiveReceiver(trans.getDeviceID());
    		break;
    	}
    	
    	if(newTrans == null){
    		Log.w(TAG, "Device (" + trans.getDeviceName() + ") is not active.  Cannot set taint: " + tag);
    		return;
    	}
    	newTrans.taint = tag;
    }
    
    /**
     * 
     * @param trans a dummy CovertTransceiver representing the device to retrieve the taint value of
     * @return the taint value (as a short)
     */
    public int getTaint(CovertTransceiver trans){
    	
    	CovertTransceiver newTrans = null;
    	switch(trans.getType()){
    	case CovertTransceiver.TYPE_RECEIVER:
    		newTrans = getActiveReceiver(trans.getDeviceID());
    		break;
    		
    	case CovertTransceiver.TYPE_SENDER:
    		newTrans = getActiveSender(trans.getDeviceID());
    		break;
    	}

    	if(newTrans == null){ 
    		Log.w(TAG, "Selected device (" + trans.getDeviceName() + ") is not active, returning TAINT_CLEAR.");
    		return Taint.TAINT_CLEAR;
    	}
    	else{ return newTrans.taint; }
    }
    
    /**
     * 
     * @param time Time in ms until this Timer will fire.  Timer will wait at least this long.  If this is a bump, time will be added to the existing timer
     * @param dev The device to be removed when the timer is fired.  This device uniquely identifies a timer
     * @hide
     */
    public void setOrBumpTimer(long time, CovertTransceiver dev){
    	GSTimer t;
    	try{
    		t = getTimer(dev);
    		t.bumpDuration(time);
    		Log.d(TAG, "setOrBumpTimer, adding: " + time + " ms to timer for: " + dev.getDeviceName());
    	} catch (IllegalArgumentException e){
    		Log.d(TAG, "setOrBumpTimer, Creating New GSTimer with " + time + "ms for: " + dev.getDeviceName());
    		t = new GSTimer(time, dev);
    		gsTimers.add(t);
    	}
    	startTimerHelper(t);
    }
    
    /**
     * 
     * @param dev the device used to identify the timer
     * @hide
     */
    public void startTimer(CovertTransceiver dev){
    	GSTimer timer = getTimer(dev);
    	startTimerHelper(timer);
    }
    
    private void startTimerHelper(GSTimer timer){
    	if(!timer.checkRunning()){
    		timer.setRunning(true);
    		Thread t = new Thread(timer);
        	Log.d(TAG, "Started timer with " + timer.getTimeRemaining() + " ms left for dev: " + timer.dev.getDeviceName());
    		t.start();
    	}
    }
    
    
    private GSTimer getTimer(CovertTransceiver dev){
    	GSTimer tmp = new GSTimer(-1, dev);
    	for(int i = 0; i < gsTimers.size(); i++){
    		if(gsTimers.get(i).equals(tmp)){
    			return gsTimers.get(i);
    		}
    	}
    	throw new IllegalArgumentException("No timer for device: " + dev);
    }
    
    // Guard Service Timer
    private class GSTimer implements Runnable{
    	private final String TAG = GSTimer.class.getName();
    	
    	private long timeRemaining;
    	private long lastCheckTS;
    	private boolean running = false;
    	private CovertTransceiver dev;
    	
    	public GSTimer(long newTimeRemaining, CovertTransceiver newDev){
    		timeRemaining = newTimeRemaining;
    		lastCheckTS = System.currentTimeMillis();
    		dev = newDev;
    	}
    	
    	@Override
    	public void run(){
    		Log.d(TAG, "GSTimer thread running");
    		while(running){
    			long nowTS = System.currentTimeMillis();
    			long dur = (nowTS - lastCheckTS);
    			lastCheckTS = nowTS;
    			timeRemaining = timeRemaining - dur;
    			if(timeRemaining <= 0){
    				Log.d(TAG, "TIMER UP!");
    				timeRemaining = 0;
    				running = false;
    				removeDev();
    			}
    		}
    	}
    	
    	private void removeDev(){
    		if(dev.getType() == CovertTransceiver.TYPE_SENDER){
    			removeActiveTx(dev);
    		}
    		else if(dev.getType() == CovertTransceiver.TYPE_RECEIVER){
    			removeActiveRx(dev);
    		}
    		else{
    			throw new IllegalStateException("Invalid Type ID: " + dev.getType());
    		}
    		
    	}

    	public void setRunning(boolean onoff){ running = onoff; }
    	public boolean checkRunning(){ return running; }
    	public long getTimeRemaining(){ return timeRemaining; }
    	
    	public void bumpDuration(long bumpTime){
    		if(bumpTime < 0){
    			throw new IllegalArgumentException("bumpDuration received negative bumpTime: " + bumpTime);
    		}
    		timeRemaining = (timeRemaining + bumpTime);
    		Log.d(TAG, "Duration bumped.  Time remaining: " + timeRemaining);
    	}
    	
    	public boolean equals(GSTimer other){ return this.dev.getDeviceID() == other.dev.getDeviceID(); }
    	
    }
}
