package genesis.block.tileentity;

import java.util.*;

import com.google.common.base.Optional;
import com.google.common.collect.*;

import genesis.client.GenesisClient;
import genesis.combo.*;
import genesis.combo.variant.IMetadata;
import genesis.util.ItemStackKey;

import net.minecraft.block.*;
import net.minecraft.block.properties.*;
import net.minecraft.block.state.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayerBlockPlacement;
import net.minecraft.tileentity.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.common.*;

import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.common.eventhandler.Event.Result;

public class BlockGenesisFlowerPot extends BlockFlowerPot
{
	public interface IFlowerPotPlant
	{
		int getColorMultiplier(ItemStack contents, IBlockAccess world, BlockPos pos);
	}
	
	public static class PropertyContents extends PropertyHelper<ItemStackKey>
	{
		protected final BiMap<ItemStackKey, String> values;
		
		public PropertyContents(String name, BiMap<ItemStackKey, String> values)
		{
			super(name, ItemStackKey.class);
			
			this.values = values;
		}
		
		@Override
		public Collection<ItemStackKey> getAllowedValues()
		{
			Set<ItemStackKey> keySet = values.keySet();
			return keySet;
		}
		
		@Override
		public String getName(ItemStackKey value)
		{
			return values.get(value);
		}

		@Override
		public Optional<ItemStackKey> parseValue(String value)
		{
			if (values.containsValue(value))
				return Optional.of(values.inverse().get(value));
			
			return Optional.absent();
		}
	}
	
	private static final Map<ItemStackKey, ItemStackKey> PAIR_MAP = Maps.newHashMap();
	
	/**
	 * Used to get the same exact instance of ItemStackKey for an ItemStack, because BlockStateContainer$StateImplementation doesn't handle new instances for default property values in withProperty.
	 */
	public static ItemStackKey getStackKey(ItemStack stack)
	{
		if (stack == null)
		{
			return null;
		}
		
		ItemStackKey newKey = new ItemStackKey(stack);
		
		if (PAIR_MAP.containsKey(newKey))
		{
			return PAIR_MAP.get(newKey);
		}
		
		PAIR_MAP.put(newKey, newKey);
		return newKey;
	}
	
	private static final BiMap<ItemStackKey, String> stacksToNames = HashBiMap.create();//new LinkedHashMap<ItemStackKey, String>();
	private static final Map<ItemStackKey, IFlowerPotPlant> stacksToCustoms = new HashMap<ItemStackKey, IFlowerPotPlant>();
	
	public static void registerPlantForPot(ItemStack stack, String name)
	{
		stacksToNames.put(getStackKey(stack), name);
	}
	
	public static String getPlantName(ItemStack stack)
	{
		return stacksToNames.get(getStackKey(stack));
	}
	
	public static boolean isPlantRegistered(ItemStack stack)
	{
		return getPlantName(stack) != null;
	}
	
	public static void registerPlantCustoms(ItemStack stack, IFlowerPotPlant customs)
	{
		stacksToCustoms.put(getStackKey(stack), customs);
	}
	
	public static IFlowerPotPlant getPlantCustoms(ItemStack stack)
	{
		return stacksToCustoms.get(getStackKey(stack));
	}
	
	public static <V extends IMetadata<V>> void registerPlantsForPot(VariantsOfTypesCombo<V> combo, ObjectType<?, ?> type, IFlowerPotPlant customs)
	{
		for (V variant : combo.getValidVariants(type))
		{
			ItemStack stack = combo.getStack(type, variant);
			registerPlantForPot(stack, type.getVariantName(variant));
			registerPlantCustoms(stack, customs);
		}
	}
	
	public static <V extends IMetadata<V>> void registerPlantsForPot(VariantsCombo<V, ?, ?> combo, IFlowerPotPlant customs)
	{
		registerPlantsForPot(combo, combo.getObjectType(), customs);
	}
	
	protected PropertyContents contentsProp;
	
	public BlockGenesisFlowerPot()
	{
		super();
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@Override
	public TileEntityGenesisFlowerPot createTileEntity(World world, IBlockState state)
	{
		return new TileEntityGenesisFlowerPot();
	}
	
	/**
	 * To be called after all plants for this pot have been registered.
	 */
	public void afterAllRegistered()
	{
		contentsProp = new PropertyContents("contents", stacksToNames);
		blockState = new BlockStateContainer(this, contentsProp);
		setDefaultState(blockState.getBaseState());
	}
	
	@Override
	public int getMetaFromState(IBlockState state)
	{
		return 0;
	}
	
	@Override
	public IBlockState getStateFromMeta(int metadata)
	{
		return getDefaultState();
	}
	
	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos)
	{
		TileEntityGenesisFlowerPot pot = getTileEntity(world, pos);
		
		if (pot != null)
		{
			ItemStackKey key = getStackKey(pot.getContents());
			
			if (stacksToNames.containsKey(key))
			{
				state = state.withProperty(contentsProp, key);
			}
		}
		
		return state;
	}
	
	@SubscribeEvent
	public void onBlockInteracted(PlayerInteractEvent event)
	{
		/* TODO: Figure out how this works with dual wielding.
		if (event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
		{
			return;
		}
		
		ItemStack stack = event.getEntityPlayer().getHeldItemMainhand();
		
		if (stack == null)
			return;
		
		World world = event.getWorld();
		BlockPos pos = event.getPos();
		
		IBlockState state = world.getBlockState(pos);
		
		if (state.getBlock() != Blocks.flower_pot)
			return;
		
		state = Blocks.flower_pot.getActualState(state, world, pos);
		EnumFlowerType contents = state.getValue(BlockFlowerPot.CONTENTS);
		
		if (contents != EnumFlowerType.EMPTY)
		{
			return;
		}
		
		if (isPlantRegistered(stack))
		{
			world.setBlockState(pos, getDefaultState());
			
			TileEntityGenesisFlowerPot pot = getTileEntity(world, pos);
			
			if (pot != null)
			{
				pot.setContents(stack);
				
				event.setUseBlock(Result.DENY);
				event.setUseItem(Result.DENY);
				
				EntityPlayer player = event.getEntityPlayer();
				
				if (world.isRemote)	// We must send a packet to the server telling it that the player right clicked or else it won't place the plant in the flower pot.
				{
					Minecraft mc = GenesisClient.getMC();
					EntityPlayerSP spPlayer = mc.thePlayer;
					
					if (spPlayer == player)
					{
						Vec3d hitVec = mc.objectMouseOver.hitVec;
						hitVec = hitVec.subtract(pos.getX(), pos.getY(), pos.getZ());
						Packet<?> packet = new CPacketPlayerBlockPlacement(pos, event.getFace().getIndex(), stack, (float) hitVec.xCoord, (float) hitVec.yCoord, (float) hitVec.zCoord);
						spPlayer.sendQueue.addToSendQueue(packet);
						
						event.setCanceled(true);
					}
				}
				
				if (!player.capabilities.isCreativeMode)
				{
					stack.stackSize--;
				}
			}
			else
			{
				world.setBlockState(pos, state);
			}
		}*/
	}
	
	public static TileEntityGenesisFlowerPot getTileEntity(IBlockAccess world, BlockPos pos)
	{
		TileEntity te = world.getTileEntity(pos);
		
		if (te instanceof TileEntityGenesisFlowerPot)
		{
			return (TileEntityGenesisFlowerPot) te;
		}
		
		return null;
	}
}
