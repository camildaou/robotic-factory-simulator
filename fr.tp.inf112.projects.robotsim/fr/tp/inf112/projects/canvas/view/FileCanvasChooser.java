package fr.tp.inf112.projects.canvas.view;

import java.awt.Component;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import fr.tp.inf112.projects.canvas.model.CanvasChooser;

/**
 * A canvas chooser that internally uses a {@code JFileChooser} to browse the file system and let the user chose a file into which the 
 * canvas model is stored.
 * @author Dominique Blouin
 *
 */
public class FileCanvasChooser implements CanvasChooser {
	
	/**
	 * The file extension separator character.
	 */
	private static final char EXTENSION_SEPARATOR_CHAR = '.';


	/**
	 * The UI component that will serve as the parent of the {@code JFileChooser} displayed to the user. This is typically
	 * a Java AWT component such as a {@code CanvasViewer} object. 
	 */
	private Component viewer;
	
    /**
     * Used by the {@code JFileChooser} to filter the files presented to the user according to specific file extensions.
     */
    private final FileNameExtensionFilter fileNameFilter;
    
    /**
     * Construct a canvas chooser object using a given file extension and a label for documents of this file extension.
     * @param fileExtension A non {@code null} {@code String} for the desired file extension
     * @param documentTypeLabel A {@code String} label to be displayed for documents of this file extension. For example,
     * the label would be "Word" for *.docx documents.
     */
    public FileCanvasChooser(final String fileExtension,
    						 final String documentTypeLabel) {
    	this(null, fileExtension, documentTypeLabel);
    }

	public FileCanvasChooser(final Component viewer,
							 final String fileExtension,
							 final String documentTypeLabel) {
		this.viewer = viewer;
		
		if (fileExtension == null) {
			throw new IllegalArgumentException("File extension cannot be null.");
		}

		fileNameFilter = new FileNameExtensionFilter(documentTypeLabel + " files " + "(*." + fileExtension + ")", fileExtension);
	}

	public Component getViewer() {
		return this.viewer;
	}
	
	public void setViewer(Component viewer) {
		this.viewer = viewer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String choseCanvas() 
	throws IOException {
		return browseCanvases(true);
	}

	protected String browseCanvases(final boolean open) 
	throws IOException {
		final JFileChooser chooser = new JFileChooser(); 
	    chooser.setFileFilter(fileNameFilter);
	    
	    final int returnVal;
	    
	    if (open) {
	    	returnVal = chooser.showOpenDialog(viewer);
	    }
	    else {
	    	returnVal = chooser.showSaveDialog(viewer);
	    }
	    
	    if (returnVal == JFileChooser.APPROVE_OPTION) {
	    	final File file = chooser.getSelectedFile();
	    	
	    	return file.getPath();
	    }
	    
	    return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String newCanvasId()
	throws IOException {
		String canvasId = browseCanvases(false);
    	
    	if (canvasId != null) {
    		if (!isFileExtensionValid(canvasId)) {
    			canvasId = canvasId.concat(EXTENSION_SEPARATOR_CHAR + getFileExtension());
    		}
        	
        	if (!isValid(canvasId)) {
    			throw new IOException("Invalid canvas file name '" + canvasId + "'.");
        	}
    	}
    	
    	return canvasId;
	}
	
	private boolean isValid(final String canvasId) {
		return canvasId != null && !canvasId.isEmpty() && isFileExtensionValid(canvasId);
	}
	
	private boolean isFileExtensionValid(final String canvasId) {
		return canvasId.endsWith(EXTENSION_SEPARATOR_CHAR + getFileExtension());
	}
	
	private String getFileExtension() {
		return fileNameFilter.getExtensions()[0];
	}
}
