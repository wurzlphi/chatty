
package chatty.util.gif;

import java.awt.Dimension;

/**
 * An image consisting of several frames (e.g. an animated GIF), intended to be
 * animated by AnimatedImageSource.
 * 
 * @author tduva
 */
public interface AnimatedImage {

    public int[] getFrame(int frame) throws Exception;
    public int getFrameCount();
    public int getDelay(int frame);
    public Dimension getSize();
    public String getName();
    public int getPreferredPauseFrame();
    
    public static void setAnimationPause(int state) {
        AnimatedImageSource.setAnimationPause(state);
    }
    
}
