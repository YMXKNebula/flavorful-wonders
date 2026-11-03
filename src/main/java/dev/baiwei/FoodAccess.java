package dev.baiwei;
import net.minecraft.world.entity.player.Player;
public interface FoodAccess {
    void baiwei$bind(Player player);
    float baiwei$pay(float amount);
    float baiwei$available();
    void baiwei$updateCapacity();
}
