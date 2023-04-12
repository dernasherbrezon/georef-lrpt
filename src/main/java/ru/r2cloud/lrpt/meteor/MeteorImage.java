package ru.r2cloud.lrpt.meteor;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.jradio.lrpt.Packet;
import ru.r2cloud.jradio.meteor.MeteorImagePacket;

public class MeteorImage {

	public static final int METEOR_SPACECRAFT_ID = 0;
	private static final Logger LOG = LoggerFactory.getLogger(MeteorImage.class);

	private final Map<Integer, ImageChannel> channelByApid = new HashMap<>();

	public MeteorImage(Iterator<Packet> input) {
		while (input.hasNext()) {
			Packet cur = input.next();
			if (cur.getApid() == ChannelType.ADMIN_PACKET_APID) {
				continue;
			}
			try {
				MeteorImagePacket meteorPacket = new MeteorImagePacket(cur);
				ImageChannel channel = getOrCreateChannel(cur.getApid());
				// explicitly start from the beginning
				if (channel.getLastPacket() == -1) {
					channel.setCurrentY(0);
					channel.setFirstPacket(cur.getSequenceCount());
					channel.setFirstMcu(meteorPacket.getMcuNumber());
					channel.setMillisecondOfDay(cur.getMillisecondOfDay());
				} else {
					channel.appendRows(ImageChannelUtil.calculateMissingRows(channel.getLastMcu(), channel.getLastPacket(), meteorPacket.getMcuNumber(), cur.getSequenceCount()));
				}
				channel.setTimestampForCurrentRow(cur.getMillisecondOfDay());
				channel.setLastPacket(cur.getSequenceCount());
				channel.setLastMcu(meteorPacket.getMcuNumber());
				channel.setCurrentX(meteorPacket.getMcuNumber() * 8);
				while (meteorPacket.hasNext()) {
					channel.fill(meteorPacket.next());
					channel.setCurrentX(channel.getCurrentX() + 8);
				}
			} catch (Exception e) {
				LOG.error("unable to decode packet", e);
			}
		}

		// find first channel and align other channels based on it
		ImageChannel first = findFirst(channelByApid.values());
		if (first != null) {
			for (ImageChannel cur : channelByApid.values()) {
				if (cur == first) {
					continue;
				}
				ImageChannelUtil.align(first, cur);
			}
		}
	}

	public BufferedImage toBufferedImage(ChannelType redId, ChannelType greenId, ChannelType blueId) {
		if (channelByApid.isEmpty()) {
			return null;
		}
		ImageChannel red = channelByApid.get(redId.getApid());
		ImageChannel green = channelByApid.get(greenId.getApid());
		ImageChannel blue = channelByApid.get(blueId.getApid());

		int maxHeight = -1;
		if (red != null) {
			maxHeight = Math.max(red.getCurrentY() + 8, maxHeight);
		}
		if (green != null) {
			maxHeight = Math.max(green.getCurrentY() + 8, maxHeight);
		}
		if (blue != null) {
			maxHeight = Math.max(blue.getCurrentY() + 8, maxHeight);
		}
		if (maxHeight <= 0) {
			// incorrect apid provided
			return null;
		}
		BufferedImage result = new BufferedImage(ImageChannel.WIDTH, maxHeight, BufferedImage.TYPE_INT_ARGB);
		for (int row = 0; row < result.getHeight(); row++) {
			for (int col = 0; col < result.getWidth(); col++) {
				int index = row * result.getWidth() + col;
				int rgb;
				if (isTransparent(red, index) || isTransparent(green, index) || isTransparent(blue, index)) {
					rgb = 0;
				} else {
					rgb = (0xFF << 24) | getRGB(getColor(red, index), getColor(green, index), getColor(blue, index));
				}
				result.setRGB(col, row, rgb);
			}
		}
		return result;
	}

	public Map<Integer, ImageChannel> getChannelByApid() {
		return channelByApid;
	}

	private static int getRGB(int r, int g, int b) {
		return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	private ImageChannel getOrCreateChannel(int apid) {
		ImageChannel result = channelByApid.get(apid);
		if (result == null) {
			result = new ImageChannel(apid);
			channelByApid.put(apid, result);
		}
		return result;
	}

	private static ImageChannel findFirst(Collection<ImageChannel> all) {
		ImageChannel result = null;
		for (ImageChannel cur : all) {
			if (result == null || cur.getMillisecondOfDay() < result.getMillisecondOfDay()) {
				result = cur;
			}
		}
		return result;
	}

	private static int getColor(ImageChannel channel, int index) {
		if (channel == null || index >= channel.getData().length) {
			return 0;
		}
		return channel.getData()[index];
	}

	private static boolean isTransparent(ImageChannel channel, int index) {
		if (channel == null) {
			return false;
		}
		if (index >= channel.getData().length) {
			return true;
		}
		return channel.getTransparentData()[index] == 0xFF;
	}

}
