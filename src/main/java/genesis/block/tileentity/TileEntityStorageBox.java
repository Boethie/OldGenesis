package genesis.block.tileentity;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.PeekingIterator;

import genesis.common.sounds.GenesisSoundEvents;
import genesis.util.Constants.Unlocalized;
import genesis.util.FacingHelpers;
import genesis.util.GenesisMath;
import genesis.util.SimpleIterator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.util.Constants.NBT;

public class TileEntityStorageBox extends TileEntityInventoryLootBase implements ISidedInventory, ITickable, IInventory
{
	public static final AxisDirection MAIN_DIR = AxisDirection.NEGATIVE;
	
	public static final int SLOTS_W = 9;
	public static final int SLOTS_H = 3;
	
	public static final int USERS_UPDATE_ID = 1;
	public static final int OPEN_DIRECTION_UPDATE_ID = 2;
	
	protected final int[] inventorySlots;
	private Axis axis = null;
	private EnumMap<AxisDirection, Boolean> connections = new EnumMap<>(AxisDirection.class);
	
	//private int lastUsersCheck = 0;	// TODO: May need to implement regular checks for using players, like in the vanilla chest
	private int users = 0;
	
	protected EnumFacing openDirection = EnumFacing.NORTH;
	protected float prevOpenAnimation = 0;
	protected float openAnimation = 0;
	
	public TileEntityStorageBox()
	{
		super(SLOTS_W * SLOTS_H, true);
		
		inventorySlots = new int[inventory.length];
		for (int i = 0; i < inventorySlots.length; i++)
			inventorySlots[i] = i;
		
		for (AxisDirection dir : AxisDirection.values())
			connections.put(dir, false);
	}
	
	@Override
	public BlockStorageBox getBlockType()
	{
		return (BlockStorageBox) super.getBlockType();
	}
	
	public void sendUpdate()
	{
		if (worldObj != null)
		{
			//worldObj.markBlockForUpdate(pos);
			IBlockState state = worldObj.getBlockState(pos);
			worldObj.notifyBlockUpdate(pos, state, state, 0b1000);
		}
	}
	
	public Axis getAxis()
	{
		return axis;
	}
	
	public void setAxis(Axis axis)
	{
		this.axis = axis;
	}
	
	public void checkAxis()
	{
		if (!isConnected(AxisDirection.POSITIVE) && !isConnected(AxisDirection.NEGATIVE))
		{
			setAxis(null);
		}
	}
	
	public void setConnectedForced(AxisDirection axisDirection, boolean connected)
	{
		connections.put(axisDirection, connected);
	}
	
	public boolean setConnected(AxisDirection dir, boolean connected)
	{
		boolean ret = true;
		AxisDirection otherConnectDir = FacingHelpers.getOpposite(dir);
		TileEntityStorageBox connectBox = getBlockType().getTileEntity(worldObj, pos.offset(FacingHelpers.getFacing(getAxis(), dir)));
		
		if (connected)
		{
			if (connectBox != null && connectBox.getAxis() == null)
			{
				connectBox.setAxis(getAxis());
			}
			
			if (connectBox == null || getAxis() != connectBox.getAxis())
			{
				connectBox = null;
				ret = false;
			}
		}
		
		setConnectedForced(dir, connected);
		
		if (connectBox != null && !connectBox.isConnected(otherConnectDir) && !connectBox.setConnected(otherConnectDir, true))
		{
			setConnectedForced(dir, false);
			ret = false;
		}
		
		checkAxis();
		sendUpdate();
		return ret;
	}
	
	public boolean setConnected(EnumFacing side, boolean connected)
	{
		if (getAxis() == null || side.getAxis() == getAxis())
		{
			setAxis(side.getAxis());
			return setConnected(side.getAxisDirection(), connected);
		}
		
		return false;
	}
	
	public boolean isConnected(AxisDirection axisDirection)
	{
		return connections.get(axisDirection);
	}
	
	public boolean isConnected(EnumFacing side) {
		return side.getAxis() == getAxis() && isConnected(side.getAxisDirection());
	}
	
	public int getSlotsWidth()
	{
		return SLOTS_W;
	}
	
	public int getSlotsHeight()
	{
		return SLOTS_H;
	}
	
	public float getOpenAnimation(float partialTick)
	{
		TileEntityStorageBox box = getMainBox();
		return GenesisMath.lerp(box.prevOpenAnimation, box.openAnimation, partialTick);
	}
	
	@Override
	public boolean canRenderBreaking()
	{
		return true;
	}
	
	protected void setPrevOpenAnimation(float value)
	{
		value = MathHelper.clamp_float(value, 0, 1);
		
		for (TileEntityStorageBox box : iterableFromMainToEnd())
		{
			box.prevOpenAnimation = value;
		}
	}
	
	protected void setOpenAnimation(float value)
	{
		value = MathHelper.clamp_float(value, 0, 1);
		
		for (TileEntityStorageBox box : iterableFromMainToEnd())
		{
			box.openAnimation = value;
		}
	}
	
	@Override
	public void update()
	{
		if (!isConnected(MAIN_DIR))
		{
			final float epsilon = 0.001F;
			
			// Play sound for opening and closing.
			// Also contains debug code for looping animation.
			SoundEvent sound = null;
			
			if (openAnimation <= epsilon && users > 0)
			{
				sound = GenesisSoundEvents.BLOCK_STORAGE_BOX_OPEN;
				
				setUserCount(1, 1);
			}
			else if (openAnimation >= 1 - epsilon && users <= 0)
			{
				sound = GenesisSoundEvents.BLOCK_STORAGE_BOX_CLOSE;
				
				setUserCount(0, 0);
			}
			
			if (sound != null)
			{
				double centerX = 0;
				double centerY = 0;
				double centerZ = 0;
				int boxes = 0;
				
				for (TileEntityStorageBox box : iterableFromThisToEnd())	// Use this -> end because this box should be main.
				{
					centerX += box.getPos().getX();
					centerY += box.getPos().getY();
					centerZ += box.getPos().getZ();
					boxes++;
				}
				
				centerX /= boxes;
				centerY /= boxes;
				centerZ /= boxes;
				
				worldObj.playSound(centerX, centerY, centerZ,
						sound, SoundCategory.BLOCKS,
						1 + worldObj.rand.nextFloat() * 0.2F, 0.9F + worldObj.rand.nextFloat() * 0.1F, false);
			}
			
			float target = getUserCount() > 0 ? 1 : 0;
			float exagTarget = (target - 0.5F) * 1.5F + 0.5F;	// Makes the animation play a little faster and reach 0 or 1.
			
			setPrevOpenAnimation(openAnimation);
			setOpenAnimation(openAnimation + 0.06F / (exagTarget - openAnimation));
		}
	}
	
	public int getUserCount()
	{
		return getMainBox().users;
	}
	
	public EnumFacing getOpenDirection()
	{
		return getMainBox().openDirection;
	}
	
	public void sendUsersUpdate()
	{
		if (!worldObj.isRemote)
		{
			worldObj.addBlockEvent(pos, getBlockType(), USERS_UPDATE_ID, getUserCount());
		}
	}
	
	public void sendUsersUpdateAndNotify()
	{
		if (!worldObj.isRemote)
		{
			sendUsersUpdate();
			worldObj.notifyNeighborsOfStateChange(pos, getBlockType());
			worldObj.notifyNeighborsOfStateChange(pos.down(), getBlockType());
		}
	}
	
	public void sendOpenDirectionUpdate()
	{
		if (!worldObj.isRemote)
		{
			worldObj.addBlockEvent(pos, getBlockType(), OPEN_DIRECTION_UPDATE_ID, getOpenDirection().getHorizontalIndex());
		}
	}
	
	protected void setUserCount(int users, int min)
	{
		users = Math.max(users, min);
		
		for (TileEntityStorageBox box : iterableFromMainToEnd())
		{
			box.users = users;
		}
	}
	
	protected void setOpenDirection(EnumFacing value)
	{
		for (TileEntityStorageBox box : iterableFromMainToEnd())
		{
			box.openDirection = value;
		}
	}
	
	@Override
	public boolean receiveClientEvent(int id, int value)
	{
		switch (id)
		{
		case USERS_UPDATE_ID:
			setUserCount(value, 0);
			return true;
		case OPEN_DIRECTION_UPDATE_ID:
			setOpenDirection(EnumFacing.getHorizontal(value));
			return true;
		}
		
		return super.receiveClientEvent(id, value);
	}
	
	@Override
	public void openInventory(EntityPlayer player)
	{
		if (!player.isSpectator())
		{
			if (getUserCount() <= 0 && getOpenAnimation(1) <= 0.01)
			{
				if (getAxis() != null)
				{
					setOpenDirection(FacingHelpers.getFacingAlongAxis(FacingHelpers.rotateY(getAxis()), player.rotationYaw));
				}
				else
				{
					setOpenDirection(EnumFacing.fromAngle(player.rotationYaw));
				}
				
				sendOpenDirectionUpdate();
			}
			
			setUserCount(getUserCount() + 1, 1);
			sendUsersUpdateAndNotify();
		}
	}
	
	@Override
	public void closeInventory(EntityPlayer player)
	{
		if (!player.isSpectator())
		{
			setUserCount(getUserCount() - 1, 0);
			sendUsersUpdateAndNotify();
		}
	}
	
	@Override
	protected void writeVisualData(NBTTagCompound compound, boolean save)
	{
		super.writeVisualData(compound, save);
		
		Axis axis = getAxis();
		compound.setString("axis", axis == null ? "none" : axis.getName());
		
		NBTTagList connectionList = new NBTTagList();
		
		for (Map.Entry<AxisDirection, Boolean> entry : connections.entrySet())
		{
			NBTTagCompound connection = new NBTTagCompound();
			connection.setString("direction", entry.getKey().name().toLowerCase());
			connection.setBoolean("connected", entry.getValue());
			connectionList.appendTag(connection);
		}
		
		compound.setTag("connections", connectionList);
	}
	
	@Override
	protected void readVisualData(NBTTagCompound compound, boolean save)
	{
		super.readVisualData(compound, save);
		
		IBlockState oldState = null;
		
		if (worldObj != null)
			oldState = worldObj.getBlockState(pos);
		
		setAxis(FacingHelpers.getAxis(compound.getString("axis")));
		
		NBTTagList list = compound.getTagList("connections", NBT.TAG_COMPOUND);
		
		for (int i = 0; i < list.tagCount(); i++)
		{
			NBTTagCompound connection = list.getCompoundTagAt(i);
			setConnectedForced(FacingHelpers.getAxisDirection(connection.getString("direction")), connection.getBoolean("connected"));
		}
		
		if (worldObj != null)
		{
			IBlockState newState = worldObj.getBlockState(pos);
			worldObj.notifyBlockUpdate(pos, oldState, newState, 0b1000);
		}
	}
	
	@Override
	public boolean isUseableByPlayer(EntityPlayer player)
	{
		return true;
	}
	
	@Override
	public String getName()
	{
		return Unlocalized.CONTAINER_UI + "storageBox." + (getWidth() <= 1 ? "normal" : "large");
	}
	
	@Override
	public int[] getSlotsForFace(EnumFacing side)
	{
		return inventorySlots;
	}
	
	@Override
	public boolean canInsertItem(int slot, ItemStack stack, EnumFacing direction)
	{
		return true;
	}
	
	@Override
	public boolean canExtractItem(int slot, ItemStack stack, EnumFacing direction)
	{
		return true;
	}
	
	public static class BoxIterator extends SimpleIterator<TileEntityStorageBox>
	{
		private final TileEntityStorageBox start;
		private final EnumFacing direction;
		
		public BoxIterator(TileEntityStorageBox startBox, EnumFacing iterDirection)
		{
			super(false);
			
			start = startBox;
			direction = iterDirection;
		}
		
		@Override
		protected TileEntityStorageBox computeNext()
		{
			TileEntityStorageBox current = getCurrent();
			
			if (current == null)
			{
				return start;
			}
			
			TileEntityStorageBox next = direction == null || !current.isConnected(direction) ? null : current.getBlockType().getTileEntity(current.worldObj, current.getPos().offset(direction));
			
			return next != null && next.isConnected(direction.getOpposite()) ? next : null;
		}
	}
	
	protected EnumFacing getMainFacing()
	{
		return FacingHelpers.getFacing(getAxis(), MAIN_DIR);
	}
	
	protected EnumFacing getEndFacing()
	{
		EnumFacing mainFacing = getMainFacing();
		return mainFacing == null ? null : mainFacing.getOpposite();
	}
	
	public PeekingIterator<TileEntityStorageBox> iteratorFromThisToMain()
	{
		return new BoxIterator(this, getMainFacing());
	}
	
	public Iterable<TileEntityStorageBox> iterableFromThisToMain()
	{
		return this::iteratorFromThisToMain;
	}
	
	protected TileEntityStorageBox getMainBox()
	{
		TileEntityStorageBox boxOut = null;
		
		for (TileEntityStorageBox curBox : iterableFromThisToMain())
		{
			boxOut = curBox;
		}
		
		return boxOut;
	}
	
	public PeekingIterator<TileEntityStorageBox> iteratorFromThisToEnd()
	{
		return new BoxIterator(this, getEndFacing());
	}
	
	public Iterable<TileEntityStorageBox> iterableFromThisToEnd()
	{
		return this::iteratorFromThisToEnd;
	}
	
	public PeekingIterator<TileEntityStorageBox> iteratorOrderless()
	{
		final Iterator<TileEntityStorageBox> toMain = iteratorFromThisToMain();
		final Iterator<TileEntityStorageBox> toEnd = iteratorFromThisToEnd();
		toEnd.next();	// Remove duplicate center box iteration.
		
		return new SimpleIterator<TileEntityStorageBox>(false)
		{
			@Override
			protected TileEntityStorageBox computeNext()
			{
				if (toMain.hasNext())
				{
					return toMain.next();
				}
				else if (toEnd.hasNext())
				{
					return toEnd.next();
				}
				
				return null;
			}
		};
	}
	
	public int getWidth()
	{
		int width = 0;
		Iterator<TileEntityStorageBox> iter = iteratorOrderless();
		
		while (iter.hasNext())
		{
			width++;
			iter.next();
		}
		
		return width;
	}
	
	public PeekingIterator<TileEntityStorageBox> iteratorFromMainToEnd()
	{
		return new BoxIterator(getMainBox(), getEndFacing());
	}
	
	public Iterable<TileEntityStorageBox> iterableFromMainToEnd()
	{
		return this::iteratorFromMainToEnd;
	}
}
