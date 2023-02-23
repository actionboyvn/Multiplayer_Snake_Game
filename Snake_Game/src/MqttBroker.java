import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Random;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
public class MqttBroker extends Thread{
	static final String broker = "tcp://broker.hivemq.com:1883";
	//static final String broker = "tcp://test.mosquitto.org:1883";
	static final String topic = "multiplayer_snake_game";	
	MqttClient client;
	String clientId;
	int myId;
	int numberOfOnlinePlayers;
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
		connectToBroker();			
	}
	public void connectToBroker() throws MqttSecurityException, MqttException, InterruptedException {
		client.connect();
		client.subscribe(topic);
		MqttMessage message = new MqttMessage("ping".getBytes());
		client.publish(topic, message);
		Thread.sleep(300);	
		myId = numberOfOnlinePlayers;
		if (myId != 1) {
			client.unsubscribe(topic);
			client.subscribe(topic + Integer.toString(myId));
		}	
	}
	public void disconnectFromBroker() throws MqttPersistenceException, MqttException {
		client.publish(topic, new MqttMessage("disconnected".getBytes()));
		client.disconnect();
	}
	public MqttCallback myCallBack() {
		return new MqttCallback() {
			@Override
			public void connectionLost(Throwable cause) {
				// TODO Auto-generated method stub								
			}
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				String s = new String(message.getPayload());		
				if (s.compareTo("disconnected") == 0) 
					numberOfOnlinePlayers -= 1;
				else
				if (s.compareTo("ping") == 0) {
					numberOfOnlinePlayers += 1;
					if (numberOfOnlinePlayers > 1)
						client.publish(topic, new MqttMessage(("online=" + Integer.toString(numberOfOnlinePlayers)).getBytes()));
				}
				else
				if (s.substring(0, 7).compareTo("online=") == 0) {					
					numberOfOnlinePlayers = Integer.parseInt(s.substring(7, s.length()));
				}			
				else {					
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
				// TODO Auto-generated method stub			
			}	
		};		
	}
	public void publishMessage(String sms) throws MqttPersistenceException, MqttException {
		MqttMessage message = new MqttMessage(sms.getBytes());
		if (myId == 1)
			for (int i = 2; i <= numberOfOnlinePlayers; i++)
				client.publish(topic + Integer.toString(i), message);					
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
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						else
							try {
								client.publish(topic + Integer.toString(i), message);
							} catch (MqttException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}					
			}
			
		});
		t1.start();		
	}
}
