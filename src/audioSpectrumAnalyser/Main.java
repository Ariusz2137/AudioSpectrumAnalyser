package audioSpectrumAnalyser;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import com.formdev.flatlaf.FlatDarkLaf;

public class Main {
	public static void main(String[] args){
		System.setProperty("sun.java2d.uiScale.enabled", "false"); //ignore scaling
		BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<>();		
		FlatDarkLaf.setup();
		MainPanel mp = new MainPanel(blockingQueue);
		JFrame frame = new JFrame("Audio spectrum analyzer");
		try {
			frame.setIconImage(ImageIO.read(new File("res/icon.png")));
		} catch (IOException e) {
			System.err.println("App icon not found");
		}
		frame.add(mp);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setMinimumSize(new Dimension(1600, 300));
		frame.setVisible(true);
		while (true) {
			try {
            Runnable task = blockingQueue.take();
            task.run();
			} catch (InterruptedException e) {
				break;
			}

        }
	}
}
