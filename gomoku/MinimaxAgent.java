package gomoku;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/** Advanced MinimaxAgent
 * @author TakLee96 */
public class MinimaxAgent extends Agent {

    private static final double infinity = 1E15;
    private static final double gamma = 0.99;
    private static final int maxDepth = 12;
    private static final int maxBigDepth = 4;
    private static final int branch = 15;

    private class Node {
        public Action a;
        public double v;
        public Node(Action a, double v) {
            this.a = a;
            this.v = v;
        }
        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            Node o = (Node) other;
            return a.equals(o.a);
        }
        @Override
        public int hashCode() {
            return a.hashCode();
        }
        @Override
        public String toString() {
            return "Node(" + a.toString() + ", " + v + ")";
        }
    }

    private static Comparator<Node> blackComparator = new Comparator<Node>(){
        public int compare(Node a, Node b) { return (int) (b.v - a.v); }
    };
    private static Comparator<Node> whiteComparator = new Comparator<Node>(){
        public int compare(Node a, Node b) { return (int) (a.v - b.v); }
    };

    private Counter blackWeights;
    private Counter whiteWeights;
    public MinimaxAgent(boolean isBlack) {
        super(isBlack);
        blackWeights = new Counter();
        whiteWeights = new Counter();
        Counter.read(blackWeights, whiteWeights, "gomoku/myweight.csv");
    }

    private double value(State s) {
        if (s.isBlacksTurn()) return blackWeights.mul(s.extractFeatures());
        return whiteWeights.mul(s.extractFeatures());
    }

    private Node maxvalue(State s, double alpha, double beta, int depth, int bigDepth, Set<Action> actions) {
        Action maxaction = null; double maxvalue = -infinity, val = 0;
        Rewinder rewinder = null;
        for (Action a : actions) {
            rewinder = s.move(a);
            val = gamma * value(s, alpha, beta, depth, bigDepth).v;
            s.rewind(rewinder);
            if (val > maxvalue) {
                maxvalue = val;
                maxaction = a;
            }
            if (val > beta) {
                return new Node(maxaction, maxvalue);
            }
            alpha = Math.max(alpha, val);
        }
        if (maxaction == null) throw new RuntimeException("everybody is too small");
        return new Node(maxaction, maxvalue);
    }

    private Node minvalue(State s, double alpha, double beta, int depth, int bigDepth, Set<Action> actions) {
        Action minaction = null; double minvalue = infinity, val = 0;
        Rewinder rewinder = null;
        for (Action a : actions) {
            rewinder = s.move(a);
            val = gamma * value(s, alpha, beta, depth, bigDepth).v;
            s.rewind(rewinder);
            if (val < minvalue) {
                minvalue = val;
                minaction = a;
            }
            if (val < alpha) {
                return new Node(minaction, minvalue);
            }
            beta = Math.min(beta, val);
        }
        if (minaction == null) throw new RuntimeException("everybody is too large");
        return new Node(minaction, minvalue);
    }

    private Node value(State s, double alpha, double beta, int depth, int bigDepth) {
        if (s.win(true))
            return new Node(null, infinity);
        if (s.win(false))
            return new Node(null, -infinity);
        if (s.ended())
            return new Node(null, 0.0);
        if (depth == maxDepth || bigDepth == maxBigDepth)
            return new Node(null, value(s));
        depth += 1;
        Set<Action> actions = getActions(s);
        if (actions.size() == 0)
            return new Node(s.randomAction(), (s.isBlacksTurn()) ? -infinity : infinity);
        if (actions.size() > 3)
            bigDepth += 1;
        if (s.isBlacksTurn())
            return maxvalue(s, alpha, beta, depth, bigDepth, actions);
        return minvalue(s, alpha, beta, depth, bigDepth, actions);
    }

    private int four(Counter features, boolean isBlack) {
        if (isBlack)
            return (features.getInt("-oooox") + features.getInt("four-o"));
        return (features.getInt("-xxxxo") + features.getInt("four-x"));
    }
    private int straightFour(Counter features, boolean isBlack) {
        if (isBlack)
            return features.getInt("-oooo-");
        return features.getInt("-xxxx-");
    }
    private int three(Counter features, boolean isBlack) {
        if (isBlack)
            return (features.getInt("-o-oo-") +
                    features.getInt("-oo-o-") +
                    features.getInt("-ooo-"));
        return (features.getInt("-x-xx-") +
                features.getInt("-xx-x-") +
                features.getInt("-xxx-"));
    }

    private Set<Action> movesExtendFour(State s, Counter features) {
        Set<Action> result = new HashSet<Action>(1, 2);
        String win = (s.isBlacksTurn()) ? "win-o" : "win-x";
        for (Action a : s.getLegalActions()) {
            if (Extractor.diffFeatures(s, a).getInt(win) > 0) {
                result.add(a);
                return result;
            }
            // Rewinder rewinder = s.move(a);
            // if (s.win(w))
            //     result.add(a);
            // s.rewind(rewinder);
            // if (!result.isEmpty()) return result;
        }
        throw new RuntimeException("my four is missing?");
    }
    private Set<Action> movesCounterFour(State s, Counter features) {
        Set<Action> result = new HashSet<Action>(1, 2);
        boolean w = s.isBlacksTurn();
        for (Action a : s.getLegalActions()) {
            if (-four(Extractor.diffFeatures(s, a), !w) >= four(features, !w)) {
                result.add(a);
                return result;
            }
            // Rewinder rewinder = s.move(a);
            // if (!containsFour(!w, s.extractFeatures(), !w))
            //     result.add(a);
            // s.rewind(rewinder);
            // if (!result.isEmpty()) return result;
        }
        return result;
    }
    private Set<Action> movesExtendThree(State s, Counter features) {
        Set<Action> result = new HashSet<Action>(1, 2);
        boolean w = s.isBlacksTurn();
        for (Action a : s.getLegalActions()) {
            if (straightFour(Extractor.diffFeatures(s, a), w) > 0) {
                result.add(a);
                return result;
            }
            // Rewinder rewinder = s.move(a);
            // if (containsStraightFour(!w, s.extractFeatures(), w))
            //     result.add(a);
            // s.rewind(rewinder);
            // if (!result.isEmpty()) return result;
        }
        if (three(features, !w) > 0)
            return movesCounterThree(s, features);
        return movesBestGrowth(s, features);
    }
    private Set<Action> movesCounterThree(State s, Counter features) {
        Set<Action> result = new HashSet<Action>(3, 2);
        boolean w = s.isBlacksTurn();
        for (Action a : s.getLegalActions()) {
            if (-three(Extractor.diffFeatures(s, a), !w) >= three(features, !w))
                result.add(a);
            // Rewinder rewinder = s.move(a);
            // if (!containsThree(!w, s.extractFeatures(), !w))
            //     result.add(a);
            // s.rewind(rewinder);
        }
        return result;
    }
    private Set<Action> movesBestGrowth(State s, Counter features) {
        Set<Action> result = new HashSet<Action>();
        boolean w = s.isBlacksTurn();
        Action[] actions = s.getLegalActions();
        Node[] nodes = new Node[actions.length];
        for (int i = 0; i < actions.length; i++)
            nodes[i] = new Node(actions[i], heuristic(s, actions[i]));
        Arrays.sort(nodes, (w) ? blackComparator : whiteComparator);
        for (int i = 0; i < branch && i < nodes.length; i++)
            result.add(nodes[i].a);
        return result;
    }

    private int heuristic(State s, Action a) {
        Counter diff = Extractor.diffFeatures(s, a);
        int score = 0;
        for (String key : diff.keySet())
            score += Extractor.sign(key) * diff.getInt(key);
        return score;
    }

    private Set<Action> getActions(State s) {
        Counter f = s.extractFeatures();
        boolean w = s.isBlacksTurn();
        if (four(f, w) > 0 || straightFour(f, w) > 0)
            return movesExtendFour(s, f);
        if (straightFour(f, !w) > 0)
            return new HashSet<Action>(1);
        if (four(f, !w) > 0)
            return movesCounterFour(s, f);
        if (three(f, w) > 0)
            return movesExtendThree(s, f);
        if (three(f, !w) > 0)
            return movesCounterThree(s, f);
        return movesBestGrowth(s, f);
    }

    @Override
    public Action getAction(State s) {
        if (!s.isTurn(isBlack))
            throw new RuntimeException("not my turn");
        if (!s.started())
            return s.start;
        return value(s, -infinity, infinity, 0, 0).a;
    }

}