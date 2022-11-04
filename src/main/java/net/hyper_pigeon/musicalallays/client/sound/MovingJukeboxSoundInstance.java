package net.hyper_pigeon.musicalallays.client.sound;

import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

public class MovingJukeboxSoundInstance extends MovingSoundInstance {
    private final AllayEntity allayEntity;

    public MovingJukeboxSoundInstance(AllayEntity allayEntity, SoundEvent musicDiskSoundEvent) {
        super(musicDiskSoundEvent, SoundCategory.RECORDS, allayEntity.getRandom());
        this.allayEntity = allayEntity;
        this.repeat = true;
        this.volume = 4.0f;
    }

    @Override
    public void tick() {
        this.x = allayEntity.getX();
        this.y = allayEntity.getY();
        this.z = allayEntity.getZ();
    }
}
