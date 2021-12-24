public class Symbol {
    public String type;
    public int location;

    Symbol() {
        type = null;
        location = 0;
    }

    Symbol(String t, int l) {
        type = t;
        location = l;
    }
}
