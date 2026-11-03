package dev.baiwei.client;
import dev.baiwei.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;

public final class BaiweiClient implements ClientModInitializer {
    private static final Identifier ROW=Identifier.fromNamespaceAndPath(BaiweiMod.ID,"stomach_food_row");
    private static boolean appleSkin;
    @Override public void onInitializeClient(){
        FoodEffectTooltips.init();
        appleSkin=FabricLoader.getInstance().isModLoaded("appleskin");
        if(appleSkin){
            AppleSkinCompat.init();
            HudElementRegistry.attachElementBefore(VanillaHudElements.FOOD_BAR,Identifier.fromNamespaceAndPath(BaiweiMod.ID,"food_frame"),(g,d)->AppleSkinCompat.beginFrame());
        }
        HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR,ROW,(g,d)->draw(g));
        HudStatusBarHeightRegistry.addRight(ROW,p->showExtraRow(p)?10:0);
    }
    private static boolean showExtraRow(Player p){return Effects.has(p,18)&&!p.isCreative()&&!p.isSpectator()&&!(p.getVehicle() instanceof net.minecraft.world.entity.LivingEntity);}
    private static void draw(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();
        if(!(mc.getCameraEntity() instanceof Player p)||!showExtraRow(p))return;
        int right=g.guiWidth()/2+91,top=g.guiHeight()-HudStatusBarHeightRegistry.getHeight(ROW);
        int extra=Math.clamp(p.getFoodData().getFoodLevel()-20,0,20);
        String suffix=p.hasEffect(MobEffects.HUNGER)?"_hunger":"";
        for(int i=0;i<10;i++){
            int x=right-i*8-9;
            g.blitSprite(RenderPipelines.GUI_TEXTURED,Identifier.withDefaultNamespace("hud/food_empty"+suffix),x,top,9,9);
            String fill=extra>i*2+1?"full":extra==i*2+1?"half":null;
            if(fill!=null)g.blitSprite(RenderPipelines.GUI_TEXTURED,Identifier.withDefaultNamespace("hud/food_"+fill+suffix),x,top,9,9);
        }
        if(appleSkin)AppleSkinCompat.drawExtra(g,p,right,top);
    }
}
