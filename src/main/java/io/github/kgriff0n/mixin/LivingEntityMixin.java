package io.github.kgriff0n.mixin;

import io.github.kgriff0n.IExplosiveEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements IExplosiveEntity {

    @Shadow public abstract void remove(Entity.RemovalReason reason);

    @Shadow public abstract @Nullable EntityAttributeInstance getAttributeInstance(RegistryEntry<EntityAttribute> attribute);

    @Unique
    private boolean ignited;
    @Unique
    private int fuseTime;

    @Override
    public void everything_is_tnt$setIgnited() {
        ignited = true;
        if ((LivingEntity) (Object) this instanceof MobEntity mob) {
            mob.setAiDisabled(true);
        }
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void tick(CallbackInfo ci) {

        if (ignited) {
            EntityAttributeInstance scale = this.getAttributeInstance(EntityAttributes.SCALE);
            scale.setBaseValue(scale.getValue() * 1.01);

            Entity entity = (Entity) (Object) this;
            World world = entity.getWorld();

            if (fuseTime % 15 == 0) {
                List<ServerPlayerEntity> players = world.getEntitiesByClass(
                        ServerPlayerEntity.class,
                        entity.getBoundingBox().expand(30),
                        player -> true
                );
                for (ServerPlayerEntity player : players) {
                    player.networkHandler.sendPacket(new DamageTiltS2CPacket((LivingEntity) entity));
                }
            }

            if (fuseTime == 30) {
                fuseTime = 0;
                ignited = false;
                world.createExplosion(entity, entity.getX(), entity.getY(), entity.getZ(), ((LivingEntity) entity).getHealth() * 0.15f, World.ExplosionSourceType.MOB);
                if (entity instanceof ServerPlayerEntity) {
                    entity.kill((ServerWorld) world);
                    scale.setBaseValue(1);
                } else {
                    remove(Entity.RemovalReason.DISCARDED);
                }
            }
            fuseTime++;
        }
    }

    @Inject(at = @At("HEAD"), method = "writeCustomData")
    private void writeData(WriteView view, CallbackInfo ci) {
        view.putBoolean("Ignited", ignited);
        view.putInt("Fuse", fuseTime);
    }

    @Inject(at = @At("HEAD"), method = "readCustomData")
    private void readData(ReadView view, CallbackInfo ci) {
        ignited = view.getBoolean("Ignited", false);
        fuseTime = view.getInt("Fuse", 0);
    }
}
