package com.kazeik.util;

import org.jivesoftware.smack.XMPPException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;

import com.kazeik.net.XmppUtils;

public class UserConfig {
	public static final int PORT = 5222;
	public static final String SERVICENAME = "117.27.138.79";
	public static String SERVICE_NAME = "";

	private static ProgressDialog mProg = null;

	public static final int CONN_OK = 1000;
	public static final int CONN_NULL = 1004;
	public static final int CONN_CONNECD = 1001;
	public static final int CONN_AUTH = 1002;

	public static final int SEND_MSG = 1;
	public static final int RECEIVER_MSG = 2;
	public static final int NOTIF_MSG = 3;

	public static boolean DEBUG = true;

	/**
	 * ��ʾ������������������޷��÷��ؼ���ȡ��
	 * 
	 * @param ct
	 */
	public static void showDialogs(Context ct) {
		if (null != mProg) {
			hideDialog();
		}
		mProg = ProgressDialog.show(ct, "", "������...");
	}

	public static void hideDialog() {
		if (null != mProg) {
			mProg.dismiss();
			mProg = null;
		}
	}

	public static void Log(String title, String msg) {
		if (DEBUG) {
			android.util.Log.v(title, msg);
		}
	}

	/**
	 * �õ��û�����״̬
	 * 
	 * @param userName
	 *            �û�����JID
	 * @return
	 * @throws XMPPException
	 */
	public static String getUserState(String userName) throws XMPPException {
		return XmppUtils.getInstance().getConnection().getRoster()
				.getPresence(userName).getStatus();
	}

	public static void saveMsg(Context ct, String title, String value) {
		SharedPreferences userInfo = ct.getSharedPreferences("user_info", 0);
		userInfo.edit().putString(title, value).commit();
	}

	public static String getMsg(Context ct, String title) {
		SharedPreferences userInfo = ct.getSharedPreferences("user_info", 0);
		return userInfo.getString(title, "");
	}
}
