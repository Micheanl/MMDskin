package com.micheanl.model.client.mmd;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Pose;

import java.util.List;
import java.util.Optional;

public final class MMDPlayerAnimationSelector {
    private static final float SPRINT_SPEED = 0.9F;
    private static final float WALK_SPEED = 0.02F;

    private MMDPlayerAnimationSelector() {
    }

    public static MMDPlayerAction selectAction(AvatarRenderState state) {
        if (state.deathTime > 0.0F) {
            return MMDPlayerAction.DIE;
        }
        if (state.hasPose(Pose.SLEEPING)) {
            return MMDPlayerAction.SLEEP;
        }
        if (state.isFallFlying) {
            return MMDPlayerAction.ELYTRA_FLY;
        }
        if (state.isVisuallySwimming) {
            return MMDPlayerAction.SWIM;
        }
        if (state.hasPose(Pose.SWIMMING) && !state.isInWater) {
            return MMDPlayerAction.CRAWL;
        }
        if (state.isPassenger) {
            return MMDPlayerAction.RIDE;
        }
        if (state.isCrouching) {
            return MMDPlayerAction.SNEAK;
        }
        if (state.walkAnimationSpeed >= SPRINT_SPEED) {
            return MMDPlayerAction.SPRINT;
        }
        if (state.walkAnimationSpeed > WALK_SPEED) {
            return MMDPlayerAction.WALK;
        }
        return MMDPlayerAction.IDLE;
    }

    public static Optional<MMDAnimationRuntime.AnimationEntry> select(AvatarRenderState state, List<MMDAnimationRuntime.AnimationEntry> animations) {
        MMDPlayerAction action = selectAction(state);
        Optional<MMDAnimationRuntime.AnimationEntry> selected = animations.stream()
                .filter(entry -> entry.action() == action)
                .findFirst();
        return selected.isPresent() ? selected : animations.stream()
                .filter(entry -> entry.action() == MMDPlayerAction.IDLE)
                .findFirst();
    }
}
