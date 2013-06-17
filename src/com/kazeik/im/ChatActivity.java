/**
 * 2013-6-16 kazeik
 */
package com.kazeik.im;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.kazeik.adapter.ChatAdapter;
import com.kazeik.data.ChatMsg;
import com.kazeik.data.FriendInfo;
import com.kazeik.net.XmppUtils;
import com.kazeik.util.UserConfig;

/**
 * @author kazeik
 * 
 */
public class ChatActivity extends Activity implements OnClickListener {
	ListView lv_user = null;

	FriendInfo f_info = null;

	LinearLayout ll_button = null;

	Button btn_send = null;

	EditText et_input = null;
	private ChatAdapter adapter;
	public static ChatHandler mHandler = null;

	public class ChatHandler extends Handler {
		@Override
		public void handleMessage(android.os.Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case UserConfig.SEND_MSG:

				break;
			case UserConfig.RECEIVER_MSG:
				adapter.addCount((ChatMsg) msg.obj);
				lv_user.setSelection(adapter.getCount() - 1);// 设置移动到最后一行
				break;
			case UserConfig.NOTIF_MSG:
				lv_user.setSelection(adapter.getCount() - 1);// 设置移动到最后一行
				adapter.notifyDataSetChanged();
				break;
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chatmain);

		lv_user = (ListView) findViewById(R.id.lv_userlist);

		adapter = new ChatAdapter(this);
		lv_user.setAdapter(adapter);
		// lv_user.setSelection(chatMsgList.size() - 1);

		btn_send = (Button) findViewById(R.id.btn_send);
		btn_send.setOnClickListener(this);

		et_input = (EditText) findViewById(R.id.et_input);
		et_input.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (TextUtils.isEmpty(s.toString())) {
					btn_send.setEnabled(false);
				} else {
					btn_send.setEnabled(true);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				if (TextUtils.isEmpty(s.toString())) {
					btn_send.setEnabled(false);
				} else {
					btn_send.setEnabled(true);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (TextUtils.isEmpty(s.toString())) {
					btn_send.setEnabled(false);
				} else {
					btn_send.setEnabled(true);
				}
			}
		});

		Intent tempInten = getIntent();
		if (null != tempInten) {
			f_info = (FriendInfo) getIntent().getSerializableExtra("info");
		}
		mHandler = new ChatHandler();
		if (null == f_info) {
			f_info = new FriendInfo();
			ChatMsg tempMsg = (ChatMsg) getIntent().getSerializableExtra(
					"chatmsg");
			if (null == tempMsg) {
				Toast.makeText(this, "获取好友信息失败", Toast.LENGTH_SHORT).show();
				finish();
			} else {
				f_info.setUsername(tempMsg.getUsername());
				adapter.addCount(tempMsg);
				mHandler.sendEmptyMessage(UserConfig.NOTIF_MSG);
			}
		}
		setTitle("和 " + f_info.getNickname() + "聊天");

	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		mHandler = null;
		this.finish();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_send:
			String sendmsg = et_input.getText().toString().trim();
			if (!XmppUtils.getInstance().isLogin()) {
				Toast.makeText(this, "网络已断开..", Toast.LENGTH_SHORT).show();
				return;
			}
			try {
				Message message = new Message(f_info.getJid(), Type.chat);
				message.setBody(sendmsg);
				XmppUtils.getInstance().getConnection().sendPacket(message);
			} catch (Exception e1) {
				e1.printStackTrace();
				Toast.makeText(ChatActivity.this, "没有连网", Toast.LENGTH_SHORT)
						.show();
			}
			et_input.setText("");
			ChatMsg msg_me = new ChatMsg();
			msg_me.setMsg(sendmsg);
			msg_me.setType(UserConfig.SEND_MSG);
			msg_me.setUsername(StringUtils.parseName(XmppUtils.getInstance()
					.getUser()));
			android.os.Message tempmsg = new android.os.Message();
			tempmsg.obj = msg_me;
			tempmsg.what = UserConfig.RECEIVER_MSG;
			mHandler.sendMessage(tempmsg);
			break;
		}
	}

}
