package io.github.kgriff0n.mixin;

import io.github.kgriff0n.IExplosiveEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.DamageTiltS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
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
                world.createExplosion(entity, entity.getX(), entity.getY(), entity.getZ(), ((LivingEntity) entity).getHealth() * 0.15f, World.ExplosionSourceType.MOB);
                remove(Entity.RemovalReason.DISCARDED);
            }
            fuseTime++;
        }
    }

    @Inject(at = @At("HEAD"), method = "writeCustomDataToNbt")
    private void writeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("Ignited", ignited);
        nbt.putInt("Fuse", fuseTime);
    }

    @Inject(at = @At("HEAD"), method = "readCustomDataFromNbt")
    private void readNbt(NbtCompound nbt, CallbackInfo ci) {
        ignited = nbt.getBoolean("Ignited", false);
        fuseTime = nbt.getInt("Fuse", 0);
    }
}
