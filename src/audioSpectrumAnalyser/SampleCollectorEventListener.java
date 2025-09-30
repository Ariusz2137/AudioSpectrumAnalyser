package audioSpectrumAnalyser;

public interface SampleCollectorEventListener {
	public void samplesBufferAvailable(float[] samples);
	
	public void totalSamplesNumberChanged(int newSize);
	
	public void sampleRateChanged(int newSampleRate);
	
	public void hostChanged(String newHost);
	
	public void deviceChanged(String newDevice);
	
	public void errorOccured(String message);
}
