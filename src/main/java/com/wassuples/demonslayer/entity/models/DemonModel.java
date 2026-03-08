package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.entity.custom.DemonEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

// This generic model will work for any entity that extends DemonEntity.

/**
 * GeckoLib animated model for the Demon entity.
 */
public class DemonModel<T extends DemonEntity> extends HumanoidModel<T> {
    public DemonModel(ModelPart pRoot) {
        super(pRoot);
    }
}