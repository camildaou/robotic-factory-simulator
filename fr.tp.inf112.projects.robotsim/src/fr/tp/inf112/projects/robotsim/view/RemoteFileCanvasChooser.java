package fr.tp.inf112.projects.robotsim.view;

import java.awt.Component;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import fr.tp.inf112.projects.canvas.view.FileCanvasChooser;
import fr.tp.inf112.projects.robotsim.model.RemoteFactoryPersistenceManager;

/**
 * Remote canvas chooser that allows users to select factory models
 * stored on a remote server.
 */
public class RemoteFileCanvasChooser extends FileCanvasChooser {
	private static final Logger LOGGER = Logger.getLogger(RemoteFileCanvasChooser.class.getName());
	private static final long serialVersionUID = 1L;
	private static final char EXTENSION_SEPARATOR_CHAR = '.';
	
	private final RemoteFactoryPersistenceManager remotePersistenceManager;
	private final String extension;
	
	public RemoteFileCanvasChooser(String extension, String description, 
	                                RemoteFactoryPersistenceManager remotePersistenceManager) {
		super(extension, description);
		this.extension = extension;
		this.remotePersistenceManager = remotePersistenceManager;
	}
	
	@Override
	protected String browseCanvases(boolean open) throws IOException {
		if (open) {
			// Opening a file - show list of available models from server
			return browseRemoteModelsForOpen(getViewer());
		} else {
			// Saving a file - prompt for filename
			return browseRemoteModelsForSave(getViewer());
		}
	}
	
	private String browseRemoteModelsForOpen(Component parent) {
		LOGGER.info("Browsing remote models for opening");
		
		// Get list of available models from server
		String[] models = remotePersistenceManager.listModels();
		
		if (models == null || models.length == 0) {
			JOptionPane.showMessageDialog(parent, 
				"No factory models found on the server.", 
				"No Models Available", 
				JOptionPane.INFORMATION_MESSAGE);
			return null;
		}
		
		// Show selection dialog
		Object selectedModel = JOptionPane.showInputDialog(
			parent,
			"Select a factory model to open:",
			"Open Factory Model",
			JOptionPane.QUESTION_MESSAGE,
			null,
			models,
			models[0]
		);
		
		if (selectedModel != null) {
			String modelId = selectedModel.toString();
			LOGGER.info("User selected model: " + modelId);
			return modelId;
		}
		
		return null;
	}
	
	private String browseRemoteModelsForSave(Component parent) {
		LOGGER.info("Browsing remote models for saving");
		
		// Prompt user for filename
		String filename = JOptionPane.showInputDialog(
			parent,
			"Enter a name for the factory model:",
			"Save Factory Model",
			JOptionPane.QUESTION_MESSAGE
		);
		
		if (filename != null && !filename.trim().isEmpty()) {
			// Ensure .factory extension
			if (!filename.endsWith(EXTENSION_SEPARATOR_CHAR + extension)) {
				filename = filename + EXTENSION_SEPARATOR_CHAR + extension;
			}
			
			LOGGER.info("User entered filename: " + filename);
			return filename;
		}
		
		return null;
	}
}
