package com.wassuples.demonslayer.missions;

import com.wassuples.demonslayer.ranking.Rank;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

/**
 * Immutable representation of a Demon Slayer Corps mission.
 *
 * <p>Missions are loaded from JSON datapacks via {@link MissionReloadListener} and stored
 * in {@link MissionRegistry}. Each mission has objectives to complete, rewards to grant,
 * and a minimum {@link Rank} requirement.
 *
 * <p>Supports network serialization via {@link net.minecraft.network.FriendlyByteBuf} for
 * client-side display in the Crow Dispatch screen.
 *
 * @see MissionRegistry
 * @see MissionObjective
 * @see MissionReward
 */
public record Mission(
    ResourceLocation id,
    MissionType type,
    Component title,
    List<Component> description,
    List<MissionObjective> objectives,
    List<MissionReward> rewards,
    Rank requirement
) {
    public Mission(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), buf.readEnum(MissionType.class), buf.readComponent(), buf.readList(FriendlyByteBuf::readComponent),
                buf.readList(ObjectiveType::fromBytes), buf.readList(RewardType::fromBytes), buf.readEnum(Rank.class));
    }

    public static Mission fromDefinition(MissionDefinition definition) {
        return new Mission(definition.id(), definition.type(), definition.title(), definition.description(), definition.objectives(), definition.rewards(), definition.requirement());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeEnum(type);
        buf.writeComponent(title);
        buf.writeCollection(description, FriendlyByteBuf::writeComponent);
        buf.writeCollection(objectives, (b, o) -> {
            b.writeResourceLocation(o.getType().getId());
            o.toBytes(b);
        });
        buf.writeCollection(rewards, (b, r) -> {
            b.writeResourceLocation(r.getType().getId());
            r.toBytes(b);
        });
        buf.writeEnum(requirement);
    }
}
