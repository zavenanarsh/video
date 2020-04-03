package video;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

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

	boolean removeRed, removeGreen, removeBlue;
	int width, height, frameCount, currentFrame;
	double fps;

	BufferedImage showFrame;

	JPanel visibleFrame;

	String basePath;
	String tempPath;

	public static void main(String[] args) {
		new Video();
	}

	Video() {

		basePath = new File("").getAbsolutePath();
		tempPath = basePath + "\\temp";

		UI();
	}

	public void loadVideo(String filePath) {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// check if file exists
		if (!Paths.get(filePath).toFile().exists()) {
			System.out.println(filePath + " does not exit!");
			return;
		}

		VideoCapture videoCapture = new VideoCapture(filePath);
		if (!videoCapture.isOpened()) {
			System.out.println("Error! file can't be opened!");
			return;
		}

		frameCount = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
		fps = videoCapture.get(Videoio.CAP_PROP_FPS);
		width = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH);
		height = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
		currentFrame = 0;

		// make output folder if does not exist
		File dir = new File(tempPath);
		if (!dir.exists()) {
			dir.mkdir();
		}

		// read in all frames
		Mat frame = new Mat();
		while (videoCapture.read(frame)) {
			
			Imgcodecs.imwrite(tempPath + "\\in-" + currentFrame + ".png", frame);
			
			System.out.println("Reading frame " + currentFrame + " of " + (frameCount-1));
			currentFrame++;
		}
		videoCapture.release();
		System.out.println("Done!");

		processVideo();
	}

	public void processVideo() {
		// process frames

		ArrayList<BufferedImage> frames = new ArrayList<>();
		
		try {
			frames.add(ImageIO.read(new File(tempPath + "\\in-0.png")));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Starting to process frames --------------------------");
		for (int i = 0; i < frameCount - 1; i++) {
			try {
				frames.add(ImageIO.read(new File(tempPath + "\\in-" + (i + 1) + ".png")));

				while (frames.size() > 2) {
					frames.remove(0);
				}

				File outputFile = new File(tempPath + "\\out-" + i + ".png");
				ImageIO.write(subtract(frames.get(0), frames.get(1)), "png", outputFile);
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("Processing frame " + (i + 1) + " of " + (frameCount - 1));
		}
		System.out.println("Done!");

		saveVideo();
	}

	public void saveVideo() {
		// save video
		System.out.println("Saving video --------------------------");
		VideoWriter videoWriter = new VideoWriter("output.avi", VideoWriter.fourcc('x', '2', '6', '4'), fps,
				new Size(width, height));

		for (int i = 0; i < frameCount - 1; i++) {
			try {
				Mat mat = Imgcodecs.imread(tempPath + "\\out-" + i + ".png");
				videoWriter.write(mat);

			} catch (Exception e) {
				e.printStackTrace();
			}
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

	public void difference(int r, int g, int b) {
		if (removeRed == true) {
			if (r > g) {
				r = g;
			}
		}

		if (removeGreen == true) {
			if (g > r) {
				g = r;
			}
		}

		if (removeBlue == true) {
			if (b > g) {
				b = g;
			}
		}
	}

	public void UI() {

		JFrame frame = new JFrame();
		frame.setSize(1000, 500);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setTitle("Image captured");
		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

		JButton choose = new JButton("Choose Video");
		JButton button = new JButton("Motion Detect!");
		JCheckBox red = new JCheckBox("Red");
		JCheckBox green = new JCheckBox("Green");
		JCheckBox blue = new JCheckBox("Blue");
		JLabel difference = new JLabel("Select a colour to use the difference colour method.");

		button.setPreferredSize(new Dimension(150, 50));
		choose.setPreferredSize(new Dimension(150, 50));
		red.setPreferredSize(new Dimension(50, 50));
		green.setPreferredSize(new Dimension(70, 50));
		blue.setPreferredSize(new Dimension(50, 50));
		difference.setPreferredSize(new Dimension(300, 50));

		JPanel panel = new JPanel();
		visibleFrame = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(showFrame, 0, 0, this); // see javadoc for more info on the parameters
			}
		};

		panel.add(choose);
		// panel.add(difference);
		// panel.add(red);
		// panel.add(green);
		// panel.add(blue);
		// panel.add(button);

		frame.add(panel);
		frame.add(visibleFrame);

		choose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// FileChooser

				JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

				int returnValue = jfc.showOpenDialog(null);
				// int returnValue = jfc.showSaveDialog(null);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					File selectedFile = jfc.getSelectedFile();
					loadVideo(selectedFile.getAbsolutePath());
				}
			}
		});
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// display/center the jdialog when the button is pressed
				JDialog d = new JDialog(frame, "Starting...", true);
				d.setLocationRelativeTo(frame);
				d.setVisible(true);
				// close JFrame
				frame.dispose();
			}
		});

		removeRed = false;
		removeGreen = false;
		removeBlue = false;

		red.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeRed = true;

			}
		});
		green.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeGreen = true;

			}
		});

		blue.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeBlue = true;

			}
		});

		frame.setVisible(true);

	}

}