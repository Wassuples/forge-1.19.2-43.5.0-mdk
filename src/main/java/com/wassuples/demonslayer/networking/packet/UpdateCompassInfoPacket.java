package com.wassuples.demonslayer.networking.packet;

import com.wassuples.demonslayer.client.CompassInfoRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Network packet: update compass info.
 *
 * @see com.wassuples.demonslayer.networking.ModMessages
 */
public class UpdateCompassInfoPacket {

    // Universal Stats
    private final int entityId;
    private final Component entityTypeName;
    private final float health, maxHealth;
    private final int armor;
    private final int air, maxAir;
    private final BlockPos position;
    private final List<String> effects;
    private final Component mainHandItem, offHandItem;

    // Player-Specific Stats
    private final int hunger;

    // Slayer Stats
    private final String slayerRank;
    private final int breath, maxBreath;
    private final String style;

    // Demon Stats
    private final boolean isDemon;
    private final String demonRank;
    private final int bloodPoints;
    private final String bda;


    public UpdateCompassInfoPacket(int entityId, Component entityTypeName, float health, float maxHealth, int armor, int air, int maxAir, BlockPos position, List<String> effects, Component mainHandItem, Component offHandItem, int hunger, String slayerRank, int breath, int maxBreath, String style, boolean isDemon, String demonRank, int bloodPoints, String bda) {
        this.entityId = entityId;
        this.entityTypeName = entityTypeName;
        this.health = health;
        this.maxHealth = maxHealth;
        this.armor = armor;
        this.air = air;
        this.maxAir = maxAir;
        this.position = position;
        this.effects = effects;
        this.mainHandItem = mainHandItem;
        this.offHandItem = offHandItem;
        this.hunger = hunger;
        this.slayerRank = slayerRank;
        this.breath = breath;
        this.maxBreath = maxBreath;
        this.style = style;
        this.isDemon = isDemon;
        this.demonRank = demonRank;
        this.bloodPoints = bloodPoints;
        this.bda = bda;
    }

    public UpdateCompassInfoPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.entityTypeName = buf.readComponent();
        this.health = buf.readFloat();
        this.maxHealth = buf.readFloat();
        this.armor = buf.readInt();
        this.air = buf.readInt();
        this.maxAir = buf.readInt();
        this.position = buf.readBlockPos();
        this.effects = buf.readList(FriendlyByteBuf::readUtf);
        this.mainHandItem = buf.readComponent();
        this.offHandItem = buf.readComponent();
        this.hunger = buf.readInt();
        this.slayerRank = buf.readUtf();
        this.breath = buf.readInt();
        this.maxBreath = buf.readInt();
        this.style = buf.readUtf();
        this.isDemon = buf.readBoolean();
        this.demonRank = buf.readUtf();
        this.bloodPoints = buf.readInt();
        this.bda = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeComponent(entityTypeName);
        buf.writeFloat(health);
        buf.writeFloat(maxHealth);
        buf.writeInt(armor);
        buf.writeInt(air);
        buf.writeInt(maxAir);
        buf.writeBlockPos(position);
        buf.writeCollection(effects, FriendlyByteBuf::writeUtf);
        buf.writeComponent(mainHandItem);
        buf.writeComponent(offHandItem);
        buf.writeInt(hunger);
        buf.writeUtf(slayerRank);
        buf.writeInt(breath);
        buf.writeInt(maxBreath);
        buf.writeUtf(style);
        buf.writeBoolean(isDemon);
        buf.writeUtf(demonRank);
        buf.writeInt(bloodPoints);
        buf.writeUtf(bda);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        supplier.get().enqueueWork(() -> CompassInfoRenderer.updateInfo(this));
        return true;
    }

    // Add getters for everything
    public int getEntityId() { return entityId; }
    public Component getEntityTypeName() { return entityTypeName; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public int getArmor() { return armor; }
    public int getAir() { return air; }
    public int getMaxAir() { return maxAir; }
    public BlockPos getPosition() { return position; }
    public List<String> getEffects() { return effects; }
    public Component getMainHandItem() { return mainHandItem; }
    public Component getOffHandItem() { return offHandItem; }
    public int getHunger() { return hunger; }
    public String getSlayerRank() { return slayerRank; }
    public int getBreath() { return breath; }
    public int getMaxBreath() { return maxBreath; }
    public String getStyle() { return style; }
    public boolean isDemon() { return isDemon; }
    public String getDemonRank() { return demonRank; }
    public int getBloodPoints() { return bloodPoints; }
    public String getBda() { return bda; }
}