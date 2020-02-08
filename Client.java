import java.io.*;
import java.net.*;

/**
* The program is a chat server that can connect to clients.
* This class contains a main method instantiating a new ClientInstance.
* @author Michael Zhou 
* @version Dec 10, 2017
*/
public class Client {
	public static void main(String[] args) throws Exception {
		ClientInstance client = new ClientInstance();
		client.start();
	}
}

/**
* This class creates a new client that can connect and interact with the server.
*/
class ClientInstance {
	private int portNumber = 5555;
	private String welcome = "Please type your username.";
	private String accepted = "Your username is accepted.";
	private Socket socket = null;
	private BufferedReader in;
	private PrintWriter out;
	private boolean isAllowedToChat = false;
	private boolean isServerConnected = false;
	private String clientName;

	/**
	 * Calls multiple methods to enable the client to connect to a server via a socket.
	 */
	public void start() {
		establishConnection();
		handleOutgoingMessages();
		handleIncomingMessages();
	}

	/**
	 * This prompts the client for the IP address of the server that they would like to join.
	 */
	private void establishConnection() {
			String serverAddress = getClientInput( "What is the address of the server that you wish to connect to?" );
			try {
				socket = new Socket( serverAddress, portNumber );
				in = new BufferedReader( new InputStreamReader( socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				isServerConnected = true;
			}
			catch (IOException e) {
				System.err.println( "You have failed to establish a connection." );
				System.err.println( "Exception in handleConnection(): " + e );
			}
			handleProfileSetUp();
		} // end of establishConnection() in the class ClientInstance

	/**
	 * This authorises the client that has joined the ability to chat.
	 * After client has successfully joined the chat, it confirms the
	 * client should be able to start broadcasting of messages.
	 */
	private void handleProfileSetUp() {
		String line = null;
		while ( ! isAllowedToChat ) {
			try { line = in.readLine(); }
			catch (IOException e) {
				System.err.println( "User failed to complete profile setup." );
				System.err.println( "Exception in handleProfileSetUp:" + e );
			}
			if ( line.startsWith( welcome ) ) {
				out.println( getClientInput( welcome ) );
			}
			else if (line.startsWith( accepted ) ) {
				isAllowedToChat = true;
				System.out.println( accepted +" You can now type messages." );
				System.out.println( "To see a list of commands, type \\help." );
			}
			else System.out.println( line );
		}
	}	// end of handleProfileSetUp()	in the class ClientInstance

	/**
	 * This method deals with all of the outgoing messages.
	 */
	private void handleOutgoingMessages() { //Sender thread
		Thread senderThread = new Thread( new Runnable(){
			public void run() {
				while ( isServerConnected  ){
					out.println( getClientInput( null ) );
				}
			}
		});
		senderThread.start();
	} // end of handleOutgoingMessages() in the class ClientInstance

	/**
	 * This method prompts the client for their input messages into the chat.
	 * @param hint A String variable passed through as an argument to store the client's prompted input.
	 * @return The client's input message.
	 */
	private String getClientInput (String hint) {
		String message = null;
		try {
			BufferedReader reader = new BufferedReader(
				new InputStreamReader( System.in ) );
			if ( hint != null ) { System.out.println( hint ); }
			message = reader.readLine();
			if ( ! isAllowedToChat ) { clientName = message; }
		}
		catch (IOException e) {
			System.err.println( "Message failed to send." );
			System.err.println( "Exception in getClientInput(): " + e );
		}
		return message;
	} // end of getClientInput() in the class ClientInstance

	/**
	 * This method handles the message responses from the server.
	 */
	private void handleIncomingMessages() { // Listener thread
		Thread listenerThread = new Thread( new Runnable() {
			public void run() {
				while ( isServerConnected ) {
					String line = null;
					try {
						line = in.readLine();
						if ( line == null ) {
							isServerConnected = false;
							System.err.println( "Disconnected from the server" );
							closeConnection();
							break;
						}
						System.out.println( line );
					}
					catch (IOException e) {
						isServerConnected = false;
						System.err.println( "The server has gone offline " );
						System.err.println( "IOE in handleIncomingMessages()" + e );
						break;
					}
				}
			}
		});
		listenerThread.start();
	} // end of handleIncomingMessages() in the class ClientInstance

	/**
	 * This method terminates the connection between the client and the server when finished.
	 */
	void closeConnection() {
		try {
			socket.close();
			System.exit(0); // finish the client program
		}
		catch (IOException e) {
			System.err.println( "Exception when closing the socket" );
			System.err.println( e.getMessage() );
		}
	} // end of closeConnection() in the class ClientInstance

} // end of the class ClientInstance
