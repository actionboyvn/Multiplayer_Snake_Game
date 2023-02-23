import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.swing.*;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;


import java.util.Random;
public class GamePanel extends JPanel implements ActionListener{
	static final int SCREEN_WIDTH = 800;
	static final int SCREEN_HEIGHT = 800;
	static final int STATS_SCREEN_WIDTH = 200;
	static final int UNIT_SIZE = 20;
	static final int MAX_SNAKE_SIZE = (SCREEN_WIDTH / UNIT_SIZE) * (SCREEN_HEIGHT / UNIT_SIZE);
	static final int DELAY = 200;
	static final int INITIAL_SNAKE_SIZE = 10;
	static final Color[] SNAKE_COLOR = {Color.cyan, Color.green, Color.yellow, Color.white};
	boolean execute;
	int snake_size[];
	int x[][];
	int y[][];	
	int x_initial_this;
	int y_initial_this;
	int apple_x;
	int apple_y;
	int stats[];
	Random rand;
	Timer timer;
	char direction;
	MqttBroker protocol;
	int snake_id;
	public GamePanel(){
		try {
			protocol = new MqttBroker(this);
		} catch (MqttSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		snake_id = protocol.myId - 1;		
		execute = true;				
		timer = new Timer(DELAY,this);		
		rand = new Random();
		snakeInit();
		stats = new int[4];
		generateNewApple();
		panelInit();				
		startGame();		
	}	
	public void snakeInit() {
		snake_size = new int[4];
		snake_size[snake_id] = INITIAL_SNAKE_SIZE;
		x = new int[4][MAX_SNAKE_SIZE];
		y = new int[4][MAX_SNAKE_SIZE];
		for (int i = 0; i < 4; i++) {
			int x_initial, y_initial;
			switch (i) {
			case 0:
				x_initial = y_initial = 0;
				break;
			case 1:
				x_initial = (SCREEN_WIDTH / UNIT_SIZE - 1) * UNIT_SIZE;
				y_initial = 0;
				break;
			case 2:
				x_initial = (SCREEN_WIDTH / UNIT_SIZE - 1) * UNIT_SIZE;
				y_initial = (SCREEN_HEIGHT / UNIT_SIZE - 1) * UNIT_SIZE;
				break;
			default:
				x_initial = 0;
				y_initial = (SCREEN_HEIGHT / UNIT_SIZE - 1) * UNIT_SIZE;
			}
			if (snake_id == i) {
				x_initial_this = x_initial;
				y_initial_this = y_initial;
			}
			for (int j = 0; j < INITIAL_SNAKE_SIZE; j++) {
				x[i][j] = x_initial;
				y[i][j] = y_initial;
			}
		}
		direction = 'N';
	}
	public void snakeRestart(){
		snake_size[snake_id] = INITIAL_SNAKE_SIZE;
		for (int i = 0; i < snake_size[snake_id]; i++) { 
			x[snake_id][i] = x_initial_this; 
			y[snake_id][i] = y_initial_this;		
		}		
		direction = 'N';
	}
	public void panelInit() {
		this.setPreferredSize(new Dimension(SCREEN_WIDTH + STATS_SCREEN_WIDTH, SCREEN_HEIGHT));
		this.setBackground(Color.black);
		this.setFocusable(true);
		this.addKeyListener(new GameKeyAdapter());		
	}
	public void startGame() {		
		timer.start();		
	}
	public void snakeMoves() {
		int tail_x = x[snake_id][snake_size[snake_id] - 1];
		int tail_y = y[snake_id][snake_size[snake_id] - 1];
		for (int i = snake_size[snake_id] - 1; i > 0; i--) {
			x[snake_id][i] = x[snake_id][i - 1];
			y[snake_id][i] = y[snake_id][i - 1];
		}		
		switch(direction) {
		case 'U':
			y[snake_id][0] -= UNIT_SIZE;
			break;	
		case 'D':
			y[snake_id][0] += UNIT_SIZE;
			break;
		case 'L':
			x[snake_id][0] -= UNIT_SIZE;
			break;
		case 'R':
			x[snake_id][0] += UNIT_SIZE;
			break;
		}
		if (checkCollision()) {
			snakeRestart();
			return;
		}
		if (x[snake_id][0] == apple_x && y[snake_id][0] == apple_y) {			
			x[snake_id][snake_size[snake_id]] = tail_x;
			y[snake_id][snake_size[snake_id]] = tail_y;			
			snake_size[snake_id]++;
			stats[snake_id] = Math.max(stats[snake_id], snake_size[snake_id] - INITIAL_SNAKE_SIZE);
			generateNewApple();
		}
	}
	public boolean checkCollision() {
		if (x[snake_id][snake_size[snake_id] - 1] == x[snake_id][snake_size[snake_id] - 2] 
				&& y[snake_id][snake_size[snake_id] - 1] == y[snake_id][snake_size[snake_id] - 2]) 
			return false;
		if (x[snake_id][0] >= SCREEN_WIDTH || x[snake_id][0] < 0 
				|| y[snake_id][0] >= SCREEN_HEIGHT || y[snake_id][0] < 0)
			return true;
		for (int i = 1; i < snake_size[snake_id]; i++)
			if (x[snake_id][i] == x[snake_id][0] && y[snake_id][i] == y[snake_id][0])
				return true;		
		return false;			
	}	
	public void generateNewApple() {
		boolean satisfied = false;
		while (!satisfied) {				
			apple_x = rand.nextInt(SCREEN_WIDTH / UNIT_SIZE) * UNIT_SIZE;
			apple_y = rand.nextInt(SCREEN_HEIGHT / UNIT_SIZE) *  UNIT_SIZE;
			satisfied = true;
			for (int i = 0; i < snake_size[snake_id]; i++)
				if (apple_x == x[snake_id][i] && apple_y == y[snake_id][i])
					satisfied = false;
		}		
	}
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);		
		for (int j = 0; j <= SCREEN_WIDTH / UNIT_SIZE; j++) 
			g.drawLine(j * UNIT_SIZE, 0, j * UNIT_SIZE, SCREEN_HEIGHT);			
		for (int i = 0; i < SCREEN_HEIGHT / UNIT_SIZE ; i++)
			g.drawLine(0, i * UNIT_SIZE, SCREEN_WIDTH, i * UNIT_SIZE);
		g.setColor(Color.red);
		g.fillOval(apple_x, apple_y, UNIT_SIZE, UNIT_SIZE);							
		for (int j = 0; j < 4; j++) {
			stats[j] = Math.max(stats[j], snake_size[j] - INITIAL_SNAKE_SIZE);			
			g.setColor(SNAKE_COLOR[j]);
			for (int i = 0; i < snake_size[j]; i++) 				
				g.fillRect(x[j][i], y[j][i], UNIT_SIZE, UNIT_SIZE);
			g.setColor(Color.red);
			g.setFont( new Font("Arial",Font.BOLD, 18));
			g.drawString("Your score: " + (snake_size[snake_id] - INITIAL_SNAKE_SIZE), SCREEN_WIDTH, g.getFont().getSize());
			g.drawString("STATS", SCREEN_WIDTH, g.getFont().getSize() + 80);			
			g.setColor(SNAKE_COLOR[j]);
			g.setFont( new Font("Arial",Font.PLAIN, 18));								
			g.drawString("Player: " + (j + 1) + ": " + stats[j], SCREEN_WIDTH, g.getFont().getSize() + 100 + 20 * j);
		}		
	}	
	@Override
	public void actionPerformed(ActionEvent e) {		
		if (execute && direction != 'N') {
			snakeMoves();				
			try {
				sendToBroker();
			} catch (IOException | InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}		
		}	
		repaint();
	}	
	public void sendToBroker() throws IOException, InterruptedException{
		int[] data = new int[snake_size[snake_id] * 2 + 1];
		for (int i = 0; i < snake_size[snake_id]; i++) {
			data[i] = x[snake_id][i];
			data[i + snake_size[snake_id]] = y[snake_id][i]; 
		}		
		data[snake_size[snake_id] * 2] = snake_id;
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    new ObjectOutputStream(out).writeObject(data);
	    String data_serialized = new String(Hex.encodeHex(out.toByteArray()));	    
		try {		    
			protocol.publishData(data_serialized);			
		} catch (MqttException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public class GameKeyAdapter extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			switch(e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				if(direction != 'R') {
					direction = 'L';					
					snakeMoves();
					repaint();
				}
				break;
			case KeyEvent.VK_RIGHT:
				if(direction != 'L') {
					direction = 'R';					
					snakeMoves();
					repaint();
				}
				break;
			case KeyEvent.VK_UP:
				if(direction != 'D') {
					direction = 'U';					
					snakeMoves();
					repaint();
				}
				break;
			case KeyEvent.VK_DOWN:
				if(direction != 'U') {
					direction = 'D';					
					snakeMoves();
					repaint();
				}
				break;
			}
		}
	}
			
}
