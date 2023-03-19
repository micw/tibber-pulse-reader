package de.wyraz.sml;

public class SMLMessage extends AbstractSMLObject {
	
	protected byte[] transactionId;
	protected Integer groupNo;
	protected Integer abortOnError;
	protected SMLMessageBody messageBody;
	protected Integer crc16Actual;
	protected Integer crc16Expected;
	protected Boolean crc16Ok;
	
	public byte[] getTransactionId() {
		return transactionId;
	}
	public Integer getGroupNo() {
		return groupNo;
	}
	public Integer getAbortOnError() {
		return abortOnError;
	}
	public SMLMessageBody getMessageBody() {
		return messageBody;
	}
	public Integer getCrc16Actual() {
		return crc16Actual;
	}
	public Integer getCrc16Expected() {
		return crc16Expected;
	}
	public Boolean getCrc16Ok() {
		return crc16Ok;
	}
	
	
}

