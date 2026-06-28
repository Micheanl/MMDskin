use std::collections::HashSet;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Mutex, OnceLock};

static NEXT_HANDLE: AtomicU64 = AtomicU64::new(1);
static ENGINES: OnceLock<Mutex<HashSet<u64>>> = OnceLock::new();

fn engines() -> &'static Mutex<HashSet<u64>> {
    ENGINES.get_or_init(|| Mutex::new(HashSet::new()))
}

pub fn create_engine() -> u64 {
    let handle = NEXT_HANDLE.fetch_add(1, Ordering::Relaxed);
    let mut engines = engines().lock().unwrap();
    engines.insert(handle);
    handle
}

pub fn destroy_engine(handle: u64) -> bool {
    let mut engines = engines().lock().unwrap();
    engines.remove(&handle)
}
