/**
 * 
 */
package com.kazeik.im;

import android.app.Application;

/**
 * 全局application类
 * @author kazeik
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
