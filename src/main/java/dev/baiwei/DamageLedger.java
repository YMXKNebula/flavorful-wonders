package dev.baiwei;
import net.minecraft.world.entity.LivingEntity;
/** One frame per native hurt call; child damage calls cannot spend or heal the parent twice. */
public final class DamageLedger {
    public static final class Frame {
        final LivingEntity target;
        float paid;
        Frame(LivingEntity target){this.target=target;}
        public float total(){return paid;}
    }
    private static final ThreadLocal<java.util.ArrayDeque<Frame>> STACK=ThreadLocal.withInitial(java.util.ArrayDeque::new);
    public static Frame begin(LivingEntity target){var frame=new Frame(target);STACK.get().push(frame);return frame;}
    public static void record(LivingEntity target,float amount){var s=STACK.get();if(!s.isEmpty()&&s.peek().target==target&&amount>0)s.peek().paid+=amount;}
    public static void end(){var s=STACK.get();s.pop();if(s.isEmpty())STACK.remove();}
}
