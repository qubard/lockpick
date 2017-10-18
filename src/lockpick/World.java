package lockpick;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class World {
	
	private BufferedImage img = null;
	private int number = -1;
	
	public World(BufferedImage img, int number) {
		this.img = img;
		this.number = number;
	}
	
	public int getWorld() {
		return number;
	}
	
	public BufferedImage getImage() {
		return img;
	}
	
	public boolean matches(BufferedImage other) {
		// does this world image match another img?
		if(img != null && img.getWidth() == other.getWidth() && img.getHeight() == other.getHeight()) {
			for(int y = 0; y < img.getHeight(); y++) { 
				for(int x = 0; x < img.getWidth(); x++) {
					Color c = new Color(img.getRGB(x, y));
					int r1 = Math.abs(c.getRed()-38);
					int g1 = Math.abs(c.getGreen()-32);
					int b1 = Math.abs(c.getBlue()-26);
					int s = r1+g1+b1;
					
					Color c2 = new Color(other.getRGB(x, y));
					int r2 = Math.abs(c2.getRed()-38);
					int g2 = Math.abs(c2.getGreen()-32);
					int b2 = Math.abs(c2.getBlue()-26);
					int s2 = r2+g2+b2;
					if(s > 40 && s2 <= 40) {
						return false;
					}
					
					if(s2 > 40 && s <= 40) {
						return false;
					}
				}
			}
		} else {
			return false;
		}
		return true;
	}
	
}
