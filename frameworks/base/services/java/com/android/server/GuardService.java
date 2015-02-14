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
    private boolean running = true;
    private Context ctx;
    private Thread t;
    private Handler handler;
    private volatile Thread[] txDelayThread = new Thread[CovertTransceiver.COVERT_NUMS]; // yytang

    protected CovertSender[] activeTx = new CovertSender[CovertTransceiver.COVERT_NUMS];
    //protected ArrayList<CovertReceiver> activeRx = new ArrayList<CovertReceiver>();
    //protected ArrayList<CovertSender> activeTx = new ArrayList<CovertSender>();
    //private ArrayList<GSTimer> gsTimers = new ArrayList<GSTimer>();
    
    private boolean[] channelDefenseState = new boolean[5];
    
    
	/**
	* @hide
	* */
    public GuardService(Context newCTX){
        super();
        Log.d(TAG, "Constructing GuardService");
        ctx = newCTX;
        initGuardService();
        t = new GuardServiceThread("Guard Service Thread");
        t.start();
    }

    private void initGuardService(){
        activeTx[0] = new CovertSender("speaker");
        activeTx[1] = new CovertSender("vibrator");
        activeTx[2] = new CovertSender("flash");
        activeTx[3] = new CovertSender("user");
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
                case GuardServiceHelper.ADD_ACTIVE_TX: // add sender
                	tx = (CovertSender)msg.obj;
                    addTxInstances(tx);
                    getActiveSenders();
                	//updateChannels();
                	break;
                	
                case GuardServiceHelper.REMOVE_ACTIVE_TX: // remove sender
                	tx = (CovertSender)msg.obj;
                	removeTxInstances(tx);
                    getActiveSenders();
                	//updateChannels();
                	break;
            }
        }
    }

    private void addTxInstances(CovertSender tx){
        int index = tx.getDeviceID();
		
		// designed for Audiotrack
		if(!tx.flag)
        	activeTx[index].taint = tx.taint;
		else
			activeTx[index].flag = tx.flag;
		
        switch(index){
            case 0:
                activeTx[index].taint |= Taint.TAINT_SPKR;
                break;
            case 1:
                activeTx[index].taint |= Taint.TAINT_VIB;
                break;
            case 2:
                activeTx[index].taint |= Taint.TAINT_FLASH;
                break;
            case 3:
                activeTx[index].taint |= Taint.TAINT_USER;
                break;
        }
    }

    private void removeTxInstances(CovertSender tx){
        int index = tx.getDeviceID();
        activeTx[index].taint = Taint.TAINT_CLEAR;
		activeTx[index].flag = false;
    }
	
    /**
     * @hide
     */
    public void addActiveTx(CovertTransceiver trans){
        Log.d(TAG, "addActiveSink called");
        CovertSender tx = trans.toCovertSender();
        activeTxChangeMessage(tx, GuardServiceHelper.ADD_ACTIVE_TX);
    }
    
    /**
     * @hide 
     */
    public void removeActiveTx(CovertTransceiver trans){
        Log.d(TAG, "removeActiveSink called");
        CovertSender tx = trans.toCovertSender();
        activeTxChangeMessage(tx, GuardServiceHelper.REMOVE_ACTIVE_TX);
    }

    private void activeTxChangeMessage(CovertSender tx, int msgType){
        Message msg = Message.obtain();
        msg.what = msgType;
        msg.obj = tx;
        Log.i("activeTxChangeMessage", "1");
        handler.dispatchMessage(msg);

        // yytang
        int index = tx.getDeviceID();
        if(msgType == GuardServiceHelper.ADD_ACTIVE_TX){
            if(tx.delay > 0) {
                txDelayThread[index] = new execTxDelayThread(tx);
                txDelayThread[index].start();
            }
            else
                txDelayThread[index] = null;
        }
        else
            txDelayThread[index] = null;
    }

    private class execTxDelayThread extends Thread {
        private long delay;
        private int index;
        private String deviceName;

        public execTxDelayThread(CovertSender tx){
            delay = tx.delay;
            index = tx.getDeviceID();
            deviceName = tx.getDeviceName();
        }

        public void run(){
            Log.d(TAG, "execTxDelayThread Started");
            Thread thisThread = Thread.currentThread();
            try {
                // we need to remove vibrator from active tx array after sleep
                sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Log.i(deviceName, "after sleep()");
            // if no new thread is created
            if(thisThread == txDelayThread[index]){
                Message msg = Message.obtain();
                msg.what = GuardServiceHelper.REMOVE_ACTIVE_TX;
                msg.obj = new CovertSender(deviceName);
                Log.d(deviceName, "remove device from tx array");
                handler.dispatchMessage(msg);
            }
        }
    }

    
    
    /**
     * @hide - yytang
     */
    public int getTaint(CovertTransceiver trans){
        // find out the Tx for each deviceName
        // check if Tx is in the activeTx array
        // return the Tx ID of if covert channel is found, otherwise return -1
        //
        // We are concerned with five channels:
        // 1.) Ultrasound
        // 2.) Vib + Accel
        // 3.) Speaker + Accel
        // 4.) Flash + Cam
        // 5.) User + Gryo

		CovertReceiver rx = trans.toCovertReceiver();

        int taint = 0;
        CovertSender tx;
        Log.d("isCovertPresent", "" + rx.getDeviceName());
        // Speaker + Microphone
        if(rx.getDeviceName().equalsIgnoreCase("microphone")){
			if(activeTx[CovertTransceiver.DEV_SPKR].flag)
            	taint = taint | activeTx[CovertTransceiver.DEV_SPKR].taint;
        }

        // Vib + Accel
        if(rx.getDeviceName().equalsIgnoreCase("accelerometer")){
            taint = taint | activeTx[CovertTransceiver.DEV_VIB].taint;
        }

        // Speaker + Accel
        if(rx.getDeviceName().equalsIgnoreCase("accelerometer")){
			if(activeTx[CovertTransceiver.DEV_SPKR].flag)
            	taint = taint | activeTx[CovertTransceiver.DEV_SPKR].taint;
        }

        // Flash + Cam
        if(rx.getDeviceName().equalsIgnoreCase("camera")){
            taint = taint | activeTx[CovertTransceiver.DEV_FLASH].taint;
        }

        // User + Gryo
        if(rx.getDeviceName().equalsIgnoreCase("Gyroscope")){
            taint = taint | activeTx[CovertTransceiver.DEV_USER].taint;
        }
        if(taint != 0)
            Log.i("getTaint", "Find covert channel");
        else
            Log.i("getTaint", "No covert channel");
        return taint;
    }






    public String getActiveSenders(){
        Log.i("getActiveSenders", "length = " + activeTx.length);
        String s = "activeTx : ";
        for(int i = 0; i < activeTx.length; i ++){
            Log.i("getActiveSenders", "taint" + i + "=" + activeTx[i].taint);
			Log.i("getActiveSenders", "flag" + i + "=" + activeTx[i].flag);
            if(activeTx[i].taint != 0)
                s = s + activeTx[i].getDeviceName() + " ";
        }
        Log.i("getActiveSenders", "222");
        Log.d(TAG, s);

        return s;
    }
    
    /**
     * @hide
     * @return tells whether or not the GuardService is running
     * The service is actually always running, but this variable
     * will be checked in the various data delivery methods (such as dispatchSensorEvent)
     * so we can do time profiling with / without the service running easily
     */
    public boolean isRunning(){
    	return running;
    }
    
    /**
     * @hide
     * @param see isRunning() for more details
     */
    public void setRunning(boolean newVal){
    	running = newVal;
    }
}
