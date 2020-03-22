package video;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class Video {

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        String basePath = new File("").getAbsolutePath();
        System.out.println("basePath " + basePath);

        String filePath = "\\1.avi";
        String newPath = (basePath + filePath);
        String output = (basePath + "\\output\\output.mp4");
        String finalOutput = (basePath + "\\output");
        double FRAME_RATE = 50;
        int SECONDS_TO_RUN_FOR = 20;

        System.out.println("newPath " + newPath);

        if (!Paths.get(newPath).toFile().exists()) {
            System.out.println("File " + filePath + " does not exist!");
            return;
        }

        VideoCapture camera = new VideoCapture(newPath);

        if (!camera.isOpened()) {
            System.out.println("Error! Camera can't be opened!");
            return;
        }
        Mat frame = new Mat();

        int video_length = (int) camera.get(Videoio.CAP_PROP_FRAME_COUNT);
        int frame_number = (int) camera.get(Videoio.CAP_PROP_POS_FRAMES);

        // save frame as jpg
        while (camera.read(frame)) {
            Imgcodecs.imwrite(basePath + "\\output\\" + frame_number + ".jpg", frame);
            frame_number++;
            if (frame_number == video_length - 1) {
                break;
            }
        }

        // reading the frames
        System.out.println(video_length + " Frames extracted");

        BufferedImage bufferedImage = matToBufferedImage(frame);
        showWindow(bufferedImage);
        camera.release();

        // read the previously extracted frames that were saved and load it in the array
        BufferedImage[] bImg = new BufferedImage[video_length];
        for (int i = 0; i < video_length - 1; i++) {
            try {
                bImg[i] = ImageIO.read(new File(basePath + "\\output\\" + i + ".jpg"));

            } catch (Exception e) {
                System.out.println("Cannot load the provided image");
            }
        }

        // save file as image
        // File outputfile;
        // for (int x = 0; x<bImg.length-1;x++) {
        // outputfile = new File(basePath +"\\output\\"+x+"s.png");
        // //use subtract method for images
        // ImageIO.write(subtract(bImg[1].getHeight(), bImg[1].getWidth(),
        // bImg[1].getType(), bImg.length, bImg, x)
        // , "png", outputfile);
        //
        // }
        //
        //
        // //method to save images as movie
        // convertJPGtoMovie(bImg, basePath +"\\output\\");
        //

    }

    public BufferedImage subtract(int height, int width, int imageType, int length, BufferedImage[] src, int x) {
        BufferedImage result = new BufferedImage(width, height, imageType);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {

                int red = getRed(src[x].getRGB(i, j)) - getRed(src[x].getRGB(i, j));
                int green = getGreen(src[x].getRGB(i, j)) - getGreen(src[x].getRGB(i, j));
                int blue = getBlue(src[x].getRGB(i, j)) - getBlue(src[x].getRGB(i, j));
                result.setRGB(i, j, new Color(red, green, blue).getRGB());

            }
        }

        return result;
    }

    //
    protected int getRed(int pixel) {
        return (pixel >>> 16) & 0xFF;
    }

    protected int getGreen(int pixel) {
        return (pixel >>> 8) & 0xFF;
    }

    protected int getBlue(int pixel) {
        return pixel & 0xFF;
    }

    public static void convertJPGtoMovie(BufferedImage[] links, String vidPath) {

        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(vidPath + "video" + ".mpeg", 640, 720);
        try {
            recorder.setFrameRate(1);
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
            recorder.setVideoBitrate(9000);
            recorder.setFormat("mpeg");
            recorder.setVideoQuality(0); // maximum quality
            recorder.start();
            for (int i = 0; i < links.length; i++) {
                Java2DFrameConverter converter1 = new Java2DFrameConverter();

                recorder.record(converter1.convert(links[i]));
            }
            recorder.stop();
        } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
            e.printStackTrace();
        }

    }

    private static BufferedImage matToBufferedImage(Mat frame) {
        int type = 0;
        if (frame.channels() == 1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        } else if (frame.channels() == 3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(frame.width(), frame.height(), type);
        WritableRaster raster = image.getRaster();
        DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
        byte[] data = dataBuffer.getData();
        frame.get(0, 0, data);

        return image;
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