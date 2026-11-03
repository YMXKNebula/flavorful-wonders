package dev.baiwei.clienttest;
import dev.baiwei.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.resources.Identifier;

public final class FoodRowsClientTest implements FabricClientGameTest {
    public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().create()){
            world.getConnection().waitForChunksDownload();
            world.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
                p.addEffect(new MobEffectInstance(Effects.get(18),24000,0));p.getFoodData().setFoodLevel(30);p.getFoodData().setSaturation(26);
            });
            context.waitFor(mc->mc.player!=null&&Effects.has(mc.player,18)&&mc.player.getFoodData().getFoodLevel()==30&&Math.abs(mc.player.getFoodData().getSaturationLevel()-26)<.01);
            context.setScreen(()->null);context.waitTicks(5);
            var tooltipFoods=new net.minecraft.world.item.ItemStack[3];
            context.runOnClient(mc->{var previews=FoodTooltipClientChecks.verify(mc);System.arraycopy(previews,0,tooltipFoods,0,previews.length);});
            context.runOnClient(mc->{
                var row=Identifier.fromNamespaceAndPath(BaiweiMod.ID,"stomach_food_row");
                if(HudStatusBarHeightRegistry.getHeight(row)-HudStatusBarHeightRegistry.getHeight(VanillaHudElements.FOOD_BAR)!=10)throw new AssertionError("Second row must reserve exactly ten pixels");
                for(int n=1;n<=21;n++)try(var in=mc.getResourceManager().getResource(Identifier.fromNamespaceAndPath(BaiweiMod.ID,"textures/mob_effect/"+Effects.IDS[n]+".png")).orElseThrow().open()){
                    var image=javax.imageio.ImageIO.read(in);if(image.getWidth()!=18||image.getHeight()!=18)throw new AssertionError("Non-native effect icon dimensions");
                }catch(java.io.IOException e){throw new AssertionError(e);}
            });
            BaiweiMod.LOG.info("Baiwei HUD screenshot: {}",context.takeScreenshot("stomach_two_rows"));
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD));});
            context.waitFor(mc->mc.player.getMainHandItem().is(net.minecraft.world.item.Items.BREAD));context.waitTicks(12);
            BaiweiMod.LOG.info("Baiwei food-preview screenshot: {}",context.takeScreenshot("stomach_food_preview"));
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.addEffect(new MobEffectInstance(Effects.get(17),24000,0));});
            context.waitFor(mc->Effects.has(mc.player,17));context.waitTicks(8);
            BaiweiMod.LOG.info("Baiwei double-meal preview: {}",context.takeScreenshot("stomach_food_preview_double_meal"));
            context.runOnClient(mc->mc.player.setAirSupply(50));context.waitTicks(2);
            BaiweiMod.LOG.info("Baiwei air-bar screenshot: {}",context.takeScreenshot("stomach_two_rows_air"));
            context.setScreen(()->new FoodTooltipClientChecks.PreviewScreen(tooltipFoods[0]));context.waitTicks(3);
            BaiweiMod.LOG.info("Food effect tooltip screenshot: {}",context.takeScreenshot("food_effect_tooltip"));
            context.setScreen(()->new FoodTooltipClientChecks.PreviewScreen(tooltipFoods[1]));context.waitTicks(3);
            BaiweiMod.LOG.info("Instant food tooltip screenshot: {}",context.takeScreenshot("instant_food_tooltip"));
            context.setScreen(()->new FoodTooltipClientChecks.PreviewScreen(tooltipFoods[2]));context.waitTicks(3);
            BaiweiMod.LOG.info("Native food tooltip screenshot: {}",context.takeScreenshot("native_food_tooltip"));
            context.setScreen(()->null);context.waitTicks(2);
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.removeEffect(Effects.get(18));});
            context.waitFor(mc->!Effects.has(mc.player,18)&&mc.player.getFoodData().getFoodLevel()==20&&mc.player.getFoodData().getSaturationLevel()<=20);
            context.waitTicks(2);
            context.runOnClient(mc->{var row=Identifier.fromNamespaceAndPath(BaiweiMod.ID,"stomach_food_row");if(HudStatusBarHeightRegistry.getElementHeight(row)!=0)throw new AssertionError("Expired stomach left a HUD gap");});
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.getFoodData().setFoodLevel(0);});
            context.waitFor(mc->mc.player.getFoodData().getFoodLevel()==0);
            context.runOnClient(mc->{
                try{var method=net.minecraft.client.player.LocalPlayer.class.getDeclaredMethod("isSprintingPossible",boolean.class);method.setAccessible(true);if(!(Boolean)method.invoke(mc.player,false))throw new AssertionError("Actual client sprint eligibility still blocks zero food");}
                catch(ReflectiveOperationException e){throw new RuntimeException(e);}
            });
            BaiweiMod.LOG.info("BAIWEI_CLIENT_VERIFIED: native 18px icons, synchronized food/saturation, second food row and expiry layout");
        }
    }
}
