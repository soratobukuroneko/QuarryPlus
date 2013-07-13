package org.yogpstop.qp;

import java.util.LinkedList;
import java.util.logging.Level;

import buildcraft.BuildCraftBuilders;
import buildcraft.BuildCraftCore;
import buildcraft.BuildCraftFactory;
import buildcraft.BuildCraftSilicon;
import buildcraft.api.gates.ActionManager;
import buildcraft.api.gates.ITrigger;
import buildcraft.api.gates.ITriggerProvider;
import buildcraft.api.transport.IPipe;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.Property;

import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.FMLLog;

@Mod(modid = "QuarryPlus", name = "QuarryPlus", version = "@VERSION@", dependencies = "required-after:BuildCraft|Factory")
@NetworkMod(clientSideRequired = true, serverSideRequired = false, channels = { PacketHandler.BTN, PacketHandler.NBT, PacketHandler.OGUI, PacketHandler.Tile }, packetHandler = PacketHandler.class)
public class QuarryPlus implements ITriggerProvider {
	@SidedProxy(clientSide = "org.yogpstop.qp.client.ClientProxy", serverSide = "org.yogpstop.qp.CommonProxy")
	public static CommonProxy proxy;

	@Mod.Instance("QuarryPlus")
	public static QuarryPlus instance;

	public static Block blockQuarry, blockMarker, blockMover, blockMiningWell, blockPump;

	public static int RecipeDifficulty;

	public static final int guiIdContainerMiner = 1;
	public static final int guiIdContainerMover = 2;
	public static final int guiIdGuiQuarryFortuneList = 3;
	public static final int guiIdGuiQuarrySilktouchList = 4;

	@Mod.PreInit
	public static void preInit(FMLPreInitializationEvent event) {
		Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());
		try {
			cfg.load();
			blockQuarry = (new BlockQuarry(cfg.getBlock("Quarry", 4001).getInt()));
			blockMarker = (new BlockMarker(cfg.getBlock("Marker", 4002).getInt()));
			blockMover = (new BlockMover(cfg.getBlock("EnchantMover", 4003).getInt()));
			blockMiningWell = (new BlockMiningWell(cfg.getBlock("MiningWell", 4004).getInt()));
			blockPump = (new BlockPump(cfg.getBlock("Pump", 4005).getInt()));

			Property RD = cfg.get(Configuration.CATEGORY_GENERAL, "RecipeDifficulty", 2);
			RD.comment = "0:AsCheatRecipe,1:EasyRecipe,2:NormalRecipe(Default),3:HardRecipe,other:NormalRecipe";
			RecipeDifficulty = RD.getInt(2);
			TileQuarry.CE_BB = cfg.get(Configuration.CATEGORY_GENERAL + ".BreakBlock", "PowerCoefficientWithEfficiency", 1.3D).getDouble(1.3D);
			TileQuarry.BP_BB = cfg.get(Configuration.CATEGORY_GENERAL + ".BreakBlock", "BasePower", 40D).getDouble(40D);
			TileQuarry.CE_MF = cfg.get(Configuration.CATEGORY_GENERAL + ".MakeFrame", "PowerCoefficientWithEfficiency", 1.3D).getDouble(1.3D);
			TileQuarry.BP_MF = cfg.get(Configuration.CATEGORY_GENERAL + ".MakeFrame", "BasePower", 25D).getDouble(25D);
			TileQuarry.CE_MH = cfg.get(Configuration.CATEGORY_GENERAL + ".MoveHead", "PowerCoefficientWithEfficiency", 1.3D).getDouble(1.3D);
			TileQuarry.BP_MH = cfg.get(Configuration.CATEGORY_GENERAL + ".MoveHead", "BasePower", 200D).getDouble(200D);
			TileQuarry.CF = cfg.get(Configuration.CATEGORY_GENERAL + ".BreakBlock", "PowerCoefficientWithFortune", 1.3D).getDouble(1.3D);
			TileQuarry.CS = cfg.get(Configuration.CATEGORY_GENERAL + ".BreakBlock", "PowerCoefficientWithSilktouch", 2D).getDouble(2D);
			TileMiningWell.CE = cfg.get(Configuration.CATEGORY_GENERAL + ".MiningWell", "PowerCoefficientWithEfficiency", 1.3D).getDouble(1.3D);
			TileMiningWell.BP = cfg.get(Configuration.CATEGORY_GENERAL + ".MiningWell", "BasePower", 40D).getDouble(40D);
			TileMiningWell.CF = cfg.get(Configuration.CATEGORY_GENERAL + ".MiningWell", "PowerCoefficientWithFortune", 1.3D).getDouble(1.3D);
			TileMiningWell.CS = cfg.get(Configuration.CATEGORY_GENERAL + ".MiningWell", "PowerCoefficientWithSilktouch", 2D).getDouble(2D);
			TilePump.CE_R = cfg.get(Configuration.CATEGORY_GENERAL + ".Pump.RemoveLiquid", "PowerCoefficientWithEfficiency", 1.3D).getDouble(1.3D);
			TilePump.BP_R = cfg.get(Configuration.CATEGORY_GENERAL + ".Pump.RemoveLiquid", "BasePower", 10D).getDouble(10D);
			TilePump.CE_F = cfg.get(Configuration.CATEGORY_GENERAL + ".Pump.MakeFrame", "PowerCoefficientWithEfficiency", 1.3D).getDouble(1.3D);
			TilePump.BP_F = cfg.get(Configuration.CATEGORY_GENERAL + ".Pump.MakeFrame", "BasePower", 25D).getDouble(25D);
			cfg.getCategory(Configuration.CATEGORY_GENERAL)
					.setComment(
							"PowerCoefficientWith(EnchantName) is Coefficient with correspond enchant.\nWithEfficiency value comes reciprocal number.\nBasePower is basical using power with no enchants.");
		} catch (Exception e) {
			FMLLog.log(Level.SEVERE, e, "Error Massage");
		} finally {
			cfg.save();
		}
		LanguageRegistry.instance().loadLocalization("/org/yogpstop/qp/lang/en_US.lang", "en_US", false);
		LanguageRegistry.instance().loadLocalization("/org/yogpstop/qp/lang/ja_JP.lang", "ja_JP", false);
		ForgeChunkManager.setForcedChunkLoadingCallback(instance, new ChunkLoadingHandler());
	}

	@Mod.Init
	public void init(FMLInitializationEvent event) {
		GameRegistry.registerBlock(blockQuarry, "QuarryPlus");
		GameRegistry.registerBlock(blockMarker, "MarkerPlus");
		GameRegistry.registerBlock(blockMover, "EnchantMover");
		GameRegistry.registerBlock(blockMiningWell, "MiningWellPlus");
		GameRegistry.registerBlock(blockPump, "PumpPlus");

		GameRegistry.registerTileEntity(TileQuarry.class, "QuarryPlus");
		GameRegistry.registerTileEntity(TileMarker.class, "MarkerPlus");
		GameRegistry.registerTileEntity(TileMiningWell.class, "MiningWellPlus");
		GameRegistry.registerTileEntity(TilePump.class, "PumpPlus");

		ActionManager.registerTriggerProvider(this);

		switch (RecipeDifficulty) {
		case 0:
			GameRegistry.addRecipe(new ItemStack(blockMarker, 1),
					new Object[] { "X", "Y", Character.valueOf('Y'), BuildCraftBuilders.markerBlock, Character.valueOf('X'), Item.redstone });
			GameRegistry.addRecipe(new ItemStack(blockQuarry, 1),
					new Object[] { "X", "Y", Character.valueOf('Y'), BuildCraftFactory.quarryBlock, Character.valueOf('X'), Item.redstone });
			GameRegistry.addRecipe(new ItemStack(blockMover, 1), new Object[] { "X", "Y", Character.valueOf('Y'), BuildCraftFactory.autoWorkbenchBlock,
					Character.valueOf('X'), Item.redstone });
			GameRegistry.addRecipe(new ItemStack(blockMiningWell, 1), new Object[] { "X", "Y", Character.valueOf('Y'), BuildCraftFactory.miningWellBlock,
					Character.valueOf('X'), Item.redstone });
			break;
		case 1:
			GameRegistry.addRecipe(new ItemStack(blockMarker, 1),
					new Object[] { "X", "Y", Character.valueOf('Y'), BuildCraftBuilders.markerBlock, Character.valueOf('X'), Item.ingotGold });
			GameRegistry.addRecipe(
					new ItemStack(blockQuarry, 1),
					new Object[] { " X ", "DYD", Character.valueOf('Y'), BuildCraftFactory.quarryBlock, Character.valueOf('X'), Block.anvil,
							Character.valueOf('D'), new ItemStack(BuildCraftSilicon.redstoneChipset, 1, 3) });
			GameRegistry.addRecipe(new ItemStack(blockMover, 1), new Object[] { "X", "Y", "Z", Character.valueOf('Z'), BuildCraftFactory.autoWorkbenchBlock,
					Character.valueOf('Y'), Block.anvil, Character.valueOf('X'), Block.enchantmentTable });
			GameRegistry.addRecipe(new ItemStack(blockMiningWell, 1), new Object[] { "X", "Y", Character.valueOf('Y'), BuildCraftFactory.miningWellBlock,
					Character.valueOf('X'), new ItemStack(BuildCraftSilicon.redstoneChipset, 1, 3) });
			break;
		case 3:
			GameRegistry.addRecipe(new ItemStack(blockMarker, 1), new Object[] { "X", "Y", "Z", Character.valueOf('X'),
					new ItemStack(BuildCraftSilicon.redstoneChipset, 1, 2), Character.valueOf('Y'), BuildCraftBuilders.markerBlock, Character.valueOf('Z'),
					BuildCraftCore.diamondGearItem });
			GameRegistry.addRecipe(new ItemStack(blockQuarry, 1),
					new Object[] { "GBG", "CQC", "WAW", Character.valueOf('G'), BuildCraftCore.diamondGearItem, Character.valueOf('B'), Block.blockDiamond,
							Character.valueOf('C'), new ItemStack(BuildCraftSilicon.redstoneChipset, 1, 3), Character.valueOf('Q'),
							BuildCraftFactory.quarryBlock, Character.valueOf('W'), BuildCraftCore.wrenchItem, Character.valueOf('A'),
							BuildCraftFactory.autoWorkbenchBlock });
			GameRegistry
					.addRecipe(new ItemStack(blockMover, 1),
							new Object[] { "DED", "GAG", "OOO", Character.valueOf('D'), Block.blockDiamond, Character.valueOf('E'), Block.enchantmentTable,
									Character.valueOf('O'), Block.obsidian, Character.valueOf('A'), Block.anvil, Character.valueOf('G'),
									BuildCraftCore.diamondGearItem });
			GameRegistry.addRecipe(new ItemStack(blockMiningWell, 1), new Object[] { " X ", "XYX", " Z ", Character.valueOf('Y'),
					BuildCraftFactory.miningWellBlock, Character.valueOf('X'), new ItemStack(BuildCraftSilicon.redstoneChipset, 1, 3), Character.valueOf('Z'),
					Block.blockDiamond });
			break;
		default:
			GameRegistry.addRecipe(new ItemStack(blockMarker, 1), new Object[] { "X", "Y", Character.valueOf('X'),
					new ItemStack(BuildCraftSilicon.redstoneChipset, 1, 2), Character.valueOf('Y'), BuildCraftBuilders.markerBlock });
			GameRegistry.addRecipe(new ItemStack(blockQuarry, 1), new Object[] { "GDG", "IQI", "WAW", Character.valueOf('G'), BuildCraftCore.goldGearItem,
					Character.valueOf('D'), new ItemStack(BuildCraftSilicon.redstoneChipset, 1, 3), Character.valueOf('I'), BuildCraftCore.ironGearItem,
					Character.valueOf('Q'), BuildCraftFactory.quarryBlock, Character.valueOf('W'), BuildCraftCore.wrenchItem, Character.valueOf('A'),
					BuildCraftFactory.autoWorkbenchBlock });
			GameRegistry.addRecipe(new ItemStack(blockMover, 1), new Object[] { "DED", "OAO", "OOO", Character.valueOf('D'), BuildCraftCore.diamondGearItem,
					Character.valueOf('E'), Block.enchantmentTable, Character.valueOf('O'), Block.obsidian, Character.valueOf('A'), Block.anvil });
			GameRegistry.addRecipe(new ItemStack(blockMiningWell, 1), new Object[] { "X", "Y", "Z", Character.valueOf('Y'), BuildCraftFactory.miningWellBlock,
					Character.valueOf('X'), new ItemStack(BuildCraftSilicon.redstoneChipset, 1, 3), Character.valueOf('Z'),
					BuildCraftFactory.autoWorkbenchBlock });
		}
		NetworkRegistry.instance().registerGuiHandler(this, new GuiHandler());
		proxy.registerTextures();
	}

	public static String getname(short blockid, int meta) {
		StringBuffer sb = new StringBuffer();
		sb.append(blockid);
		if (meta != 0) {
			sb.append(":");
			sb.append(meta);
		}
		sb.append("  ");
		ItemStack cache = new ItemStack(blockid, 1, meta);
		if (cache.getItem() == null) {
			sb.append(StatCollector.translateToLocal("tof.nullblock"));
		} else if (cache.getDisplayName() == null) {
			sb.append(StatCollector.translateToLocal("tof.nullname"));
		} else {
			sb.append(cache.getDisplayName());
		}
		return sb.toString();
	}

	public static String getname(long data) {
		return getname((short) (data % 0x1000), (int) (data >> 12));
	}

	public static long data(short id, int meta) {
		return id | (meta << 12);
	}

	public static final boolean getBit(short value, byte pos) {
		return (value << (16 - pos) >>> (15)) == 0 ? false : true;
	}

	@Override
	public LinkedList<ITrigger> getPipeTriggers(IPipe pipe) {
		return null;
	}

	@Override
	public LinkedList<ITrigger> getNeighborTriggers(Block block, TileEntity tile) {
		LinkedList<ITrigger> res = new LinkedList<ITrigger>();
		if (tile instanceof TileBasic) {
			res.add(TileBasic.active);
			res.add(TileBasic.deactive);
		}
		return res;
	}
}