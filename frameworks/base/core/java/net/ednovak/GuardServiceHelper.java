package net.ednovak;

import net.ednovak.Transceiver.CovertTransceiver;
import android.os.IGuardService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class GuardServiceHelper {
	private static final String TAG = GuardServiceHelper.class.getName();
	
    public static final int ADD_ACTIVE_RX = 1;
    public static final int ADD_ACTIVE_TX = 2;
    public static final int REMOVE_ACTIVE_RX = 3;
    public static final int REMOVE_ACTIVE_TX = 4;
    
    public static final int CHAN_ULTRASOUND = 0;
    public static final int CHAN_VIB_ACCEL = 1;
    public static final int CHAN_SPKR_ACCEL = 2;
    public static final int CHAN_FLASH_CAM = 3;
    public static final int CHAN_USER_GYRO = 4;
    
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
	
    public static IGuardService getIGSInstance(){
	    try{
	    	return IGuardService.Stub.asInterface(ServiceManager.getService("GuardService"));
	    } catch (Exception e){
	    	e.printStackTrace();
	    	throw new IllegalStateException("Exception creating igs");
	    }
    }
    
    public static void debugHelper(IGuardService igs, CovertTransceiver trans){
		Log.d("GuardServiceHelper", "igs: " + igs.toString());
		Log.d("GuardServiceHelper", "component: " + trans.getComponentName());
		Log.d("GuardServiceHelper", "device Name: " + trans.getDeviceName());
		Log.d("GuardServiceHelper", "device ID: " + trans.getDeviceID());
    }
    
    
    public static boolean remoteExcProtectedCheckChannels(IGuardService igs, int[] chans){
    	boolean res = false;
    	try{
    		res = igs.checkChannels(chans);
    	}
    	catch(RemoteException e){
    		Log.d(TAG, "Caught Remote Exception");
    		e.printStackTrace();
    	}
    	return res;
    }

    // This takes a CovertTransceiver because the AIDL files do not like
    // the inherited parcelable implementation of CovertSender and CovertReceiver
    // It also tries to guard the "RemoteException"
    // but that doesn't work over processes anyway or something?
    // got this error once: E/JavaBinder(  404): *** Uncaught remote exception!  (Exceptions are not yet supported across processes.)
    public static void remoteExcProtectedActiveChange(IGuardService igs, CovertTransceiver trans, int type){
    	if(igs == null){

    		// This usually occurs because the service interface has been called before the service
    		// has actually been created by the system in SystemServer.java
    		Log.w(TAG, "GuardServiceHelper: Passed a null IGuardService.  Creating one now");
    		return;
    	}
    	
    	//Log.d(TAG, getTypeString(type) + "  device ID: " + trans.getDeviceID());
    	
    	// trans might be a CovertSender or a CovertReceiver or a CovertTransceiver
    	// for the pracelable / aidl stuff I need it to be a CovertTransceiver
    	trans = (CovertTransceiver)trans;
    	
    	//debugHelper(igs, trans);
    	
    	try{	
	    	switch(type){
	    	case ADD_ACTIVE_RX:
	    		igs.addActiveRx(trans);
	    		break;
	    	case REMOVE_ACTIVE_RX:
	    		igs.removeActiveRx(trans);
	    		break;
	    	case ADD_ACTIVE_TX:
	    		igs.addActiveTx(trans);
	    		break;
	    	case REMOVE_ACTIVE_TX:
	    		igs.removeActiveTx(trans);
	    		break;
	    	}
    	} catch (RemoteException e){
    		Log.d(TAG, "Caught Remote Exception");
    		e.printStackTrace();
    	}
    }
    
    public static void remoteExcProtectedSetOrBumpTimer(long dur, CovertTransceiver dev){
    	IGuardService igs = GuardServiceHelper.getIGSInstance();
    	try{
    		igs.setOrBumpTimer(dur, dev);
    	}
    	catch (RemoteException e){
    		Log.d(TAG, "Caught Remote Exception");
    		e.printStackTrace();
    	}
    }
    
   public static void remoteExcProtectedStartTimer(CovertTransceiver trans){
	   IGuardService igs = GuardServiceHelper.getIGSInstance();
	   try{
		   igs.startTimer(trans);
	   } catch(RemoteException e){
		   Log.d(TAG, "Caught Remote Exception");
		   e.printStackTrace();
	   }
   }
   
   
   public static int remoteExcProtectedGetTaint(CovertTransceiver trans){
	   IGuardService igs = GuardServiceHelper.getIGSInstance();
	   try{
		   return igs.getTaint(trans);
	   } catch(RemoteException e){
		   Log.d(TAG, "Caught Remote Exception");
		   e.printStackTrace();
	   }
	   return -1;
   }
   
   public static void remoteExcProtectedCombineTaint(int taint, CovertTransceiver trans){
	   IGuardService igs = GuardServiceHelper.getIGSInstance();
	   try{
		   igs.combineTaint(taint, trans);
	   } catch (RemoteException e){
		   Log.d(TAG, "Caught Remote Exception");
		   e.printStackTrace();
	   }
   }
    
    // Used to print the action that happened easily
    public static String getTypeString(int type){
    	switch(type){
    	case ADD_ACTIVE_RX:
    		return "Add Receiver";
    	case REMOVE_ACTIVE_RX:
    		return "Remove Receiver";
    	case ADD_ACTIVE_TX:
    		return "Add Transmitter";
    	case REMOVE_ACTIVE_TX:
    		return "Remove Transmitter";
		default:
    		return "Invalid Type: " + type;	
    	}
    }
    
    // this is not yet working correctly
    // Basically, the package name is not always at stackTraceArr[1]
    public static String getPkgHack(){
    	Throwable th = new Throwable();
    	StackTraceElement[] stackTraceArr = th.getStackTrace();
    	//for(int i = 0; i < stackTraceArr.length; i++){
    	//	Log.d(TAG, i + " : " + stackTraceArr[i]);
    	//}
    	return stackTraceArr[1].toString();
    }
 
    
    public static int getSenderIDFromChan(int chan){
    	switch(chan){
    	case CHAN_ULTRASOUND:
    		return DEV_SPKR;
    	case CHAN_VIB_ACCEL:
			return DEV_VIB;
    	case CHAN_SPKR_ACCEL:
    		return DEV_SPKR;
    	case CHAN_FLASH_CAM:
    		return DEV_FLASH;
    	case CHAN_USER_GYRO:
    		return DEV_USER;
    	default:
    		throw new IllegalArgumentException("Invalid Channel Integer: " + chan);
    	}
    	
    }
}
