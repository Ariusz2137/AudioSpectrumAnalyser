package audioSpectrumAnalyser;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JPanel;

public class VisualiserFixedBarWidth extends JPanel implements Visualiser{
	private int[] freqBounds;
	private int sampleRate;
	private int buffSize;
	private int bandsNum;
	private int minFreq = 20;
	private int maxFreq = 22000;
	private int sensivity;
	
	private float[] fftRes;
	
	private int[] resFreq;
	private float[] resPow;
	private int[] barsPosPx;
	
	private int prevPanelWidth = 0;
	private int prevPanelHeight = 0;
	
	private Color[] gradient;
	private BufferedImage legendImage;
	private int barWidth;
	
	private boolean mouseOver = false;
	private int mouseX;
	
	private long prevTime;
	
	public VisualiserFixedBarWidth() {
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
		prevTime = System.nanoTime();
	}
	private void calculateGradient() {
		
		Color[] gradientColors = {Color.green, Color.yellow, Color.red};
		float[] colorsPos = {0.0f, 0.4f, 0.90f};
		
		int steps = this.getHeight();
		
		gradient = new Color[steps];
		
		for(int i = 0; i < steps * colorsPos[0]; i++) {
			gradient[i] = gradientColors[0];
		}
		float redDelta, greenDelta, blueDelta, currRed, currGreen, currBlue;
		for(int i = 0; i < gradientColors.length-1; i++) {
			redDelta = (gradientColors[i+1].getRed() - gradientColors[i].getRed())*1.0f/(colorsPos[i+1] - colorsPos[i])/steps;
			blueDelta = (gradientColors[i+1].getBlue() - gradientColors[i].getBlue())*1.0f/(colorsPos[i+1] - colorsPos[i])/steps;
			greenDelta = (gradientColors[i+1].getGreen() - gradientColors[i].getGreen())*1.0f/(colorsPos[i+1] - colorsPos[i])/steps;
			
			currRed = gradientColors[i].getRed();
			currGreen = gradientColors[i].getGreen();
			currBlue = gradientColors[i].getBlue();
			
			for(int j = (int)(steps*colorsPos[i]); j < (int)(steps*colorsPos[i+1]); j++) {
				gradient[j] = new Color((int)currRed, (int)currGreen, (int)currBlue);
				currRed += redDelta;
				currGreen += greenDelta;
				currBlue += blueDelta;			
			}
		}
		
		
		for(int i = (int)(steps * colorsPos[colorsPos.length-1]); i < steps; i++) {
			gradient[i] = gradientColors[colorsPos.length-1];
		}
	}
	
	public void update(float[] fftRes) {
		this.fftRes = fftRes;
		fetchFftResults();
		this.repaint();
	}
	@Override
	protected void paintComponent(Graphics g) {	
		super.paintComponent(g);
		boolean newSize = false;
		if(prevPanelWidth != this.getWidth() || prevPanelHeight != this.getHeight() ) {
			calculateGradient();
			prevPanelWidth = getWidth();
			prevPanelHeight = getHeight();
			newSize = true;
			
		}
		int totalWidth = this.getWidth();
		int lineSpacing = 0;
		int lineWidth = totalWidth/bandsNum;
		barWidth = lineWidth;
		float heightMultiplier = this.getHeight()/300.0f;
		int missingLines = totalWidth - lineWidth*bandsNum;
		float additLinesSpacing = totalWidth*1.0f/missingLines;
		float currAdditTagetLine = additLinesSpacing;
		Graphics2D graphics2d = (Graphics2D)g;
		//System.out.printf("missing lines: %d, add every %dpx", missingLines, addLineEveryNPixels);
		BufferedImage image = new BufferedImage(totalWidth, this.getHeight(), BufferedImage.TYPE_INT_ARGB);
		
		int x = 0, y1 = this.getHeight(), y2 = 0;
		if(newSize || legendImage == null) {
			barsPosPx = new int[bandsNum];
			for(int i = 0; i < bandsNum; i++) {
				barsPosPx[i] = x;
				for(int j = 0; j < lineWidth; j++) {
					y2 = (y1 - (int)(Math.pow(sensivity, 2)*resPow[i]*heightMultiplier));
					for(int k = y1-1; k >= (y2 < 0 ? 0 : y2); k--) {
						//System.out.printf("x: %d, y: %d", x+j, k);
						image.setRGB(x, k, gradient[gradient.length-k-1].getRGB());	
						//System.out.printf("default: x: %d, y: %d\n", x+j, k);
					}
					x++;
				}
				if(x > currAdditTagetLine) { //this increases width of some bars by 1px so that visualisation fills full image
					for(int k = y1-1; k >= (y2 < 0 ? 0 : y2); k--) {
						//System.out.printf("x: %d, y: %d\n", x, k);
						image.setRGB(x, k, gradient[gradient.length-k-1].getRGB());	
					}
					currAdditTagetLine += additLinesSpacing;
					x++;
				}
			}
			legendImage = createLegendImage();
		}
		else {
			for(int i = 0; i < bandsNum; i++) {
				for(int j = 0; j < lineWidth; j++) {
					y2 = (y1 - (int)(Math.pow(sensivity, 2)*resPow[i]*heightMultiplier));
					for(int k = y1-1; k >= (y2 < 0 ? 0 : y2); k--) {
						//System.out.printf("x: %d, y: %d", x+j, k);
						image.setRGB(x, k, gradient[gradient.length-k-1].getRGB());	
						//System.out.printf("default: x: %d, y: %d\n", x+j, k);
					}
					x++;
				}
				if(x > currAdditTagetLine) { //this increases width of some bars by 1px to perfectly fit visualisation in full image
					for(int k = y1-1; k >= (y2 < 0 ? 0 : y2); k--) {
						//System.out.printf("x: %d, y: %d\n", x, k);
						image.setRGB(x, k, gradient[gradient.length-k-1].getRGB());	
					}
					currAdditTagetLine += additLinesSpacing;
					x++;
				}
			}
		}
		graphics2d.drawImage(legendImage, 0, 0, null);
		graphics2d.drawImage(image, 0, 0, null);
		String fpsString = Integer.toString((int)(1/((System.nanoTime() - prevTime)/1000000000d))) + "FPS";
		prevTime = System.nanoTime();
		
		FontMetrics fm = graphics2d.getFontMetrics();
		graphics2d.setStroke(new BasicStroke(2));
		graphics2d.drawString(fpsString, totalWidth - fm.stringWidth(fpsString) , getHeight());
		
		if(mouseX > -1 && freqBounds != null) {
			graphics2d.drawLine(mouseX, 0, mouseX, getHeight());
			if(mouseX < getWidth()-barWidth) {
				String toDraw = Integer.toString(findFreqAtXPos(mouseX))+"Hz";
				int strWidth = fm.stringWidth(toDraw);
				if(strWidth < getWidth() - mouseX)
					graphics2d.drawString(toDraw, mouseX + 2, 10);
				else
					graphics2d.drawString(toDraw, mouseX - 2 - strWidth, 10);
			}
			else {
				String toDraw = Integer.toString(freqBounds[freqBounds.length-1])+"Hz";
				int strWidth = fm.stringWidth(toDraw);
				if(strWidth < getWidth() - mouseX)
					graphics2d.drawString(toDraw, mouseX + 2, 10);
				else
					graphics2d.drawString(toDraw, mouseX - 2 - strWidth, 10);
			}
		}
	}
	
	private int findFreqAtXPos(int xPos) {
		return freqBounds[binSearch(0, barsPosPx.length, xPos)];
	}
	private int binSearch(int min, int max, int x) {
		if(max >= min) {
			int mid = min + (max - min)/2;
			if(barsPosPx[mid] == x)
				return mid;
			if(barsPosPx[mid] > x) 
				return binSearch(min, mid-1, x);
			return binSearch(mid+1, max, x);
		}
		return max;
	}
	
	private BufferedImage createLegendImage() {
		if(freqBounds == null || barsPosPx == null || barsPosPx.length != freqBounds.length) {
			return null;
		}
		BufferedImage res = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		Color scndColor = Color.white;
		Graphics2D graphics2d = res.createGraphics();
		graphics2d.setStroke(new BasicStroke(1));
		FontMetrics fm = graphics2d.getFontMetrics();
		int fontyOffset = fm.getAscent();
		int currFreq = minFreq;
		int currDelta = 10;
		boolean drawFreq = true;
		for(int i = 0; i < barsPosPx.length; i++) {
			while(freqBounds[i] - currDelta > currFreq) {
				currFreq += currDelta;
				if(currFreq >= currDelta*10) {
					currDelta *= 10;
				}
			}
			if(freqBounds[i] > currFreq) {
				if(drawFreq) {
					graphics2d.setColor(scndColor);
					graphics2d.drawLine(barsPosPx[i], fontyOffset+1, barsPosPx[i], getHeight()-1);
					if(currFreq > 10000) {
						graphics2d.drawString(Integer.toString(currFreq/1000) + "k", barsPosPx[i] - 2 - fm.stringWidth(Integer.toString(currFreq/1000) + "k"), fontyOffset);
					}
					else if(currFreq <= minFreq) {
						graphics2d.drawString(currFreq >= 1000 ? Integer.toString(currFreq/1000) + "k" : Integer.toString(currFreq), barsPosPx[i]+2, fontyOffset);
					}		
					else {
						String toDraw = currFreq >= 1000 ? Integer.toString(currFreq/1000) + "k" : Integer.toString(currFreq);
						graphics2d.drawString(toDraw, barsPosPx[i]+2 - fm.stringWidth(toDraw)/2, fontyOffset);
					}	
				}
				else {
					graphics2d.setColor(Color.lightGray);
					graphics2d.drawLine(barsPosPx[i], fontyOffset+1, barsPosPx[i], getHeight()-1);
				}
				drawFreq = !drawFreq;
				currFreq += currDelta;
				if(currFreq >= currDelta*10) {
					currDelta *= 10;
				}
			}
		}	
		return res;
	}
	
	
	public void setBandsNum(int newBandsNum) {
		this.bandsNum = newBandsNum;
		calculateBounds();
		legendImage = createLegendImage();
		resFreq = new int[newBandsNum];
		resPow = new float[newBandsNum];
		
	}
	
	public void calculateBounds() {
		System.out.printf("sample rate: %d, buffSize: %d\n", sampleRate, buffSize);
		if(sampleRate <= 0 || buffSize <= 0)
			return;
		int[] linear = new int[buffSize/2];
		float[] logarithmic = new float[bandsNum + 1];
		float multiplier = sampleRate * 1.0f / buffSize;
		int it = 1;
		while(Math.floor(it*multiplier) < minFreq)
			it++;

		for(int i = it; i < buffSize/2 + 1; i++) {
			linear[i-it] = (int)Math.ceil(i * multiplier);
		}
		float sectionLog = (float)(Math.log10(maxFreq) - Math.log10(minFreq))/bandsNum;
		logarithmic[0] = (float)(Math.log10(minFreq) + sectionLog);
		for(int i = 1; i < bandsNum + 1; i++) {
			logarithmic[i] = logarithmic[i-1] + sectionLog;
		}
		int[] logarithmicInt = new int[bandsNum];
		for(int i = 0; i < bandsNum; i++) {
			logarithmicInt[i] = (int)Math.ceil(Math.pow(10, logarithmic[i]));
		}
		System.out.println(Arrays.toString(linear));
		System.out.println(Arrays.toString(logarithmicInt));
		freqBounds = new int[bandsNum];
		for(int i = 0; i < bandsNum; i++) {
			freqBounds[i] = logarithmicInt[i] > linear[i] ? logarithmicInt[i] : linear[i];
			if(freqBounds[i] > maxFreq + 1)
				throw new IllegalArgumentException("too sctrict params, try lowering FPS or number of bands, or increase sub buffers (chunks) number");
		}
		if(freqBounds.length > 0) System.out.printf("|Band %2d: %5d Hz - %5d Hz|\n", 1, 1, freqBounds[0]);
		for(int i = 0; i < bandsNum-1; i++) {
			System.out.printf("|Band %2d: %5d Hz - %5d Hz|\n", i+2, freqBounds[i], freqBounds[i+1]);
	    }
	}
	private void fetchFftResults() {
		int max, actIdx = 0, count;
		float sum;
		double freqSpacing = sampleRate*1.0f/buffSize;
		double currFreq = freqSpacing;
		for(int i = 0; i < bandsNum; i++) {
			max = freqBounds[i];
			sum = 0;
			count = 0;
			while(currFreq <= max) {
				sum += fftRes[actIdx++];
				count++;
				currFreq += freqSpacing;
			}
			resFreq[i] = max;
			sum = sum*1.0f/count;
			resPow[i] = (float)(Math.log10(sum > 1 ? sum : 1));
		}
	}
	public void setSampleRate(int newSampleRate) {
		this.sampleRate = newSampleRate;
		calculateBounds();
		legendImage = createLegendImage();
	}
	public void setBuffSize(int newBuffSize) {
		this.buffSize = newBuffSize;
		calculateBounds();
		legendImage = createLegendImage();
	}
	public void setSensivity(int newSens) {
		this.sensivity = newSens;
	}
	public void addEventListener(VisualiserEventListener el) {}
	
	public void showError(String message) {}
}