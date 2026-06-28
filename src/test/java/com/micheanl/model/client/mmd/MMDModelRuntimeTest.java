package com.micheanl.model.client.mmd;

import com.micheanl.model.client.nativebridge.MMDAnimationSummary;
import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDModelSkeleton;
import com.micheanl.model.client.nativebridge.MMDSampledPose;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MMDModelRuntimeTest {
    @Test
    void staysEmptyWhenNoModelsExist() throws Exception {
        MMDModelRuntime runtime = new MMDModelRuntime(
                root -> List.of(),
                root -> List.of(),
                () -> path -> {
                    throw new AssertionError();
                },
                loaded -> {}
        );

        runtime.reload(Path.of("models"));

        assertFalse(runtime.mesh().isPresent());
    }

    @Test
    void loadsFirstIndexedModel() throws Exception {
        MMDModelMesh mesh = mesh();
        MMDModelSkeleton skeleton = skeleton();
        MMDModelRuntime runtime = new MMDModelRuntime(
                root -> List.of(new ModelIndexEntry(root.resolve("a.pmx"), new ModelHash("0".repeat(64)), 3)),
                root -> List.of(animation("idle")),
                () -> path -> new MMDModelRuntime.LoadedModel(mesh, skeleton, () -> {}),
                loaded -> {}
        );

        runtime.reload(Path.of("models"));

        assertSame(mesh, runtime.mesh().orElseThrow());
        assertSame(skeleton, runtime.renderData().orElseThrow().skeleton());
        assertEquals(MMDPlayerAction.IDLE, runtime.animations().getFirst().action());
    }

    @Test
    void returnsNoSampleForNonNativeTestModel() throws Exception {
        MMDModelRuntime runtime = new MMDModelRuntime(
                root -> List.of(new ModelIndexEntry(root.resolve("a.pmx"), new ModelHash("0".repeat(64)), 3)),
                root -> List.of(animation("idle")),
                () -> path -> new MMDModelRuntime.LoadedModel(mesh(), skeleton(), () -> {}),
                loaded -> {}
        );

        runtime.reload(Path.of("models"));
        MMDSampledPose sample = runtime.sample(runtime.animations().getFirst(), 1.0F);

        assertNull(sample);
    }

    @Test
    void closesLoadedModel() throws Exception {
        AtomicBoolean closed = new AtomicBoolean();
        MMDModelRuntime runtime = new MMDModelRuntime(
                root -> List.of(new ModelIndexEntry(root.resolve("a.pmx"), new ModelHash("0".repeat(64)), 3)),
                root -> List.of(),
                () -> path -> new MMDModelRuntime.LoadedModel(mesh(), skeleton(), () -> closed.set(true)),
                loaded -> {}
        );

        runtime.reload(Path.of("models"));
        runtime.close();

        assertTrue(closed.get());
        assertFalse(runtime.mesh().isPresent());
    }

    private static MMDModelMesh mesh() {
        return new MMDModelMesh(
                new float[] {0.0F, 1.0F, 2.0F},
                new float[] {0.0F, 1.0F, 0.0F},
                new float[] {0.25F, 0.75F},
                new int[] {0, 0, 0},
                new int[] {0},
                new int[] {3},
                new float[] {1.0F}
        );
    }

    private static MMDModelSkeleton skeleton() {
        return new MMDModelSkeleton(
                new int[] {-1},
                new float[] {0.0F, 1.0F, 2.0F},
                new int[] {0, 0, 0, 0},
                new float[] {1.0F, 0.0F, 0.0F, 0.0F}
        );
    }

    private static MMDAnimationRuntime.AnimationEntry animation(String name) {
        return new MMDAnimationRuntime.AnimationEntry(
                name,
                Path.of(name + ".vmd"),
                new MMDAnimationSummary(1, 1, 0, 0, 0, 0, 0),
                MMDPlayerAction.fromAnimationName(name)
        );
    }
}
