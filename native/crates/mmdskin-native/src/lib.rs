mod handles;
mod status;

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
}
