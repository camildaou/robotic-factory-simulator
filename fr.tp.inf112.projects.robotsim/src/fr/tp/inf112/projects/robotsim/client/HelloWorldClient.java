package fr.tp.inf112.projects.robotsim.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Simple "Hello World" client that sends messages to the server
 * and receives responses.
 */
public class HelloWorldClient {
	private static final Logger LOGGER = Logger.getLogger(HelloWorldClient.class.getName());
	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 8080;
	
	private final String host;
	private final int port;
	
	public HelloWorldClient(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	public String sendMessage(String message) throws IOException {
		LOGGER.info("Connecting to server at " + host + ":" + port);
		
		try (Socket socket = new Socket(host, port);
		     ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
		     ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
			
			// Send message to server
			LOGGER.info("Sending message: " + message);
			output.writeObject(message);
			output.flush();
			
			// Read response from server
			Object responseObject = input.readObject();
			
			if (responseObject instanceof String) {
				String response = (String) responseObject;
				LOGGER.info("Received response: " + response);
				return response;
			} else {
				throw new IOException("Unexpected response type: " + responseObject.getClass().getName());
			}
			
		} catch (ClassNotFoundException e) {
			throw new IOException("Class not found while deserializing response", e);
		}
	}
	
	public static void main(String[] args) {
		String host = DEFAULT_HOST;
		int port = DEFAULT_PORT;
		String message = "Hello World";
		
		if (args.length > 0) {
			message = args[0];
		}
		if (args.length > 1) {
			host = args[1];
		}
		if (args.length > 2) {
			try {
				port = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {
				LOGGER.severe("Invalid port number: " + args[2]);
				System.exit(1);
			}
		}
		
		HelloWorldClient client = new HelloWorldClient(host, port);
		
		try {
			String response = client.sendMessage(message);
			System.out.println("Server response: " + response);
		} catch (IOException e) {
			LOGGER.severe("Failed to communicate with server: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
