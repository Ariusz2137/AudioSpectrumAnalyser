package audioSpectrumAnalyser;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;

public class VisualiserCurve extends JPanel implements Visualiser {
	private BufferedImage legendImage;
	
	private int sampleRate;
	private int buffSize;
	private int sensivity;
	
	private int fftResSize;
	
	private int minFreq = 20;
	private int maxFreq = 22000;
	
	private float[] fftRes;
	
	private int[] freqAtPx; //stores corresponding frequency for each column
	private ArrayList<ArrayList<Integer>> binsAtPx; //stores ArrayList of corresponding fft bins for each pixel
	private LinkedHashMap<Integer, Integer> interpolationData;
	private float[] curveData; //stores interpolated array of size same as panel width
	private SgFilter sgf;
	private float[] x;
	
	private boolean mouseOver;
	private int mouseX;
	
	private JLayeredPane layeredPane;
	private JPanel settingsPanel;
	private JPanel visPanel;
	
	private boolean fill = true;
	private boolean smoothing = true;
	private int smoothingWindowSize = 15;
	private int smoothingPolynominal = 2;
	
	private JCheckBox fillCheckBox;
	private JCheckBox smoothingCheckBox;
	private JSpinner windowSizeSpinner;
	private JSpinner polynominalSpinner;
	private JToggleButton settingsButton;
	
	private boolean isError;
	private String errorMsg;
	VisualiserEventListener el;

	public VisualiserCurve() {
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				calculateFreqAtPx();
				calculateBinsAtPx();
				calculateLayeredPaneParams();
				revalidate();
			}	
		});
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				mouseOver = true;
			}
			public void mouseExited(MouseEvent e) {
				mouseOver = false;
				mouseX = -1;
			}
		});
		this.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if(mouseOver) {
					mouseX = e.getX();
				}
			}
		});
		sgf = new SgFilter(15, 0, 3);
		isError = false;
		
		visPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {	
				super.paintComponent(g);
				Graphics2D graphics2d = (Graphics2D)g;
				if(isError) {
					if(errorMsg == null) {
						g.drawString("unknown error", 0, getHeight()/2);
						return;
					}
					g.drawString(errorMsg, 0, getHeight()/2);
					return;
				}
				
				graphics2d.drawImage(legendImage, 0, 0, null);
				
				if(fftRes != null && getHeight() != 0)
					graphics2d.drawImage(createVisualisationImage(), 0, 0, null);
				
				if(!settingsButton.isSelected()) {
					FontMetrics fm = graphics2d.getFontMetrics();
					if(mouseX > -1 && freqAtPx != null) {
						graphics2d.drawLine(mouseX, 0, mouseX, getHeight());
						String toDraw = Integer.toString(freqAtPx[mouseX])+"Hz";
						int strWidth = fm.stringWidth(toDraw);
						if(strWidth < getWidth() - mouseX)
							graphics2d.drawString(toDraw, mouseX + 2, 10);
						else
							graphics2d.drawString(toDraw, mouseX - 2 - strWidth, 10);
					}
				}
			}
		};
		this.setLayout(new BorderLayout());
		settingsPanel = new JPanel();
		settingsPanel.setBackground(new Color(70, 73, 75, 220));
		
		GridBagLayout visGridBagLayout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		settingsPanel.setLayout(visGridBagLayout);
	
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		
		JLabel fillLabel = new JLabel("Fill: ");
		visGridBagLayout.setConstraints(fillLabel, c);
		settingsPanel.add(fillLabel);
		
		fillCheckBox = new JCheckBox();
		fillCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				el.settingsFieldChanged("fill", Boolean.toString(fillCheckBox.isSelected()));
			}
		});
		c.gridx = 1;
		visGridBagLayout.setConstraints(fillCheckBox, c);
		settingsPanel.add(fillCheckBox);
		
		JLabel smoothingLabel = new JLabel("Smoothing: ");
		c.gridx = 0;
		c.gridy = 1;
		visGridBagLayout.setConstraints(smoothingLabel, c);
		settingsPanel.add(smoothingLabel);
		
		smoothingCheckBox = new JCheckBox();
		smoothingCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				el.settingsFieldChanged("smoothing", Boolean.toString(smoothingCheckBox.isSelected()));
				
			}
		});
		c.gridx = 1;
		visGridBagLayout.setConstraints(smoothingCheckBox, c);
		settingsPanel.add(smoothingCheckBox);
		
		JLabel windowSizeLabel = new JLabel("Smoothing window size: ");
		c.gridx = 0;
		c.gridy = 2;
		visGridBagLayout.setConstraints(windowSizeLabel, c);
		settingsPanel.add(windowSizeLabel);
		
		windowSizeSpinner = new JSpinner(new SpinnerNumberModel(15, 3, 999, 2));
		JComponent comp = windowSizeSpinner.getEditor();
	    JFormattedTextField field = (JFormattedTextField) comp.getComponent(0);
	    DefaultFormatter formatter = (DefaultFormatter) field.getFormatter();
	    formatter.setCommitsOnValidEdit(true);
	    windowSizeSpinner.addChangeListener(new ChangeListener() {
	        @Override
	        public void stateChanged(ChangeEvent e) {
	        	sgf = new SgFilter((int)windowSizeSpinner.getValue(), 0, (int)polynominalSpinner.getValue());
	        	el.settingsFieldChanged("windowSize", Integer.toString((int)windowSizeSpinner.getValue()));
	        }
	    });
		c.gridx = 1;
		visGridBagLayout.setConstraints(windowSizeSpinner, c);
		settingsPanel.add(windowSizeSpinner);
		
		JLabel polynominalLabel = new JLabel("Smoothing polynominal: ");
		c.gridx = 0;
		c.gridy = 3;
		visGridBagLayout.setConstraints(polynominalLabel, c);
		settingsPanel.add(polynominalLabel);
		
		polynominalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
		JComponent comp2 = polynominalSpinner.getEditor();
	    JFormattedTextField field2 = (JFormattedTextField) comp2.getComponent(0);
	    DefaultFormatter formatter2 = (DefaultFormatter) field2.getFormatter();
	    formatter2.setCommitsOnValidEdit(true);
	    polynominalSpinner.addChangeListener(new ChangeListener() {
	        @Override
	        public void stateChanged(ChangeEvent e) {
	            sgf = new SgFilter((int)windowSizeSpinner.getValue(), 0, (int)polynominalSpinner.getValue());
	            el.settingsFieldChanged("polynominal", Integer.toString((int)polynominalSpinner.getValue()));
	        }
	    });
		c.gridx = 1;
		visGridBagLayout.setConstraints(polynominalSpinner, c);
		settingsPanel.add(polynominalSpinner);
		
		settingsButton = new JToggleButton("Settings");
		settingsButton.setFocusable(false);
		settingsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				settingsPanel.setVisible(settingsButton.isSelected());
				
			}
		});
		
		settingsPanel.setVisible(false);
		layeredPane = new JLayeredPane();
		layeredPane.add(visPanel, Integer.valueOf(0));
		layeredPane.add(settingsPanel, Integer.valueOf(1));
		layeredPane.add(settingsButton, Integer.valueOf(2));
		this.add(layeredPane, BorderLayout.CENTER);
	}

	public void update(float[] fftRes) {
		if(fftResSize != fftRes.length) {
			showError("fft res length does not match declared value");
		}
		isError = false;
		this.fftRes = fftRes;
		if(binsAtPx == null) {
			calculateFreqAtPx();
			calculateBinsAtPx();
		}
		this.repaint();
	}
	
	
	private void calculateFreqAtPx() {
		try {
			if(getWidth() == 0) return;
			legendImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

			float[] logarithmic = new float[getWidth()+1];

			float sectionLog = (float)(Math.log10(maxFreq) - Math.log10(minFreq))/getWidth();
			logarithmic[0] = (float)(Math.log10(minFreq) + sectionLog);
			for(int i = 1; i < getWidth()+1; i++) {
				logarithmic[i] = logarithmic[i-1] + sectionLog;
			}
			freqAtPx = new int[getWidth()];
			for(int i = 0; i < getWidth(); i++) {
				freqAtPx[i] = (int)Math.ceil(Math.pow(10, logarithmic[i]));
			}
			createLegendImage();
		} catch (Exception e) {
			showError("error in calculateFreqAtPx" + e.getMessage());
		}
		
	
	}
	private void createLegendImage() {
		Color scndColor = Color.white;
		Graphics2D graphics2d = legendImage.createGraphics();
		graphics2d.setStroke(new BasicStroke(1));
		FontMetrics fm = graphics2d.getFontMetrics();
		int fontYOffset = fm.getAscent();
		int currFreq = minFreq;
		int currDelta = 10;
		boolean drawFreq = true;
		for(int i = 0; i < getWidth(); i++) {
			if(currFreq <= freqAtPx[i]) {
				if(drawFreq) {
					graphics2d.setColor(scndColor);
					graphics2d.drawLine(i, fontYOffset+1, i, getHeight()-1);
					if(currFreq > 10000) {
						graphics2d.drawString(Integer.toString(currFreq/1000) + "k", i - 2 - fm.stringWidth(Integer.toString(currFreq/1000) + "k"), fontYOffset);
					}
					else if(currFreq <= minFreq) {
						graphics2d.drawString(currFreq >= 1000 ? Integer.toString(currFreq/1000) + "k" : Integer.toString(currFreq), i+2, fontYOffset);
					}		
					else {
						String toDraw = currFreq >= 1000 ? Integer.toString(currFreq/1000) + "k" : Integer.toString(currFreq);
						graphics2d.drawString(toDraw, i+2 - fm.stringWidth(toDraw)/2, fontYOffset);
					}	
				}
				else {
					graphics2d.setColor(Color.lightGray);
					graphics2d.drawLine(i, fontYOffset+1, i, getHeight()-1);
				}
				drawFreq = !drawFreq;
				currFreq += currDelta;
				if(currFreq >= currDelta*10) {
					currDelta *= 10;
				}
			}
		}
	}
	
	private BufferedImage createVisualisationImage() {
		BufferedImage res = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		try {
			float sum;
			float heightMultiplier = (float)sensivity * getHeight()/100;
			int i = 0;
			curveData = new float[getWidth()];
			while(i < getWidth()) {
				sum = 0;
				if( binsAtPx.get(i).size() == 0) continue;
				for(int j = binsAtPx.get(i).getFirst(); j <= binsAtPx.get(i).getLast(); j++) {
					sum += fftRes[j];
				}
				
				sum /= binsAtPx.get(i).size();
				sum = (float)(Math.log10(sum > 1 ? sum : 1)) * heightMultiplier;
				
				if(sum > getHeight())
					sum = getHeight();
				
				curveData[i] = sum;
				
				if(interpolationData.get(i) != null) {
					i += interpolationData.get(i);
				}
				else {
					i++;
				}
				
						
			}

			Set<Integer> interpolationIdxs = interpolationData.keySet();
			int idx;
			float pixelDelta;
			int mtp;
			for(Object idxO : interpolationIdxs) {
				idx = (int)idxO;
				pixelDelta = (curveData[idx +  interpolationData.get(idx)] - curveData[idx])/(interpolationData.get(idx));
				mtp = 1;

				for(int k = idx+1; k < idx + interpolationData.get(idx); k++){
					curveData[k] = mtp*pixelDelta + curveData[idx];
					mtp++;
				}
			}
			//System.out.println(Arrays.toString(curveData));	
			if(smoothingCheckBox.isSelected()) {
				sgf.windowSize = (int)windowSizeSpinner.getValue();
				sgf.process(curveData, x, curveData);
			}
			int y1, y2;
			int height = getHeight();
			Graphics2D g2d = (Graphics2D)res.getGraphics();
			
			Color[] gradientColors = {Color.green, Color.yellow, Color.red};
			float[] colorsPos = {0.0f, 0.4f, 0.90f};
			
			g2d.setPaint(new LinearGradientPaint(new Point(0, height), new Point(0, 0), colorsPos, gradientColors));
			
			int[] xPts = new int[getWidth()+2];
			int[] yPts = new int[getWidth()+2];

			for(int j = 0; j < getWidth(); j++) {
				xPts[j+1] = j;
				yPts[j+1] = (height - (int)curveData[j]) > height ? height : (height - (int)curveData[j]);
			}
			xPts[0] = 0;
			yPts[0] = height;
			
			xPts[xPts.length-1] = getWidth();
			yPts[yPts.length-1] = height;
			if(fillCheckBox.isSelected())
				g2d.fillPolygon(xPts, yPts, getWidth()+2);
			else
				g2d.drawPolygon(xPts, yPts, getWidth()+2);

		} catch (Exception e) {
			showError("error in createVisualisationImage" + e.getMessage());
		}
		
		return res;
	}
	
	private void calculateBinsAtPx() { //also fills interpolationData, x
		try {
			if(getWidth() <= 0 || sampleRate == 0 || buffSize == 0 || freqAtPx == null || fftRes == null) return;
			System.out.printf("vis: sample rate: %d, buffsize: %d\n", sampleRate, buffSize);
			
			binsAtPx = new ArrayList<ArrayList<Integer>>(getWidth());
			double freqSpacing = sampleRate*1.0d/buffSize;
			double currFreq = freqSpacing;
			int binIndex = 0;
			interpolationData = new LinkedHashMap<Integer, Integer>();
			for(int px = 0; px < getWidth(); px++) {
				binsAtPx.add(new ArrayList<Integer>());
				while(binIndex < fftResSize) {
					//System.out.println("currFreq: " + currFreq + " FreqAtPx[px]: " + freqAtPx[px]);
					if(currFreq > freqAtPx[px]) {
						if(px > 0 &&binsAtPx.get(px-1).size() == 0) {
							showError("An error occured during calculating bins at pixels: too sparse data");
							binsAtPx = null;
							return;
						}
						if(binsAtPx.get(px).size() == 0 && px > 0) {
				 			binsAtPx.get(px).add(binsAtPx.get(px-1).getLast());
						}
						break;
					}
					binsAtPx.get(px).add(binIndex);
					binIndex++;
					currFreq += freqSpacing;
				}
			}
			
			for(int i = 0; i < binsAtPx.size(); i++) {
				if(binsAtPx.get(i).size() > 2) {
					ArrayList<Integer> tmp = new ArrayList<>();
					tmp.add(binsAtPx.get(i).getFirst());
					tmp.add(binsAtPx.get(i).getLast());
					binsAtPx.set(i, tmp);
				}
			}
			interpolationData = new LinkedHashMap<>();
			int repeatingIdx = -1;
			
			
			for(int i = 0; i < binsAtPx.size()-1; i++) {	
				if(binsAtPx.get(i).getFirst() == binsAtPx.get(i+1).getFirst()) {
					if(repeatingIdx == -1) {
						repeatingIdx = i;
						interpolationData.put(i, 2);
					}
					else {
						interpolationData.put(repeatingIdx, interpolationData.get(repeatingIdx)+1);
					}
				}
				else {
					repeatingIdx = -1;
				}
			}
			x = new float[getWidth()];
			for(int j = 0; j < getWidth(); j++) {
				x[j] = j;
			}
		} catch (Exception e) {
			showError("error in calculateBinsAtPx " + e.getMessage());
			e.printStackTrace();
		}
		
		
	}
	
	public void setSampleRate(int newSampleRate) {
		this.sampleRate = newSampleRate;
		calculateBinsAtPx();
	}
	public void setBuffSize(int newBuffSize) {
		this.buffSize = newBuffSize;
		this.fftResSize = newBuffSize % 2 == 0 ? newBuffSize / 2 : (newBuffSize-1)/2;
		calculateBinsAtPx();
	}
	public void setSensivity(int newSens) {
		this.sensivity = newSens;
	}
	public void setBandsNum(int newBandsNum) {

	}
	private void calculateLayeredPaneParams() {
		layeredPane.setPreferredSize(new Dimension(getWidth(), getHeight()));
		visPanel.setBounds(0, 0, getWidth(), getHeight());
		settingsPanel.setBounds(getWidth() - 300, 0, 300, getHeight());
		settingsButton.setBounds(getWidth() - 100, 15, 80, 20);
	}
	public void addEventListener(VisualiserEventListener el) {
		this.el = el;
		HashMap<String, String> settings = el.xmlSettingsRequested();
		System.out.println(settings.toString());
		String value;
		value = settings.get("fill");
		if(value != null)
			fillCheckBox.setSelected(Boolean.valueOf(value));
		else {
			fillCheckBox.setSelected(fill);
			el.settingsFieldChanged("fill", Boolean.toString(fill));
		}
		value = settings.get("smoothing");
		if(value != null)
			smoothingCheckBox.setSelected(Boolean.valueOf(value));
		else {
			smoothingCheckBox.setSelected(smoothing);
			el.settingsFieldChanged("smoothing", Boolean.toString(smoothing));
		}
		value = settings.get("windowSize");
		if(value != null)
			windowSizeSpinner.setValue(Integer.parseInt(value));
		else {
			windowSizeSpinner.setValue(smoothingWindowSize);
			el.settingsFieldChanged("windowSize", Integer.toString(smoothingWindowSize));
		}
		value = settings.get("polynominal");
		if(value != null)
			polynominalSpinner.setValue(Integer.parseInt(value));
		else {
			polynominalSpinner.setValue(smoothingPolynominal);
			el.settingsFieldChanged("polynominal", Integer.toString(smoothingPolynominal));
		}
		
	}
	
	public void showError(String message) {
		this.errorMsg = message;
		isError = true;
		repaint();
		el.streamStopRequested();
		new Thread(()->{
			try {
				TimeUnit.MILLISECONDS.sleep(100);
				el.streamStartRequested();
			}
			catch(Exception e) {
				showError("Error during stream restart: " + e.getMessage());
			}
			
		}).start();;
	}
	/*
	public static void main(String[] args) {
		
		VisualiserCurve vis = new VisualiserCurve();
		
		vis.setSampleRate(44100);
		vis.setBuffSize(9120);
		vis.setBuffSize(9276);
	}
	*/
}
