package ru.r2cloud.lrpt.meteor;

import java.util.Date;

public class ImagePacket {

	private int apid;
	private Date date;
	private int sequenceCount;
	private byte[] raw;
	private long vcduId;
	private boolean processed;

	public boolean isProcessed() {
		return processed;
	}

	public void setProcessed(boolean processed) {
		this.processed = processed;
	}

	public long getVcduId() {
		return vcduId;
	}

	public void setVcduId(long vcduId) {
		this.vcduId = vcduId;
	}

	public int getApid() {
		return apid;
	}

	public void setApid(int apid) {
		this.apid = apid;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public int getSequenceCount() {
		return sequenceCount;
	}

	public void setSequenceCount(int sequenceCount) {
		this.sequenceCount = sequenceCount;
	}

	public byte[] getRaw() {
		return raw;
	}

	public void setRaw(byte[] raw) {
		this.raw = raw;
	}

}
