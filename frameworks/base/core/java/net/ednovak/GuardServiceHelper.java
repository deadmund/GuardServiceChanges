package net.ednovak;

import net.ednovak.Transceiver.CovertTransceiver;
import net.ednovak.Transceiver.CovertReceiver;
import android.os.IGuardService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class GuardServiceHelper {
	private static final String TAG = GuardServiceHelper.class.getName();

    public static final int ADD_ACTIVE_TX = 1;
    public static final int REMOVE_ACTIVE_TX = 2;
	
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
		Log.d("GuardServiceHelper", "taint: " + trans.taint);
		Log.d("GuardServiceHelper", "flag: " + trans.flag);
		Log.d("GuardServiceHelper", "delay: " + trans.delay);
    }

    public static int getIGSTaint(IGuardService igs, CovertReceiver rx){
        int res = 0;
        try{
            res = igs.getTaint(rx);
        }
        catch(Exception e){
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

    	debugHelper(igs, trans);
    	
    	try{	
	    	switch(type){
                /*
	    	case ADD_ACTIVE_RX:
	    		igs.addActiveRx(trans);
	    		break;
	    	case REMOVE_ACTIVE_RX:
	    		igs.removeActiveRx(trans);
	    		break;
	    		*/
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
    
    // Used to print the action that happened easily
    public static String getTypeString(int type){
    	switch(type){
    	case ADD_ACTIVE_TX:
    		return "Add Transmitter";
    	case REMOVE_ACTIVE_TX:
    		return "Remove Transmitter";
		default:
    		return "Invalid Type: " + type;	
    	}
    }
}
