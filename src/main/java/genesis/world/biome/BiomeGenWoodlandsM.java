package genesis.world.biome;

import genesis.common.GenesisBlocks;
import genesis.world.biome.decorate.WorldGenRockBoulders;

public class BiomeGenWoodlandsM extends BiomeGenWoodlands
{
	public BiomeGenWoodlandsM(int id)
	{
		super(id);
		setBiomeName("Woodlands M");
		setHeight(0.4F, 0.7F);
	}
	
	protected void addDecorations()
	{
		addDecoration(new WorldGenRockBoulders().setWaterRequired(false).setMaxHeight(3).setRarity(4).setCountPerChunk(1));
		addDecoration(new WorldGenRockBoulders().setWaterRequired(false).setMaxHeight(3).setRarity(8).addBlocks(GenesisBlocks.rhyolite.getDefaultState()).setCountPerChunk(1));
		addDecoration(new WorldGenRockBoulders().setWaterRequired(false).setMaxHeight(3).setRarity(8).addBlocks(GenesisBlocks.dolerite.getDefaultState()).setCountPerChunk(1));
	}
	
	@Override
	public float getFogDensity()
	{
		return 0.75F;
	}
	
	@Override
	public float getNightFogModifier()
	{
		return 0.65F;
	}
}