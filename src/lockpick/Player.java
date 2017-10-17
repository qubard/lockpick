package lockpick;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.imageio.ImageIO;

public class Player {
	
	private static BufferedImage img;
	
	private static Robot robot;
	
	private final int FAIL_THRESHOLD = 8;
	private final int TOTAL_FAIL_THRESHOLD = 10;
	
	private static int boardX = -1;
	private static int boardY = -1;
	private static int boardW = 658; // dx = 146
	private static int boardH = 459; // dy = 106
	
	private final int dx = 146;
	private final int dy = 106;

	private BufferedImage trade_slice;
	private BufferedImage bank_slice;
	private BufferedImage deposit_slice;
	private BufferedImage login_slice;
	private BufferedImage play_slice;
	
	private Random r = new Random();
	
	private BufferedImage refresh(int x, int y, int w, int h) {
        	return robot.createScreenCapture(new Rectangle(x,y,w,h));
	}
	
	boolean once = false;
	
	private void save() throws IOException {
		if(!once) {
			System.out.println("Saved to buffer.png");
			File f = new File("buffer.png");
			ImageIO.write(img, "png", f);
			once = true;
		}
	}
	
	private void save(BufferedImage img, String filename) throws IOException {
		File f = new File(filename);
		ImageIO.write(img, "png", f);
	}
	
	public static BufferedImage copyImage(BufferedImage source){
	    BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
	    Graphics g = b.getGraphics();
	    g.drawImage(source, 0, 0, null);
	    g.dispose();
	    return b;
	}
	
	class MousePos {
		int x;
		int y;
		
		public MousePos(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
	MousePos[][] coords = new MousePos[16][16];
	
	private Color lockpick_npc = new Color(47,24,38);
	
	private ArrayList<Point> points;
	
	private ArrayList<Point> clusters = new ArrayList<Point>();
	
	private HashMap<Point, Boolean> ignore;
	
	private ArrayList<Color> colors = new ArrayList<Color>();
	
	private ArrayList<World> worlds = new ArrayList<World>();
	
	// n^2 clustering
	private void cluster(int color) {
		Point minp = null;
		double last_dist = Double.POSITIVE_INFINITY;
		for(Point g : points) {
			if(!ignore.containsKey(g)) {
				double dist = 0;
				for(Point p : points) {
					dist += g.distance(p.getX(), p.getY());
				}
				if(minp == null || dist < last_dist) {
					minp = g;
					last_dist = dist;
				}
			}
		}
		if(minp != null && img != null) {
			System.out.println(minp + " is our clustered point");
			for(Point g : points) {
				if(!ignore.containsKey(g) && g.distance(minp) < 50) {
					img.setRGB((int)g.getX(), (int)g.getY(), color);
					ignore.put(g, true); // clustered already
				}
			}
			clusters.add(minp);
			//img.setRGB((int)minp.getX(), (int)minp.getY(), 0x00FFFF);
		}
	}
	
	private void test_colors(Color target) {
		for(int y = 0; y < img.getHeight(); y++) {
			for(int x = 0; x < img.getWidth(); x++) {
				int col = img.getRGB(x, y);
				float r = col & 255;
				float g = (col >> 8) & 255;
				float b = (col >> 16) & 255;
				float dr = Math.abs(r-target.getRed());
				float dg = Math.abs(g-target.getGreen());
				float db = Math.abs(b-target.getBlue());
				float sim = 100-100f*(dr/255f + dg/255f + db/255f)/3f;
				if(sim > 75 && g < 160 && b > 100 && b < 130) {
					img.setRGB(x, y, 255 << 16);
				}
			}
		}
	}
	
	private void cluster(final ArrayList<Color> colors) {
		points = new ArrayList<Point>();
		ignore = new HashMap<Point, Boolean>();
		for(int y = 0; y < img.getHeight()-dy; y++) {
			for(int x = 0; x < img.getWidth()-dx; x++) {
				int col = img.getRGB(x, y);
				for(Color c : colors) {
					if(c.getRGB() == col) {
						points.add(new Point(x,y));
						img.setRGB(x, y, 255 << 16);
					}
				}
			}
		}
		cluster(255 << 8);
		cluster(255 << 8);
	}
	
	private void cluster(Color target) {
		points = new ArrayList<Point>();
		ignore = new HashMap<Point, Boolean>();
		int maxd = 0;
		for(int y = 0; y < img.getHeight()-dy; y++) {
			for(int x = 0; x < img.getWidth()-dx; x++) {
				int col = img.getRGB(x, y);
				int r = col & 255;
				int g = (col >> 8) & 255;
				int b = (col >> 16) & 255;
				int dr = r-target.getRed();
				int dg = g-target.getGreen();
				int db = b-target.getBlue();
				int d = (int)Math.sqrt(dr*dr+dg*dg+db*db); // euclidean distance of rgb components
				if(d > maxd) {
					maxd = d;
				}
			}
		}
		for(int y = 0; y < img.getHeight()-dy; y++) {
			for(int x = 0; x < img.getWidth()-dx; x++) {
				int col = img.getRGB(x, y);
				int r = col & 255;
				int g = (col >> 8) & 255;
				int b = (col >> 16) & 255;
				int dr = r-target.getRed();
				int dg = g-target.getGreen();
				int db = b-target.getBlue();
				int d = (int)(255*Math.sqrt(dr*dr+dg*dg+db*db)/(double)maxd)&255; // euclidean distance of rgb components
				double ds = Math.sqrt(dr*dr+dg*dg+db*db)/(double)maxd;
				if(ds < 0.05) {
					//img.setRGB(x, y, 255 << 16);
					points.add(new Point(x,y));
				}
			}
		}
		
		cluster(255 << 8);
		cluster(255 << 8);
		cluster(255 << 8);
	}
	
	private boolean loadWorlds() { // load worlds off disk
		File dir = new File("worlds");
		
		ignored_worlds[1] = true;
		ignored_worlds[25] = true;
		ignored_worlds[37] = true;
		ignored_worlds[65] = true;
		ignored_worlds[45] = true;
		ignored_worlds[81] = true;
		ignored_worlds[8] = true;
		ignored_worlds[16] = true;
		ignored_worlds[92] = true;
		ignored_worlds[108] = true;
		ignored_worlds[109] = true;
		ignored_worlds[112] = true;
		ignored_worlds[26] = true;
		ignored_worlds[35] = true;
		ignored_worlds[82] = true;
		ignored_worlds[53] = true;
		ignored_worlds[61] = true;
		ignored_worlds[66] = true;
		ignored_worlds[83] = true;
		ignored_worlds[84] = true;
		ignored_worlds[85] = true;
		ignored_worlds[93] = true;
		ignored_worlds[70] = true;
		ignored_worlds[73] = true;
		ignored_worlds[94] = true;
		ignored_worlds[86] = true;
		ignored_worlds[91] = true;
		ignored_worlds[49] = true;
		
		if(dir.isDirectory()) {
			for(final File f : dir.listFiles()) { // assume they're all images lol
				String filename = f.getName();
				if(filename.endsWith(".png")) {
					String world = filename.substring(0, filename.length()-4);
					try {
						worlds.add(new World(ImageIO.read(f), Integer.parseInt(world)));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return worlds.size() != 0;
	}
	
	public int countColor(int mx, int my, int w, int h, int col) {
		int n = 0;
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				try {
					if((img.getRGB(mx+x, my+y) & 0xFFFFFF) == col) {
						n++;
					}
				} catch(Exception e) {
					return 0;
				}
			}
		}
		return n;
	}
	
	boolean done_buying = false;
	
	private long[] visited_worlds = new long[113];

	private boolean[] ignored_worlds = new boolean[113];
	
	private ArrayList<WorldQueueObject> world_queue = new ArrayList<WorldQueueObject>();
	
	class WorldQueueObject {
		
		public int mx;
		public int my;
		
		public World world;
		
		public WorldQueueObject(int mx, int my, World world) {
			this.mx = mx;
			this.my = my;
			this.world = world;
		}
	}
	
	int curr_world = -1;
	
	private boolean switchWorlds() throws InterruptedException {
		int q = 0;
		int ax = 0;
		int ay = 0;
		
		BufferedImage img = refresh(boardX+544, boardY+226, 174, 204);

		world_queue = new ArrayList<WorldQueueObject>();
		for(int y = 0; y < img.getHeight(); y++) {
			int x = 40;
			Color c = new Color(img.getRGB(x, y));
			int r1 = Math.abs(c.getRed()-38);
			int g1 = Math.abs(c.getGreen()-32);
			int b1 = Math.abs(c.getBlue()-26);
			int s = r1+g1+b1;
			if(s > 40) {
				img.setRGB(x, y, 255 << 8);
				q++;
			} else {
				if(q == 15) {
					ay = y-15;
					ax = x;
					int tx = ax-25;
					int ty = ay;
					
					BufferedImage d = refresh(boardX+544+tx, boardY+226+ty, 25, 15);
					// compare d and a world image
					for(World w : worlds) {
						if(w.matches(d)) {
							System.out.println("We found " + w.getWorld());
							
							if(!ignored_worlds[w.getWorld()]) {
								if(visited_worlds[w.getWorld()] != 0) {
									long elapsed = ((new Date()).getTime()-visited_worlds[w.getWorld()])/1000;
									if(elapsed > 1500) { // 25 minutes
										// move to this world
										world_queue.add(new WorldQueueObject(boardX+544+tx, boardY+226+ty, w));
									} else {
										System.out.println("Was gonna go to " + w.getWorld() + " but < 25 mins elapsed");
									}
								} else {
									// move to this world
									world_queue.add(new WorldQueueObject(boardX+544+tx, boardY+226+ty, w));
								}
								break;
							}
						}
					}
				} else if(q < 15) {
					ax = x;
					ay = y+1;
				}
				q = 0;
			}
		}
		
		if(world_queue.size() == 0)  {
			System.out.println("Need to scroll down!");
			Thread.sleep(155+r.nextInt(20));
			smooth(boardX+721+r.nextInt(9), boardY+246+r.nextInt(168));
			Thread.sleep(155+r.nextInt(50));
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			Thread.sleep(155+r.nextInt(200));
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			return false;
		} else {
			// choose something from world queue at random?
			System.out.println("Choosing at random");
			int i = r.nextInt(world_queue.size());
			WorldQueueObject chosen = world_queue.get(i);
			int k = 0;
			while(chosen.world.getWorld() == curr_world && k < world_queue.size()) {
				System.out.println("Trying another world (collision)");
				chosen = world_queue.get(r.nextInt(world_queue.size()));
				k++;
			}
			
			if(k >= world_queue.size()) {
				Thread.sleep(155+r.nextInt(20));
				smooth(boardX+721+r.nextInt(9), boardY+246+r.nextInt(168));
				Thread.sleep(155+r.nextInt(50));
				robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
				Thread.sleep(155+r.nextInt(200));
				robot.mouseRelease(InputEvent.BUTTON1_MASK);
				robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
				return false;
			}
			
			curr_world = chosen.world.getWorld();
			Date curr = new Date();
			visited_worlds[chosen.world.getWorld()] = curr.getTime();
			System.out.println("Visiting " + chosen.world.getWorld());
			Thread.sleep(55+r.nextInt(20));
			smooth(chosen.mx+r.nextInt(100), chosen.my+r.nextInt(10));
			Thread.sleep(55+r.nextInt(200));
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			Thread.sleep(55+r.nextInt(200));
			try {
				read_stamps();
				stamp(chosen.world.getWorld(), curr.getTime());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		return true;
	}
	
	private void read_stamps() throws IOException {
		Path p = Paths.get("", "visits");
		List<String> fileContent = new ArrayList<>(Files.readAllLines(p, StandardCharsets.UTF_8));

		for (int i = 0; i < fileContent.size(); i++) {
			String line = fileContent.get(i);
			String[] delim = line.split(" ");
			if(delim.length == 2) {
				int w = Integer.parseInt(delim[0]);
				long time = Long.parseLong(delim[1]);
				System.out.println("Loaded world " +  w + ", " + time);
				visited_worlds[w] = new Date(time).getTime();
			}
		}
	}
	
	private void stamp(int world, long time) throws IOException {
		Path p = Paths.get("", "visits");
		List<String> fileContent = new ArrayList<>(Files.readAllLines(p, StandardCharsets.UTF_8));
		System.out.println("Stamping world " + world);
		
		if(fileContent.size() == 0) {
			fileContent.add(world + " " + time);
		} else {
			boolean matched = false;
			for (int i = 0; i < fileContent.size(); i++) {
				String line = fileContent.get(i);
				String[] delim = line.split(" ");
				if(delim.length == 2) {
					int w = Integer.parseInt(delim[0]);
					if(w == world) {
						matched = true;
						System.out.println("Updating entry");
						fileContent.remove(line);
						fileContent.add(w + " " + time);
					}
				}
			}
			
			if(!matched) {
				fileContent.add(world + " " + time);
			}
		}

		Files.write(p, fileContent, StandardCharsets.UTF_8);
	}

	public boolean find() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		BufferedImage img = refresh(0,0,screenSize.width,screenSize.height);
		for(int y = 0; y < img.getHeight()-1; y++) {
			for(int x = 0; x < img.getWidth()-1; x++) {
				if((img.getRGB(x, y)&0xFFFFFF) == 0x191305 && (img.getRGB(x+1, y)&0xFFFFFF) == 0x191305 && (img.getRGB(x, y+1)&0xFFFFFF) == 0x322C19 && (img.getRGB(x+1, y+1)&0xFFFFFF) == 0x191711) {
					boardX = x+4;
					boardY = y+4;
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean loggedIn() {
		int x = 0;
		int y = 0;
		BufferedImage img = refresh(boardX-4, boardY-4, 2, 2);
		return (img.getRGB(x, y)&0xFFFFFF) == 0x191305 
				&& (img.getRGB(x+1, y)&0xFFFFFF) == 0x191305 
				&& (img.getRGB(x, y+1)&0xFFFFFF) == 0x322C19 
				&& (img.getRGB(x+1, y+1)&0xFFFFFF) == 0x191711;
	}
	
	int cx = 3;
	int cy = 3;
	
	public void smooth(int tx, int ty) throws InterruptedException { // do some sort of	interpolation that gets exponential
		double d = Math.sqrt((tx-cx)*(tx-cx) + (ty-cy)*(ty-cy));
		double od = d;
		double vx = tx-cx;
		double vy = ty-cy;
		
		double a = (new Random()).nextDouble()/100+0.1;
		double b = (new Random()).nextDouble()/100+0.1;
		
		double r = d/od;
		
		while(d/od > 0.15) {
			cx += vx*a;
			cy += vy*b;
			robot.mouseMove(cx, cy);
			// randomly do an exponential curve with some factor or move statically in a straight line
			Thread.sleep(5+(new Random()).nextInt(3));
			d = Math.sqrt((tx-cx)*(tx-cx) + (ty-cy)*(ty-cy));
			vx = tx-cx;
			vy = ty-cy;
			if(d/od >= r) {
				break;
			} else {
				r = d/od;
			}
		}
		Thread.sleep(5+(new Random()).nextInt(3));
		robot.mouseMove(tx, ty);
		cx = tx;
		cy = ty;
	}

	public void play() throws InterruptedException {
		int failticks = 0;
		
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		
		Point z = MouseInfo.getPointerInfo().getLocation();
		cx = z.x;
		cy = z.y;
		
		System.out.println("Input password:");
		Scanner s = new Scanner(System.in);
		String pw = s.nextLine();
		s.close();
		
		if(!init_resources()) {
			return;
		}
		
		System.out.println("Waiting 3 seconds...");
		Thread.sleep(3000);
		System.out.println("Running");
		
		// bank npc colors
		colors.add(new Color(73,66,30));
		colors.add(new Color(62,55,24));
		colors.add(new Color(18,16,5));
		colors.add(new Color(106,99,67));
		colors.add(new Color(50,44,20));
		colors.add(new Color(39,36,16));
		colors.add(new Color(66,59,26));
		
		colors.add(new Color(61,54,22));
		colors.add(new Color(57,50,20));
		colors.add(new Color(50,44,17));
		colors.add(new Color(93,86,55));
		
		for(int i = 0; i < visited_worlds.length; i++) {
			visited_worlds[i] = 0;
		}
		
		System.out.println("Loaded " + worlds.size() + " world images");
		
		boolean running = true;
		int bought = 0;
		int total_fails = 0;
		
		while(running && total_fails < TOTAL_FAIL_THRESHOLD) {
			clusters = new ArrayList<Point>();
			
			if(!loggedIn()) {
				System.out.println("Not logged in. Breaking for 1-3 min.");
				Thread.sleep(60000*(r.nextInt(2)+1));
				System.out.println("Trying to login..");
				if(!login(pw)) {
					running = false;
					System.out.println("Could not login.");
					Thread.sleep(3000+r.nextInt(1000));
				} else {
					System.out.println("Successfully logged in!");
				}
			}
			
			img = refresh(boardX-4, boardY-4, boardW+4, boardH+4);
			
			if(!done_buying) {
				cluster(lockpick_npc);
			} else {
				Thread.sleep(500+r.nextInt(500));
				System.out.println("Trying to find bank npc");
				cluster(colors);
			}
			
			if(clusters.size() == 0) {
				smooth(boardX+r.nextInt(boardW), boardY+r.nextInt(boardH));
				// move the mouse since this bug *rarely* can happen
			}
			
			for(Point p : clusters) {
				smooth(boardX+p.x, boardY+p.y);
				Thread.sleep(100);
				robot.mousePress(InputEvent.BUTTON3_MASK);
				Thread.sleep(250);
				img = refresh(boardX, boardY, boardW, boardH);
				int[] trade = find_slice_yellow(trade_slice);
				if(!done_buying) {
					if(failticks < FAIL_THRESHOLD) {
						if(trade != null) {
							trade[0] = p.x;
							System.out.println("Found npc");
							failticks = 0;
							bought = buy_lockpicks(trade);
							done_buying = bought > 0;
							if(done_buying) {
								break;
							}
						} else {
							failticks++;
							Thread.sleep(250);
							smooth(boardX+r.nextInt(boardW), boardY+r.nextInt(boardH));
						}
					} else {
						total_fails++;
						System.out.println("Could not find lockpick NPC! Waiting 5s");
						Thread.sleep(250);
						smooth(boardX+r.nextInt(boardW), boardY+r.nextInt(boardH));
						Thread.sleep(5000);
						failticks = 0;
						System.out.println("Reseting failticks");
					}
				} else {
					if(failticks < FAIL_THRESHOLD) {
						img = refresh(boardX, boardY, boardW, boardH);
						int[] banker = find_slice_yellow(bank_slice);
						if(bought != 0 && banker != null) {
							System.out.println("FOUND BANKING NPC!");
							failticks = 0;
							boolean did_bank = bank(banker);
							
							if(did_bank) {
								// change worlds
								boolean badSwitch = switchWorlds();
								while(!badSwitch) {
									badSwitch = switchWorlds();
									Thread.sleep(1000+r.nextInt(200));
								}
								total_fails = 0;
								Thread.sleep(1000+r.nextInt(500));
								done_buying = false;
								break;
							}
						} else {
							if(bought == 0) {
								// change worlds
								boolean badSwitch = switchWorlds();
								while(!badSwitch) {
									badSwitch = switchWorlds();
									Thread.sleep(1000+r.nextInt(200));
								}
								total_fails = 0;
								Thread.sleep(1000+r.nextInt(500));
								done_buying = false;
							} else {
								failticks++;
								Thread.sleep(250);
								System.out.println("Trying to find banker");
							}
						}
					} else {
						System.out.println("Could not find bank NPC! Waiting 5s");
						Thread.sleep(250);
						smooth(boardX+r.nextInt(boardW), boardY+r.nextInt(boardH));
						Thread.sleep(5000);
						failticks = 0;
						total_fails++;
						System.out.println("Reseting failticks");
					}
				}
				Thread.sleep(300+r.nextInt(100));
			}
		}
		
		System.out.println("Stopped. Total fails: " + total_fails);
	}
	
	private boolean init_resources() {
		try {
			deposit_slice = ImageIO.read(new File("misc/deposit.png"));
		} catch (IOException e) {
			System.out.println("Could not load deposit slice.");
			return false;
		}
		
		try {
			login_slice = ImageIO.read(new File("misc/login.png"));
		} catch (IOException e) {
			System.out.println("Could not load login slice.");
			return false;
		}
		
		try {
			play_slice = ImageIO.read(new File("misc/play.png"));
		} catch (IOException e) {
			System.out.println("Could not load play slice.");
			return false;
		}
		
		if(!find()) {
			System.out.println("Could not find board!");
			return false;
		}
		
		if(!loadWorlds()) {
			System.out.println("Could not find worlds dir.");
			return false;
		}
		
		try {
			bank_slice = ImageIO.read(new File("misc/bank.png"));
		} catch (IOException e) {
			System.out.println("Could not load bank slice.");
			return false;
		}
		
		try {
			trade_slice = ImageIO.read(new File("misc/trade.png"));
		} catch (IOException e) {
			System.out.println("Could not load trade slice.");
			return false;
		}
		
		return true;
	}

	private boolean bank(int[] banker) throws InterruptedException {
		smooth(boardX+banker[0]+r.nextInt(bank_slice.getWidth()), boardY+banker[1]+r.nextInt(9)-4);
		Thread.sleep(100+r.nextInt(200));
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		Thread.sleep(55+r.nextInt(200));
		int bank = 0;
		int bankfails = 0;
		while(bank == 0 && bankfails < 10) {
			Thread.sleep(500);
			img = refresh(boardX, boardY, boardW, boardH);
			bank = countColor(183, 21, 5, 5, 0xFF981F);
			bankfails++;
			System.out.println("Finding bank");
		}
		
		if(bankfails < 10) {
			smooth(boardX+610+r.nextInt(5), boardY+220+r.nextInt(6));
			Thread.sleep(150+r.nextInt(50));
			robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
			Thread.sleep(150+r.nextInt(50));
			
			img = refresh(boardX, boardY, boardW, boardH);
			int[] deposit = find_slice(deposit_slice);
			
			if(deposit != null) {
				smooth(boardX+610+r.nextInt(5), boardY+deposit[1]+r.nextInt(6)); 
			} else {
				smooth(boardX+610+r.nextInt(5), boardY+220+r.nextInt(6)+83); // deposit all
			}
			
			Thread.sleep(150+r.nextInt(50));
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			Thread.sleep(150+r.nextInt(50));
			smooth(boardX+473+r.nextInt(15), boardY+8+r.nextInt(18));
			Thread.sleep(150+r.nextInt(50));
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			Thread.sleep(50+r.nextInt(50));
			
			return true;
		}
		return false;
	}
	
	// returns how many were bought
	private int buy_lockpicks(int[] p) throws InterruptedException {
		Thread.sleep(204);
		smooth(boardX+p[0], boardY+p[1]+r.nextInt(8));
		Thread.sleep(100);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		int store = 0;
		int storefails = 0;
		while(store == 0 && storefails < 10) {
			Thread.sleep(500);
			img = refresh(boardX, boardY, boardW, boardH);
			store = countColor(155, 35, 5,5, 0xFF981F);
			System.out.println("Finding store");
			storefails++;
		}
		if(store > 0) {
			System.out.println("Store window is open");
			int tx = boardX+128+r.nextInt(25);
			int ty = boardY+68+r.nextInt(25);
			
			img = refresh(boardX, boardY, boardW, boardH);
			int empty = countColor(121, 65, 5, 8, 0xFFFF00);
			System.out.println("empty : " + empty);
			int x = 0;
			while(x < 3 && empty != 18) {
				smooth(tx, ty);
				x++;
				Thread.sleep(100+r.nextInt(200));
				robot.mousePress(InputEvent.BUTTON3_MASK);
				Thread.sleep(30+r.nextInt(155));
				smooth(tx+r.nextInt(30)-15, ty+66+r.nextInt(10));
				Thread.sleep(100+r.nextInt(200));
				robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
				Thread.sleep(55+r.nextInt(200));
				Thread.sleep(r.nextInt(500)+100);
				img = refresh(boardX, boardY, boardW, boardH);
				empty = countColor(121, 65, 5, 8, 0xFFFF00);
			}
			
			Thread.sleep(800+r.nextInt(100));
			smooth(boardX+475+r.nextInt(19), boardY+30+r.nextInt(18));
			Thread.sleep(50+r.nextInt(50));
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			return x;
		}
		return -1;
	}

	private boolean login(String pw) throws InterruptedException {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		
		if(boardX != -1 && boardY != -1) {
			img = refresh(0,0,screenSize.width,screenSize.height);
		} else {
			img = refresh(boardX, boardY, boardW, boardH);
		}
		
		int[] login = find_slice(login_slice);
		
		if(login == null) {
			return false;
		}

		smooth(login[0]+15+r.nextInt(132), login[1]+30+r.nextInt(32));
		Thread.sleep(r.nextInt(150)+50);
		robot.mousePress(InputEvent.BUTTON1_MASK);
		Thread.sleep(r.nextInt(250)+50);
		
		pw = pw.toUpperCase();
		char[] chars = pw.toCharArray();
		
		for(char c : chars) {
			robot.keyPress((char)c);
			robot.keyRelease((char)c);
			Thread.sleep(100+r.nextInt(50));
		}
		
		robot.keyPress(KeyEvent.VK_ENTER);
		
		img = refresh(0,0,screenSize.width,screenSize.height);
		
		int[] play = find_slice(play_slice);
		int failticks = 0;
		
		while(play == null && failticks < 10) {
			failticks++;
			if(boardX == -1 && boardY == -1) {
				img = refresh(0,0,screenSize.width,screenSize.height);
			} else {
				img = refresh(boardX, boardY, boardW, boardH);
			}
			play = find_slice(play_slice);
			Thread.sleep(1000+r.nextInt(500));
		}
		
		if(play != null) {
			System.out.println("Found play!");
		} else {
			System.out.println("Could not find play");
			play = new int[] {290, 305};
		}
		
		if(!loggedIn()) {
			smooth(boardX+play[0]+5+r.nextInt(100), boardY+play[1]+5+r.nextInt(20));
			Thread.sleep(50+r.nextInt(250));
			robot.mousePress(InputEvent.BUTTON1_MASK);
		} else {
			return true;
		}
		
		failticks = 0;
		
		if(failticks < 10) {
			while(failticks < 10) {
				if(loggedIn()) {
					return true;
				} else {
					failticks++;
					Thread.sleep(1000);
				}
			}
		}
		return false;
	}

	private int[] find_slice(BufferedImage slice) {
		// use the already refreshed buffer img
		for(int y = 0; y < img.getHeight(); y++) {
			for(int x = 0 ; x < img.getWidth()-slice.getWidth(); x++) {
				boolean bad = false;
				for(int k = 0; k < slice.getWidth(); k++) {
					int col = slice.getRGB(k, 0)&0xFFFFFF;
					if(col != 0xFF0000 && (img.getRGB(x+k, y)&0xFFFFFF) != col) {
						bad = true;
						break;
					}
				}
				if(!bad) {
					return new int[] {x,y};
				}
			}
		}
		return null;
	}
	
	// allow white on yellow comparisons to pass through (in case we're already highlighting it)
	private int[] find_slice_yellow(BufferedImage slice) {
		// use the already refreshed buffer img
		for(int y = 0; y < img.getHeight(); y++) {
			for(int x = 0 ; x < img.getWidth()-slice.getWidth(); x++) {
				boolean bad = false;
				for(int k = 0; k < slice.getWidth(); k++) {
					int col = slice.getRGB(k, 0)&0xFFFFFF;
					int v = (img.getRGB(x+k, y)&0xFFFFFF);
					if(col != 0xFF0000 && v != col && !(col == 0xFFFFFF && v == 0xFFFF00)) {
						bad = true;
						break;
					}
				}
				if(!bad) {
					return new int[] {x,y};
				}
			}
		}
		return null;
	}

}
