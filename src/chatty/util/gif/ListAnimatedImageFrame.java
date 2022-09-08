
package chatty.util.gif;

import com.pngencoder.PngEncoder;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.IntStream;


/**
 * A single frame of a ListAnimatedImage. The pixel data is compressed in the
 * PNG format. Decoding the pixel data on each frame may cause higher CPU usage
 * than just storing the entire frame uncompressed, however the difference seems
 * fairly small. It might make sense to make compression optional though.
 * 
 * @author tduva
 */
public class ListAnimatedImageFrame {

    private final int delay;
    private final int width;
    private final int height;
    private final int visiblePixelCount;

    private final int[] uncompressed;
    
    public ListAnimatedImageFrame(BufferedImage image, int delay) throws IOException {
        this.delay = delay;
        this.width = image.getWidth();
        this.height = image.getHeight();
        
        // Determine frame "visibility"
        uncompressed = image.getRGB(0, 0, width, height, null, 0, width);
        int transparentPixelCount = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = uncompressed[y * width + x];
                if ((pixel & 0xff000000) == 0) {
                    transparentPixelCount++;
                }
            }
        }
        this.visiblePixelCount = width * height - transparentPixelCount;
    }
    
    /**
     * Fill the given pixels array with the decoded pixels.
     */
    public int[] getImage() {
        return uncompressed;
    }
    
    public int getDelay() {
        return delay;
    }
    
    /**
     * Returns the number of pixels with higher than 0 alpha value.
     * 
     * @return 
     */
    public int getVisiblePixelCount() {
        return visiblePixelCount;
    }
    
}
