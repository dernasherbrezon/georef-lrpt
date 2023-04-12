package ru.r2cloud.lrpt;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MainTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testSuccess() throws Exception {
		Main.main(new String[] { "--orekit-path", "./src/test/resources/r2cloud-data/orekit-data", "--output-dir", tempFolder.getRoot().getAbsolutePath(), "--tle-file", "./src/test/resources/2022-12-20.txt", "--vcdu-files", "./src/test/resources/2022_12_20_20_25_*.vcdu" });
		assertImage("expected/2022_12_20_20_25_30.png", new File(tempFolder.getRoot(), "2022_12_20_20_25_30.png"));
	}

	public static void assertImage(String expectedFilename, File bais) throws IOException {
		try (InputStream is = new BufferedInputStream(new FileInputStream(bais))) {
			assertImage(expectedFilename, is);
		}
	}

	public static void assertImage(String expectedFilename, InputStream bais) throws IOException {
		try (InputStream is1 = MainTest.class.getClassLoader().getResourceAsStream(expectedFilename)) {
			BufferedImage expected = ImageIO.read(is1);
			BufferedImage actual = ImageIO.read(bais);
			for (int i = 0; i < expected.getWidth(); i++) {
				for (int j = 0; j < expected.getHeight(); j++) {
					assertEquals(expected.getRGB(i, j), actual.getRGB(i, j));
				}
			}
		}
	}
}
