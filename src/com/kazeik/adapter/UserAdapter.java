package com.kazeik.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kazeik.data.FriendInfo;
import com.kazeik.im.R;

public class UserAdapter extends BaseAdapter {
	Context ct = null;
	ArrayList<FriendInfo> allName;
	private LayoutInflater mViewInflater;

	public UserAdapter(Context context, ArrayList<FriendInfo> all) {
		this.ct = context;
		this.allName = all;

		mViewInflater = (LayoutInflater) ct
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return allName.size();
	}

	@Override
	public Object getItem(int arg0) {
		return allName.get(arg0);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SubView subview = null;
		if (null == convertView) {
			convertView = mViewInflater.inflate(R.layout.listitem, null);
			subview = new SubView();
			subview.tv_userName = (TextView) convertView
					.findViewById(R.id.userName);
			subview.tv_state = (TextView) convertView.findViewById(R.id.state);
			convertView.setTag(subview);
		} else {
			subview = (SubView) convertView.getTag();
		}

		subview.tv_userName.setText(allName.get(position).getNickname());
		String state = allName.get(position).getUserState();
		if (!TextUtils.isEmpty(state) && state.equals("available ")) {
			subview.tv_state.setText("ÔÚÏß");
		} else {
			subview.tv_state.setVisibility(View.GONE);
		}

		return convertView;
	}

	class SubView {
		TextView tv_userName;
		TextView tv_state;
	}
}
