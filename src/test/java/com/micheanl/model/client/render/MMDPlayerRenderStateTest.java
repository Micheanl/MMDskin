package com.micheanl.model.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

final class MMDPlayerRenderStateTest {
    @Test
    void customPlayerRenderingIsDisabledByDefault() {
        assertFalse(MMDPlayerRenderState.isEnabled());
    }
}
