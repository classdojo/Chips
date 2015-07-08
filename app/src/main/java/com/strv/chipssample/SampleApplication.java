package com.strv.chipssample;

import android.app.Application;


/**
 * Created by Roman on 08/07/2015.
 */
public class SampleApplication extends Application {

	private static SampleApplication sInstance;


	public static SampleApplication getInstance() {
		return sInstance;
	}


	@Override
	public void onCreate() {
		super.onCreate();
		sInstance = this;
	}
}
