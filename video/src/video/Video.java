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
        System.out.println("Starting program --------------------------");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        String basePath = new File("").getAbsolutePath();

        String fileName = "1.avi";
        String filePath = (basePath + "\\" + fileName);
        System.out.println("Attempting to load: " + fileName);
        System.out.println("At file path: " + filePath);

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
        System.out.println("File read successful --------------------------");

        ArrayList<BufferedImage> sourceFrames = new ArrayList<>();
        int frameCount = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
        double fps = videoCapture.get(Videoio.CAP_PROP_FPS);
        int width = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        int currentFrame = 0;
        System.out.println("File info:");
        System.out.println("\twidth: " + width);
        System.out.println("\theight: " + height);
        System.out.println("\tfps: " + fps);
        System.out.println("\tnumber of frames: " + frameCount);

        // read in all frames
        System.out.println("Starting to read individual frames --------------------------");
        Mat frame = new Mat();
        while (videoCapture.read(frame)) {
            sourceFrames.add(matToBuf(frame));
            currentFrame++;
            System.out.println("Reading frame " + currentFrame + " of " + frameCount);
        }
        videoCapture.release();
        System.out.println("Done!");

        // process frames
        System.out.println("Starting to process frames --------------------------");
        ArrayList<BufferedImage> outputFrames = new ArrayList<>();
        for (int i = 0; i < sourceFrames.size() - 1; i++) {
            BufferedImage frame1 = sourceFrames.get(i);
            BufferedImage frame2 = sourceFrames.get(i + 1);

            outputFrames.add(subtract(frame1, frame2));

            System.out.println("Processing frame " + (i+1) + " of " + (sourceFrames.size() - 1));
        }
        System.out.println("Done!");

        // save video
        System.out.println("Saving video --------------------------");
        VideoWriter videoWriter = new VideoWriter("1output.avi", VideoWriter.fourcc('x', '2', '6', '4'), fps,
                new Size(width, height));

        Iterator<BufferedImage> iterator = outputFrames.iterator();
        while (iterator.hasNext()) {
            Mat mat = bufToMat(iterator.next());
            videoWriter.write(mat);
        }
        videoWriter.release();
        System.out.println("Done!");
    }

    public BufferedImage subtract(BufferedImage src1, BufferedImage src2) {
        int width = src1.getWidth();
        int height = src1.getHeight();

        BufferedImage out = new BufferedImage(width, height, src1.getType());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixel1 = new Color(src1.getRGB(x, y));
                Color pixel2 = new Color(src2.getRGB(x, y));

                int r = pixel1.getRed() - pixel2.getRed();
                int g = pixel1.getGreen() - pixel2.getGreen();
                int b = pixel1.getBlue() - pixel2.getBlue();

                Color pixelOut = new Color(clamp(r), clamp(g), clamp(b));

                out.setRGB(x, y, pixelOut.getRGB());
            }
        }
        return out;
    }

    public int clamp(int in) {
        in = in > 255 ? 255 : in;
        in = in < 0 ? 0 : in;
        return in;
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

    private static void showWindow(BufferedImage img) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new JLabel(new ImageIcon(img)));
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(img.getWidth() / 2, img.getHeight() / 2 + 30);
        frame.setTitle("Image captured");
        frame.setVisible(true);
    }

}