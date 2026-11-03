package dev.baiwei.test;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Observes actual native particle dispatch during this test only. Never included in the mod JAR. */
public final class HeartParticleCapture {
    private static List<Vec3> active;
    public static void begin(){active=new ArrayList<>();}
    public static void record(double x,double y,double z){if(active!=null)active.add(new Vec3(x,y,z));}
    public static List<Vec3> end(){var result=List.copyOf(active);active=null;return result;}
}
