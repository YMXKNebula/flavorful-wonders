package dev.baiwei;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class Effects {
    public static final String[] IDS={"", "durability_guard","hunger_compensation","vitality","metabolism","silver_tongue","death_resistance","good_touch","water_stride","nether_sovereign","end_sovereign","insight","bloodthirst","hover","immovable","fast_eating","mighty","double_meal","great_stomach","fear","breeding_aura","forest_child"};
    private static final java.util.Map<Integer,Holder.Reference<MobEffect>> ENTRIES=new java.util.HashMap<>();
    public static void register(){
        for(int n=1;n<IDS.length;n++)ENTRIES.put(n,Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,Identifier.fromNamespaceAndPath(BaiweiMod.ID,IDS[n]),new PassiveEffect(n)));
    }
    public static Identifier id(String id){
        if(id.startsWith("original:"))return Identifier.fromNamespaceAndPath(BaiweiMod.ID,IDS[Integer.parseInt(id.substring(9))]);
        return Identifier.parse(id);
    }
    public static Holder<MobEffect> get(int n){return ENTRIES.get(n);}
    public static boolean has(LivingEntity e,int n){var h=get(n);return h!=null&&e.hasEffect(h);}
    public static int level(LivingEntity e,int n){var h=get(n);var m=h==null?null:e.getEffect(h);return m==null?0:m.getAmplifier()+1;}
    private static final class PassiveEffect extends MobEffect {
        private final int number;
        PassiveEffect(int n){super(MobEffectCategory.BENEFICIAL,color(n));number=n;}
        @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){return number>=19;}
        @Override public boolean applyEffectTick(ServerLevel level,LivingEntity entity,int amplifier){RuntimeEffects.auraTick(level,entity,number);return true;}
        private static int color(int n){int[] c={0xD3B671,0xD26F55,0xD78D77,0xF4C367,0x82B465,0xD6BD68,0xD69A4A};return c[(n-1)%c.length];}
    }
}
