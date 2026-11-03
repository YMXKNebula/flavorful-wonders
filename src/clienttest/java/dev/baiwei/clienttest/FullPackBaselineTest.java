package dev.baiwei.clienttest;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
/** No Baiwei classes or resources: checks the pack's native integrated-server path. */
public final class FullPackBaselineTest implements FabricClientGameTest {
    public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().create()){
            world.getConnection().waitForChunksDownload();
            context.waitFor(mc->mc.player!=null&&mc.level!=null);
            context.setScreen(()->null);context.waitTicks(40);
            context.runOnClient(mc->{if(mc.player.getFoodData().getFoodLevel()<0)throw new AssertionError("Invalid native player food state");});
            org.slf4j.LoggerFactory.getLogger("baiwei_tests").info("BASELINE_CLIENT_VERIFIED: full pack integrated world joined without Baiwei");
            context.takeScreenshot("full_pack_baseline");
        }
    }
}
