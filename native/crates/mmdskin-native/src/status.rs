#[repr(i32)]
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum NativeStatus {
    Ok = 0,
    InvalidArgument = 1,
    NotFound = 2,
    InternalError = 3,
}
