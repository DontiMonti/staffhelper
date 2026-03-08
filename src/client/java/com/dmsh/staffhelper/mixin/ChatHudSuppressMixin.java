package com.dmsh.staffhelper.mixin;

import com.dmsh.staffhelper.feature.AfkZoneFeature;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudSuppressMixin {

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void staffhelper$suppressAlts1(Text message, CallbackInfo ci) {
        if (message == null) return;
        if (AfkZoneFeature.shouldSuppressChatMessage(message.getString())) {
            ci.cancel();
        }
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void staffhelper$suppressAlts2(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        if (message == null) return;
        if (AfkZoneFeature.shouldSuppressChatMessage(message.getString())) {
            ci.cancel();
        }
    }
}
