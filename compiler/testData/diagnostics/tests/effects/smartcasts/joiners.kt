// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

@Returns(ConstantValue.TRUE)
@JoinConditions(JoiningStrategy.ANY)
fun anyIsNotString(@Not @IsInstance(String::class) x: Any?,
                   @Not @IsInstance(String::class) y: Any?,
                   @Not @IsInstance(String::class)z: Any?)
        = x !is String || y !is String || z !is String

fun testAnyJoiner(x: Any?, y: Any?, z: Any?) {
    if (anyIsNotString(x, y, z)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
    }
}

@Returns(ConstantValue.TRUE)
@JoinConditions(JoiningStrategy.NONE)   // pretty pointless, just for testing purposes
fun notIsString(@IsInstance(String::class) x: Any?) = x !is String

fun testNoneJoiner(x: Any?) {
    if (notIsString(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}