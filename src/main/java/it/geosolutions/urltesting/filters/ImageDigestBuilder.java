package it.geosolutions.urltesting.filters;

import it.geosolutions.urltesting.util.ContentTypeHelper;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.media.jai.PlanarImage;



public class ImageDigestBuilder
{

    public static final int FUZZY_ROUND_INTERVALS = 10;

    private PlanarImage image;

    public ImageDigestBuilder(RenderedImage image)
    {
        this.image = PlanarImage.wrapRenderedImage(image);
    }

    /**
     * Obtain a digest on a fuzzy approximation of the pixel values. This rounds each pixel to the nearest
     * multiple of {@link #FUZZY_ROUND_INTERVALS} and uses that value for the digest calculation.
     * The goal is to provide a value that will be consistent for minor changes to the image
     * @return A digest value on a fuzzy approximation of the pixels.
     * @throws NoSuchAlgorithmException
     */
    private String getPixelFuzzyDigest(int roundPixels) throws NoSuchAlgorithmException
    {
        // note that we assume we can load the entire image in memory, if this breaks (due to memory limitations)
        // move to use RandomIter for accessing each pixel (which is slower), but requires less memory
        // or get chunks at a time using the same approach below
        Raster raster = image.getData();
        int width = image.getWidth();
        int height = image.getHeight();
        int numBands = image.getNumBands();
        int[] pixels = new int[height * width * numBands];
        raster.getPixels(0, 0, width, height, pixels);

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        for (int h = 0; h < height; h++)
        {
            for (int w = 0; w < width; w++)
            {
                int offset = (h * width * numBands) + (w * numBands);
                for (int band = 0; band < numBands; band++)
                {
                    int roundVal = (int) (pixels[offset + band] / roundPixels);
                    // just in case we really have an int, we convert it to a byte[] for the digest
                    md.update(
                        new byte[]
                        {
                            (byte) (roundVal >>> 24),
                            (byte) (roundVal >>> 16),
                            (byte) (roundVal >>> 8),
                            (byte) (roundVal)
                        });

                }
            }
        }

        return ContentTypeHelper.getHexString(md.digest());
    }

    /**
     * Builds a fuzzy digest of the image, that is, one that won't change
     * if the image pixel values change only slightly
     * @return
     * @throws NoSuchAlgorithmException
     */
    public String getPixelFuzzyDigest() throws NoSuchAlgorithmException
    {
        return getPixelFuzzyDigest(FUZZY_ROUND_INTERVALS);
    }


    /**
     * Builds a straigth digest of the image, that is, one that will change
     * if the image pixel values change only slightly
     * @return
     * @throws NoSuchAlgorithmException
     */
    public String getPixelDigest() throws NoSuchAlgorithmException
    {
        return getPixelFuzzyDigest(FUZZY_ROUND_INTERVALS);
    }


}

