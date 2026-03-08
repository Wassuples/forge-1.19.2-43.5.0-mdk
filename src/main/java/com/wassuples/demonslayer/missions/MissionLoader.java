package com.wassuples.demonslayer.missions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wassuples.demonslayer.DemonSlayer;
import com.wassuples.demonslayer.ranking.Rank;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mission system: Mission Loader.
 *
 * @see com.wassuples.demonslayer.missions.MissionRegistry
 */
@Mod.EventBusSubscriber(modid = DemonSlayer.MOD_ID)
public final class MissionLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private MissionLoader() {
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new MissionReloadListener());
    }

    static MissionDefinition parseDefinition(ResourceLocation id, JsonObject json) {
        MissionType type = MissionType.valueOf(GsonHelper.getAsString(json, "type").toUpperCase());
        Component title = Component.translatable(GsonHelper.getAsString(json, "title"));

        List<Component> description = new ArrayList<>();
        JsonArray descArray = GsonHelper.getAsJsonArray(json, "description", new JsonArray());
        for (JsonElement element : descArray) {
            description.add(Component.translatable(element.getAsString()));
        }

        List<MissionObjective> objectives = new ArrayList<>();
        JsonArray objectiveArray = GsonHelper.getAsJsonArray(json, "objectives");
        for (JsonElement element : objectiveArray) {
            JsonObject objJson = element.getAsJsonObject();
            ResourceLocation typeId = new ResourceLocation(GsonHelper.getAsString(objJson, "type"));
            ObjectiveType<?> objectiveType = ObjectiveType.REGISTRY.get(typeId);
            if (objectiveType == null) {
                throw new IllegalArgumentException("Unknown objective type " + typeId);
            }
            objectives.add(objectiveType.deserialize(objJson));
        }

        List<MissionReward> rewards = new ArrayList<>();
        JsonArray rewardArray = GsonHelper.getAsJsonArray(json, "rewards");
        for (JsonElement element : rewardArray) {
            JsonObject rewardJson = element.getAsJsonObject();
            ResourceLocation rewardId = new ResourceLocation(GsonHelper.getAsString(rewardJson, "type"));
            RewardType<?> rewardType = RewardType.REGISTRY.get(rewardId);
            if (rewardType == null) {
                throw new IllegalArgumentException("Unknown reward type " + rewardId);
            }
            rewards.add(rewardType.deserialize(rewardJson));
        }

        Rank requirement = Rank.valueOf(GsonHelper.getAsString(json, "requirement", Rank.CIVILIAN.name()).toUpperCase());
        return new MissionDefinition(id, type, title, description, objectives, rewards, requirement);
    }
}
