package com.dmsh.staffhelper.mixin;

import com.dmsh.staffhelper.util.NameDecorations;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public class PlayerListEntryDisplayNameMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void staffhelper$appendStarToTab(CallbackInfoReturnable<Text> cir) {
        PlayerListEntry self = (PlayerListEntry)(Object)this;

        String name = self.getProfile().getName();
        Text original = cir.getReturnValue();

        if (original == null) {
            Text base = Text.literal(name);
            Team team = self.getScoreboardTeam();
            original = (team == null) ? base : Team.decorateName(team, base);
        }

        cir.setReturnValue(NameDecorations.withDecorationIfTarget(name, original));
    }
}
