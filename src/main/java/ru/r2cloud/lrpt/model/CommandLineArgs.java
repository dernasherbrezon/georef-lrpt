package ru.r2cloud.lrpt.model;

import java.util.List;

import com.beust.jcommander.Parameter;

import ru.r2cloud.lrpt.FileValidator;

public class CommandLineArgs {

	@Parameter(names = "--tle-file", description = "File with TLE. Should be from the same data as VCDU", required = true, validateWith = FileValidator.class)
	private String tleFile;

	@Parameter(names = "--vcdu-files", description = "Comma-separated files containing VCDU in the binary format. Example: ./*.vcdu", required = true)
	private List<String> vcduFiles;

	@Parameter(names = "--irotation-degrees", description = "An angle between satellite camera and nadir. Default: 2.6")
	private Double irotationDegrees = 2.0;

	@Parameter(names = "--jrotation-degrees", description = "An angle between satellite camera and nadir. Default: 0.6")
	private Double jrotationDegrees = 0.4;

	@Parameter(names = "--output-dir", description = "Directory where to output the result. Default: current", required = true, validateWith = FileValidator.class)
	private String outputDir = ".";

	@Parameter(names = "--start-time", description = "Time in UTC timezone of the first VCDU file. Format: yyyy-MM-dd HH:mm:ss")
	private String startTime;

	@Parameter(names = "--orekit-path", description = "Path where to find orekit urls or where to download them into. Default: current")
	private String orekitPath = ".";

	@Parameter(names = "--orekit-urls", description = "Comma-separated urls where to download orekit master data. Default: https://gitlab.orekit.org/orekit/orekit-data/-/archive/master/orekit-data-master.zip,https://r2cloud.s3.amazonaws.com/dist/orekit-data-master.zip")
	private List<String> orekitUrls;

	@Parameter(names = "--help", description = "This help", help = true)
	private boolean help;

	public String getOrekitPath() {
		return orekitPath;
	}

	public void setOrekitPath(String orekitPath) {
		this.orekitPath = orekitPath;
	}

	public List<String> getOrekitUrls() {
		return orekitUrls;
	}

	public void setOrekitUrls(List<String> orekitUrls) {
		this.orekitUrls = orekitUrls;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getOutputDir() {
		return outputDir;
	}

	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	public String getTleFile() {
		return tleFile;
	}

	public void setTleFile(String tleFile) {
		this.tleFile = tleFile;
	}

	public List<String> getVcduFiles() {
		return vcduFiles;
	}

	public void setVcduFiles(List<String> vcduFiles) {
		this.vcduFiles = vcduFiles;
	}

	public Double getIrotationDegrees() {
		return irotationDegrees;
	}

	public void setIrotationDegrees(Double irotationDegrees) {
		this.irotationDegrees = irotationDegrees;
	}

	public Double getJrotationDegrees() {
		return jrotationDegrees;
	}

	public void setJrotationDegrees(Double jrotationDegrees) {
		this.jrotationDegrees = jrotationDegrees;
	}

	public boolean isHelp() {
		return help;
	}

	public void setHelp(boolean help) {
		this.help = help;
	}

}
