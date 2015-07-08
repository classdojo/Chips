package com.strv.chipssample;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/**
 * Created by Roman on 08/07/2015.
 */
public class Preferences {

	private static final String USE_LIST = "use_list";
	private SharedPreferences mSharedPreferences;
	private Context mContext;

	public Preferences(Context context)
	{
		if (context == null) context = SampleApplication.getInstance();
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		mContext = context;
	}

	public void setUseList(boolean useList){
		mSharedPreferences.edit().putBoolean(USE_LIST, useList).commit();
	}

	public boolean useList(){
		return mSharedPreferences.getBoolean(USE_LIST, true);
	}
}
