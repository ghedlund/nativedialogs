package ca.phon.ui.nativedialogs.demo;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 * Demo frame for native dialogs.
 *
 */
public class NativeDialogsDemo extends JFrame {
	
	private static final long serialVersionUID = 6418535207662766158L;
	
	private SaveDialogPanel saveDialogPanel;
	
	private OpenDialogPanel openDialogPanel;
	
	private MessageDialogPanel messageDialogPanel;
	
	private FontDialogPanel fontDialogPanel;
	
	private ColorDialogPanel colorDialogPanel;

	public NativeDialogsDemo() {
		super("Native Dialogs Demo");
		
		init();
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	private void init() {
		final JTabbedPane tabPane = new JTabbedPane();
		
		saveDialogPanel = new SaveDialogPanel();
		tabPane.addTab("Save Dialog", saveDialogPanel);
		
		openDialogPanel = new OpenDialogPanel();
		tabPane.addTab("Open Dialog", openDialogPanel);
		
		messageDialogPanel = new MessageDialogPanel();
		tabPane.addTab("Message Dialog", messageDialogPanel);
		
		fontDialogPanel = new FontDialogPanel();
		tabPane.addTab("Font Dialog", fontDialogPanel);
		
		colorDialogPanel = new ColorDialogPanel();
		tabPane.addTab("Color Dialog", colorDialogPanel);
		
		setLayout(new BorderLayout());
		add(tabPane, BorderLayout.CENTER);
	}
	
	/**
	 * main
	 */
	public static void main(String[] args) {
		final Runnable onEDT = new Runnable() {
			
			@Override
			public void run() {
				final NativeDialogsDemo demo = new NativeDialogsDemo();
				demo.pack();
				demo.setVisible(true);
			}
		};
		SwingUtilities.invokeLater(onEDT);
	}
	
}
