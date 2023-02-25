
import javax.swing.JFrame;

import org.eclipse.paho.client.mqttv3.MqttException;

public class SnakeGame extends JFrame{
	GamePanel gamePanel;
	public SnakeGame() {
		gamePanel = new GamePanel();
		this.add(gamePanel);
		gameInit();
	}
	private void gameInit() {
		this.setTitle("Snake Game");
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.setResizable(false);
		this.pack();
		this.setLocationRelativeTo(null);
	}
	@Override 
	public void dispose() {
		try {
			gamePanel.sendDisconnectionMessage();
		} catch (MqttException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.dispose();
		System.exit(ABORT);
	}
	public static void main(String[] args) {
		JFrame game = new SnakeGame();
		game.setVisible(true);
		}
	}
