package audioSpectrumAnalyser;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SettingsPanel extends JPanel {

	private GridBagConstraints c;
	private GridBagLayout gridBag;
	
	private SettingsEventListener el;
	
	// ++default settings
	private int fps = 60;
	private int overlap = 10;
	private int stereoMode = 0;
	private int windowFunc = 1;
	private int sensivityMin = 0;
	private int sensivityMax = 100;
	private int sensivity = 50;
	
	private String xmlVisSettingsNodeName = "VisSettings";
	
	private final String xmlFilename = "config.xml";
	private Color fieldsErrBgColor = new Color(100, 0, 0);
	// --default settings
	
	private Element visSettingsElement;
	
	private String currHostName;
	private String currDeviceName;
	
	private Color fieldsBackgrColor;
	
	public SettingsPanel() {
		readXmlFile();
		gridBag = new GridBagLayout();
		c = new GridBagConstraints();
		this.setLayout(gridBag);
		addToGrid(new JLabel("Target FPS:"), 0, 0, 1);
		
		JFormattedTextField targetFPSField = constructNumField(fps);
		targetFPSField.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				
				try {
					if(Integer.parseInt(targetFPSField.getText()) < 1)
	                    targetFPSField.setBackground(fieldsErrBgColor);
	                else{
	                    targetFPSField.setBackground(fieldsBackgrColor);
	                    el.fpsChanged(Integer.parseInt(targetFPSField.getText()));
	                    updateXmlFile("fps", targetFPSField.getText());
	                } 
				} catch (NumberFormatException ex) {
					targetFPSField.setBackground(fieldsErrBgColor);
				}
				
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				try {
					if(Integer.parseInt(targetFPSField.getText()) < 1)
	                    targetFPSField.setBackground(fieldsErrBgColor);
	                else{
	                    targetFPSField.setBackground(fieldsBackgrColor);
	                    el.fpsChanged(Integer.parseInt(targetFPSField.getText()));
	                    updateXmlFile("fps", targetFPSField.getText());
	                } 
				} catch (NumberFormatException ex) {
					targetFPSField.setBackground(fieldsErrBgColor);
				}
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {}
		});
		
		addToGrid(targetFPSField, 0, 1, 1);
		JPanel targetFPSPlusMinus = constructPlusMinus(targetFPSField);
		addToGrid(targetFPSPlusMinus, 0, 2, 1);
		
		addToGrid(new JLabel("Window Overlap:"), 1, 0, 1);
		JFormattedTextField subBufferField = constructNumField(overlap);
		subBufferField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				try {
					if(Integer.parseInt(subBufferField.getText()) < 1) {
						subBufferField.setBackground(fieldsErrBgColor);
					}
	                else{
	                	subBufferField.setBackground(fieldsBackgrColor);
	                    el.overlappingWindowsChanged(Integer.parseInt(subBufferField.getText()));
	                    updateXmlFile("overlap", subBufferField.getText());
	                } 
				} catch (NumberFormatException ex) {
					subBufferField.setBackground(fieldsErrBgColor);
				}
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				try {
					if(Integer.parseInt(subBufferField.getText()) < 1) {
						subBufferField.setBackground(fieldsErrBgColor);
					}
	                else{
	                	subBufferField.setBackground(fieldsBackgrColor);
	                	el.overlappingWindowsChanged(Integer.parseInt(subBufferField.getText()));
	                	updateXmlFile("overlap", subBufferField.getText());
	                } 
				} catch (NumberFormatException ex) {
					subBufferField.setBackground(fieldsErrBgColor);
				}
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {}
		});
		addToGrid(subBufferField, 1, 1, 1);
		JPanel subBufferPlusMinus = constructPlusMinus(subBufferField);
		addToGrid(subBufferPlusMinus, 1, 2, 1);
		
		addToGrid(new JLabel("Stereo mix mode:"), 3, 0, 1);
		JComboBox<String> stereoModeBox = new JComboBox<String>();
		stereoModeBox.addItem("Average");
		stereoModeBox.addItem("Left only");
		stereoModeBox.addItem("Right only");
		stereoModeBox.setSelectedIndex(stereoMode);
		stereoModeBox.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				el.stereoModeChanged(stereoModeBox.getSelectedIndex());
				updateXmlFile("stereoMode", stereoModeBox.getSelectedIndex());
			}
		});
		addToGrid(stereoModeBox, 3, 1, 2);
		
		addToGrid(new JLabel("Window function"), 4, 0, 1);
		JComboBox<String> windowFunction = new JComboBox<String>();
		windowFunction.addItem("Rectengular (none)");
		windowFunction.addItem("Hann");
		windowFunction.addItem("Hamming");
		windowFunction.addItem("Blackman");
		windowFunction.setSelectedIndex(windowFunc);
		windowFunction.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				el.windowFunctionChanged(windowFunction.getSelectedIndex());
				updateXmlFile("windowFunc", windowFunction.getSelectedIndex());
			}
		});
		addToGrid(windowFunction, 4, 1, 2);
		
		JSlider sensivity = new JSlider(SwingConstants.HORIZONTAL, sensivityMin, sensivityMax, this.sensivity);
		sensivity.addChangeListener(new ChangeListener() {		
			@Override
			public void stateChanged(ChangeEvent e) {
				el.sensivityChanged(sensivity.getValue());	
				updateXmlFile("sensivity", sensivity.getValue());
			}
		});
		addToGrid(new JLabel("Sensivity:"), 5, 0, 1);
		addToGrid(sensivity, 5, 1, 2);
		fieldsBackgrColor = targetFPSField.getBackground();
	}
	public void setHostName(String hostName) {
		this.currHostName = hostName;
		updateXmlFile("hostName", hostName);
	}
	public void setDeviceName(String deviceName) {
		this.currDeviceName = deviceName;
		updateXmlFile("deviceName", deviceName);
	}
	
	private File file;
	private DocumentBuilderFactory factory;
	private Document doc;
	private DocumentBuilder builder;
	private Transformer transformer;
	private NodeList elems;
	
	private String[] xmlNodesNames;
	
	private void readXmlFile() {
		file = new File(xmlFilename);
		factory = DocumentBuilderFactory.newInstance();
		
		try {
			builder = factory.newDocumentBuilder();
			if(file.exists()) {
				doc = builder.parse(file);
				Element root = doc.getDocumentElement();
				elems = root.getChildNodes();
				Node node;
				
				xmlNodesNames = new String[elems.getLength()];
				transformer = TransformerFactory.newInstance().newTransformer();
				for(int i = 0; i < elems.getLength(); i++) {
					node = elems.item(i);
					//System.out.printf("name: %s value: %s\n", node.getNodeName(), node.getTextContent());
					if(node.getNodeName() == xmlVisSettingsNodeName) {
						visSettingsElement = (Element)node;
						continue;
					}
					
					switch (node.getNodeName().trim()) {	
					case "fps": {
						fps = Integer.parseInt(node.getTextContent());
						break;
					}
					case "overlap": {
						overlap = Integer.parseInt(node.getTextContent());
						break;
					}
					case "stereoMode": {
						 stereoMode = Integer.parseInt(node.getTextContent());
						 break;
					}
					case "windowFunc": {
						windowFunc = Integer.parseInt(node.getTextContent());
						break;
					}
					case "sensivityMin": {
						sensivityMin = Integer.parseInt(node.getTextContent());
						break;
					}
					case "sensivityMax": {
						sensivityMax = Integer.parseInt(node.getTextContent());
						break;
					}
					case "sensivity": {
						sensivity = Integer.parseInt(node.getTextContent());
						break;
					}
					case "hostName": {
						currHostName = node.getTextContent();
						break;
					}
					case "deviceName": {
						currDeviceName = node.getTextContent();
						break;
					}
					case "visPanel": {
						continue;
					}
					default:
						System.err.println("Unexpected value: " + node.getNodeName());
						continue;
					}
					xmlNodesNames[i] = node.getNodeName();
				}
			}
			else {
				doc = builder.newDocument();
				doc.appendChild(doc.createElement("SettingsPanel"));
				xmlNodesNames = new String[] {"fps", "overlap", "stereoMode", "windowFunc", "sensivityMin", "sensivityMax", "sensivity", "hostName", "deviceName"};
				
				currHostName = new String();
				currDeviceName = new String();
				
				addToDocument(doc, xmlNodesNames[0], Integer.toString(fps));
				addToDocument(doc, xmlNodesNames[1], Integer.toString(overlap));
				addToDocument(doc, xmlNodesNames[2], Integer.toString(stereoMode));
				addToDocument(doc, xmlNodesNames[3], Integer.toString(windowFunc));
				addToDocument(doc, xmlNodesNames[4], Integer.toString(sensivityMin));
				addToDocument(doc, xmlNodesNames[5], Integer.toString(sensivityMax));
				addToDocument(doc, xmlNodesNames[6], Integer.toString(sensivity));
				addToDocument(doc, xmlNodesNames[7], currHostName);
				addToDocument(doc, xmlNodesNames[8], currDeviceName);
				
				visSettingsElement = doc.createElement(xmlVisSettingsNodeName);
				
				doc.getDocumentElement().appendChild(visSettingsElement);
				
				elems = doc.getChildNodes().item(0).getChildNodes();
				transformer = TransformerFactory.newInstance().newTransformer();
				transformer.transform(new DOMSource(doc), new StreamResult(file));
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch(NumberFormatException e) {
			System.err.println("Invalid value in XML file");
			e.printStackTrace();
		}
	}
	private void updateXmlFile(String name, int value) {
		for(int i = 0; i < xmlNodesNames.length; i++) {
			if(name.equals(xmlNodesNames[i])) {
				elems.item(i).setTextContent(Integer.toString(value));
				try {
					transformer.transform(new DOMSource(doc), new StreamResult(file));
				} catch (TransformerException e) {
					e.printStackTrace();
				}
				return;
			}
		}
		System.err.println("Unknown XML node name: " + name);
	}
	private void updateXmlFile(String name, String value) {
		for(int i = 0; i < xmlNodesNames.length; i++) {
			if(name.equals(xmlNodesNames[i])) {
				elems.item(i).setTextContent(value);
				try {
					transformer.transform(new DOMSource(doc), new StreamResult(file));
				} catch (TransformerException e) {
					e.printStackTrace();
				}
				return;
			}
		}
		System.err.println("Unknown XML node name: " + name);
	}
	
	private void addToDocument(Document doc, String name, String value) {
		Element element = doc.createElement(name);
		element.setTextContent(value);
		doc.getDocumentElement().appendChild(element);
	}
	
	public void addEventListener(SettingsEventListener el) {
		this.el = el;
		el.setFullConfig(fps, overlap, stereoMode);
		el.windowFunctionChanged(windowFunc);
		el.sensivityChanged(sensivity);
		if(!currHostName.equals(""))
			el.setHost(currHostName);
		if(!currHostName.equals("") && !currDeviceName.equals(""))
			el.setDevice(currDeviceName);
	}
	public HashMap<String, String> getVisSettings(){
		HashMap<String, String> res = new HashMap<String, String>();
		NodeList nodeList = visSettingsElement.getChildNodes();
		for(int i = 0; i < nodeList.getLength(); i++) {
			res.put(nodeList.item(i).getNodeName(), nodeList.item(i).getTextContent());
		}	
		return res;
	}
	public void saveVisSetting(String name, String value) {
		NodeList nodeList = visSettingsElement.getChildNodes();
		boolean newNode = true;
		for(int i = 0; i < nodeList.getLength(); i++) {
			if(nodeList.item(i).getNodeName().equals(name)) {
				nodeList.item(i).setTextContent(value);
				newNode = false;
			}
		}
		if(newNode) {
			Element element = doc.createElement(name);
			element.setTextContent(value);
			visSettingsElement.appendChild(element);
		}
		try {
			transformer.transform(new DOMSource(doc), new StreamResult(file));
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return;
	}
	
    private JFormattedTextField constructNumField(int dflt){
        JFormattedTextField res = new JFormattedTextField();
        res.setText(Integer.toString(dflt));
        res.setColumns(3);
        return res;
    }
    private JPanel constructPlusMinus(JFormattedTextField field){
        JPanel res = new JPanel();
        res.setLayout(new BoxLayout(res, BoxLayout.X_AXIS));
        res.setPreferredSize(new Dimension(100, 25));
        JButton plus = new JButton("+");
        JButton minus = new JButton("-");
        plus.setFocusable(false);
        minus.setFocusable(false);
        plus.setPreferredSize(new Dimension(100, 25));
        minus.setPreferredSize(new Dimension(100 ,25));
        plus.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){   
                try{
                	field.setText(Integer.toString(Integer.parseInt(field.getText()) +1));
                }
                catch(NumberFormatException ex){
                    System.err.println("number parsing failed");
                }
            }
        });
        minus.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                try{
                    field.setText(Integer.toString(Integer.parseInt(field.getText()) - 1));
                }
                catch(NumberFormatException ex){
                    System.err.println("number parsing failed");
                }
            }
        });
        
        res.add(plus);
        res.add(minus);
        return res;
    }
	
	private void addToGrid(java.awt.Component comp, int y, int x, int width){
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = width;
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.BOTH;
        gridBag.setConstraints(comp, c);
        this.add(comp);
    }
}
