mod handles;
mod status;

use jni::objects::{JClass, JFloatArray, JIntArray, JLongArray, JString};
use jni::{errors::Result as JniResult, EnvUnowned, Outcome};
use mmd_anim::format::{
    MmdFormatKind, detect_mmd_format, parse_pmd_model, parse_pmx_model, parse_vmd_animation,
};
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
    let (kind, summary, mesh, skeleton) = match detect_mmd_format(&data, Some(path)) {
        MmdFormatKind::Pmd => match parse_pmd_model(&data) {
            Ok(model) => (
                handles::ModelKind::Pmd,
                pmd_summary(&model),
                pmd_mesh(&model),
                pmd_skeleton(&model),
            ),
            Err(_) => (
                handles::ModelKind::Pmd,
                handles::ModelSummary::default(),
                handles::ModelMesh::default(),
                handles::ModelSkeleton::default(),
            ),
        },
        MmdFormatKind::Pmx => match parse_pmx_model(&data) {
            Ok(model) => (
                handles::ModelKind::Pmx,
                pmx_summary(&model),
                pmx_mesh(&model),
                pmx_skeleton(&model),
            ),
            Err(_) => (
                handles::ModelKind::Pmx,
                handles::ModelSummary::default(),
                handles::ModelMesh::default(),
                handles::ModelSkeleton::default(),
            ),
        },
        _ => return NativeStatus::InvalidArgument as i32,
    };
    let model = handles::create_model(engine, kind, summary, mesh, skeleton);
    unsafe {
        *out_model = model;
    }
    NativeStatus::Ok as i32
}

fn pmd_summary(model: &mmd_anim::format::PmdParsedModel) -> handles::ModelSummary {
    handles::ModelSummary {
        vertices: model.metadata.counts.vertices as u32,
        indices: model.geometry.indices.len() as u32,
        materials: model.metadata.counts.materials as u32,
        bones: model.metadata.counts.bones as u32,
    }
}

fn pmx_summary(model: &mmd_anim::format::PmxParsedModel) -> handles::ModelSummary {
    handles::ModelSummary {
        vertices: model.metadata.counts.vertices as u32,
        indices: model.geometry.indices.len() as u32,
        materials: model.metadata.counts.materials as u32,
        bones: model.metadata.counts.bones as u32,
    }
}

fn pmd_mesh(model: &mmd_anim::format::PmdParsedModel) -> handles::ModelMesh {
    let mut positions = Vec::with_capacity(model.geometry.vertices.len() * 3);
    let mut normals = Vec::with_capacity(model.geometry.vertices.len() * 3);
    let mut uvs = Vec::with_capacity(model.geometry.vertices.len() * 2);
    for vertex in &model.geometry.vertices {
        positions.extend_from_slice(&vertex.position);
        normals.extend_from_slice(&vertex.normal);
        uvs.extend_from_slice(&vertex.uv);
    }
    let mut material_starts = Vec::with_capacity(model.materials.len());
    let mut material_counts = Vec::with_capacity(model.materials.len());
    let mut material_alphas = Vec::with_capacity(model.materials.len());
    let mut start = 0_u32;
    for material in &model.materials {
        let count = material.face_count.saturating_mul(3);
        material_starts.push(start);
        material_counts.push(count);
        material_alphas.push(material.diffuse[3]);
        start = start.saturating_add(count);
    }
    handles::ModelMesh {
        positions,
        normals,
        uvs,
        indices: model.geometry.indices.iter().map(|&index| u32::from(index)).collect(),
        material_starts,
        material_counts,
        material_alphas,
    }
}

fn pmd_skeleton(model: &mmd_anim::format::PmdParsedModel) -> handles::ModelSkeleton {
    let mut parent_indices = Vec::with_capacity(model.skeleton.bones.len());
    let mut positions = Vec::with_capacity(model.skeleton.bones.len() * 3);
    for bone in &model.skeleton.bones {
        parent_indices.push(bone.parent_index);
        positions.extend_from_slice(&bone.position);
    }
    let mut skin_indices = Vec::with_capacity(model.geometry.vertices.len() * 4);
    let mut skin_weights = Vec::with_capacity(model.geometry.vertices.len() * 4);
    for vertex in &model.geometry.vertices {
        let first_weight = f32::from(vertex.bone_weight) / 100.0;
        skin_indices.push(nonnegative_u32(vertex.bone_indices[0]));
        skin_indices.push(nonnegative_u32(vertex.bone_indices[1]));
        skin_indices.push(0);
        skin_indices.push(0);
        skin_weights.push(first_weight);
        skin_weights.push(1.0 - first_weight);
        skin_weights.push(0.0);
        skin_weights.push(0.0);
    }
    handles::ModelSkeleton {
        parent_indices,
        positions,
        skin_indices,
        skin_weights,
    }
}

fn pmx_mesh(model: &mmd_anim::format::PmxParsedModel) -> handles::ModelMesh {
    let mut material_starts = Vec::with_capacity(model.geometry.material_groups.len());
    let mut material_counts = Vec::with_capacity(model.geometry.material_groups.len());
    let mut material_alphas = Vec::with_capacity(model.geometry.material_groups.len());
    for group in &model.geometry.material_groups {
        material_starts.push(group.start as u32);
        material_counts.push(group.count as u32);
        let alpha = model
            .materials
            .get(group.material_index)
            .map(|material| material.diffuse[3])
            .unwrap_or(1.0);
        material_alphas.push(alpha);
    }
    handles::ModelMesh {
        positions: model.geometry.positions.clone(),
        normals: model.geometry.normals.clone(),
        uvs: model.geometry.uvs.clone(),
        indices: model.geometry.indices.clone(),
        material_starts,
        material_counts,
        material_alphas,
    }
}

fn pmx_skeleton(model: &mmd_anim::format::PmxParsedModel) -> handles::ModelSkeleton {
    let mut parent_indices = Vec::with_capacity(model.skeleton.bones.len());
    let mut positions = Vec::with_capacity(model.skeleton.bones.len() * 3);
    for bone in &model.skeleton.bones {
        parent_indices.push(bone.parent_index);
        positions.extend_from_slice(&bone.position);
    }
    handles::ModelSkeleton {
        parent_indices,
        positions,
        skin_indices: model.geometry.skin_indices.clone(),
        skin_weights: model.geometry.skin_weights.clone(),
    }
}

fn nonnegative_u32(value: i32) -> u32 {
    if value < 0 { 0 } else { value as u32 }
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
pub unsafe extern "C" fn mmdskin_model_mesh_counts(
    handle: u64,
    out_counts: *mut u32,
    out_len: usize,
) -> i32 {
    if handle == 0 || out_counts.is_null() || out_len < 5 {
        return NativeStatus::InvalidArgument as i32;
    }
    let Some(mesh) = handles::model_mesh(handle) else {
        return NativeStatus::NotFound as i32;
    };
    let out = unsafe { slice::from_raw_parts_mut(out_counts, out_len) };
    out[0] = mesh.positions.len() as u32;
    out[1] = mesh.normals.len() as u32;
    out[2] = mesh.uvs.len() as u32;
    out[3] = mesh.indices.len() as u32;
    out[4] = mesh.material_starts.len() as u32;
    NativeStatus::Ok as i32
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mmdskin_model_mesh_read(
    handle: u64,
    out_positions: *mut f32,
    out_positions_len: usize,
    out_normals: *mut f32,
    out_normals_len: usize,
    out_uvs: *mut f32,
    out_uvs_len: usize,
    out_indices: *mut u32,
    out_indices_len: usize,
    out_material_starts: *mut u32,
    out_material_starts_len: usize,
    out_material_counts: *mut u32,
    out_material_counts_len: usize,
    out_material_alphas: *mut f32,
    out_material_alphas_len: usize,
) -> i32 {
    if handle == 0
        || out_positions.is_null()
        || out_normals.is_null()
        || out_uvs.is_null()
        || out_indices.is_null()
        || out_material_starts.is_null()
        || out_material_counts.is_null()
        || out_material_alphas.is_null()
    {
        return NativeStatus::InvalidArgument as i32;
    }
    let Some(mesh) = handles::model_mesh(handle) else {
        return NativeStatus::NotFound as i32;
    };
    if out_positions_len < mesh.positions.len()
        || out_normals_len < mesh.normals.len()
        || out_uvs_len < mesh.uvs.len()
        || out_indices_len < mesh.indices.len()
        || out_material_starts_len < mesh.material_starts.len()
        || out_material_counts_len < mesh.material_counts.len()
        || out_material_alphas_len < mesh.material_alphas.len()
    {
        return NativeStatus::InvalidArgument as i32;
    }
    unsafe {
        slice::from_raw_parts_mut(out_positions, mesh.positions.len())
            .copy_from_slice(&mesh.positions);
        slice::from_raw_parts_mut(out_normals, mesh.normals.len()).copy_from_slice(&mesh.normals);
        slice::from_raw_parts_mut(out_uvs, mesh.uvs.len()).copy_from_slice(&mesh.uvs);
        slice::from_raw_parts_mut(out_indices, mesh.indices.len()).copy_from_slice(&mesh.indices);
        slice::from_raw_parts_mut(out_material_starts, mesh.material_starts.len())
            .copy_from_slice(&mesh.material_starts);
        slice::from_raw_parts_mut(out_material_counts, mesh.material_counts.len())
            .copy_from_slice(&mesh.material_counts);
        slice::from_raw_parts_mut(out_material_alphas, mesh.material_alphas.len())
            .copy_from_slice(&mesh.material_alphas);
    }
    NativeStatus::Ok as i32
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mmdskin_model_skeleton_counts(
    handle: u64,
    out_counts: *mut u32,
    out_len: usize,
) -> i32 {
    if handle == 0 || out_counts.is_null() || out_len < 4 {
        return NativeStatus::InvalidArgument as i32;
    }
    let Some(skeleton) = handles::model_skeleton(handle) else {
        return NativeStatus::NotFound as i32;
    };
    let out = unsafe { slice::from_raw_parts_mut(out_counts, out_len) };
    out[0] = skeleton.parent_indices.len() as u32;
    out[1] = skeleton.positions.len() as u32;
    out[2] = skeleton.skin_indices.len() as u32;
    out[3] = skeleton.skin_weights.len() as u32;
    NativeStatus::Ok as i32
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mmdskin_model_skeleton_read(
    handle: u64,
    out_parent_indices: *mut i32,
    out_parent_indices_len: usize,
    out_positions: *mut f32,
    out_positions_len: usize,
    out_skin_indices: *mut u32,
    out_skin_indices_len: usize,
    out_skin_weights: *mut f32,
    out_skin_weights_len: usize,
) -> i32 {
    if handle == 0
        || out_parent_indices.is_null()
        || out_positions.is_null()
        || out_skin_indices.is_null()
        || out_skin_weights.is_null()
    {
        return NativeStatus::InvalidArgument as i32;
    }
    let Some(skeleton) = handles::model_skeleton(handle) else {
        return NativeStatus::NotFound as i32;
    };
    if out_parent_indices_len < skeleton.parent_indices.len()
        || out_positions_len < skeleton.positions.len()
        || out_skin_indices_len < skeleton.skin_indices.len()
        || out_skin_weights_len < skeleton.skin_weights.len()
    {
        return NativeStatus::InvalidArgument as i32;
    }
    unsafe {
        slice::from_raw_parts_mut(out_parent_indices, skeleton.parent_indices.len())
            .copy_from_slice(&skeleton.parent_indices);
        slice::from_raw_parts_mut(out_positions, skeleton.positions.len())
            .copy_from_slice(&skeleton.positions);
        slice::from_raw_parts_mut(out_skin_indices, skeleton.skin_indices.len())
            .copy_from_slice(&skeleton.skin_indices);
        slice::from_raw_parts_mut(out_skin_weights, skeleton.skin_weights.len())
            .copy_from_slice(&skeleton.skin_weights);
    }
    NativeStatus::Ok as i32
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn mmdskin_animation_summary(
    path: *const u8,
    path_len: usize,
    out_summary: *mut u32,
    out_len: usize,
) -> i32 {
    if path.is_null() || path_len == 0 || out_summary.is_null() || out_len < 7 {
        return NativeStatus::InvalidArgument as i32;
    }
    let bytes = unsafe { slice::from_raw_parts(path, path_len) };
    let Ok(path) = str::from_utf8(bytes) else {
        return NativeStatus::InvalidArgument as i32;
    };
    let Ok(data) = fs::read(path) else {
        return NativeStatus::NotFound as i32;
    };
    if detect_mmd_format(&data, Some(path)) != MmdFormatKind::Vmd {
        return NativeStatus::InvalidArgument as i32;
    }
    let Ok(animation) = parse_vmd_animation(&data) else {
        return NativeStatus::InvalidArgument as i32;
    };
    let out = unsafe { slice::from_raw_parts_mut(out_summary, out_len) };
    out[0] = animation.metadata.max_frame;
    out[1] = animation.metadata.counts.bones as u32;
    out[2] = animation.metadata.counts.morphs as u32;
    out[3] = animation.metadata.counts.cameras as u32;
    out[4] = animation.metadata.counts.lights as u32;
    out[5] = animation.metadata.counts.self_shadows as u32;
    out[6] = animation.metadata.counts.properties as u32;
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

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_modelMeshCountsRaw(
    mut env: EnvUnowned<'_>,
    _class: JClass<'_>,
    handle: i64,
    out_counts: JLongArray<'_>,
) -> i32 {
    if handle < 0 || out_counts.is_null() {
        return NativeStatus::InvalidArgument as i32;
    }
    match env
        .with_env(|env| -> JniResult<i32> {
            if out_counts.len(env)? < 5 {
                return Ok(NativeStatus::InvalidArgument as i32);
            }
            let mut counts = [0_u32; 5];
            let status = unsafe {
                mmdskin_model_mesh_counts(handle as u64, counts.as_mut_ptr(), counts.len())
            };
            if status == NativeStatus::Ok as i32 {
                out_counts.set_region(
                    env,
                    0,
                    &[
                        counts[0] as i64,
                        counts[1] as i64,
                        counts[2] as i64,
                        counts[3] as i64,
                        counts[4] as i64,
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

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_modelMeshReadRaw(
    mut env: EnvUnowned<'_>,
    _class: JClass<'_>,
    handle: i64,
    out_positions: JFloatArray<'_>,
    out_normals: JFloatArray<'_>,
    out_uvs: JFloatArray<'_>,
    out_indices: JIntArray<'_>,
    out_material_starts: JIntArray<'_>,
    out_material_counts: JIntArray<'_>,
    out_material_alphas: JFloatArray<'_>,
) -> i32 {
    if handle < 0
        || out_positions.is_null()
        || out_normals.is_null()
        || out_uvs.is_null()
        || out_indices.is_null()
        || out_material_starts.is_null()
        || out_material_counts.is_null()
        || out_material_alphas.is_null()
    {
        return NativeStatus::InvalidArgument as i32;
    }
    match env
        .with_env(|env| -> JniResult<i32> {
            let positions_len = out_positions.len(env)? as usize;
            let normals_len = out_normals.len(env)? as usize;
            let uvs_len = out_uvs.len(env)? as usize;
            let indices_len = out_indices.len(env)? as usize;
            let material_starts_len = out_material_starts.len(env)? as usize;
            let material_counts_len = out_material_counts.len(env)? as usize;
            let material_alphas_len = out_material_alphas.len(env)? as usize;
            let mut positions = vec![0.0_f32; positions_len];
            let mut normals = vec![0.0_f32; normals_len];
            let mut uvs = vec![0.0_f32; uvs_len];
            let mut indices = vec![0_u32; indices_len];
            let mut material_starts = vec![0_u32; material_starts_len];
            let mut material_counts = vec![0_u32; material_counts_len];
            let mut material_alphas = vec![0.0_f32; material_alphas_len];
            let status = unsafe {
                mmdskin_model_mesh_read(
                    handle as u64,
                    positions.as_mut_ptr(),
                    positions.len(),
                    normals.as_mut_ptr(),
                    normals.len(),
                    uvs.as_mut_ptr(),
                    uvs.len(),
                    indices.as_mut_ptr(),
                    indices.len(),
                    material_starts.as_mut_ptr(),
                    material_starts.len(),
                    material_counts.as_mut_ptr(),
                    material_counts.len(),
                    material_alphas.as_mut_ptr(),
                    material_alphas.len(),
                )
            };
            if status == NativeStatus::Ok as i32 {
                let indices = indices.iter().map(|&value| value as i32).collect::<Vec<_>>();
                let material_starts = material_starts
                    .iter()
                    .map(|&value| value as i32)
                    .collect::<Vec<_>>();
                let material_counts = material_counts
                    .iter()
                    .map(|&value| value as i32)
                    .collect::<Vec<_>>();
                out_positions.set_region(env, 0, &positions)?;
                out_normals.set_region(env, 0, &normals)?;
                out_uvs.set_region(env, 0, &uvs)?;
                out_indices.set_region(env, 0, &indices)?;
                out_material_starts.set_region(env, 0, &material_starts)?;
                out_material_counts.set_region(env, 0, &material_counts)?;
                out_material_alphas.set_region(env, 0, &material_alphas)?;
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
pub unsafe extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_modelSkeletonCountsRaw(
    mut env: EnvUnowned<'_>,
    _class: JClass<'_>,
    handle: i64,
    out_counts: JLongArray<'_>,
) -> i32 {
    if handle < 0 || out_counts.is_null() {
        return NativeStatus::InvalidArgument as i32;
    }
    match env
        .with_env(|env| -> JniResult<i32> {
            if out_counts.len(env)? < 4 {
                return Ok(NativeStatus::InvalidArgument as i32);
            }
            let mut counts = [0_u32; 4];
            let status = unsafe {
                mmdskin_model_skeleton_counts(handle as u64, counts.as_mut_ptr(), counts.len())
            };
            if status == NativeStatus::Ok as i32 {
                out_counts.set_region(
                    env,
                    0,
                    &[
                        counts[0] as i64,
                        counts[1] as i64,
                        counts[2] as i64,
                        counts[3] as i64,
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

#[unsafe(no_mangle)]
pub unsafe extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_modelSkeletonReadRaw(
    mut env: EnvUnowned<'_>,
    _class: JClass<'_>,
    handle: i64,
    out_parent_indices: JIntArray<'_>,
    out_positions: JFloatArray<'_>,
    out_skin_indices: JIntArray<'_>,
    out_skin_weights: JFloatArray<'_>,
) -> i32 {
    if handle < 0
        || out_parent_indices.is_null()
        || out_positions.is_null()
        || out_skin_indices.is_null()
        || out_skin_weights.is_null()
    {
        return NativeStatus::InvalidArgument as i32;
    }
    match env
        .with_env(|env| -> JniResult<i32> {
            let parent_indices_len = out_parent_indices.len(env)? as usize;
            let positions_len = out_positions.len(env)? as usize;
            let skin_indices_len = out_skin_indices.len(env)? as usize;
            let skin_weights_len = out_skin_weights.len(env)? as usize;
            let mut parent_indices = vec![0_i32; parent_indices_len];
            let mut positions = vec![0.0_f32; positions_len];
            let mut skin_indices = vec![0_u32; skin_indices_len];
            let mut skin_weights = vec![0.0_f32; skin_weights_len];
            let status = unsafe {
                mmdskin_model_skeleton_read(
                    handle as u64,
                    parent_indices.as_mut_ptr(),
                    parent_indices.len(),
                    positions.as_mut_ptr(),
                    positions.len(),
                    skin_indices.as_mut_ptr(),
                    skin_indices.len(),
                    skin_weights.as_mut_ptr(),
                    skin_weights.len(),
                )
            };
            if status == NativeStatus::Ok as i32 {
                let skin_indices = skin_indices
                    .iter()
                    .map(|&value| value as i32)
                    .collect::<Vec<_>>();
                out_parent_indices.set_region(env, 0, &parent_indices)?;
                out_positions.set_region(env, 0, &positions)?;
                out_skin_indices.set_region(env, 0, &skin_indices)?;
                out_skin_weights.set_region(env, 0, &skin_weights)?;
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
pub unsafe extern "system" fn Java_com_micheanl_model_client_nativebridge_MMDNative_animationSummaryRaw(
    mut env: EnvUnowned<'_>,
    _class: JClass<'_>,
    path: JString<'_>,
    out_summary: JLongArray<'_>,
) -> i32 {
    if path.is_null() || out_summary.is_null() {
        return NativeStatus::InvalidArgument as i32;
    }
    match env
        .with_env(|env| -> JniResult<i32> {
            if out_summary.len(env)? < 7 {
                return Ok(NativeStatus::InvalidArgument as i32);
            }
            let path = path.try_to_string(env)?;
            let mut summary = [0_u32; 7];
            let status = unsafe {
                mmdskin_animation_summary(path.as_ptr(), path.len(), summary.as_mut_ptr(), summary.len())
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
                        summary[4] as i64,
                        summary[5] as i64,
                        summary[6] as i64,
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
    fn model_load_reads_pmx_mesh() {
        let engine = crate::mmdskin_engine_create();
        let bytes = fs::read(pmx_fixture_path()).unwrap();
        let path = write_temp_model("mesh.pmx", &bytes);
        let mut model = 0_u64;

        assert_eq!(
            unsafe { crate::mmdskin_model_load(engine, path.as_ptr(), path.len(), &mut model) },
            NativeStatus::Ok as i32
        );

        let mut counts = [0_u32; 5];
        assert_eq!(
            unsafe { crate::mmdskin_model_mesh_counts(model, counts.as_mut_ptr(), counts.len()) },
            NativeStatus::Ok as i32
        );
        assert_eq!(counts, [9, 9, 6, 3, 1]);

        let mut positions = vec![0.0_f32; counts[0] as usize];
        let mut normals = vec![0.0_f32; counts[1] as usize];
        let mut uvs = vec![0.0_f32; counts[2] as usize];
        let mut indices = vec![0_u32; counts[3] as usize];
        let mut material_starts = vec![0_u32; counts[4] as usize];
        let mut material_counts = vec![0_u32; counts[4] as usize];
        let mut material_alphas = vec![0.0_f32; counts[4] as usize];

        assert_eq!(
            unsafe {
                crate::mmdskin_model_mesh_read(
                    model,
                    positions.as_mut_ptr(),
                    positions.len(),
                    normals.as_mut_ptr(),
                    normals.len(),
                    uvs.as_mut_ptr(),
                    uvs.len(),
                    indices.as_mut_ptr(),
                    indices.len(),
                    material_starts.as_mut_ptr(),
                    material_starts.len(),
                    material_counts.as_mut_ptr(),
                    material_counts.len(),
                    material_alphas.as_mut_ptr(),
                    material_alphas.len(),
                )
            },
            NativeStatus::Ok as i32
        );
        assert_eq!(indices, [0, 1, 2]);
        assert_eq!(material_starts, [0]);
        assert_eq!(material_counts, [3]);
        assert_eq!(material_alphas, [1.0]);

        assert_eq!(crate::mmdskin_model_destroy(model), NativeStatus::Ok as i32);
        assert_eq!(crate::mmdskin_engine_destroy(engine), NativeStatus::Ok as i32);
    }

    #[test]
    fn model_load_reads_pmx_skeleton() {
        let engine = crate::mmdskin_engine_create();
        let bytes = fs::read(pmx_fixture_path()).unwrap();
        let path = write_temp_model("skeleton.pmx", &bytes);
        let mut model = 0_u64;

        assert_eq!(
            unsafe { crate::mmdskin_model_load(engine, path.as_ptr(), path.len(), &mut model) },
            NativeStatus::Ok as i32
        );

        let mut counts = [0_u32; 4];
        assert_eq!(
            unsafe {
                crate::mmdskin_model_skeleton_counts(model, counts.as_mut_ptr(), counts.len())
            },
            NativeStatus::Ok as i32
        );
        assert_eq!(counts, [3, 9, 12, 12]);

        let mut parent_indices = vec![0_i32; counts[0] as usize];
        let mut positions = vec![0.0_f32; counts[1] as usize];
        let mut skin_indices = vec![0_u32; counts[2] as usize];
        let mut skin_weights = vec![0.0_f32; counts[3] as usize];

        assert_eq!(
            unsafe {
                crate::mmdskin_model_skeleton_read(
                    model,
                    parent_indices.as_mut_ptr(),
                    parent_indices.len(),
                    positions.as_mut_ptr(),
                    positions.len(),
                    skin_indices.as_mut_ptr(),
                    skin_indices.len(),
                    skin_weights.as_mut_ptr(),
                    skin_weights.len(),
                )
            },
            NativeStatus::Ok as i32
        );
        assert_eq!(parent_indices[0], -1);
        assert_eq!(skin_indices.len(), skin_weights.len());

        assert_eq!(crate::mmdskin_model_destroy(model), NativeStatus::Ok as i32);
        assert_eq!(crate::mmdskin_engine_destroy(engine), NativeStatus::Ok as i32);
    }

    #[test]
    fn reads_vmd_animation_summary() {
        let path = vmd_fixture_path();
        let path = path.to_string_lossy().into_owned().into_bytes();
        let mut summary = [0_u32; 7];

        assert_eq!(
            unsafe {
                crate::mmdskin_animation_summary(path.as_ptr(), path.len(), summary.as_mut_ptr(), summary.len())
            },
            NativeStatus::Ok as i32
        );
        assert!(summary[0] > 0);
        assert!(summary[1] > 0 || summary[3] > 0);
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

    fn vmd_fixture_path() -> PathBuf {
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
                        .join("vmd")
                        .join("ik_multi_bone_nondefault.vmd");
                    if path.exists() {
                        return path;
                    }
                }
            }
        }
        panic!("missing mmd-anim-format VMD fixture");
    }

}
