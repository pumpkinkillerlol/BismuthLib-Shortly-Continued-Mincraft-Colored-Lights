package ru.paulevs.bismuthlib.mixin;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import org.joml.Matrix4f;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.paulevs.bismuthlib.BismuthLibClient;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	@Shadow private @Nullable ClientLevel level;
	
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void cf_onRenderLevel(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo info) {
		if (this.level != null) {
			BismuthLibClient.update(
				this.level,
				Mth.floor(camera.getPosition().x / 16.0),
				Mth.floor(camera.getPosition().y / 16.0),
				Mth.floor(camera.getPosition().z / 16.0)
			);
			BismuthLibClient.bindWithUniforms();
		}
	}
	
	@Inject(method = "renderSectionLayer", at = @At(
		value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/CompiledShaderProgram;apply()V",
		shift = Shift.BEFORE
	))
	private void cf_onRenderSectionLayer(RenderType renderType, double d, double e, double f, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo info) {
		BismuthLibClient.bindWithUniforms();
	}
}
