package net.hyper_pigeon.musicalallays.mixin;

import net.hyper_pigeon.musicalallays.client.sound.MovingJukeboxSoundInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AllayBrain;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AllayEntity.class)
public abstract class AllayEntityMixin extends PathAwareEntity {

    private MovingJukeboxSoundInstance song;

    @Shadow
    protected abstract void decrementStackUnlessInCreative(PlayerEntity player, ItemStack stack);


    protected AllayEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "interactMob",at = @At("HEAD"), cancellable = true)
    private void insertOrRemoveDisc(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack itemStack = player.getStackInHand(hand);
        ItemStack itemStack2 = this.getStackInHand(Hand.MAIN_HAND);
        if (itemStack.isEmpty() && itemStack2.getItem() == Items.JUKEBOX) {
            boolean bl = itemStack2.getOrCreateNbt().contains("RecordItem");
            if (bl) {
                if(!world.isClient()) {
                    ItemStack recordItemStack = ItemStack.fromNbt(itemStack2.getOrCreateNbt().getCompound("RecordItem"));
                    this.swingHand(Hand.MAIN_HAND);
                    player.giveItemStack(recordItemStack);
                    itemStack2.getOrCreateNbt().remove("RecordItem");
                }
                else {
                    this.world.playSoundFromEntity(player, this, SoundEvents.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.NEUTRAL, 2.0F, 1.0F);
                    MinecraftClient.getInstance().getSoundManager().stop(song);
                }
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
        else if (itemStack.getItem() instanceof MusicDiscItem && itemStack2.getItem() == Items.JUKEBOX) {
            boolean bl = itemStack2.getOrCreateNbt().contains("RecordItem");
            if (!bl) {

                MusicDiscItem musicDiscItem = (MusicDiscItem) itemStack.getItem();
                itemStack2.getOrCreateNbt().put("RecordItem", itemStack.writeNbt(new NbtCompound()));
                this.decrementStackUnlessInCreative(player, itemStack);

                song = new MovingJukeboxSoundInstance((AllayEntity) (Object)this,musicDiscItem.getSound());


                if(world.isClient()) {
                    this.world.playSoundFromEntity(player, this, SoundEvents.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.NEUTRAL, 2.0F, 1.0F);
                    MinecraftClient.getInstance().inGameHud.setRecordPlayingOverlay(musicDiscItem.getDescription());
                    MinecraftClient.getInstance().getSoundManager().playNextTick(song);
                }

                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }

    public boolean canTeleport(){
        return this.getMainHandStack().getItem().equals(Items.JUKEBOX) && distanceFromLikedPlayer((AllayEntity)(Object)this) > 64 && !this.isLeashed();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void teleportToLikedPlayer(CallbackInfo ci){
        if (canTeleport()){
            ServerPlayerEntity likedPlayer = AllayBrain.getLikedPlayer((AllayEntity)(Object)(this)).get();
            this.teleport(likedPlayer.getX(),likedPlayer.getY(),likedPlayer.getZ());
        }
    }

    @Inject(method="damage", at = @At("HEAD"), cancellable = true)
    public void immuneToDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir){
        if(this.getMainHandStack().getItem().equals(Items.JUKEBOX)){
            cir.setReturnValue(false);
        }
    }

    public double distanceFromLikedPlayer(AllayEntity allayEntity){
        if(AllayBrain.getLikedPlayer((AllayEntity)(Object)(this)).isPresent()){
            return allayEntity.getPos().distanceTo(AllayBrain.getLikedPlayer((AllayEntity)(Object)(this)).get().getPos());
        }
        return 0;
    }

}
