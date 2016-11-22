/**
 * Controls the server half of the program.
 * User on the server end choose a username
 * and a port to open server on. Pressing connect
 * will open the server up to client connections. 
 * Once a connection comes through each member of
 * the chatroom can send messages amongst each other. 
 * If server disconnects chatroom will disconnect all 
 * clients.
 * 
 * @author Johnathan McNutt
 */
package com.jmcnutt.chat;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

public class ChatServer extends JFrame implements ActionListener, Runnable {	
	//sets the maximum number of clients that can connect to the chatroom
	private final int MAXIMUM_CLIENTS = 10;
	
	//stores the clients usernames
	private String[] _usernames = new String[MAXIMUM_CLIENTS];
	
	private ServerSocket _serverSocket;
	private Socket[] _connectionSocket = new Socket[MAXIMUM_CLIENTS];
	
	private DataOutputStream[] _outToClient = new DataOutputStream[MAXIMUM_CLIENTS];
	private BufferedReader[] _inFromClient = new BufferedReader[MAXIMUM_CLIENTS];
	
	private String _serverSentence = "";
	private String _clientSentence = "";
	
	//panel containing all other panels
	private JPanel _pContainer = new JPanel(new BorderLayout());
	//panel containing chat setup and disconnection elements
	private JPanel _pNorth = new JPanel(new GridLayout(1, 5));
	//panel containing message sending elements
	private JPanel _pSouth = new JPanel(new BorderLayout());
	
	private JLabel _username = new JLabel("Username:");
	private JTextField _usernameEntry = new JTextField("Host");
	private JLabel _port = new JLabel("Port:");
	private JTextField _portEntry = new JTextField("4444");
	private JButton _connect = new JButton("Connect");
	private JTextArea _chat = new JTextArea("");
	private JTextField _chatEntry = new JTextField("");
	private JButton _send = new JButton("Send");
	
	//panel containing chat area
	private JScrollPane _pCenter = new JScrollPane(this._chat);
	
	/**
	 * constructs the server JFrame GUI.
	 */
	public ChatServer(){
		this._pNorth.add(this._username);
		this._pNorth.add(this._usernameEntry);
		this._pNorth.add(this._port);
		this._pNorth.add(this._portEntry);
		this._pNorth.add(this._connect);
		
		this._pSouth.add(this._chatEntry, BorderLayout.CENTER);
		this._pSouth.add(this._send, BorderLayout.EAST);
		
		this._pCenter.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		this._pContainer.add(this._pNorth, BorderLayout.NORTH);
		this._pContainer.add(this._pCenter, BorderLayout.CENTER);
		this._pContainer.add(this._pSouth, BorderLayout.SOUTH);
		
		//chat functionality disabled until connection is made
		this._chat.setEditable(false);
		this._chatEntry.setEditable(false);
		this._send.setEnabled(false);	
		
		//adds listener for button click
		this._connect.addActionListener(this);
		this._send.addActionListener(this);
		
		//adds everything to the window
		this.add(this._pContainer);
	}
	
	/**
	 * check for new messages 4 times each second.
	 */
	private ActionListener taskPerformer = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			checkInput();
		}
	};
	
	/**
	 * Checks for messages from the clients. If a message is received
	 * relays the message to the rest of the clients.
	 */
	private void checkInput() {
		for(int i = 0; i < MAXIMUM_CLIENTS; i++) {
			try{
				if(this._connectionSocket[i] != null){
					//if message received from client displays message
					if (this._inFromClient[i].ready()){
						this._clientSentence = this._inFromClient[i].readLine();
						
						if(this._clientSentence != null){
							//if client disconnects
							if(this._clientSentence.equals("EXIT")){					
								this.removeClient(i);
							}
							else {
								this._chat.append(this._clientSentence + "\n");
								
								this.relayMessage(this._clientSentence, i);
							}
						}
					}
				}
			}//end try
			catch(java.net.SocketException ex) {
				//Called if client is unreachable
			}
			catch(Exception ex){
				JOptionPane.showMessageDialog(null, "Unknown Exception: " + ex.toString());
			}
		}//end for loop
	}
	
	//handles automatic message checking
	private Timer _frameHandler = new Timer(250, taskPerformer);
	
	/**
	 * sends a message to all clients, except the original sender
	 * 
	 * @param message - String to send
	 * @param senderIndex - index of sender. -1 if host is sender
	 */
	private void relayMessage(String message, int senderIndex){
		for(int i = 0; i < MAXIMUM_CLIENTS; i++) {
			//send the sentence to all clients except original sender
			if(i != senderIndex) {
				//try block to prevent broken connection from interrupting message sending
				try {
					if(this._connectionSocket[i] != null){
						this._outToClient[i].writeBytes(message + "\n");
					}
				}
				//Called if client is unreachable
				catch(java.net.SocketException ex) {
					String s = this._usernames[i] + " has left the room";
					
					this._chat.append(s + "\n");
					
					this.removeClient(i);
					
					//sends that a user has disconnect to all clients
					this.relayMessage(s, -1);
				}
				catch(Exception ex){
					JOptionPane.showMessageDialog(null, "Unknown Exception: " + ex.toString());
				}
			}
		}//end for loop
	}
	
	/**
	 * removes a single client from the chatroom by index.
	 * 
	 * @param clientIndex - the index of the client to be removed
	 */
	private void removeClient(int clientIndex) {
		if(this._connectionSocket[clientIndex] != null) {
			try {
				this._connectionSocket[clientIndex].close();
			}
			catch(IOException ex) {
				//closing a socket requires IOException throw or handle
			}
			
			this._connectionSocket[clientIndex] = null;
			this._inFromClient[clientIndex] = null;
			this._outToClient[clientIndex] = null;
			
			this._usernames[clientIndex] = null;
		}
	}
	
	/**
	 * a separate thread of execution so looking for a connection doesn't freeze the GUI.
	 * If a connection is made a new thread is created to check for more connections.
	 */
	public void run(){
		try {
			Socket socket;
			socket = this._serverSocket.accept();
			
			int newConnection;
			for(newConnection = 0; newConnection < MAXIMUM_CLIENTS; newConnection++) {
				if(this._connectionSocket[newConnection] == null)
					break;
			}
			
			//checks if there was a free connection
			if(newConnection != MAXIMUM_CLIENTS) {
				this.connectClient(socket, newConnection);
			}
			//sets up a rejection connection if full
			else {
				this.rejectClient(socket);
			}
			
			//begins new connection thread so more clients can be connected
			Thread connectionThread = new Thread(this);
			connectionThread.start();
		}//end try
		catch (IOException ex) {
			//activates if connection is closed before client is found
		}
		catch(NullPointerException ex) {
			//activates if connection is closed while searching for more clients
		}
		catch(Exception ex){
			JOptionPane.showMessageDialog(null, "Unknown Exception: " + ex.toString());
		}
	}//end run method
	
	/**
	 * connects a single client to the chatroom.
	 * 
	 * @param socket - socket client is accepted from
	 * @param index - free array index to set up connection on
	 */
	private void connectClient(Socket socket, int index) throws IOException {
		this._connectionSocket[index] = socket;
		
		this._inFromClient[index] = new BufferedReader
			(new InputStreamReader(this._connectionSocket[index].getInputStream()));
			
		this._outToClient[index] = 
			new DataOutputStream(this._connectionSocket[index].getOutputStream());
		
		this._clientSentence = this._inFromClient[index].readLine();
		
		this._usernames[index] = this._clientSentence;
		
		this._clientSentence += " joined the room";
		
		this._chat.append(this._clientSentence + "\n");
		
		this.relayMessage(this._clientSentence, -1);
	}
	
	/**
	 * rejects a client from connecting to the chatroom.
	 * 
	 * @param socket - socket with accepted client
	 */
	private void rejectClient(Socket socket) throws IOException {
		try {
			DataOutputStream das = 
					new DataOutputStream(socket.getOutputStream());
			
			das.writeBytes("FULL\n");
			
			//gives client time to receive message
			Thread.sleep(1000);
			
			socket.close();
		}
		catch(InterruptedException ex){
			//activates if Thread is interrupted during sleep
			socket.close();
		}
	}
	
	/**
	 * activates when the user presses a button.
	 * determines what button was pressed.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("Connect")){
			this.connect();
		}
		else if(e.getActionCommand().equals("Disconnect")){
			this.disconnect();
		}
		else if(e.getActionCommand().equals("Send")){
			this.send();
		}
	}

	/**
	 * opens connections for clients.
	 */
	private void connect(){
		try{
			this._serverSocket = 
					new ServerSocket(Integer.parseInt(this._portEntry.getText())); 
			
			this._connect.setText("Disconnect");
			this._chatEntry.setEditable(true);
			this._send.setEnabled(true);
			
			this._usernameEntry.setEditable(false);
			this._portEntry.setEditable(false);
			
			//starts a new thread that looks for client connections
			Thread connectionThread = new Thread(this);
			connectionThread.start();
			
			//starts looking for data from clients
			this._frameHandler.start();
			
			this._chat.append("Waiting on Connection\n");
		}
		catch(java.net.BindException e) {
			JOptionPane.showMessageDialog(null, "Port " + this._portEntry.getText() + " Already in use");
		}
		catch(Exception e){
			JOptionPane.showMessageDialog(null, "Unknown Exception: " + e.toString());
		}
	}

	/**
	 * disconnects the user from the clients.
	 */
	private void disconnect() {
		this._serverSentence = this._usernameEntry.getText() + " has left the room.";
		
		this.relayMessage(this._serverSentence, -1);
		this.relayMessage("EXIT", -1);
		
		for(int i = 0; i < MAXIMUM_CLIENTS; i++)
			this.removeClient(i);
		
		try { 
			this._serverSocket.close();
		}
        catch(IOException e){  
        	//catch required to close socket
        }
			
		this._serverSocket = null;
		this._chat.append("You have left the room.\n");
		
		this._usernameEntry.setEditable(true);
		this._portEntry.setEditable(true);
		
		this._send.setEnabled(false);
		this._chatEntry.setEditable(false);
		
		this._connect.setText("Connect");
		
		this._frameHandler.stop();
	}
	
	/**
	 * Sends whatever message the user has typed
	 * into the chat area to all clients connected.
	 */
	private void send() {
		//prevents users from entering exit command
		if(this._chatEntry.getText().equals("EXIT"))
			this._chatEntry.setText("EXIT ");
		
		this._serverSentence = this._usernameEntry.getText() + ": " + this._chatEntry.getText();
		
		this._chat.append("You: " + this._chatEntry.getText() + "\n");
		
		this._chatEntry.setText("");
		
		this.relayMessage(this._serverSentence, -1);
	}
}//end ChatServer class