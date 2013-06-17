package com.kazeik.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import com.kazeik.data.ChatMsg;
import com.kazeik.data.FriendInfo;
import com.kazeik.data.GroupInfo;
import com.kazeik.im.ChatActivity;
import com.kazeik.im.W_IMsgActivity;
import com.kazeik.util.UserConfig;

public class XmppUtils implements PacketListener {

	public static int SERVER_PORT = 5222;// 服务端口
	public static final String RESOURCE = "kazeik";
	public static final int LOGIN_ERROR_REPEAT = 409;// 重复登录
	public static final int LOGIN_ERROR_NET = 502;// 服务不可用
	public static final int LOGIN_ERROR_PWD = 401;// 密码错误 或其他
	public static final int LOGIN_ERROR = 404;// 未知错误

	private final int ERROR_CONN = 4001;
	private final int ERROR_REGISTER = 4002;
	private final int ERROR_REGISTER_REPEATUSER = 4003;
	private final int SUCCESS = 201;
	private static XMPPConnection conn;
	private static XmppUtils instance;

	public static boolean DEBUG = true;
	Roster roster = null;
	public List<GroupInfo> groupList;
	FriendInfo friendInfo;
	GroupInfo groupInfo;
	private List<FriendInfo> friendList;

	// private static FriendInfo currFriendInfo;

	public static XmppUtils getInstance() {
		if (instance == null) {
			instance = new XmppUtils();
		}
		return instance;
	}

	/**
	 * 创建XMPP连接实例
	 * 
	 * @return
	 * @throws org.jivesoftware.smack.XMPPException
	 */
	public void createConnection() throws XMPPException {
		if (null == conn || !conn.isAuthenticated()) {
			XMPPConnection.DEBUG_ENABLED = UserConfig.DEBUG;// 开启DEBUG模式
			// 配置连接
			ConnectionConfiguration config = new ConnectionConfiguration(
					UserConfig.SERVICENAME, SERVER_PORT, UserConfig.SERVICENAME);
			config.setReconnectionAllowed(true);
			config.setSendPresence(true);
			config.setSASLAuthenticationEnabled(true);

			conn = new XMPPConnection(config);
			conn.connect();// 连接到服务器
			PacketFilter presenceFilter = new PacketTypeFilter(Packet.class);
			conn.addPacketListener(this, presenceFilter);
			configureConnection(ProviderManager.getInstance());
		}
	}

	public XMPPConnection getConnection() throws XMPPException {
		return conn;
	}

	public String getUser() {
		return conn.getUser();
	}

	/**
	 * 是否已经登录
	 * 
	 * @return
	 */
	public boolean isLogin() {
		if (conn == null)
			return false;// 连接未生成
		else if (!conn.isConnected())
			return false;// 连接未生效
		else if (!conn.isAuthenticated())
			return false;// 连接未认证
		return true;
	}

	public void sendOnLine() {
		if (conn == null || !conn.isConnected() || !conn.isAuthenticated())
			throw new RuntimeException("连接有问题");

		Presence presence = new Presence(Type.available);
		conn.sendPacket(presence);
	}

	public List<FriendInfo> loadFriend() {
		try {
			final XMPPConnection conn = XmppUtils.getInstance().getConnection();
			roster = conn.getRoster();
			Collection<RosterGroup> groups = roster.getGroups();

			groupList = new ArrayList<GroupInfo>();

			for (RosterGroup group : groups) {
				groupInfo = new GroupInfo();
				friendList = new ArrayList<FriendInfo>();
				groupInfo.setGroupName(group.getName());
				Collection<RosterEntry> entries = group.getEntries();
				for (RosterEntry entry : entries) {
					if ("both".equals(entry.getType().name())) {// 只添加双边好友
						friendInfo = new FriendInfo();
						friendInfo.setUsername(StringUtils.parseName(entry
								.getUser()));
						friendInfo.setNickname(entry.getName());
						String state = roster.getPresence(entry.getName())
								.getStatus();
						friendInfo.setUserState(state);
						friendList.add(friendInfo);
						friendInfo = null;
					}
				}
				groupInfo.setFriendInfoList(friendList);
				groupList.add(groupInfo);
				groupInfo = null;
			}
			if (groupList.isEmpty()) {
				groupInfo = new GroupInfo();
				groupInfo.setGroupName("Friends");
				groupInfo.setFriendInfoList(new ArrayList<FriendInfo>());
				groupList.add(groupInfo);
				groupInfo = null;
			}
			addRosterListener();
			return friendList;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void addRosterListener() {
		roster.addRosterListener(new RosterListener() {
			@Override
			public void presenceChanged(Presence presence) {
				UserConfig.Log("presenceChanged", presence.toXML() + " | "
						+ presence.getMode().toString() + "   "
						+ presence.getType().toString());
				String[] value = new String[2];
				value[0] = StringUtils.parseName(presence.getFrom());
				value[1] = presence.getMode().toString();
				android.os.Message msg = new android.os.Message();
				msg.obj = value;
				msg.what = 3;
				W_IMsgActivity.xHandler.sendMessage(msg);
			}

			@Override
			public void entriesUpdated(Collection<String> arg0) {
				for (String msg : arg0) {
					UserConfig.Log("entriesUpdated", msg);
				}
			}

			@Override
			public void entriesDeleted(Collection<String> arg0) {
				for (String msg : arg0) {
					UserConfig.Log("entriesDeleted", msg);
				}
			}

			@Override
			public void entriesAdded(Collection<String> arg0) {
				for (String msg : arg0) {
					UserConfig.Log("entriesAdded", msg);
				}
			}
		});
	}

	/**
	 * 关闭XmppConnection连接
	 * 
	 */
	public void closeConn() {
		if (null != conn && conn.isConnected()) {
			Presence pres = new Presence(Presence.Type.unavailable);
			conn.disconnect(pres);
			conn = null;
		}
	}

	public int registerUser(String user, String pass) {
		try {
			Registration registration = new Registration();
			registration.setType(IQ.Type.SET);
			registration.setTo(UserConfig.SERVICENAME);
			registration.setUsername(user.trim());
			registration.setPassword(pass.trim());
			PacketFilter filter = new AndFilter(new PacketIDFilter(
					registration.getPacketID()), new PacketTypeFilter(IQ.class));
			PacketCollector collector = XmppUtils.getInstance().getConnection()
					.createPacketCollector(filter);
			XmppUtils.getInstance().getConnection().sendPacket(registration);
			Packet response = collector.nextResult(SmackConfiguration
					.getPacketReplyTimeout());
			collector.cancel();
			if (response == null) {
				throw new XMPPException(
						"No response from server on status set.");
			}
			if (response.getError() != null) {
				throw new XMPPException(response.getError());
			}
			return SUCCESS;
		} catch (XMPPException e) {
			if (e.getXMPPError() != null && e.getXMPPError().getCode() == 409) {
				return ERROR_REGISTER_REPEATUSER;// 帐号已存在
			} else {
				return ERROR_REGISTER; // 注册失败
			}
		}
	}

	/**
	 * xmpp配置
	 * 
	 * @param pm
	 */
	private void configureConnection(ProviderManager pm) {
		// Private Data Storage
		pm.addIQProvider("query", "jabber:iq:private",
				new PrivateDataManager.PrivateDataIQProvider());

		// Time
		try {
			pm.addIQProvider("query", "jabber:iq:time",
					Class.forName("org.jivesoftware.smackx.packet.Time"));
		} catch (Exception e) {
			e.printStackTrace();
			// Logs.v(TAG,
			// "Can't load class for org.jivesoftware.smackx.packet.Time");
		}

		// Roster Exchange
		pm.addExtensionProvider("x", "jabber:x:roster",
				new RosterExchangeProvider());

		// Message Events
		pm.addExtensionProvider("x", "jabber:x:event",
				new MessageEventProvider());

		// Chat State
		pm.addExtensionProvider("active",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("composing",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("paused",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("inactive",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		pm.addExtensionProvider("gone",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		// XHTML
		pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",
				new XHTMLExtensionProvider());

		// Group Chat Invitations
		pm.addExtensionProvider("x", "jabber:x:conference",
				new GroupChatInvitation.Provider());

		// Service Discovery # Items //解析房间列表
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",
				new DiscoverItemsProvider());

		// Service Discovery # Info //某一个房间的信息
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());

		// Data Forms
		pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());

		// MUC User
		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
				new MUCUserProvider());

		// MUC Admin
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
				new MUCAdminProvider());

		// MUC Owner
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
				new MUCOwnerProvider());

		// Delayed Delivery
		pm.addExtensionProvider("x", "jabber:x:delay",
				new DelayInformationProvider());

		// Version
		try {
			pm.addIQProvider("query", "jabber:iq:version",
					Class.forName("org.jivesoftware.smackx.packet.Version"));
		} catch (ClassNotFoundException e) {
			// Not sure what's happening here.
		}
		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

		// Offline Message Requests
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline",
				new OfflineMessageRequest.Provider());

		// Offline Message Indicator
		pm.addExtensionProvider("offline",
				"http://jabber.org/protocol/offline",
				new OfflineMessageInfo.Provider());

		// Last Activity
		pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());

		// User Search
		pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());

		// SharedGroupsInfo
		pm.addIQProvider("sharedgroup",
				"http://www.jivesoftware.org/protocol/sharedgroup",
				new SharedGroupsInfo.Provider());

		// JEP-33: Extended Stanza Addressing
		pm.addExtensionProvider("addresses",
				"http://jabber.org/protocol/address",
				new MultipleAddressesProvider());
		// FileTransfer
		pm.addIQProvider("si", "http://jabber.org/protocol/si",
				new StreamInitiationProvider());

		pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams",
				new BytestreamsProvider());

		// pm.addIQProvider("open", "http://jabber.org/protocol/ibb",
		// new IBBProviders.Open());
		//
		// pm.addIQProvider("close", "http://jabber.org/protocol/ibb",s
		// new IBBProviders.Close());
		//
		// pm.addExtensionProvider("data", "http://jabber.org/protocol/ibb",
		// new IBBProviders.Data());

		// Privacy
		pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());

		pm.addIQProvider("command", "http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider());
		pm.addExtensionProvider("malformed-action",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.MalformedActionError());
		pm.addExtensionProvider("bad-locale",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadLocaleError());
		pm.addExtensionProvider("bad-payload",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadPayloadError());
		pm.addExtensionProvider("bad-sessionid",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadSessionIDError());
		pm.addExtensionProvider("session-expired",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.SessionExpiredError());
	}

	@Override
	public void processPacket(Packet packet) {
		if (packet instanceof org.jivesoftware.smack.packet.Message) {
			org.jivesoftware.smack.packet.Message msg = (org.jivesoftware.smack.packet.Message) packet;
			if (msg.getType() == org.jivesoftware.smack.packet.Message.Type.chat) {
				UserConfig.Log("tag", packet.toXML());
				android.os.Message msgt = new android.os.Message();
				ChatMsg msg_other = new ChatMsg();
				msg_other.setMsg(msg.getBody());
				msg_other.setType(UserConfig.RECEIVER_MSG);
				msg_other.setUsername(StringUtils.parseName(msg.getFrom()));
				msgt.obj = msg_other;
				msgt.what = UserConfig.RECEIVER_MSG;
				if (null != ChatActivity.mHandler) {
					ChatActivity.mHandler.sendMessage(msgt);
				} else {
					W_IMsgActivity.xHandler.sendMessage(msgt);
				}
			}
		}
	}
}
