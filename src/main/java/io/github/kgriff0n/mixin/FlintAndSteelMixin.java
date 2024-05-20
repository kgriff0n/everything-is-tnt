package io.github.kgriff0n.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelItem.class)
public class FlintAndSteelMixin {
	@Inject(at = @At("HEAD"), method = "useOnBlock", cancellable = true)
	private void init(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
		//TODO check dispensers, maybe null exception on context.getPlayer()
		if (!context.getPlayer().isSneaking()) {
			World world = context.getWorld();
			BlockPos pos = context.getBlockPos();
			BlockState blockState = world.getBlockState(pos);

			TntEntity blockTNT = new TntEntity(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, context.getPlayer());
			blockTNT.setBlockState(blockState);

			// remove block
			world.setBlockState(pos, Blocks.AIR.getDefaultState());

			// playsound & spawn
			world.playSound(context.getPlayer(), context.getPlayer().getBlockPos(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0f, 1.0f);
			context.getPlayer().getMainHandStack().damage(1, context.getPlayer(), LivingEntity.getSlotForHand(context.getHand()));
			world.spawnEntity(blockTNT);

			// cancel fire
			cir.setReturnValue(ActionResult.SUCCESS);
		}
	}
}