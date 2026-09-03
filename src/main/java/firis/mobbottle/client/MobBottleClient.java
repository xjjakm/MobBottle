package firis.mobbottle.client;

import firis.mobbottle.MobBottle;
import firis.mobbottle.client.renderer.MobBottleBlockEntityRenderer;
import firis.mobbottle.client.renderer.MobBottleBlockEntitySpecialModelRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;

public class MobBottleClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        //ブロック描画系登録
        BlockEntityRenderers.register(
                MobBottle.FirisBlockEntityType.BLOCK_ENTITY_TYPE,
                MobBottleBlockEntityRenderer::new);

        //アイテム描画イベント登録
        SpecialModelRenderers.ID_MAPPER.put(
                Identifier.fromNamespaceAndPath(MobBottle.MODID, "mobbottle_special"),
                MobBottleBlockEntitySpecialModelRenderer.Unbaked.MAP_CODEC);
    }
}