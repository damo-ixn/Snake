import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static java.lang.String.format;
import java.util.*;
import java.util.List;
import javax.swing.*;
 
public class Snake extends JPanel implements Runnable {
    enum Dir {
        up(0, -1), right(1, 0), down(0, 1), left(-1, 0);
 
        Dir(int x, int y) {
            this.x = x; this.y = y;
        }
 
        final int x, y;
    }
 

    static JFrame frame = new JFrame();
    static Random rand = new Random();
    
    static final int FRAME_WIDTH = 640;
    static final int FRAME_HEIGHT = 440;
    static final int WALL = -1;
    static int speed = 5;
    static int max_Energy = 5000;
    static int energy_Increase = 500;
    static int max_Treats = 5;
    static int max_SpecialTreats = 2;
    static int energy_Rate = 20;
    static int add_Chance = 10;
    static int add_SpecialChance = 100;
    static int remove_Chance = 50;
    static int remove_SpecialChance = 20;
    static int food_ScoreIncrease = 100;
    static int specialFood_ScoreIncrease = 250;
    static int max_FPS = 250;
    static boolean pause = false;
    static String highScoreName = null;
    
    static JButton play_Button = new JButton("PLAY");
    static JButton highscore_Button = new JButton("HIGH SCORES");
    static JButton settings_Button = new JButton("SETTINGS");
    static JButton return_Button = new JButton("RETURN");
    static JButton submit_Button = new JButton("SUBMIT");
    static JButton apply_Button = new JButton("Save Changes");
    
    static JTextField name_textField = new JTextField("ENTER NAME:", 5);
    static JTextField fpsInput= new JTextField(Integer.toString(max_FPS), 3);
    static JTextField speedInput = new JTextField(Integer.toString(speed), 2);
    static JTextField maxEnergyInput = new JTextField(Integer.toString(max_Energy), 4);
    static JTextField energyConsumeInput= new JTextField(Integer.toString(energy_Rate), 3);
    static JTextField maxTreatsInput = new JTextField(Integer.toString(max_Treats), 3);
    static JTextField lifetimeInput = new JTextField(Integer.toString(remove_Chance), 3);
    static JTextField spawnDelayInput = new JTextField(Integer.toString(add_Chance), 3);
    static JTextField maxSpecialInput = new JTextField(Integer.toString(max_SpecialTreats), 3);
    static JTextField specialLifetimeInput = new JTextField(Integer.toString(remove_SpecialChance), 3);
    static JTextField specialSpawnDelayInput = new JTextField(Integer.toString(add_SpecialChance), 3);
    
    boolean gameRunning = false;
    boolean gameLost = false;
    boolean settingsOpen = false;
    boolean highscoresOpen = false;
 
    Thread gameThread;
    Dir dir;
    long score;
    int nCols = (int) FRAME_WIDTH / 10;
    int nRows = (int) FRAME_HEIGHT / 10;
    int energy;
 
    int[][] grid;
    List<Point> snake, treats, specialTreats;
    Font smallFont;
 
    public Snake() {
        setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setBackground(Color.white);
        setFont(new Font("ComicSans", Font.BOLD, 48));
        setFocusable(true);
 
        smallFont = getFont().deriveFont(Font.BOLD, 18);
        initGrid();
 
        addKeyListener(new KeyAdapter() {
 
            @Override
            public void keyPressed(KeyEvent e) {
 
                switch (e.getKeyCode()) {
 
                    case KeyEvent.VK_UP:
                        if (dir != Dir.down)
                            dir = Dir.up;
                        break;
 
                    case KeyEvent.VK_LEFT:
                        if (dir != Dir.right)
                            dir = Dir.left;
                        break;
 
                    case KeyEvent.VK_RIGHT:
                        if (dir != Dir.left)
                            dir = Dir.right;
                        break;
 
                    case KeyEvent.VK_DOWN:
                        if (dir != Dir.up)
                            dir = Dir.down;
                        break;
                    case KeyEvent.VK_SPACE:
                    	pauseGame();
                }
            }
        });
    }
 
    void startNewGame() {
        gameRunning = true;
        gameLost = false;
 
        stop();
        initGrid();
        treats = new LinkedList<>();
        specialTreats = new LinkedList<>();
 
        dir = Dir.left;
        energy = max_Energy;
        score = 0;
 
        snake = new ArrayList<>();
        for (int x = 0; x < 7; x++)
            snake.add(new Point(nCols / 2 + x, nRows / 2));
 
        do
            addTreat();
        while(treats.isEmpty());
 
        (gameThread = new Thread(this)).start();
    }
 
    void stop() {
        if (gameThread != null) {
            Thread tmp = gameThread;
            gameThread = null;
            tmp.interrupt();
        }
    }
 
    void initGrid() {
        grid = new int[nRows][nCols];
        for (int x = 0; x < nRows; x++) {
            for (int y = 0; y < nCols; y++) {
                if (y == 0 || y == nCols - 1 || x == 0 || x == nRows - 1)
                    grid[x][y] = WALL;
            }
        }
    }
 
    @Override
    public void run() {
    	while (Thread.currentThread() == gameThread) {
    		while (pause) {
    			try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
				}
    			showPause();
    		}
    		try {
                Thread.sleep(Math.max(75 - (score * speed / 500), 25));
            } catch (InterruptedException e) {
                return;
            }
            
            if (energyUsed() || hitsWall() || hitsSnake()) {
                gameOver();
            } else {
            	char treat = eatsTreat();
                if (treat == 'n') {
                    score += food_ScoreIncrease;
                    energy += energy_Increase;
                    if (energy > max_Energy) {
                    	energy = max_Energy;
                    }
                    growSnake();
                } 
                if (treat == 's') {
                    score += specialFood_ScoreIncrease;
                    energy = max_Energy;
                    growSnake();
                } 
                
                renderMove();
            }
            repaint();
    	}
    }
 
    void renderMove() {
    	long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000/amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();
        long renderLastTime = System.nanoTime();
        double renderNs=1000000000/max_FPS;
        double renderDelta = 0;

        long now = System.nanoTime();
        delta += (now - lastTime) / ns;
        lastTime = now;
        while(delta >= 1){
            run();
            delta--;
        }

        now = System.nanoTime();
        renderDelta += (now - renderLastTime) / renderNs;
        renderLastTime = now;
        while(gameRunning && renderDelta >= 1){
        	moveSnake();
            addTreat();
            score++;
            renderDelta--;
        }

        if(System.currentTimeMillis() - timer > 1000){
            timer += 1000;
        }
        moveSnake();
        addTreat();
        score++;
    }
    
    boolean energyUsed() {
        energy -= energy_Rate;
        return energy <= 0;
    }
 
    boolean hitsWall() {
        Point head = snake.get(0);
        int nextCol = head.x + dir.x;
        int nextRow = head.y + dir.y;
        return grid[nextRow][nextCol] == WALL;
    }
 
    boolean hitsSnake() {
        Point head = snake.get(0);
        int nextCol = head.x + dir.x;
        int nextRow = head.y + dir.y;
        for (Point p : snake)
            if (p.x == nextCol && p.y == nextRow)
                return true;
        return false;
    }
 
    char eatsTreat() {
        Point head = snake.get(0);
        int nextCol = head.x + dir.x;
        int nextRow = head.y + dir.y;
        for (Point p : treats) {
            if (p.x == nextCol && p.y == nextRow) {
            	treats.remove(p);
                return 'n';
            }
        }
        for (Point p : specialTreats) {
            if (p.x == nextCol && p.y == nextRow) {
            	specialTreats.remove(p);
                return 's';
            }
        }
        return 'e';
    }
 
    void gameOver() {
        gameRunning = false;
        gameLost = true;
        stop();
    }
 
    void moveSnake() {
        for (int i = snake.size() - 1; i > 0; i--) {
            Point p1 = snake.get(i - 1);
            Point p2 = snake.get(i);
            p2.x = p1.x;
            p2.y = p1.y;
        }
        Point head = snake.get(0);
        head.x += dir.x;
        head.y += dir.y;
    }
 
    void growSnake() {
        Point tail = snake.get(snake.size() - 1);
        int x = tail.x + dir.x;
        int y = tail.y + dir.y;
        snake.add(new Point(x, y));
    }
 
    void addTreat() {
        if (treats.size() < max_Treats) {
 
            if (rand.nextInt(add_Chance) == 0) {
            	int x, y;
                    while (true) {
 
                        x = rand.nextInt(nCols);
                        y = rand.nextInt(nRows);
                        if (grid[y][x] != 0)
                            continue;
 
                        Point p = new Point(x, y);
                        if (snake.contains(p) || treats.contains(p) || specialTreats.contains(p))
                            continue;
 
                        treats.add(p);
                        break;
                    }
                } else if (rand.nextInt(remove_Chance) == 0) {
                    try {
                    	treats.remove(0);            
                    } catch (Exception e) {
                    }
                }    
        }
        if (specialTreats.size() < max_SpecialTreats) {
        	 
            if (rand.nextInt(add_SpecialChance) == 0) {
            	int x, y;
                    while (true) {
 
                        x = rand.nextInt(nCols);
                        y = rand.nextInt(nRows);
                        if (grid[y][x] != 0)
                            continue;
 
                        Point p = new Point(x, y);
                        if (snake.contains(p) || treats.contains(p) || specialTreats.contains(p))
                            continue;
 
                        specialTreats.add(p);
                        break;
                    }
                } else if (specialTreats.size() > max_SpecialTreats || rand.nextInt(remove_SpecialChance) == 0) {
                    try {
                    	specialTreats.remove(0);            
                    } catch (Exception e) {
                    }
                }    
        }
    }
 
    void drawGrid(Graphics2D g) {
    	clearButtons();
    	
        g.setColor(Color.lightGray);
        for (int x = 0; x < nRows; x++) {
            for (int y = 0; y < nCols; y++) {
                if (grid[x][y] == WALL)
                    g.fillRect(y * 10, x * 10, 10, 10);
            }
        }
    }
 
    void drawSnake(Graphics2D g) {
        g.setColor(Color.blue);
        for (Point p : snake)
            g.fillRect(p.x * 10, p.y * 10, 10, 10);
 
        g.setColor((energy < max_Energy / 4) ? Color.red : Color.orange);
        Point head = snake.get(0);
        g.fillRect(head.x * 10, head.y * 10, 10, 10);
    }
 
    void drawTreats(Graphics2D g) {
        g.setColor(Color.green);
        for (Point p : treats) {
            g.fillRect(p.x * 10, p.y * 10, 10, 10);
        }
        g.setColor(Color.yellow);
        for (Point p : specialTreats) {
        	g.fillRect(p.x * 10, p.y * 10, 10, 10);
        }
    }
 
    public class play implements ActionListener {
    	public void actionPerformed(ActionEvent arg0) {
    		startNewGame();
		}
    }
    
    public class showSettings implements ActionListener {
    	public void actionPerformed(ActionEvent arg0) {
    		settingsOpen = true;
    		repaint();
		}
    }
    
    public class showHighScores implements ActionListener {
    	public void actionPerformed(ActionEvent arg0) {
    		highscoresOpen = true;
    		repaint();
		}
    }
    
    public class returns implements ActionListener {
    	public void actionPerformed(ActionEvent arg0) {
    		Graphics2D g =  (Graphics2D) getGraphics();
    		settingsOpen = false;
    		highscoresOpen = false;
    		gameRunning = false;
    		gameLost = false;
    		
    		clearButtons();
    		drawStartScreen(g);
    		repaint();
		}
    }
    
    public class submit implements ActionListener {
    	public void actionPerformed(ActionEvent arg0) {
    		boolean validName = false;
    		String name = name_textField.getText();
    		if (name.length() <= 5 && name.length() > 0 ) {
    			validName = true;
    		}
    		if (validName) {
    			remove(submit_Button);
    			remove(name_textField);
    			revalidate();
    			highScoreName = name;
    			repaint();
    		} else {
    			name = null;
    		}
		}
    }
    
    public class apply implements ActionListener {
    	public void actionPerformed(ActionEvent arg0) {
    		Graphics g = getGraphics();
    		g.setFont(smallFont);
    		try {
    			max_FPS = Integer.parseInt(fpsInput.getText());
    			speed = Integer.parseInt(speedInput.getText());
    			max_Energy = Integer.parseInt(maxEnergyInput.getText());
    			energy_Rate = Integer.parseInt(energyConsumeInput.getText());
    			max_Treats = Integer.parseInt(maxTreatsInput.getText());
    			remove_Chance = Integer.parseInt(lifetimeInput.getText());
    			add_Chance = Integer.parseInt(spawnDelayInput.getText());
    			max_SpecialTreats = Integer.parseInt(maxSpecialInput.getText());
    			remove_SpecialChance = Integer.parseInt(specialLifetimeInput.getText());
    			add_SpecialChance = Integer.parseInt(specialSpawnDelayInput.getText());
    			
    			g.drawString("Changes Made", 250, 390);
    		} catch(Exception e) {
    			g.drawString("Error", 250, 390);
    		}
    		repaint();
		}
    }
    
    public class maxTreatsClicked implements ActionListener {
    	public void actionPerformed(ActionEvent arg0) {
    		maxTreatsInput.requestFocus();
    	}
    }
    
    void openSettings() {
    	
    	JLabel fps = new JLabel("FPS");
    	add(fps);
    	fps.setLocation(50,20);
    	fps.setSize(150, 30);
		fps.setForeground(Color.black);
		
    	add(fpsInput);
    	fpsInput.setLocation(250, 20);
    	fpsInput.setSize(150, 30);
    	fpsInput.setBackground(Color.lightGray);
    	fpsInput.setForeground(Color.black);
    	
    	JLabel speedLabel = new JLabel("Speed");
    	add(speedLabel);
    	speedLabel.setLocation(50, 55);
    	speedLabel.setSize(150, 30);
    	speedLabel.setForeground(Color.black);
    	
    	
    	add(speedInput);
    	speedInput.setLocation(250, 55);
    	speedInput.setSize(150, 30);
    	speedInput.setBackground(Color.lightGray);
    	speedInput.setForeground(Color.black);
		
    	JLabel maxEnergy = new JLabel("Maximum Energy");
    	add(maxEnergy);
    	maxEnergy.setLocation(50, 90);
    	maxEnergy.setSize(150, 30);
    	maxEnergy.setForeground(Color.black);
    	
    	
    	add(maxEnergyInput);
    	maxEnergyInput.setLocation(250, 90);
    	maxEnergyInput.setSize(150, 30);
    	maxEnergyInput.setBackground(Color.lightGray);
    	maxEnergyInput.setForeground(Color.black);
    	
    	JLabel energyConsume = new JLabel("Energy Consumption Rate");
    	add(energyConsume);
    	energyConsume.setLocation(50, 125);
    	energyConsume.setSize(150, 30);
    	energyConsume.setForeground(Color.black);
     
    	add(energyConsumeInput);
    	energyConsumeInput.setLocation(250, 125);
    	energyConsumeInput.setSize(150, 30);
    	energyConsumeInput.setBackground(Color.lightGray);
    	energyConsumeInput.setForeground(Color.black);
    	
    	JLabel maxTreats = new JLabel("Maximum Treats");
    	add(maxTreats);
    	maxTreats.setLocation(50, 160);
    	maxTreats.setSize(150, 30);
    	maxTreats.setForeground(Color.black);
    	
    	add(maxTreatsInput);
    	maxTreatsInput.setLocation(250, 160);
    	maxTreatsInput.setSize(150, 30);
    	maxTreatsInput.setBackground(Color.lightGray);
    	maxTreatsInput.setForeground(Color.black);
    	maxTreatsInput.addActionListener(new maxTreatsClicked());
    	
    	JLabel lifetime = new JLabel("Treat Lifetime");
    	add(lifetime);
    	lifetime.setLocation(50, 195);
    	lifetime.setSize(150, 30);
    	lifetime.setForeground(Color.black);
    	
    	add(lifetimeInput);
    	lifetimeInput.setLocation(250, 195);
    	lifetimeInput.setSize(150, 30);
    	lifetimeInput.setBackground(Color.lightGray);
    	lifetimeInput.setForeground(Color.black);
    	
    	JLabel spawnDelay = new JLabel("Treat Spawn Delay");
    	add(spawnDelay);
    	spawnDelay.setLocation(50, 230);
    	spawnDelay.setSize(150, 30);
    	spawnDelay.setForeground(Color.black);
    	
    	add(spawnDelayInput);
    	spawnDelayInput.setLocation(250, 230);
    	spawnDelayInput.setSize(150, 30);
    	spawnDelayInput.setBackground(Color.lightGray);
    	spawnDelayInput.setForeground(Color.black);
    	
    	JLabel maxSpecial = new JLabel("Maximum Special Treats");
    	add(maxSpecial);
    	maxSpecial.setLocation(50, 265);
    	maxSpecial.setSize(150, 30);
    	maxSpecial.setForeground(Color.black);
    	
    	add(maxSpecialInput);
    	maxSpecialInput.setLocation(250, 265);
    	maxSpecialInput.setSize(150, 30);
    	maxSpecialInput.setBackground(Color.lightGray);
    	maxSpecialInput.setForeground(Color.black);
    	
    	JLabel specialLifetime = new JLabel("Special Treat Lifetime");
    	add(specialLifetime);
    	specialLifetime.setLocation(50, 300);
    	specialLifetime.setSize(150, 30);
    	specialLifetime.setForeground(Color.black);
    	
    	add(specialLifetimeInput);
    	specialLifetimeInput.setLocation(250, 300);
    	specialLifetimeInput.setSize(150, 30);
    	specialLifetimeInput.setBackground(Color.lightGray);
    	specialLifetimeInput.setForeground(Color.black);
    	
    	JLabel specialSpawnDelay = new JLabel("Special Treat Spawn Delay");
    	add(specialSpawnDelay);
    	specialSpawnDelay.setLocation(50, 335);
    	specialSpawnDelay.setSize(150, 30);
    	specialSpawnDelay.setForeground(Color.black);
    	
    	add(specialSpawnDelayInput);
    	specialSpawnDelayInput.setLocation(250, 335);
    	specialSpawnDelayInput.setSize(150, 30);
    	specialSpawnDelayInput.setBackground(Color.lightGray);
    	specialSpawnDelayInput.setForeground(Color.black);
    	
    	apply apply = new apply();
    	add(apply_Button);
    	apply_Button.setLocation(50, 370);
    	apply_Button.setSize(150, 30);
    	apply_Button.setBackground(Color.lightGray);
    	apply_Button.setForeground(Color.black);
    	apply_Button.addActionListener(apply);
    	}
    
    void blankReturnScreen(Graphics2D g) {
    	clearButtons();
    	
    	g.clearRect(0, 0, FRAME_WIDTH, FRAME_HEIGHT);
    	
    	drawGrid(g);
    	
    	returns returns = new returns();
        add(return_Button);
        return_Button.setFocusable(false);
        return_Button.setLocation(0, 0);
        return_Button.addActionListener(returns);
        return_Button.setSize(100, 25);
        return_Button.setBackground(Color.lightGray);
        return_Button.setForeground(Color.blue);
    }
    
    void openHighScores(Graphics2D g) {
    	blankReturnScreen(g);
    	score = 0;
    	drawHighScores(g);
    }
    
    void drawHighScores(Graphics2D g) {
    	String[] names = new String[5];
    	long[] scores = new long[5];
    	
    	int index = 0;
    	String fileName = "C:\\Users\\admin\\Documents\\My Games\\Java\\Snake\\Snake\\src\\save.txt";
    	try {
			Scanner scanner = new Scanner(new File(fileName));
			while (scanner.hasNextLine()) {
				if (index % 2 == 0) {
					names[index / 2] = scanner.nextLine();
				} else {
					scores[(index - 1) / 2] = Long.parseLong(scanner.nextLine());
				}
				index++;
			}
			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    
    	g.setFont(smallFont);
    	
    	if (score > scores[4]) {
    		writeFile(g, names, scores, fileName);
    	} else {
    		g.drawString("Name:", 160, 150);
			g.drawString("Score:", 360, 150);
			g.setColor(Color.lightGray);
			 
    		for (int x = 0; x < 5; x++) {
        			g.drawString(names[x], 160, 175 + (25 * x));
        			g.drawString(Long.toString(scores[x]), 360, 175 + (25 * x));
        		}
    	}
    }
    
    void writeFile(Graphics2D g, String[] names, long[] scores, String fileName) {
    	boolean scoreWritten = false;
    	int userX = 5;
    	
    	g.drawString("Name:", 160, 125);
		g.drawString("Score:", 360, 125);
		g.setColor(Color.gray);
		
    	for (int x = 0; x < 5; x++) {
    		if (score < scores[x] || scoreWritten) {
    			g.drawString(names[x], 160, 150 + (25 * x));
    			g.drawString(Long.toString(scores[x]), 360, 150 + (25 * x));
    		} else {
    			for (int y = 4; y > x; y--) {
    				names[y] = names[y--];
    				scores[y] = scores[y--];
    			}
    			enterHighscoreName(g, x);
    			names[x] = highScoreName;
    			scores[x] = score;
    			scoreWritten = true;
    			userX = x;
    		}
    	}
    	
    	if (scoreWritten && names[userX] != null) {
    		FileWriter fileWriter = null;
			try {
				fileWriter = new FileWriter(fileName);
			} catch (IOException e) {
			}
    		PrintWriter printWriter = new PrintWriter(fileWriter);
    		for (int x = 0; x < 5; x++) {
    			printWriter.println(names[x]);
    			printWriter.println(scores[x]);
    		}
    		printWriter.close();
    		scoreWritten = false;
    		score = 0;
    	}
    	
    }
    
    void enterHighscoreName(Graphics2D g, int position) {
    	g.drawString(Long.toString(score), 360, 150 + (25 * position));
    	
		add(name_textField);
		name_textField.setLocation(160, 130 + (25 * position));
		name_textField.setSize(150, 30);
		name_textField.setBackground(Color.white);
		name_textField.setForeground(Color.black);
		
		submit submit = new submit();
        add(submit_Button);        
        submit_Button.setFocusable(false);
        submit_Button.setLocation(getWidth() / 2 - 75, getHeight() - 100);
        submit_Button.addActionListener(submit);
        submit_Button.setSize(150, 30);
        submit_Button.setBackground(Color.lightGray);
        submit_Button.setForeground(Color.blue);
     }
    
    void clearButtons() {
    	removeAll();
    }
    
    void pauseGame() {
    	pause = (pause) ? false : true;
    }
    
    void showPause() {
    	Graphics g = getGraphics();
    	Font pauseFont = new Font("ComicSans", Font.BOLD, 128);
    	g.setColor(Color.gray);
    	g.setFont(pauseFont);
    	g.drawString("PAUSED", 60, 250);
    }
    
    void drawStartScreen(Graphics2D g) {
    	g.setColor(Color.blue);
        g.setFont(getFont());
        g.drawString("Snake", 240, 190);
        g.setColor(Color.orange);
        
        showHighScores showHighScores = new showHighScores();
        add(highscore_Button);        
        highscore_Button.setFocusable(false);
        highscore_Button.setLocation(getWidth() / 4 - 75, getHeight() - 50);
        highscore_Button.addActionListener(showHighScores);
        highscore_Button.setSize(150, 30);
        highscore_Button.setBackground(Color.lightGray);
        highscore_Button.setForeground(Color.blue);
        
        play play = new play();
        add(play_Button);
        play_Button.setFocusable(false);
        play_Button.setLocation(getWidth() / 2 - 75, getHeight() - 50);
        play_Button.addActionListener(play);
        play_Button.setSize(150, 30);
        play_Button.setBackground(Color.lightGray);
        play_Button.setForeground(Color.blue);

        showSettings showSettings = new showSettings();
        add(settings_Button);
        settings_Button.setFocusable(false);
        settings_Button.setLocation(getWidth() * 3 / 4 - 75, getHeight() - 50);
        settings_Button.addActionListener(showSettings);
        settings_Button.setSize(150, 30);
        settings_Button.setBackground(Color.lightGray);
        settings_Button.setForeground(Color.blue);
    }
 
    void drawStats(Graphics2D g) {
        g.setFont(smallFont);
        g.setColor(getForeground());
        String s = format("score: %d", score);
        g.drawString(s, 30, getHeight() - 30);
        g.drawString(format("energy: %d", energy), getWidth() - 150, getHeight() - 30);
    }
 
    @Override
    public void paintComponent(Graphics gg) {
        super.paintComponent(gg);
        Graphics2D g = (Graphics2D) gg;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
 
        drawGrid(g);
 
        if (gameRunning) {
        	drawSnake(g);
            drawTreats(g);
            drawStats(g);
        }
        else if (gameLost) {
        	drawGameOverScreen(g);
        }
        else if (settingsOpen) {
        	blankReturnScreen(g);
        	openSettings();
        	repaint();
        }
        else if (highscoresOpen) {
        	openHighScores(g);
        }
        else {
        	g.clearRect(0, 0, getWidth(), getHeight());
        	clearButtons();
        	drawStartScreen(g);
        }
    }
 
    void drawGameOverScreen(Graphics2D g) {
    	g.setColor(Color.black);
    	g.drawString("Game Over!", 160, 75);
    	
    	drawHighScores(g);
    	
    	play play = new play();
        add(play_Button);
        play_Button.setFocusable(false);
        play_Button.setLocation(getWidth() / 2 - 165, getHeight() - 50);
        play_Button.addActionListener(play);
        play_Button.setSize(150, 30);
        play_Button.setBackground(Color.lightGray);
        play_Button.setForeground(Color.blue);
       
        returns returns = new returns();
        add(return_Button);
        return_Button.setFocusable(false);
        return_Button.setSize(150, 30);
        return_Button.setLocation(getWidth() / 2 + 15, getHeight() - 50);
        return_Button.addActionListener(returns);
        return_Button.setBackground(Color.lightGray);
        return_Button.setForeground(Color.blue);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setTitle("Snake");
            frame.setResizable(false);
            frame.add(new Snake(), BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}