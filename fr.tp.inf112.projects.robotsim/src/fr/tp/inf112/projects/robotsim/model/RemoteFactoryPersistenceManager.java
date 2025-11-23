package fr.tp.inf112.projects.robotsim.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

import fr.tp.inf112.projects.canvas.controller.Observer;
import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import fr.tp.inf112.projects.canvas.model.impl.AbstractCanvasPersistenceManager;

/**
 * Remote persistence manager for the robotic factory simulator.
 * Communicates with a FactoryPersistenceServer to save and load Factory models.
 */
public class RemoteFactoryPersistenceManager extends AbstractCanvasPersistenceManager {
	private static final Logger LOGGER = Logger.getLogger(RemoteFactoryPersistenceManager.class.getName());
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 8888;
	private static final String LIST_COMMAND = "LIST";
	
	private final String serverHost;
	private final int serverPort;
	
	public RemoteFactoryPersistenceManager(CanvasChooser canvasChooser) {
		this(canvasChooser, DEFAULT_HOST, DEFAULT_PORT);
	}
	
	public RemoteFactoryPersistenceManager(CanvasChooser canvasChooser, String serverHost, int serverPort) {
		super(canvasChooser);
		this.serverHost = serverHost;
		this.serverPort = serverPort;
	}
	
	@Override
	public void persist(Canvas canvas) throws IOException {
		if (!(canvas instanceof Factory)) {
			throw new IOException("Canvas is not a Factory instance");
		}
		
		Factory factory = (Factory) canvas;
		String canvasId = factory.getId();
		
		LOGGER.info("Persisting factory to remote server: " + canvasId);
		
		try (Socket socket = new Socket(serverHost, serverPort);
		     ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
		     ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
			
			// Send factory to server
			output.writeObject(factory);
			output.flush();
			
			// Read response
			Object response = input.readObject();
			
			if (response instanceof String && "SUCCESS".equals(response)) {
				LOGGER.info("Successfully persisted factory: " + canvasId);
			} else if (response instanceof Exception) {
				throw new IOException("Server error: " + ((Exception) response).getMessage());
			} else {
				throw new IOException("Unexpected response from server: " + response);
			}
			
		} catch (ClassNotFoundException e) {
			throw new IOException("Class not found: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Legacy method for backward compatibility.
	 */
	public boolean persist(Canvas canvas, String canvasId) {
		if (!(canvas instanceof Factory)) {
			LOGGER.severe("Canvas is not a Factory instance");
			return false;
		}
		
		Factory factory = (Factory) canvas;
		
		// Set the factory ID before persisting
		if (canvasId != null && !canvasId.isEmpty()) {
			factory.setId(canvasId);
		}
		
		LOGGER.info("Persisting factory to remote server: " + factory.getId());
		
		try (Socket socket = new Socket(serverHost, serverPort);
		     ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
		     ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
			
			// Send factory to server
			output.writeObject(factory);
			output.flush();
			
			// Read response
			Object response = input.readObject();
			
			if (response instanceof String && "SUCCESS".equals(response)) {
				LOGGER.info("Successfully persisted factory: " + factory.getId());
				return true;
			} else if (response instanceof Exception) {
				LOGGER.severe("Server error: " + ((Exception) response).getMessage());
				return false;
			} else {
				LOGGER.severe("Unexpected response from server: " + response);
				return false;
			}
			
		} catch (IOException e) {
			LOGGER.severe("Failed to persist factory: " + e.getMessage());
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			LOGGER.severe("Class not found: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public Canvas read(String canvasId) throws IOException {
		LOGGER.info("Reading factory from remote server: " + canvasId);
		
		try (Socket socket = new Socket(serverHost, serverPort);
		     ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
		     ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
			
			// Send read request to server
			output.writeObject(canvasId);
			output.flush();
			
			// Read response
			Object response = input.readObject();
			
			if (response instanceof Factory) {
				Factory factory = (Factory) response;
				LOGGER.info("Successfully read factory: " + canvasId);
				return factory;
			} else if (response instanceof Exception) {
				throw new IOException("Server error: " + ((Exception) response).getMessage());
			} else {
				throw new IOException("Unexpected response from server: " + response);
			}
			
		} catch (ClassNotFoundException e) {
			throw new IOException("Class not found: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Request list of available model files from the server.
	 */
	public String[] listModels() {
		LOGGER.info("Requesting model list from remote server");
		
		try (Socket socket = new Socket(serverHost, serverPort);
		     ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
		     ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
			
			// Send LIST command to server
			output.writeObject(LIST_COMMAND);
			output.flush();
			
			// Read response
			Object response = input.readObject();
			
			if (response instanceof String[]) {
				String[] models = (String[]) response;
				LOGGER.info("Received list of " + models.length + " models");
				return models;
			} else {
				LOGGER.severe("Unexpected response from server: " + response);
				return new String[0];
			}
			
		} catch (IOException e) {
			LOGGER.severe("Failed to list models: " + e.getMessage());
			e.printStackTrace();
			return new String[0];
		} catch (ClassNotFoundException e) {
			LOGGER.severe("Class not found: " + e.getMessage());
			e.printStackTrace();
			return new String[0];
		}
	}
	
	@Override
	public boolean delete(Canvas canvas) throws IOException {
		// Not implemented as it's never called by the simulator UI
		LOGGER.warning("Delete operation not implemented for remote persistence");
		return false;
	}
}
