package com.micheanl.model.client.mmd;

import com.micheanl.model.client.nativebridge.MMDAnimationSummary;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MMDPlayerAnimationSelectorTest {
    @Test
    void selectsDeathBeforeMovement() {
        AvatarRenderState state = new AvatarRenderState();
        state.deathTime = 1.0F;
        state.walkAnimationSpeed = 1.0F;

        assertEquals(MMDPlayerAction.DIE, MMDPlayerAnimationSelector.selectAction(state));
    }

    @Test
    void selectsMovementActions() {
        AvatarRenderState state = new AvatarRenderState();
        assertEquals(MMDPlayerAction.IDLE, MMDPlayerAnimationSelector.selectAction(state));

        state.walkAnimationSpeed = 0.2F;
        assertEquals(MMDPlayerAction.WALK, MMDPlayerAnimationSelector.selectAction(state));

        state.walkAnimationSpeed = 1.0F;
        assertEquals(MMDPlayerAction.SPRINT, MMDPlayerAnimationSelector.selectAction(state));

        state.isCrouching = true;
        assertEquals(MMDPlayerAction.SNEAK, MMDPlayerAnimationSelector.selectAction(state));
    }

    @Test
    void selectsPoseActions() {
        AvatarRenderState state = new AvatarRenderState();
        state.pose = Pose.SLEEPING;
        assertEquals(MMDPlayerAction.SLEEP, MMDPlayerAnimationSelector.selectAction(state));

        state.pose = Pose.STANDING;
        state.isFallFlying = true;
        assertEquals(MMDPlayerAction.ELYTRA_FLY, MMDPlayerAnimationSelector.selectAction(state));

        state.isFallFlying = false;
        state.isVisuallySwimming = true;
        assertEquals(MMDPlayerAction.SWIM, MMDPlayerAnimationSelector.selectAction(state));

        state.isVisuallySwimming = false;
        state.pose = Pose.SWIMMING;
        state.isInWater = false;
        assertEquals(MMDPlayerAction.CRAWL, MMDPlayerAnimationSelector.selectAction(state));
    }

    @Test
    void fallsBackToIdleEntryWhenSelectedActionIsMissing() {
        AvatarRenderState state = new AvatarRenderState();
        state.walkAnimationSpeed = 1.0F;
        MMDAnimationRuntime.AnimationEntry idle = entry("idle");

        assertEquals(idle, MMDPlayerAnimationSelector.select(state, List.of(idle)).orElseThrow());
    }

    private static MMDAnimationRuntime.AnimationEntry entry(String name) {
        return new MMDAnimationRuntime.AnimationEntry(
                name,
                Path.of(name + ".vmd"),
                new MMDAnimationSummary(1, 1, 0, 0, 0, 0, 0),
                MMDPlayerAction.fromAnimationName(name)
        );
    }
}
