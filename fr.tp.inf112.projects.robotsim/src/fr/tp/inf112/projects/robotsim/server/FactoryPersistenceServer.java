package fr.tp.inf112.projects.robotsim.server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import fr.tp.inf112.projects.robotsim.model.Factory;
import fr.tp.inf112.projects.robotsim.model.FactoryPersistenceManager;

/**
 * Remote persistence server for the robotic factory simulator.
 * Handles requests to save and load Factory models.
 * 
 * Protocol:
 * - Receives String: Read factory model with that ID/filename
 * - Receives Factory: Save factory model with its ID as filename
 * - Receives "LIST": Return array of available model filenames
 */
public class FactoryPersistenceServer {
	private static final Logger LOGGER = Logger.getLogger(FactoryPersistenceServer.class.getName());
	private static final int DEFAULT_PORT = 8888;
	private static final String STORAGE_DIR = "server_models";
	private static final String LIST_COMMAND = "LIST";
	
	private final int port;
	private final FactoryPersistenceManager localPersistenceManager;
	private boolean running;
	
	public FactoryPersistenceServer(int port) {
		this.port = port;
		this.localPersistenceManager = new FactoryPersistenceManager(null);
		this.running = false;
		
		// Create storage directory if it doesn't exist
		File storageDir = new File(STORAGE_DIR);
		if (!storageDir.exists()) {
			storageDir.mkdirs();
			LOGGER.info("Created storage directory: " + STORAGE_DIR);
		}
	}
	
	public void start() throws IOException {
		running = true;
		
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			LOGGER.info("FactoryPersistenceServer started on port " + port);
			LOGGER.info("Storage directory: " + new File(STORAGE_DIR).getAbsolutePath());
			
			while (running) {
				LOGGER.info("Waiting for client connection...");
				
				try (Socket clientSocket = serverSocket.accept()) {
					LOGGER.info("Client connected: " + clientSocket.getInetAddress());
					
					handleClient(clientSocket);
					
				} catch (IOException e) {
					if (running) {
						LOGGER.severe("Error handling client: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private void handleClient(Socket clientSocket) throws IOException {
		try (ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
		     ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream())) {
			
			// Read request from client
			Object receivedObject = input.readObject();
			
			if (receivedObject instanceof String) {
				String request = (String) receivedObject;
				
				if (LIST_COMMAND.equals(request)) {
					// List available models
					handleListRequest(output);
				} else {
					// Read factory model
					handleReadRequest(request, output);
				}
				
			} else if (receivedObject instanceof Factory) {
				// Save factory model
				handleSaveRequest((Factory) receivedObject, output);
				
			} else {
				LOGGER.warning("Received unknown object type: " + receivedObject.getClass().getName());
				output.writeObject(new IOException("Unknown request type"));
				output.flush();
			}
			
		} catch (ClassNotFoundException e) {
			LOGGER.severe("Class not found while deserializing: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void handleReadRequest(String modelId, ObjectOutputStream output) throws IOException {
		LOGGER.info("READ request for model: " + modelId);
		
		try {
			String fullPath = STORAGE_DIR + File.separator + modelId;
			Factory factory = (Factory) localPersistenceManager.read(fullPath);
			
			output.writeObject(factory);
			output.flush();
			
			LOGGER.info("Successfully sent factory model: " + modelId);
			
		} catch (Exception e) {
			LOGGER.severe("Error reading factory model: " + e.getMessage());
			output.writeObject(e);
			output.flush();
		}
	}
	
	private void handleSaveRequest(Factory factory, ObjectOutputStream output) throws IOException {
		LOGGER.info("SAVE request for factory: " + factory.getId());
		
		try {
			// The persist method now only takes Canvas parameter
			// The factory ID already contains the filename
			localPersistenceManager.persist(factory);
			
			// Send success response
			output.writeObject("SUCCESS");
			output.flush();
			
			LOGGER.info("Successfully saved factory model: " + factory.getId());
			
		} catch (Exception e) {
			LOGGER.severe("Error saving factory model: " + e.getMessage());
			output.writeObject(e);
			output.flush();
		}
	}
	
	private void handleListRequest(ObjectOutputStream output) throws IOException {
		LOGGER.info("LIST request received");
		
		try {
			File storageDir = new File(STORAGE_DIR);
			String[] files = storageDir.list((dir, name) -> name.endsWith(".factory"));
			
			if (files == null) {
				files = new String[0];
			}
			
			output.writeObject(files);
			output.flush();
			
			LOGGER.info("Sent list of " + files.length + " files");
			
		} catch (Exception e) {
			LOGGER.severe("Error listing files: " + e.getMessage());
			output.writeObject(new String[0]);
			output.flush();
		}
	}
	
	public void stop() {
		running = false;
		LOGGER.info("Server stopped");
	}
	
	public static void main(String[] args) {
		int port = DEFAULT_PORT;
		
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				LOGGER.severe("Invalid port number: " + args[0]);
				System.exit(1);
			}
		}
		
		FactoryPersistenceServer server = new FactoryPersistenceServer(port);
		
		// Add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOGGER.info("Shutting down server...");
			server.stop();
		}));
		
		try {
			server.start();
		} catch (IOException e) {
			LOGGER.severe("Failed to start server: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
