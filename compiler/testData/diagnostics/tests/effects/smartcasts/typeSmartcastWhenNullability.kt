// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*


// x is String -> Returns(NULL) <- useless in annotation
// <=> (in this particular case)
// x !is String -> Returns(NOT_NULL)
@Returns(ConstantValue.NOT_NULL)
fun nullWhenString(@Not @IsInstance(String::class) x: Any?) = if (x is String) null else 42

fun testNullWhenString(x: Any?) {
    if (nullWhenString(x) == null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun testNullWhenStringInversed(x: Any?) {
    if (nullWhenString(x) != null) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

// x !is String -> Returns(NULL)
// <=> (in this particular case)
// x is String -> Returns(NOT_NULL) <- useless in annotation
@Returns(ConstantValue.NULL)
fun nullWhenNotString(@Not @IsInstance(String::class) x: Any?) = if (x !is String) null else 42

fun testNullNotWhenString(x: Any?) {
    if (nullWhenNotString(x) == null) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun testNullWhenNotString(x: Any?) {
    if (nullWhenNotString(x) != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}