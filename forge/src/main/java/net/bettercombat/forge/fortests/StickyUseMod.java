package net.bettercombat.forge.fortests;

import net.bettercombat.BetterCombat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.option.KeyBinding;

@Mod.EventBusSubscriber(modid = BetterCombat.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class StickyUseMod {

    private static final KeyBinding TOGGLE_STICKY_USE = new KeyBinding (
            "key." + BetterCombat.MODID + ".toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.misc"
    );

    private static boolean stickyUseEnabled = false;
    private static boolean lastChange = true;
    private static int tickDalay = 10;
    private static int currentTickDelay = 0;

    @Mod.EventBusSubscriber(
            modid = BetterCombat.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegisterKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_STICKY_USE);
        }
    }

    private static void onRegisterKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_STICKY_USE);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // обработка горячей клавиши
        if (TOGGLE_STICKY_USE.isPressed() && currentTickDelay == tickDalay) {
            stickyUseEnabled = !stickyUseEnabled;
            currentTickDelay = 0;
        }

        if (currentTickDelay < tickDalay){
            currentTickDelay++;
        }

        if (lastChange != stickyUseEnabled) {
            if (stickyUseEnabled) {
                mc.options.useKey.setPressed(true);
            } else {
                mc.options.useKey.setPressed(false);
            }
            lastChange = stickyUseEnabled;
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        // на всякий случай снимаем зажатие при выходе
        stickyUseEnabled = false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.options.useKey.setPressed(false);
        }
    }
}