package com.micheanl.model.client.imgui;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class MMDImGuiController {
    private final BooleanSupplier enabledSource;
    private final Consumer<Boolean> enabledSink;
    private final Runnable reloadModel;
    private boolean closeRequested;

    public MMDImGuiController(BooleanSupplier enabledSource, Consumer<Boolean> enabledSink, Runnable reloadModel) {
        this.enabledSource = Objects.requireNonNull(enabledSource, "enabledSource");
        this.enabledSink = Objects.requireNonNull(enabledSink, "enabledSink");
        this.reloadModel = Objects.requireNonNull(reloadModel, "reloadModel");
    }

    public void applyActions(int actions) {
        if ((actions & MMDImGuiActions.TOGGLE_ENABLED) != 0) {
            this.enabledSink.accept(!this.enabledSource.getAsBoolean());
        }
        if ((actions & MMDImGuiActions.RELOAD_MODEL) != 0) {
            this.reloadModel.run();
        }
        if ((actions & MMDImGuiActions.CLOSE_SCREEN) != 0) {
            this.closeRequested = true;
        }
    }

    public boolean consumeCloseRequested() {
        boolean value = this.closeRequested;
        this.closeRequested = false;
        return value;
    }
}
