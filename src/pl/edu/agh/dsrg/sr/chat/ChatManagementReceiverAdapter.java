package pl.edu.agh.dsrg.sr.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatAction.ActionType;
import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatState;

import com.google.protobuf.InvalidProtocolBufferException;

public class ChatManagementReceiverAdapter extends ReceiverAdapter {

	private JChannel channel;
	
	private Map<String, LinkedList<String>> state;
	
	public ChatManagementReceiverAdapter(JChannel channel) {
		this.channel = channel;
		this.channel.setReceiver(this);
		state = new HashMap<String, LinkedList<String>>();
		try {
			this.channel.getState(null, 10000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }

	@Override
    public void receive(Message msg) {
				
		ChatAction action = null;
		action = (ChatAction) msg.getObject();

		
		ActionType type = action.getAction();
		String msgChannel = action.getChannel();
		String nickName = action.getNickname();
		
		synchronized (state) {

			if (type == ActionType.JOIN) {
				add(msgChannel, nickName);
			} else if (type == ActionType.LEAVE) {
				state.get(msgChannel).remove(nickName);
			}

		}
    }
	
	private void add(String channel, String nickname) {
		if (!state.containsKey(channel)) {
			state.put(channel, new LinkedList<String>());
		}
		state.get(channel).add(nickname);
	}
	
	private ChatState mapToState(Map<String, LinkedList<String>> state) {

		ChatState.Builder builder = ChatState.newBuilder();

		for (Map.Entry<String, LinkedList<String>> e : state.entrySet()) {
			for (String s : e.getValue()) {
				ChatAction action = ChatAction.newBuilder()
						.setAction(ActionType.JOIN)
						.setChannel(e.getKey())
						.setNickname(s)
						.build();
				builder.addState(action);
			}
		}

		ChatState chatState = builder.build();
		return chatState;

	}

	@Override
    public void getState(OutputStream output) throws Exception {
        synchronized(state) {
            Util.objectToStream(mapToState(state), new DataOutputStream(output));
        }
        
    }

	@Override
    public void setState(InputStream input) throws Exception {
        ChatState chatState = (ChatState) Util.objectFromStream(new DataInputStream(input));
        synchronized(state) {
            state.clear();
            for (ChatAction a : chatState.getStateList()) {
            	add(a.getChannel(), a.getNickname());
            }
        }
    }
	
	void close() {
		this.channel.close();
	}
	
	String listChannels() {
		StringBuilder builder = new StringBuilder();
		
		synchronized(state) {
			for (Map.Entry<String, LinkedList<String>> e : state.entrySet()) {
				builder.append(e.getKey())
				.append(":\n");

				for (String s : e.getValue()) {
					builder.append("\t")
					.append(s)
					.append("\n");
				}
			}
		}
		
		return builder.toString();
	}

}
