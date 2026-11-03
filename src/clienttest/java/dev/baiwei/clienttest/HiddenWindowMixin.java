package dev.baiwei.clienttest.mixin;
import com.mojang.blaze3d.platform.Window;
import com.mojang.renderpearl.api.device.GpuBackend;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
/** Isolated test fixture only. Never ships in the Mod JAR. */
@Mixin(Window.class)
abstract class HiddenWindowMixin {
    @WrapOperation(method="createWindow",at=@At(value="INVOKE",target="Lcom/mojang/renderpearl/api/device/GpuBackend;createWindow(Ljava/lang/String;IIJ)J"))
    private long baiwei$hidden(GpuBackend backend,String title,int width,int height,long flags,Operation<Long> original){return original.call(backend,title,width,height,flags|SDLVideo.SDL_WINDOW_HIDDEN);}
}
