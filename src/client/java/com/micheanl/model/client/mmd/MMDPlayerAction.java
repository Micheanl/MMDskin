package com.micheanl.model.client.mmd;

import java.util.Locale;

public enum MMDPlayerAction {
    IDLE("idle"),
    WALK("walk"),
    SPRINT("sprint"),
    SNEAK("sneak"),
    SWIM("swim"),
    CRAWL("crawl"),
    ELYTRA_FLY("elytraFly"),
    DIE("die"),
    RIDE("ride"),
    SLEEP("sleep"),
    LIE_DOWN("lieDown"),
    ON_CLIMBABLE("onClimbable"),
    ON_CLIMBABLE_UP("onClimbableUp"),
    ON_CLIMBABLE_DOWN("onClimbableDown"),
    ON_HORSE("onHorse"),
    SWING_LEFT("swingLeft"),
    SWING_RIGHT("swingRight"),
    ITEM_ACTIVE_BOW_LEFT_USING("itemActive_minecraft.bow_Left_using"),
    ITEM_ACTIVE_IRON_SWORD_RIGHT_SWINGING("itemActive_minecraft.iron_sword_Right_swinging"),
    ITEM_ACTIVE_SHIELD_LEFT_USING("itemActive_minecraft.shield_Left_using"),
    ITEM_ACTIVE_SHIELD_RIGHT_USING("itemActive_minecraft.shield_Right_using"),
    UNKNOWN("");

    private final String animationName;

    MMDPlayerAction(String animationName) {
        this.animationName = animationName;
    }

    public String animationName() {
        return this.animationName;
    }

    public static MMDPlayerAction fromAnimationName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        for (MMDPlayerAction action : values()) {
            if (action.animationName.toLowerCase(Locale.ROOT).equals(normalized)) {
                return action;
            }
        }
        return UNKNOWN;
    }
}
