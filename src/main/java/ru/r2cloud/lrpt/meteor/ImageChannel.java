package ru.r2cloud.lrpt.meteor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ImageChannel {

	public static final int WIDTH = 196 * 8;

	private final int apid;

	private int[] data = new int[8 * WIDTH];
	private int[] transparentData = new int[8 * WIDTH];
	// top left corner of the channel
	private int currentX = 0;
	private int currentY = 0;

	private long millisecondOfDay;
	private int firstPacket;
	private int firstMcu;
	private int lastPacket = -1;
	private int lastMcu = -1;

	private int currentRow;
	private Map<Integer, Long> rowToTimeMillis = new HashMap<>();

	public ImageChannel(int apid) {
		this.apid = apid;
		this.currentRow = 0;
		// by default make all tranparent
		Arrays.fill(transparentData, 0xFF);
	}
	
	public Map<Integer, Long> getRowToTimeMillisMapping() {
		return rowToTimeMillis;
	}

	public void setTimestampForCurrentRow(long timeMillis) {
		rowToTimeMillis.put(currentRow, timeMillis);
	}

	public void fill(int[] mcu) {
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				data[(currentY + row) * WIDTH + (currentX + col)] = mcu[row * 8 + col];
				// make pixel non-transparent
				transparentData[(currentY + row) * WIDTH + (currentX + col)] = 0;
			}
		}
	}

	public void appendRows(int rowsToAdd) {
		if (rowsToAdd == 0) {
			return;
		}
		int[] newData = new int[data.length + 8 * rowsToAdd * WIDTH];
		System.arraycopy(data, 0, newData, 0, data.length);
		this.data = newData;
		
		int[] newTransparent = new int[transparentData.length + 8 * rowsToAdd * WIDTH];
		System.arraycopy(transparentData, 0, newTransparent, 0, transparentData.length);
		Arrays.fill(newTransparent, transparentData.length, newTransparent.length, 0xFF);
		transparentData = newTransparent;
		
		currentY += 8 * rowsToAdd;
		currentRow += rowsToAdd;
	}

	public void prependRows(int numberOfRows) {
		if (numberOfRows == 0) {
			return;
		}
		int numberOfPixelsToPrepend = 8 * numberOfRows * WIDTH;
		int[] newData = new int[data.length + numberOfPixelsToPrepend];
		System.arraycopy(data, 0, newData, numberOfPixelsToPrepend, data.length);
		this.data = newData;
		
		int[] newTransparent = new int[transparentData.length + numberOfPixelsToPrepend];
		System.arraycopy(transparentData, 0, newTransparent, numberOfPixelsToPrepend, transparentData.length);
		Arrays.fill(newTransparent, 0, numberOfPixelsToPrepend, 0xFF);
		transparentData = newTransparent;

		currentY += 8 * numberOfRows;
		Map<Integer, Long> incrementedRowToTimeMillisMapping = new HashMap<>();
		for (Entry<Integer, Long> cur : rowToTimeMillis.entrySet()) {
			incrementedRowToTimeMillisMapping.put(numberOfRows + cur.getKey(), cur.getValue());
		}
		rowToTimeMillis = incrementedRowToTimeMillisMapping;
		currentRow += numberOfRows;
	}

	public int[] getData() {
		return data;
	}

	public int[] getTransparentData() {
		return transparentData;
	}
	
	public int getApid() {
		return apid;
	}

	public int getLastPacket() {
		return lastPacket;
	}

	public void setLastPacket(int lastPacket) {
		this.lastPacket = lastPacket;
	}

	public int getCurrentX() {
		return currentX;
	}

	public void setCurrentX(int currentX) {
		this.currentX = currentX;
	}

	public int getCurrentY() {
		return currentY;
	}

	public void setCurrentY(int nextY) {
		if (nextY > currentY) {
			appendRows((nextY - currentY) / 8);
		}
		this.currentY = nextY;
	}

	public int getFirstPacket() {
		return firstPacket;
	}

	public void setFirstPacket(int firstPacket) {
		this.firstPacket = firstPacket;
	}

	public int getFirstMcu() {
		return firstMcu;
	}

	public void setFirstMcu(int firstMcu) {
		this.firstMcu = firstMcu;
	}

	public int getLastMcu() {
		return lastMcu;
	}

	public void setLastMcu(int lastMcu) {
		this.lastMcu = lastMcu;
	}

	public long getMillisecondOfDay() {
		return millisecondOfDay;
	}

	public void setMillisecondOfDay(long millisecondOfDay) {
		this.millisecondOfDay = millisecondOfDay;
	}
}
