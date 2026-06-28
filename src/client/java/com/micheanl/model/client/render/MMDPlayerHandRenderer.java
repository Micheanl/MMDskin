package com.micheanl.model.client.render;

import com.micheanl.model.client.mmd.MMDModelRuntime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.FabricOrderedSubmitNodeCollector;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;

import java.util.Optional;

public final class MMDPlayerHandRenderer {
    private MMDPlayerHandRenderer() {
    }

    public static boolean submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, HumanoidArm arm) {
        Optional<MMDModelRuntime.ModelRenderData> data = MMDModelRuntime.instance().renderData();
        if (data.isEmpty()) {
            return false;
        }
        ((FabricOrderedSubmitNodeCollector) submitNodeCollector).submitCustom(
                SubmitRenderPhases.SOLID,
                new MMDPlayerHandSubmit(new Matrix4f(poseStack.last().pose()), data.get().mesh(), data.get().transform(), arm, lightCoords)
        );
        return true;
    }
}
