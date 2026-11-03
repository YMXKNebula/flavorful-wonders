package dev.baiwei.clienttest;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import java.util.*;
/** Test-only preparation shim, identical to the existing optional Baiwei ordering fix. */
public final class BaselinePackPlugin implements IMixinConfigPlugin {
    public void onLoad(String name){
        var loader=FabricLoader.getInstance();
        if(loader.isModLoaded("carpet")&&(loader.isModLoaded("polymorph_plus")||loader.isModLoaded("polymorph")))try{
            var config=org.spongepowered.asm.mixin.transformer.Config.create("carpet.mixins.json",org.spongepowered.asm.mixin.MixinEnvironment.getDefaultEnvironment()).getConfig();
            var field=config.getClass().getDeclaredField("priority");field.setAccessible(true);field.setInt(config,Math.min(0,field.getInt(config)));
            org.slf4j.LoggerFactory.getLogger("baiwei_tests").info("BASELINE_FIXTURE: Carpet preparation ordering only; no Baiwei gameplay classes");
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    public String getRefMapperConfig(){return null;}
    public boolean shouldApplyMixin(String target,String mixin){return true;}
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public List<String> getMixins(){return null;}
    public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){}
}
