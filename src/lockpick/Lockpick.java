package lockpick;

public class Lockpick {

	public static void main(String[] args) {
		try {
			(new Player()).play();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
