package ru.r2cloud.lrpt.meteor;

public class ChannelType {

	public static final int ADMIN_PACKET_APID = 70;
	public static final int DEFAULT_IR_APID = 68;
	public static final int DEFAULT_RED_APID = 66;
	public static final int DEFAULT_GREEN_APID = 65;
	public static final int DEFAULT_BLUE_APID = 64;

	private int apid;
	private String description;

	public ChannelType() {
		// do nothing
	}

	public ChannelType(int apid, String description) {
		this.apid = apid;
		this.description = description;
	}

	public int getApid() {
		return apid;
	}

	public void setApid(int apid) {
		this.apid = apid;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + apid;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChannelType other = (ChannelType) obj;
		if (apid != other.apid)
			return false;
		return true;
	}

}
