package firis.mobbottle.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import firis.mobbottle.MobBottle;
import firis.mobbottle.block.entity.MobBottleBlockEntity;
import firis.mobbottle.block.entity.MobBottleBlockEntityClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

/***
 * モブボトルアイテム描画
 */
public class MobBottleBlockEntitySpecialModelRenderer implements SpecialModelRenderer<ItemStack> {

    //描画用のBlockEntity
    public MobBottleBlockEntity blockEntity = null;

    /***
     * 描画用パラメータ取得
     */
    @Override
    @Nullable
    public ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    /**
     * アイテム描画処理
     */
    @Override
    public void submit(@Nullable ItemStack stack, PoseStack pose, SubmitNodeCollector submitNodeCollector, int light, int overlay, boolean hasFoil, int outlineColor) {

        if (stack == null) return;

        //描画用情報設定
        if (blockEntity == null) {
            blockEntity = new MobBottleBlockEntity(BlockPos.ZERO, MobBottle.FirisBlocks.MOB_BOTTLE.defaultBlockState());
        }

        MobBottleBlockEntityClient blockEntityClient = this.blockEntity.getClient();

        blockEntityClient.setMobBottleDataFromBEWLR(stack);
        //描画用の方角を設定
        blockEntityClient.SetRendererDirection(Direction.WEST);

        //カメラの取得
        CameraRenderState camera = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState;

        //モブボトルブロック描画(BERの描画パイプラインを再利用)
        BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        MobBottleBlockEntityRenderer.MobBottleRenderState state = extractRenderState(dispatcher);

        pose.pushPose();
        if (state != null) {
            //アイテムパイプラインから渡されたライトを使用する
            //(extractRenderStateは原点(0,0,0)のワールドライトを計算するため、そのまま使うと
            // 昼夜・天候・ディメンション遷移で輝度が変化し、アイテムがチラついて見える)
            state.lightCoords = light;
            if (state.displayEntity != null) {
                state.displayEntity.lightCoords = light;
            }
            dispatcher.submit(state, pose, submitNodeCollector, camera);
        }
        pose.popPose();
    }

    /***
     * 描画用ステートの抽出
     * tryExtractRenderStateのhasLevel/shouldRender等のチェックでnullになる場合でも、
     * 瓶本体は描画できるように手動でステートを生成するフォールバックを持つ
     */
    private MobBottleBlockEntityRenderer.MobBottleRenderState extractRenderState(BlockEntityRenderDispatcher dispatcher) {

        MobBottleBlockEntityRenderer.MobBottleRenderState state = null;
        try {
            state = (MobBottleBlockEntityRenderer.MobBottleRenderState) dispatcher.tryExtractRenderState(this.blockEntity, 0.0f, null, false);
        } catch (Throwable ignored) {
            state = null;
        }

        //通常の抽出に失敗した場合は手動フォールバックで瓶だけ描画する
        if (state == null) {
            try {
                BlockEntityRenderer<?, ?> renderer0 = dispatcher.getRenderer(this.blockEntity);
                if (renderer0 instanceof MobBottleBlockEntityRenderer renderer) {
                    state = renderer.createRenderState();
                    renderer.extractRenderState(this.blockEntity, state, 0.0f, Vec3.ZERO, null);
                }
            } catch (Throwable ignored) {
                state = null;
            }
        }
        return state;
    }

    /***
     * 描画範囲の指定
     * 8頂点を指定する
     */
    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new org.joml.Vector3f(0, 0, 0));
        output.accept(new org.joml.Vector3f(1, 0, 0));
        output.accept(new org.joml.Vector3f(0, 1, 0));
        output.accept(new org.joml.Vector3f(1, 1, 0));
        output.accept(new org.joml.Vector3f(0, 0, 1));
        output.accept(new org.joml.Vector3f(1, 0, 1));
        output.accept(new org.joml.Vector3f(0, 1, 1));
        output.accept(new org.joml.Vector3f(1, 1, 1));
    }

    /***
     * 描画処理定義
     */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<ItemStack> {

        public static final MapCodec<MobBottleBlockEntitySpecialModelRenderer.Unbaked> MAP_CODEC =
                MapCodec.unit(new MobBottleBlockEntitySpecialModelRenderer.Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.BakingContext context) {
            return new MobBottleBlockEntitySpecialModelRenderer();
        }
    }
}