/**
 * 2013-6-16 kazeik
 */
package com.kazeik.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kazeik.data.ChatMsg;
import com.kazeik.im.R;
import com.kazeik.util.UserConfig;

/**
 * @author kazeik
 * 
 */
public class ChatAdapter extends BaseAdapter {
	private ArrayList<ChatMsg> chatMsgList = null;
	private LayoutInflater inflater;

	public ChatAdapter(Context ct) {
		inflater = LayoutInflater.from(ct);
		this.chatMsgList = new ArrayList<ChatMsg>();
	}

	@Override
	public int getCount() {
		return chatMsgList.size();
	}

	public void addCount(ChatMsg msg) {
		chatMsgList.add(msg);
		notifyDataSetChanged();
	}

	@Override
	public Object getItem(int position) {
		return chatMsgList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemViewType(int position) {
		return chatMsgList.get(position).getType();
	}

	@Override
	public int getViewTypeCount() {
		return 3;// size要大于布局个数
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder = null;
		int type = getItemViewType(position);
		ChatMsg chatMsg = chatMsgList.get(position);
		if (convertView == null) {
			holder = new Holder();
			switch (type) {
			case UserConfig.SEND_MSG:
				convertView = inflater.inflate(R.layout.chat_me, null);
				holder.msg = (TextView) convertView.findViewById(R.id.msg_me);
				break;
			case UserConfig.RECEIVER_MSG:
				convertView = inflater.inflate(R.layout.chat_other, null);
				holder.msg = (TextView) convertView
						.findViewById(R.id.msg_other);
				break;
			}
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}

		holder.msg.setText(chatMsg.getMsg());
		return convertView;
	}

	class Holder {
		TextView msg;
		// ImageView icon;
	}

}
