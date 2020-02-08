import java.io.*;
import java.net.*;
import java.util.*;

/**
* This class allows the server to interact with the client.
* It contains the following primary features:
* <ol>
* 	<li> Listen for new connections from clients. </li>
* 	<li> Handle connections from multiple clients.</li>
* 	<li> Respond to client requests </li>
* 	<li> Broadcast chat to all clients </li>
* </ol>
* @author Michael Zhou ; Student ID: 201031238 ; Computer Science Username: u6mz
* @version Dec 10, 2017
*/
public class Server {
	private static final int portNumber = 5555; // The port that the server listens on
	private String welcome = "Please type your username.";
	private String accepted = "Your username is accepted.";
	private String nameChange = "What would you like to be called instead then?";
	private String[] commands = { "\\help", "\\quit" , "\\numberclients", "\\servertime", "\\clienttime", "\\ipaddress", "\\changename" };
	private String[] description = { "Shows all the commands you can prompt",
										"Disconnects you from the server",
										"Check how many clients are currently connected to the chat",
										"Shows how long the server has been running for",
										"Shows how long you have been in the chat server",
										"Reveals the server's IP Address",
										"Change your name"
									};
	private int count = 0;
	private int tag = 0;
	private long startTime = 0;
	private long activeTime;
	private String ipAddress;
	private ServerSocket ss; // for the method "shutDown"

	/**
	* The set of all the print writers for all the clients to broadcast messages.
	*/
	private HashSet<String> clientNames = new HashSet<String>();
	private HashSet<PrintWriter> clientWriters
	= new HashSet<PrintWriter>(); // for many clients

	/**
	 * This is the application main method,
	 * @param args Unused.
	 * @throws IOException On input error.
	 * @see IOException
	 */
	public static void main (String[] args) throws IOException {
		Server server = new Server();
		server.start();
	}

	/**
	* Creates a new server that can accept connections from new clients.
	* which just listens on a port and creates a handler thread.
	* @throws IOException On input error.
	*/
	void start() throws IOException {
		ss = new ServerSocket( portNumber );
		InetAddress ip = InetAddress.getLocalHost();
		ipAddress = ip.getHostAddress();
		System.out.println( "Echo server at "
			+ ipAddress + " is waiting for connections ..." );
		Socket socket;
		Thread thread;
		startTime = System.currentTimeMillis();
		try {
			while ( true ) {
				socket = ss.accept();
				thread = new Thread( new HandleSession( socket, ipAddress ) );
				thread.start();
			}
		}
		catch (Exception e)  {
			System.out.println( e.getMessage() );
		}
		finally {
			shutDown();
		}
	}

	/**
	 * This issues a confirmation message to inform that the servers will shut down.
	 */
	public void shutDown() {
		try {
			ss.close();
			System.out.println( "The server is shutting down." );
		}
		catch (Exception e) {
			System.err.println( "Problem shutting down the server." );
			System.err.println( e.getMessage() );
		}
	}
	/**
	 * This is a handler thread class.
	 * It is responsible for dealing with the clients broadcasting of messages.
	 */
	class HandleSession implements Runnable {
		private Socket socket;
		private String ip;
		String name; // for the current client
		BufferedReader in = null;
		PrintWriter out = null;

		/**
		 * This is the constructor for the handler thread.
		 * @param socket variable to store socket that will be created.
		 * @param ip passes the IP address of the server that the client wants to join.
		 */
		HandleSession (Socket socket, String ip) {
			this.socket = socket;
			this.ip = ip;
		}

		/**
		 * Services this thread's client by repeatedly requesting the client to prompt a new
		 * username from a client until a unique one has been submitted, then acknowledges the name
		 * and registers the output stream for the client, then repeatedly gets inputs and broadcasts them.
		 */
		public void run() {
			try {
				createStreams();
				getClientUserName();
				listenForClientMessages();
			}
			catch (IOException e) {
				System.out.println( e );
			}
			finally {
				closeConnection();
			}
		} // end of run() in the class HandleSession

		/**
		 * This method creates streams for the socket.
		 * Listens for new connections from clients.
		 */
		private void createStreams() {
			try {
				in = new BufferedReader( new
					InputStreamReader( socket.getInputStream() ) );
				out = new PrintWriter( new
					OutputStreamWriter( socket.getOutputStream() ) );
				clientWriters.add( out );
				System.out.println( "One connection has been detected attempting to join the chat." );
			}
			catch (IOException e) {
				System.err.println( "Exception in createStreams(): " + e );
			}
		} // end of createStreams() in the class HandleSession

		/**
		 * This method requests a valid username from the client. It will
		 * check if the username has been already taken up in the chat or not.
		 * @throws IOException on input error.
		 */
		private void getClientUserName() throws IOException {
			while ( true ) {
				out.println( welcome ); out.flush();
				try { name = in.readLine(); }
				catch (IOException e) {
					System.err.println( "Username can not be accepted." );
					System.err.println("Exception in getClientUserName: " + e);
				}
				if ( name == null ) return;
				if ( ! clientNames.contains( name))  {
					clientNames.add( name );
					break;
				}
				out.println( "Sorry, this username is unavailable");
				out.flush();
			}
			// Now that a successful name has been chosen, add the socket's print writer to the set of all
			// writers so this client can receive broadcast messages otherwise the client may not see the message
			out.println( accepted + "Please feel free to type messages." );
			out.flush();
			System.out.println( name + " has entered the chat.");
			activeTime = System.currentTimeMillis();
			count++;
			out.flush();
		}	// end of getClientUserName() in the class HandleSession

		/**
		 * This method prompts user for a new username after requesting a name change.
		 * @throws IOException on input error.
		 */
		private void getNewClientUserName() throws IOException {
			while ( true ) {
				out.println( welcome );
				out.flush();
				try { name = in.readLine(); }
				catch (IOException e) {
					System.err.println("Exception in getNewClientUserName: " + e );
				}
				if ( name == null ) return;
				if ( ! clientNames.contains( name ))  {
					clientNames.add( name );
					break;
				}
				out.println( "Sorry, this username is unavailable" );
				out.flush();
			}
				// Now that a successful name has been chosen, the socket's print writer to the set of all writers is added.
				out.flush();
				broadcast ( "The client has now been renamed to " + name + ".");
				out.flush();
				// Confirm that the client can receive broadcast messages otherwise the client may not see the message
				out.println( "Congratulations, you have now successfully been renamed to " + name + "." );
				out.println( "Please feel free to type messages." );
		}	// end of getClientUserName() in the class HandleSession

		/**
		 * This method sends all broadcasting of chat messages to all clients.
		 * @throws IOException on input error.
		 */
		private void listenForClientMessages() throws IOException {
			String line; // input from a remote client
			while ( in != null ) {
				line = in.readLine();
				if ( line == null ) break;
				if ( line.startsWith("\\") ) {
					if ( ! processClientRequest( line ) ) return;
				}
				else broadcast( name + " has said: " + line);
			}
		} // end of listenForClientMessages() in the class HandleSession

		/**
		 * This method handles the broadcasting of messages from each client.
		 * The socket's print writer is added to the set of all print writers so
		 * a client can receive new broadcast messages.
		 * @param message String variable to store the broadcast of messages.
		 */
		private void broadcast (String message)
		{
			for (PrintWriter writer : clientWriters) {
				writer.println( message ); writer.flush();
			}
			System.out.println( message );
		} // end of broadcast() in the class HandleSession

		/**
		 * Depending on what command that the client has requested, this method could:
		 * <ul>
		 * <li> Show how long the server has been running for measured in seconds </li>
		 * <li> Show how long the client has been in the chat for measured in second </li>
		 * <li> Reveal the server's IP Address </li>
		 * <li> Show how many clients in total are currently connected to the chat </li>
		 * </ul>
		 * @param command String variable that holds all the commands the client can use.
		 * @return A list of commands the client can use to access more features.
		 * @throws IOException On input error.
		 */
		private boolean processClientRequest (String command) throws IOException {
			// allows the client to change their username
			if ( command.equals("\\changename") ) {
				broadcast ( name + " is currently requesting a name change." );
				out.flush();
				while ( true ) {
					out.println( nameChange );
					out.flush();
					clientNames.remove(name);
					getNewClientUserName();
					break;
				}
			}
			// reveals the IP address of the server that they are currently connected to
			if ( command.equals( "\\ipaddress" ) ) {
				out.println( "IP address of server is: " + ip );
				out.flush();
			}
			// reveals how long the client has spent within the chat
			if ( command.equals( "\\clienttime" ) ) {
				long clientRunningTime = System.currentTimeMillis() - activeTime;
				out.println( name + " has been active for: " + clientRunningTime/1000 + " seconds" );
				out.flush();
			}
			// reveals how long the server has been running for
			if ( command.equals("\\servertime") ) {
				long serverRunningTime = System.currentTimeMillis() - startTime;
				out.println("The server has been running for: " + serverRunningTime/1000 + " seconds");
				out.flush();
			}
			// reveals the number of clients that are currently connected to the chat
			if ( command.equals("\\numberclients") ) {
				out.println("The number of clients currently in the chat is: " + count);
				out.flush();
			}
			// disconnects the client from the chat.
			if ( command.equals("\\quit") )
				return false;
				out.flush();
			//this prints out a list of commands the user can have access to
			if ( command.equals("\\help") ) {
				while(tag<7){
					for (String c : commands) {
						out.println( "Command " + c  + " - " + description[tag]);
						tag++;
						}
						out.flush();
					}
			} return true;
		} // end of processClientRequest() in the class HandleSession

		/**
		 * This allows the server to handle the client disconnecting abruptly in a graceful way.
		 * The client that has disconnected will have their name removed and closes the socket.
		 */
		private void closeConnection() {
			if ( name != null ) {
				// removes the client names from the set
				broadcast( name + " has left the chat." );
				clientNames.remove( name );
				count--;
			}
			// removes the client's print writers from the set
			if ( out != null ) {
				clientWriters.remove( out );
			}
			// closes the socket
			try {
				socket.close();
			}
			catch (IOException e) {
				System.err.println( "Exception when closing the socket" );
				System.err.println( e.getMessage() );
			}
		} // end of closeConnection() in the class HandleSession

	} // end of the class HandleSession

} // end of the class ServerWithClient
