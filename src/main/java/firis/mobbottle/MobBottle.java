package firis.mobbottle;

import com.mojang.logging.LogUtils;
import firis.mobbottle.block.MobBottleBlock;
import firis.mobbottle.block.MobBottleEmptyBlock;
import firis.mobbottle.block.entity.MobBottleBlockEntity;
import firis.mobbottle.component.MobBottleMobData;
import firis.mobbottle.item.MobBottleBlockItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;

import java.util.Set;

public class MobBottle implements ModInitializer {

    public static final String MODID = "mobbottle";

    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * ブロック参照用定義
     */
    public static class FirisBlocks {
        public static final Block MOB_BOTTLE = Registry.register(
                BuiltInRegistries.BLOCK,
                Identifier.fromNamespaceAndPath(MODID, "mob_bottle"),
                new MobBottleBlock(MobBottleBlock.PROPERTIES.setId(
                        ResourceKey.create(Registries.BLOCK,
                                Identifier.fromNamespaceAndPath(MODID, "mob_bottle")))));
        public static final Block MOB_BOTTLE_EMPTY = Registry.register(
                BuiltInRegistries.BLOCK,
                Identifier.fromNamespaceAndPath(MODID, "mob_bottle_empty"),
                new MobBottleEmptyBlock(MobBottleBlock.PROPERTIES.setId(
                        ResourceKey.create(Registries.BLOCK,
                                Identifier.fromNamespaceAndPath(MODID, "mob_bottle_empty")))));
    }

    /**
     * アイテム参照用定義
     */
    public static class FirisItems {
        public static final BlockItem MOB_BOTTLE = Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(MODID, "mob_bottle"),
                new MobBottleBlockItem(FirisBlocks.MOB_BOTTLE));
        public static final BlockItem MOB_BOTTLE_EMPTY =
                Registry.register(BuiltInRegistries.ITEM,
                        Identifier.fromNamespaceAndPath(MODID, "mob_bottle_empty"),
                        new BlockItem(FirisBlocks.MOB_BOTTLE_EMPTY, new net.minecraft.world.item.Item.Properties()
                                .setId(ResourceKey.create(Registries.ITEM,
                                        Identifier.fromNamespaceAndPath(MODID, "mob_bottle_empty")))
                                .useBlockDescriptionPrefix()));
    }

    /**
     * BlockEntityType参照用定義
     */
    public static class FirisBlockEntityType {
        public static final BlockEntityType<MobBottleBlockEntity> BLOCK_ENTITY_TYPE = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(MODID, "mob_bottle_be"),
                new BlockEntityType<>(MobBottleBlockEntity::new, Set.of(FirisBlocks.MOB_BOTTLE)));
    }

    /**
     * DataComponentType参照用定義
     */
    public static class FirisDataComponentType {
        public static final DataComponentType<MobBottleMobData> MOBBOTTLE_TYPE = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(MODID, "mob_data_type"),
                DataComponentType.<MobBottleMobData>builder()
                        .persistent(MobBottleMobData.CODEC)
                        .networkSynchronized(MobBottleMobData.STREAM_CODEC)
                        .build());
    }

    @Override
    public void onInitialize() {
        //Fabricではブロック・アイテム・BlockEntityType・DataComponentTypeはstatic初期化で登録する
        //ここでは関連オブジェクトの参照を確定させつつクリエイティブタブへ登録する
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            output.accept(FirisItems.MOB_BOTTLE_EMPTY);
            output.accept(FirisItems.MOB_BOTTLE);
        });

        //26.2ではEntityのinteract処理からItem#interactLivingEntityが呼ばれなくなったため
        //FabricのUseEntityCallbackで「生物を右クリックして捕獲」の処理を復元する
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            ItemStack handStack = player.getItemInHand(hand);
            if (handStack.getItem() instanceof MobBottleBlockItem mobBottleItem
                    && entity instanceof LivingEntity livingEntity) {
                return mobBottleItem.interactLivingEntity(handStack, player, livingEntity, hand);
            }
            return InteractionResult.PASS;
        });

        //static初期化を確実に実施する
        LOGGER.info("MobBottle registered: {} / {}", FirisItems.MOB_BOTTLE_EMPTY, FirisItems.MOB_BOTTLE);
    }
}