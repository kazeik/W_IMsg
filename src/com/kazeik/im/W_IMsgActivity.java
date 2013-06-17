package com.kazeik.im;

import java.util.ArrayList;
import java.util.Iterator;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.kazeik.adapter.UserAdapter;
import com.kazeik.data.ChatMsg;
import com.kazeik.data.FriendInfo;
import com.kazeik.net.XmppUtils;
import com.kazeik.util.UserConfig;

public class W_IMsgActivity extends Activity implements OnClickListener,
		OnItemClickListener {
	/** Called when the activity is first created. */
	Button btn_login = null;
	EditText et_userName = null;
	public static XmppHandler xHandler = null;
	ListView lv_user = null;
	UserAdapter adapter = null;
	ArrayList<FriendInfo> alltemp = new ArrayList<FriendInfo>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if (!UserConfig.DEBUG) {
			String value = UserConfig.getMsg(this, "index");
			if (TextUtils.isEmpty(value)) {
				value = "0";
				UserConfig.saveMsg(this, "index", value);
			} else {
				int index = Integer.parseInt(value);
				if (index < 5) {
					index++;
					UserConfig.saveMsg(this, "index", String.valueOf(index));
				} else {
					Toast.makeText(this, "使用超过次数限制", Toast.LENGTH_SHORT).show();
					finish();
				}
			}
		}
		xHandler = new XmppHandler();

		btn_login = (Button) findViewById(R.id.btn_login);
		btn_login.setOnClickListener(this);

		lv_user = (ListView) findViewById(R.id.lv_userlist);
		lv_user.setOnItemClickListener(this);

		adapter = new UserAdapter(W_IMsgActivity.this, alltemp);
		lv_user.setAdapter(adapter);

		et_userName = (EditText) findViewById(R.id.et_name);
		if (XmppUtils.DEBUG) {
			et_userName.setText("newuser8");
		}
	}

	public class XmppHandler extends Handler {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			UserConfig.hideDialog();
			switch (msg.what) {
			case 1:
				alltemp = (ArrayList<FriendInfo>) msg.obj;
				if (null != alltemp) {
					adapter = new UserAdapter(W_IMsgActivity.this, alltemp);
					lv_user.setAdapter(adapter);
				} else {
					Toast.makeText(W_IMsgActivity.this, "好友列表为空",
							Toast.LENGTH_SHORT).show();
				}
				break;
			case 0:
				String value = (String) msg.obj;
				Toast.makeText(W_IMsgActivity.this, value, Toast.LENGTH_SHORT)
						.show();
				break;
			case 999:
				btn_login.setEnabled(false);
				Toast.makeText(W_IMsgActivity.this, "登录成功", Toast.LENGTH_SHORT)
						.show();
				break;
			case 3:
				String[] vastate = (String[]) msg.obj;
				if (!alltemp.isEmpty()) {
					Iterator<FriendInfo> iter = alltemp.iterator();
					while (iter.hasNext()) {
						FriendInfo info = (FriendInfo) iter.next();
						if (vastate[0].equals(Presence.Mode.xa.toString())) {
							alltemp.remove(info);
						} else {
							info.setUserState(vastate[1]);
						}
					}
				} else {
					if (!vastate[0].equals(Presence.Mode.xa.toString())) {
						FriendInfo tempInfo = new FriendInfo();
						tempInfo.setUsername(vastate[0]);
						tempInfo.setUserState(vastate[1]);
						alltemp.add(tempInfo);
					}
				}
				adapter.notifyDataSetChanged();
				break;
			case UserConfig.RECEIVER_MSG:
				ChatMsg tem = (ChatMsg) msg.obj;
				if (null != tem) {
					showAlertMsg(tem);
				}
				break;
			case 99:
				W_IMsgActivity.this.finish();
				break;
			}
		}
	}

	private void showAlertMsg(ChatMsg msg) {
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification n = new Notification(R.drawable.ic_launcher,
				msg.getUsername() + ":" + msg.getMsg(),
				System.currentTimeMillis());
		n.flags = Notification.FLAG_AUTO_CANCEL;
		Intent i = new Intent(this, ChatActivity.class);
		i.putExtra("chatmsg", msg);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this,
				R.string.app_name, i, PendingIntent.FLAG_UPDATE_CURRENT);

		n.setLatestEventInfo(this, getString(R.string.app_name),
				msg.getUsername() + ":" + msg.getMsg(), contentIntent);
		nm.notify(R.string.app_name, n);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_login:
			final String value = et_userName.getText().toString();
			if (TextUtils.isEmpty(value)) {
				Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show();
				return;
			}
			if (XmppUtils.getInstance().isLogin()) {
				return;
			}
			UserConfig.showDialogs(this);
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						XmppUtils.getInstance().createConnection();
						XmppUtils.getInstance().getConnection()
								.login(value, "123456");
						XmppUtils.getInstance().sendOnLine();
						String us = XmppUtils.getInstance().getConnection()
								.getUser();
						UserConfig.SERVICE_NAME = StringUtils.parseServer(us);
						XmppUtils.getInstance().loadFriend();
						// Message msg = new Message();
						// msg.obj = temp;
						// msg.what = 1;
						// xHandler.sendMessage(msg);
						xHandler.sendEmptyMessage(999);
					} catch (XMPPException e) {
						e.printStackTrace();
						Message msg = new Message();
						msg.what = 0;
						msg.obj = e.getXMPPError().getCode();
						xHandler.sendMessage(msg);
					}
				}
			}).start();

			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		FriendInfo temp = alltemp.get(arg2);
		Intent intt = new Intent(this, ChatActivity.class);
		intt.putExtra("info", temp);
		this.startActivity(intt);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, 1, 1, "退出");
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case 1:
			new Thread(new Runnable() {

				@Override
				public void run() {
					XmppUtils.getInstance().closeConn();
					xHandler.sendEmptyMessage(99);
				}
			}).start();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}