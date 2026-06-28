package com.micheanl.model.client.render;

import com.micheanl.model.client.nativebridge.MMDModelMesh;
import com.micheanl.model.client.nativebridge.MMDModelSkeleton;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

final class MMDGpuMeshCache implements AutoCloseable {
    private final IdentityHashMap<MMDModelMesh, Entry> meshes = new IdentityHashMap<>();

    MMDGpuMesh get(MMDModelMesh mesh, MMDModelSkeleton skeleton) {
        Entry entry = this.meshes.get(mesh);
        if (entry != null && entry.skeleton == skeleton) {
            return entry.gpuMesh;
        }
        if (entry != null) {
            entry.gpuMesh.close();
        }
        MMDGpuMesh gpuMesh = MMDGpuMesh.upload(mesh, skeleton);
        this.meshes.put(mesh, new Entry(skeleton, gpuMesh, true));
        return gpuMesh;
    }

    void beginFrame() {
        for (Entry entry : this.meshes.values()) {
            entry.used = false;
        }
    }

    void keep(MMDModelMesh mesh) {
        Entry entry = this.meshes.get(mesh);
        if (entry != null) {
            entry.used = true;
        }
    }

    void endFrame() {
        Iterator<Map.Entry<MMDModelMesh, Entry>> iterator = this.meshes.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next().getValue();
            if (!entry.used) {
                entry.gpuMesh.close();
                iterator.remove();
            }
        }
    }

    @Override
    public void close() {
        for (Entry entry : this.meshes.values()) {
            entry.gpuMesh.close();
        }
        this.meshes.clear();
    }

    private static final class Entry {
        private final MMDModelSkeleton skeleton;
        private final MMDGpuMesh gpuMesh;
        private boolean used;

        private Entry(MMDModelSkeleton skeleton, MMDGpuMesh gpuMesh, boolean used) {
            this.skeleton = skeleton;
            this.gpuMesh = gpuMesh;
            this.used = used;
        }
    }
}
