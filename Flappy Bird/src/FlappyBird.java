import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.*;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 360;
    int boardHeight = 640;

    private Font customFont;
    // Images
    Image backgroundImg;
    Image birdImg;
    Image topPipeImg;
    Image bottomPipeImg;

    // Bird
    int birdX = boardWidth / 8;
    int birdY = boardHeight / 2;
    int birdWidth = 34;
    int birdHeight = 24;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image img;

        Bird(Image img) {
            this.img = img;

        }
    }

    // Pipes
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64;
    int pipeHeight = 512;

    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;

        boolean passed = false;

        Pipe(Image img) {
            this.img = img;
        }
    }

    // game logic
    Bird bird;
    int velocityX = -4;
    int velocityY = 0;
    int gravity = 1;

    ArrayList<Pipe> pipes;
    Random random = new Random();

    Timer gameLoop;
    Timer placePipesTimer;

    boolean gameOver = false;
    double score = 0;
    int highScore = 0;

    private static final String HIGH_SCORE_KEY = "flappyBirdHighScore";

    private Preferences prefs;
    JButton restartButton;

    FlappyBird() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        // setBackground(Color.blue);
        setFocusable(true);
        addKeyListener(this);

        loadCustomFont();

        prefs = Preferences.userNodeForPackage(FlappyBird.class);
        highScore = prefs.getInt(HIGH_SCORE_KEY, 0);

        // load images
        backgroundImg = new ImageIcon(getClass().getResource("./flappybirdbg.png")).getImage();
        birdImg = new ImageIcon(getClass().getResource("./flappybird.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("./toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("./bottompipe.png")).getImage();
        // bird
        bird = new Bird(birdImg);
        pipes = new ArrayList<Pipe>();

        // Restart Button
        restartButton = new JButton("Restart");
        restartButton.setFont(customFont.deriveFont(Font.BOLD, 18));
        restartButton.setBounds(boardWidth / 2 - 60, boardHeight / 2, 120, 40);
        restartButton.addActionListener(e -> restartGame());
        restartButton.setVisible(false);

        setLayout(null);
        add(restartButton);

        // place pipes timer
        placePipesTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placePipes();
            }
        });
        placePipesTimer.start();

        // game timer
        gameLoop = new Timer(1000 / 60, this); // 1000/60=16.6
        gameLoop.start();

    }

    private void loadCustomFont() {
        try {
            InputStream is = getClass().getResourceAsStream("./PressStart2P-Regular.ttf");
            customFont = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
        } catch (Exception e) {
            e.printStackTrace();
            customFont = new Font("Arial", Font.PLAIN, 12);
        }
    }

    public void placePipes() {
        // (0-1) * pipeHeight/2 -> 0-256
        // 128
        // 0 - 128 - (0 - 256) --> pipeHeight/4 -> 3/4 pipeHeight

        int randomPipeY = (int) (pipeY - pipeHeight / 4 - Math.random() * (pipeHeight / 2));
        int openingSpace = boardHeight / 4;

        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);

        Pipe bottomPipe = new Pipe(bottomPipeImg);
        bottomPipe.y = topPipe.y + pipeHeight + openingSpace;
        pipes.add(bottomPipe);
    }

    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        System.out.println("draw");
        // background
        g.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);

        // bird
        g.drawImage(bird.img, bird.x, bird.y, bird.width, bird.height, null);

        // pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        // score and highscore
        g.setColor(Color.white);
        g.setFont(customFont.deriveFont(18f));
        g.drawString(String.valueOf((int) score), 10, 30);
        g.drawString("High: " + highScore, boardWidth - 150, 30);

        if (gameOver) {
            g.setFont(customFont.deriveFont(Font.BOLD, 20f));
            g.drawString("Game Over!", boardWidth / 2 - 80, boardHeight / 2 - 40);

            g.setFont(customFont.deriveFont(18f));
            g.drawString("Score: " + (int) score, boardWidth / 2 - 60, boardHeight / 2);
            g.drawString("High Score: " + highScore, boardWidth / 2 - 110, boardHeight / 2 + 30);

            restartButton.setFont(customFont.deriveFont(10f));
            restartButton.setBounds(boardWidth / 2 - 50, boardHeight / 2 + 60, 130, 50);
            restartButton.setVisible(true);
        }

    }

    public void move() {
        // bird
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0);

        // pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;

            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                pipe.passed = true;
                score += 0.5;
            }

            if (collision(bird, pipe)) {
                gameOver = true;
                updateHighScore();
                restartButton.setVisible(true);
            }
        }

        if (bird.y > boardHeight) {
            gameOver = true;
            updateHighScore();
            restartButton.setVisible(true);
        }
    }

    private void updateHighScore() {
        if ((int) score > highScore) {
            highScore = (int) score;
            // save new record
            prefs.putInt(HIGH_SCORE_KEY, highScore);
            try {
                prefs.flush(); // save changes
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean collision(Bird a, Pipe b) {
        return a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();

        if (gameOver) {
            placePipesTimer.stop();
            gameLoop.stop();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            velocityY = -9;
        }
    }

    public void restartGame() {
        bird.y = birdY;
        velocityY = 0;
        pipes.clear();
        score = 0;
        gameOver = false;
        restartButton.setVisible(false);
        gameLoop.start();
        placePipesTimer.start();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}