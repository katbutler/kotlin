// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
package o

@kotlin.internal.Returns(kotlin.internal.ConstantValue.FALSE)
fun isString(@kotlin.internal.Not @kotlin.internal.IsInstance(String::class) x: Any?) = x is String

@kotlin.internal.Returns(kotlin.internal.ConstantValue.TRUE)
fun notIsString(@kotlin.internal.Not @kotlin.internal.IsInstance(String::class) x: Any?) = x !is String

fun testSimple(x: Any?) {
    x.<!UNRESOLVED_REFERENCE!>length<!>

    if (isString(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun testSpilling(x: Any?) {
    x.<!UNRESOLVED_REFERENCE!>length<!>

    if (isString(x)) <!DEBUG_INFO_SMARTCAST!>x<!>.length

    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun testInversion(x: Any?) {
    if (notIsString(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun testInversionSpilling(x: Any?) {
    x.<!UNRESOLVED_REFERENCE!>length<!>

    if (notIsString(x)) else <!DEBUG_INFO_SMARTCAST!>x<!>.length

    x.<!UNRESOLVED_REFERENCE!>length<!>
}

