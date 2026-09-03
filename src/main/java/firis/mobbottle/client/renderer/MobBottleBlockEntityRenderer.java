package firis.mobbottle.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import firis.mobbottle.block.entity.MobBottleBlockEntity;
import firis.mobbottle.block.entity.MobBottleBlockEntityClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/***
 * モブボトルブロック描画
 */
public class MobBottleBlockEntityRenderer implements BlockEntityRenderer<MobBottleBlockEntity, MobBottleBlockEntityRenderer.MobBottleRenderState> {

    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

    protected final EntityRenderDispatcher entityRenderer;
    protected final BlockModelResolver blockModelResolver;

    public MobBottleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
        this.blockModelResolver = context.blockModelResolver();
    }

    /***
     * 描画用ステート
     */
    public static class MobBottleRenderState extends BlockEntityRenderState {
        //描画用Entityステート
        @Nullable
        public EntityRenderState displayEntity = null;
        //ブロックの向き
        public Direction direction = Direction.NORTH;
        //モブサイズ
        public float scale = 0.35F;
        //モブのY軸設定
        public float positionY = 0.0F;
        //外装ブロック用モデルステート
        public final BlockModelRenderState blockModel = new BlockModelRenderState();
    }

    /***
     * 描画用状態の生成
     */
    @Override
    public MobBottleRenderState createRenderState() {
        return new MobBottleRenderState();
    }

    /***
     * アイテム描画(SpecialModelRenderer)からも呼ばれるため、
     * ダミーのBlockEntity(BlockPos.ZERO)とカメラ距離による除外判定は行わない
     */
    @Override
    public boolean shouldRender(MobBottleBlockEntity blockEntity, net.minecraft.world.phys.Vec3 cameraPosition) {
        return true;
    }

    /***
     * BlockEntityから描画用情報を抽出する
     */
    @Override
    public void extractRenderState(
            MobBottleBlockEntity blockEntity,
            MobBottleRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        MobBottleBlockEntityClient blockEntityClient = blockEntity.getClient();

        //描画用Entityの抽出
        Entity entity = blockEntityClient.getRenderEntity();
        if (entity != null) {
            //描画専用Entityはtickされないため、partialTicksに依存する補間
            //(ageInTicks、頭/体の回転 lerp、位置 lerp)が毎秒値として折り返し、
            //モデルが微振動(ジッター)して見える。常にpartialTicks=0で抽出して完全静止させる
            try {
                state.displayEntity = this.entityRenderer.extractEntity(entity, 0.0F);
            } catch (Throwable t) {
                //稀な抽出失敗(ヘッドアイテムのモデル解決不備など)でも瓶本体は描画を継続する
                state.displayEntity = null;
            }
            if (state.displayEntity != null) {
                state.displayEntity.lightCoords = state.lightCoords;
                //確実に静止させるためアニメーションタイマーも固定する
                state.displayEntity.ageInTicks = 0.0F;
                //瓶の内側で影が描かれると影の揺らぎ・ちらつきで生物が震えているように見えるため除去する
                state.displayEntity.shadowPieces.clear();
                state.displayEntity.shadowRadius = 0.0F;
            }
        }

        state.direction = blockEntityClient.getRenderDirection();
        state.scale = blockEntityClient.getRenderScale();
        state.positionY = blockEntityClient.getRenderPositionY();

        //外装ブロックモデルの抽出
        this.blockModelResolver.update(state.blockModel, blockEntityClient.getRenderBlockState(), BLOCK_DISPLAY_CONTEXT);
    }

    /***
     * モブボトル描画処理
     */
    @Override
    public void submit(MobBottleRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        submitMobBottle(this.entityRenderer, state, poseStack, submitNodeCollector, camera, OverlayTexture.NO_OVERLAY, 0);
    }

    /***
     * モブボトル共通描画処理
     * アイテム描画(SpecialModelRenderer)からも呼び出される
     */
    public static void submitMobBottle(
            EntityRenderDispatcher entityRenderer,
            MobBottleRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera,
            int overlayCoords,
            int outlineColor
    ) {
        //Mobの描画
        if (state.displayEntity != null) {
            poseStack.pushPose();

            float scale = state.scale;
            float positionY = state.positionY;

            //位置とサイズと方角を設定
            poseStack.translate(0.5d, positionY, 0.5d);
            poseStack.scale(scale, scale, scale);

            Quaternionf quaternion = state.direction.getRotation();
            quaternion.mul(new Quaternionf().fromAxisAngleDeg(1, 0, 0, -90f));
            quaternion.mul(new Quaternionf().fromAxisAngleDeg(0, 1, 0, 180f));
            poseStack.mulPose(quaternion);

            entityRenderer.submit(state.displayEntity, camera, 0.0d, 0.0d, 0.0d, poseStack, submitNodeCollector);

            poseStack.popPose();
        }

        //ブロックの描画
        poseStack.pushPose();
        state.blockModel.submit(poseStack, submitNodeCollector, state.lightCoords, overlayCoords, outlineColor);
        poseStack.popPose();
    }
}