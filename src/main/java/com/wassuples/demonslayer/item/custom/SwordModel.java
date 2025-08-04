package com.wassuples.demonslayer.item.custom;

public enum SwordModel {
    DEFAULT("default", 0),

    // Add each Hashira and their unique ID here
    WATER("Water, Giyu", 1),
    INSECT("Insect, Shinobu", 2),
    LOVE("Love, Mitsuri", 3),
    SNAKE("Snake, Obanai", 4),
    WIND("Wind, Sanemi", 5),
    STONE("Stone, Gyomei", 6),
    MIST("Mist, Muichiro", 7),
    FLAME("Flame, Rengoku", 8),
    SOUND("Sound, Tengen", 9),
    THUNDER("Thunder, Jigoro", 10),
    FLOWER("Flower, Kanae", 11);

    private final String modelName;
    private final int customModelData;

    SwordModel(String modelName, int customModelData) {
        this.modelName = modelName;
        this.customModelData = customModelData;
    }

    public String getModelName() {
        return modelName;
    }

    public int getCustomModelData() {
        return customModelData;
    }
}