/*
* aidl file : frameworks/base/core/java/android/os/IGuardService.aidl
* This file contains definitions of functions which are exposed by service
*/

package android.os;

import net.ednovak.Transceiver.CovertTransceiver;


interface IGuardService {
	
	
	/**
	*@hide
	*/
	void addActiveRx(in CovertTransceiver trans);
	
	/**
	*@hide
	*/
	void removeActiveRx(in CovertTransceiver trans);
	
	/**
	*@hide
	*/
	void addActiveTx(in CovertTransceiver trans);
	
	/**
	*@hide
	*/
	void removeActiveTx(in CovertTransceiver trans);
	
	/**
	*@hide
	*/
	String getActiveReceivers();
	
	/**
	*@hide
	*/
	String getActiveSenders();
	
	/**
	*@hide
	*/
	void clearList(int type);
	
	/**
	*@hide
	*/
	boolean checkChannels(in int[] chans);
	
	/**
	*@hide
	*/    
	void setOrBumpTimer(long time, in CovertTransceiver dev);
	
	/**
	*@hide
	*/
	void startTimer(in CovertTransceiver dev);
	
	/**
	*@hide
	*/
	void combineTaint(int tag, in CovertTransceiver trans);
	
	/**
	*@hide
	*/
	void setTaint(int tag, in CovertTransceiver trans);
	
	/**
	*@hide
	*/
	int getTaint(in CovertTransceiver trans);

}
