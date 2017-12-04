package de.Linus122.TimeIsMoney.version.v1_11_R1;

import de.Linus122.TimeIsMoney.tools.ActionBarUtils;
import net.minecraft.server.v1_11_R1.IChatBaseComponent;
import net.minecraft.server.v1_11_R1.PacketPlayOutChat;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import static de.Linus122.TimeIsMoney.tools.Utils.CC;

/**
 * NBT Utils for v1_11_R1.
 *
 * @author Linus122
 * @since 1.9.6.1
 */
public class NBTUtils implements ActionBarUtils {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendActionBarMessage(Player p, String message) {
		IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + CC(message) + "\"}");
		PacketPlayOutChat bar = new PacketPlayOutChat(icbc, (byte) 2);
		((CraftPlayer) p).getHandle().playerConnection.sendPacket(bar);
	}
}
