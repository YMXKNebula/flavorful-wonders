package dev.baiwei.mixin;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import java.util.*;
public final class BaiweiMixinPlugin implements IMixinConfigPlugin {
    private static boolean orderedCarpet;
    public void onLoad(String mixinPackage){
        var loader=FabricLoader.getInstance();
        if(!orderedCarpet&&loader.isModLoaded("carpet")&&(loader.isModLoaded("polymorph_plus")||loader.isModLoaded("polymorph"))
                &&loader.getModContainer("carpet").orElseThrow().findPath("carpet/mixins/CustomPacketPayload_networkStuffMixin.class").isPresent()){
            // Polymorph initializes its packet platform while Mixin is still preparing configs.
            // Reorder configuration preparation, leaving Carpet's bytecode and mixin priorities intact.
            // Loader 0.20.1 / Mixin 0.8.7 has no public setter for configuration preparation priority.
            try{
                var handle=org.spongepowered.asm.mixin.transformer.Config.create("carpet.mixins.json",org.spongepowered.asm.mixin.MixinEnvironment.getDefaultEnvironment());
                var config=handle.getConfig();var field=config.getClass().getDeclaredField("priority");
                if(field.trySetAccessible()){
                    // MixinConfig.compareTo sorts preparation priorities in ascending order.
                    field.setInt(config,Math.min(0,field.getInt(config)));orderedCarpet=true;
                    org.slf4j.LoggerFactory.getLogger("baiweiqixiao").info("Prepared Carpet config ahead of eager Polymorph packet initialization");
                }
            }catch(ReflectiveOperationException e){org.slf4j.LoggerFactory.getLogger("baiweiqixiao").warn("Unable to apply optional Carpet preparation ordering",e);}
        }
    }
    public String getRefMapperConfig(){return null;}
    public boolean shouldApplyMixin(String target,String mixin){return !mixin.endsWith("AppleSkinOverlayAccess")||FabricLoader.getInstance().isModLoaded("appleskin");}
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public List<String> getMixins(){return null;}
    public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){}
}
