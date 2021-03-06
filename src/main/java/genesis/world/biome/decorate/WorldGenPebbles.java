package genesis.world.biome.decorate;

import genesis.block.BlockPebble;
import genesis.combo.ToolItems;
import genesis.combo.variant.EnumToolMaterial;
import genesis.common.GenesisItems;
import genesis.util.WorldUtils;
import genesis.util.functional.WorldBlockMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableList;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WorldGenPebbles extends WorldGenDecorationBase
{
	public List<EnumToolMaterial> pebbleTypes = new ArrayList<>();
	
	protected List<PropertyBool> pebbleProperties = ImmutableList.of(BlockPebble.NE, BlockPebble.SE, BlockPebble.SW, BlockPebble.NW);
	private boolean waterRequired = true;
	
	public WorldGenPebbles()
	{
		super(WorldBlockMatcher.STANDARD_AIR, WorldBlockMatcher.SOLID_TOP);
		
		pebbleTypes.add(EnumToolMaterial.DOLERITE);
		pebbleTypes.add(EnumToolMaterial.RHYOLITE);
		pebbleTypes.add(EnumToolMaterial.GRANITE);
		pebbleTypes.add(EnumToolMaterial.QUARTZ);
		pebbleTypes.add(EnumToolMaterial.BROWN_FLINT);
		pebbleTypes.add(EnumToolMaterial.BLACK_FLINT);
	}
	
	@Override
	public boolean place(World world, Random rand, BlockPos pos)
	{
		IBlockState state = world.getBlockState(pos);
		
		if (!state.getBlock().isAir(state, world, pos))
			return false;
		
		if (waterRequired && !WorldUtils.waterInRange(world, pos, 4, 3, 4))
			return false;
		
		int maxPebbles = 1 + rand.nextInt(3);
		IBlockState pebble = GenesisItems.TOOLS.getBlockState(ToolItems.PEBBLE, pebbleTypes.get(rand.nextInt(pebbleTypes.size())));
		List<PropertyBool> pebbles = new ArrayList<>(pebbleProperties);
		
		for (int i = 1; i <= maxPebbles; ++i)
		{
			pebble = pebble.withProperty(getPosition(pebbles, rand), true);
		}
		
		setAirBlock(world, pos, pebble);
		
		return true;
	}
	
	public WorldGenPebbles setWaterRequired(boolean required)
	{
		waterRequired = required;
		return this;
	}
	
	protected PropertyBool getPosition(List<PropertyBool> pebbles, Random rand)
	{
		int index = rand.nextInt(pebbles.size());
		PropertyBool pos = pebbles.get(index);
		pebbles.remove(index);
		return pos;
	}
}
