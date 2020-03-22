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
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
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
            frames.add(mat2Img(frame));
        }
        videoCapture.release();

        // process frames
        VideoWriter videoWriter = new VideoWriter("1output.avi", -1, fps,
                new Size(width, height));

        Iterator<BufferedImage> iterator = frames.iterator();
        while (iterator.hasNext()) {
            Mat mat = img2Mat(iterator.next());
            videoWriter.write(mat);
        }
        videoWriter.release();
    }

    /**
     * https://www.codeproject.com/Tips/752511/How-to-Convert-Mat-to-BufferedImage-Vice-Versa
     * 
     * @param in
     * @return
     */
    public static BufferedImage mat2Img(Mat in) {
        BufferedImage out;
        byte[] data = new byte[320 * 240 * (int) in.elemSize()];
        int type;
        in.get(0, 0, data);

        if (in.channels() == 1)
            type = BufferedImage.TYPE_BYTE_GRAY;
        else
            type = BufferedImage.TYPE_3BYTE_BGR;

        out = new BufferedImage(320, 240, type);

        out.getRaster().setDataElements(0, 0, 320, 240, data);
        return out;
    }

    /**
     * https://www.codeproject.com/Tips/752511/How-to-Convert-Mat-to-BufferedImage-Vice-Versa
     * 
     * @param in
     * @return
     */
    public static Mat img2Mat(BufferedImage in) {
        Mat out;
        byte[] data;
        int r, g, b;

        if (in.getType() == BufferedImage.TYPE_INT_RGB) {
            out = new Mat(240, 320, CvType.CV_8UC3);
            data = new byte[320 * 240 * (int) out.elemSize()];
            int[] dataBuff = in.getRGB(0, 0, 320, 240, null, 0, 320);
            for (int i = 0; i < dataBuff.length; i++) {
                data[i * 3] = (byte) ((dataBuff[i] >> 16) & 0xFF);
                data[i * 3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
                data[i * 3 + 2] = (byte) ((dataBuff[i] >> 0) & 0xFF);
            }
        } else {
            out = new Mat(240, 320, CvType.CV_8UC1);
            data = new byte[320 * 240 * (int) out.elemSize()];
            int[] dataBuff = in.getRGB(0, 0, 320, 240, null, 0, 320);
            for (int i = 0; i < dataBuff.length; i++) {
                r = (byte) ((dataBuff[i] >> 16) & 0xFF);
                g = (byte) ((dataBuff[i] >> 8) & 0xFF);
                b = (byte) ((dataBuff[i] >> 0) & 0xFF);
                data[i] = (byte) ((0.21 * r) + (0.71 * g) + (0.07 * b)); // luminosity
            }
        }
        out.put(0, 0, data);
        return out;
    }

    // // reading the frames
    // System.out.println(video_length + " Frames extracted");

    // BufferedImage bufferedImage = matToBufferedImage(frame);
    // showWindow(bufferedImage);
    // camera.release();

    // // read the previously extracted frames that were saved and load it in the
    // array
    // BufferedImage[] bImg = new BufferedImage[video_length];
    // for (int i = 0; i < video_length - 1; i++) {
    // try {
    // bImg[i] = ImageIO.read(new File(basePath + "\\output\\" + i + ".jpg"));

    // } catch (Exception e) {
    // System.out.println("Cannot load the provided image");
    // }
    // }

    // // save file as image
    // File outputfile;
    // for (int x = 0; x < bImg.length - 1; x++) {
    // outputfile = new File(basePath + "\\output\\" + x + "s.png");
    // // use subtract method for images
    // try {
    // ImageIO.write(bImg[x], "png", outputfile);
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }

    // }

    // // method to save images as movie
    // convertJPGtoMovie(bImg, basePath + "\\output\\");

    // }

    // public BufferedImage subtract(int height, int width, int imageType, int
    // length, BufferedImage[] src, int x) {
    // BufferedImage result = new BufferedImage(width, height, imageType);

    // for (int i = 0; i < width; i++) {
    // for (int j = 0; j < height; j++) {

    // int red = getRed(src[x].getRGB(i, j)) - getRed(src[x].getRGB(i, j));
    // int green = getGreen(src[x].getRGB(i, j)) - getGreen(src[x].getRGB(i, j));
    // int blue = getBlue(src[x].getRGB(i, j)) - getBlue(src[x].getRGB(i, j));
    // result.setRGB(i, j, new Color(red, green, blue).getRGB());

    // }
    // }

    // return result;
    // }

    // //
    // protected int getRed(int pixel) {
    // return (pixel >>> 16) & 0xFF;
    // }

    // protected int getGreen(int pixel) {
    // return (pixel >>> 8) & 0xFF;
    // }

    // protected int getBlue(int pixel) {
    // return pixel & 0xFF;
    // }

    // public static void convertJPGtoMovie(BufferedImage[] links, String vidPath) {

    // FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(vidPath + "video" +
    // ".mpeg", 640, 720);
    // try {
    // recorder.setFrameRate(1);
    // recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
    // recorder.setVideoBitrate(9000);
    // recorder.setFormat("mpeg");
    // recorder.setVideoQuality(0); // maximum quality
    // recorder.start();
    // for (int i = 0; i < links.length; i++) {
    // Java2DFrameConverter converter1 = new Java2DFrameConverter();

    // recorder.record(converter1.convert(links[i]));
    // }
    // recorder.stop();
    // } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
    // e.printStackTrace();
    // }

    // }

    // private static BufferedImage matToBufferedImage(Mat frame) {
    // int type = 0;
    // if (frame.channels() == 1) {
    // type = BufferedImage.TYPE_BYTE_GRAY;
    // } else if (frame.channels() == 3) {
    // type = BufferedImage.TYPE_3BYTE_BGR;
    // }
    // BufferedImage image = new BufferedImage(frame.width(), frame.height(), type);
    // WritableRaster raster = image.getRaster();
    // DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
    // byte[] data = dataBuffer.getData();
    // frame.get(0, 0, data);

    // return image;
    // }

    // private static void showWindow(BufferedImage img) {
    // JFrame frame = new JFrame();
    // frame.getContentPane().add(new JLabel(new ImageIcon(img)));
    // frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    // frame.setSize(img.getWidth() / 2, img.getHeight() / 2 + 30);
    // frame.setTitle("Image captured");
    // frame.setVisible(true);
    // }

}