package net.minecraft.util;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;

import java.util.Map;
import java.util.UUID;

public class Session
{
    private final String username;
    private final String playerID;
    private final String token;
    private final Session.Type field_152429_d;

    public Session(String p_i1098_1_, String p_i1098_2_, String p_i1098_3_, String p_i1098_4_)
    {
        this.username = p_i1098_1_;
        this.playerID = p_i1098_2_;
        this.token = p_i1098_3_;
        this.field_152429_d = Session.Type.func_152421_a(p_i1098_4_);
    }

    public String getSessionID()
    {
        return "token:" + this.token + ":" + this.playerID;
    }

    public String getPlayerID()
    {
        return this.playerID;
    }

    public UUID getUniqueID() {
        return UUIDTypeAdapter.fromString(this.getPlayerID());
    }

    public String getUsername()
    {
        return this.username;
    }

    public String getToken()
    {
        return this.token;
    }

    public GameProfile func_148256_e()
    {
        try
        {
            UUID var1 = UUIDTypeAdapter.fromString(this.getPlayerID());
            return new GameProfile(var1, this.getUsername());
        }
        catch (IllegalArgumentException var2)
        {
            return new GameProfile((UUID)null, this.getUsername());
        }
    }

    public Session.Type func_152428_f()
    {
        return this.field_152429_d;
    }

    public static enum Type
    {
        LEGACY("LEGACY", 0, "legacy"),
        MOJANG("MOJANG", 1, "mojang");
        private static final Map field_152425_c = Maps.newHashMap();
        private final String field_152426_d;

        private static final Session.Type[] $VALUES = new Session.Type[]{LEGACY, MOJANG};

        private Type(String p_i1096_1_, int p_i1096_2_, String p_i1096_3_)
        {
            this.field_152426_d = p_i1096_3_;
        }

        public static Session.Type func_152421_a(String p_152421_0_)
        {
            return (Session.Type)field_152425_c.get(p_152421_0_.toLowerCase());
        }

        static {
            Session.Type[] var0 = values();
            int var1 = var0.length;

            for (int var2 = 0; var2 < var1; ++var2)
            {
                Session.Type var3 = var0[var2];
                field_152425_c.put(var3.field_152426_d, var3);
            }
        }
    }
}package net.peuc.client.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TargetUtils {

    public static PlayerEntity getClosestTarget(double range) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return null;

        PlayerEntity closest = null;
        double minDistance = range * range;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;

            double distSq = mc.player.squaredDistanceTo(player);
            if (distSq < minDistance) {
                minDistance = distSq;
                closest = player;
            }
        }
        return closest;
    }

    public static float[] getRotationsToEntity(PlayerEntity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d targetPos = target.getEyePos();

        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;

        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        return new float[]{
            MathHelper.wrapDegrees(yaw),
            MathHelper.wrapDegrees(pitch)
        };
    }
}package net.peuc.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.peuc.client.utils.TargetUtils;

public class KillAura {
    public static boolean enabled = true;

    public static void onTick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        PlayerEntity target = TargetUtils.getClosestTarget(4.5);
        if (target != null) {
            float[] rotations = TargetUtils.getRotationsToEntity(target);
            mc.player.setYaw(rotations[0]);
            mc.player.setPitch(rotations[1]);

            if (mc.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}package net.peuc.client.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.peuc.client.utils.TargetUtils;

public class ElytraHelper {
    public static boolean enabled = true;

    public static void onTick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !mc.player.isFallFlying()) return;

        PlayerEntity target = TargetUtils.getClosestTarget(60.0);
        if (target != null) {
            float[] rotations = TargetUtils.getRotationsToEntity(target);
            mc.player.setYaw(rotations[0]);
            mc.player.setPitch(rotations[1]);
        }
    }
}package net.peuc.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.peuc.client.modules.ElytraHelper;
import net.peuc.client.modules.KillAura;

public class PeucClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null) {
                KillAura.onTick();
                ElytraHelper.onTick();
            }
        });
    }
}package net.peuc.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.peuc.client.modules.ElytraHelper;
import net.peuc.client.modules.KillAura;
import org.lwjgl.glfw.GLFW;

public class PeucClientMod implements ClientModInitializer {

    // R tuşuna KillAura atıyoruz
    private static KeyBinding killAuraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.peuc.killaura",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_R, // R Tuşu
        "category.peuc"
    ));

    // G tuşuna ElytraHelper atıyoruz
    private static KeyBinding elytraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.peuc.elytra",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_G, // G Tuşu
        "category.peuc"
    ));

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // R tuşuna basıldığında KillAura durumunu değiştirir
            while (killAuraKey.wasPressed()) {
                KillAura.enabled = !KillAura.enabled;
                client.player.sendMessage(
                    net.minecraft.text.Text.literal("KillAura: " + (KillAura.enabled ? "Açık" : "Kapalı")), 
                    true
                );
            }

            // G tuşuna basıldığında ElytraHelper durumunu değiştirir
            while (elytraKey.wasPressed()) {
                ElytraHelper.enabled = !ElytraHelper.enabled;
                client.player.sendMessage(
                    net.minecraft.text.Text.literal("ElytraHelper: " + (ElytraHelper.enabled ? "Açık" : "Kapalı")), 
                    true
                );
            }

            // Modüller aktifse çalıştır
            KillAura.onTick();
            ElytraHelper.onTick();
        });
    }
}
package net.peuc.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.peuc.client.modules.ElytraHelper;
import net.peuc.client.modules.KillAura;
import org.lwjgl.glfw.GLFW;

public class PeucClientMod implements ClientModInitializer {

    // R tuşuna KillAura atıyoruz
    private static KeyBinding killAuraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.peuc.killaura",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_R, // R Tuşu
        "category.peuc"
    ));

    // G tuşuna ElytraHelper atıyoruz
    private static KeyBinding elytraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
        "key.peuc.elytra",
        InputUtil.Type.KEYSYM,
        GLFW.GLFW_KEY_G, // G Tuşu
        "category.peuc"
    ));

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // R tuşuna basıldığında KillAura durumunu değiştirir
            while (killAuraKey.wasPressed()) {
                KillAura.enabled = !KillAura.enabled;
                client.player.sendMessage(
                    net.minecraft.text.Text.literal("KillAura: " + (KillAura.enabled ? "Açık" : "Kapalı")), 
                    true
                );
            }

            // G tuşuna basıldığında ElytraHelper durumunu değiştirir
            while (elytraKey.wasPressed()) {
                ElytraHelper.enabled = !ElytraHelper.enabled;
                client.player.sendMessage(
                    net.minecraft.text.Text.literal("ElytraHelper: " + (ElytraHelper.enabled ? "Açık" : "Kapalı")), 
                    true
                );
            }

            // Modüller aktifse çalıştır
            KillAura.onTick();
            ElytraHelper.onTick();
        });
    }
}





