package audioSpectrumAnalyser;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JPanel;

public class VisualiserFixedFreqScale extends JPanel implements Visualiser{
	
	private BufferedImage legendImage;
	
	private int sampleRate;
	private int buffSize;
	private int sensivity;
	private int bandsNum;
	
	private int minFreq = 20;
	private int maxFreq = 22000;
	
	private float[] fftRes;
	
	private int[] freqAtPx; //stores corresponding frequency for each column
	private ArrayList<ArrayList<Integer>> binsAtPx; //stores ArrayList of corresponding fft bins for each pixel
	
	private boolean mouseOver;
	private int mouseX;
	
	public VisualiserFixedFreqScale() {
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				calculateFreqAtPx();
				calculateBinsAtPx();
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
	}

	public void update(float[] fftRes) {
		this.fftRes = fftRes;
		//System.out.println(binsAtPx);
		//System.out.println(Arrays.toString(fftRes));
		if(binsAtPx == null) {
			calculateFreqAtPx();
			calculateBinsAtPx();
		}
		this.repaint();
	}
	
	@Override
	protected void paintComponent(Graphics g) {	
		super.paintComponent(g);
		Graphics2D graphics2d = (Graphics2D)g;
		graphics2d.drawImage(legendImage, 0, 0, null);
		
		if(fftRes != null && getHeight() != 0)
			graphics2d.drawImage(createVisualisationImage(), 0, 0, null);
		
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
	private void calculateFreqAtPx() {
		if(getHeight() <= 0 || getWidth() == 0) return;
		legendImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		System.out.printf("sample rate: %d, buffSize: %d\n", sampleRate, buffSize);
		if(sampleRate <= 0 || buffSize <= 0)
			return;
		float[] logarithmic = new float[getWidth()+1];
		float multiplier = sampleRate * 1.0f / buffSize;
		int it = 1;
		while(Math.floor(it*multiplier) < minFreq)
			it++;

		float sectionLog = (float)(Math.log10(maxFreq) - Math.log10(minFreq))/getWidth();
		logarithmic[0] = (float)(Math.log10(minFreq) + sectionLog);
		for(int i = 1; i < getWidth()+1; i++) {
			logarithmic[i] = logarithmic[i-1] + sectionLog;
		}
		freqAtPx = new int[getWidth()];
		for(int i = 0; i < getWidth(); i++) {
			freqAtPx[i] = (int)Math.ceil(Math.pow(10, logarithmic[i]));
		}
		//System.out.println(Arrays.toString(freqAtPx));
		createLegendImage();
	
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
			
			float heightMultiplier = (float)Math.pow(sensivity, 2) * getHeight()/300;
			for(int i = 0; i < getWidth(); i++) {
				sum = 0;
				
				//System.out.println("first bin: " + binsAtPx.get(i).getFirst() + " last bin: " + binsAtPx.get(i).getLast());
				if( binsAtPx.get(i).size() == 0) continue;
				for(int j = binsAtPx.get(i).getFirst(); j <= binsAtPx.get(i).getLast(); j++) {
					sum += fftRes[j];
				}
				
				sum /= binsAtPx.get(i).size();
				sum = (float)(Math.log10(sum > 1 ? sum : 1)) * heightMultiplier;
				
				if(sum > getHeight())
					sum = getHeight();
				for(int j = getHeight()-1; j >= getHeight() - sum; j--) {
					res.setRGB(i, j, Color.green.getRGB());
				}
			}
			
		} catch (Exception e) {
			System.err.println("error during generating visualisation frame: " + e.getMessage());
		}
		
		return res;
	}
	
	private void calculateBinsAtPx() {
		if(getWidth() <= 0 || sampleRate == 0 || buffSize == 0 || bandsNum == 0 || freqAtPx == null) return;
		binsAtPx = new ArrayList<ArrayList<Integer>>(getWidth());
		double freqSpacing = sampleRate*1.0f/buffSize;
		double currFreq = freqSpacing;
		int binIndex = 0;
		for(int px = 0; px < getWidth(); px++) {
			binsAtPx.add(new ArrayList<Integer>());
			while(binIndex < fftRes.length) {
				//System.out.println("currFreq: " + currFreq + " FreqAtPx[px]: " + freqAtPx[px]);
				if(currFreq > freqAtPx[px]) {
					if(binsAtPx.get(px).size() == 0) {
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
		System.out.println(binsAtPx);
		
	}
	
	public void setSampleRate(int newSampleRate) {
		this.sampleRate = newSampleRate;
		calculateBinsAtPx();
	}
	public void setBuffSize(int newBuffSize) {
		this.buffSize = newBuffSize;
		calculateBinsAtPx();
	}
	public void setSensivity(int newSens) {
		this.sensivity = newSens;
	}
	public void setBandsNum(int newBandsNum) {
		this.bandsNum = newBandsNum;
		if(freqAtPx == null) {
			calculateFreqAtPx();
		}
		calculateBinsAtPx();
	}
}
