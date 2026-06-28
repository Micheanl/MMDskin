mod handles;
mod status;

use std::ffi::c_void;

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

#[cfg(test)]
mod tests {
    use super::*;

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
}
