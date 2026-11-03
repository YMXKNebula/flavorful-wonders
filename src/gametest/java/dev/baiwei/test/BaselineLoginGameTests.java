package dev.baiwei.test;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** No Baiwei imports or entrypoint; reproduces the pack's native login path without this mod. */
public final class BaselineLoginGameTests {
    @GameTest public void unmodifiedPackCanLogInNativeTestPlayer(GameTestHelper t){t.makeMockServerPlayerInLevel();t.succeed();}
}
