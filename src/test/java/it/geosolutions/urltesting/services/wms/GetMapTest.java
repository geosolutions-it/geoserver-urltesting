package it.geosolutions.urltesting.services.wms;

import static it.geosolutions.urltesting.ImageAssert.assertColorModel;
import static it.geosolutions.urltesting.ImageAssert.assertFuzzyDigest;
import static it.geosolutions.urltesting.ImageAssert.assertPixelStructure;
import static it.geosolutions.urltesting.ImageAssert.assertSize;
import it.geosolutions.urltesting.HTTPRequestBuilder;
import it.geosolutions.urltesting.HttpTest;

import java.awt.Transparency;
import java.awt.image.RenderedImage;

import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;


public class GetMapTest extends HttpTest
{

    public GetMapTest()
    {
        super();
    }

    private HTTPRequestBuilder getMap()
    {
        return http("/geoserver/wms").queryString(
                "service=WMS&version=1.1.0&request=GetMap&srs=EPSG:4326&styles=&format=image/png");
    }

    @Test
    public void WMS_GetMap_States() throws Exception
    {
        GetMethod method =
            getMap().kvp("layers", "topp:states").kvp("transparent",
                "true").kvp("styles", "").kvp("width", "1216").kvp("height", "995").kvp("bbox",
                "-111.807947851619,31.570406701588,-109.681226776512,33.3091803003854").get();
        assertHttpResponse(200, method, "image/png");

        // turn into a rendered image
        RenderedImage image = image(method);
        assertSize(1216, 995, image);
        assertPixelStructure(4, 32, image);
        assertColorModel(3, Transparency.TRANSLUCENT, true, image);
        assertFuzzyDigest("d3153443895e5ebdfa07f792711561c0bcf26d3b", image);
    }
/*
    // @Test
    public void testTranslucentPng8() throws Exception
    {
        GetMethod method =
            http("/mapservice/wmsaccess").queryString(
                "service=WMS&version=1.1.0&request=GetMap&srs=EPSG:4326&styles=&format=image/png8").kvp("layers",
                "DigitalGlobe:ImageStripFootprint,DigitalGlobe:ImageStrip").kvp("transparent",
                "true").kvp("styles", ",").kvp("width", "1216").kvp("height", "995").kvp("bbox",
                "-111.807947851619,31.570406701588,-109.681226776512,33.3091803003854").get();
        assertHttpResponse(200, method, "image/png");

        // turn into a rendered image
        RenderedImage image = image(method);
        assertSize(1216, 995, image);
        assertPixelStructure(1, 8, image);
        assertColorModel(3, Transparency.TRANSLUCENT, true, image);
        // make sure the color model is paletted with translucent entries, instead of having a single transparent entry
        Assert.assertTrue(((IndexColorModel) image.getColorModel()).getMapSize() > 4);
        assertFuzzyDigest("7de1fa9f822c6c2e48054fb9d392ce15bb5daedf", image);
    }

    @Test
    public void testGeneralizedFeatures() throws Exception
    {
        GetMethod method =
            http("/mapservice/wmsaccess").queryString(
                "LAYERS=DigitalGlobe%3AImageStripMetadata&SRS=EPSG%3A3857&SERVICE=WMS" +
                "&VERSION=1.1.1&REQUEST=GetMap&STYLES=&BBOX=7514065.6275,3572323.369573,8140237.763125,4198495.505198" +
                "&WIDTH=256&HEIGHT=256&FORMAT=image%2Fpng8&TRANSPARENT=TRUE").get();
        assertHttpResponse(200, method, "image/png");

        // turn into a rendered image
        RenderedImage image = image(method);
        assertSize(256, 256, image);
        assertPixelStructure(1, 8, image);
        assertColorModel(3, Transparency.TRANSLUCENT, true, image);
        // make sure the color model is paletted with translucent entries, instead of having a single transparent entry
        Assert.assertTrue(((IndexColorModel) image.getColorModel()).getMapSize() > 4);
        // assertFuzzyDigest("dd51c0b69a2435010657dcce93c46c9014d0d124", image);
        //
        // apparently digest changed again on 20131107 on false positive failure because of new imagery
        //
        assertFuzzyDigest("e5318e2c5232e7c03adc5b76d43e4ab81aa79a1a", image);
    }

    @Test
    public void bgColor() throws Exception
    {
        GetMethod method =
            getMap().queryString(
                "LAYERS=DigitalGlobe:ImageStrip&FORMAT=image/png&BBOX=-111.18,32.1,-111,32.19&srcWIDTH=700&srcHEIGHT=500&width=256&height=256&bgcolor=0xFF0000").get();
        assertHttpResponse(200, method, "image/png");

        RenderedImage image = image(method);
        // ImageIO.write(image, "png", new java.io.File("/tmp/image.png"));
        assertSize(256, 256, image);
        assertPixelStructure(3, 24, image);
        assertColorModel(3, Transparency.OPAQUE, false, image);
        assertFuzzyDigest("2fade093d77dc3e60fc8ec31bb73c9bcc9f107c3", image);

        // double check we have letter boxing on top and bottom, but not on sides, and that it matches the specified bg color
        assertPixelEquals(image, 128, 0, Color.RED);
        assertPixelEquals(image, 128, 255, Color.RED);
        // assertPixelNotEquals(image, 0, 128, Color.RED);
        // assertPixelNotEquals(image, 255, 128, Color.RED);
    }

    @Test
    public void thumbnailSuccesfull() throws Exception
    {
        GetMethod method =
            getMap().queryString(
                "LAYERS=DigitalGlobe:ImageStrip&FORMAT=image/png&BBOX=-111.18,32.1,-111,32.19&srcWIDTH=700&srcHEIGHT=500&width=256&height=256").get();
        assertHttpResponse(200, method, "image/png");

        RenderedImage image = image(method);
        // ImageIO.write(image, "png", new java.io.File("/tmp/image.png"));
        assertSize(256, 256, image);
        assertPixelStructure(3, 24, image);
        assertColorModel(3, Transparency.OPAQUE, false, image);
        assertFuzzyDigest("ac8785424805080af15602b9527c37695a93bb1a", image);

        // double check we have letter boxing on top and bottom, but not on sides
        assertPixelEquals(image, 128, 0, Color.WHITE);
        assertPixelEquals(image, 128, 255, Color.WHITE);
        assertPixelNotEquals(image, 0, 128, Color.BLACK);
        assertPixelNotEquals(image, 255, 128, Color.BLACK);
    }

    @Test
    public void thumbnailSourceTooLarge() throws Exception
    {
        GetMethod method =
            getMap().queryString(
                "LAYERS=DigitalGlobe:ImageStrip&FORMAT=image/jpeg&BBOX=-111.18,32.1,-111,32.19&srcWIDTH=8000&srcHEIGHT=8000&width=256&height=256").get();
        // map service filter does not abide to proper OGC style reporting and uses http codes instead
        assertEquals(200, method.getStatusCode());
    }

    @Test
    public void thumbnailTooLarge() throws Exception
    {
        GetMethod method =
            getMap().queryString(
                "LAYERS=DigitalGlobe:ImageStrip&FORMAT=image/jpeg&BBOX=-111.18,32.1,-111,32.19&srcWIDTH=1600&srcHEIGHT=1600&width=1500&height=1500").get();
        // this is a proper service exception
        assertHttpResponse(200, method, "application/vnd.ogc.se_xml", "application/vnd.ogc.se_xml;charset=UTF-8");

        Document dom = dom(method);
        assertXpathEvaluatesTo("1", "count(//ServiceExceptionReport/ServiceException)", dom);

        String exceptionMessage = XMLUnit.newXpathEngine().evaluate("//ServiceExceptionReport/ServiceException", dom);
        Assert.assertTrue(exceptionMessage.contains("width and height maximum allowed value is"));
    }

    @Test
    public void thumbnailTooSmall() throws Exception
    {
        GetMethod method =
            getMap().queryString(
                "LAYERS=DigitalGlobe:ImageStrip&FORMAT=image/jpeg&BBOX=-111.18,32.1,-111,32.19&srcWIDTH=700&srcHEIGHT=500&width=10&height=10").get();
        // this is a proper service exception
        assertHttpResponse(200, method, "application/vnd.ogc.se_xml", "application/vnd.ogc.se_xml;charset=UTF-8");

        Document dom = dom(method);
        assertXpathEvaluatesTo("1", "count(//ServiceExceptionReport/ServiceException)", dom);

        String exceptionMessage = XMLUnit.newXpathEngine().evaluate("//ServiceExceptionReport/ServiceException", dom);
        Assert.assertTrue(exceptionMessage.contains("width and height minimum allowed value is"));
    }

    @Test
    public void thumbnailLargerThanSource() throws Exception
    {
        GetMethod method =
            getMap().queryString(
                "LAYERS=DigitalGlobe:ImageStrip&FORMAT=image/jpeg&BBOX=-111.18,32.1,-111,32.19&srcWIDTH=700&srcHEIGHT=500&width=701&height=501").get();
        // this is a proper service exception
        assertHttpResponse(200, method, "application/vnd.ogc.se_xml", "application/vnd.ogc.se_xml;charset=UTF-8");

        Document dom = dom(method);
        assertXpathEvaluatesTo("1", "count(//ServiceExceptionReport/ServiceException)", dom);

        String exceptionMessage = XMLUnit.newXpathEngine().evaluate("//ServiceExceptionReport/ServiceException", dom);
        Assert.assertTrue(exceptionMessage.contains("srcWidth and srcHeight must be larger than the requested width and height"));
    }


    @Test
    public void thumbnailCustomLetterboxColor() throws Exception
    {
        GetMethod method =
            getMap().queryString(
                "LAYERS=DigitalGlobe:ImageStrip&FORMAT=image/png&BBOX=-111.4,32.1,-111.2,32.19&srcWIDTH=700&srcHEIGHT=500&width=256&height=256" +
                "&bgColor=0xFFFFFF&letterBoxColor=0xFF0000").get();
        assertHttpResponse(200, method, "image/png");

        RenderedImage image = image(method);
        // ImageIO.write(image, "png", new java.io.File("/tmp/image.png"));
        assertSize(256, 256, image);
        assertPixelStructure(3, 24, image);
        assertColorModel(3, Transparency.OPAQUE, false, image);
        assertFuzzyDigest("ac8785424805080af15602b9527c37695a93bb1a", image);

        // double check we have letter boxing on top and bottom red, and bg white on the left
        // assertPixelEquals(image, 150, 0, Color.RED);
        // assertPixelEquals(image, 150, 255, Color.RED);
        assertPixelEquals(image, 0, 120, Color.WHITE);
    }*/
}
