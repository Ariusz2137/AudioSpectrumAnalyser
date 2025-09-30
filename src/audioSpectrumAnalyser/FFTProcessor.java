package audioSpectrumAnalyser;

import java.util.Arrays;

import org.jtransforms.fft.FloatFFT_1D;

public class FFTProcessor{
	
	private interface ProcessfftResults {
		float[] execute(float[] data);
	}
	private static class ProcessfftResultsEven implements ProcessfftResults{
		public float[] execute(float data[]) {
			float res[] = new float[data.length/2];
			for(int i = 0; i < data.length - 2; i += 2) {
				res[i/2] = (float)(Math.sqrt(Math.pow(data[i+2], 2)  + Math.pow(data[i+3], 2))*10000/data.length*Math.sqrt(i/2*freqSpacing)); 	//turns out that multiplying each bin by cubic root of its index + 1 actually makes result flat on pink noise, which 
			}																																//is similar to how humans perceive frequencies loudness, found it randomly
			res[data.length/2-1] = Math.abs(data[1]);
			return res;
		}
	}
	private static class ProcessfftResultsOdd implements ProcessfftResults{
		public float[] execute(float data[]) {
			float res[] = new float[(data.length- 1) / 2];
			for(int i = 0; i < data.length - 3; i += 2) {
				res[i/2] = (float)(Math.sqrt(Math.pow(data[i+2], 2)  + Math.pow(data[i+3], 2)) *10000/data.length*Math.sqrt(i/2*freqSpacing));
			}
			res[res.length - 1] = (float)Math.sqrt(Math.pow(data[data.length-1], 2)  + Math.pow(data[1], 2));
			return res;
		}
	}
	
	private int windowFunction; //0 for none, 1 for Hann, 2 for Hamming, 3 for Blackman
	private int sampleRate;
	private static float freqSpacing;
	
	private ProcessfftResults processfftResults;
	
	private int samplesNum;
	private float[] window;
	
	private FloatFFT_1D jTransFFT;
	
	public FFTProcessor() {
	}
	public void setSize(int size) {
		samplesNum = size;
		freqSpacing = sampleRate*1.0f/samplesNum;
		jTransFFT = new FloatFFT_1D(size);
		if(size % 2 == 0)
			processfftResults = new ProcessfftResultsEven();
		else 
			processfftResults = new ProcessfftResultsOdd();
		setWindow(windowFunction);
	
		
	}
	public void setWindow(int windowID) {
		windowFunction = windowID;
		window = new float[samplesNum];
		double a; 
		double c;
		switch (windowFunction) {
		case 0: {
			Arrays.fill(window, 1);
			break;
		}
		case 1:
			c = 2*Math.PI/(samplesNum-1);
			for(int i = 0; i < samplesNum; i++) {
				window[i] = (float)(0.5*(1-Math.cos(c*i))); //Hann window
			}
			break;
		case 2:
			a = 25.0d/46;
			c = 2*Math.PI/(samplesNum-1);
			for(int i = 0; i < samplesNum; i++) {
				window[i] = (float)(a - ((1-a)*Math.cos(c*i))); //Hamming window
			}
			break;
		case 3:
			c = 2*Math.PI/(samplesNum-1);
			double a0 = 7938.0d/18608;
			double a1 = 9240.0d/18608;
			double a2 = 1430.0d/18608;
			for(int i = 0; i < samplesNum; i++) {
				window[i] = (float)(a0 - a1 * Math.cos(c*i) + a2 * Math.cos(2*c*i)); //Blackman window
			}
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + windowFunction);
		}
		
	}
	
	public float[] run(float[] data) {
		//new Thread(()->{
			if(data.length != samplesNum)
				throw new IllegalArgumentException("array length (" + data.length + ") does not match declared size (" + samplesNum + ")");
			for(int i = 0; i < samplesNum; i++) {
				data[i] *= window[i];
			}
			
			jTransFFT.realForward(data);
			//System.out.println("raw data: " + Arrays.toString(data));
			float[] res = processfftResults.execute(data);
			//for(int i = 0; i < 20; i++) {
			//	System.out.println((48000/samplesNum*(i+1)) + "Hz - " + res[i]);
			//}
			return res;
		//}).start();
	}
	public void setSampleRate(int newSampleRate) {
		sampleRate = newSampleRate;
		if(samplesNum != 0) {
			freqSpacing = sampleRate*1.0f/samplesNum;
		}
	}
}
