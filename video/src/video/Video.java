package video;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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

	boolean removeRed, removeGreen, removeBlue, showFiltered;
	int width, height, frameCount, currentFrame;
	double fps;

	BufferedImage showFrame;

	GridBagConstraints gbc = new GridBagConstraints();

	String basePath;
	String tempPath;

	JPanel visibleFrame;

	JSlider slider;
	JSlider gammaSlider;
	double gamma;
	JSlider sampleSlider;
	int sample;

	JButton fullResCache;
	
	Thread showFrameThread;

	public static void main(String[] args) {
		new Video();
	}

	Video() {

		basePath = new File("").getAbsolutePath();
		tempPath = basePath + "\\temp";

		slider = initSlider();
		gammaSlider = initGammaSlider();
		sampleSlider = initSampleSlider();

		UI();
	}

	public JSlider initSampleSlider() {
		JSlider newSlider = new JSlider(1, 50);

		newSlider.setValue(4);
		sample = 4;

		newSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				sample = newSlider.getValue();
				if (showFiltered) {
					showFrame(slider.getValue());
				}
			}
		});
		return newSlider;
	}

	public JSlider initGammaSlider() {
		JSlider newSlider = new JSlider(0, 1000);

		newSlider.setValue(100);
		gamma = 1;

		newSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				gamma = newSlider.getValue() * .01;
				if (showFiltered) {
					showFrame(slider.getValue());
				}
			}
		});

		return newSlider;
	}

	public JSlider initSlider() {
		JSlider newSlider = new JSlider();
		newSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				showFrame(newSlider.getValue());
			}
		});
		newSlider.setMinimum(0);
		newSlider.setMaximum(1);
		newSlider.setEnabled(false);
		newSlider.setMinorTickSpacing(1);
		newSlider.setMajorTickSpacing(10);
		newSlider.setPaintTicks(true);
		newSlider.setPaintLabels(true);
		return newSlider;
	}

	public void showFrame(int index) {
		if (showFrameThread != null && showFrameThread.isAlive()) {
			showFrameThread.interrupt();
		}
		showFrameThread = new Thread() {
			@Override
			public void run() {
				try {
					if (Thread.interrupted())
						return;
					if (showFiltered) {
						BufferedImage newFrame = processFrame(index);
						if (newFrame != null) {
							showFrame = newFrame;
						}

					} else {
						showFrame = ImageIO.read(new File(tempPath + "\\in-" + index + ".png"));
					}
					if (Thread.interrupted())
						return;
					if (visibleFrame != null) {
						visibleFrame.repaint();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		showFrameThread.start();
	}

	public void loadVideo(String filePath) {

		Thread thread = new Thread() {
			@Override
			public void run() {
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

				// set video pram
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
				slider.setEnabled(false);
				while (videoCapture.read(frame)) {

					Imgcodecs.imwrite(tempPath + "\\in-" + currentFrame + ".png", frame);
					System.out.println("Reading frame " + currentFrame + " of " + (frameCount - 1));

					slider.setMaximum(currentFrame);
					slider.setValue(currentFrame);
					currentFrame++;
				}
				slider.setEnabled(true);
				videoCapture.release();
				System.out.println("Done!");
			}
		};
		thread.start();

		// processVideo();
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
				ImageIO.write(process(frames.get(0), frames.get(1), 1, gamma), "png", outputFile);
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("Processing frame " + (i + 1) + " of " + (frameCount - 1));
		}
		System.out.println("Done!");

		saveVideo();
	}

	public BufferedImage processFrame(int index) {
		BufferedImage result = null;
		try {
			if (Thread.interrupted())
				return result;
			BufferedImage frame1 = ImageIO.read(new File(tempPath + "\\in-" + (index) + ".png"));
			if (Thread.interrupted())
				return result;
			BufferedImage frame2 = ImageIO.read(new File(tempPath + "\\in-" + (index + 1) + ".png"));
			if (Thread.interrupted())
				return result;
			result = process(frame1, frame2, sample, gamma);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
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

	public BufferedImage process(BufferedImage src1, BufferedImage src2, int sample, double gamma) {
		int width = src1.getWidth();
		int height = src1.getHeight();

		BufferedImage out = new BufferedImage(width / sample, height / sample, src1.getType());

		for (int y = 0; y < height / sample; y++) {
			for (int x = 0; x < width / sample; x++) {
				if (Thread.interrupted())
					return out;
				Color pixel1 = new Color(src1.getRGB(x * sample, y * sample));
				Color pixel2 = new Color(src2.getRGB(x * sample, y * sample));

				int r = Math.abs(pixel1.getRed() - pixel2.getRed());
				int g = Math.abs(pixel1.getGreen() - pixel2.getGreen());
				int b = Math.abs(pixel1.getBlue() - pixel2.getBlue());

				r = (int) (Math.pow(r / (double) 255, 1 / gamma) * 255);
				g = (int) (Math.pow(g / (double) 255, 1 / gamma) * 255);
				b = (int) (Math.pow(b / (double) 255, 1 / gamma) * 255);

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

	public Color difference(Color color) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();

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
		return new Color(r,g,b);
	}

	public void UI() {

		JFrame frame = new JFrame();
		frame.setSize(1000, 500);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setTitle("Image captured");
		frame.setLayout(new GridBagLayout());

		JButton choose = new JButton("Load Video");
		JButton button = new JButton("Motion Detect!");
		JCheckBox red = new JCheckBox("Red");
		JCheckBox green = new JCheckBox("Green");
		JCheckBox blue = new JCheckBox("Blue");
		JCheckBox viewFiltered = new JCheckBox("View Filtered");

		fullResCache = new JButton("Full Resolution Disk Cache");

		JLabel gammaText = new JLabel("Gamma");
		JLabel samplesText = new JLabel("Proxy");

		JPanel panel = new JPanel();
		panel.add(choose);
		panel.add(viewFiltered);

		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
		panel1.add(gammaText);
		panel1.add(gammaSlider);
		panel.add(panel1);

		JPanel panel2 = new JPanel();
		panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
		panel2.add(samplesText);
		panel2.add(sampleSlider);
		panel.add(panel2);
		
		JPanel panel3 = new JPanel();
		panel3.setLayout(new BoxLayout(panel3, BoxLayout.Y_AXIS));
		panel3.add(red);
		panel3.add(green);
		panel3.add(blue);
		panel.add(panel3);

		visibleFrame = new JPanel() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				draw(g, this);
			}
		};
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weighty = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		frame.add(panel, gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weighty = 1;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.BOTH;
		frame.add(visibleFrame, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weighty = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		frame.add(slider, gbc);
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.weighty = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		frame.add(fullResCache, gbc);

		fullResCache.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				processVideo();
			}

		});

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

		viewFiltered.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showFiltered = viewFiltered.isSelected();
				if (showFiltered) {
					if (slider.getValue() > frameCount - 2) {
						slider.setValue(frameCount - 2);
					}
					slider.setMaximum(frameCount - 2);
				} else {
					slider.setMaximum(frameCount - 1);
				}
				showFrame(slider.getValue());
			}
		});
		red.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeRed = red.isSelected();
			}
		});
		green.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeGreen = green.isSelected();
			}
		});

		blue.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeBlue = blue.isSelected();
			}
		});

		frame.setVisible(true);

	}

	void draw(Graphics g, JPanel panel) {

		g.fillRect(0, 0, panel.getSize().width, panel.getSize().height);

		int newWidth = 0;
		int newHeight = 0;

		if (((double) width) / height > ((double) panel.getSize().width) / panel.getSize().height) {
			newWidth = panel.getSize().width;
			double scale = ((double) width) / newWidth;
			newHeight = (int) (height / scale);
		} else {
			newHeight = panel.getSize().height;
			double scale = ((double) height) / newHeight;
			newWidth = (int) (width / scale);
		}
		int cx = panel.getSize().width / 2 - newWidth / 2;
		int cy = panel.getSize().height / 2 - newHeight / 2;
		g.drawImage(showFrame, cx, cy, newWidth, newHeight, panel); // see javadoc for more info on the parameters
	}

}