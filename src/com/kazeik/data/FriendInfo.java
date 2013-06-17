package com.kazeik.data;

import java.io.Serializable;

import android.text.TextUtils;

import com.kazeik.util.UserConfig;

public class FriendInfo implements Serializable {
	private String username;
	private String nickname;

	private String userState;

	public String getUserState() {
		return userState;
	}

	public void setUserState(String userState) {
		this.userState = userState;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getNickname() {
		if (TextUtils.isEmpty(nickname))
			return username;
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getJid() {
		if (username == null)
			return null;
		return username + "@" + UserConfig.SERVICE_NAME;
	}
}
