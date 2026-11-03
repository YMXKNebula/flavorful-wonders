package dev.baiwei;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class FoodRules {
    private static final Map<String,JsonObject> RULES=new HashMap<>();
    public record AddedEffect(String id,int level,int ticks,float probability){}
    private static final Map<String,List<AddedEffect>> ADDED_PREVIEWS=new HashMap<>();
    private static final Map<String,Set<String>> NATIVE_PREVIEWS=new HashMap<>();
    private record Context(LivingEntity eater,String food){}
    private static final ThreadLocal<Context> CONTEXT=new ThreadLocal<>();
    public static int size(){return RULES.size();}
    public static void load(){
        try(var in=FoodRules.class.getResourceAsStream("/data/baiweiqixiao/food_effects.json")){
            if(in==null)throw new IOException("Missing approved food mappings");
            install(JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject());
            var override=FabricLoader.getInstance().getConfigDir().resolve("baiweiqixiao/food_overrides.json");
            if(Files.exists(override))try(var reader=Files.newBufferedReader(override)){install(JsonParser.parseReader(reader).getAsJsonObject());}
        }catch(IOException|RuntimeException e){throw new IllegalStateException("Invalid Baiwei food configuration",e);}
    }
    private static void install(JsonObject root){
        for(var entry:root.getAsJsonObject("foods").entrySet()){
            String ns=entry.getKey().split(":",2)[0];
            if(!FabricLoader.getInstance().isModLoaded(ns))continue;
            RULES.put(entry.getKey(),entry.getValue().getAsJsonObject());
            var rule=entry.getValue().getAsJsonObject();var added=new ArrayList<AddedEffect>();
            for(var value:rule.getAsJsonArray("added")){
                var r=value.getAsJsonObject();
                if(Effects.id(r.get("effect").getAsString()).getPath().equals("durability_guard")&&Effects.id(r.get("effect").getAsString()).getNamespace().equals(BaiweiMod.ID))r.addProperty("level",1);
                added.add(new AddedEffect(r.get("effect").getAsString(),r.get("level").getAsInt(),r.get("ticks").getAsInt(),r.get("probability").getAsFloat()));
            }
            ADDED_PREVIEWS.put(entry.getKey(),List.copyOf(added));
            var nativeRules=rule.getAsJsonObject("native");NATIVE_PREVIEWS.put(entry.getKey(),nativeRules==null?Set.of():Set.copyOf(nativeRules.keySet()));
        }
    }
    public static boolean hasRule(String food){return RULES.containsKey(food);}
    public static List<AddedEffect> addedEffects(String food){return ADDED_PREVIEWS.getOrDefault(food,List.of());}
    public static Set<String> overriddenNativeEffects(String food){return NATIVE_PREVIEWS.getOrDefault(food,Set.of());}
    public static Object begin(LivingEntity eater,ItemStack stack){Object old=CONTEXT.get();CONTEXT.set(new Context(eater,BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));return old;}
    public static void restore(Object old){if(old==null)CONTEXT.remove();else CONTEXT.set((Context)old);}
    public static String currentFood(){var c=CONTEXT.get();return c==null?null:c.food();}
    public static MobEffectInstance nativeEffect(LivingEntity eater,MobEffectInstance incoming){
        var c=CONTEXT.get();if(c==null||c.eater()!=eater)return incoming;
        return adjustNativeEffect(c.food(),incoming);
    }
    public static MobEffectInstance adjustNativeEffect(String food,MobEffectInstance incoming){
        var f=RULES.get(food);if(f==null)return incoming;
        String id=BuiltInRegistries.MOB_EFFECT.getKey(incoming.getEffect().value()).toString();
        var n=f.getAsJsonObject("native");if(n==null||!n.has(id))return incoming;
        var r=n.getAsJsonObject(id);if(r.has("remove")&&r.get("remove").getAsBoolean())return null;
        int amp=incoming.getAmplifier(),duration=incoming.getDuration();
        if(number(r,"level"))amp=r.get("level").getAsInt()-1;
        else if(number(r,"level_offset"))amp+=r.get("level_offset").getAsInt();
        if(number(r,"ticks"))duration=r.get("ticks").getAsInt();
        else if(number(r,"duration_multiplier")&&duration!=-1)duration=Math.multiplyExact(duration,r.get("duration_multiplier").getAsInt());
        return new MobEffectInstance(incoming.getEffect(),duration,amp,incoming.isAmbient(),incoming.isVisible(),incoming.showIcon());
    }
    private static boolean number(JsonObject r,String name){return r.has(name)&&!r.get(name).isJsonNull();}
    public static void applyAdded(LivingEntity eater,String food){
        if(!(eater.level() instanceof ServerLevel level))return;
        var f=RULES.get(food);if(f==null)return;
        for(var a:f.getAsJsonArray("added")){
            var r=a.getAsJsonObject();if(eater.getRandom().nextFloat()>=r.get("probability").getAsFloat())continue;
            var holder=BuiltInRegistries.MOB_EFFECT.get(Effects.id(r.get("effect").getAsString()));
            if(holder.isEmpty())throw new IllegalStateException("Unregistered configured effect: "+r);
            var h=holder.get();int amp=r.get("level").getAsInt()-1;
            if(h.value().isInstantaneous())h.value().applyInstantaneousEffect(level,eater,eater,eater,amp,1.0);
            else eater.addEffect(new MobEffectInstance(h,r.get("ticks").getAsInt(),amp));
        }
    }
}
