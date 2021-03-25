package dev.tr7zw.entityculling.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.tr7zw.entityculling.EntityCullingMod;
import dev.tr7zw.entityculling.access.Cullable;
import dev.tr7zw.entityculling.access.EntityRendererInter;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow
    private EntityRendererManager renderManager;

    @Inject(at = @At("HEAD"), method = "renderEntity", cancellable = true)
    private void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta,
            MatrixStack matrices, IRenderTypeBuffer vertexConsumers, CallbackInfo info) {
        Cullable cullable = (Cullable) entity;
        if (cullable.isForcedVisible()) {
            EntityCullingMod.instance.renderedEntities++;
            return;
        }
        if (cullable.isCulled()) {
            @SuppressWarnings("unchecked")
            EntityRenderer<Entity> entityRenderer = (EntityRenderer<Entity>) renderManager.getRenderer(entity);
            @SuppressWarnings("unchecked")
            EntityRendererInter<Entity> entityRendererInter = (EntityRendererInter<Entity>) entityRenderer;
            if (EntityCullingMod.instance.config.renderNametagsThroughWalls && matrices != null
                    && vertexConsumers != null && entityRendererInter.shadowHasLabel(entity)) {
                double x = MathHelper.lerp((double) tickDelta, (double) entity.lastTickPosX, (double) entity.getPosX())
                        - cameraX;
                double y = MathHelper.lerp((double) tickDelta, (double) entity.lastTickPosY, (double) entity.getPosY())
                        - cameraY;
                double z = MathHelper.lerp((double) tickDelta, (double) entity.lastTickPosZ, (double) entity.getPosZ())
                        - cameraZ;
                Vector3d Vector3d = entityRenderer.getRenderOffset(entity, tickDelta);
                double d = x + Vector3d.getX();
                double e = y + Vector3d.getY();
                double f = z + Vector3d.getZ();
                matrices.push();
                matrices.translate(d, e, f);
                entityRendererInter.shadowRenderLabelIfPresent(entity, entity.getDisplayName(), matrices,
                        vertexConsumers, this.renderManager.getPackedLight(entity, tickDelta));
                matrices.pop();
            }
            EntityCullingMod.instance.skippedEntities++;
            info.cancel();
            return;
        }
        EntityCullingMod.instance.renderedEntities++;
    }

}
