/**
 * Controls the client half of the chat
 * program. Client can enter a username,
 * address, and port before connecting.
 * Once a server opens a connection 
 * clients can finish the connection 
 * and enter a chatroom controlled by the 
 * server until either they or the server
 * disconnect. Multiple clients can connect 
 * to one server.
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
import java.net.ConnectException;
import java.net.Socket;
import java.util.Random;

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

public class ChatClient extends JFrame implements ActionListener {
	private Socket _clientSocket;
	
	private DataOutputStream _outToServer;
	private BufferedReader _inFromUser;
	
	private String _clientSentence;
	private String _serverSentence;
	
	//panel containing all other panels
	private JPanel _pContainer = new JPanel(new BorderLayout());
	//panel containing chat setup and disconnection elements
	private JPanel _pNorth = new JPanel(new GridLayout(1, 7));
	//panel containing message sending elements
	private JPanel _pSouth = new JPanel(new BorderLayout());
	
	//used to give each guest a different username
	Random rand = new Random();
	int id = rand.nextInt(10000);
	
	private JLabel _username = new JLabel("Username:");
	private JTextField _usernameEntry = new JTextField("Guest" + id);
	private JLabel _host = new JLabel("Host:");
	private JTextField _hostEntry = new JTextField("127.0.0.1");
	private JLabel _port = new JLabel("Port:");
	private JTextField _portEntry = new JTextField("4444");
	private JButton _connect = new JButton("Connect");
	private JTextArea _chat = new JTextArea("");
	private JTextField _chatEntry = new JTextField("");
	private JButton _send = new JButton("Send");
	
	//panel containing chat area
	private JScrollPane _pCenter = new JScrollPane(this._chat);
	
	/**
	 * sets up the client GUI
	 */
	public ChatClient(){
		this._pNorth.add(this._username);
		this._pNorth.add(this._usernameEntry);
		this._pNorth.add(this._host);
		this._pNorth.add(this._hostEntry);
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
	 * looks for responses 4 times a second
	 */
	private ActionListener taskPerformer = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			checkInput();
		}
	};
	
	/**
	 * sets up reader and writer elements if not set up or
	 * checks for messages on open reader
	 */
	private void checkInput() {
		try{
			//sets up streams if not set up
			if(this._outToServer == null){
				this.initConnection();
			}
			//displays message if one has been received from server
			if(this._inFromUser.ready()){
				this._serverSentence = this._inFromUser.readLine();
				
				if(this._serverSentence != null){
					//if server exits client also disconnects
					if(this._serverSentence.equals("EXIT")){
						this.exitRoom();
					}
					else
						this._chat.append(this._serverSentence + "\n");
				}
			}
		}
		catch(ConnectException e){
			JOptionPane.showMessageDialog(null, "Chat room is currently full");
			
			this.exitRoom();
		}
		catch(Exception e){
			JOptionPane.showMessageDialog(null, "Unknown Exception: " + e.toString());
		}	
	}

	//handles automatic message checking
	private Timer _frameHandler = new Timer(250, taskPerformer);
	
	/**
	 * removes the client from the current chatroom
	 */
	private void exitRoom(){
		try {
			this._clientSocket.close();
		}
		catch(IOException ex){
			//closing a socket requires IOException throw or handle
		}
		
		this._clientSocket = null;
		this._inFromUser = null;
		this._outToServer = null;
		
		this._usernameEntry.setEditable(true);
		this._hostEntry.setEditable(true);
		this._portEntry.setEditable(true);
		
		this._send.setEnabled(false);
		this._chatEntry.setEditable(false);
		
		this._frameHandler.stop();
		
		this._connect.setText("Connect");
	}
	
	/**
	 * activates when a button is pressed
	 * directing the flow to the correct button.
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
	 * initializes connection to host
	 * 
	 * @throws ConnectionException - chatroom full
	 */
	private void initConnection() throws IOException, ConnectException, InterruptedException {
			this._outToServer = 
					new DataOutputStream(this._clientSocket.getOutputStream());
		
			this._inFromUser = new BufferedReader
					(new InputStreamReader(this._clientSocket.getInputStream()));
			
			this._outToServer.writeBytes(this._usernameEntry.getText() + "\n");
			
			//gives server time to respond
			Thread.sleep(1000);
			
			if(this._inFromUser.ready()){
				this._serverSentence = this._inFromUser.readLine();
				
				if(this._serverSentence.equals("FULL"))
					throw new ConnectException("Chat room is currently full");
				
				this._chat.append(this._serverSentence + "\n");
			}
			else
				throw new ConnectException("Chat room is currently full");		
	}
	
	/**
	 * attempts to connect to a server
	 */
	private void connect(){
		try{
			this._clientSocket = 
					new Socket(this._hostEntry.getText(), Integer.parseInt(this._portEntry.getText())); 
			
			this._connect.setText("Disconnect");
			this._chatEntry.setEditable(true);
			this._send.setEnabled(true);
			
			this._usernameEntry.setEditable(false);
			this._hostEntry.setEditable(false);
			this._portEntry.setEditable(false);
			
			this._frameHandler.start();
		}
		//called if host cannot be found at address and port
		catch(ConnectException e){
			JOptionPane.showMessageDialog(null, "Host " + this._hostEntry.getText() + " at Port " + this._portEntry.getText() + " not found.");	
		}
		catch(Exception e){
			JOptionPane.showMessageDialog(null, "Unknown Exception: " + e.toString());
		}
	}
	

	/**
	 * disconnects the user from the server.
	 */
	private void disconnect() {
		try { 
			if(this._clientSocket.isConnected()){
				this._clientSentence = this._usernameEntry.getText() + " has left the room.\n";
			
				this._chat.append("You have left the room.\n");
				
				this._outToServer.writeBytes(this._clientSentence);

				this._clientSentence = "EXIT";
			
				this._outToServer.writeBytes(this._clientSentence);
			}
		}
		catch(java.net.SocketException e) {
			//called if server can't be reached
		}
        catch(Exception e) {  
        	JOptionPane.showMessageDialog(null, "Unknown Exception: " + e.toString());
        }
		
		this.exitRoom();
	}
	
	/**
	 * Sends whatever the user has
	 * typed in the chat area to the
	 * server.
	 */
	private void send() {
		//prevents users from entering exit command
		if(this._chatEntry.getText().equals("EXIT"))
			this._chatEntry.setText("EXIT ");
		
		try { 
			this._clientSentence = this._usernameEntry.getText() + ": " + this._chatEntry.getText() + "\n";
			
			this._outToServer.writeBytes(this._clientSentence);
			
			this._chat.append("You: " + this._chatEntry.getText() + "\n");
			
			this._chatEntry.setText("");
		}
		catch(java.net.SocketException e) {	
			this._chat.append("Connection to host ended abruptly\n");
			this._chatEntry.setText("");
			
			this.exitRoom();
		}
        catch(Exception e){  
        	JOptionPane.showMessageDialog(null, "Unknown Exception: " + e.toString());
        }
	}
}