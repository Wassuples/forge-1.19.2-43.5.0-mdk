package com.wassuples.demonslayer.entity.models;

import com.wassuples.demonslayer.entity.custom.HashiraEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

public class HashiraModel<T extends HashiraEntity> extends HumanoidModel<T> {
    public HashiraModel(ModelPart pRoot) {
        super(pRoot);
    }
}