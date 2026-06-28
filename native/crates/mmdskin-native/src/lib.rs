mod handles;
mod status;

use jni::objects::{JClass, JLongArray, JString};
use jni::{errors::Result as JniResult, EnvUnowned, Outcome};
use mmd_anim::format::{MmdFormatKind, detect_mmd_format, parse_pmd_model, parse_pmx_model};
use std::ffi::c_void;
use std::fs;
use std::slice;
use std::str;

pub use status::NativeStatus;

#[unsafe(no_mangle)]
pub extern "C" fn mmdskin_native_version() -> i32 {
    1
}

#[unsafe(no_mangle)]
pub extern "C" fn mmdskin_engine_create() -> u64 {
    handles::create_engine()
}

#[unsafe(no_mangle)]
pub extern "C" fn mmdskin_engine_destroy(handle: u64) -> i32 {
    if handle == 0 {
        return NativeStatus::InvalidArgument as i32;
    }
    if handles::destroy_engine(handle) {
        NativeStatus::Ok as i32
    } else {
        NativeStatus::NotFound as i32
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mmdskin_model_load(
    engine: u64,
    path: *const u8,
    path_len: usize,
    out_model: *mut u64,
) -> i32 {
    if engine == 0 || path.is_null() || path_len == 0 || out_model.is_null() {
        return NativeStatus::InvalidArgument as i32;
    }
    if !handles::engine_exists(engine) {
        return NativeStatus::NotFound as i32;
    }
    let bytes = unsafe { slice::from_raw_parts(path, path_len) };
    let Ok(path) = str::from_utf8(bytes) else {
        return NativeStatus::InvalidArgument as i32;
    };
    let Ok(data) = fs::read(path) else {
        return NativeStatus::NotFound as i32;
    };
    let (kind, summary) = match detect_mmd_format(&data, Some(path)) {
        MmdFormatKind::Pmd => (
            handles::ModelKind::Pmd,
            parse_pmd_model(&data).map(pmd_summary).unwrap_or_default(),
        ),
        MmdFormatKind::Pmx => (
            handles::ModelKind::Pmx,
            parse_pmx_model(&data).map(pmx_summary).unwrap_or_default(),
        ),
        _ => return NativeStatus::InvalidArgument as i32,
    };
    let model = handles::create_model(engine, kind, summary);
    unsafe {
        *out_model = model;
    }
    NativeStatus::Ok as i32
}

fn pmd_summary(model: mmd_anim::format::PmdParsedModel) -> handles::ModelSummary {
    handles::ModelSummary {
        vertices: model.metadata.counts.vertices as u32,
        indices: model.geometry.indices.len() as u32,
        materials: model.metadata.counts.materials as u32,
        bones: model.metadata.counts.bones as u32,
    }
}

fn pmx_summary(model: mmd_anim::format::PmxParsedModel) -> handles::ModelSummary {
    handles::ModelSummary {
        vertices: model.metadata.counts.vertices as u32,
        indices: model.geometry.indices.len() as u32,
        materials: model.metadata.counts.materials as u32,
        bones: model.metadata.counts.bones as u32,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn mmdskin_model_destroy(handle: u64) -> i32 {
    if handle == 0 {
        return NativeStatus::InvalidArgument as i32;
    }
    if handles::destroy_model(handle) {
        NativeStatus::Ok as i32
    } else {
        NativeStatus::NotFound as i32
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn mmdskin_model_kind(handle: u64) -> i32 {
    if handle == 0 {
        return NativeStatus::InvalidArgument as i32;
    }
    match handles::model_kind(handle) {
        Some(handles::ModelKind::Pmd) => 10,
        Some(handles::ModelKind::Pmx) => 11,
        None => NativeStatus::NotFound as i32,
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mmdskin_model_summary(
    handle: u64,
    out_summary: *mut u32,
    out_len: usize,
) -> i32 {
    if handle == 0 || out_summary.is_null() || out_len < 4 {
        return NativeStatus::InvalidArgument as i32;
    }
    let Some(summary) = handles::model_summary(handle) else {
        return NativeStatus::NotFound as i32;
    };
    let out = unsafe { slice::from_raw_parts_mut(out_summary, out_len) };
    out[0] = summary.vertices;
    out[1] = summary.indices;
    out[2] = summary.materials;
    out[3] = summary.bones;
    NativeStatus::Ok as i32
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_nativeVersion(
    _env: *mut c_void,
    _class: *mut c_void,
) -> i32 {
    mmdskin_native_version()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_engineCreateRaw(
    _env: *mut c_void,
    _class: *mut c_void,
) -> i64 {
    mmdskin_engine_create() as i64
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_engineDestroyRaw(
    _env: *mut c_void,
    _class: *mut c_void,
    handle: i64,
) -> i32 {
    if handle < 0 {
        return NativeStatus::InvalidArgument as i32;
    }
    mmdskin_engine_destroy(handle as u64)
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_modelLoadRaw(
    mut env: EnvUnowned<'_>,
    _class: JClass<'_>,
    engine: i64,
    path: JString<'_>,
    out_model: JLongArray<'_>,
) -> i32 {
    if engine < 0 || path.is_null() || out_model.is_null() {
        return NativeStatus::InvalidArgument as i32;
    }
    match env
        .with_env(|env| -> JniResult<i32> {
            if out_model.len(env)? == 0 {
                return Ok(NativeStatus::InvalidArgument as i32);
            }
            let path = path.try_to_string(env)?;
            let mut model = 0_u64;
            let status = unsafe {
                mmdskin_model_load(engine as u64, path.as_ptr(), path.len(), &mut model)
            };
            if status == NativeStatus::Ok as i32 {
                out_model.set_region(env, 0, &[model as i64])?;
            }
            Ok(status)
        })
        .into_outcome()
    {
        Outcome::Ok(status) => status,
        Outcome::Err(_) | Outcome::Panic(_) => NativeStatus::InternalError as i32,
    }
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_modelLoadRawBytes(
    _env: *mut c_void,
    _class: *mut c_void,
    engine: i64,
    path: *const u8,
    path_len: i32,
    out_model: *mut i64,
) -> i32 {
    if engine < 0 || path_len < 0 || out_model.is_null() {
        return NativeStatus::InvalidArgument as i32;
    }
    let mut model = 0_u64;
    let status = unsafe { mmdskin_model_load(engine as u64, path, path_len as usize, &mut model) };
    if status == NativeStatus::Ok as i32 {
        unsafe {
            *out_model = model as i64;
        }
    }
    status
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_modelDestroyRaw(
    _env: *mut c_void,
    _class: *mut c_void,
    handle: i64,
) -> i32 {
    if handle < 0 {
        return NativeStatus::InvalidArgument as i32;
    }
    mmdskin_model_destroy(handle as u64)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_modelKindRaw(
    _env: *mut c_void,
    _class: *mut c_void,
    handle: i64,
) -> i32 {
    if handle < 0 {
        return NativeStatus::InvalidArgument as i32;
    }
    mmdskin_model_kind(handle as u64)
}

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_modelSummaryRaw(
    mut env: EnvUnowned<'_>,
    _class: JClass<'_>,
    handle: i64,
    out_summary: JLongArray<'_>,
) -> i32 {
    if handle < 0 || out_summary.is_null() {
        return NativeStatus::InvalidArgument as i32;
    }
    match env
        .with_env(|env| -> JniResult<i32> {
            if out_summary.len(env)? < 4 {
                return Ok(NativeStatus::InvalidArgument as i32);
            }
            let mut summary = [0_u32; 4];
            let status = unsafe {
                mmdskin_model_summary(handle as u64, summary.as_mut_ptr(), summary.len())
            };
            if status == NativeStatus::Ok as i32 {
                out_summary.set_region(
                    env,
                    0,
                    &[
                        summary[0] as i64,
                        summary[1] as i64,
                        summary[2] as i64,
                        summary[3] as i64,
                    ],
                )?;
            }
            Ok(status)
        })
        .into_outcome()
    {
        Outcome::Ok(status) => status,
        Outcome::Err(_) | Outcome::Panic(_) => NativeStatus::InternalError as i32,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;
    use std::path::PathBuf;
    use std::time::{SystemTime, UNIX_EPOCH};

    #[test]
    fn version_is_positive() {
        assert!(crate::mmdskin_native_version() > 0);
    }

    #[test]
    fn engine_lifecycle_rejects_zero_handle() {
        assert_eq!(
            crate::mmdskin_engine_destroy(0),
            NativeStatus::InvalidArgument as i32
        );
    }

    #[test]
    fn engine_lifecycle_rejects_missing_handle() {
        assert_eq!(
            crate::mmdskin_engine_destroy(999_999),
            NativeStatus::NotFound as i32
        );
    }

    #[test]
    fn engine_lifecycle_destroys_existing_handle() {
        let handle = crate::mmdskin_engine_create();
        assert_ne!(handle, 0);
        assert_eq!(crate::mmdskin_engine_destroy(handle), NativeStatus::Ok as i32);
        assert_eq!(
            crate::mmdskin_engine_destroy(handle),
            NativeStatus::NotFound as i32
        );
    }

    #[test]
    fn model_load_rejects_missing_engine() {
        let mut model = 0_u64;
        let path = model_path_bytes("missing-engine.pmx");

        assert_eq!(
            unsafe { crate::mmdskin_model_load(999_999, path.as_ptr(), path.len(), &mut model) },
            NativeStatus::NotFound as i32
        );
        assert_eq!(model, 0);
    }

    #[test]
    fn model_load_rejects_invalid_arguments() {
        let engine = crate::mmdskin_engine_create();
        let mut model = 0_u64;
        let path = model_path_bytes("invalid-args.pmx");

        assert_eq!(
            unsafe { crate::mmdskin_model_load(engine, std::ptr::null(), path.len(), &mut model) },
            NativeStatus::InvalidArgument as i32
        );
        assert_eq!(
            unsafe { crate::mmdskin_model_load(engine, path.as_ptr(), 0, &mut model) },
            NativeStatus::InvalidArgument as i32
        );
        assert_eq!(
            unsafe {
                crate::mmdskin_model_load(engine, path.as_ptr(), path.len(), std::ptr::null_mut())
            },
            NativeStatus::InvalidArgument as i32
        );
        assert_eq!(crate::mmdskin_engine_destroy(engine), NativeStatus::Ok as i32);
    }

    #[test]
    fn model_load_creates_model_handle() {
        let engine = crate::mmdskin_engine_create();
        let path = write_temp_model("valid.pmx", b"PMX ");
        let mut model = 0_u64;

        assert_eq!(
            unsafe { crate::mmdskin_model_load(engine, path.as_ptr(), path.len(), &mut model) },
            NativeStatus::Ok as i32
        );
        assert_ne!(model, 0);
        assert_eq!(crate::mmdskin_model_kind(model), 11);
        let mut summary = [1_u32; 4];
        assert_eq!(
            unsafe { crate::mmdskin_model_summary(model, summary.as_mut_ptr(), summary.len()) },
            NativeStatus::Ok as i32
        );
        assert_eq!(summary, [0, 0, 0, 0]);
        assert_eq!(crate::mmdskin_model_destroy(model), NativeStatus::Ok as i32);
        assert_eq!(crate::mmdskin_engine_destroy(engine), NativeStatus::Ok as i32);
    }

    #[test]
    fn model_load_rejects_missing_file() {
        let engine = crate::mmdskin_engine_create();
        let path = model_path_bytes("missing-file.pmx");
        let mut model = 0_u64;

        assert_eq!(
            unsafe { crate::mmdskin_model_load(engine, path.as_ptr(), path.len(), &mut model) },
            NativeStatus::NotFound as i32
        );
        assert_eq!(model, 0);
        assert_eq!(crate::mmdskin_engine_destroy(engine), NativeStatus::Ok as i32);
    }

    #[test]
    fn model_load_rejects_unknown_format() {
        let engine = crate::mmdskin_engine_create();
        let path = write_temp_model("unknown.bin", b"not mmd");
        let mut model = 0_u64;

        assert_eq!(
            unsafe { crate::mmdskin_model_load(engine, path.as_ptr(), path.len(), &mut model) },
            NativeStatus::InvalidArgument as i32
        );
        assert_eq!(model, 0);
        assert_eq!(crate::mmdskin_engine_destroy(engine), NativeStatus::Ok as i32);
    }

    #[test]
    fn model_load_accepts_pmd_model_file() {
        let engine = crate::mmdskin_engine_create();
        let path = write_temp_model("valid.pmd", b"Pmd");
        let mut model = 0_u64;

        assert_eq!(
            unsafe { crate::mmdskin_model_load(engine, path.as_ptr(), path.len(), &mut model) },
            NativeStatus::Ok as i32
        );
        assert_ne!(model, 0);
        assert_eq!(crate::mmdskin_model_kind(model), 10);
        let mut summary = [1_u32; 4];
        assert_eq!(
            unsafe { crate::mmdskin_model_summary(model, summary.as_mut_ptr(), summary.len()) },
            NativeStatus::Ok as i32
        );
        assert_eq!(summary, [0, 0, 0, 0]);
        assert_eq!(crate::mmdskin_model_destroy(model), NativeStatus::Ok as i32);
        assert_eq!(crate::mmdskin_engine_destroy(engine), NativeStatus::Ok as i32);
    }

    #[test]
    fn model_load_reads_pmx_summary() {
        let engine = crate::mmdskin_engine_create();
        let bytes = fs::read(pmx_fixture_path()).unwrap();
        let path = write_temp_model("fixture.pmx", &bytes);
        let mut model = 0_u64;

        assert_eq!(
            unsafe { crate::mmdskin_model_load(engine, path.as_ptr(), path.len(), &mut model) },
            NativeStatus::Ok as i32
        );
        let mut summary = [0_u32; 4];
        assert_eq!(
            unsafe { crate::mmdskin_model_summary(model, summary.as_mut_ptr(), summary.len()) },
            NativeStatus::Ok as i32
        );
        assert_eq!(summary, [3, 3, 1, 3]);
        assert_eq!(crate::mmdskin_model_destroy(model), NativeStatus::Ok as i32);
        assert_eq!(crate::mmdskin_engine_destroy(engine), NativeStatus::Ok as i32);
    }

    #[test]
    fn jni_wrappers_delegate_to_c_abi() {
        assert_eq!(
            crate::Java_com_micheanl_model_client_nativebridge_MMDNative_nativeVersion(
                std::ptr::null_mut(),
                std::ptr::null_mut()
            ),
            crate::mmdskin_native_version()
        );
        let handle = crate::Java_com_micheanl_model_client_nativebridge_MMDNative_engineCreateRaw(
            std::ptr::null_mut(),
            std::ptr::null_mut(),
        );
        assert_ne!(handle, 0);
        assert_eq!(
            crate::Java_com_micheanl_model_client_nativebridge_MMDNative_engineDestroyRaw(
                std::ptr::null_mut(),
                std::ptr::null_mut(),
                handle
            ),
            NativeStatus::Ok as i32
        );
    }

    #[test]
    fn jni_model_wrappers_delegate_to_c_abi() {
        let engine = crate::Java_com_micheanl_model_client_nativebridge_MMDNative_engineCreateRaw(
            std::ptr::null_mut(),
            std::ptr::null_mut(),
        );
        let path = write_temp_model("jni.pmx", b"PMX ");
        let mut model = 0_i64;
        assert_eq!(
            unsafe {
                crate::Java_com_micheanl_model_client_nativebridge_MMDNative_modelLoadRawBytes(
                    std::ptr::null_mut(),
                    std::ptr::null_mut(),
                    engine,
                    path.as_ptr(),
                    path.len() as i32,
                    &mut model,
                )
            },
            NativeStatus::Ok as i32
        );
        assert_ne!(model, 0);
        assert_eq!(
            crate::Java_com_micheanl_model_client_nativebridge_MMDNative_modelDestroyRaw(
                std::ptr::null_mut(),
                std::ptr::null_mut(),
                model
            ),
            NativeStatus::Ok as i32
        );
        assert_eq!(
            crate::Java_com_micheanl_model_client_nativebridge_MMDNative_engineDestroyRaw(
                std::ptr::null_mut(),
                std::ptr::null_mut(),
                engine
            ),
            NativeStatus::Ok as i32
        );
    }

    fn model_path_bytes(file_name: &str) -> Vec<u8> {
        temp_model_path(file_name).to_string_lossy().into_owned().into_bytes()
    }

    fn write_temp_model(file_name: &str, bytes: &[u8]) -> Vec<u8> {
        let path = temp_model_path(file_name);
        fs::write(&path, bytes).unwrap();
        path.to_string_lossy().into_owned().into_bytes()
    }

    fn temp_model_path(file_name: &str) -> PathBuf {
        let nonce = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_nanos();
        std::env::temp_dir().join(format!(
            "mmdskin-native-{}-{}",
            nonce,
            file_name
        ))
    }

    fn pmx_fixture_path() -> PathBuf {
        let cargo_home = std::env::var_os("CARGO_HOME")
            .map(PathBuf::from)
            .unwrap_or_else(|| PathBuf::from(std::env::var_os("HOME").unwrap()).join(".cargo"));
        let registry_src = cargo_home.join("registry").join("src");
        for registry in fs::read_dir(registry_src).unwrap() {
            let registry = registry.unwrap().path();
            for package in fs::read_dir(registry).unwrap() {
                let package = package.unwrap().path();
                let Some(name) = package.file_name().and_then(|name| name.to_str()) else {
                    continue;
                };
                if name.starts_with("mmd-anim-format-") {
                    let path = package
                        .join("fixtures")
                        .join("pmx")
                        .join("ik_multi_axis_limit.pmx");
                    if path.exists() {
                        return path;
                    }
                }
            }
        }
        panic!("missing mmd-anim-format PMX fixture");
    }

}
