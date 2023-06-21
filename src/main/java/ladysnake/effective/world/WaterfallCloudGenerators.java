package ladysnake.effective.world;

import com.sammy.lodestone.systems.rendering.particle.Easing;
import com.sammy.lodestone.systems.rendering.particle.ParticleBuilders;
import com.sammy.lodestone.systems.rendering.particle.ParticleTextureSheets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ladysnake.effective.Effective;
import ladysnake.effective.EffectiveConfig;
import ladysnake.effective.EffectiveUtils;
import ladysnake.effective.sound.WaterfallSoundInstance;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WaterfallCloudGenerators {
	public static final List<Waterfall> generators = new ArrayList<>();
	public static final Object2IntMap<Waterfall> particlesToSpawn = new Object2IntOpenHashMap<>();
	private static World lastWorld = null;

	public static void addGenerator(FluidState state, BlockPos pos) {
		if (pos == null || !EffectiveConfig.cascades || state.getFluid() != Fluids.FLOWING_WATER || generators.contains(pos)) {
			return;
		}
		float waterfallHeight = getWaterfallHeight(MinecraftClient.getInstance().world, pos, state);
		if (waterfallHeight > 0) {
			synchronized (generators) {
				generators.removeIf(waterfall -> waterfall.blockPos().equals(pos));
				generators.add(new Waterfall(pos, waterfallHeight));
			}
		}
	}

	public static void tick() {
		MinecraftClient client = MinecraftClient.getInstance();
		World world = client.world;
		if (client.isPaused() || world == null) {
			return;
		}
		synchronized (generators) {
			if (world != lastWorld) {
				generators.clear();
				particlesToSpawn.clear();
				lastWorld = world;
			}
			tickParticles(world);
			if (world.getTime() % 3 != 0) {
				return;
			}
			generators.forEach(waterfall -> {
				if (waterfall == null) {
					return;
				}
				scheduleParticleTick(waterfall, 6);
				float distance = MathHelper.sqrt((float) client.player.getBlockPos().getSquaredDistance(waterfall.blockPos()));
				if (distance > EffectiveConfig.cascadeSoundDistanceBlocks || EffectiveConfig.cascadeSoundsVolumeMultiplier == 0 || EffectiveConfig.cascadeSoundDistanceBlocks == 0) {
					return;
				}
				if (world.random.nextInt(200) == 0) { // check for player visibility to avoid underground cascades being heard on the surface, but that shit don't work: && canSeeWaterfall(world, blockPos, MinecraftClient.getInstance().player)) {
					client.getSoundManager().play(WaterfallSoundInstance.ambient(Effective.AMBIENCE_WATERFALL, (1.2f + world.random.nextFloat() / 10f) + (1 - waterfall.height()), waterfall.blockPos(), EffectiveConfig.cascadeSoundDistanceBlocks), (int) (distance / 2));
				}
			});
			generators.removeIf(waterfall -> waterfall == null || getWaterfallHeight(world, waterfall.blockPos(), world.getFluidState(waterfall.blockPos())) <= 0);
		}
	}

	private static void tickParticles(World world) {
		for (Waterfall waterfall : particlesToSpawn.keySet()) {
			if (waterfall != null) {
				particlesToSpawn.computeInt(waterfall, (blockPos, integer) -> integer - 1);
				addWaterfallCloud(world, waterfall);
			}
		}
		particlesToSpawn.values().removeIf(integer -> integer < 0);
	}

	private static float getWaterfallHeight(BlockView world, BlockPos pos, FluidState fluidState) {
		if (!EffectiveConfig.cascades || fluidState.getFluid() != Fluids.FLOWING_WATER || world == null) {
			return 0f;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		if (Math.sqrt(pos.getSquaredDistance(client.player.getBlockPos())) > client.options.getViewDistance().get() * 32) {
			return 0f;
		}
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		if (world.getFluidState(mutable.set(pos, Direction.DOWN)).getFluid() != Fluids.WATER) {
			return 0f;
		}
		boolean foundAir = false;
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				if (x != 0 || z != 0) {
					BlockState blockState = world.getBlockState(mutable.set(pos.getX() + x, pos.getY(), pos.getZ() + z));
					if (blockState.isAir()) {
						foundAir = true;
						break;
					}
				}
			}
		}
		if (!foundAir) {
			return 0f;
		}
		if (Arrays.stream(Direction.values()).anyMatch(direction -> direction.getAxis() != Direction.Axis.Y && world.getFluidState(mutable.set(pos.getX() + direction.getOffsetX(), pos.getY() - 1, pos.getZ() + direction.getOffsetZ())).getFluid() == Fluids.WATER)) {
			return fluidState.getHeight();
		}
		return 0f;
	}

	public static void addWaterfallCloud(World world, Waterfall waterfall) {
		boolean isGlowingWater = EffectiveUtils.isGlowingWater(world, waterfall.blockPos());
		Color glowingWaterColor = EffectiveUtils.getGlowingWaterColor(world, waterfall.blockPos());
		Color white = new Color(0xFFFFFF);
		BlockPos blockPos = waterfall.blockPos();

		for (int i = 0; i < EffectiveConfig.cascadeCloudDensity; i++) {
			if (waterfall != null) {
				double offsetX = world.getRandom().nextGaussian() / 5f;
				double offsetZ = world.getRandom().nextGaussian() / 5f;

				ParticleBuilders.create(Effective.WATERFALL_CLOUD).setScale((1f + world.random.nextFloat()) * waterfall.height()).setColor(isGlowingWater ? glowingWaterColor : white, isGlowingWater ? glowingWaterColor : white).setLifetime(10).overrideRenderType(EffectiveUtils.isGlowingWater(world, blockPos) ? ParticleTextureSheets.TRANSPARENT : ParticleTextureSheet.PARTICLE_SHEET_OPAQUE).setMotion((world.getRandom().nextFloat() * waterfall.height()) / 4f * Math.signum(offsetX), (world.getRandom().nextFloat() * waterfall.height()) / 4f, (world.getRandom().nextFloat() * waterfall.height()) / 4f * Math.signum(offsetZ)).spawn(world, blockPos.getX() + .5 + offsetX, blockPos.getY() + world.getRandom().nextFloat(), blockPos.getZ() + .5 + offsetZ);
			}
		}

		if (waterfall.height() == 0.8888889f) {
			if ((world.random.nextFloat() * 100f) <= EffectiveConfig.cascadeMistDensity) {
				double offsetX = world.getRandom().nextGaussian() / 5f;
				double offsetZ = world.getRandom().nextGaussian() / 5f;

				ParticleBuilders.create(Effective.MIST).setSpin((world.random.nextFloat() - world.random.nextFloat()) / 20f).setScale(10f + world.random.nextFloat() * 5f).setAlpha(0f, 0.1f, 0f).setAlphaEasing(Easing.SINE_OUT).setLifetime(500).enableNoClip().overrideRenderType(ParticleTextureSheets.TRANSPARENT).setMotion(world.getRandom().nextFloat() / 15f * Math.signum(offsetX), world.getRandom().nextGaussian() / 25f, world.getRandom().nextFloat() / 15f * Math.signum(offsetZ)).spawn(world, blockPos.getX() + .5 + offsetX, blockPos.getY() + world.getRandom().nextFloat(), blockPos.getZ() + .5 + offsetZ);
			}
		}

	}

	public static void scheduleParticleTick(Waterfall waterfall, int ticks) {
		if (waterfall.blockPos() != null) {
			particlesToSpawn.put(waterfall, ticks);
		}
	}

	public static boolean canSeeWaterfall(World world, BlockPos waterfallPos, PlayerEntity player) {
		return world.raycast(new RaycastContext(new Vec3d(waterfallPos.getX(), waterfallPos.getY(), waterfallPos.getZ()), player.getCameraPosVec(MinecraftClient.getInstance().getTickDelta()), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player)).getBlockPos().equals(player.getBlockPos());
	}
}
