package dev.baiwei.mixin.client;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="squeek.appleskin.client.HUDOverlayHandler",remap=false)
public interface AppleSkinOverlayAccess {
    @Accessor("flashAlpha") float baiwei$flashAlpha();
}
