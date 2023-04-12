package ru.r2cloud.lrpt.model;

import java.util.Date;
import java.util.Map.Entry;

import org.orekit.rugged.linesensor.LineDatation;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import ru.r2cloud.lrpt.meteor.ImageChannel;
import ru.r2cloud.lrpt.meteor.MeteorImage;

public class MeteorLineDatation implements LineDatation {

	private final MeteorImage image;

	public MeteorLineDatation(MeteorImage image) {
		this.image = image;
	}

	@Override
	public AbsoluteDate getDate(double lineNumber) {
		Date date = getUtcDate(lineNumber);
		if (date == null) {
			return null;
		}
		return new AbsoluteDate(date, TimeScalesFactory.getUTC());
	}

	public Date getUtcDate(double lineNumber) {
		int line = (int) lineNumber / 8;
		for (Entry<Integer, ImageChannel> cur : image.getChannelByApid().entrySet()) {
			Long time = cur.getValue().getRowToTimeMillisMapping().get(line);
			if (time != null) {
				return new Date(time);
			}
		}
		return null;
	}

	@Override
	public double getLine(AbsoluteDate date) {
		return 0;
	}

	@Override
	public double getRate(double lineNumber) {
		return 0;
	}

}
