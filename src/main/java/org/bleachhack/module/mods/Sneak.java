package org.bleachhack.module.mods;

import org.bleachhack.event.events.EventPacket;
import org.bleachhack.event.events.EventTick;
import org.bleachhack.eventbus.BleachSubscribe;
import org.bleachhack.module.Module;
import org.bleachhack.module.ModuleCategory;
import org.bleachhack.setting.module.SettingMode;

import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;

public class Sneak extends Module {

	private boolean packetSent;

	public Sneak() {
		super("Sneak", KEY_UNBOUND, ModuleCategory.MOVEMENT, "Makes you automatically sneak.",
				new SettingMode("Mode", "Legit", "Packet").withDesc("Mode for sneaking (Only other players will see u sneaking with packet mode)."));
	}

	@Override
	public void onDisable(boolean inWorld) {
		packetSent = false;
		mc.options.keyShift.setDown(false);

		// TODO(26.2): PRESS/RELEASE_SHIFT_KEY command packets were removed; sneak state is now sent via ServerboundPlayerInputPacket.
		if (inWorld)
			mc.player.connection.send(new ServerboundPlayerInputPacket(mc.player.input.keyPresses));

		super.onDisable(inWorld);
	}

	@Override
	public void onEnable(boolean inWorld) {
		super.onEnable(inWorld);

		if (getSetting(0).asMode().getMode() == 1) {
			if (inWorld)
				mc.player.connection.send(new ServerboundPlayerInputPacket(withShift(mc.player.input.keyPresses)));

			packetSent = true;
		}
	}

	@BleachSubscribe
	public void onTick(EventTick event) {
		if (getSetting(0).asMode().getMode() == 0) {
			mc.options.keyShift.setDown(true);
		} else if (getSetting(0).asMode().getMode() == 1 && !packetSent) {
			mc.player.connection.send(new ServerboundPlayerInputPacket(withShift(mc.player.input.keyPresses)));
			packetSent = true;
		}
	}

	@BleachSubscribe
	public void onSendPacket(EventPacket.Send event) {
		// Was: cancel RELEASE_SHIFT_KEY packets. Now: force the shift flag on outgoing input packets.
		if (event.getPacket() instanceof ServerboundPlayerInputPacket p && !p.input().shift())
			event.setPacket(new ServerboundPlayerInputPacket(withShift(p.input())));
	}

	private static Input withShift(Input in) {
		return new Input(in.forward(), in.backward(), in.left(), in.right(), in.jump(), true, in.sprint());
	}
}
