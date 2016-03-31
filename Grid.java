public class Grid {
    private static enum Who {
        EMPTY, BLACK, WHITE
    }
    
    private Who w;
    public Grid() {
        w = Who.EMPTY;
    }

    public boolean isBlack() {
        return w == Who.BLACK;
    }

    public boolean isWhite() {
        return w == Who.WHITE;
    }

    public boolean is(boolean isBlack) {
        if (isBlack) {
            return isBlack();
        }
        return isWhite();
    }

    public boolean isEmpty() {
        return w == Who.EMPTY;
    }

    public void put(boolean isBlack) {
        w = (isBlack) ? Who.BLACK : Who.WHITE;
    }

    public String getImage() {
        switch (w) {
            case BLACK: return "public/img/grid-blue.jpg";
            case WHITE: return "public/img/grid-green.jpg";
            default: return "public/img/grid-empty.jpg";
        }
    }
}