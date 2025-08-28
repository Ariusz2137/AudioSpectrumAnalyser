package audioSpectrumAnalyser;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicScrollPaneUI.VSBChangeListener;

public class MainPanel extends JPanel {

	public MainPanel(BlockingQueue<Runnable> bq) {
		GridBagLayout gridBag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(gridBag);
		
		Visualiser vis = new VisualiserCurve();
		c.gridx = 0;
		c.weightx = 1;
		c.weighty = 0.8;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		gridBag.setConstraints((JPanel)vis, c);
		this.add((JPanel)vis);
		
		SampleCollector sc = new SampleCollector(bq);
		c.gridx = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridy = 1;
		gridBag.setConstraints(sc, c);
		this.add(sc);
		
		SettingsPanel sp = new SettingsPanel();
		
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		
		gridBag.setConstraints(sp, c);
		this.add(sp);
		
		FFTProcessor fftProcessor = new FFTProcessor();
		
		sc.addEventListener(new SampleCollectorEventListener() {		
			@Override
			public void samplesBufferAvailable(float[] samples) {	
				new Thread(()->{
					float[] fftOut = fftProcessor.run(samples);
					vis.update(fftOut);	
				}).start();
			}
			@Override
			public void totalSamplesNumberChanged(int newSize) {
				fftProcessor.setSize(newSize);
				vis.setBuffSize(newSize);
			}
			@Override
			public void sampleRateChanged(int newSampleRate) {
				vis.setSampleRate(newSampleRate);
			}
			@Override
			public void hostChanged(String newHost) {
				sp.setHostName(newHost);
			}
			@Override
			public void deviceChanged(String newDevice) {
				sp.setDeviceName(newDevice);
			}
		}); 
		sp.addEventListener(new SettingsEventListener() {
			
			@Override
			public void windowFunctionChanged(int newFunction) {
				fftProcessor.setWindow(newFunction);	
			}
			
			@Override
			public void stopRequested() {
				sc.stop();
				
			}
			
			@Override
			public void stereoModeChanged(int newMode) {
				sc.changeStereoMode(newMode);
				sc.start();	
			}
			
			@Override
			public void startRequested() {
				sc.start();
			}
			
			@Override
			public void sensivityChanged(int newSens) {
				vis.setSensivity(newSens);		
			}
			
			@Override
			public void overlappingWindowsChanged(int newOverlap) {
				sc.changeWindowOverlap(newOverlap);
				sc.start();		
			}
			
			@Override
			public void fpsChanged(int newFps) {
				sc.changeFps(newFps);
				sc.start();	
			}
			@Override
			public void setFullConfig(int fps, int Overlap, int stereoMode) {
				sc.setConfig(fps, Overlap, stereoMode);
				sc.start();
			}
			@Override
			public void setBandsNumber(int newBands) {
				vis.setBandsNum(newBands);
			}
			@Override
			public void setHost(String hostName) {
				sc.setSystem(hostName);
			}
			@Override
			public void setDevice(String devName) {
				sc.setDevice(devName);
			}
		});
	}
}
