/**
 * Driver for Simple GUI Chat program. Starts by giving
 * the user the option to be the server or the client
 * making the appropriate window. Server can connect to
 * multiple clients allowing all to exchange messages. 
 * Disconnecting the server from the chat will disconnect 
 * all clients.
 * 
 * @author Johnathan McNutt
 */
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.jmcnutt.chat.ChatClient;
import com.jmcnutt.chat.ChatServer;

public class ChatDriver extends JFrame implements ActionListener {

	/**
	 * Builds the starting window which contains
	 * two buttons, one to start the program
	 * as a server and the other starts the
	 * program as a client
	 */
	public ChatDriver(){
		JPanel p1 = new JPanel();
		p1.setLayout(new GridLayout(2, 1));
		
		JButton server = new JButton("Server");
		server.addActionListener(this);
		
		JButton client = new JButton("Client");
		client.addActionListener(this);
		
		p1.add(server);
		p1.add(client);
		
		this.add(p1);
	}
	
	/**
	 * begins the program by creating a JFrame window
	 */
	public static void main(String[] args) {
		JFrame newWindow = new ChatDriver();
	    newWindow.setTitle("Simple Chat");
	    newWindow.setSize(640, 480);
	    newWindow.setLocationRelativeTo(null);
	    newWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    newWindow.setVisible(true);
	}//end Main

	/**
	 * activates when either the Server or
	 * Client button are pressed
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("Server")){
			this.setVisible(false);
			
			JFrame serverWindow = new ChatServer();
			serverWindow.setTitle("Chat Server");
		    serverWindow.setSize(640, 480);
		    serverWindow.setLocationRelativeTo(null);
		    serverWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		    serverWindow.setVisible(true);
		}
		else if(e.getActionCommand().equals("Client")){
			this.setVisible(false);
			
			JFrame clientWindow = new ChatClient();
			clientWindow.setTitle("Chat Client");
		    clientWindow.setSize(640, 480);
		    clientWindow.setLocationRelativeTo(null);
		    clientWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		    clientWindow.setVisible(true);
		}
	}
}//end ChatDriver Class