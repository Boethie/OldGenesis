package genesis.network;

import java.util.*;

import genesis.block.tileentity.TileEntityKnapper.KnappingSlotMessage;
import genesis.common.Genesis;
import genesis.entity.living.flying.EntityMeganeura.MeganeuraUpdateMessage;
import genesis.network.client.*;
import genesis.util.sound.SoundUtils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;

import net.minecraftforge.fml.common.network.NetworkRegistry.*;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.*;

public class GenesisNetwork extends SimpleNetworkWrapper
{
	protected int currentID = 0;
	
	public GenesisNetwork(String channelName)
	{
		super(channelName);
	}
	
	public <Q extends IMessage, A extends IMessage> void registerMessage(IMessageHandler<? super Q, ? extends A> messageHandler, Class<Q> requestMessageType, Side handlerSide)
	{
		registerMessage(messageHandler, requestMessageType, currentID++, handlerSide);
	}
	
	public void registerMessages()
	{
		registerMessage(new SoundUtils.MovingEntitySoundMessage.Handler(), SoundUtils.MovingEntitySoundMessage.class, Side.CLIENT);
		registerMessage(new MultiPartBreakMessage.Handler(), MultiPartBreakMessage.class, Side.SERVER);
		registerMessage(new MultiPartActivateMessage.Handler(), MultiPartActivateMessage.class, Side.SERVER);
		registerMessage(new MeganeuraUpdateMessage.Handler(), MeganeuraUpdateMessage.class, Side.CLIENT);
		registerMessage(new KnappingSlotMessage.Handler(), KnappingSlotMessage.class, Side.SERVER);
	}
	
	public void sendToAllAround(IMessage message, World world, double x, double y, double z, double range)
	{
		sendToAllAround(message, new TargetPoint(world.provider.getDimension(), x, y, z, range));
	}
	
	public void sendToAllAround(IMessage message, World world, Vec3d vec, double range)
	{
		sendToAllAround(message, world, vec.xCoord, vec.yCoord, vec.zCoord, range);
	}
	
	public void sendToAllTracking(IMessage message, Entity entity)
	{
		if (!entity.worldObj.isRemote)
		{
			Set<? extends EntityPlayer> players = ((WorldServer) entity.worldObj).getEntityTracker().getTrackingPlayers(entity);
			
			for (EntityPlayer player : players)
			{
				sendTo(message, (EntityPlayerMP) player);
			}
		}
		else
		{
			Genesis.logger.warn("Something attempted to send a message to other players from the client.", new Exception());
		}
	}
}
