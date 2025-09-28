package net.bettercombat.client.animation;

import com.mojang.logging.LogUtils;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import net.bettercombat.logic.InputManager;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.PlayerInputState;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class CustomAnimationPlayer extends KeyframeAnimationPlayer {

    public static final Logger LOGGER = LogManager.getLogger(CustomAnimationPlayer.class);

    private final String name;


    public CustomAnimationPlayer(KeyframeAnimation emote, int t, boolean mutable) {
        super(emote, t, mutable);
        this.name = "";
    }

    public CustomAnimationPlayer(KeyframeAnimation emote, int t, String name) {
        super(emote, t, false);
        this.name = name;
    }

    public boolean isWindingDown(float tickDelta) {
        int windDownStart = getData().endTick + ((getData().stopTick - getData().endTick) / 4);
        return ((getTick() + tickDelta) > (windDownStart + 0.5F)); // + 0.5 for smoother transition
    }

    @Override
    public @NotNull FirstPersonMode getFirstPersonMode(float tickDelta) {
        if (isWindingDown(tickDelta)) {
            return FirstPersonMode.NONE;
        }
        return super.getFirstPersonMode(tickDelta);
    }

    @Override
    public void tick() {
        var mask = PlayerInputState.get(MinecraftClient.getInstance().player.getUuid()).getMask();
        var isRightClicked = InputManager.rightClick(mask);
        var vector = InputManager.asString(mask);
        var isLastTick = getCurrentTick() >= getData().returnToTick - 1 && name!=null && name.contains("block") && name.contains(vector);
        if (!isRightClicked || !isLastTick) {
            super.tick();
        }
    }
}
