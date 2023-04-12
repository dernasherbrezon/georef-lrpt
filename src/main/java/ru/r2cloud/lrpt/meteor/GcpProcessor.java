package ru.r2cloud.lrpt.meteor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.SinCos;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.rugged.api.AlgorithmId;
import org.orekit.rugged.api.BodyRotatingFrameId;
import org.orekit.rugged.api.EllipsoidId;
import org.orekit.rugged.api.InertialFrameId;
import org.orekit.rugged.api.Rugged;
import org.orekit.rugged.api.RuggedBuilder;
import org.orekit.rugged.linesensor.LineDatation;
import org.orekit.rugged.linesensor.LineSensor;
import org.orekit.rugged.los.FixedRotation;
import org.orekit.rugged.los.LOSBuilder;
import org.orekit.rugged.los.TimeDependentLOS;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.r2cloud.lrpt.OreKitDataClient;
import ru.r2cloud.lrpt.model.CommandLineArgs;

public class GcpProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(GcpProcessor.class);
	private static final int WIDTH = 1568;

	private String orekitPath;

	private List<String> orekitUrls;

	private Frame eme2000;
	private BodyCenterPointing earthCenterAttitudeLaw;
	private List<Vector3D> rawDirs;

	public GcpProcessor(String orekitPath, List<String> orekitUrls) {
		this.orekitPath = orekitPath;
		this.orekitUrls = orekitUrls;
	}

	public void start() {
		File orekitData = new File(orekitPath);
		if (!orekitData.exists()) {
			LOG.info("orekit master data doesn't exist. downloading now. it might take some time");
			OreKitDataClient client = new OreKitDataClient(orekitUrls);
			try {
				client.downloadAndSaveTo(orekitData.toPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		DataProvidersManager manager = DataProvidersManager.getInstance();
		manager.addProvider(new DirectoryCrawler(orekitData));

		eme2000 = FramesFactory.getEME2000();
		Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
		OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, earthFrame);
		earthCenterAttitudeLaw = new BodyCenterPointing(eme2000, earth);

		rawDirs = new ArrayList<>();
		final double delta = FastMath.toRadians(110.5) / WIDTH;
		for (int i = 0; i < WIDTH; i++) {
			final SinCos sc = FastMath.sinCos((i - WIDTH / 2) * delta);
			rawDirs.add(new Vector3D(0.0, sc.sin(), sc.cos()));
		}
	}

	public File process(int imageHeight, File imageFile, String outputFilename, CommandLineArgs params, TLE tle, LineDatation lineDatation, ChannelType red, ChannelType green, ChannelType blue) {
		// 1. configure TLE
		TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(tle, earthCenterAttitudeLaw, 2700);
		tlePropagator.setSlaveMode();

		// 2. configure LOS
		LOSBuilder losBuilder = new LOSBuilder(rawDirs);
		if (params.getIrotationDegrees() != null) {
			losBuilder.addTransform(new FixedRotation("i-rotation", Vector3D.PLUS_I, FastMath.toRadians(params.getIrotationDegrees())));
		}
		if (params.getJrotationDegrees() != null) {
			losBuilder.addTransform(new FixedRotation("j-rotation", Vector3D.PLUS_J, FastMath.toRadians(params.getJrotationDegrees())));
		}
		TimeDependentLOS lineOfSight = losBuilder.build();
		LineSensor lineSensor = new LineSensor("mySensor", lineDatation, Vector3D.ZERO, lineOfSight);

		// 3. setup pv and angular coordinates
		List<TimeStampedPVCoordinates> satellitePVList = new ArrayList<>();
		List<TimeStampedAngularCoordinates> satelliteQList = new ArrayList<>();
		AbsoluteDate absDate = lineDatation.getDate(0);
		LOG.info("generating .vrt for: {}", absDate);
		// FIXME find number of seconds in the pass
		for (int i = 0; i < 60 * 10; i++) {
			AbsoluteDate ephemerisDate = absDate.shiftedBy(i);
			TimeStampedPVCoordinates pvCoordinates = tlePropagator.getPVCoordinates(ephemerisDate, eme2000);
			satellitePVList.add(pvCoordinates);
			Attitude att = earthCenterAttitudeLaw.getAttitude(tlePropagator, ephemerisDate, eme2000);
			satelliteQList.add(att.getOrientation());
		}

		// 4. configure Rugged
		Rugged rugged = new RuggedBuilder().setAlgorithm(AlgorithmId.IGNORE_DEM_USE_ELLIPSOID).setEllipsoid(EllipsoidId.WGS84, BodyRotatingFrameId.ITRF).setTimeSpan(absDate, absDate.shiftedBy(60 * 10), 0.01, 8 / lineSensor.getRate(0))
				.setTrajectory(InertialFrameId.EME2000, satellitePVList, 4, CartesianDerivativesFilter.USE_PV, satelliteQList, 4, AngularDerivativesFilter.USE_R).addLineSensor(lineSensor).build();

		// 5. setup GCP sparce indexes
		int heightRate;
		// FIXME 0 line and last line (8pixels height) are the same.
		// that's because time is the same
		if (imageHeight / 64 > 1) {
			// take into account every 8 rows. each row is 8 pixels
			heightRate = 8;
		} else {
			int rows = imageHeight / 8;
			if (rows % 2 == 0) {
				// speed up things a bit for pictures under 8 rows
				// take 0,2,4,8 rows instead of each row
				heightRate = 2;
			} else {
				heightRate = 1;
			}
		}
		int widthRate = 32;
		List<Integer> widthIndexes = new ArrayList<>();
		for (int j = 0; j < WIDTH / widthRate; j++) {
			widthIndexes.add(j * widthRate);
		}
		widthIndexes.add(WIDTH - 1);
		List<Integer> heightIndexes = new ArrayList<>();
		int i = 0;
		int numberOfLines = imageHeight / 8;
		int reducedNumber = numberOfLines / heightRate;
		// skip 1 before last row
		if (heightRate == 1) {
			reducedNumber--;
		}
		for (; i < reducedNumber; i++) {
			heightIndexes.add(i * heightRate * 8);
		}
		heightIndexes.add(imageHeight - 1);

		// 7. output file
		File result = new File(imageFile.getParentFile(), outputFilename);
		long counter = 0;
		Vector3D position = lineSensor.getPosition();
		try (BufferedWriter w = new BufferedWriter(new FileWriter(result))) {
			w.append("<VRTDataset rasterXSize=\"1568\" rasterYSize=\"" + imageHeight + "\">");
			w.append("<GCPList Projection=\"EPSG:4326\">");
			for (Integer height : heightIndexes) {
				AbsoluteDate firstLineDate = lineSensor.getDate(height);
				if (firstLineDate == null) {
					// raw data can contain gaps with no timestamps
					continue;
				}
				for (Integer widthIndex : widthIndexes) {
					Vector3D los = lineSensor.getLOS(firstLineDate, widthIndex);
					GeodeticPoint gp = rugged.directLocation(firstLineDate, position, los);
					w.append("<GCP Id=\"" + counter + "\" Pixel=\"" + (WIDTH - widthIndex) + ".5\" Line=\"" + (height) + ".5\" X=\"" + FastMath.toDegrees(gp.getLongitude()) + "\" Y=\"" + FastMath.toDegrees(gp.getLatitude()) + "\" Z=\"0.0\" />\n");
					counter++;
				}
			}
			w.append("</GCPList>");
			w.append("<VRTRasterBand dataType=\"Byte\" band=\"1\"><Description>" + red.getDescription() + "</Description><SimpleSource><SourceFilename relativeToVRT=\"1\">" + imageFile.getName() + "</SourceFilename><SourceBand>1</SourceBand></SimpleSource></VRTRasterBand>\n");
			w.append("<VRTRasterBand dataType=\"Byte\" band=\"2\"><Description>" + green.getDescription() + "</Description><SimpleSource><SourceFilename relativeToVRT=\"1\">" + imageFile.getName() + "</SourceFilename><SourceBand>2</SourceBand></SimpleSource></VRTRasterBand>\n");
			w.append("<VRTRasterBand dataType=\"Byte\" band=\"3\"><Description>" + blue.getDescription() + "</Description><SimpleSource><SourceFilename relativeToVRT=\"1\">" + imageFile.getName() + "</SourceFilename><SourceBand>3</SourceBand></SimpleSource></VRTRasterBand>\n");
			w.append("<VRTRasterBand dataType=\"Byte\" band=\"4\"><Description>Alpha</Description><ColorInterp>Alpha</ColorInterp><SimpleSource><SourceFilename relativeToVRT=\"1\">" + imageFile.getName() + "</SourceFilename><SourceBand>4</SourceBand></SimpleSource></VRTRasterBand>\n");
			w.append("</VRTDataset>");
		} catch (IOException e) {
			LOG.error("unable to write .vrt file: " + result.getAbsolutePath(), e);
			return null;
		}
		return result;
	}

}
