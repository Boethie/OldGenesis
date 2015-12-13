package genesis.item;

import java.util.*;

import genesis.common.GenesisCreativeTabs;
import genesis.metadata.IFood;
import genesis.metadata.IMetadata;
import genesis.metadata.VariantsOfTypesCombo;
import genesis.metadata.VariantsOfTypesCombo.ObjectType;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemGenesisFood<V extends IMetadata<V>> extends ItemFood
{
	public final VariantsOfTypesCombo<V> owner;
	
	protected final List<V> variants;
	protected final ObjectType<Block, ? extends ItemDish> type;
	
	public ItemGenesisFood(VariantsOfTypesCombo<V> owner, ObjectType<Block, ? extends ItemDish> type, List<V> variants, Class<V> variantClass)
	{
		super(0, 0, false);
		
		this.owner = owner;
		this.type = type;
		this.variants = variants;
		
		setCreativeTab(GenesisCreativeTabs.FOOD);
		setHasSubtypes(true);
	}
	
	@Override
	public String getUnlocalizedName(ItemStack stack)
	{
		return owner.getUnlocalizedName(stack, super.getUnlocalizedName(stack));
	}
	
	@Override
	public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems)
	{
		owner.fillSubItems(type, variants, subItems);
	}
	
	@Override
	public int getHealAmount(ItemStack stack)
	{
		return ((IFood) owner.getVariant(stack)).getFoodAmount();
	}
	
	@Override
	public float getSaturationModifier(ItemStack stack)
	{
		return ((IFood) owner.getVariant(stack)).getSaturationModifier();
	}
	
	@Override
	protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player)
	{
		if (!world.isRemote)
		{
			/*for (Pair<Float, PotionEffect> entry : effects)
			{
				if ((entry.getValue().getPotionID() > 0) && (world.rand.nextFloat() < entry.getKey()))
				{
					// Defensive copying
					PotionEffect effect = entry.getValue();
					player.addPotionEffect(new PotionEffect(effect.getPotionID(), effect.getDuration(), effect.getAmplifier()));
				}
			}*/
		}
	}
}
