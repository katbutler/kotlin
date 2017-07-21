// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

@Returns(ConstantValue.FALSE)
fun isString(@Not @IsInstance(String::class) x: Any?) = x is String

@Returns(ConstantValue.TRUE)
fun notIsString(@Not @IsInstance(String::class) x: Any?) = x !is String

@Returns(ConstantValue.TRUE)
fun notIsInt(@Not @IsInstance(Int::class) x: Any?) = x !is Int


fun testNeutralCondition(x: Any?) {
    if (((isString(x) && true) || false) && (1 == 1)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun textExplicitAlwaysFalse(x: Any?) {
    if ((isString(x) && false) || (1 == 1 && true == false)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun implicitAlwaysFalse(x: Any?) {
    if (isString(x) && !isString(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun implicitAlwaysFalseSpilling(x: Any?) {
    if (isString(x) && !isString(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun intersectingInfo(x: Any?, y: Any?) {
    if ((isString(x) && y is String) || (!notIsString(x) && !notIsInt(y))) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        y.<!UNRESOLVED_REFERENCE!>length<!>
        y.<!UNRESOLVED_REFERENCE!>inc<!>()
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        y.<!UNRESOLVED_REFERENCE!>length<!>
        y.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun intersectingInfo2(x: Any?, y: Any?) {
    // In each arg of "||"-operator presented fact "x is String" which should lead to smartcast.
    // Also there are 3 additional facts: "x is Int", "y is String", "y is Int". One
    // of them is absent in each arg of "||"-operator, so they *shouldn't* lead to smartcast

    if ((isString(x) && !notIsInt(x) && y is String) ||
        (!notIsString(x) && isString(y) && y is Int) ||
        (x is String && !notIsInt(y) && x is Int)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
        y.<!UNRESOLVED_REFERENCE!>length<!>
        y.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
    x.<!UNRESOLVED_REFERENCE!>inc<!>()
    y.<!UNRESOLVED_REFERENCE!>length<!>
    y.<!UNRESOLVED_REFERENCE!>inc<!>()
}
