package it.geosolutions.urltesting;

import it.geosolutions.urltesting.filters.ImageDigestBuilder;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.security.NoSuchAlgorithmException;

import javax.media.jai.PlanarImage;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;


public class ImageAssert
{

    /**
     * Checks the image has the specified size
     * @param width
     * @param height
     * @param image
     */
    public static void assertSize(int width, int height, RenderedImage image)
    {
        assertEquals("Unexpected image width", width, image.getWidth());
        assertEquals("Unexpected image height", height, image.getHeight());
    }

    /**
     * Checks the basic pixel structure in the image
     */
    public static void assertPixelStructure(int bands, int bitsPerPixel, RenderedImage image)
    {
        assertEquals("Unexpected number of bands", bands, image.getSampleModel().getNumBands());

        int bits = 0;
        for (int bpp : image.getSampleModel().getSampleSize())
        {
            bits += bpp;
        }
        assertEquals("Unexpected number of bits per pixel", bitsPerPixel, bits);
    }

    /**
     * Basic checks on the image color model
     * @param colorComponents
     * @param transparency
     * @param alpha
     * @param image
     */
    public static void assertColorModel(int colorComponents, int transparency, boolean alpha, RenderedImage image)
    {
        assertEquals("Unexecpted number of color components", colorComponents, image.getColorModel().getNumColorComponents());
        assertEquals("Transparency check", transparency, image.getColorModel().getTransparency());
        assertEquals("Unexpected alpha channel absence/presence", alpha, image.getColorModel().hasAlpha());
    }

    /**
     * Fuzzy digest checks
     *
     * @param digest
     * @param image
     */
    public static void assertFuzzyDigest(String digest, RenderedImage image) throws NoSuchAlgorithmException
    {
        String actual = new ImageDigestBuilder(image).getPixelFuzzyDigest();
        assertEquals("Fuzzy digest did not match", digest, actual);
    }

    /**
     * Checks the pixel i/j has the specified color
     * @param image
     * @param i
     * @param j
     * @param color
     */
    public static void assertPixelEquals(RenderedImage image, int col, int row, Color color)
    {
        Color actual = getPixelColor(image, col, row);
        assertEquals(color, actual);
    }

    /**
     * Checks the pixel i/j has the specified color
     * @param image
     * @param col
     * @param row
     * @param color
     */
    public static void assertPixelNotEquals(RenderedImage image, int col, int row, Color color)
    {
        Color actual = getPixelColor(image, col, row);
        assertThat(color, not(equalTo(actual)));
    }


    /**
     * Gets a specific pixel color from the specified buffered image
     *
     * TODO: make this work directly on the image tiles in case the image is not a buffered one
     * @param image
     * @param col
     * @param row
     * @param color
     * @return
     */
    public static Color getPixelColor(RenderedImage image, int col, int row)
    {
        ColorModel cm = image.getColorModel();
        Raster raster;
        if (image instanceof BufferedImage)
        {
            raster = ((BufferedImage) image).getRaster();
        }
        else
        {
            raster = PlanarImage.wrapRenderedImage(image).getAsBufferedImage().getRaster();
        }

        Object pixel = raster.getDataElements(col, row, null);

        Color actual;
        if (cm.hasAlpha())
        {
            actual = new Color(cm.getRed(pixel), cm.getGreen(pixel), cm.getBlue(pixel), cm.getAlpha(pixel));
        }
        else
        {
            actual = new Color(cm.getRed(pixel), cm.getGreen(pixel), cm.getBlue(pixel), 255);
        }

        return actual;
    }
}
