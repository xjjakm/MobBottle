package firis.mobbottle;

import com.mojang.logging.LogUtils;
import firis.mobbottle.block.MobBottleBlock;
import firis.mobbottle.block.MobBottleEmptyBlock;
import firis.mobbottle.block.entity.MobBottleBlockEntity;
import firis.mobbottle.component.MobBottleMobData;
import firis.mobbottle.item.MobBottleBlockItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
                                .useBlockDescriptionPrefix()) {
                            @SuppressWarnings("deprecation")
                            @Override
                            public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                                        TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
                                tooltipAdder.accept(Component.translatable("info.mobbottle.mob_bottle_empty").withStyle(ChatFormatting.GRAY));
                            }
                        });
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
        //専用のクリエイティブタブ(生物展示瓶)を追加する
        Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(MODID, "mobbottle"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .icon(() -> new ItemStack(FirisItems.MOB_BOTTLE))
                        .title(Component.translatable("itemGroup.mobbottle"))
                        .displayItems((parameters, output) -> {
                            output.accept(FirisItems.MOB_BOTTLE);
                            output.accept(FirisItems.MOB_BOTTLE_EMPTY);
                        })
                        .build());
        //従来通り「道具とユーティリティ」タブにも両方の瓶を残す
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

        //26.xでは「獲得済みの材料を手にしていてもレシピが自動解放されない」(初回入手/初回クラフトのみ解放)ため
        //バックパックに材料が揃っている場合にレシピを自動解放するイベントを登録する
        registerRecipeAutoUnlock();
    }

    /**
     * 手持ちアイテムでレシピ条件が揃った場合にレシピを自動解放する
     * <p>GetRecipeIdsを利用したバニラの「材料入手で解放」をサーバー側で代替実装する。
     * アイテムを拾うたびに走るような専用イベントはFabric APIに存在しないため、
     * ServerTickEventsで1秒毎にバックパックを検査する(軽量、解放済みレシピは再送されない)。
     */
    private static void registerRecipeAutoUnlock() {
        //レシピの材料タグ(data/mobbottle/recipe と同一の物を使用する)
        TagKey<Item> glassIngredient = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "glass_blocks/cheap"));
        TagKey<Item> gemsIngredient = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "gems"));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            //1秒毎に1回だけ走らせる
            if (server.getTickCount() % 20 != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                Inventory inv = player.getInventory();
                List<ResourceKey<Recipe<?>>> toUnlock = new ArrayList<>();

                //材料(木のハーフブロック/ガラス/宝石)をいずれか1つでも持っていたら「生物展示瓶」のレシピを解放(バニラの「材料入手で解放」に近い挙動)
                if (inv.contains(ItemTags.WOODEN_SLABS) || inv.contains(glassIngredient) || inv.contains(gemsIngredient)) {
                    toUnlock.add(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(MODID, "mob_bottle")));
                }
                //瓶を持っていたら「瓶→空瓶」変換レシピを解放
                if (inv.contains(stack -> stack.is(FirisItems.MOB_BOTTLE))) {
                    toUnlock.add(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(MODID, "mob_bottle_1")));
                }
                //空瓶を持っていたら「空瓶→瓶」変換レシピを解放
                if (inv.contains(stack -> stack.is(FirisItems.MOB_BOTTLE_EMPTY))) {
                    toUnlock.add(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(MODID, "mob_bottle_2")));
                }

                if (!toUnlock.isEmpty()) {
                    player.awardRecipesByKey(toUnlock);
                }
            }
        });
    }
}