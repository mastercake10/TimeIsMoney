package v1_12_R1;

import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import de.Linus122.TimeIsMoney.Utils;
import net.minecraft.server.v1_12_R1.ChatMessageType;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.PacketPlayOutChat;

public class NBTUtils implements Utils{
  public void sendActionBarMessage(Player p, String message)
  {
	    IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + message.replace('&', '§') + "\"}");
	    PacketPlayOutChat bar = new PacketPlayOutChat(icbc, ChatMessageType.GAME_INFO);
	    ((CraftPlayer)p).getHandle().playerConnection.sendPacket(bar);
  }
}
