package dev.baiwei.client;
import dev.baiwei.Effects;
import dev.baiwei.mixin.client.AppleSkinOverlayAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import squeek.appleskin.api.event.*;
import squeek.appleskin.client.HUDOverlayHandler;
import squeek.appleskin.ModConfig;

/** Loaded on the client only, after checking AppleSkin's mod presence. */
public final class AppleSkinCompat {
    private static HUDOverlayEvent.Saturation saturation;
    private static HUDOverlayEvent.HungerRestored heldFood;
    public static void init(){
        HUDOverlayEvent.Saturation.EVENT.register(e->saturation=e);
        HUDOverlayEvent.HungerRestored.EVENT.register(e->heldFood=e);
        FoodValuesEvent.EVENT.register(e->{
            if(e.player!=null&&Effects.has(e.player,17)&&e.modifiedFoodComponent!=null){
                var f=e.modifiedFoodComponent;e.modifiedFoodComponent=new FoodProperties(f.nutrition()*2,f.saturation()*2,f.canAlwaysEat());
            }
        });
    }
    public static void beginFrame(){saturation=null;heldFood=null;}
    public static void drawExtra(GuiGraphicsExtractor g,Player p,int right,int top){
        var h=HUDOverlayHandler.INSTANCE;var config=ModConfig.INSTANCE;if(h==null||config==null)return;
        var mc=Minecraft.getInstance();int ticks=mc.gui.hud.getGuiTicks();
        float currentSat=Math.clamp(p.getFoodData().getSaturationLevel()-20,0,20);
        if(saturation!=null&&saturation.context==g&&!saturation.isCanceled)h.drawSaturationOverlay(g,0,currentSat,mc,right,top,1,ticks);
        if(heldFood==null||heldFood.context!=g||heldFood.isCanceled)return;
        int now=p.getFoodData().getFoodLevel(),extraFood=Math.clamp(now-20,0,20);
        int future=Math.min(40,now+heldFood.foodComponent.nutrition()),futureExtra=Math.clamp(future-20,0,20);
        float alpha=((AppleSkinOverlayAccess)h).baiwei$flashAlpha();
        if(futureExtra>extraFood)h.drawHungerOverlay(g,extraFood,futureExtra-extraFood,mc,right,top,alpha,config.showVanillaAnimationsOverlay,ticks);
        if(config.showSaturationHudOverlay){
            float futureSat=Math.clamp(Math.min(future,p.getFoodData().getSaturationLevel()+heldFood.foodComponent.saturation())-20,0,20);
            if(futureSat>currentSat)h.drawSaturationOverlay(g,futureSat-currentSat,currentSat,mc,right,top,alpha,ticks);
        }
    }
}
