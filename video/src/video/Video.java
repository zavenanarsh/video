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

	boolean removeRed, removeGreen, useAsMask, showFiltered;
	int width, height, frameCount, currentFrame;
	double fps;

	boolean diskCacheVideo = false;

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

	JSlider multiFrameSlider;
	int stackFrames;
	JSlider diffFrameSlider;
	int diffFrames;

	JButton loadVideoButton = new JButton("Load Video");
	JCheckBox red = new JCheckBox("Red");
	JCheckBox green = new JCheckBox("Green");
	JCheckBox useAsMaskCheck = new JCheckBox("Use As Mask");
	JCheckBox viewFiltered = new JCheckBox("View Filtered");
	JButton fullResCache = new JButton("Full Resolution Disk Cache");;
	JButton saveButton = new JButton("Save");

	Thread showFrameThread;

	int cores;

	public static void main(String[] args) {
		new Video();
	}

	Video() {

		basePath = new File("").getAbsolutePath();
		tempPath = basePath + "\\temp";

		slider = initSlider();
		gammaSlider = initGammaSlider();
		sampleSlider = initSampleSlider();
		multiFrameSlider = initMultiFrameSlider();
		diffFrameSlider = initDiffFrameSlider();

		red.setEnabled(false);
		green.setEnabled(false);
		useAsMaskCheck.setEnabled(false);
		viewFiltered.setEnabled(false);
		slider.setEnabled(false);
		gammaSlider.setEnabled(false);
		sampleSlider.setEnabled(false);
		fullResCache.setEnabled(false);
		saveButton.setEnabled(false);
		multiFrameSlider.setEnabled(false);
		diffFrameSlider.setEnabled(false);

		cores = Runtime.getRuntime().availableProcessors();

		UI();
	}

	public void doneLoadingVideo() {
		loadVideoButton.setEnabled(false);
		loadVideoButton.setText("Fully Loaded");
		setViewFiltered(true);

		saveButton.setEnabled(false);
		red.setEnabled(true);
		green.setEnabled(true);
		useAsMaskCheck.setEnabled(true);
		viewFiltered.setEnabled(true);
		slider.setEnabled(true);
		gammaSlider.setEnabled(true);
		sampleSlider.setEnabled(true);
		fullResCache.setEnabled(true);
		multiFrameSlider.setEnabled(true);
		diffFrameSlider.setEnabled(true);
	}

	public void diskCaching() {
		saveButton.setEnabled(false);
		red.setEnabled(false);
		green.setEnabled(false);
		useAsMaskCheck.setEnabled(false);
		viewFiltered.setEnabled(false);
		slider.setEnabled(false);
		gammaSlider.setEnabled(false);
		sampleSlider.setEnabled(false);
		fullResCache.setEnabled(false);
		multiFrameSlider.setEnabled(false);
		diffFrameSlider.setEnabled(false);
		fullResCache.setText("Processing...");
	}

	public void doneDiskCaching() {
		saveButton.setEnabled(true);
		slider.setEnabled(true);
		fullResCache.setText("Done Full Res Disk Cache");
	}

	public void saving() {
		saveButton.setEnabled(false);
		saveButton.setText("Saving...");
	}

	public int numberOfUsableFrames() {
		return frameCount - stackFrames;
	}

	public JSlider initMultiFrameSlider() {
		JSlider newSlider = new JSlider(2, 20);

		newSlider.setValue(2);
		stackFrames = 2;

		newSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				stackFrames = newSlider.getValue();
				diffFrameSlider.setMaximum(stackFrames - 1);
				if (diffFrameSlider.getValue() > diffFrameSlider.getMaximum()) {
					diffFrameSlider.setValue(diffFrameSlider.getMaximum());
				}
				if (showFiltered) {
					int usableFrames = numberOfUsableFrames();
					slider.setMaximum(usableFrames);
					if (slider.getValue() > usableFrames) {
						slider.setValue(usableFrames);
					}
					showFrame(slider.getValue());
				}
			}
		});
		return newSlider;
	}

	public JSlider initDiffFrameSlider() {
		JSlider newSlider = new JSlider(1, stackFrames - 1);

		newSlider.setValue(1);
		diffFrames = 1;

		newSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				diffFrames = newSlider.getValue();
				if (showFiltered) {
					showFrame(slider.getValue());
				}
			}
		});
		return newSlider;
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

					if (diskCacheVideo) {
						showFrame = ImageIO.read(new File(tempPath + "\\out-" + index + ".png"));
					} else if (showFiltered) {
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
		loadVideoButton.setEnabled(false);
		loadVideoButton.setText("Loading...");

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

				slider.setMaximum(frameCount - 1);

				// read in all frames
				Mat frame = new Mat();
				while (videoCapture.read(frame)) {

					Imgcodecs.imwrite(tempPath + "\\in-" + currentFrame + ".png", frame);
					System.out.println("Reading frame " + currentFrame + " of " + (frameCount - 1));

					slider.setValue(currentFrame);
					currentFrame++;
				}
				doneLoadingVideo();
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
		diskCacheVideo = true;
		diskCaching();
		slider.setMaximum(numberOfUsableFrames());

		Thread thread = new Thread() {

			@Override
			public void run() {
				try {
					for (int i = 0; i < stackFrames - 1; i++) {
						frames.add(ImageIO.read(new File(tempPath + "\\in-" + i + ".png")));
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("Starting to process frames --------------------------");
				for (int i = stackFrames - 1; i < frameCount; i++) {
					int currentFrameIndex = i - stackFrames + 1;
					try {

						frames.add(ImageIO.read(new File(tempPath + "\\in-" + i + ".png")));

						while (frames.size() > stackFrames) {
							frames.remove(0);
						}

						File outputFile = new File(tempPath + "\\out-" + currentFrameIndex + ".png");
						ImageIO.write(process(frames, 1, gamma), "png", outputFile);

						slider.setValue(currentFrameIndex);
					} catch (IOException e) {
						e.printStackTrace();
					}

					System.out.println("Processing frame " + currentFrameIndex + " of " + numberOfUsableFrames());
				}
				System.out.println("Done!");
				doneDiskCaching();
			}
		};
		thread.start();
	}

	public BufferedImage processFrame(int index) {
		BufferedImage result = null;
		try {
			if (Thread.interrupted())
				return result;
			ArrayList<BufferedImage> src = new ArrayList<>();
			for (int i = 0; i < stackFrames; i++) {
				BufferedImage frame = ImageIO.read(new File(tempPath + "\\in-" + (index + i) + ".png"));
				src.add(frame);
				if (Thread.interrupted())
					return result;
			}
			result = process(src, sample, gamma);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public void saveVideo(String path) {
		// save video
		saving();
		Thread thread = new Thread() {
			@Override
			public void run() {
				System.out.println("Saving video --------------------------");
				VideoWriter videoWriter = new VideoWriter(path + "\\output.avi", VideoWriter.fourcc('x', '2', '6', '4'),
						fps, new Size(width, height));

				for (int i = 0; i < numberOfUsableFrames(); i++) {
					try {
						Mat mat = Imgcodecs.imread(tempPath + "\\out-" + i + ".png");
						videoWriter.write(mat);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				videoWriter.release();
				System.out.println("Done!");
				saveButton.setText("File at: " + path + "\\output.avi");
			}
		};
		thread.start();
	}

	public BufferedImage process(ArrayList<BufferedImage> src, int sample, double gamma) {

		BufferedImage out = new BufferedImage(width / sample, height / sample, src.get(0).getType());
		ArrayList<Thread> threads = new ArrayList<>();
		for (int i = 0; i < cores; i++) {
			final int proxyHeight = height / sample;
			final int rowHeight = proxyHeight / cores;
			final int index = i;
			Thread thread = new Thread() {
				@Override
				public void run() {

					int yMin = rowHeight * index;
					int yMax = rowHeight * (index + 1);
					if (index == cores - 1) {
						yMax = proxyHeight;
					}

					for (int y = yMin; y < yMax; y++) {
						for (int x = 0; x < width / sample; x++) {
							if (Thread.interrupted())
								return;

							// temporal averaging
							int r1, g1, b1, r2, g2, b2;
							r1 = g1 = b1 = r2 = g2 = b2 = 0;
							for (int i = 0; i < src.size() - diffFrames; i++) {
								Color pixel = new Color(src.get(i).getRGB(x * sample, y * sample));
								r1 += pixel.getRed();
								g1 += pixel.getGreen();
								b1 += pixel.getBlue();
							}
							for (int i = diffFrames; i < src.size(); i++) {
								Color pixel = new Color(src.get(i).getRGB(x * sample, y * sample));
								r2 += pixel.getRed();
								g2 += pixel.getGreen();
								b2 += pixel.getBlue();
							}
							r1 /= src.size() - diffFrames;
							g1 /= src.size() - diffFrames;
							b1 /= src.size() - diffFrames;
							r2 /= src.size() - diffFrames;
							g2 /= src.size() - diffFrames;
							b2 /= src.size() - diffFrames;

							int r = Math.abs(r1 - r2);
							int g = Math.abs(g1 - g2);
							int b = Math.abs(b1 - b2);

							r = (int) (Math.pow(r / 255.0, 1 / gamma) * 255);
							g = (int) (Math.pow(g / 255.0, 1 / gamma) * 255);
							b = (int) (Math.pow(b / 255.0, 1 / gamma) * 255);

							if (useAsMask) {
								Color pixel = new Color(src.get(src.size() / 2).getRGB(x * sample, y * sample));
								r = (int) ((r / 255.0) * pixel.getRed());
								g = (int) ((g / 255.0) * pixel.getGreen());
								b = (int) ((b / 255.0) * pixel.getBlue());
							}

							Color pixelOut = new Color(clamp(r), clamp(g), clamp(b));

							out.setRGB(x, y, pixelOut.getRGB());
						}
					}
				}
			};
			threads.add(thread);
			thread.start();
		}

		// wait for threads to finish
		boolean allAlive = true;
		while (allAlive) {
			boolean allDead = true;
			for (int i = 0; i < threads.size(); i++) {
				if (threads.get(i).isAlive()) {
					allDead = false;
				}
			}
			if (allDead) {
				allAlive = false;
			}
		}
		return out;
	}

	public int clamp(int in) {
		in = in > 255 ? 255 : in;
		in = in < 0 ? 0 : in;
		return in;
	}

	public void UI() {

		JFrame frame = new JFrame();
		frame.setSize(1000, 500);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setTitle("Image captured");
		frame.setLayout(new GridBagLayout());

		JLabel gammaText = new JLabel("Gamma");
		JLabel samplesText = new JLabel("Proxy");

		JLabel multiFrameText = new JLabel("Frames");
		JLabel deltaText = new JLabel("Delta");

		JPanel panel = new JPanel();
		panel.add(loadVideoButton);
		panel.add(viewFiltered);

		JPanel panel1;
		JPanel panel2 = new JPanel();
		panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));

		panel1 = new JPanel();
		panel1.add(gammaText);
		panel1.add(gammaSlider);
		panel2.add(panel1);

		panel1 = new JPanel();
		panel1.add(samplesText);
		panel1.add(sampleSlider);
		panel2.add(panel1);

		panel1 = new JPanel();
		panel1.add(multiFrameText);
		panel1.add(multiFrameSlider);
		panel2.add(panel1);

		panel1 = new JPanel();
		panel1.add(deltaText);
		panel1.add(diffFrameSlider);
		panel2.add(panel1);

		panel.add(panel2);

		panel.add(useAsMaskCheck);

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
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.weighty = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		frame.add(saveButton, gbc);

		fullResCache.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				processVideo();
			}

		});

		loadVideoButton.addActionListener(new ActionListener() {
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
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				int returnValue = jfc.showOpenDialog(null);
				// int returnValue = jfc.showSaveDialog(null);

				if (returnValue == JFileChooser.APPROVE_OPTION) {
					File selectedFile = jfc.getSelectedFile();
					loadVideoButton.setEnabled(false);
					saveVideo(selectedFile.getAbsolutePath());
				}
			}
		});

		viewFiltered.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setViewFiltered(viewFiltered.isSelected());
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

		useAsMaskCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				useAsMask = useAsMaskCheck.isSelected();
				showFrame(slider.getValue());
			}
		});

		frame.setVisible(true);

	}

	void setViewFiltered(boolean value) {
		showFiltered = value;
		viewFiltered.setSelected(value);
		if (showFiltered) {
			int usableFrames = numberOfUsableFrames();
			if (slider.getValue() > usableFrames) {
				slider.setValue(usableFrames);
			}
			slider.setMaximum(usableFrames);
		} else {
			slider.setMaximum(frameCount - 1);
		}
		showFrame(slider.getValue());
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