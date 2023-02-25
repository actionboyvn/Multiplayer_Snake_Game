import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
public class MqttBroker extends Thread{
	static final String broker = "tcp://broker.hivemq.com:1883";
	static final String topic = "multiplayer_snake_game";	
	static final int MAX_NUMBER_OF_PLAYERS = 4;	
	MqttClient client;
	String clientId;
	int myId;
	int numberOfOnlinePlayers;
	boolean playerIsOnline[];
	String playerConnectionTime[];
	DateTimeFormatter myFormatObj;
	Random rand;
	GamePanel gamePanel;	
	public MqttBroker(GamePanel panel) throws MqttSecurityException, MqttException, InterruptedException {
		myId = 0;
		numberOfOnlinePlayers = 0;
		rand = new Random();
		gamePanel = panel;
		clientId = "Random player" + Integer.toString(rand.nextInt(200));	
		client = new MqttClient(broker, clientId, new MemoryPersistence());
		client.setCallback(myCallBack());
		playerIsOnline = new boolean[MAX_NUMBER_OF_PLAYERS + 1];
		playerConnectionTime = new String[MAX_NUMBER_OF_PLAYERS + 1];
		Arrays.fill(playerIsOnline, false);
		connectToBroker();			
	}
	public void connectToBroker() throws MqttSecurityException, MqttException, InterruptedException {
		client.connect();
		client.subscribe(topic);	
		publishMessage("ping");
		Thread.sleep(300);	
		for (int i = 1; i <= MAX_NUMBER_OF_PLAYERS; i++)
			if (!playerIsOnline[i]) { 
				myId = i;
				break;
			}
		playerIsOnline[myId] = true;
		myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
		playerConnectionTime[myId] = LocalDateTime.now().format(myFormatObj);
		calculateNumberOfOnlinePlayers();
		if (myId != 1) {
			client.unsubscribe(topic);
			client.subscribe(topic + Integer.toString(myId));
		}	
		publishMessage("connected" + Integer.toString(myId));
	}
	public void calculateNumberOfOnlinePlayers() {
		int sum = 0;
		for (int i = 1; i <= MAX_NUMBER_OF_PLAYERS; i++)
			if (playerIsOnline[i])
				sum++;
		numberOfOnlinePlayers = sum;
	}
	public void disconnectFromBroker() throws MqttPersistenceException, MqttException, InterruptedException {
		publishData("disconnected" + Integer.toString(myId));
	}
	public MqttCallback myCallBack() {
		return new MqttCallback() {
			@Override
			public void connectionLost(Throwable cause) {
										
			}
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				String s = new String(message.getPayload());		
				if (s.length() > 12 && s.substring(0, 12).compareTo("disconnected") == 0) { 
					playerIsOnline[Integer.parseInt(s.substring(12, 13))] = false;
					calculateNumberOfOnlinePlayers();
				}
				else 
				if (s.length() > 9 && s.substring(0, 9).compareTo("connected") == 0) {					
					int playerId = Integer.parseInt(s.substring(9, 10));
					String connectionTime = s.substring(10, s.length());
					playerIsOnline[playerId] = true;
					if (playerId != myId)
						if (connectionTime.length() > 0)
							playerConnectionTime[playerId] = connectionTime;
						else 
							playerConnectionTime[playerId] = LocalDateTime.now().format(myFormatObj);
					calculateNumberOfOnlinePlayers();
				}
				else					
				if (s.compareTo("ping") == 0) {					
					if (myId >= 1)	
						publishMessage("connected" + Integer.toString(myId) + playerConnectionTime[myId]);
				}								
				else {					
					if (myId == 0)
						return;
					ByteArrayInputStream in = new ByteArrayInputStream(Hex.decodeHex(s.toCharArray()));
				    int[] data = (int[]) new ObjectInputStream(in).readObject();				    
				    int another_snake_id = data[data.length - 1];				
				    gamePanel.snake_size[another_snake_id] = data.length / 2;
				    if (myId == another_snake_id + 1)
						return;				    
				    for (int i = 0; i < data.length / 2; i++) {				    	
				    	gamePanel.x[another_snake_id][i] = data[i];
				    	gamePanel.y[another_snake_id][i] = data[i + data.length / 2];
				    }					    
				}
			}
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {

			}	
		};		
	}					
	public void publishMessage(String message) {
		MqttMessage message_bytes = new MqttMessage(message.getBytes());
		for (int i = 1; i <= MAX_NUMBER_OF_PLAYERS; i++)
			if (i == 1)
				try {
					client.publish(topic, message_bytes);
				} catch (MqttException e) {					
					e.printStackTrace();
				}
			else
				try {
					client.publish(topic + Integer.toString(i), message_bytes);
				} catch (MqttException e) {
					e.printStackTrace();
				}					
	}
	
	public void publishData(String data) throws MqttPersistenceException, MqttException, InterruptedException {
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				MqttMessage message = new MqttMessage(data.getBytes());
				for (int i = 1; i <= 4; i++)
					if (i != myId)
						if (i == 1)
							try {
								client.publish(topic, message);
							} catch (MqttException e) {
								e.printStackTrace();
							}
						else
							try {
								client.publish(topic + Integer.toString(i), message);
							} catch (MqttException e) {
								e.printStackTrace();
							}					
			}
			
		});
		t1.start();		
	}
}
