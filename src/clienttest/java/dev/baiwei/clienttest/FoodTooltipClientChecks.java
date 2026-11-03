package dev.baiwei.clienttest;
import dev.baiwei.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.*;
import java.util.*;

public final class FoodTooltipClientChecks {
    public static ItemStack[] verify(Minecraft mc){
        int checked=0,grants=0,removed=0,kept=0,instant=0;ItemStack instantFood=null;
        try(var in=FoodTooltipClientChecks.class.getResourceAsStream("/data/baiweiqixiao/food_effects.json")){
            var root=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("foods");
            for(var entry:root.entrySet()){
                var item=BuiltInRegistries.ITEM.getOptional(Identifier.parse(entry.getKey()));if(item.isEmpty())continue;
                var stack=new ItemStack(item.get());if(!stack.has(DataComponents.CONSUMABLE))continue;
                var lines=lines(mc,stack);String text=join(lines);checked++;
                var added=entry.getValue().getAsJsonObject().getAsJsonArray("added");
                if(!added.isEmpty())check(text.contains("百味奇效 · 食用效果"),entry.getKey()+" has no effect header");
                for(var value:added){
                    var grant=value.getAsJsonObject();var holder=BuiltInRegistries.MOB_EFFECT.get(Effects.id(grant.get("effect").getAsString())).orElseThrow();
                    String name=Component.translatable(holder.value().getDescriptionId()).getString();
                    check(text.contains(name),entry.getKey()+" missing localized "+name);grants++;
                    if(holder.value().isInstantaneous()){
                        check(lines.stream().anyMatch(c->c.getString().contains(name)&&c.getString().contains("瞬时")&&!c.getString().contains("0:00")),entry.getKey()+" instant effect is misleading");
                        instant++;instantFood=stack;
                    }
                }
                var nativeRules=entry.getValue().getAsJsonObject().getAsJsonObject("native");
                if(nativeRules.has("farmersdelight:nourishment")){
                    var rule=nativeRules.getAsJsonObject("farmersdelight:nourishment");
                    if(rule.get("remove").getAsBoolean()){check(!text.contains("滋养"),entry.getKey()+" still advertises removed nourishment");removed++;}
                    else{
                        long count=lines.stream().filter(c->c.getString().contains("滋养")).count();check(count==1,entry.getKey()+" nourishment duplicated or missing: "+count);kept++;
                        if(rule.has("ticks")&&!rule.get("ticks").isJsonNull()){
                            int seconds=rule.get("ticks").getAsInt()/20;String time=seconds/60+":"+String.format(java.util.Locale.ROOT,"%02d",seconds%60);
                            check(lines.stream().anyMatch(c->c.getString().contains("滋养")&&c.getString().contains(time)),entry.getKey()+" stale native duration");
                        }
                    }
                }
                check(!text.contains("tooltip.baiweiqixiao.")&&!text.contains("effect.baiweiqixiao."),entry.getKey()+" untranslated tooltip");
            }
        }catch(java.io.IOException e){throw new RuntimeException(e);}
        var bacon=stack("farmersdelight:bacon");String baconText=join(lines(mc,bacon));
        check(baconText.contains("活力 I")&&baconText.contains("1:40"),"Updated semi-processed food level/time omitted");
        check(join(lines(mc,stack("farmersdelight:melon_popsicle"))).contains("50%"),"Updated popsicle healing probability omitted");
        check(join(lines(mc,stack("farmersdelight:honey_cookie"))).contains("25%"),"Batch cookie probability changed unexpectedly");
        var salad=BuiltInRegistries.ITEM.getOptional(Identifier.parse("delightful:field_salad"));
        if(salad.isPresent()){
            String text=join(lines(mc,new ItemStack(salad.get())));
            check(lines(mc,new ItemStack(salad.get())).stream().anyMatch(c->c.getString().contains("急迫 II")&&c.getString().contains("4:30"))&&!text.contains("滋养"),"Updated salad tooltip does not match approved rules");
        }
        check(!join(lines(mc,new ItemStack(Items.CARROT))).contains("百味奇效 · 食用效果"),"Raw materials falsely advertise added effects");
        var custom=bacon.copy();var consume=custom.get(DataComponents.CONSUMABLE);
        custom.set(DataComponents.CONSUMABLE,new net.minecraft.world.item.component.Consumable(consume.consumeSeconds(),consume.animation(),consume.sound(),consume.hasConsumeParticles(),List.of(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.GLOWING,100)),.25f))));
        check(join(lines(mc,custom)).contains("25%"),"Native conditional effect probability omitted");
        int nativeFoods=0,nativeEffects=0;
        for(var item:BuiltInRegistries.ITEM){
            var stack=new ItemStack(item);if(!stack.has(DataComponents.FOOD))continue;
            String food=BuiltInRegistries.ITEM.getKey(item).toString();if(FoodRules.hasRule(food))continue;
            var c=stack.get(DataComponents.CONSUMABLE);if(c==null)continue;
            String text=join(lines(mc,stack));boolean hasNative=false;
            for(var action:c.onConsumeEffects())if(action instanceof net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect apply){
                if(apply.probability()<=0)continue;
                for(var e:apply.effects()){
                    String name=Component.translatable(e.getDescriptionId()).getString();
                    check(text.contains(name),food+" native-only food missing "+name);nativeEffects++;hasNative=true;
                }
            }if(hasNative)nativeFoods++;
        }
        String apple=join(lines(mc,new ItemStack(Items.GOLDEN_APPLE)));
        check(apple.contains("生命恢复")&&apple.contains("伤害吸收"),"Vanilla native food effects omitted");
        for(var grant:FoodRules.addedEffects("farmersdelight:vegetable_soup"))if(grant.id().equals("original:01"))check(grant.level()==1,"Old durability level survives in tooltip data");
        check(Component.translatable(Effects.get(13).value().getDescriptionId()).getString().equals("轻如鸿毛"),"Feather name not updated");
        check(Component.translatable(Effects.get(19).value().getDescriptionId()).getString().equals("恐吓"),"Intimidation name not updated");
        BaiweiMod.LOG.info("BAIWEI_NATIVE_TOOLTIPS_VERIFIED: {} native-only foods, {} native entries (including vanilla foods)",nativeFoods,nativeEffects);
        BaiweiMod.LOG.info("BAIWEI_TOOLTIPS_VERIFIED: {} foods, {} added grants, {} removed and {} retained nourishment, {} instant grants",checked,grants,removed,kept,instant);
        return new ItemStack[]{salad.map(ItemStack::new).orElse(bacon),instantFood==null?bacon:instantFood,new ItemStack(Items.GOLDEN_APPLE)};
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static List<Component> lines(Minecraft mc,ItemStack s){return s.getTooltipLines(Item.TooltipContext.of(mc.level),mc.player,TooltipFlag.NORMAL);}
    private static String join(List<Component> lines){return String.join("\n",lines.stream().map(Component::getString).toList());}
    private static ItemStack stack(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));}
    public static final class PreviewScreen extends Screen {
        private final ItemStack stack;
        public PreviewScreen(ItemStack stack){super(Component.literal("Food effects"));this.stack=stack;}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial){
            g.fill(0,0,width,height,0xff20232a);
            g.text(font,stack.getHoverName(),30,25,0xffeeeeee);
            g.item(stack,80,70);
            g.setTooltipForNextFrame(font,stack,105,75);
        }
    }
}
