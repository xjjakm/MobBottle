package firis.mobbottle.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class FirisEntityHelper {

    /**
     * 描画専用Entityに仮のIDを割り当てるための共通カウンタ
     * (実EntityのIDと衝突しないよう負の値を使用)
     */
    private static final AtomicInteger RENDER_ENTITY_ID = new AtomicInteger();

    /**
     * EntityのCompoundTagからEntityを生成する
     */
    public static Entity createEntityFromTag(CompoundTag tag, Level level) {

        //nullチェック
        if (tag == null) return null;

        Entity entity = null;
        try {
            //Tag -> ValueInput
            ValueInput input = TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    level.registryAccess(),
                    tag
            );

            //EntityType取得
            Optional<EntityType<?>> optEntityType = EntityType.by(input);
            if (optEntityType.isEmpty()) {
                return null;
            }

            //Entity生成
            entity = optEntityType.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            if (entity != null) {
                //情報の上書き
                entity.load(input);
                //クライアント側でEntity#createした場合、Level#getNextEntityIdが0を返すためIDが割り当てられない
                //(実EntityはスパウンパケットでIDが設定されるが、描画専用Entityでは設定されない)
                //描画時にEntity#getIdを呼ぶとIllegalStateExceptionになるため、一意な仮IDを割り当てる
                assignRenderEntityId(entity);
            }

        } catch (Exception e) {
            entity = null;
        }
        return entity;
    }

    /**
     * 描画専用EntityにID未割り当て(0)の場合のみ仮IDを割り当てる
     */
    private static void assignRenderEntityId(Entity entity) {
        try {
            //既にIDが割り当てられている場合は何もしない
            entity.getId();
        } catch (IllegalStateException e) {
            entity.setId(-RENDER_ENTITY_ID.incrementAndGet());
        }
    }

    /**
     * EntityからCompoundTagを生成する
     */
    public static CompoundTag createTagFromEntity(Entity entity) {

        CompoundTag tag = new CompoundTag();

        if (entity != null) {
            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            entity.save(output);
            tag = output.buildResult();
        }
        return tag;
    }

}
