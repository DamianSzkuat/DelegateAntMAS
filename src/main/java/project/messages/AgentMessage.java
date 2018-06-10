package project.messages;

import com.github.rinde.rinsim.core.model.comm.MessageContents;

import project.smartMessage.SmartMessage;

public class AgentMessage implements MessageContents {

	private SmartMessage smartMessage;
	
	public AgentMessage(SmartMessage smartMessage){
		this.smartMessage = smartMessage;
	}
	
	public SmartMessage retrieveSmartMessage() {
		return this.smartMessage;
	}
}