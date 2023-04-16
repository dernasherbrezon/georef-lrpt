package ru.r2cloud.lrpt;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.imageio.ImageIO;

import org.orekit.propagation.analytical.tle.TLE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import ru.r2cloud.jradio.lrpt.LRPTInputStream;
import ru.r2cloud.jradio.lrpt.Packet;
import ru.r2cloud.jradio.lrpt.Vcdu;
import ru.r2cloud.lrpt.meteor.ChannelType;
import ru.r2cloud.lrpt.meteor.GcpProcessor;
import ru.r2cloud.lrpt.meteor.MeteorImage;
import ru.r2cloud.lrpt.model.CommandLineArgs;
import ru.r2cloud.lrpt.model.MeteorLineDatation;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(String[] argv) {
		CommandLineArgs args = new CommandLineArgs();
		JCommander parser = JCommander.newBuilder().addObject(args).build();
		try {
			parser.parse(argv);
		} catch (ParameterException e) {
			LOG.error(e.getMessage());
			parser.usage();
			System.exit(-1);
		}
		if (args.isHelp()) {
			parser.usage();
			return;
		}

		if (args.getOrekitUrls() == null) {
			List<String> urls = new ArrayList<>();
			urls.add("https://gitlab.orekit.org/orekit/orekit-data/-/archive/master/orekit-data-master.zip");
			urls.add("https://r2cloud.s3.amazonaws.com/dist/orekit-data-master.zip");
			args.setOrekitUrls(urls);
		}

		GcpProcessor gcp = new GcpProcessor(args.getOrekitPath(), args.getOrekitUrls());
		gcp.start();

		TLE tle = readTle(args);
		if (tle == null) {
			return;
		}

		List<Path> vcduPaths = new ArrayList<>();
		for (String curPattern : args.getVcduFiles()) {
			int index = curPattern.lastIndexOf('/');
			String basedir = ".";
			if (index != -1) {
				basedir = curPattern.substring(0, index);
			}
			@SuppressWarnings("resource")
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + curPattern);
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(new File(basedir).toPath(), matcher::matches)) {
				dirStream.forEach(path -> {
					try {
						vcduPaths.add(path);
					} catch (Exception e) {
						LOG.info("skipping: {}", path, e);
					}
				});
			} catch (IOException e1) {
				LOG.error("unable to read: {}", curPattern, e1);
			}
		}
		Collections.sort(vcduPaths);
		String filename = null;
		List<Vcdu> vcdus = new ArrayList<>();
		for (Path cur : vcduPaths) {
			if (filename == null) {
				filename = getFilenameWithoutExtension(cur);
			}
			try (LRPTInputStream is = new LRPTInputStream(new BufferedInputStream(new FileInputStream(cur.toFile())))) {
				LOG.info("reading: {}", cur);
				while (is.hasNext()) {
					vcdus.add(is.next());
				}
			} catch (IOException e) {
				LOG.error("unable to read: {}", cur, e);
			}
		}

		Collections.sort(vcdus, new Comparator<Vcdu>() {
			@Override
			public int compare(Vcdu o1, Vcdu o2) {
				return Integer.compare(o1.getCounter(), o2.getCounter());
			}
		});

		List<Packet> packets = new ArrayList<>();
		int previous = 0;
		for (Vcdu curVcdu : vcdus) {
			if (curVcdu.getCounter() == previous) {
				continue;
			}
			previous = curVcdu.getCounter();
			packets.addAll(curVcdu.getPackets());
		}

		if (packets.isEmpty()) {
			LOG.info("no packets found");
			return;
		}

		Date startTime = null;
		if (args.getStartTime() == null) {
			startTime = guessFromTheFilename(filename);
			if (startTime == null) {
				return;
			}
		} else {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			try {
				startTime = sdf.parse(args.getStartTime());
			} catch (Exception e) {
				LOG.error("unable to parse date: {}", args.getStartTime(), e);
				return;
			}
		}
		Date currentDay = convertToMidnightMoscow(startTime, packets.get(0).getMillisecondOfDay());
		for (Packet cur : packets) {
			// this is actually becomes millisecond since 1970 Europe/Moscow
			cur.setMillisecondOfDay(currentDay.getTime() + cur.getMillisecondOfDay());
		}

		File outputBasedir = new File(args.getOutputDir());
		if (!outputBasedir.isDirectory()) {
			LOG.error("output directory is not a directory: {}", outputBasedir.getAbsolutePath());
			return;
		}

		File imageFile = new File(outputBasedir, filename + ".png");
		MeteorImage image = new MeteorImage(packets.iterator());

		Map<Integer, ChannelType> channels = new HashMap<>();
		// this is actually green, but there is nothing at ~0.4μm where blue supposed to
		// be, so shift all spectrum a little bit to get artificial colors
		channels.put(64, new ChannelType(64, "Blue - 0.5~0.7μm"));
		channels.put(65, new ChannelType(65, "Green - 0.7~1.1μm"));
		channels.put(66, new ChannelType(66, "Red - 1.6~1.8μm"));
		channels.put(67, new ChannelType(67, "InfraRed1 - 3.5~4.1μm"));
		channels.put(68, new ChannelType(68, "InfraRed2 - 10.5~11.5μm"));
		channels.put(69, new ChannelType(69, "InfraRed3 - 11.5~12.5μm"));

		ChannelType red = null;
		// only one at a time channel was enabled on meteor. So safe to fallback
		if (image.getChannelByApid().containsKey(66)) {
			red = channels.get(66);
		} else if (image.getChannelByApid().containsKey(67)) {
			red = channels.get(67);
		} else if (image.getChannelByApid().containsKey(68)) {
			red = channels.get(68);
		} else if (image.getChannelByApid().containsKey(69)) {
			red = channels.get(69);
		}
		ChannelType green = channels.get(ChannelType.DEFAULT_GREEN_APID);
		ChannelType blue = channels.get(ChannelType.DEFAULT_BLUE_APID);

		BufferedImage bufImage = image.toBufferedImage(red, green, blue);
		if (bufImage == null) {
			LOG.info("image is empty: {}", filename);
			return;
		}
		if (bufImage.getHeight() < 16) {
			LOG.info("image height is too small: {}", bufImage.getHeight());
			return;
		}
		try {
			ImageIO.write(bufImage, "png", imageFile);
		} catch (IOException e) {
			LOG.error("unable to save image: {}", imageFile.getAbsolutePath(), e);
			return;
		}

		MeteorLineDatation lineDatation = new MeteorLineDatation(image);

		gcp.process(bufImage.getHeight(), imageFile, filename, args, tle, lineDatation, red, green, blue);

	}

	private static Date guessFromTheFilename(String filename) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		try {
			return sdf.parse(filename);
		} catch (Exception e) {
		}
		try {
			return new Date(Long.valueOf(filename));
		} catch (NumberFormatException e2) {
		}
		LOG.error("unable to guess datetime from the filename. Specify explicitly from the command line");
		return null;
	}

	private static Date convertToMidnightMoscow(Date gmtVcdu, long millisSinceMidnight) {
		Calendar vcduDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		vcduDate.setTime(gmtVcdu);

		Calendar result = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
		result.set(Calendar.YEAR, vcduDate.get(Calendar.YEAR));
		result.set(Calendar.MONTH, vcduDate.get(Calendar.MONTH));
		result.set(Calendar.DAY_OF_MONTH, vcduDate.get(Calendar.DAY_OF_MONTH));
		result.set(Calendar.HOUR_OF_DAY, 0);
		result.set(Calendar.MINUTE, 0);
		result.set(Calendar.SECOND, 0);
		result.set(Calendar.MILLISECOND, 0);

		Date resultDate = result.getTime();

		result.setTime(gmtVcdu);
		LOG.info("requested date: {} adjusted date: {}", result.getTime(), new Date(result.getTimeInMillis() + millisSinceMidnight));
		return resultDate;
	}

	private static TLE readTle(CommandLineArgs args) {
		List<String> lines;
		try {
			lines = Files.readAllLines(Path.of(args.getTleFile()));
		} catch (IOException e) {
			LOG.error("unable to read tle file: {}", args.getTleFile(), e);
			return null;
		}

		TLE tle = null;
		if (lines.size() == 2) {
			tle = new TLE(lines.get(0).trim(), lines.get(1).trim());
		} else if (lines.size() >= 3) {
			tle = new TLE(lines.get(1).trim(), lines.get(2).trim());
		} else {
			LOG.error("invalid TLE format. Expected 2 or 3 lines. Got: {}", lines.size());
			return null;
		}
		return tle;
	}

	private static String getFilenameWithoutExtension(Path path) {
		String fullname = path.toFile().getName();
		int index = fullname.lastIndexOf('.');
		if (index == -1) {
			return fullname;
		}
		return fullname.substring(0, index);
	}

}
