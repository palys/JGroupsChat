package pl.edu.agh.dsrg.sr.chat;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import com.google.protobuf.InvalidProtocolBufferException;

import pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.ChatMessage;

public class Receiver extends ReceiverAdapter {
	
	private String name = "";
	
	public Receiver(String name) {
		this.name = name;
	}

	@Override
	public void receive(Message msg) {
		ChatMessage message = (ChatMessage) msg.getObject();
		System.out.println(name + ": " + message.getMessage());
	}
}
