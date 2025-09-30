package audioSpectrumAnalyser;

public interface Visualiser {
	public void update(float[] fftRes);
	
	public void setBandsNum(int newBandsNum);
	
	public void setSampleRate(int newSampleRate);
	
	public void setBuffSize(int newBuffSize);
	
	public void setSensivity(int newSens);
	
	public void showError(String message);
	
	public void addEventListener(VisualiserEventListener el);
	
}
