package gomoku;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Random;
import java.util.Map;

/** Gomoku GameState Object
 * @author TakLee96 */
public class State {
    /***********************
     *** CLASS CONSTANTS ***
     ***********************/
    // size of board
    public static final int N = 15;
    // default starting action
    public static final Action start = new Action(N/2, N/2);
    // neighbor positions on the board
    private static final Action[] neighbors = new Action[]{
        new Action(1, 0), new Action(-1, 0), new Action(0, 1), new Action(0, -1),
        new Action(1, 1), new Action(-1, 1), new Action(1, -1), new Action(-1, -1),
        new Action(2, 0), new Action(-2, 0), new Action(0, 2),  new Action(0, -2),
        new Action(2, 2),  new Action(-2, 2), new Action(2, -2),  new Action(-2, -2)
    };

    /***************************
     *** INSTANCE ATTRIBUTES ***
     ***************************/
    // the direction of the five generated
    private int dx, dy;
    // helper structure to store the winning moves
    public LinkedList<Action> five;
    // game history
    public LinkedList<Action> history;
    // data structure for easy generation of legal actions
    private HashSet<Action> legalActions;
    // did any one win the game?
    private boolean wins;
    // the board
    private Grid[][] board;
    // the random
    private Random random;
    // the features
    private Counter features;

    /*******************
     *** CONSTRUCTOR ***
     *******************/
    public State() {
        dx = 0; dy = 0;
        five = new LinkedList<Action>();
        history = new LinkedList<Action>();
        legalActions = new HashSet<Action>();
        legalActions.add(start);
        wins = false;
        board = new Grid[N][N];
        random = new Random();
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++)
                board[i][j] = new Grid();
        features = new Counter();
    }
    public State clone() {
        State s = new State();
        for (Action a : history)
            s.move(a);
        return s;
    }

    /********************
     *** CORE UTILITY ***
     ********************/
    public int numMoves() { return history.size(); }
    public boolean started() { return numMoves() > 0; }
    public boolean isBlacksTurn() { return numMoves() % 2 == 0; }
    public boolean isTurn(boolean isBlack) { return !(isBlack ^ isBlacksTurn()); }
    public boolean canMove(Action a) { return a != null && canMove(a.x(), a.y()); }
    public boolean canMove(int x, int y) { return !ended() && inBound(x, y) && board[x][y].isEmpty(); }
    public Grid get(Action a) { return get(a.x(), a.y()); }
    public Grid get(int x, int y) { return board[x][y]; }
    public boolean ended() { return wins || numMoves() == N * N; }
    public boolean inBound(Action a) { return inBound(a.x(), a.y()); }
    public boolean inBound(int x, int y) { return (x >= 0 && x < N && y >= 0 && y < N); }
    public boolean win(boolean isBlack) { return wins && isTurn(!isBlack); }
    public Action randomAction() { return getLegalActions()[random.nextInt(legalActions.size())]; }
    public Counter extractFeatures() { return features; }

    public Action[] getLegalActions() {
        if (legalActions.size() == 0) {
            System.out.println("#ended says: " + ended());
            throw new RuntimeException("no action available");
        }
        return legalActions.toArray(new Action[legalActions.size()]);
    }

    public Rewinder move(Action a) { return move(a.x(), a.y()); }
    public Rewinder move(int x, int y) {
        if (ended())
            throw new RuntimeException("game has already ended");
        Counter diffFeatures = null;
        try {
            diffFeatures = Extractor.diffFeatures(this, x, y);
            features.add(diffFeatures);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(this + "@ (" + x + ", " + y + ", " + isBlacksTurn() + ")");
            System.out.println(diffFeatures);
            System.out.println(history);
            throw new RuntimeException("stop here");
        }
        boolean who = isBlacksTurn();
        board[x][y].put(who);
        Action move = new Action(x, y);

        LinkedList<Action> removedLegalActions = new LinkedList<Action>();
        int nx = 0, ny = 0; Action a = null;
        for (Action d : neighbors) {
            nx = x + d.x();
            ny = y + d.y();
            if (inBound(nx, ny) && board[nx][ny].isEmpty()) {
                a = new Action(nx, ny);
                if (legalActions.add(a))
                    removedLegalActions.add(a);
            }
        }
        legalActions.remove(move);
        history.addLast(move);

        wins = check(who);
        if (wins) {
            nx = x; ny = y;
            nx += dx; ny += dy;
            while (inBound(nx, ny) && board[nx][ny].is(who)) {
                five.add(new Action(nx, ny));
                nx += dx; ny += dy;
            }
            nx = x - dx; ny = y - dy;
            while (inBound(nx, ny) && board[nx][ny].is(who)) {
                five.add(new Action(nx, ny));
                nx -= dx; ny -= dy;
            }
            five.add(new Action(x, y));
        }
        return new Rewinder(removedLegalActions, diffFeatures);
    }

    private int count(boolean isBlack, int x, int y, int dx, int dy) {
        int count = 0;
        x += dx; y += dy;
        while (inBound(x, y) && board[x][y].is(isBlack)) {
            count += 1;
            x += dx; y += dy;
        }
        return count;
    }

    private boolean check(boolean isBlack) {
        if (!started() || isTurn(isBlack)) {
            return false;
        }
        Action a = history.getLast();
        int newX = a.x(); int newY = a.y();
        if (1 + count(isBlack, newX, newY, (int) 1, (int) 0)
              + count(isBlack, newX, newY, (int)-1, (int) 0) == 5) {
            dx = 1; dy = 0;
            return true;
        }
        if (1 + count(isBlack, newX, newY, (int) 0, (int) 1)
              + count(isBlack, newX, newY, (int) 0, (int)-1) == 5) {
            dx = 0; dy = 1;
            return true;
        }
        if (1 + count(isBlack, newX, newY, (int) 1, (int) 1)
              + count(isBlack, newX, newY, (int)-1, (int)-1) == 5) {
            dx = 1; dy = 1;
            return true;
        }
        if (1 + count(isBlack, newX, newY, (int) 1, (int)-1)
              + count(isBlack, newX, newY, (int)-1, (int) 1) == 5) {
            dx = 1; dy = -1;
            return true;
        }
        return false;
    }

    public Action rewind(Rewinder rewinder) {
        if (!started())
            throw new RuntimeException("rewind at the beginning");
        Action last = history.pollLast();
        board[last.x()][last.y()].clean();
        for (Action a : rewinder.removedLegalActions)
            if (!legalActions.remove(a))
                throw new RuntimeException("illegal rewinder");
        legalActions.add(last);
        if (wins) {
            wins = false;
            five.clear();
        }
        features.sub(rewinder.diffFeatures);
        return last;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("- ");
        for (int k = 0; k < N; k++) {
            if (k < 10) {
                sb.append(k);
            } else {
                sb.append((char) (k - 10 + 'A'));
            }
            sb.append(" ");
        }
        sb.append("Y\n");
        for (int i = 0; i < N; i++) {
            if (i < 10) {
                sb.append(i);
            } else {
                sb.append((char) (i - 10 + 'A'));
            }
            sb.append(" ");
            for (int j = 0; j < N; j++) {
                if (board[i][j].isEmpty()) {
                    sb.append("+ ");
                } else if (board[i][j].isBlack()) {
                    sb.append("o ");
                } else {
                    sb.append("x ");
                }
            }
            sb.append("|\n");
        }
        sb.append("X ");
        for (int l = 0; l <= N; l++) {
            sb.append("- ");
        }
        sb.append("\n");
        return sb.toString();
    }

}
