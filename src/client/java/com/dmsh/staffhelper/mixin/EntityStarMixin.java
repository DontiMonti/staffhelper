package com.dmsh.staffhelper.mixin;

import com.dmsh.staffhelper.util.NameDecorations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds a yellow star to the end of DontiMonti's name in places where
 * the game uses {@link Entity#getDisplayName()} (e.g. name tag above the head).
 */
@Mixin(Entity.class)
public abstract class EntityStarMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void staffhelper$appendStarToDisplayName(CallbackInfoReturnable<Text> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity player)) return;

        String nick = player.getGameProfile().getName();
        Text base = cir.getReturnValue();
        if (base == null) return;

        Text decorated = NameDecorations.withDecorationIfTarget(nick, base);
        if (decorated != base) {
            cir.setReturnValue(decorated);
        }
    }
}
