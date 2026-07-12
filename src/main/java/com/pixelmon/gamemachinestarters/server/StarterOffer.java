package com.pixelmon.gamemachinestarters.server;

import java.util.ArrayList;
import java.util.List;

import com.pixelmon.gamemachinestarters.net.OfferSlot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** A player's persisted three-slot starter offer (server-side). */
public record StarterOffer(List<OfferSlot> slots) {

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (OfferSlot slot : slots) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putString("name", slot.name());
            slotTag.putString("ability", slot.ability());
            slotTag.putInt("tier", slot.tier());
            list.add(slotTag);
        }
        tag.put("slots", list);
        return tag;
    }

    public static StarterOffer fromNbt(CompoundTag tag) {
        List<OfferSlot> slots = new ArrayList<>();
        ListTag list = tag.getList("slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slotTag = list.getCompound(i);
            slots.add(new OfferSlot(
                slotTag.getString("name"),
                slotTag.getString("ability"),
                slotTag.getInt("tier")
            ));
        }
        return new StarterOffer(slots);
    }
}
