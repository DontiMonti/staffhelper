package com.dmsh.staffhelper.mixin;

import com.dmsh.staffhelper.util.NameDecorations;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public abstract class ChatHudStarMixin {
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private Text staffhelper$decorateChatMessage2(Text value, Text message, MessageSignatureData sig, MessageIndicator ind) {
        return NameDecorations.applyStarsToChat(value);
    }
}
