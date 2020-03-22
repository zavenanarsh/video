package video;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.opencv.core.*;
import org.opencv.videoio.*;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class Video {

    public static void main(String[] args) {
        new Video();
    }

    Video() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        String basePath = new File("").getAbsolutePath();

        String fileName = "1.avi";
        String filePath = (basePath + "\\" + fileName);

        System.out.println("file path: " + filePath);

        // check if file exists
        if (!Paths.get(filePath).toFile().exists()) {
            System.out.println(fileName + " does not exit!");
            return;
        }

        VideoCapture videoCapture = new VideoCapture(filePath);
        if (!videoCapture.isOpened()) {
            System.out.println("Error! file can't be opened!");
            return;
        }

        ArrayList<BufferedImage> frames = new ArrayList<>();
        int frameCount = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
        double fps = videoCapture.get(Videoio.CAP_PROP_FPS);
        int width = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        System.out.println("width: " + width);
        System.out.println("height: " + height);
        System.out.println("fps: " + fps);
        System.out.println("frameCount: " + frameCount);

        // read in all frames
        Mat frame = new Mat();
        while (videoCapture.read(frame)) {
            frames.add(matToBuf(frame));
        }
        videoCapture.release();

        // process frames

        // save video
        // FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(basePath + "\\video" +
        // ".mpeg", width, height);
        // recorder.setFrameRate(fps);
        // recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
        // recorder.setVideoBitrate(9000);
        // recorder.setFormat("mpeg");
        // recorder.setVideoQuality(0); // maximum quality
        // try {
        // recorder.start();
        // Java2DFrameConverter converter1 = new Java2DFrameConverter();
        // Iterator<BufferedImage> iterator = frames.iterator();
        // while (iterator.hasNext()) {
        // recorder.record(converter1.convert(iterator.next()));
        // }
        // recorder.stop();
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

        System.out.println(frames.get(0).getWidth());
        System.out.println(frames.get(0).getHeight());

        VideoWriter videoWriter = new VideoWriter("1output.avi", VideoWriter.fourcc('x', '2', '6', '4'), fps,
                new Size(width, height));

        Iterator<BufferedImage> iterator = frames.iterator();
        while (iterator.hasNext()) {
            Mat mat = bufToMat(iterator.next());
            videoWriter.write(mat);
        }
        videoWriter.release();
    }

    /**
     * https://riptutorial.com/opencv/example/21963/converting-an-mat-object-to-an-bufferedimage-object
     * 
     * @param in
     * @return
     */
    private BufferedImage matToBuf(Mat in) {
        if (!in.empty()) {
            int type = BufferedImage.TYPE_BYTE_GRAY;
            if (in.channels() > 1) {
                type = BufferedImage.TYPE_3BYTE_BGR;
            }
            int bufferSize = in.channels() * in.cols() * in.rows();
            byte[] b = new byte[bufferSize];
            in.get(0, 0, b); // get all the pixels
            BufferedImage out = new BufferedImage(in.cols(), in.rows(), type);
            final byte[] targetPixels = ((DataBufferByte) out.getRaster().getDataBuffer()).getData();
            System.arraycopy(b, 0, targetPixels, 0, b.length);
            return out;
        }

        return null;
    }

    /**
     * https://stackoverflow.com/questions/14958643/converting-bufferedimage-to-mat-in-opencv
     * 
     * @param in
     * @return
     */
    public static Mat bufToMat(BufferedImage in) {
        Mat out = new Mat(in.getHeight(), in.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) in.getRaster().getDataBuffer()).getData();
        out.put(0, 0, data);
        return out;
    }

    // private static void showWindow(BufferedImage img) {
    // JFrame frame = new JFrame();
    // frame.getContentPane().add(new JLabel(new ImageIcon(img)));
    // frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    // frame.setSize(img.getWidth() / 2, img.getHeight() / 2 + 30);
    // frame.setTitle("Image captured");
    // frame.setVisible(true);
    // }

}