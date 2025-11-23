package fr.tp.inf112.projects.robotsim.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Simple "Hello World" server that receives messages from clients
 * and responds with "I received [message]!".
 */
public class HelloWorldServer {
	private static final Logger LOGGER = Logger.getLogger(HelloWorldServer.class.getName());
	private static final int DEFAULT_PORT = 8080;
	
	private final int port;
	private boolean running;
	
	public HelloWorldServer(int port) {
		this.port = port;
		this.running = false;
	}
	
	public void start() throws IOException {
		running = true;
		
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			LOGGER.info("HelloWorldServer started on port " + port);
			
			while (running) {
				LOGGER.info("Waiting for client connection...");
				
				try (Socket clientSocket = serverSocket.accept()) {
					LOGGER.info("Client connected: " + clientSocket.getInetAddress());
					
					handleClient(clientSocket);
					
				} catch (IOException e) {
					if (running) {
						LOGGER.severe("Error handling client: " + e.getMessage());
					}
				}
			}
		}
	}
	
	private void handleClient(Socket clientSocket) throws IOException {
		try (ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
		     ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream())) {
			
			// Read message from client
			Object receivedObject = input.readObject();
			
			if (receivedObject instanceof String) {
				String message = (String) receivedObject;
				LOGGER.info("Received message: " + message);
				
				// Send response back to client
				String response = "I received " + message + "!";
				output.writeObject(response);
				output.flush();
				
				LOGGER.info("Sent response: " + response);
			} else {
				LOGGER.warning("Received non-String object: " + receivedObject.getClass().getName());
			}
			
		} catch (ClassNotFoundException e) {
			LOGGER.severe("Class not found while deserializing: " + e.getMessage());
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
		
		HelloWorldServer server = new HelloWorldServer(port);
		
		try {
			server.start();
		} catch (IOException e) {
			LOGGER.severe("Failed to start server: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
