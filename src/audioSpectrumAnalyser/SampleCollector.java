package audioSpectrumAnalyser;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Identity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import xt.audio.Enums.XtEnumFlags;
import xt.audio.Enums.XtSample;
import xt.audio.Enums.XtSystem;
import xt.audio.Structs.XtBuffer;
import xt.audio.Structs.XtBufferSize;
import xt.audio.Structs.XtChannels;
import xt.audio.Structs.XtDeviceStreamParams;
import xt.audio.Structs.XtFormat;
import xt.audio.Structs.XtMix;
import xt.audio.Structs.XtStreamParams;
import xt.audio.XtAudio;
import xt.audio.XtDevice;
import xt.audio.XtDeviceList;
import xt.audio.XtPlatform;
import xt.audio.XtSafeBuffer;
import xt.audio.XtService;
import xt.audio.XtStream;

public class SampleCollector extends JPanel {
	
	private int defaultSampleRate = 44100;

	private JComboBox<String> systemBox;
	private JComboBox<String> devicesBox;
	
	private XtPlatform platform;
	private XtService service;
	private XtDeviceList deviceList;
	private XtDevice currDevice;
	private String currDeviceID;
	private XtStream stream;
	
	private ArrayList<XtSystem> systems;
	private ArrayList<String> deviceIDs;

	private boolean isActive = false;
	private int singleBufferLength;
	private int subBuffersNum;
	private MainBuffer mainBuffer;
	private int stereoMode; //0 for average, 1 for left, 2 for right 
	public int sampleRate;
	private int fps;
	private int prevTotalBuffSize;
	
	private BlockingQueue<Runnable> queue;
	private SampleCollectorEventListener el;
	
	private boolean isFloat32;
	private static float multiplier;
	
	private class MainBuffer{
		public float[] buffer;
		public int bufferMaxSize;
		public int bufferCurrentSize;
		public int chunkSize;
		public int finalBufferSize;
		
		public MainBuffer(int maxSize, int chunkSize, int finalBufferSize) {
			buffer = new float[maxSize];
			bufferMaxSize = maxSize;
			bufferCurrentSize = 0;
			this.chunkSize = chunkSize;
			this.finalBufferSize = finalBufferSize;
			System.out.println("mainBuffer config: bufferMaxSize:"  + bufferMaxSize + " chunkSize: " + chunkSize + " finalBufferSize" + finalBufferSize);
		}
		public void addNewSamples(float[] samples, int size) {
			if(samples.length + bufferCurrentSize > bufferMaxSize) {
				el.errorOccured("audio callback aborted due to main buffer overflow");
				stop();
				isActive = false;
				return;
			}
			System.arraycopy(samples, 0, buffer, bufferCurrentSize, size);
			bufferCurrentSize += size; 
			if(bufferCurrentSize >= finalBufferSize) {
				el.samplesBufferAvailable(Arrays.copyOfRange(buffer, 0, finalBufferSize));
				System.arraycopy(buffer, chunkSize, buffer, 0, finalBufferSize - chunkSize + size);
				bufferCurrentSize -= chunkSize;
			}
		}
	}
	
	public SampleCollector(BlockingQueue<Runnable> bq) {
		this.setMinimumSize(new Dimension(1000, 300));
		queue = bq;
		ArrayList<String> systemStrings = new ArrayList<>();
		systems = new ArrayList<>();
		deviceIDs = new ArrayList<>();
		XtAudio.setOnError(SampleCollector::onError);
		platform = XtAudio.init(Integer.toString(this.hashCode()), null);
		
		for(XtSystem system : platform.getSystems()) {
			systemStrings.add(system.name());
			systems.add(system);
		}
		
		systemBox = new JComboBox<String>(systemStrings.toArray(new String[0]));
		this.add(new JLabel("Select host:"));
		this.add(systemBox);
		systemBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showSystemDevices(systems.get(systemBox.getSelectedIndex()));
				el.hostChanged(systemStrings.get(systemBox.getSelectedIndex()));
			}
		});
		devicesBox = new JComboBox<String>();
		devicesBox.setPreferredSize(new Dimension(400, 25));
		this.add(new JLabel("Select device:"));
		this.add(devicesBox);
		devicesBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(devicesBox.getSelectedIndex() >= 0)
					start(deviceIDs.get(devicesBox.getSelectedIndex()));
					el.deviceChanged((String)devicesBox.getSelectedItem());
			}
		});
		this.stereoMode = -1;
	}
	public void setSystem(String systemName) {
		for(int i = 0; i < systems.size(); i++) {
			if(systems.get(i).name().equals(systemName)) {
				systemBox.setSelectedIndex(i);
				return;
			}
		}
		System.err.println("Could not find System (Host): " + systemName);
	}
	public void setDevice(String deviceName) {
		try {
			queue.put(new Thread(()->{
				//System.out.println("system item count: " + devicesBox.getItemCount());
				for(int i = 0; i < devicesBox.getItemCount(); i++) {
					//System.out.printf("device at index %d: %s, looking for: %s", i, devicesBox.getItemAt(i), deviceName);
					if(devicesBox.getItemAt(i).equals(deviceName)) {
						devicesBox.setSelectedIndex(i);
						return;
					}
				}
				System.err.println("Could not find Device: " + deviceName);
				})
			);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public int changeWindowOverlap(int newOverlap) {
		if(newOverlap < 1)
			return -1;
		subBuffersNum = newOverlap;
		return 0;
	}
	public int changeFps(int newFps) {
		if(newFps < 1)
			return -1;
		fps = newFps;
		return 0;
	}
	public int changeStereoMode(int newStereoMode) {
		if(stereoMode < 0 || stereoMode > 3)
			return -1;
		stereoMode = newStereoMode;
		return 0;
	}
	
	public int setConfig(int fps, int Overlap, int stereoMode) {
		if(fps < 1 || Overlap < 1 || stereoMode < 0 || stereoMode > 3)
			return -1;
		this.fps = fps;
		this.subBuffersNum = Overlap;
		this.stereoMode = stereoMode;
		return 0;
	}
	
	public void addEventListener(SampleCollectorEventListener el) {
		this.el = el;
	}
	
	private void showSystemDevices(XtSystem choosen){
		try {
			queue.put(()->{
				if (deviceList != null) deviceList.close();
				deviceIDs.clear();
				service = platform.getService(choosen);
				deviceList = service.openDeviceList(EnumSet.of(XtEnumFlags.INPUT));
				devicesBox.removeAllItems();
				for(int i = 0; i < deviceList.getCount(); i++) {
					String id = deviceList.getId(i);
					if(deviceList.getName(id).toLowerCase().contains("exclusive")) { //skips exclusive mode in WASAPI
						continue;
					}
					deviceIDs.add(id);
					devicesBox.addItem(deviceList.getName(id));
				}
				//System.out.println("system item count: " + devicesBox.getItemCount());
				this.revalidate();
				this.repaint();
			});
		} catch(Exception e) {
			System.err.println("Error while opening selected system");
			e.printStackTrace();
		}
	}
	public int start() {
		if(currDeviceID != null && stereoMode != -1 && fps != 0 && subBuffersNum != 0)
			start(currDeviceID);
		else 
			return -1;
		return 0;
	}
	
	
	private void start(String devID) {
		try {
			queue.put(()->{
				try {
					if(isActive) {
						stream.stop();
						isActive = false;
					}
					System.out.println("currDevId:" + currDeviceID);
					currDevice = service.openDevice(devID);
					XtChannels channels = new XtChannels(2, 0, 0, 0);
					XtFormat format;
					isFloat32 = true;
					try {
						format = new XtFormat(new XtMix(currDevice.getMix().get().rate, XtSample.FLOAT32) , channels);
						if(!currDevice.supportsFormat(format)) {
							isFloat32 = false;
							format = new XtFormat(new XtMix(currDevice.getMix().get().rate, XtSample.INT24) , channels);
							multiplier = (float)(1.0f/Math.pow(2, 23));
						}
						if(!currDevice.supportsFormat(format)) {
							format = new XtFormat(new XtMix(currDevice.getMix().get().rate, XtSample.INT32) , channels);
							multiplier = (float)(1.0f/Math.pow(2, 31));
						}
					} catch (NoSuchElementException e) {
						System.out.println("cannot get device sample rate, using default settings...");
						format = new XtFormat(new XtMix(defaultSampleRate, XtSample.FLOAT32), channels);
						if(!currDevice.supportsFormat(format)) {
							isFloat32 = false;
							format = new XtFormat(new XtMix(currDevice.getMix().get().rate, XtSample.INT24) , channels);
							multiplier = (float)(1.0f/Math.pow(2, 23));
						}
						if(!currDevice.supportsFormat(format)) {
							format = new XtFormat(new XtMix(currDevice.getMix().get().rate, XtSample.INT32) , channels);
							multiplier = (float)(1.0f/Math.pow(2, 31));
						}
					}
					if(!currDevice.supportsFormat(format))
						throw new Exception("Format error");
					currDeviceID = devID;
					
					singleBufferLength = format.mix.rate / fps;
					
                    if(singleBufferLength * subBuffersNum != prevTotalBuffSize) {
                    	prevTotalBuffSize = singleBufferLength * subBuffersNum;
                    	el.totalSamplesNumberChanged(prevTotalBuffSize);
                    	
                    }
                    
                    if(format.mix.rate != this.sampleRate) {
                    	sampleRate = format.mix.rate;
                    	el.sampleRateChanged(sampleRate);
                    }
                    
                    XtBufferSize bufferSize = currDevice.getBufferSize(format);
                    
					mainBuffer = new MainBuffer(singleBufferLength * (subBuffersNum + 1) + (int)bufferSize.current, singleBufferLength, singleBufferLength * subBuffersNum);
					//System.out.println(bufferSize.size());
					//XtBufferSize bufferSize = new XtBufferSize();
					//System.out.println(currDevice.getBufferSize(format) + "calculated buffer (singleBufferLength):" + singleBufferLength + " buffer to FFT(final buffer): " + singleBufferLength * subBuffersNum);
					
					XtStreamParams streamParams;
					if(isFloat32) {
						switch (stereoMode) {
							case 0:
								streamParams = new XtStreamParams(true, SampleCollector::audioCallbackAvg, null, null);	
								break;		
							case 1:
								streamParams = new XtStreamParams(true, SampleCollector::audioCallbackLeft, null, null);
								break;
							case 2:
								streamParams = new XtStreamParams(true, SampleCollector::audioCallbackRight, null, null);
								break;
							
							default:
								throw new IllegalArgumentException("Unexpected value: " + stereoMode);
						}
					}
					else {
						streamParams = new XtStreamParams(true, SampleCollector::audioCallbackAvgInt, null, null);
					}
					XtDeviceStreamParams deviceStreamParams = new XtDeviceStreamParams(streamParams, format, bufferSize.current);
					//System.out.println("buff_size: " + bufferSize.current);
					stream = currDevice.openStream(deviceStreamParams, mainBuffer);
					XtSafeBuffer safe = XtSafeBuffer.register(stream);
					isActive = true;
					stream.start();
					
					//stop();
				}
				catch (Exception e) {
					System.err.println("Error while opening selected device");
					e.printStackTrace();
				}
			});
		}
		catch(InterruptedException e) {
			System.err.println("Error while opening selected device");
			e.printStackTrace();
		}
		
	}
	static int audioCallbackLeft(XtStream stream, XtBuffer buffer, Object user) throws Exception {
		XtSafeBuffer safe = XtSafeBuffer.get(stream);
		if(safe == null)
			return -1;
		MainBuffer mainBuffer = (MainBuffer)user;
		//if(mainBuffer.bufferCurrentSize + buffer.frames >= mainBuffer.bufferMaxSize) {
		//	System.err.println("audio callback aborted due to main buffer overflow");
		//	stream.stop();
		//	return 0;
		//}
		safe.lock(buffer);
		float[] audio = (float[])safe.getInput();
		float[] resSamples = new float[buffer.frames];
		for(int i = 0; i < buffer.frames * 2; i += 2) {
			resSamples[i/2] = audio[i];
		}
		mainBuffer.addNewSamples(resSamples, buffer.frames);			
		safe.unlock(buffer);
		return 0;		
	}
	static int audioCallbackRight(XtStream stream, XtBuffer buffer, Object user) throws Exception {
		XtSafeBuffer safe = XtSafeBuffer.get(stream);
		if(safe == null)
			return -1;
		MainBuffer mainBuffer = (MainBuffer)user;
		//if(mainBuffer.bufferCurrentSize + buffer.frames >= mainBuffer.bufferMaxSize) {
		//	System.err.println("audio callback aborted due to main buffer overflow");
		//	return 0;
		//}
		safe.lock(buffer);
		float[] audio = (float[])safe.getInput();
		float[] resSamples = new float[buffer.frames];
		for(int i = 0; i < buffer.frames * 2; i += 2) {
			resSamples[i/2] = audio[i+1];
		}
		mainBuffer.addNewSamples(resSamples, buffer.frames);			
		safe.unlock(buffer);
		return 0;		
	}
	static int audioCallbackAvg(XtStream stream, XtBuffer buffer, Object user) throws Exception {
		XtSafeBuffer safe = XtSafeBuffer.get(stream);
		if(safe == null)
			return -1;
		MainBuffer mainBuffer = (MainBuffer)user;
		//if(mainBuffer.bufferCurrentSize + buffer.frames >= mainBuffer.bufferMaxSize) {
		//	System.err.println("audio callback aborted due to main buffer overflow");
		//	return 0;
		//}
		safe.lock(buffer);
		float[] audio = (float[])safe.getInput();
		float[] resSamples = new float[buffer.frames];
		for(int i = 0; i < buffer.frames * 2; i += 2) {
			resSamples[i/2] = (audio[i] + audio[i+1])/2;
		}
		mainBuffer.addNewSamples(resSamples, buffer.frames);			
		safe.unlock(buffer);
		return 0;		
	}
	static int audioCallbackAvgInt(XtStream stream, XtBuffer buffer, Object user) throws Exception {
		XtSafeBuffer safe = XtSafeBuffer.get(stream);
		if(safe == null)
			return -1;
		MainBuffer mainBuffer = (MainBuffer)user;
		//if(mainBuffer.bufferCurrentSize + buffer.frames >= mainBuffer.bufferMaxSize) {
		//	System.err.println("audio callback aborted due to main buffer overflow");
		//	return 0;
		//}
		safe.lock(buffer);
		float[] audio;

		int[] audioTmp = (int[])safe.getInput();
		audio = new float[audioTmp.length];
		for(int i = 0; i < audio.length; i++) {
			audio[i] = audioTmp[i]*multiplier;
		}
		System.out.println(audio[0]);
		
		float[] resSamples = new float[buffer.frames];
		for(int i = 0; i < buffer.frames * 2; i += 2) {
			resSamples[i/2] = (audio[i] + audio[i+1])/2;
		}
		mainBuffer.addNewSamples(resSamples, buffer.frames);			
		safe.unlock(buffer);
		return 0;		
	}
	
	public int stop() {	
		try {
			queue.put(() -> {
				stream.stop();
			});
			isActive = false;
			
		} catch (Exception e) {
			el.errorOccured(e.getMessage());
		}	
		return 0;

	}

	static void onError(String message) {
        System.out.println("error: " + message);
    }
		
}
