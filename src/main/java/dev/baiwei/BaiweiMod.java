package dev.baiwei;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BaiweiMod implements ModInitializer {
    public static final String ID="baiweiqixiao";
    public static final Logger LOG=LoggerFactory.getLogger(ID);
    @Override public void onInitialize() {
        Effects.register();
        FoodRules.load();
        ServerPlayerEvents.COPY_FROM.register((oldPlayer,newPlayer,alive)->RuntimeEffects.copyHover(oldPlayer,newPlayer));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for(var p:server.getPlayerList().getPlayers()) RuntimeEffects.playerTick(p);
        });
        LOG.info("百味奇效 1.0: {} food mappings, 21 original effects",FoodRules.size());
    }
}
