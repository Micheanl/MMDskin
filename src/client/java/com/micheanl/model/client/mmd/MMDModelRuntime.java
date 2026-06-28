package com.micheanl.model.client.mmd;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDNative;
import com.micheanl.model.client.nativebridge.MMDNativeEngine;
import com.micheanl.model.client.nativebridge.MMDNativeModel;
import com.micheanl.model.client.render.MMDPlayerRenderState;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class MMDModelRuntime implements AutoCloseable {
    public record LoadedModel(MMDModelMesh mesh, AutoCloseable closeable) implements AutoCloseable {
        public LoadedModel {
            Objects.requireNonNull(mesh, "mesh");
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
    public interface LoaderFactory {
        ModelLoader create();
    }

    @FunctionalInterface
    public interface ModelLoader {
        LoadedModel load(Path path);
    }

    private static final MMDModelRuntime INSTANCE = new MMDModelRuntime(
            ModelIndexer::index,
            NativeModelLoader::new,
            MMDPlayerRenderState::setEnabled
    );

    private final Indexer indexer;
    private final LoaderFactory loaderFactory;
    private final Consumer<Boolean> enabledSink;
    private LoadedModel loaded;

    public MMDModelRuntime(Indexer indexer, LoaderFactory loaderFactory, Consumer<Boolean> enabledSink) {
        this.indexer = Objects.requireNonNull(indexer, "indexer");
        this.loaderFactory = Objects.requireNonNull(loaderFactory, "loaderFactory");
        this.enabledSink = Objects.requireNonNull(enabledSink, "enabledSink");
    }

    public static MMDModelRuntime instance() {
        return INSTANCE;
    }

    public static Path defaultModelRoot() {
        return FabricLoader.getInstance().getConfigDir().resolve("mmdskin").resolve("models");
    }

    public void reload(Path root) throws IOException {
        closeLoaded();
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
        return model == null ? Optional.empty() : Optional.of(model.mesh());
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
            return new LoadedModel(model.mesh(), () -> {
                model.close();
                engine.close();
            });
        }
    }
}
