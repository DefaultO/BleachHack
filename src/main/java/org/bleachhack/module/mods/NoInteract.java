package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventInteract;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingBlockList;

import java.util.stream.Stream;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * @author CUPZYY
 */

public class NoInteract extends Module {

    public NoInteract() {
        super("NoInteract", KEY_UNBOUND, ModuleCategory.PLAYER, "Prevents you from interacting with certain blocks.",
                new SettingBlockList("Edit Blocks", "Edit NoInteract Blocks",
                        // 26.2: individual bed constants were replaced by the Blocks.BED ColorCollection
                        Stream.concat(Blocks.BED.asList().stream(), Stream.of(Blocks.RESPAWN_ANCHOR))
                                .toArray(Block[]::new)).withDesc("Edit the blocks to not interact with."));
    }

    @BleachSubscribe
    public void onSendPacket(EventInteract.InteractBlock event) {
        if (getSetting(0).asList(Block.class).contains(mc.level.getBlockState(event.getHitResult().getBlockPos()).getBlock())) {
        	event.setCancelled(true);
        }
    }
}
