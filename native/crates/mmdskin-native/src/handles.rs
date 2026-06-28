use std::collections::{HashMap, HashSet};
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Mutex, OnceLock};

static NEXT_HANDLE: AtomicU64 = AtomicU64::new(1);
static ENGINES: OnceLock<Mutex<HashSet<u64>>> = OnceLock::new();
static MODELS: OnceLock<Mutex<HashMap<u64, ModelHandle>>> = OnceLock::new();

struct ModelHandle {
    engine: u64,
}

fn engines() -> &'static Mutex<HashSet<u64>> {
    ENGINES.get_or_init(|| Mutex::new(HashSet::new()))
}

fn models() -> &'static Mutex<HashMap<u64, ModelHandle>> {
    MODELS.get_or_init(|| Mutex::new(HashMap::new()))
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
        models.retain(|_, model| model.engine != handle);
    }
    removed
}

pub fn engine_exists(handle: u64) -> bool {
    let engines = engines().lock().unwrap();
    engines.contains(&handle)
}

pub fn create_model(engine: u64, _path: &str) -> u64 {
    let handle = NEXT_HANDLE.fetch_add(1, Ordering::Relaxed);
    let mut models = models().lock().unwrap();
    models.insert(handle, ModelHandle { engine });
    handle
}

pub fn destroy_model(handle: u64) -> bool {
    let mut models = models().lock().unwrap();
    models.remove(&handle).is_some()
}
