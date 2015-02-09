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
	void addActiveTx(in CovertTransceiver trans);
	
	/**
	*@hide
	*/
	void removeActiveTx(in CovertTransceiver trans);
	
	/**
	*@hide
	*/
	String getActiveSenders();
	
	/**
	*@hide
	*/
	int getTaint(in CovertTransceiver trans);

}
