package ladysnake.effective.mixin.water.integration.sodium;

import com.mojang.blaze3d.vertex.VertexConsumer;
import ladysnake.effective.world.WaterfallCloudGenerators;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidRenderer.class)
public class SodiumWaterfallGeneratorAdder {
	@Inject(method = "render", at = @At("HEAD"))
	public void effective$generateWaterfall(BlockRenderView world, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState state, CallbackInfo ci) {
		WaterfallCloudGenerators.addGenerator(state, pos.toImmutable());
	}
}
