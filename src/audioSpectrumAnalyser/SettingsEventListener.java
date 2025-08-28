package audioSpectrumAnalyser;

public interface SettingsEventListener {
	public void fpsChanged(int newFps);
	
	public void overlappingWindowsChanged(int newOverlap);
	
	public void stereoModeChanged(int newMode);
	
	public void startRequested();
	
	public void stopRequested();
	
	public void windowFunctionChanged(int newFunction);
	
	public void sensivityChanged(int newSens);
	
	public void setFullConfig(int fps, int Overlap, int stereoMode);
	
	public void setBandsNumber(int newBands);
	
	public void setHost(String hostName);
	
	public void setDevice(String devName);
}
