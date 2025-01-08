package ca.ucalgary.cpsc.frogger;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;


public class Frogger extends Application {
    // Empty canvas that we can add stuff onto
    private final Pane root = new Pane();

    // initializing the screen width and height
    private static final int SCREEN_WIDTH = 630;  // the x value of our screen
    private static final int SCREEN_HEIGHT = 630; // the y value of our screen

    // global variables
    private int score = 0;
    private Label scoreLabel = null;
    private Label livesLabel = null;

    // global variables for the speed of the car and log, will increase later
    public static double leftCarMovement = 1;
    public static double rightCarMovement = 1.1;

    public static double leftLogMovement = 1;
    public static double rightLogMovement = 1.1;


    // creates the player sprites
    private final Sprite player = new Sprite((UNIT_SIZE*10), (SCREEN_HEIGHT - UNIT_SIZE), UNIT_SIZE , UNIT_SIZE, "player","infinite" ,0, Color.INDIGO);

    // creates the road zone with the starting x, y, size, tag, and image
    private final Sprite roadZone = new Sprite(0,(SCREEN_HEIGHT - 10*UNIT_SIZE), SCREEN_WIDTH, 10*UNIT_SIZE, "zone","null" ,99, Color.GREY);

    // creates the safe zones with the starting x, y, size, tag, and image
    private final Sprite startingSafeZone1 = new Sprite(0, (SCREEN_HEIGHT - UNIT_SIZE), SCREEN_WIDTH, UNIT_SIZE, "zone","null", 99, Color.GREEN);
    private final Sprite startingSafeZone2 = new Sprite(0, (SCREEN_HEIGHT - 7*UNIT_SIZE), SCREEN_WIDTH, UNIT_SIZE, "zone","null", 99, Color.GREEN);
    private final Sprite startingSafeZone3 = new Sprite(0,(SCREEN_HEIGHT - 11*UNIT_SIZE), SCREEN_WIDTH, UNIT_SIZE, "zone","null" ,99, Color.GREEN);

    // creates the zone (places of the map where it differs)
    private final Sprite waterZone = new Sprite(0, (UNIT_SIZE), SCREEN_WIDTH, 9*UNIT_SIZE, "waterZone","null", 99, Color.DEEPSKYBLUE);

    // how we split it into the grids
    private static final int UNIT_SIZE = 30;

    // to stop the tick generator when the player/user lost the game
    private AnimationTimer gameCollisionTimer = null;
    private AnimationTimer gameOverAndLoopTimer = null;

    // our main pane/screen where our game runs (frogger game with game logic and all)
    private Parent createContent() {
        // set the size of the pane (displayed window)
        root.setPrefSize(SCREEN_WIDTH,SCREEN_HEIGHT);

        // adds the zones to the window
        root.getChildren().add(roadZone);
        root.getChildren().add(waterZone);
        root.getChildren().add(startingSafeZone1);
        root.getChildren().add(startingSafeZone2);
        root.getChildren().add(startingSafeZone3);

        // game logic that will always check in the background (a.k.a the movement of everything besides the player)
        gameCollisionTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {

                // car movement is running and the lily pads, collision is also in this method
                collisionCar();

                // log movement is running and the collision is also in this method
                collisionLog();
            }
        };

        // game logic to wrap around all the cars and logs
        gameOverAndLoopTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {

                // checks if the car or log is off the screen so it can re-appear on the other side
                try {
                    loopAround();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        // starts the loop (timer)
        gameOverAndLoopTimer.start();
        gameCollisionTimer.start();

        // adds the car method (which initialize the cars)
        cars();

        // adds the log method (which initialize the logs)
        logs();

        // adds the lilly pads method (which initialize the lilly pads)
        lillyPads();

        //
        scoreLabel = new Label("Score: " + score);
        scoreLabel.setLayoutY(0);
        scoreLabel.setLayoutX(3);
        scoreLabel.setFont(new Font("Rockwell", 20));
        scoreLabel.setLineSpacing(4.3);
        root.getChildren().add(scoreLabel);

        livesLabel = new Label("Lives: " + player.lives);
        livesLabel.setLayoutX(554);
        livesLabel.setLayoutY(0);
        livesLabel.setFont(new Font("Rockwell", 20));
        root.getChildren().add(livesLabel);

        root.getChildren().add(player); // to display => add it into our Pane();

        // display root aka gameEngine
        return root;
    }

    private void gameOver() throws IOException {
        if (player.lives <= 0){

            writeToFile(playerName, String.valueOf(score));

            Rectangle blackScreen = new Rectangle(SCREEN_HEIGHT,SCREEN_WIDTH, Color.BLACK);
            blackScreen.setY(0);
            blackScreen.setX(0);
            root.getChildren().add(blackScreen);

            HBox hBox = new HBox();
            hBox.setTranslateX(365);
            hBox.setTranslateY(365);
            root.getChildren().add(hBox);

            Label gameOverLabel = new Label("Game Over");
            gameOverLabel.setTextFill(Color.RED);
            gameOverLabel.setFont(new Font("Rockwell", 50));
            gameOverLabel.setLayoutX(SCREEN_WIDTH/3 - UNIT_SIZE);
            gameOverLabel.setLayoutY(SCREEN_HEIGHT/3);

            root.getChildren().add(gameOverLabel);
            gameCollisionTimer.stop();
            gameOverAndLoopTimer.stop();

        }

    }

    // writes player's high score to a highScore text file
    private void writeToFile(String name, String score) throws IOException {
        File file = new File("score/highScores.txt");
        Scanner kbReader = new Scanner(file);
        String str = "";

        while(kbReader.hasNextLine()){
            str += kbReader.nextLine() + " ";
        }
        String highScores[] = str.split(" ");

        String oldName = "";
        String oldScore = "";

        for (int i = 2; i < highScores.length; i = i + 3) {
            try {
                if (Integer.parseInt(score) > Integer.parseInt(highScores[i])) {
                    oldName = highScores[i-1];
                    oldScore = (highScores[i]);
                    highScores[i-1] = name;
                    highScores[i] = score;
                    name = oldName;
                    score = oldScore;
                }
            } catch (Exception e) {
                oldName = highScores[i-1];
                oldScore = highScores[i];
                highScores[i-1] = name;
                highScores[i] = score;
                name = oldName;
                score = oldScore;
            }
        }
        String saveHighScore = "";
        for (int i = 2; i < highScores.length; i  = i + 3) {
            saveHighScore += highScores[i-2] + " " + highScores[i-1] + " " + highScores[i] + "\n";
        }
        FileWriter fw = new FileWriter("score/highScores.txt");
        PrintWriter pw = new PrintWriter(fw);

        pw.print(saveHighScore);
        pw.close();
    }

    private static int lilLength = UNIT_SIZE * 3;
    // create the lilly pads and add them to the game screen
    private void lillyPads () {

        // the lilly pads in between the logs --> 3 lilly pads
        Sprite lillyPad = new Sprite((UNIT_SIZE), (SCREEN_HEIGHT - (15 * UNIT_SIZE)), lilLength, UNIT_SIZE, "lil","15" ,99, Color.LIGHTGREEN);
        root.getChildren().add(lillyPad);
        lillyPad = new Sprite((UNIT_SIZE * 9), (SCREEN_HEIGHT - (15 * UNIT_SIZE)), lilLength, UNIT_SIZE, "lil1", "15" ,99, Color.LIGHTGREEN);
        root.getChildren().add(lillyPad);
        lillyPad = new Sprite((SCREEN_WIDTH - 4 * UNIT_SIZE), SCREEN_HEIGHT - (15 * UNIT_SIZE), lilLength, UNIT_SIZE,"lil2", "15" ,99, Color.LIGHTGREEN);
        root.getChildren().add(lillyPad);

        // the lilly pads in the final lane --> 3 lilly pads
        lillyPad = new Sprite((UNIT_SIZE),  UNIT_SIZE, 2*UNIT_SIZE, UNIT_SIZE, "endLil1", "19" ,99, Color.LIGHTGREEN);
        root.getChildren().add(lillyPad);
        lillyPad = new Sprite((UNIT_SIZE * 9), UNIT_SIZE, lilLength, UNIT_SIZE, "endLil2", "19" ,99, Color.LIGHTGREEN);
        root.getChildren().add(lillyPad);
        lillyPad = new Sprite((SCREEN_WIDTH - 3*UNIT_SIZE), UNIT_SIZE, 2*UNIT_SIZE, UNIT_SIZE, "endLil3", "19" ,99, Color.LIGHTGREEN);
        root.getChildren().add(lillyPad);
    }


    private int logLength = UNIT_SIZE * 7;
    // create the logs and add them to the game screen
    private void logs() {
        int height = UNIT_SIZE;

        // lane 12 --> 3 logs
        Sprite log = new Sprite((UNIT_SIZE), (SCREEN_HEIGHT - ( 12* UNIT_SIZE) ), logLength, height, "log", "12,1",0, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE* 28), (SCREEN_HEIGHT - ( 12* UNIT_SIZE) ), logLength, height, "log", "12,2",0, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE * 14), (SCREEN_HEIGHT - (12 * UNIT_SIZE)), logLength, height, "log", "12,3",0, Color.SADDLEBROWN);
        root.getChildren().add(log);

        // lane 13 --> 3 logs
        log = new Sprite((UNIT_SIZE* 11), (SCREEN_HEIGHT - (13 * UNIT_SIZE)), logLength, height, "log", "13,1",1, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE), (SCREEN_HEIGHT - (13 * UNIT_SIZE)), logLength, height, "log", "13,2",1, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE * 23), (SCREEN_HEIGHT - (13 * UNIT_SIZE)), logLength, height, "log", "13,3",1, Color.SADDLEBROWN);
        root.getChildren().add(log);

        // lane 14 --> 3 logs
        log = new Sprite((UNIT_SIZE * 2), (SCREEN_HEIGHT - (14 * UNIT_SIZE)), logLength,height, "log", "14,1",1, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE * 13), (SCREEN_HEIGHT - (14 * UNIT_SIZE)), logLength, height, "log", "14,2",1, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE * 27), (SCREEN_HEIGHT - (14 * UNIT_SIZE)), logLength, height, "log", "14,3",1, Color.SADDLEBROWN);
        root.getChildren().add(log);

        // lane 15 holds the first lilly pads

        // lane 16 --> 2 logs
        log = new Sprite((UNIT_SIZE ), (SCREEN_HEIGHT - (16 * UNIT_SIZE)), logLength, height, "log", "16,1",0, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE * 15), (SCREEN_HEIGHT - (16 * UNIT_SIZE)), logLength, height, "log", "16,2",0, Color.SADDLEBROWN);
        root.getChildren().add(log);

        // lane 17 --> 3 logs
        log = new Sprite((UNIT_SIZE), (SCREEN_HEIGHT - (17 * UNIT_SIZE)), logLength,height, "log","17,1", 1, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE * 18), (SCREEN_HEIGHT - (17 * UNIT_SIZE)), logLength, height, "log", "17,2",1, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE * 27), (SCREEN_HEIGHT - (17 * UNIT_SIZE)), logLength, height, "log", "17,3",1, Color.SADDLEBROWN);
        root.getChildren().add(log);

        // lane 18 --> 3 logs
        log = new Sprite((UNIT_SIZE * 3), (SCREEN_HEIGHT - (18 * UNIT_SIZE)), logLength,height, "log", "18,1",0, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE * 19), (SCREEN_HEIGHT - (18 * UNIT_SIZE)), logLength, height, "log", "18,2",0, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE * 30), (SCREEN_HEIGHT - (18 * UNIT_SIZE)), logLength, height, "log", "18,3",0, Color.SADDLEBROWN);
        root.getChildren().add(log);

        // lane 19 --> 2 logs
        log = new Sprite((UNIT_SIZE * 28), (SCREEN_HEIGHT - (19 * UNIT_SIZE)), logLength, height, "log", "19,1",1, Color.SADDLEBROWN);
        root.getChildren().add(log);
        log = new Sprite((UNIT_SIZE * 5), (SCREEN_HEIGHT - (19 * UNIT_SIZE)), logLength, height, "log","19,2", 1, Color.SADDLEBROWN);
        root.getChildren().add(log);
    }

    private final int carWidth = UNIT_SIZE + 5;
    // create the cars and add them to the game screen
    private void cars(){
        // instead of seeing numbers we can use this to keep track of the variables
        int carPosX = UNIT_SIZE + 1;
        int carPosY = - (UNIT_SIZE) + 1;
        final int carHeight = UNIT_SIZE - 4;

        //  lane 1  -- > 3 cars spread out
        Sprite car = new Sprite(carPosX, (SCREEN_HEIGHT + carPosY - (UNIT_SIZE)),carWidth, carHeight, "car", "1,1",0, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX* 7), (SCREEN_HEIGHT + carPosY - (UNIT_SIZE)),carWidth, carHeight, "car", "1,2",0, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX* 14), (SCREEN_HEIGHT + carPosY - (UNIT_SIZE)),carWidth, carHeight, "car", "1,3",0, Color.RED);
        root.getChildren().add(car);

        // lane 2  --> 2 cars
        car = new Sprite((carPosX*5), (SCREEN_HEIGHT + 2*carPosY - (UNIT_SIZE + 1)),carWidth, carHeight, "car", "2,1",1, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX* 13), (SCREEN_HEIGHT + 2*carPosY - (UNIT_SIZE+1) ),carWidth, carHeight, "car", "2,2",1, Color.RED);
        root.getChildren().add(car);

        // lane 3 --> 2 cars
        car = new Sprite((carPosX * 2), (SCREEN_HEIGHT + 3*carPosY - (UNIT_SIZE + 1)),carWidth, carHeight, "car","3,1", 0, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX* 12), (SCREEN_HEIGHT + 3*carPosY - (UNIT_SIZE+1)),carWidth, carHeight, "car", "3,2",0, Color.RED);
        root.getChildren().add(car);

        // lane 4 --> 3 cars
        car = new Sprite((carPosX), (SCREEN_HEIGHT + 4*carPosY - (UNIT_SIZE + 1)),carWidth, carHeight, "car", "4,1",1, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX * 20), (SCREEN_HEIGHT + 4*carPosY - (UNIT_SIZE + 1)),carWidth, carHeight, "car", "4,2",1, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX * 10), (SCREEN_HEIGHT + 4*carPosY - (UNIT_SIZE + 1)),carWidth, carHeight, "car", "4,3",1, Color.RED);
        root.getChildren().add(car);

        // lane 5 --> 4 cars
        car = new Sprite((0), (SCREEN_HEIGHT + 5*carPosY - (UNIT_SIZE + 2)),carWidth, carHeight, "car", "5,1",0, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX * 11), (SCREEN_HEIGHT + 5*carPosY - (UNIT_SIZE + 2)),carWidth, carHeight, "car", "5,2",0, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX * 19), (SCREEN_HEIGHT + 5*carPosY - (UNIT_SIZE + 2)),carWidth, carHeight, "car", "5,3",0, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX * 7), (SCREEN_HEIGHT + 5*carPosY - (UNIT_SIZE + 2)),carWidth, carHeight, "car","5,4", 0, Color.RED);
        root.getChildren().add(car);

        // lane 6 holds a safe zone (#2)

        // lane 7 -- > 2 cars
        car = new Sprite((carPosX * 5), (SCREEN_HEIGHT + 7*carPosY - (UNIT_SIZE+4)),carWidth, carHeight, "car", "7,1",1, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX * 10), (SCREEN_HEIGHT + 7*carPosY - (UNIT_SIZE+4)),carWidth, carHeight, "car", "7,2",1, Color.RED);
        root.getChildren().add(car);

        // lane 8 -- > 4 cars
        car = new Sprite((carPosX * 2), (SCREEN_HEIGHT + 8*carPosY - (UNIT_SIZE+6)),carWidth, carHeight, "car", "8,1",0, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX * 8), (SCREEN_HEIGHT + 8*carPosY - (UNIT_SIZE+6)),carWidth, carHeight, "car", "8,2",0, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX * 16), (SCREEN_HEIGHT + 8*carPosY - (UNIT_SIZE+6)),carWidth, carHeight, "car", "8,3",0, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX * 14), (SCREEN_HEIGHT + 8*carPosY - (UNIT_SIZE+6)),carWidth, carHeight, "car", "8,4",0, Color.RED);
        root.getChildren().add(car);

        // lane 9 --> 2
        car = new Sprite((carPosX * 3), (SCREEN_HEIGHT + 9*carPosY - (UNIT_SIZE+6)),carWidth, carHeight, "car", "9,1",1, Color.RED);
        root.getChildren().add(car);
        car = new Sprite((carPosX * 11), (SCREEN_HEIGHT + 9*carPosY - (UNIT_SIZE+6)),carWidth, carHeight, "car", "9,2",1,Color.RED);
        root.getChildren().add(car);
    }


    // will collect all the game sprites that we created and added to the game screen so when we check for specific once
    private List<Sprite> Sprites(){
        return root.getChildren().stream().filter(n -> n instanceof Sprite).map(n -> (Sprite)n).collect(Collectors.toList());
    }

    private void loopAround() throws IOException {
        Sprites().forEach(s ->{
            switch (s.type) {
                case "car":
                    // left
                    if (s.getTranslateX() < - carWidth - 10) {
                        s.setTranslateX(SCREEN_WIDTH + carWidth + 9);
                    }
                    if (s.getTranslateX() > SCREEN_WIDTH + carWidth + 10) {
                        s.setTranslateX(- carWidth - 9);
                    }
                    break;
                case "log":
                    if (s.getTranslateX() < - logLength - 5) {
                        s.setTranslateX(SCREEN_WIDTH + logLength + 4);
                    }
                    if (s.getTranslateX() > SCREEN_WIDTH + logLength + 5) {
                        s.setTranslateX(- logLength - 4);
                    }
                    break;
            }
        });
        gameOver();
    }

    public boolean notOnLil1 = false;
    public boolean notOnLil2 = false;
    public boolean notOnLil3 = false;

    public boolean notOnEndLil1 = false;
    public boolean notOnEndLil2 = false;
    public boolean notOnEndLil3 = false;

    public boolean isLane121 = false;
    public boolean isLane122 = false;
    public boolean isLane123 = false;

    public boolean isLane131 = false;
    public boolean isLane132 = false;
    public boolean isLane133 = false;

    public boolean isLane141 = false;
    public boolean isLane142 = false;
    public boolean isLane143 = false;

    public boolean isLane161 = false;
    public boolean isLane162 = false;

    public boolean isLane171 = false;
    public boolean isLane172 = false;
    public boolean isLane173 = false;

    public boolean isLane181 = false;
    public boolean isLane182 = false;
    public boolean isLane183 = false;

    public boolean isLane191 = false;
    public boolean isLane192 = false;
    // we can tell this what to look out for I filled it with if the player is in the car block it resets the character
    // and make it lose a life.
    private static boolean notWater = false;

    private void collisionLog() {
        Sprites().forEach(s-> {
            switch (s.type) {
                case "log":
                    if (s.dir == 1) {
                        s.moveLeft(leftLogMovement);//TODO: CHANGE VALUE LATER BUT RN ITS FOR TESTING
                    } else if (s.dir == 0){
                        s.moveRight(rightLogMovement);//TODO: CHANGE VALUE LATER BUT RN ITS FOR TESTING
                    }
                    // must be between logs length and depends of direction as going left means + and right means subtract
                    if ((s.getTranslateX() + logLength - UNIT_SIZE - 3) >= (player.getTranslateX() - UNIT_SIZE) && (player.getTranslateX() + UNIT_SIZE - 5) >= (s.getTranslateX()) && player.getTranslateY() == s.getTranslateY() && s.dir == 1) {
                        if (player.getTranslateX() > 0) {
                            player.moveLeft(leftLogMovement);
                            notWater = true;
                        }

                    }

                    if ((s.getTranslateX() + logLength - UNIT_SIZE - 3) >= (player.getTranslateX() - UNIT_SIZE) && (player.getTranslateX() + UNIT_SIZE - 5) >= (s.getTranslateX()) && player.getTranslateY() == s.getTranslateY() && s.dir != 1) {
                        if (player.getTranslateX() < SCREEN_WIDTH - UNIT_SIZE) {
                            player.moveRight(rightLogMovement);
                            notWater = true;
                        }
                    }


                    if (!((s.getTranslateX() + logLength - UNIT_SIZE - 3) >= (player.getTranslateX() - UNIT_SIZE) && (player.getTranslateX() + UNIT_SIZE - 5) > (s.getTranslateX())) && player.getTranslateY() == s.getTranslateY()) {
                        if (s.laneAndNumber.equals("12,1")) {
                            isLane121 = true;
                        } else if (s.laneAndNumber.equals("12,2")) {
                            isLane122 = true;
                        } else if (s.laneAndNumber.equals("12,3")) {
                            isLane123 = true;
                        }
                        if (s.laneAndNumber.equals("13,1")) {
                            isLane131 = true;
                        } else if (s.laneAndNumber.equals("13,2")) {
                            isLane132 = true;
                        } else if (s.laneAndNumber.equals("13,3")) {
                            isLane133 = true;
                        }
                        if (s.laneAndNumber.equals("14,1")) {
                            isLane141 = true;
                        } else if (s.laneAndNumber.equals("14,2")) {
                            isLane142 = true;
                        } else if (s.laneAndNumber.equals("14,3")) {
                            isLane143 = true;
                        }
                        if (s.laneAndNumber.equals("16,1")) {
                            isLane161 = true;
                        } else if (s.laneAndNumber.equals("16,2")) {
                            isLane162 = true;
                        }
                        if (s.laneAndNumber.equals("17,1")) {
                            isLane171 = true;
                        } else if (s.laneAndNumber.equals("17,2")) {
                            isLane172 = true;
                        } else if (s.laneAndNumber.equals("17,3")) {
                            isLane173 = true;
                        }
                        if (s.laneAndNumber.equals("18,1")) {
                            isLane181 = true;
                        } else if (s.laneAndNumber.equals("18,2")) {
                            isLane182 = true;
                        } else if (s.laneAndNumber.equals("18,3")) {
                            isLane183 = true;
                        }
                        if (s.laneAndNumber.equals("19,1")) {
                            isLane191 = true;
                        } else if (s.laneAndNumber.equals("19,2")) {
                            isLane192 = true;
                        }


                    } else {
                        isLane121 = false;
                        isLane122 = false;
                        isLane123 = false;
                        isLane131 = false;
                        isLane132 = false;
                        isLane133 = false;
                        isLane141 = false;
                        isLane142 = false;
                        isLane143 = false;
                        isLane161 = false;
                        isLane162 = false;
                        isLane171 = false;
                        isLane172 = false;
                        isLane173 = false;
                        isLane181 = false;
                        isLane182 = false;
                        isLane183 = false;
                        isLane191 = false;
                        isLane192 = false;
                    }

                    if (isLane121 && isLane122 && isLane123 || isLane131 && isLane132 && isLane133 || isLane141 && isLane142 && isLane143
                            || isLane161 && isLane162 || isLane171 && isLane172 && isLane173 || isLane181 && isLane182 && isLane183 || isLane191 && isLane192) {
                        player.lives--;
                        displayLives();
                        player.setTranslateY((SCREEN_HEIGHT - UNIT_SIZE));
                        player.setTranslateX((UNIT_SIZE * 10));
                    }

                    if (player.getTranslateY() == UNIT_SIZE * 10 && notWater || player.getTranslateY() == UNIT_SIZE * 6 && notWater || player.getTranslateY() == UNIT_SIZE && notWater) {
                        double remainderOfBlock = player.getTranslateX() % UNIT_SIZE;
                        if (remainderOfBlock > (double) (UNIT_SIZE / 2)) {
                            player.setTranslateX(player.getTranslateX() + (UNIT_SIZE - remainderOfBlock));
                        } else if (remainderOfBlock < (double) (UNIT_SIZE / 2)) {
                            player.setTranslateX(player.getTranslateX() - remainderOfBlock);
                        }
                        notWater = false;
                    }
                    if (player.getTranslateY() == UNIT_SIZE) {
                        score += 5;
                        scoreLabel.setText("Score: " + score);
                        leftCarMovement += 0.3;
                        rightCarMovement += 0.5;
                        rightLogMovement += 0.25;
                        leftLogMovement += 0.5;
                        player.setTranslateY((SCREEN_HEIGHT - UNIT_SIZE));
                        player.setTranslateX((UNIT_SIZE * 10));
                    }
                    break;
            }
        });
    }

    private void displayLives(){
        if (player.lives > 1) {
            livesLabel.setText("Lives: " + player.lives);
        } else {
            livesLabel.setText("Lives: " + player.lives);
            livesLabel.setTextFill(Color.RED);
        }
    }

    private void collisionCar() {
        Sprites().forEach(s -> {
            switch (s.type) {
                case "car":
                    if (s.dir == 1) {
                        s.moveLeft(leftCarMovement);
                    } else {
                        s.moveRight(rightCarMovement);
                    }
                    if(s.getTranslateX() == player.getTranslateX() && s.getTranslateY() == player.getTranslateY() || s.getBoundsInParent().intersects(player.getBoundsInParent())) {
                        player.lives --;
                        displayLives();
                        player.setTranslateY((SCREEN_HEIGHT - UNIT_SIZE));
                        player.setTranslateX((UNIT_SIZE*10));
                    }
                    break;

                case "lil":
                    if ( player.getTranslateY() == s.getTranslateY() && !(player.getTranslateX() + UNIT_SIZE <= s.getTranslateX() + lilLength && player.getTranslateX() >= s.getTranslateX())){
                        notOnLil1 = true;
                    } else {
                        notOnLil1 = false;
                    }
                    if (notOnLil1 && notOnLil2 && notOnLil3) {
                        player.lives --;
                        displayLives();
                        player.setTranslateY((SCREEN_HEIGHT - UNIT_SIZE));
                        player.setTranslateX((UNIT_SIZE*10));
                    }
                    break;
                case "lil1":
                    if ( player.getTranslateY() == s.getTranslateY() && !(player.getTranslateX() + UNIT_SIZE <= s.getTranslateX() + lilLength && player.getTranslateX() >= s.getTranslateX())) {
                        notOnLil2 = true;
                    } else {
                        notOnLil2 = false;
                    }

                    if (notOnLil1 && notOnLil2 && notOnLil3) {
                        player.lives --;
                        displayLives();
                        player.setTranslateY((SCREEN_HEIGHT - UNIT_SIZE));
                        player.setTranslateX((UNIT_SIZE*10));
                    }
                    break;
                case "lil2":
                    if ( player.getTranslateY() == s.getTranslateY() && !(player.getTranslateX() + UNIT_SIZE <= s.getTranslateX() + lilLength && player.getTranslateX() >= s.getTranslateX()) ){
                        notOnLil3 = true;
                    } else {
                        notOnLil3 = false;
                    }
                    if (notOnLil1 && notOnLil2 && notOnLil3) {
                        player.lives --;
                        displayLives();
                        player.setTranslateY((SCREEN_HEIGHT - UNIT_SIZE));
                        player.setTranslateX((UNIT_SIZE*10));
                    }
                    break;
                case "endLil1":
                    if (player.getTranslateY() == s.getTranslateY() && !(player.getTranslateX() +UNIT_SIZE <= s.getTranslateX() + 2*UNIT_SIZE && player.getTranslateX() >= s.getTranslateX())) {
                        notOnEndLil1 = true;
                    } else {
                        notOnEndLil1 = false;
                    }
                    break;
                case "endLil2":
                    if ( player.getTranslateY() == s.getTranslateY() && !(player.getTranslateX() + UNIT_SIZE <= s.getTranslateX() + lilLength && player.getTranslateX() >= s.getTranslateX()) ){
                        notOnEndLil2 = true;
                    } else {
                        notOnEndLil2 = false;
                    }
                    if (notOnEndLil1 && notOnEndLil2 && notOnEndLil3) {
                        player.lives --;
                        displayLives();
                        player.setTranslateY((SCREEN_HEIGHT - UNIT_SIZE));
                        player.setTranslateX((UNIT_SIZE*10));
                    }
                    break;
                case "endLil3":
                    if (player.getTranslateY() == s.getTranslateY() && !(player.getTranslateX() + UNIT_SIZE <= s.getTranslateX() + 2*UNIT_SIZE && player.getTranslateX() >= s.getTranslateX())) {
                        notOnEndLil3 = true;
                    } else {
                        notOnEndLil3 = false;
                    }
                    break;
            }

        });
    }

    public String playerName = "";

    // will create the action listener type and where we start the game
    @Override
    public void start(Stage stage) throws Exception {
        stage.setResizable(false);

        // Create the start screen
        Scene startScene = createStartScreen(stage);
        // Set the start screen as the initial scene
        stage.setScene(startScene);
        stage.show();
    }

    private Scene createStartScreen(Stage stage) {
        Pane startRoot = new Pane();
        startRoot.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        ImageView bgImg;
        try {
            bgImg = new ImageView((new Image(Objects.requireNonNull(getClass().getResource("/sprites/startingScreen.png")).toExternalForm())));
        } catch (NullPointerException e) {
            System.out.println("Image file not found! Ensure it's in the correct directory. Catch file");
            return null;
        }
        bgImg.setFitWidth(SCREEN_WIDTH);
        bgImg.setFitHeight(SCREEN_HEIGHT);
        startRoot.getChildren().add(bgImg);


        // Create the "start game" button
        Button startButton = new Button("Start Game");
        startButton.setLayoutX(SCREEN_WIDTH / 2 - 50);
        startButton.setLayoutY(SCREEN_HEIGHT / 2 - 30);

        // Create "high score" button
        Button highscrButton = new Button("High Scores");
        highscrButton.setLayoutX(SCREEN_WIDTH / 2 - 50);
        highscrButton.setLayoutY(SCREEN_HEIGHT / 2 + 30);

        // added the text field title
        Text nameTitle = new Text("Player Name:");
        nameTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        nameTitle.setX(SCREEN_WIDTH / 2 - 80);
        nameTitle.setY(SCREEN_HEIGHT / 3.1);
        startRoot.getChildren().add(nameTitle);
        // added the text label
        TextField textField = new TextField();
        textField.setLayoutX(SCREEN_WIDTH/2 - 80);
        textField.setLayoutY(SCREEN_HEIGHT / 3);
        startRoot.getChildren().add(textField);

        highscrButton.setOnAction(e-> {
            Scene highScoresScene = createHighScoresScene(stage);
            stage.setScene(highScoresScene);
        });

        // Changes the screen to the game scene
        startButton.setOnAction(e -> {
            // changes the screen to the game scene
            playerName = textField.getText().strip();
            playerName = playerName.replace(' ', '_');
            Scene gameScene = new Scene(createContent());

            stage.setScene(gameScene);

            gameScene.setOnKeyPressed(event -> {
                // moves the frog with WSDA and arrow keys
                if (player.lives >= 0) {
                    switch (event.getCode()) {
                        case W:
                        case UP:
                            player.moveUp();
                            break;
                        case A:
                        case LEFT:
                            player.moveLeft();
                            break;
                        case S:
                        case DOWN:
                            player.moveDown();
                            break;
                        case D:
                        case RIGHT:
                            player.moveRight();
                            break;
                        default:
                            break;


                    }
                }
            });
        });

        startRoot.getChildren().addAll(startButton, highscrButton);

        return new Scene(startRoot);
    }

    private Scene createHighScoresScene(Stage stage) {
        Pane highScoresRoot = new Pane();
        highScoresRoot.setPrefSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        ImageView hsImg;
        try {
            // image got from https://www.istockphoto.com/videos/cute-backgrounds
            hsImg = new ImageView((new Image(Objects.requireNonNull(getClass().getResource("/sprites/highscore.jpg")).toExternalForm())));
        } catch (NullPointerException e) {
            System.out.println("Image file not found! Ensure it's in the correct directory. Catch file");
            return null;
        }

        hsImg.setFitWidth(SCREEN_WIDTH);
        hsImg.setFitHeight(SCREEN_HEIGHT);
        highScoresRoot.getChildren().add(hsImg);

        // High scores title
        Text highScoresTitle = new Text("High Scores:");
        highScoresTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        highScoresTitle.setX(SCREEN_WIDTH / 2 - 50);
        highScoresTitle.setY(40);
        highScoresRoot.getChildren().add(highScoresTitle);

        // Display high scores
        File highScoresFile = new File("score/highScores.txt");
        if (highScoresFile.exists()) {
            // used https://www.javatpoint.com/java-bufferedreader-class - to figure out buffer reader
            // Attempt to open the file for reading line by line
            try (BufferedReader reader = new BufferedReader(new FileReader(highScoresFile))) {
                String line;
                int yOffset = 70;
                // Reads one line from the file at a time until there are no more lines to read
                while ((line = reader.readLine()) != null) {
                    // node containing the current line read from the file
                    Text scoreText = new Text(line);
                    scoreText.setX(SCREEN_WIDTH / 2 - 50);
                    scoreText.setY(yOffset);
                    // Making text visible on the screen
                    highScoresRoot.getChildren().add(scoreText);
                    yOffset += 30; // Move each score down by 30 pixels
                }
                // Handle any IOE exceptions
            } catch (IOException ex) {
                Text errorText = new Text("Error reading high scores.");
                errorText.setX(SCREEN_WIDTH / 2 - 75);
                errorText.setY(70);
                highScoresRoot.getChildren().add(errorText);
            }
        }
        // Bring the user back to main menu with a back button
        Button backButton = new Button("Back to Main Menu");
        backButton.setLayoutX(SCREEN_WIDTH / 2 - 50);
        backButton.setLayoutY(SCREEN_HEIGHT - 100);
        // Displays the start screen when the button is pressed
        backButton.setOnAction(e -> {
            Scene startScene = createStartScreen(stage);
            stage.setScene(startScene);
        });
        highScoresRoot.getChildren().add(backButton);


        return new Scene(highScoresRoot);

    }


    // Creates the sprites and the methods used by the sprites
    private static class Sprite extends Rectangle {
        int lives = 3;
        final String type;
        final String laneAndNumber;
        final int dir;

        Sprite(int x, int y, int w, int h, String type, String laneAndNumber, int dir, Color color) {
            super(w, h, color);
            this.type = type;
            this.laneAndNumber = laneAndNumber;
            this.dir = dir;
            setTranslateX(x);
            setTranslateY(y);
        }

        void moveLeft() {
            if (getTranslateX() - UNIT_SIZE >= 0) {
                setTranslateX(getTranslateX() - UNIT_SIZE);

            }
            if (getTranslateX() - UNIT_SIZE < -UNIT_SIZE ) {
                setTranslateX(0);
            }

        }

        void moveLeft(double b) {
            setTranslateX(getTranslateX() - b);

        }

        void moveRight() {
            if (getTranslateX() + UNIT_SIZE < SCREEN_WIDTH) {
                setTranslateX(getTranslateX() + UNIT_SIZE);
            }
        }

        void moveRight(double b) {
            setTranslateX(getTranslateX() + b);

        }

        void moveUp() {
            if (getTranslateY() - UNIT_SIZE >= UNIT_SIZE) {
                setTranslateY(getTranslateY() - UNIT_SIZE);
            }
        }

        void moveDown(){
            if (getTranslateY() + UNIT_SIZE < SCREEN_HEIGHT) {
                setTranslateY(getTranslateY() + UNIT_SIZE);
            }
        }
    }


    // running the game
    public static void main(String[] args) {
        launch(args);
    }
}
