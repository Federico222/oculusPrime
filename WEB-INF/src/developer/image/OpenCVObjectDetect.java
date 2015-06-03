package developer.image;


import oculusPrime.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;
import org.opencv.video.BackgroundSubtractorMOG2;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class OpenCVObjectDetect {

    private State state = null;
    private Settings settings = null;
    private Application app = null;
    public volatile boolean imageupdated = false;
    public volatile Mat detect = new Mat();
    private final long TIMEOUT = Util.ONE_HOUR;

    public static final String HUMAN = "human";

    HOGDescriptor hog;


    public OpenCVObjectDetect(Application a) {
        app = a;
        settings = Settings.getReference();
        state = State.getReference();
    }

    public void detectGo(final String mode) {
        if (state.exists(State.values.objectdetect)) {
            Util.log("error, object detect already running", this);
            return;
        }

        state.delete(State.values.streamactivity);
        state.set(State.values.objectdetect, mode);

        if (mode.equals(HUMAN)) {
            hog = new HOGDescriptor();
            hog.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
//            hog.setSVMDetector(HOGDescriptor.getDaimlerPeopleDetector());
        }


        new Thread(new Runnable() {
            public void run() {

                Util.log("objectdetect "+mode, this);

                Mat frame;
                int trigger = 0;
                long timeout = System.currentTimeMillis() + TIMEOUT;

                while (state.exists(State.values.objectdetect) && System.currentTimeMillis() < timeout) {

                    if (!state.exists(State.values.objectdetect)) {
                        Util.log("error, object detect timeout", this);
                        return;
                    }

                    boolean fg = app.frameGrab();
                    long waittime = System.currentTimeMillis() + 2000;
                    while (state.getBoolean(State.values.framegrabbusy) && System.currentTimeMillis() < waittime) {
                        Util.delay(1);
                        if (!state.exists(State.values.objectdetect)) return; // help reduce cpu quicker on shutdown
                    }
                    if (state.getBoolean(State.values.framegrabbusy) || !fg) {
                        app.driverCallServer(PlayerCommands.messageclients,
                                "OpenCVObjectDetect().detectGo() frame unavailable");
                        state.delete(State.values.objectdetect);
                        return;
                    }
                    BufferedImage img = ImageUtils.toBufferedImageOfType(app.processedImage, BufferedImage.TYPE_3BYTE_BGR);

                    frame = OpenCVUtils.bufferedImageToMat(img);
//                    Mat result = new Mat();
//                    frame.copyTo(result);

                    MatOfRect found = new MatOfRect();
                    MatOfDouble weight = new MatOfDouble();

//                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGRA2GRAY);

                    if (mode.equals(HUMAN))
                        hog.detectMultiScale(frame, found, weight, 0, new Size(8, 8), new Size(32, 32), 1.05, 2, false);

                    Rect [] rects = found.toArray();
                    if (rects.length > 0) { // maybe detection!

                        if (!state.exists(State.values.objectdetect)) break;

                        for (int i = 0; i < rects.length; i++) {
                            if (rects[i].x < 0 || rects[i].x + rects[i].width > frame.width()) {
                                continue;
                            }

                            if (trigger >= 2) {

                                Core.rectangle(frame, new Point(rects[i].x, rects[i].y),
                                        new Point(rects[i].x + rects[i].width, rects[i].y + rects[i].height),
                                        new Scalar(255, 0, 0, 255), 2);

                                Application.processedImage = OpenCVUtils.matToBufferedImage(frame);

                                state.set(State.values.streamactivity, mode);
                                if (app != null)
                                    app.driverCallServer(PlayerCommands.messageclients, mode + " detected");
                                break;

                            } else trigger++;

                        }
                    } else trigger = 0;

                    Util.delay(50); // cpu saver
                }

                state.delete(State.values.objectdetect);
                Util.log("objectdetect exit", this);
            }
        }).start();

    }


    private void humanDetectGoTest() {

        new Thread(new Runnable() { public void run() {

            int f = 0;
            hog = new HOGDescriptor();
            hog.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());

            String port = "5080";
            if (settings != null) {
                port = settings.readRed5Setting("http.port");
            }

            int trigger = 0;

            while (true) {

                if (state != null) {
                    if (!state.exists(State.values.objectdetect)) break;
                    if (!state.get(State.values.stream).equals(Application.streamstate.camera.toString()) &&
                            !state.get(State.values.stream).equals(Application.streamstate.camandmic.toString()) ) {
                        state.delete(State.values.objectdetect);
                        break;
                    }
                }

                BufferedImage img = null;

                try {
                    img = ImageIO.read(new URL("http://127.0.0.1:" + port + "/oculusPrime/frameGrabHTTP"));
                } catch (IOException e) {
                    e.printStackTrace();
                    state.delete(State.values.objectdetect);
                    break;
                }

                if (img == null) {
                    Util.log("stream unavailable", this);
                    state.delete(State.values.objectdetect);
                    break;
                }

                f++;
                if (f<=1)    continue;


                detect = OpenCVUtils.bufferedImageToMat(img);

                MatOfRect found = new MatOfRect();
                MatOfDouble weight = new MatOfDouble();

                hog.detectMultiScale(detect, found, weight, 0, new Size(8, 8), new Size(32, 32), 1.05, 2, false);

                Rect [] rects = found.toArray();
                if (rects.length > 0) {
                    for (int i=0; i<rects.length; i++) {
                        if (rects[i].x<0 || rects[i].x+rects[i].width > detect.width()) continue;

                        if (trigger >= 2) {

                            Util.debug("human detected");
                            Core.rectangle(detect, new Point(rects[i].x, rects[i].y),
                                    new Point(rects[i].x + rects[i].width, rects[i].y + rects[i].height),
                                    new Scalar(255, 0, 0, 255), 2);

                        } else trigger ++;
                    }
                } else trigger = 0;

                imageupdated = true;

                Util.debug("human detect frame: "+f);
                Util.delay(50);

            }

        }     }).start();

    }



    public void detectStream(String mode) {
        if (!state.get(State.values.stream).equals(Application.streamstate.camera.toString()) &&
                !state.get(State.values.stream).equals(Application.streamstate.camandmic.toString()) ) {
            app.message("object detect unavailable, camera not running", null, null);
            return;
        }

        state.set(State.values.objectdetect, mode);
        if (mode.equals(HUMAN))  humanDetectGoTest();

        new Thread(new Runnable() {
            public void run() {
                try {

                    while(state.exists(State.values.objectdetect)) {
                        if (imageupdated) {
                            app.videoOverlayImage = OpenCVUtils.matToBufferedImage(detect);
                            imageupdated = false;
                        }
                        Util.delay(10);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    state.delete(State.values.objectdetect);
                }
            }
        }).start();
    }
}
