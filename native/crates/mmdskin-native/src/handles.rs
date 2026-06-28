use mmd_anim::format::{PmdRuntimeImport, PmxRuntimeImport};
use mmd_anim::runtime::{AnimationClip, ModelArena, RuntimeInstance};
use std::collections::{HashMap, HashSet};
use std::sync::Arc;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Mutex, OnceLock};

static NEXT_HANDLE: AtomicU64 = AtomicU64::new(1);
static ENGINES: OnceLock<Mutex<HashSet<u64>>> = OnceLock::new();
static MODELS: OnceLock<Mutex<HashMap<u64, ModelHandle>>> = OnceLock::new();
static ANIMATIONS: OnceLock<Mutex<HashMap<u64, AnimationHandle>>> = OnceLock::new();

struct ModelHandle {
    engine: u64,
    kind: ModelKind,
    summary: ModelSummary,
    mesh: ModelMesh,
    skeleton: ModelSkeleton,
    runtime: Option<ModelRuntime>,
}

pub(crate) struct ModelRuntime {
    model: Arc<ModelArena>,
    bone_name_to_index: HashMap<Vec<u8>, mmd_anim::runtime::BoneIndex>,
    morph_name_to_index: HashMap<Vec<u8>, mmd_anim::runtime::MorphIndex>,
    ik_solver_bone_name_to_index: HashMap<Vec<u8>, usize>,
}

pub struct AnimationHandle {
    pub(crate) model: u64,
    pub(crate) clip: AnimationClip,
    pub(crate) runtime: RuntimeInstance,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ModelKind {
    Pmd,
    Pmx,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct ModelSummary {
    pub vertices: u32,
    pub indices: u32,
    pub materials: u32,
    pub bones: u32,
}

#[derive(Clone, Debug, Default, PartialEq)]
pub struct ModelMesh {
    pub positions: Vec<f32>,
    pub normals: Vec<f32>,
    pub uvs: Vec<f32>,
    pub indices: Vec<u32>,
    pub material_starts: Vec<u32>,
    pub material_counts: Vec<u32>,
    pub material_alphas: Vec<f32>,
}

#[derive(Clone, Debug, Default, PartialEq)]
pub struct ModelSkeleton {
    pub parent_indices: Vec<i32>,
    pub positions: Vec<f32>,
    pub skin_indices: Vec<u32>,
    pub skin_weights: Vec<f32>,
}

pub struct LoadedModelData {
    pub kind: ModelKind,
    pub summary: ModelSummary,
    pub mesh: ModelMesh,
    pub skeleton: ModelSkeleton,
    pub(crate) runtime: Option<ModelRuntime>,
}

fn engines() -> &'static Mutex<HashSet<u64>> {
    ENGINES.get_or_init(|| Mutex::new(HashSet::new()))
}

fn models() -> &'static Mutex<HashMap<u64, ModelHandle>> {
    MODELS.get_or_init(|| Mutex::new(HashMap::new()))
}

fn animations() -> &'static Mutex<HashMap<u64, AnimationHandle>> {
    ANIMATIONS.get_or_init(|| Mutex::new(HashMap::new()))
}

pub fn create_engine() -> u64 {
    let handle = NEXT_HANDLE.fetch_add(1, Ordering::Relaxed);
    let mut engines = engines().lock().unwrap();
    engines.insert(handle);
    handle
}

pub fn destroy_engine(handle: u64) -> bool {
    let mut engines = engines().lock().unwrap();
    let removed = engines.remove(&handle);
    if removed {
        let mut models = models().lock().unwrap();
        let removed_models = models
            .iter()
            .filter_map(|(model_handle, model)| (model.engine == handle).then_some(*model_handle))
            .collect::<HashSet<_>>();
        models.retain(|_, model| model.engine != handle);
        let mut animations = animations().lock().unwrap();
        animations.retain(|_, animation| !removed_models.contains(&animation.model));
    }
    removed
}

pub fn engine_exists(handle: u64) -> bool {
    let engines = engines().lock().unwrap();
    engines.contains(&handle)
}

pub fn create_model(engine: u64, data: LoadedModelData) -> u64 {
    let handle = NEXT_HANDLE.fetch_add(1, Ordering::Relaxed);
    let mut models = models().lock().unwrap();
    models.insert(
        handle,
        ModelHandle {
            engine,
            kind: data.kind,
            summary: data.summary,
            mesh: data.mesh,
            skeleton: data.skeleton,
            runtime: data.runtime,
        },
    );
    handle
}

pub fn destroy_model(handle: u64) -> bool {
    let mut models = models().lock().unwrap();
    let removed = models.remove(&handle).is_some();
    if removed {
        let mut animations = animations().lock().unwrap();
        animations.retain(|_, animation| animation.model != handle);
    }
    removed
}

pub fn model_kind(handle: u64) -> Option<ModelKind> {
    let models = models().lock().unwrap();
    models.get(&handle).map(|model| model.kind)
}

pub fn model_summary(handle: u64) -> Option<ModelSummary> {
    let models = models().lock().unwrap();
    models.get(&handle).map(|model| model.summary)
}

pub fn model_mesh(handle: u64) -> Option<ModelMesh> {
    let models = models().lock().unwrap();
    models.get(&handle).map(|model| model.mesh.clone())
}

pub fn model_skeleton(handle: u64) -> Option<ModelSkeleton> {
    let models = models().lock().unwrap();
    models.get(&handle).map(|model| model.skeleton.clone())
}

pub fn create_animation(model: u64, clip: AnimationClip) -> Option<u64> {
    let models = models().lock().unwrap();
    let model_runtime = models.get(&model)?.runtime.as_ref()?;
    let runtime = RuntimeInstance::new(Arc::clone(&model_runtime.model));
    drop(models);
    let handle = NEXT_HANDLE.fetch_add(1, Ordering::Relaxed);
    let mut animations = animations().lock().unwrap();
    animations.insert(
        handle,
        AnimationHandle {
            model,
            clip,
            runtime,
        },
    );
    Some(handle)
}

pub fn destroy_animation(handle: u64) -> bool {
    let mut animations = animations().lock().unwrap();
    animations.remove(&handle).is_some()
}

pub fn with_model_runtime<T>(
    model: u64,
    reader: impl FnOnce(&ModelRuntime) -> T,
) -> Option<T> {
    let models = models().lock().unwrap();
    models
        .get(&model)
        .and_then(|model| model.runtime.as_ref())
        .map(reader)
}

pub fn with_animation_mut<T>(
    handle: u64,
    reader: impl FnOnce(&mut AnimationHandle) -> T,
) -> Option<T> {
    let mut animations = animations().lock().unwrap();
    animations.get_mut(&handle).map(reader)
}

pub fn pmx_runtime(import: PmxRuntimeImport) -> ModelRuntime {
    ModelRuntime {
        model: Arc::new(import.model),
        bone_name_to_index: import.bone_name_to_index,
        morph_name_to_index: import.morph_name_to_index,
        ik_solver_bone_name_to_index: import.ik_solver_bone_name_to_index,
    }
}

pub fn pmd_runtime(import: PmdRuntimeImport) -> ModelRuntime {
    ModelRuntime {
        model: Arc::new(import.model),
        bone_name_to_index: import.bone_name_to_index,
        morph_name_to_index: import.morph_name_to_index,
        ik_solver_bone_name_to_index: import.ik_solver_bone_name_to_index,
    }
}

impl ModelRuntime {
    pub fn bone_name_to_index(&self) -> &HashMap<Vec<u8>, mmd_anim::runtime::BoneIndex> {
        &self.bone_name_to_index
    }

    pub fn morph_name_to_index(&self) -> &HashMap<Vec<u8>, mmd_anim::runtime::MorphIndex> {
        &self.morph_name_to_index
    }

    pub fn ik_solver_bone_name_to_index(&self) -> &HashMap<Vec<u8>, usize> {
        &self.ik_solver_bone_name_to_index
    }

    pub fn solver_count(&self) -> usize {
        self.model.ik_count()
    }
}
