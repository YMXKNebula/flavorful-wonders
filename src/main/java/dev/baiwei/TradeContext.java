package dev.baiwei;
public final class TradeContext {
    private static final ThreadLocal<java.util.ArrayDeque<Boolean>> FRAMES=ThreadLocal.withInitial(java.util.ArrayDeque::new);
    public static void begin(boolean free){FRAMES.get().push(free);}
    public static boolean free(){var f=FRAMES.get();return !f.isEmpty()&&f.peek();}
    public static void end(){var f=FRAMES.get();f.pop();if(f.isEmpty())FRAMES.remove();}
}
