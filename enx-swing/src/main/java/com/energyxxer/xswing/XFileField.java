package com.energyxxer.xswing;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;

public class XFileField extends JPanel {

	private XTextField field;
	private XButton button;
	private byte operation = OPEN_ALL;
	protected File value = new File(System.getProperty("user.home"));
	private String dialogTitle = "Open...";

	public static final byte OPEN_ALL = 0;
	public static final byte OPEN_FILE = 1;
	public static final byte OPEN_DIRECTORY = 2;
	public static final byte SAVE = 3;
	
	{
		setLayout(new BorderLayout());
		setOpaque(false);
		field = new XTextField();
		button = new XButton("Browse...");
		button.setPreferredSize(new Dimension(100,25));

		button.addActionListener(e -> {

			LookAndFeel laf = UIManager.getLookAndFeel();

			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException x) {
				x.printStackTrace();
			}



			JFileChooser jfc = new JFileChooser();
			int[] modes = new int[] {JFileChooser.FILES_AND_DIRECTORIES,JFileChooser.FILES_ONLY,JFileChooser.DIRECTORIES_ONLY,JFileChooser.FILES_AND_DIRECTORIES};
			jfc.setFileSelectionMode(modes[operation]);
			jfc.setCurrentDirectory(value);
			jfc.setDialogTitle(dialogTitle);
			int result = jfc.showSaveDialog(null);
			if (result == JFileChooser.APPROVE_OPTION) {
				value = jfc.getSelectedFile();
				field.setText(value.getAbsolutePath());
			}

			try {
				UIManager.setLookAndFeel(laf);
			} catch(UnsupportedLookAndFeelException x) {
				x.printStackTrace();
			}
		});

		field.getDocument().addDocumentListener(new DocumentListener() {

			protected void update() {
				switch(operation) {
					case OPEN_ALL: {
						File file = new File(field.getText());
						if(file.exists()) XFileField.this.value = file;
						break;
					} case OPEN_FILE: {
						File file = new File(field.getText());
						if(file.exists() && file.isFile()) XFileField.this.value = file;
						break;
					} case OPEN_DIRECTORY: {
						File file = new File(field.getText());
						if(file.exists() && file.isDirectory()) XFileField.this.value = file;
						break;
					} case SAVE: {
						XFileField.this.value = new File(field.getText());
						break;
					}
				}
			}

			@Override
			public void changedUpdate(DocumentEvent arg0) {
				update();
			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				update();
			}
		});

		this.add(field, BorderLayout.CENTER);
		this.add(button, BorderLayout.EAST);
	}

	public XFileField() {}

	public XFileField(byte operation) {
		setOperation(operation);
	}

	public XFileField(File file) {
		setFile(file);
	}

	public XFileField(byte operation, File file) {
		setFile(file);
		setOperation(operation);
	}

	public void setOperation(byte operation) {
		this.operation = operation;
	}

	public byte getOperation() {return this.operation;}
	
	public void setFile(File file) {
		if(file != null) {
			field.setText(file.getAbsolutePath());
			this.value = file;
		} else {
			File dFile = new File(System.getProperty("user.home"));
			field.setText(dFile.getAbsolutePath());
			this.value = dFile;
		}
	}
	
	public File getFile() {
		return new File(field.getText());
	}
	
	public void setBorderColor(Color bc, int thickness) {
		field.setBorder(bc, thickness);
	}

	public void setDialogTitle(String title) {
		this.dialogTitle = title;
	}

	public XTextField getField() {
		return field;
	}

	public XButton getButton() {
		return button;
	}
}
