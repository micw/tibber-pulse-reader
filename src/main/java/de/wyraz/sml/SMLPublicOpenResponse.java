package de.wyraz.sml;

public class SMLPublicOpenResponse extends SMLMessageBody {
	
	protected byte[] codepage;
	protected byte[] clientId;
	protected byte[] reqFileId;
	protected byte[] serverId;
	protected SMLTime refTime;
	protected Integer smlVersion;
	
	public byte[] getCodepage() {
		return codepage;
	}
	public void setCodepage(byte[] codepage) {
		this.codepage = codepage;
	}
	public byte[] getClientId() {
		return clientId;
	}
	public void setClientId(byte[] clientId) {
		this.clientId = clientId;
	}
	public byte[] getReqFileId() {
		return reqFileId;
	}
	public void setReqFileId(byte[] reqFileId) {
		this.reqFileId = reqFileId;
	}
	public byte[] getServerId() {
		return serverId;
	}
	public void setServerId(byte[] serverId) {
		this.serverId = serverId;
	}
	public SMLTime getRefTime() {
		return refTime;
	}
	public void setRefTime(SMLTime refTime) {
		this.refTime = refTime;
	}
	public Integer getSmlVersion() {
		return smlVersion;
	}
	public void setSmlVersion(Integer smlVersion) {
		this.smlVersion = smlVersion;
	}
	
	
}
