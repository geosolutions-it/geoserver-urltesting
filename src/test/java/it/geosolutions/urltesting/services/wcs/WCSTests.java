package it.geosolutions.urltesting.services.wcs;

import static it.geosolutions.urltesting.ImageAssert.assertColorModel;
import static it.geosolutions.urltesting.ImageAssert.assertFuzzyDigest;
import static it.geosolutions.urltesting.ImageAssert.assertPixelStructure;
import static it.geosolutions.urltesting.ImageAssert.assertSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import it.geosolutions.urltesting.HTTPRequestBuilder;
import it.geosolutions.urltesting.HttpTest;

import java.awt.Transparency;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geotools.coverage.io.netcdf.NetCDFReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Test class for WCS Services
 * 
 * @author Nicola Lagomarsini GeoSolutions
 */
public class WCSTests extends HttpTest {

    public static final double TOLERANCE = 0.1d;

    private final static Logger LOGGER = Logger.getLogger(WCSTests.class.toString());

    static {
        // Setting NETCDF BASE FOLDER
        try {
            System.setProperty("NETCDF_DATA_DIR", File.createTempFile("temporary", "netcdf")
                    .getAbsolutePath());
        } catch (IOException e) {

            LOGGER.log(Level.SEVERE, e.getMessage(), e);

        }
    }

    private HTTPRequestBuilder getCoverage() {
        return http("/geoserver/wcs").queryString("request=GetCoverage&service=WCS&version=2.0.1");
    }

    @Test
    public void testGetCoverageGeoTiffSingle() throws HttpException, IOException,
            NoSuchAlgorithmException {
        // GetCoverage method
        GetMethod method = getCoverage().kvp("coverageId", "cite__external")
                .kvp("subset", "http://www.opengis.net/def/axis/OGC/0/Long(-20,20)")
                .kvp("subset", "http://www.opengis.net/def/axis/OGC/0/Lat(-50,50)")
                .kvp("scaleFactor", "0.5").kvp("Format", "geotiff").get();

        assertHttpResponse(200, method, "geotiff");

        // turn into a rendered image
        RenderedImage image = image(method);
        assertSize(1200, 3000, image);
        assertPixelStructure(1, 8, image);
        assertColorModel(1, Transparency.OPAQUE, false, image);
        assertFuzzyDigest("5c23df84f0fa0228909472c8f731b942430de425", image);

        // Repeat the request
        method = getCoverage().kvp("coverageId", "cite__external")
                .kvp("subset", "http://www.opengis.net/def/axis/OGC/0/Long(-20,20)")
                .kvp("subset", "http://www.opengis.net/def/axis/OGC/0/Lat(-50,50)")
                .kvp("scaleFactor", "0.5").kvp("Format", "geotiff").get();

        // Reading File as GeoTiff
        GeoTiffReader reader = new GeoTiffReader(method.getResponseBodyAsStream());
        // Ensure Correct BBOX
        GeneralEnvelope envelope = reader.getOriginalEnvelope();
        assertNotNull(envelope);
        assertEquals(envelope.getMinimum(0), -20d, TOLERANCE);
        assertEquals(envelope.getMinimum(1), -50d, TOLERANCE);
        assertEquals(envelope.getMaximum(0), 20d, TOLERANCE);
        assertEquals(envelope.getMaximum(1), 50d, TOLERANCE);
        // Ensure Correct CRS
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
        assertNotNull(crs);
        assertTrue(CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84));
    }

    @Test
    public void testGetCoverageGeoTiffMosaic() throws HttpException, IOException,
            NoSuchAlgorithmException {
        // GetCoverage method
        GetMethod method = getCoverage().kvp("coverageId", "cite__rastermaskint")
                .kvp("subset", "http://www.opengis.net/def/axis/OGC/0/Long(-180,180)")
                .kvp("subset", "http://www.opengis.net/def/axis/OGC/0/Lat(-90,90)")
                .kvp("scaleFactor", "0.5").kvp("Format", "geotiff").get();

        assertHttpResponse(200, method, "geotiff");

        // turn into a rendered image
        RenderedImage image = image(method);
        assertSize(13, 9, image);
        assertPixelStructure(4, 32, image);
        assertColorModel(3, Transparency.TRANSLUCENT, true, image);
        assertFuzzyDigest("397d06ddbc1db5dfec2716ad24b0597dee21e9e5", image);

        // Repeat the request
        method = getCoverage().kvp("coverageId", "cite__rastermaskint")
                .kvp("subset", "http://www.opengis.net/def/axis/OGC/0/Long(-180,180)")
                .kvp("subset", "http://www.opengis.net/def/axis/OGC/0/Lat(-90,90)")
                .kvp("scaleFactor", "0.5").kvp("Format", "geotiff").get();

        // Reading File as GeoTiff
        GeoTiffReader reader = new GeoTiffReader(method.getResponseBodyAsStream());
        // Ensure Correct BBOX
        GeneralEnvelope envelope = reader.getOriginalEnvelope();
        assertNotNull(envelope);
        assertEquals(envelope.getMinimum(0), -90d, TOLERANCE);
        assertEquals(envelope.getMinimum(1), 25d, TOLERANCE);
        assertEquals(envelope.getMaximum(0), -86d, TOLERANCE);
        assertEquals(envelope.getMaximum(1), 28d, TOLERANCE);
        // Ensure Correct CRS
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
        assertNotNull(crs);
        assertTrue(CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84));
    }

    @Test
    public void testGetCoverageNetCDF() throws HttpException, IOException, NoSuchAlgorithmException {
        // GetCoverage method
        GetMethod method = getCoverage()
                .kvp("coverageId", "it.geosolutions__NO2_single")
                .kvp("subset", "http://www.opengis.net/def/axis/OGC/0/Long(5,20)")
                .kvp("subset", "http://www.opengis.net/def/axis/OGC/0/Lat(40,50)")
                .kvp("subset", "http://www.opengis.net/def/axis/OGC/0/elevation(300,1250)")
                .kvp("subset",
                        "http://www.opengis.net/def/axis/OGC/0/time(\"2013-03-01T10:00:00.000Z\",\"2013-03-01T22:00:00.000Z\")")
                .kvp("Format", "application/x-netcdf").get();

        assertHttpResponse(200, method, "application/x-netcdf");

        // turn into a rendered image
        /*
         * RenderedImage image = image(method); assertSize(13, 9, image); assertPixelStructure(4, 32, image); assertColorModel(3,
         * Transparency.TRANSLUCENT, true, image); assertFuzzyDigest("397d06ddbc1db5dfec2716ad24b0597dee21e9e5", image);
         */
        File f = getTemporaryFile(method);

        // Create temporary file directory
        String mainFilePath = f.getCanonicalPath();

        // Selection of the hashcode for creating a unique directory of the auxiliary files
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(mainFilePath.getBytes());
        String hashCode = convertToHex(md.digest());

        String mainName = FilenameUtils.getName(mainFilePath);
        // String extension = FilenameUtils.getExtension(mainName);
        String baseName = FilenameUtils.removeExtension(mainName);
        String outputLocalFolder = "." + baseName + "_" + hashCode;
        File destinationDir = new File(f.getParentFile(), outputLocalFolder);
        FileUtils.forceMkdir(destinationDir);

        // Reading file
        NetCDFReader reader = new NetCDFReader(f, null);
        // Getting Coverage Name
        String[] coverageNames = reader.getGridCoverageNames();
        assertNotNull(coverageNames);
        // Ensure it is a single coverage
        assertEquals(reader.getGridCoverageCount(), 1);
        assertEquals(coverageNames.length, 1);
        String coverageName = coverageNames[0];
        // Ensure Correct BBOX
        GeneralEnvelope envelope = reader.getOriginalEnvelope();
        assertNotNull(envelope);
        assertEquals(envelope.getMinimum(0), 5d, TOLERANCE);
        assertEquals(envelope.getMinimum(1), 45d, TOLERANCE);
        assertEquals(envelope.getMaximum(0), 15d, TOLERANCE);
        assertEquals(envelope.getMaximum(1), 50d, TOLERANCE);
        // Ensure Correct CRS
        CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
        assertNotNull(crs);
        assertTrue(CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84));
        // Parsing metadata values
        assertEquals("true", reader.getMetadataValue(coverageName, "HAS_TIME_DOMAIN"));
        assertEquals("true", reader.getMetadataValue(coverageName, "HAS_ELEVATION_DOMAIN"));

        NetcdfDataset dataset = NetcdfDataset.openDataset(f.getAbsolutePath());
        assertNotNull(dataset);

        // Extracting a few NetCDF information
        List<Variable> variables = dataset.getVariables();
        assertEquals(variables.size(), 5);

        Variable var = null;
        for (Variable v : variables) {
            if (v.getFullName().contains("NO2")) {
                var = v;
            }
        }
        assertNotNull(var);
        dataset.close();
    }

    public static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte)
                        : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    private static File getTemporaryFile(GetMethod method) {
        File file = null;
        try {
            // Try to create the file
            file = File.createTempFile("out", ".nc");
            // read stream
            FileUtils.writeByteArrayToFile(file, method.getResponseBody());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return file;
    }
}
