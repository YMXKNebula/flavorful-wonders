package dev.baiwei.client;

import dev.baiwei.*;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import java.text.NumberFormat;
import java.util.*;

/** Displays configured grants after native food tooltip callbacks, using the same native overrides as consumption. */
public final class FoodEffectTooltips {
    private record Key(String id,float probability){}
    private record Preview(MobEffectInstance effect,float probability){}
    public static void init(){
        var phase=Identifier.fromNamespaceAndPath(BaiweiMod.ID,"food_effect_tooltips");
        ItemTooltipCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE,phase);
        ItemTooltipCallback.EVENT.register(phase,FoodEffectTooltips::append);
    }
    private static void append(ItemStack stack,Item.TooltipContext context,TooltipFlag flag,List<Component> lines){
        if(stack.isEmpty()||stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY,TooltipDisplay.DEFAULT).hideTooltip())return;
        String food=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if(!FoodRules.hasRule(food)&&!stack.has(DataComponents.FOOD))return;
        var previews=new LinkedHashMap<Key,Preview>();var replacedNames=new HashSet<String>();
        for(String id:FoodRules.overriddenNativeEffects(food))BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(id)).ifPresent(h->replacedNames.add(h.value().getDescriptionId()));
        var consumable=stack.get(DataComponents.CONSUMABLE);
        if(consumable!=null)for(var action:consumable.onConsumeEffects())if(action instanceof ApplyStatusEffectsConsumeEffect apply){
            for(var effect:apply.effects()){
                replacedNames.add(effect.getDescriptionId());var adjusted=FoodRules.adjustNativeEffect(food,effect);
                if(adjusted!=null)collect(previews,adjusted,apply.probability());
            }
        }
        for(var grant:FoodRules.addedEffects(food)){
            var holder=BuiltInRegistries.MOB_EFFECT.get(Effects.id(grant.id()));
            if(holder.isEmpty())continue;
            var effect=new MobEffectInstance(holder.get(),grant.ticks(),grant.level()-1);
            replacedNames.add(effect.getDescriptionId());collect(previews,effect,grant.probability());
        }
        // Match structured effect-name components only; retain food lore and special-use descriptions.
        lines.removeIf(line->isEffectLine(line,replacedNames));
        if(previews.isEmpty())return;
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.baiweiqixiao.food_effects").withStyle(ChatFormatting.GOLD));
        for(var preview:previews.values()){
            var effect=preview.effect();
            Component potency=effect.getAmplifier()==0?Component.literal("I"):Component.translatable("potion.potency."+effect.getAmplifier());
            Component name=Component.translatable("potion.withAmplifier",Component.translatable(effect.getDescriptionId()),potency);
            Component duration=effect.getEffect().value().isInstantaneous()?Component.translatable("tooltip.baiweiqixiao.instant"):MobEffectUtil.formatDuration(effect,1,context.tickRate());
            Component line=Component.translatable("potion.withDuration",name,duration);
            if(preview.probability()<1){
                var format=NumberFormat.getPercentInstance(Locale.ROOT);format.setMaximumFractionDigits(1);
                line=Component.translatable("tooltip.baiweiqixiao.probability",line,format.format(preview.probability()));
            }
            lines.add(line.copy().withStyle(effect.getEffect().value().getCategory().getTooltipFormatting()));
        }
    }
    private static void collect(Map<Key,Preview> previews,MobEffectInstance effect,float probability){
        if(probability<=0)return;
        var key=new Key(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).toString(),probability);
        var previous=previews.get(key);
        var merged=previous==null?new MobEffectInstance(effect):new MobEffectInstance(previous.effect());
        if(previous!=null)merged.update(effect); // Match vanilla's existing effect merge, without changing gameplay.
        previews.put(key,new Preview(merged,probability));
    }
    private static boolean isEffectLine(Component line,Set<String> names){
        if(!(line.getContents() instanceof TranslatableContents text))return false;
        return (names.contains(text.getKey())||text.getKey().startsWith("potion."))&&containsEffectName(line,names);
    }
    private static boolean containsEffectName(Component line,Set<String> names){
        if(line.getContents() instanceof TranslatableContents text){
            if(names.contains(text.getKey()))return true;
            for(Object arg:text.getArgs())if(arg instanceof Component c&&containsEffectName(c,names))return true;
        }
        for(Component sibling:line.getSiblings())if(containsEffectName(sibling,names))return true;
        return false;
    }
}
