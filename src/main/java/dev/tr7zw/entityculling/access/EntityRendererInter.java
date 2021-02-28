package dev.tr7zw.entityculling.access;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;

public interface EntityRendererInter<T extends Entity> {

	boolean shadowHasLabel(T entity);

	void shadowRenderLabelIfPresent(T entity, ITextComponent text, MatrixStack matrices, IRenderTypeBuffer vertexConsumers,
			int light);

}