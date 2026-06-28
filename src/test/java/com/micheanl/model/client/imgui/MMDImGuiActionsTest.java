package com.micheanl.model.client.imgui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class MMDImGuiActionsTest {
    @Test
    void appliesToggleAndReloadActions() {
        AtomicBoolean enabled = new AtomicBoolean(false);
        AtomicInteger reloads = new AtomicInteger();
        MMDImGuiController controller = new MMDImGuiController(enabled::get, enabled::set, reloads::incrementAndGet);

        controller.applyActions(MMDImGuiActions.TOGGLE_ENABLED | MMDImGuiActions.RELOAD_MODEL);

        assertTrue(enabled.get());
        assertEquals(1, reloads.get());
    }

    @Test
    void zeroActionDoesNotMutateState() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        AtomicInteger reloads = new AtomicInteger();
        MMDImGuiController controller = new MMDImGuiController(enabled::get, enabled::set, reloads::incrementAndGet);

        controller.applyActions(0);

        assertTrue(enabled.get());
        assertEquals(0, reloads.get());
    }

    @Test
    void closeActionIsConsumableOnce() {
        MMDImGuiController controller = new MMDImGuiController(() -> false, value -> {
        }, () -> {
        });

        controller.applyActions(MMDImGuiActions.CLOSE_SCREEN);

        assertTrue(controller.consumeCloseRequested());
        assertFalse(controller.consumeCloseRequested());
    }
}
