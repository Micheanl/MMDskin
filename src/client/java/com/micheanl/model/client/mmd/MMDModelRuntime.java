package com.micheanl.model.client.mmd;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDModelSkeleton;
import com.micheanl.model.client.nativebridge.MMDNative;
import com.micheanl.model.client.nativebridge.MMDNativeEngine;
import com.micheanl.model.client.nativebridge.MMDNativeModel;
import com.micheanl.model.client.render.MMDMeshEmitter;
import com.micheanl.model.client.render.MMDPlayerRenderState;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class MMDModelRuntime implements AutoCloseable {
    public record ModelRenderData(MMDModelMesh mesh, MMDModelSkeleton skeleton, MMDMeshEmitter.Transform transform) {
        public ModelRenderData {
            Objects.requireNonNull(mesh, "mesh");
            Objects.requireNonNull(skeleton, "skeleton");
            Objects.requireNonNull(transform, "transform");
        }
    }

    public record LoadedModel(ModelRenderData renderData, AutoCloseable closeable) implements AutoCloseable {
        public LoadedModel(MMDModelMesh mesh, MMDModelSkeleton skeleton, AutoCloseable closeable) {
            this(new ModelRenderData(mesh, skeleton, MMDMeshEmitter.Transform.player(mesh)), closeable);
        }

        public LoadedModel {
            Objects.requireNonNull(renderData, "renderData");
            Objects.requireNonNull(closeable, "closeable");
        }

        @Override
        public void close() throws Exception {
            this.closeable.close();
        }
    }

    @FunctionalInterface
    public interface Indexer {
        List<ModelIndexEntry> index(Path root) throws IOException;
    }

    @FunctionalInterface
    public interface AnimationIndexer {
        List<MMDAnimationRuntime.AnimationEntry> index(Path root) throws IOException;
    }

    @FunctionalInterface
    public interface LoaderFactory {
        ModelLoader create();
    }

    @FunctionalInterface
    public interface ModelLoader {
        LoadedModel load(Path path);
    }

    private final Indexer indexer;
    private final AnimationIndexer animationIndexer;
    private final LoaderFactory loaderFactory;
    private final Consumer<Boolean> enabledSink;
    private final Path animationRoot;
    private LoadedModel loaded;
    private List<MMDAnimationRuntime.AnimationEntry> animations = List.of();

    public MMDModelRuntime(Indexer indexer, AnimationIndexer animationIndexer, LoaderFactory loaderFactory, Consumer<Boolean> enabledSink) {
        this(indexer, animationIndexer, loaderFactory, enabledSink, Path.of("animations"));
    }

    public MMDModelRuntime(Indexer indexer, AnimationIndexer animationIndexer, LoaderFactory loaderFactory, Consumer<Boolean> enabledSink, Path animationRoot) {
        this.indexer = Objects.requireNonNull(indexer, "indexer");
        this.animationIndexer = Objects.requireNonNull(animationIndexer, "animationIndexer");
        this.loaderFactory = Objects.requireNonNull(loaderFactory, "loaderFactory");
        this.enabledSink = Objects.requireNonNull(enabledSink, "enabledSink");
        this.animationRoot = Objects.requireNonNull(animationRoot, "animationRoot");
    }

    public static MMDModelRuntime instance() {
        return Holder.INSTANCE;
    }

    public static Path defaultModelRoot() {
        return FabricLoader.getInstance().getConfigDir().resolve("mmdskin").resolve("models");
    }

    public void reload(Path root) throws IOException {
        closeLoaded();
        this.animations = this.animationIndexer.index(this.animationRoot);
        List<ModelIndexEntry> models = this.indexer.index(root);
        if (models.isEmpty()) {
            this.enabledSink.accept(false);
            return;
        }
        LoadedModel model = this.loaderFactory.create().load(models.getFirst().path());
        this.loaded = model;
        this.enabledSink.accept(true);
    }

    public Optional<MMDModelMesh> mesh() {
        LoadedModel model = this.loaded;
        return model == null ? Optional.empty() : Optional.of(model.renderData().mesh());
    }

    public Optional<ModelRenderData> renderData() {
        LoadedModel model = this.loaded;
        return model == null ? Optional.empty() : Optional.of(model.renderData());
    }

    public List<MMDAnimationRuntime.AnimationEntry> animations() {
        return this.animations;
    }

    public Optional<MMDAnimationRuntime.AnimationEntry> animation(MMDPlayerAction action) {
        return this.animations.stream()
                .filter(entry -> entry.action() == action)
                .findFirst();
    }

    @Override
    public void close() throws Exception {
        closeLoaded();
        this.enabledSink.accept(false);
    }

    private void closeLoaded() {
        LoadedModel model = this.loaded;
        if (model == null) {
            return;
        }
        this.loaded = null;
        try {
            model.close();
        } catch (Exception e) {
            throw new IllegalStateException("Native model close failed", e);
        }
    }

    private static final class NativeModelLoader implements ModelLoader {
        @Override
        public LoadedModel load(Path path) {
            MMDNativeEngine engine = MMDNative.engineCreate();
            MMDNativeModel model = engine.loadModel(path);
            return new LoadedModel(model.mesh(), model.skeleton(), () -> {
                model.close();
                engine.close();
            });
        }
    }

    private static final class Holder {
        private static final MMDModelRuntime INSTANCE = new MMDModelRuntime(
                ModelIndexer::index,
                MMDAnimationRuntime::index,
                NativeModelLoader::new,
                MMDPlayerRenderState::setEnabled,
                MMDAnimationRuntime.defaultAnimationRoot()
        );
    }
}
