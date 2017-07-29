package v1_11_R1;

import de.Linus122.TimeIsMoney.ActionBarUtils;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_11_R1.IChatBaseComponent;
import net.minecraft.server.v1_11_R1.PacketPlayOutChat;

import static de.Linus122.TimeIsMoney.Utils.CC;

public class NBTUtils implements ActionBarUtils {
  public void sendActionBarMessage(Player p, String message)
  {
	    IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + CC(message)  + "\"}");
	    PacketPlayOutChat bar = new PacketPlayOutChat(icbc, (byte)2);
	    ((CraftPlayer)p).getHandle().playerConnection.sendPacket(bar);
  }
}
