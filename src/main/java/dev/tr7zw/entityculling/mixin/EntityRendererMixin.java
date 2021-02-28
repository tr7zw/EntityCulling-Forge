package dev.tr7zw.entityculling.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.tr7zw.entityculling.access.EntityRendererInter;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> implements EntityRendererInter<T> {

	@Override
	public boolean shadowHasLabel(T entity) {
		return canRenderName(entity);
	}

	@Override
	public void shadowRenderLabelIfPresent(T entity, ITextComponent text, MatrixStack matrices,
			IRenderTypeBuffer vertexConsumers, int light) {
		renderName(entity, text, matrices, vertexConsumers, light);
	}

	@Shadow
	public abstract boolean canRenderName(T entity);

	@Shadow
	public abstract void renderName(T entityIn, ITextComponent displayNameIn, MatrixStack matrixStackIn,
			IRenderTypeBuffer bufferIn, int packedLightIn);

}
