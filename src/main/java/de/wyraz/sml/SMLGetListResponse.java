package de.wyraz.sml;

import java.util.List;

public class SMLGetListResponse extends SMLMessageBody {
	
	protected byte[] clientId;
	protected byte[] serverId;
	protected byte[] listName;
	protected SMLTime actSensorTime;
	protected List<SMLListEntry> valList;
	protected byte[] listSignature;
	protected SMLTime actGatewayTime;
	
	public byte[] getClientId() {
		return clientId;
	}
	public byte[] getServerId() {
		return serverId;
	}
	public byte[] getListName() {
		return listName;
	}
	public SMLTime getActSensorTime() {
		return actSensorTime;
	}
	public List<SMLListEntry> getValList() {
		return valList;
	}
	public byte[] getListSignature() {
		return listSignature;
	}
	public SMLTime getActGatewayTime() {
		return actGatewayTime;
	}
	
	
}
