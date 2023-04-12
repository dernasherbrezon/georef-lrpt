package ru.r2cloud.lrpt;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MainTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testSuccess() {
		Main.main(new String[] { "--orekit-path", "/Users/dernasherbrezon/git/r2cloud/src/test/resources/data/orekit-data", "--output-dir", tempFolder.getRoot().getAbsolutePath(), "--tle-file", "./src/test/resources/2022-12-20.txt", "--vcdu-files", "./src/test/resources/2022_12_20_20_25_*.vcdu" });
		//FIXME add test
	}

}
