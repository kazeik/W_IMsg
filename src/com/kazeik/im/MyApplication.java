/**
 * 
 */
package com.kazeik.im;

import android.app.Application;

/**
 * @author SKS
 * 
 */
public class MyApplication extends Application {
	public static MyApplication application = null;

	@Override
	public void onCreate() {
		super.onCreate();
		application = this;
	}

}
