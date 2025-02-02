package ladysnake.effective.mixin.chest_bubbles;

import ladysnake.effective.EffectiveConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(EnderChestBlockEntity.class)
public class UnderwaterEnderChestRandomOpener {
	private static final HashMap<BlockPos, Integer> CHESTS_TIMERS = new HashMap<>();

	@Inject(method = "clientTick", at = @At("TAIL"))
	private static void clientTick(World world, BlockPos pos, BlockState state, EnderChestBlockEntity blockEntity, CallbackInfo ci) {
		if ((EffectiveConfig.underwaterChestsOpenRandomly == EffectiveConfig.ChestsOpenOptions.RANDOMLY
			|| (EffectiveConfig.underwaterChestsOpenRandomly == EffectiveConfig.ChestsOpenOptions.ON_SOUL_SAND && world.getBlockState(pos.offset(Direction.DOWN, 1)).isOf(Blocks.SOUL_SAND)))
			&& world.isWater(pos) && world.isWater(pos.offset(Direction.UP, 1))) {

			// tick down chest timers
			if (CHESTS_TIMERS.containsKey(pos)) {
				if (CHESTS_TIMERS.get(pos) > 0 && blockEntity.stateManager.getViewerCount() <= 0) {
					CHESTS_TIMERS.put(pos, CHESTS_TIMERS.get(pos) - 1);
				} else {
					world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.AMBIENT, 0.1f, 1.0f, false);
					blockEntity.lidAnimator.setOpen(false);
					CHESTS_TIMERS.remove(pos);
				}
			}

			// randomly open chests
			if (world.random.nextInt(200) == 0
				&& blockEntity.stateManager.getViewerCount() <= 0
				&& !CHESTS_TIMERS.containsKey(pos)) {
				CHESTS_TIMERS.put(pos, 100);
				world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_ENDER_CHEST_OPEN, SoundCategory.AMBIENT, 0.1f, 1.0f, false);
				blockEntity.lidAnimator.setOpen(true);
			}
		} else {
			// remove chests if config option not enabled
			if (CHESTS_TIMERS.containsKey(pos)) {
				blockEntity.lidAnimator.setOpen(false);
				CHESTS_TIMERS.remove(pos);
			}
		}
	}

}
