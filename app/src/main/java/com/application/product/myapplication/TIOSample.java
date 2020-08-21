/* ------------------------------------------------------------------------------------------------
*
* Copyright (C) 2016 Telit Wireless Solutions GmbH, Germany
*
-------------------------------------------------------------------------------------------------*/

package com.application.product.myapplication;

import android.app.Application;

import com.telit.terminalio.TIOManager;

public class TIOSample extends Application {

	public static final String PERIPHERAL_ID_NAME = "eTracker";
	
	@Override
	public void onCreate() {
		TIOManager.initialize(this.getApplicationContext());
	}

	@Override
	public void onTerminate() {
		TIOManager.getInstance().done();
	}

}
