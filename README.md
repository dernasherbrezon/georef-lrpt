[![Main Workflow](https://github.com/dernasherbrezon/georef-lrpt/actions/workflows/build.yml/badge.svg)](https://github.com/dernasherbrezon/georef-lrpt/actions/workflows/build.yml) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=dernasherbrezon_georef-lrpt&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=dernasherbrezon_georef-lrpt)

# About

georef-lrpt application can georeference images acquired via [LRPT](https://en.wikipedia.org/wiki/Low-rate_picture_transmission) protocol. The result will be a .png file and a .vrt file. VRT file can be used by gdal to create GeoTIFF.

![LRPT georeference](/docs/geotiff-output.png?raw=true)

# Features

* Supported satellites: METEOR-M 2 (NORAD ID: 40069), METEOR-M 2-2 (NORAD ID: 44387)
* 3 channels + 1 alpha channel
* Alpha channel is generated for those regions where was no packets received for all 3 other channels
* Detect extents for manual split on anti-meridian

# How to use

## Step 1. Obtain LRPT binary files

These files should contain VCDU (data link layer frame) as they were received from the satellite. Find examples of such files in the ```src/test/resources/``` directory.

> Note:
> VCDU frames can carry variable number of image packets. So better to have a single binary file with all VCDU sorted in the ascending order or several files with filenames in the ascending order.

## Step 2. Obtain TLE

[TLE](https://en.wikipedia.org/wiki/Two-line_element_set) of the satellite. It should be in the 2-line or 3-line format.
 
> Note:
> TLE should be generated for the same day as binary files for better alignment precision.

## Step 3. Run

Get the help of all supported parameters using the command below:

```
java -jar georef-lrpt.jar --help
```

Example run:

```
java -jar georef-lrpt.jar --output-dir . --tle-file src/test/resources/2022-12-20.txt --vcdu-files "src/test/resources/*.vcdu"
```

The output will contain .png file and .vrt file.

## Step 4. Convert .vrt into GeoTIFF using gdal

Use [gdal](https://gdal.org) to convert .vrt into GeoTIFF:

```
gdalwarp -tps -overwrite -of GTIFF 2022_12_20_20_25_30_134_-14_162_-8.vrt 2022_12_20_20_25_30.tiff
```

The command above will create GeoTIFF file in the EPSG:4326 projection ( longlat WGS84 ). This projection can be further converted into epsg:3857 to use from online maps like [Google maps](http://maps.google.com/) and [OpenStreetMap](https://www.openstreetmap.org).

## Anti-meridian

GDAL and QGIS cannot detect if image is wrapped around projection. Thus special handling is needed. 

  1. Firstly extract geodetic coordinates from the .vrt filename. georef-lrpt will generate .vrt filenames according to the format: ```<first .vcdu filename>_<minX>_<minY>_<maxX>_<maxY>.vrt```. Please note that each coordinate can be negative. For example: ```2022_12_20_20_25_30_134_-14_162_-8.vrt``` Has longitude from 134 to 162 (west to east) and latitude from -14 to -8 (south to north). 

  2. Secondly generate 2 separate tiff files from the same .vrt using gdalwrap. For the filename ```2022_12_20_18_44_50_142_-55_-173_-11.vrt``` two commands need to be executed:
    * ```gdalwarp -tps -overwrite -te 142 -55 180 -11 -of GTiff 2022_12_20_18_44_50_142_-55_-173_-11.vrt 2022_12_20_18_44_50-left.tiff
    * ```gdalwarp -tps -overwrite -te -180 -55 -173 -11 -of GTiff 2022_12_20_18_44_50_142_-55_-173_-11.vrt 2022_12_20_18_44_50-right.tiff
    
Then left and right images can be added to the map. On the screenshot below the right image is drawn without alpha channel, just to highlight wrapped tiff

![Nice anti-meridian support](/docs/anti-meridian.png?raw=true)

 
 