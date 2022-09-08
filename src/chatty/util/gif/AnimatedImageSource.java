
package chatty.util.gif;

import chatty.util.ElapsedTime;

import java.awt.Dimension;
import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


/**
 * An ImageProducer for animated images.
 *
 * @author tduva
 */
public class AnimatedImageSource implements ImageProducer {

    private static final Logger LOGGER = Logger.getLogger(AnimatedImageSource.class.getName());

    private static final int INACTIVITY_SECONDS = 5;

    private final Set<ImageConsumer> consumers = new HashSet<>();

    private final AnimatedImage image;
    private final int width;
    private final int height;
    private final ColorModel colorModel;
    private final static ScheduledThreadPoolExecutor timer =
            new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());

    private int[] pixels;
    private int currentFrame = -1;
    private ScheduledFuture<?> produceFrames;
    private ElapsedTime noConsumersTime;
    private boolean errorOccured;

    public AnimatedImageSource(AnimatedImage image) {
        this.image = image;
        Dimension size = image.getSize();
        width = size.width;
        height = size.height;
        colorModel = ColorModel.getRGBdefault();
//        timer = new Timer("AnimatedImage" + (image.getName() != null ?
//                                             "-" + image.getName() : ""));
    }

    //==========================
    // Interface
    //==========================
    @Override
    public void startProduction(ImageConsumer ic) {
        addConsumer(ic);
    }

    @Override
    public synchronized void addConsumer(ImageConsumer ic) {
        if (isConsumer(ic)) {
            return;
        }
        consumers.add(ic);
        initConsumer(ic);
        sendFrame(ic);
        startThread();
    }

    @Override
    public synchronized void removeConsumer(ImageConsumer ic) {
        consumers.remove(ic);
    }

    @Override
    public synchronized boolean isConsumer(ImageConsumer ic) {
        return consumers.contains(ic);
    }

    @Override
    public void requestTopDownLeftRightResend(ImageConsumer ic) {
        // Empty
    }

    //==========================
    // Frame updates
    //==========================

    private static volatile int ANIMATION_PAUSE = -1;
    private static volatile Set<AnimatedImageSource> paused =
            Collections.synchronizedSet(Collections.newSetFromMap(
                    new IdentityHashMap<>()));

    public static void setAnimationPause(int animationPause) {
        synchronized (paused) {
            ANIMATION_PAUSE = animationPause;
            if (ANIMATION_PAUSE == -1) {
                paused.forEach(AnimatedImageSource::startThread);
                paused.clear();
            }
        }
    }

    private void startThread() {
        if (errorOccured) {
            return;
        }
        if (produceFrames == null) {
            class ProduceFrames implements Runnable {

                @Override
                public void run() {
                    if (isActive()) {
                        nextFrame();
                        // Schedule another frame for production only if production shouldn't be
                        // stopped.
                        if (!checkStopThread()) {
                            produceFrames = timer.schedule(new ProduceFrames(), getDelay(),
                                                           TimeUnit.MILLISECONDS);
                        } else {
                            produceFrames = null;
                            pixels = null;
                        }
                    } else {
                        // Add this source to the paused sources so on change a restart is triggered
                        // and we don't have check periodically.
                        paused.add(AnimatedImageSource.this);
                        // There is no update necessary until ANIMATION_PAUSE is updated.
                        produceFrames = null;
                        // Animation is paused, switch frame if necessary.
                        int pauseFrame = 0;
                        switch (ANIMATION_PAUSE) {
                            case 0:
                                pauseFrame = 0;
                                break;
                            case 1:
                                pauseFrame = currentFrame;
                                break;
                            case 2:
                                pauseFrame = image.getPreferredPauseFrame();
                                break;
                        }

                        if (pauseFrame != currentFrame || !hasPixels()) {
                            currentFrame = pauseFrame - 1;
                            nextFrame();
                        }
                    }
                }

            }
            produceFrames = timer.schedule(new ProduceFrames(), 0, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized int getDelay() {
        return image.getDelay(currentFrame);
    }

    private synchronized boolean isActive() {
        return ANIMATION_PAUSE == -1;
    }

    private synchronized boolean hasPixels() {
        return pixels != null;
    }

    /**
     * Check if enough time has passed with no consumers registered for the thread to stop.
     *
     * @return
     */
    private synchronized boolean checkStopThread() {
        if (errorOccured) {
            return true;
        }
        if (!consumers.isEmpty()) {
            noConsumersTime = null;
            return false;
        }
        if (noConsumersTime == null) {
            noConsumersTime = new ElapsedTime(true);
        }
        return noConsumersTime.secondsElapsed(INACTIVITY_SECONDS);
    }

    /**
     * Load the next frame and send to all consumers.
     */
    private synchronized void nextFrame() {
        // Load new current frame
        currentFrame = (currentFrame + 1) % image.getFrameCount();
        try {
            pixels = image.getFrame(currentFrame);
        } catch (Exception ex) {
            /**
             * This usually shouldn't happen, since the image at this point
             * would already have been parsed and now just the cached frames
             * being retrieved, but just in case.
             */
            LOGGER.warning(String.format("Error getting %s frame: %s",
                                         image.getName(), ex));
            errorOccured = true;
        }

        // Send current frame to all
        for (ImageConsumer ic : consumers) {
            sendFrame(ic);
        }
    }

    /**
     * Send the currently loaded frame (if any) to the given consumer.
     *
     * @param ic
     */
    private void sendFrame(ImageConsumer ic) {
        if (pixels == null) {
            return;
        }
        if (isConsumer(ic)) {
            ic.setPixels(0, 0, width, height, colorModel, pixels, 0, width);
        }
        if (isConsumer(ic)) {
            if (image.getFrameCount() == 1) {
                /**
                 * This class is only for animated images, but just in case only
                 * one frame is available, this would probably cause the
                 * consumer to unregister themselves, which would then
                 * eventually cause the thread to stop.
                 */
                ic.imageComplete(ImageConsumer.STATICIMAGEDONE);
            } else {
                ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
            }
        }
    }

    //==========================
    // Other
    //==========================

    private void initConsumer(ImageConsumer ic) {
        if (isConsumer(ic)) {
            ic.setDimensions(width, height);
        }
        if (isConsumer(ic)) {
            ic.setColorModel(colorModel);
        }
        if (isConsumer(ic)) {
            if (image.getFrameCount() == 1) {
                ic.setHints(
                        ImageConsumer.TOPDOWNLEFTRIGHT
                        | ImageConsumer.COMPLETESCANLINES
                        | ImageConsumer.SINGLEPASS
                        | ImageConsumer.SINGLEFRAME);
            } else {
                ic.setHints(
                        ImageConsumer.TOPDOWNLEFTRIGHT
                        | ImageConsumer.COMPLETESCANLINES);
            }
        }
    }

}
