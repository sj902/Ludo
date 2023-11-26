package ludo;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * @author shubhamjain
 */
public class Ludo extends JComponent implements Runnable {

    //Signifies turn
    //0-Red 1-Green 2-Blue 3-Yellow
    public static Color turn;

    //Which color to be used as bot
    public static int bot = -1;

    //if any piece has been captured
    public static boolean bonus;

    //Mode- BvsG-1 or RvsY-0
    public static int mode;

    //Number returned by dice
    public static int roll;

    //Winner of the game
    public static String winner;

    private static final int XOFFSET = 0;
    private static final int YOFFSET = 0;

    // current board
    private static int[][] myboard; //0-Red 1-Green 2-Blue 3-Yellow

    //Priority of a token increases as it reaches near goal
    //Help decide which token to save if needed
    private static int[][] myboardPriority;//0-Red 1-Green 2-Blue 3-Yellow
    //state of the game
    private int state; //0-roll dice 1-choose token

    // colors
    private Color[] colors; //0-Red 1-Green 2-Blue 3-Yellow

    public int timeLimit = 120;
    public static boolean drawBoard = false;
    public boolean initFlag = false;
    public static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    public static int commuicationState = -1;

    //Images to be used
    private BufferedImage redImage;
    private BufferedImage greenImage;
    private BufferedImage yellowImage;
    private BufferedImage blueImage;
    private BufferedImage redEndImage;
    private BufferedImage greenEndImage;
    private BufferedImage yellowEndImage;
    private BufferedImage blueEndImage;
    private BufferedImage redCellImage;
    private BufferedImage greenCellImage;
    private BufferedImage yellowCellImage;
    private BufferedImage blueCellImage;
    private BufferedImage starImage;
    private BufferedImage midImage;

    //Map to store next cells
    static HashMap<Integer, Integer> nextMove;

    //Safe places
    static HashSet<Integer> safePlaces;

    //Map to store Colors
    static HashMap<Color, Integer> colorMap;

    public Ludo() throws IOException {
        init();
    }

    private void play() throws IOException {
        //bot-R0,G1,B2,Y3
        if ((bot == 0 || bot == 2) && initFlag == false) {
            commuicationState = 1;
        } else {
            commuicationState = 3;
        }
        //commuicationState=1 send throw to server
        //commuicationState=2 receive dice from server
        //commuicationState=3 receive dice rolled by opponent   
        try {
            while (true) {
                if (commuicationState == 1) {
                    System.out.println("<THROW>");
                    commuicationState = 2;
                } else if (commuicationState == 2) {
                    String sMove = "";
                    String g = br.readLine();
                    String sRoll[] = g.trim().split(" ");
                    int rolls = sRoll.length - 2;

                    if (g.equals("YOU ROLLED 3 SIXES, AND THUS A DUCK")) {
                        sMove = "NA";
                    } else {
                        boolean firstTime = true;
                        for (int i = 2; i < rolls + 2; i++) {
                            roll = Integer.parseInt(sRoll[i]);
                            if (!isValid(turn, roll, false, -1) && roll != 6) {
                                if (sMove.equals("")) {
                                    sMove = "NA";
                                }
                                break;
                            } else if (!isValid(turn, roll, false, -1) && roll == 6) {
                                continue;
                            }

                            int tokenToMove = getMove();

                            //System.out.println(tokenToMove+"hii");
                            //System.out.println(tokenToMove);
                            int n = nextCell(myboard[bot][tokenToMove], turn, roll, true, tokenToMove);

                            if (firstTime) {
                                sMove = sMove + "" + (getColor(bot) + "" + tokenToMove + "_" + roll);
                                firstTime = false;
                            } else {
                                if (inHome(tokenToMove, colorMap.get(turn))) {
                                    sMove = "" + (getColor(bot) + "" + tokenToMove + "_" + roll) + "<next>" + "" + sMove;                                    
                                } else {
                                    sMove = sMove + "<next>" + (getColor(bot) + "" + tokenToMove + "_" + roll);
                                }
                            }
                            myboardPriority[bot][tokenToMove] += (inHome(tokenToMove, bot) ? 1 : roll);
                            myboard[bot][tokenToMove] = n;
                            //checkEnd(i);
                            //state = 0;
                            repaint();
                        }
                    }
                    //System.out.println(sMove);
                    System.out.println(sMove.trim());

                    commuicationState = 3;
                    updateTurn(turn);
                } else if (commuicationState == 3) {
                    String firstL = br.readLine();
                    // String sRolls[] = firstL.trim().split(" ");
                    // int len = sRolls.length - 3;
                    boolean repeat = false;

                    //   System.err.println(firstL+"debug");                
                    //  System.err.println(RorNot+"debug");
                    if (firstL.equals("REPEAT")) {
                        commuicationState = 1;
                        updateTurn(turn);
                    } else {
                        String RorNot = br.readLine();

                        String oMoves[] = RorNot.trim().split("<next>");
                        int len = oMoves.length;
                        if (oMoves[oMoves.length - 1].equals("REPEAT")) {
                            repeat = true;
                            len--;
                        }
                        if (oMoves.length == 1 && oMoves[0].equals("NA")) {

                        } else {
                            for (int i = 3; i < len + 3; i++) {
                                String sMove = oMoves[i - 3];
                                if (sMove.equals("REPEAT")) {

                                    break;
                                }
                                String sMoveProperties[] = sMove.split("_");
                                roll = Integer.parseInt(sMoveProperties[1].charAt(0) + "");
                                int tokenToMove = Integer.parseInt(sMoveProperties[0].charAt(1) + "");
                                int n = nextCell(myboard[getOpponent(bot)][tokenToMove], turn, roll, true, tokenToMove);
                                if (n < 0) {
                                    break;
                                }
                                myboardPriority[getOpponent(bot)][tokenToMove] += (inHome(tokenToMove, getOpponent(bot)) ? 1 : roll);
                                myboard[getOpponent(bot)][tokenToMove] = n;
                                //checkEnd(i);
                                // state = 0;
                                repaint();
                            }
                        }
                        if (repeat) {

                            commuicationState = 3;
                        } else {
                            commuicationState = 1;
                            updateTurn(turn);
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    private void init() throws IOException {
        String parameters[] = br.readLine().trim().split(" ");
        timeLimit = Integer.parseInt(parameters[1]);
        bot = Integer.parseInt(parameters[0]) - 1;
        mode = Integer.parseInt(parameters[2]);
        drawBoard = Boolean.parseBoolean(parameters[3]);

        /* do {
            try {
                mode = Integer.parseInt(JOptionPane.showInputDialog("Please input 0 for RvsY and 1 for BvsG (default is RvsY):"));
            } catch (Exception e) {
                System.out.println("" + e);
            }
        } while (mode != 0 && mode != 1);
        do {
            try {
                bot = Integer.parseInt(JOptionPane.showInputDialog("Enter 0 for RorB and 1 for YorG"));
            } catch (Exception e) {
                System.out.println("" + e);
            }
        } while (bot != 0 && bot != 1);*/
        if (mode == 0) {
            turn = Color.red;
        } else {
            turn = Color.blue;
        }

        state = 0;

        //Initialize safe placces on the board
        Integer[] SET_VALUES = new Integer[]{23, 91, 201, 133, 22, 37, 52, 67, 82, 97, 106, 107, 108, 109, 110, 111, 127, 142, 157, 172, 187, 202, 118, 117, 116, 115, 114, 113, 102, 122, 188, 36};
        safePlaces = new HashSet<Integer>(Arrays.asList(SET_VALUES));

        String path = "/home/mininet/NetBeansProjects/Ludo_AI/resources/";
        try {
            blueImage = ImageIO.read(new File(path + "blueImage.png"));
            redImage = ImageIO.read(new File(path + "redImage.png"));
            yellowImage = ImageIO.read(new File(path + "yellowImage.png"));
            greenImage = ImageIO.read(new File(path + "greenImage.png"));
            redEndImage = ImageIO.read(new File(path + "redEnd.png"));
            greenEndImage = ImageIO.read(new File(path + "greenEnd.png"));
            yellowEndImage = ImageIO.read(new File(path + "yellowEnd.png"));
            blueEndImage = ImageIO.read(new File(path + "blueEnd.png"));
            redCellImage = ImageIO.read(new File(path + "redCell.png"));
            greenCellImage = ImageIO.read(new File(path + "greenCell.png"));
            yellowCellImage = ImageIO.read(new File(path + "yellowCell.png"));
            blueCellImage = ImageIO.read(new File(path + "blueCell.png"));
            starImage = ImageIO.read(new File(path + "star.png"));
            midImage = ImageIO.read(new File(path + "midImage.png"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        myboard = new int[4][4];
        myboardPriority = new int[4][4];
        myboard[0][0] = 16;
        myboard[0][1] = 19;
        myboard[0][2] = 61;
        myboard[0][3] = 64;
        myboard[2][0] = 151;
        myboard[2][1] = 154;
        myboard[2][2] = 196;
        myboard[2][3] = 199;
        myboard[3][0] = 160;
        myboard[3][1] = 205;
        myboard[3][2] = 208;
        myboard[3][3] = 163;
        myboard[1][0] = 25;
        myboard[1][1] = 70;
        myboard[1][2] = 28;
        myboard[1][3] = 73;

        colors = new Color[4];
        colors[0] = Color.RED;
        colors[1] = Color.GREEN;
        colors[2] = Color.BLUE;
        colors[3] = Color.YELLOW;

        initializeMap();

        colorMap = new HashMap<Color, Integer>();
        colorMap.put(Color.red, 0);
        colorMap.put(Color.blue, 2);
        colorMap.put(Color.green, 1);
        colorMap.put(Color.yellow, 3);

        if (mode == 0) {
            if (bot == 0) {
                bot = colorMap.get(Color.red);
            } else {
                bot = colorMap.get(Color.yellow);
            }
        } else {
            if (bot == 0) {
                bot = colorMap.get(Color.blue);
            } else {
                bot = colorMap.get(Color.green);
            }
        }

    }

    static String getColor(int b) {
        String color = "";
        if (b == 0) {
            color = "R";
        } else if (b == 1) {
            color = "G";
        } else if (b == 2) {
            color = "B";
        } else if (b == 3) {
            color = "Y";
        }
        return color;
    }

    int getBot(String s) {
        int b = -1;
        if (s.equals("R")) {
            b = 0;
        } else if (s.equals("G")) {
            b = 1;
        } else if (s.equals("B")) {
            b = 2;
        } else if (s.equals("Y")) {
            b = 3;
        }
        return b;
    }

    private void initializeMap() {
        nextMove = new HashMap<Integer, Integer>();
        nextMove.put(6, 7);
        nextMove.put(7, 8);
        nextMove.put(8, 23);
        nextMove.put(21, 6);
        nextMove.put(22, 37);
        nextMove.put(23, 38);
        nextMove.put(36, 21);
        nextMove.put(37, 52);
        nextMove.put(38, 53);
        nextMove.put(51, 36);
        nextMove.put(52, 67);
        nextMove.put(53, 68);
        nextMove.put(66, 51);
        nextMove.put(67, 82);
        nextMove.put(68, 83);
        nextMove.put(81, 66);
        nextMove.put(82, 97);
        nextMove.put(83, 99);
        nextMove.put(90, 91);
        nextMove.put(91, 92);
        nextMove.put(92, 93);
        nextMove.put(93, 94);
        nextMove.put(94, 95);
        nextMove.put(95, 81);
        nextMove.put(96, -250);
        nextMove.put(96, -250);
        nextMove.put(97, -250);
        nextMove.put(97, -250);
        nextMove.put(98, -250);
        nextMove.put(98, -250);
        nextMove.put(99, 100);
        nextMove.put(100, 101);
        nextMove.put(101, 102);
        nextMove.put(102, 103);
        nextMove.put(103, 104);
        nextMove.put(104, 119);
        nextMove.put(105, 90);
        nextMove.put(106, 107);
        nextMove.put(107, 108);
        nextMove.put(108, 109);
        nextMove.put(109, 110);
        nextMove.put(110, 111);
        nextMove.put(111, -250);
        nextMove.put(111, -250);
        nextMove.put(112, -250);
        nextMove.put(112, -250);
        nextMove.put(113, -250);
        nextMove.put(113, -250);
        nextMove.put(114, 113);
        nextMove.put(115, 114);
        nextMove.put(116, 115);
        nextMove.put(117, 116);
        nextMove.put(118, 117);
        nextMove.put(119, 134);
        nextMove.put(120, 105);
        nextMove.put(121, 120);
        nextMove.put(122, 121);
        nextMove.put(123, 122);
        nextMove.put(124, 123);
        nextMove.put(125, 124);
        nextMove.put(126, -250);
        nextMove.put(126, -250);
        nextMove.put(127, -250);
        nextMove.put(127, -250);
        nextMove.put(128, -250);
        nextMove.put(128, -250);
        nextMove.put(129, 143);
        nextMove.put(130, 129);
        nextMove.put(131, 130);
        nextMove.put(132, 131);
        nextMove.put(133, 132);
        nextMove.put(134, 133);
        nextMove.put(141, 125);
        nextMove.put(142, 127);
        nextMove.put(143, 158);
        nextMove.put(156, 141);
        nextMove.put(157, 142);
        nextMove.put(158, 173);
        nextMove.put(171, 156);
        nextMove.put(172, 157);
        nextMove.put(173, 188);
        nextMove.put(186, 171);
        nextMove.put(187, 172);
        nextMove.put(188, 203);
        nextMove.put(201, 186);
        nextMove.put(202, 187);
        nextMove.put(203, 218);
        nextMove.put(216, 201);
        nextMove.put(217, 216);
        nextMove.put(218, 217);

    }

    @Override
    public void run() {
        repaint();
    }

    public void drawTokens(Graphics2D g) {
        for (int i = 0; i < 4; i++) {
            g.setColor(colors[i]);
            if (colors[i] == Color.yellow) {
                g.setColor(Color.yellow.darker());
            }
            for (int j = 0; j < 4; j++) {
                //g.fillRect((myboard[i][j] / 15) * 40 + 15, (myboard[i][j] % 15) * 40 + 15, 10, 10);
                //System.out.println("i:"+i+"j:"+j+" Color:"+colors[i]+"myboard[i][j]"+myboard[i][j]);

                g.drawRect((myboard[i][j] % 15) * 40 + 15, (myboard[i][j] / 15) * 40 + 5, 10, 10);
                g.drawRect((myboard[i][j] % 15) * 40 + 10, (myboard[i][j] / 15) * 40 + 20, 20, 10);
            }
        }
    }

    public static void placeAtHome(int i, int j) {
        myboardPriority[i][j] = 0;

        if (i == 0 && j == 0) {
            myboard[0][0] = 16;
            return;
        }
        if (i == 0 && j == 1) {
            myboard[0][1] = 19;
            return;
        }
        if (i == 0 && j == 2) {
            myboard[0][2] = 61;
            return;
        }
        if (i == 0 && j == 3) {
            myboard[0][3] = 64;
            return;
        }
        if (i == 1 && j == 0) {
            myboard[1][0] = 25;
            return;
        }
        if (i == 1 && j == 1) {
            myboard[1][1] = 70;
            return;
        }
        if (i == 1 && j == 2) {
            myboard[1][2] = 28;
            return;
        }
        if (i == 1 && j == 3) {
            myboard[1][3] = 73;
            return;
        }
        if (i == 2 && j == 0) {
            myboard[2][0] = 151;
            return;
        }
        if (i == 2 && j == 1) {
            myboard[2][1] = 154;
            return;
        }
        if (i == 2 && j == 2) {
            myboard[2][2] = 196;
            return;
        }
        if (i == 2 && j == 3) {
            myboard[2][3] = 199;
            return;
        }
        if (i == 3 && j == 0) {
            myboard[3][0] = 160;
            return;
        }
        if (i == 3 && j == 1) {
            myboard[3][1] = 205;
            return;
        }
        if (i == 3 && j == 2) {
            myboard[3][2] = 208;
            return;
        }
        if (i == 3 && j == 3) {
            myboard[3][3] = 163;
            return;
        }

    }

    private static boolean isblocked(int currentCell, Color c, int token) {

        if (safePlaces.contains(currentCell)) {
            return false;
        }

        if (currentCell == 91 || currentCell == 23 || currentCell == 133 || currentCell == 201) {
            return false;
        }
        for (int i = 0; i < 4; ++i) {
            if (i != token) {
                if (myboard[colorMap.get(c)][i] == currentCell) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int nextCell(int currentCell, Color c, int count, boolean state, int token) {

        if (isblocked(currentCell, c, token) && count == 0) {
            return -250;
        }
        if (count == 0) {
            if (state) {
                for (int i = 0; i < 4; ++i) {
                    if (i != colorMap.get(c)) {
                        for (int j = 0; j < 4; ++j) {
                            if (myboard[i][j] == currentCell && !safePlaces.contains(currentCell)) {
                                bonus = true;
                                placeAtHome(i, j);
                            }
                        }
                    }
                }

                if (c == Color.GREEN && currentCell == 97 || c == Color.RED && currentCell == 111 || c == Color.BLUE && currentCell == 127 || c == Color.YELLOW && currentCell == 113) {
                    bonus = true;
                }

            }
            return currentCell;
        } else if (c == Color.red && (currentCell == 16 || currentCell == 19 || currentCell == 61 || currentCell == 64) && (count == 6 || count == 1)) {
            return 91;
        } else if (c == Color.green && (currentCell == 25 || currentCell == 70 || currentCell == 28 || currentCell == 73) && (count == 6 || count == 1)) {
            return 23;
        } else if (c == Color.yellow && (currentCell == 160 || currentCell == 163 || currentCell == 208 || currentCell == 205) && (count == 6 || count == 1)) {
            return 133;
        } else if (c == Color.blue && (currentCell == 151 || currentCell == 154 || currentCell == 196 || currentCell == 199) && (count == 6 || count == 1)) {
            return 201;
        } else {
            try {
                if (c == Color.red && currentCell == 105) {
                    return nextCell(106, c, count - 1, state, token);
                } else if (c == Color.blue && currentCell == 217) {
                    return nextCell(202, c, count - 1, state, token);
                } else if (c == Color.green && currentCell == 7) {
                    return nextCell(22, c, count - 1, state, token);
                } else if (c == Color.yellow && currentCell == 119) {
                    return nextCell(118, c, count - 1, state, token);
                } else {
                    return nextCell(nextMove.get(currentCell), c, count - 1, state, token);
                }
            } catch (Exception e) {
                return -250;
            }
        }
    }

    private boolean isValid(Color c, int roll, boolean state, int token) {
        int k = colorMap.get(c);
        for (int i = 0; i < 4; ++i) {
            //System.out.println("nextCell(myboard[k][i],c, roll)"+nextCell(myboard[k][i],c, roll));
            if (token == -1) {
                if (nextCell(myboard[k][i], c, roll, state, i) > 0) {
                    return true;
                }
            } else {
                if (i == token && nextCell(myboard[k][i], c, roll, state, i) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkEnd(int i) {
        switch (i) {
            case 0:
                if (myboard[i][0] == 111 && myboard[i][1] == 111 && myboard[i][2] == 111 && myboard[i][3] == 111) {
                    winner = "Red ";
                }
                break;
            case 1:
                if (myboard[i][0] == 97 && myboard[i][1] == 97 && myboard[i][2] == 97 && myboard[i][3] == 97) {
                    winner = "Green ";
                }
                break;
            case 2:
                if (myboard[i][0] == 113 && myboard[i][1] == 113 && myboard[i][2] == 113 && myboard[i][3] == 113) {
                    winner = "Yellow ";
                }
                break;
            case 3:
                if (myboard[i][0] == 127 && myboard[i][1] == 127 && myboard[i][2] == 127 && myboard[i][3] == 127) {
                    winner = "Blue ";
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void paint(Graphics u) {
        Graphics2D g = (Graphics2D) u;

        // fill background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1024, 1024);

        // translate origin
        g.translate(XOFFSET, YOFFSET);

        // draw current state
        g.setColor(Color.BLACK);
        String s = "";

        g.drawImage(redImage, 0, 0, 240, 240, this);
        g.drawImage(yellowImage, 360, 360, 240, 240, this);
        g.drawImage(blueImage, 0, 360, 240, 240, this);
        g.drawImage(greenImage, 360, 0, 240, 240, this);
        g.drawImage(redEndImage, 40, 280, 200, 40, this);
        g.drawImage(greenEndImage, 280, 40, 40, 200, this);
        g.drawImage(yellowEndImage, 360, 280, 200, 40, this);
        g.drawImage(blueEndImage, 280, 360, 40, 200, this);
        g.drawImage(redCellImage, 40, 240, 40, 40, this);
        g.drawImage(greenCellImage, 320, 40, 40, 40, this);
        g.drawImage(yellowCellImage, 520, 320, 40, 40, this);
        g.drawImage(blueCellImage, 240, 520, 40, 40, this);
        g.drawImage(starImage, 240, 80, 40, 40, this);
        g.drawImage(starImage, 80, 320, 40, 40, this);
        g.drawImage(starImage, 480, 240, 40, 40, this);
        g.drawImage(starImage, 320, 480, 40, 40, this);
        g.drawImage(midImage, 240, 240, 120, 120, this);

        // draw board
        g.drawRect(0, 0, 600, 600);

        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                //Print grid numbers                 
                //g.drawString("" + (i + j * 15), i * 40 + 10, j * 40 + 10);
                g.drawRect(i * 40, j * 40, 40, 40);
                g.setColor(Color.black);
            }
        }

        g.drawImage(midImage, 240, 240, 120, 120, this);

        // draw board numbering
        g.setColor(Color.BLACK);

        // draw roll button
        {
            int wx = 600 + 32, wy = 32;
            g.translate(wx, wy);
            if (turn == Color.red) {
                g.setColor(new Color(253, 121, 151));
            } else if (turn == Color.green) {
                g.setColor(new Color(198, 223, 181));
            } else if (turn == Color.yellow) {
                g.setColor(new Color(255, 241, 206));
            } else if (turn == Color.blue) {
                g.setColor(new Color(100, 172, 240));
            }
            g.fillRect(-24, -24, 64, 64);
            g.setColor(Color.black);
            switch (roll) {
                case 1:
                    g.fillRect(5, 5, 6, 6);
                    break;
                case 2:
                    g.fillRect(-14, -14, 6, 6);
                    g.fillRect(24, 24, 6, 6);
                    break;
                case 3:
                    g.fillRect(5, 5, 6, 6);
                    g.fillRect(-14, -14, 6, 6);
                    g.fillRect(24, 24, 6, 6);
                    break;
                case 4:
                    g.fillRect(24, -14, 6, 6);
                    g.fillRect(-14, 24, 6, 6);
                    g.fillRect(24, 24, 6, 6);
                    g.fillRect(-14, -14, 6, 6);
                    break;
                case 5:
                    g.fillRect(24, -14, 6, 6);
                    g.fillRect(-14, 24, 6, 6);
                    g.fillRect(5, 5, 6, 6);
                    g.fillRect(-14, -14, 6, 6);
                    g.fillRect(24, 24, 6, 6);
                    break;
                case 6:
                    g.fillRect(24, -14, 6, 6);
                    g.fillRect(-14, 24, 6, 6);
                    g.fillRect(24, 24, 6, 6);
                    g.fillRect(-14, -14, 6, 6);
                    g.fillRect(-14, 5, 6, 6);
                    g.fillRect(24, 5, 6, 6);
                    break;
                default:
                    g.fillRect(5, 5, 6, 6);

            }

            int t = colorMap.get(turn);
            if (t == 0) {
                g.drawString("Turn: Red", -18, 64);
            }
            if (t == 2) {
                g.drawString("Turn: Blue", -18, 64);
            }
            if (t == 1) {
                g.drawString("Turn: Green", -18, 64);
            }
            if (t == 3) {
                g.drawString("Turn: Yellow", -18, 64);
            }

            g.drawString("Roll:" + roll, -18, 80);

            if (winner != null && !winner.isEmpty()) {
                g.drawString(winner + "wins.", -18, 100);
            }

            g.translate(-wx, -wy);
        }

        drawTokens(g);

        repaint();

    }

//Get respective Home square based on Color
    static int getHome(int c) {
        switch (c) {
            case 0:
                return 91;
            case 1:
                return 23;
            case 3:
                return 133;
            case 2:
                return 201;
            default:
                break;
        }
        return -1;
    }

    //Checks if the token is in home or not.Yet not started
    static boolean inHome(int token, int player) {
        int turnToken = player;
        if (myboard[turnToken][token] == 73 || myboard[turnToken][token] == 70 || myboard[turnToken][token] == 28 || myboard[turnToken][token] == 25 || myboard[turnToken][token] == 199 || myboard[turnToken][token] == 196 || myboard[turnToken][token] == 154 || myboard[turnToken][token] == 151 || myboard[turnToken][token] == 163 || myboard[turnToken][token] == 160 || myboard[turnToken][token] == 208 || myboard[turnToken][token] == 205 || myboard[turnToken][token] == 16 || myboard[turnToken][token] == 19 || myboard[turnToken][token] == 61 || myboard[turnToken][token] == 64) {
            return true;
        }
        return false;
    }

//Points earned by capturing opponents token on a particular position
    static int points(int position, int opponent) {
        int points = 0;
        if (!safePlaces.contains(position)) {
            int countCurrentPosition = 0;
            int opponentToken = -1;
            for (int j = 0; j < 4; j++) {
                if (myboard[opponent][j] == position) {
                    countCurrentPosition++;
                    opponentToken = j;
                }
            }
            if (countCurrentPosition == 1) {
                points = (myboardPriority[opponent][opponentToken]);
            }
        }
        return points;
    }
//Heuristic which returns a number showing how much is our token in danger at a particular position

    static int inDanger(int position, int opponent, int moves) {
        int hit = 0;
        int countTokenAtPosition = 0;
        if (safePlaces.contains(position)) {
            return 0;
        }
        for (int i = 0; i < 4; i++) {
            if (myboard[colorMap.get(turn)][i] == position) {
                countTokenAtPosition++;
            }
        }
        if (countTokenAtPosition > 1) {
            return 0;
        }
        for (int i = 0; i < 4; i++) {
            if (inHome(i, opponent)) {
                continue;
            }
            int opponentPossiblePosition = myboard[opponent][i];
            int possibleRoll = moves;
            while (possibleRoll > 0 && opponentPossiblePosition != -250) {
                opponentPossiblePosition = nextMove.get(opponentPossiblePosition);
                if (opponentPossiblePosition == position && myboardPriority[opponent][i] + 6 - possibleRoll + 1 < 52) {
                    hit++;
                }
                possibleRoll--;
            }
        }
        return hit;
    }

    static int getOpponent(int turnToken) {
        switch (turnToken) {
            case 0:
                return 3;
            case 3:
                return 0;
            case 1:
                return 2;
            default:
                return 1;
        }
    }
//Decides Next move

    protected int getMove() {

        int token = -1;
        int turnToken = colorMap.get(turn);
        int opponent = getOpponent(turnToken);

        ArrayList<Integer> canMoveToken = new ArrayList<>();
        //Finds token that can move in the current turn
        for (int i = 0; i < 4; i++) {
            int lRoll = roll;
            int currentCell = myboard[turnToken][i];
            if (!inHome(i, turnToken)) {
                while (lRoll > 0 && currentCell != -250) {
                    currentCell = nextMove.get(currentCell);
                    lRoll--;
                }
            }
            if (isValid(turn, roll, false, i) && currentCell != -250 && ((!inHome(i, turnToken)) ? (!isblocked(currentCell, turn, i)) : true)) {
                canMoveToken.add(i);
            }
        }

        int possibleMove = canMoveToken.size();
        Random rand = new Random();
        try {
            if (possibleMove == 1) {
                return canMoveToken.get(0);
            } else {
                //Potential to kill opponent tokens in next turn 
                double[] capturePotential = new double[possibleMove];
                //Number of opponent token in token's range in next turn
                double[] capturePotentialHits = new double[possibleMove];
                //Points earned by killing opponent token in current roll
                double[] capture = new double[possibleMove];
                //Hueristic to measure how much our token is in danger
                double[] danger = new double[possibleMove];
                //Hueristic to measure how much our token is in danger after the roll
                double[] dangerAfterRoll = new double[possibleMove];
                //If we can capture multiple of opponent's token in same turn it will choose that give's us more advantage
                double maxCapture = 0;
                int maxCaptureToken = -1;
                double maxCapturePotential = 0;
                int maxCapturePotentialToken = -1;
                double maxDanger = 0;
                int maxDangerToken = -1;
                //Calculate Capture and CapturePotential
                for (int i = 0; i < possibleMove; i++) {
                    if (inHome(canMoveToken.get(i), turnToken)) {
                        continue;
                    }
                    int local_roll = roll;
                    int position = nextMove.get(myboard[turnToken][canMoveToken.get(i)]);
                    local_roll--;
                    while (local_roll > 0 && position != -250) {
                        position = nextMove.get(position);
                        local_roll--;
                    }
                    if (myboardPriority[turnToken][canMoveToken.get(i)] + roll > 51) {
                        position = -250;
                    }
                    capture[i] = position != -250 ? points(position, opponent) : 0;
                    if (capture[i] > maxCapture) {
                        maxCapture = capture[i];
                        maxCaptureToken = i;
                    }
                    if (position != -250) {
                        int localRoll = 12 - roll;
                        int temp1 = myboard[turnToken][canMoveToken.get(i)];
                        int temp2 = myboardPriority[turnToken][canMoveToken.get(i)];
                        myboard[turnToken][canMoveToken.get(i)] = position;
                        myboardPriority[turnToken][canMoveToken.get(i)] += roll;
                        int temp = position;
                        while (localRoll > 0 && position != -250 && !isblocked(temp, turn, canMoveToken.get(i))) {
                            position = nextMove.get(position);
                            double points = points(position, opponent) / 6.0;
                            capturePotential[i] += points;
                            if (points > 0) {
                                capturePotentialHits[i]++;
                            }
                            localRoll--;
                        }
                        myboard[turnToken][canMoveToken.get(i)] = temp1;
                        myboardPriority[turnToken][canMoveToken.get(i)] = temp2;
                    }
                    if (capturePotential[i] > maxCapturePotential) {
                        maxCapturePotential = capturePotential[i];
                        maxCapturePotentialToken = i;
                    }
                }
                //Calulate Danger and DangerAfterRoll
                for (int i = 0; i < possibleMove; i++) {
                    if (inHome(canMoveToken.get(i), turnToken)) {
                        continue;
                    }
                    int tokenPosition = myboard[turnToken][canMoveToken.get(i)];
                    int hits = inDanger(tokenPosition, opponent, (myboardPriority[turnToken][canMoveToken.get(i)] > 44) ? 9 : 6);
                    danger[i] = (hits) * (myboardPriority[turnToken][canMoveToken.get(i)]) / 6.0;
                    int localRoll = roll;
                    while (localRoll > 0 && tokenPosition != -250) {
                        tokenPosition = nextMove.get(tokenPosition);
                        localRoll--;
                    }
                    if (tokenPosition != -250) {
                        int temp1 = myboard[turnToken][canMoveToken.get(i)];
                        int temp2 = myboardPriority[turnToken][canMoveToken.get(i)];
                        myboard[turnToken][canMoveToken.get(i)] = tokenPosition;
                        myboardPriority[turnToken][canMoveToken.get(i)] += roll;
                        hits = inDanger(tokenPosition, opponent, 6);
                        dangerAfterRoll[i] = (hits) * (myboardPriority[turnToken][canMoveToken.get(i)]) / 6.0;
                        myboard[turnToken][canMoveToken.get(i)] = temp1;
                        myboardPriority[turnToken][canMoveToken.get(i)] = temp2;
                    }
                    if (danger[i] > maxDanger && dangerAfterRoll[i] < danger[i]) {
                        maxDanger = danger[i];
                        maxDangerToken = i;
                    }
                }
                //System.out.println();
                /* for (int i = 0; i < possibleMove; i++) {
                System.out.print(capture[i] + " " + capturePotential[i] + " " + danger[i] + " " + dangerAfterRoll[i] + " " + myboard[turnToken][canMoveToken.get(i)] + " " + myboardPriority[turnToken][canMoveToken.get(i)]);
                System.out.println();
            }*/
                //To decide Which token to move based on hueristics
                if (maxCaptureToken != -1 && maxDangerToken == -1) {
                    token = canMoveToken.get(maxCaptureToken);
                    /* System.out.println(1 + " " + myboard[turnToken][token]);*/
                } else if (maxCaptureToken == -1 && maxDangerToken != -1) {
                    if (maxDanger < 1) {
                        for (int i = 0; i < possibleMove; i++) {
                            if (capturePotentialHits[i] > 1) {
                                token = canMoveToken.get(i);
                                /* System.out.println(2 + " " + myboard[turnToken][token]);*/
                            }
                        }
                        //If roll is 6 choose a token which is in home
                        if (roll == 6 && token == -1) {
                            for (int i = 0; i < possibleMove; i++) {
                                if (inHome(canMoveToken.get(i), turnToken)) {
                                    token = canMoveToken.get(i);
                                    /*System.out.println(3 + " " + myboard[turnToken][token]);*/
                                    break;
                                }
                            }
                        }
                    }
                    if (token == -1) {
                        token = canMoveToken.get(maxDangerToken);
                        /*System.out.println(4 + " " + myboard[turnToken][token]);*/
                    }
                } else if (maxCaptureToken == -1 && maxDangerToken == -1) {
                    for (int i = 0; i < possibleMove; i++) {
                        if (capturePotentialHits[i] > 1) {
                            token = canMoveToken.get(i);
                            /* System.out.println(5 + " " + myboard[turnToken][token]);*/
                        }
                    }
                    if (roll == 6 && token == -1) {
                        for (int i = 0; i < possibleMove; i++) {
                            if (inHome(canMoveToken.get(i), turnToken)) {
                                token = canMoveToken.get(i);
                                /*  System.out.println(6 + " " + myboard[turnToken][token]);*/
                                break;
                            }
                        }
                    }

                    if (token == -1) {
                        int findSafeMove = -1;
                        double maximumDanger = -1;
                        for (int j = 0; j < possibleMove; j++) {
                            if (dangerAfterRoll[j] <= danger[j]) {
                                if (dangerAfterRoll[j] >= maximumDanger && findSafeMove != -1 && myboardPriority[turnToken][canMoveToken.get(j)] > myboardPriority[turnToken][canMoveToken.get(findSafeMove)]) {
                                    if (myboardPriority[turnToken][canMoveToken.get(j)] < 52) {
                                        findSafeMove = j;
                                        maximumDanger = dangerAfterRoll[j];
                                    }
                                } else if (findSafeMove == -1) {
                                    findSafeMove = j;
                                    maximumDanger = dangerAfterRoll[j];
                                }
                            } else if (dangerAfterRoll[j] - danger[j] < 1) {
                                if (danger[j] != 0 && dangerAfterRoll[j] > maximumDanger) {
                                    findSafeMove = j;
                                    maximumDanger = dangerAfterRoll[j];
                                }
                            }
                        }
                        if (findSafeMove != -1) {
                            token = canMoveToken.get(findSafeMove);
                            /* System.out.println(7 + " " + myboard[turnToken][token]);*/
                        }
                        if (maxCapturePotentialToken != -1 && findSafeMove != -1 && dangerAfterRoll[maxCapturePotentialToken] <= danger[maxCapturePotentialToken]) {
                            token = canMoveToken.get(maxCapturePotentialToken);
                            /* System.out.println(8 + " " + myboard[turnToken][token]);*/
                        }
                        if (token == -1) {
                            double min = dangerAfterRoll[0];
                            int minIndex = 0;
                            for (int j = 1; j < possibleMove; j++) {
                                if (min > dangerAfterRoll[j]) {
                                    min = dangerAfterRoll[j];
                                    minIndex = j;
                                }
                            }
                            token = canMoveToken.get(minIndex);
                        }
                    }

                } else {
                    if (maxDanger > maxCapture) {
                        token = canMoveToken.get(maxDangerToken);
                        /* System.out.println(9 + " " + myboard[turnToken][token]);*/
                    } else {
                        int findSafeMove = -1;
                        double maximumDanger = -1;
                        if (dangerAfterRoll[maxCaptureToken] > danger[maxCaptureToken]) {
                            for (int j = 0; j < possibleMove; j++) {
                                if (dangerAfterRoll[j] <= danger[j]) {
                                    if (dangerAfterRoll[j] > maximumDanger && findSafeMove != -1 && myboardPriority[turnToken][canMoveToken.get(j)] > myboardPriority[turnToken][canMoveToken.get(findSafeMove)]) {
                                        findSafeMove = j;
                                    } else if (findSafeMove == -1) {
                                        findSafeMove = j;
                                    }
                                } else if (dangerAfterRoll[j] - danger[j] < 1) {
                                    if (dangerAfterRoll[j] > maximumDanger) {
                                        findSafeMove = j;
                                        maximumDanger = dangerAfterRoll[j];
                                    }
                                }
                            }
                        }
                        if (findSafeMove == -1 || capture[maxCaptureToken] != 0) {
                            token = canMoveToken.get(maxCaptureToken);
                            /* System.out.println(10 + " " + myboard[turnToken][token]);*/
                        } else {
                            token = canMoveToken.get(findSafeMove);
                            /*System.out.println(11 + " " + myboard[turnToken][token]);*/
                        }
                    }
                }
                //Prioritise the token which is very near to home column 
                if (maxCaptureToken == -1) {
                    for (int i = 0; i < possibleMove; i++) {
                        if ((roll + myboardPriority[turnToken][canMoveToken.get(i)]) > 51 && myboardPriority[turnToken][canMoveToken.get(i)] < 52) {
                            if (danger[i] != 0) {
                                token = canMoveToken.get(i);
                                /*  System.out.println(12 + " " + myboard[turnToken][token]);*/
                            }
                            break;
                        }
                    }
                }
            }
            return (token == -1 ? (canMoveToken.get(rand.nextInt(possibleMove))) : (token));
        } catch (Exception e) {
            return (canMoveToken.get(rand.nextInt(possibleMove)));
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {

        if (e.getID() != MouseEvent.MOUSE_PRESSED) {
            return;
        }

        // calculate clicked field position
        int xclick = (e.getX() - XOFFSET) / 40;
        int yclick = (e.getY() - YOFFSET) / 40;

        //System.out.println("xclick" + xclick);
        //System.out.println("yclick" + yclick);
        if (state == 0) {
            if (xclick > 14 && yclick < 2) {
                Random rand = new Random();
                roll = rand.nextInt(6) + 1;
                if (isValid(turn, roll, false, -1)) {
                    state = 1;
                    repaint();
                    int i = colorMap.get(turn);
                    if (i == bot) {
                        int tokenToMove = getMove();
                        //System.out.println(tokenToMove);
                        int n = nextCell(myboard[i][tokenToMove], turn, roll, true, tokenToMove);
                        if (n < 0) {
                            return;
                        }
                        myboardPriority[i][tokenToMove] += (inHome(tokenToMove, i) ? 1 : roll);
                        myboard[i][tokenToMove] = n;
                        checkEnd(i);
                        state = 0;
                        updateTurn(turn);
                        repaint();
                    }
                    return;
                } else {
                    updateTurn(turn);
                    repaint();
                    return;
                }
            } else {
                return;
            }

        } else if (state == 1) {
            int i = colorMap.get(turn);

            //System.out.println("myboard[i][0]" + myboard[i][0]);
            //System.out.println("xclick + (yclick * 15)" + (xclick + (yclick * 15)));
            if (myboard[i][0] == xclick + (yclick * 15)) {
                int n = nextCell(myboard[i][0], turn, roll, true, 0);
                if (n < 0) {
                    return;
                }
                myboardPriority[i][0] += (inHome(0, i) ? 1 : roll);
                myboard[i][0] = n;
                checkEnd(i);
                state = 0;
                updateTurn(turn);
                repaint();
            } else if (myboard[i][1] == xclick + (yclick * 15)) {
                int n = nextCell(myboard[i][1], turn, roll, true, 1);
                if (n < 0) {
                    return;
                }
                myboardPriority[i][1] += (inHome(1, i) ? 1 : roll);
                myboard[i][1] = n;
                checkEnd(i);
                state = 0;
                updateTurn(turn);
                repaint();
            } else if (myboard[i][2] == xclick + (yclick * 15)) {
                int n = nextCell(myboard[i][2], turn, roll, true, 2);
                if (n < 0) {
                    return;
                }
                myboardPriority[i][2] += (inHome(2, i) ? 1 : roll);
                myboard[i][2] = n;

                checkEnd(i);
                state = 0;
                updateTurn(turn);
                repaint();
            } else if (myboard[i][3] == xclick + (yclick * 15)) {
                int n = nextCell(myboard[i][3], turn, roll, true, 3);
                if (n < 0) {
                    return;
                }
                myboardPriority[i][3] += (inHome(3, i) ? 1 : roll);
                myboard[i][3] = n;
                checkEnd(i);
                state = 0;
                updateTurn(turn);
                repaint();
            } else {
                return;
            }

        }

        //repaint();
    }

    private void updateTurn(Color c) {
        //if (roll == 6 || bonus) {
        /* if (roll == 6 || bonus) {
            bonus = false;
            return;
        }*/
        if (mode == 0) {
            if (turn == Color.RED) {
                turn = Color.yellow;
            } else if (turn == Color.yellow) {
                turn = Color.red;
            }
        } else {
            if (turn == Color.blue) {
                turn = Color.green;
            } else if (turn == Color.green) {
                turn = Color.blue;
            }
        }
    }
    public static JFrame frame;
    public static Ludo ch;

    public static void main(String[] args) throws Exception {

        frame = new JFrame("Ludo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ch = new Ludo();
        ch.setPreferredSize(new Dimension(XOFFSET + 675, YOFFSET + 600));
        //ch.enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        frame.add(ch);
        frame.pack();
        frame.setVisible(drawBoard);
        ch.play();

        //System.out.println("" + nextCell(217, Color.yellow, 1));
    }

}
