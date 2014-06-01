package pl.edu.agh.dsrg.sr.chat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.protocols.BARRIER;
import org.jgroups.protocols.FD_ALL;
import org.jgroups.protocols.FD_SOCK;
import org.jgroups.protocols.FRAG2;
import org.jgroups.protocols.MERGE2;
import org.jgroups.protocols.MFC;
import org.jgroups.protocols.PING;
import org.jgroups.protocols.UDP;
import org.jgroups.protocols.UFC;
import org.jgroups.protocols.UNICAST2;
import org.jgroups.protocols.VERIFY_SUSPECT;
import org.jgroups.protocols.pbcast.FLUSH;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.protocols.pbcast.STATE_TRANSFER;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;

import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.ActionType;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatMessage;

public class Chat {
	
	private static ProtocolStack defaultStack() {
		ProtocolStack stack = new ProtocolStack();
		stack.addProtocol(new UDP())
		.addProtocol(new PING())
		.addProtocol(new MERGE2())
		.addProtocol(new FD_SOCK())
		.addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
		.addProtocol(new VERIFY_SUSPECT())
		.addProtocol(new BARRIER())
		.addProtocol(new NAKACK())
		.addProtocol(new UNICAST2())
		.addProtocol(new STABLE())
		.addProtocol(new GMS())
		.addProtocol(new UFC())
		.addProtocol(new MFC())
		.addProtocol(new FRAG2())
		.addProtocol(new STATE_TRANSFER())
		.addProtocol(new FLUSH());
		return stack;
	}
	
	private static ProtocolStack defaultStack(String multicastAddress) throws UnknownHostException {
		ProtocolStack stack = new ProtocolStack();
		stack.addProtocol(new UDP().setValue("mcast_group_addr",InetAddress.getByName(multicastAddress)))
		.addProtocol(new PING())
		.addProtocol(new MERGE2())
		.addProtocol(new FD_SOCK())
		.addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
		.addProtocol(new VERIFY_SUSPECT())
		.addProtocol(new BARRIER())
		.addProtocol(new NAKACK())
		.addProtocol(new UNICAST2())
		.addProtocol(new STABLE())
		.addProtocol(new GMS())
		.addProtocol(new UFC())
		.addProtocol(new MFC())
		.addProtocol(new FRAG2())
		.addProtocol(new STATE_TRANSFER())
		.addProtocol(new FLUSH());
		return stack;
	}
	
	static JChannel newChannelM(String multicastAddress) throws Exception {
		JChannel channel = new JChannel(false);
		ProtocolStack stack = defaultStack(multicastAddress);
		
		channel.setProtocolStack(stack);
		stack.init();
		
		channel.connect(multicastAddress);
		return channel;
	}
	
	static  JChannel newChannel(String clusterName) throws Exception {
		JChannel channel = new JChannel(false);
		ProtocolStack stack = defaultStack();
		
		channel.setProtocolStack(stack);
		stack.init();
		
		channel.connect(clusterName);
		return channel;
	}
	
	private JChannel chatManagmentChannel = null;
	
	private final static String chatManagementClusterName = "ChatManagement768264";
	
	private String nick = null;
	
	private BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
	
	private ChatManagementReceiverAdapter manager = null;
	
	private Map<String, JChannel> channels = null;
	
	public Chat() {
		try {
			chatManagmentChannel = newChannel(chatManagementClusterName);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		manager = new ChatManagementReceiverAdapter(chatManagmentChannel);
		channels = new HashMap<String, JChannel>();
	}
	
	public static void main(String[] args) {
		Chat chat = new Chat();
		
		
		if (args.length != 1) {
			System.out.println("Brak nicka");
			System.exit(-1);
		} else {
			chat.nick = args[0];
		}
		
		chat.mainLoop();
	}
	
	private void mainLoop() {
		try {
			while (true) {
				try {
					printPrompt();

					String line = in.readLine();

					if (line.startsWith("quit") || line.startsWith("exit")) {
						exit();
					}

					if (line.equals("n")) {
						newChannelIn();
					} else if (line.equals("j")) {
						joinChannelIn();
					} else if (line.equals("l")) {
						listChannels();
					} else if (line.equals("s")) {
						sendMessageIn();
					} else {
						invalidInput();
					}

				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		} finally {
			exit();
		}
	}
	
	private void notifyOthersAboutJoining(String channel, String nick) throws Exception {
		ChatAction action = ChatAction.newBuilder()
									  .setAction(ActionType.JOIN)
									  .setChannel(channel)
									  .setNickname(nick)
									  .build();
		
		chatManagmentChannel.send(new Message(null, null, action));
	}
	
	private void newChannelIn() {
		try {
			System.out.println("Podaj adres kanalu:");
			String multicastAddress = in.readLine();
			
			JChannel channel = newChannelM(multicastAddress);
			channels.put(multicastAddress, channel);
			
			notifyOthersAboutJoining(multicastAddress, nick);
			
			channel.setReceiver(new Receiver(multicastAddress));
			
		} catch(Exception e) {
			System.out.println("Blad podczas tworzenia nowego kanalu");
			newChannelIn();
		}
	}
	
	private void joinChannelIn() {
		try {
			System.out.println("Podaj adres kanalu:");
			String multicastAddress = in.readLine();
			
			JChannel channel = newChannelM(multicastAddress);
			channels.put(multicastAddress, channel);
			
			notifyOthersAboutJoining(multicastAddress, nick);
			
			channel.setReceiver(new Receiver(multicastAddress));
			
		} catch(Exception e) {
			System.out.println("Blad podczas dolaczania do kanalu");
			newChannelIn();
		}
	}
	
	private void listChannels() {
		System.out.println(manager.listChannels());
	}
	
	private void sendMessageIn() {
		try {
			System.out.println("Podaj nazwe kanalu:");
			String clusterName = in.readLine();
			
			if (channels.containsKey(clusterName)) {
				System.out.println("Podaj wiadomosc");
				String line = in.readLine();
				ChatMessage chatMessage = ChatMessage.newBuilder()
													 .setMessage(nick + ": " + line)
													 .build();
				
				channels.get(clusterName).send(new Message(null, null, chatMessage));
			} else {
				System.out.println("Nieznany kanal");
			}
		} catch(Exception e) {
			System.out.println("Blad podczas wysylania wiadomosci");
			sendMessageIn();
		}
	}
	
	private void invalidInput() {
		System.out.println("Bledna komenda");
	}
	
	private void printPrompt() {
		System.out.println("[n] - nowy kanal\n"
						 + "[j] - dolacz do istniejacego kanalu\n"
						 + "[l] - wyswietl liste kanalow i osob zapisanych do nich\n"
						 + "[s] - wyslij wiadomosc\n"
						 + "[exit] lub [quit] - zakoncz");
	}
	
	private void exit() {
		for (String s : channels.keySet()) {
			ChatAction a = ChatAction.newBuilder()
									 .setAction(ActionType.LEAVE)
									 .setChannel(s)
									 .setNickname(nick)
									 .build();
			try {
				chatManagmentChannel.send(new Message(null, null, a));
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			channels.get(s).close();
		}
		manager.close();
		System.exit(0);
	}
}








































