package audioSpectrumAnalyser;

import java.util.HashMap;

public interface VisualiserEventListener {
	public void streamStopRequested();
	
	public void streamStartRequested();
	
	public void settingsFieldChanged(String field, String value);
	
	public HashMap<String, String> xmlSettingsRequested();
}
