import javax.swing.JFrame;

public class SnakeGame extends JFrame{
	public SnakeGame() {
		gameInit();
	}
	private void gameInit() {
		this.add(new GamePanel());
		this.setTitle("Snake Game");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.pack();
		this.setLocationRelativeTo(null);
	}
	public static void main(String[] args) {
		JFrame game = new SnakeGame();
		game.setVisible(true);
	}

}
