package project.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;

import project.smartMessage.SmartMessage;

public class AckAgentMessage implements MessageContents {
	private SmartMessage smartMessage;
	
	public AckAgentMessage(SmartMessage smartMessage){
		this.smartMessage = smartMessage;
	}
	
	public SmartMessage retrieveSmartMessage() {
		return this.smartMessage;
	}
}
