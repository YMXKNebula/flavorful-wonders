package dev.baiwei.test.mixin;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Test-only: invoke Xaero's normal late server initializer for Fabric's GameTestServer. */
@Pseudo
@Mixin(targets="xaero.lib.common.event.CommonEventsFabric",remap=false)
abstract class XaeroGameTestLifecycleMixin {
    @Inject(method="serverStarting",at=@At("HEAD"))
    private void baiwei$normalServerLifecycle(MinecraftServer server,CallbackInfo ci){
        if(server instanceof DedicatedServer)return;
        try{
            Object lib=Class.forName("xaero.lib.XaeroLib").getField("INSTANCE").get(null);
            lib.getClass().getMethod("tryLoadLaterServer").invoke(lib);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Test-only Xaero lifecycle adapter failed",e);}
    }
}
