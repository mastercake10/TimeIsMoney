package v1_10_R1;

import org.bukkit.craftbukkit.v1_9_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import de.Linus122.TimeIsMoney.Utils;
import net.minecraft.server.v1_9_R1.IChatBaseComponent;
import net.minecraft.server.v1_9_R1.PacketPlayOutChat;

public class NBTUtils implements Utils{
  public void sendActionBarMessage(Player p, String message)
  {
	    IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + message.replace('&', '§') + "\"}");
	    PacketPlayOutChat bar = new PacketPlayOutChat(icbc, (byte)2);
	    ((CraftPlayer)p).getHandle().playerConnection.sendPacket(bar);
  }
}
