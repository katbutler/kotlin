// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

@Throws()
fun myAssert(@Equals(ConstantValue.FALSE) condition: Boolean) {
    if (!condition) throw kotlin.IllegalArgumentException("Assertion failed")
}

@Returns(ConstantValue.FALSE)
fun isString(@Not @IsInstance(String::class) x: Any?) = x is String

@Returns(ConstantValue.FALSE)
fun isInt(@Not @IsInstance(Int::class) x: Any?) = x is Int

@Returns(ConstantValue.FALSE)
fun notEqualsNull(@Equals(ConstantValue.NULL) x: Any?) = x != null

@Returns(ConstantValue.FALSE)
fun equalsTrue(@Not @Equals(ConstantValue.TRUE) x: Boolean) = x == true

@Returns(ConstantValue.FALSE)
fun equalsFalse(@Not @Equals(ConstantValue.FALSE) x: Boolean) = x == false

@Returns(ConstantValue.NULL)
fun nullWhenNotString(@Not @IsInstance(String::class) x: Any?) = x as? String

fun nested1(x: Any?) {
    if (equalsTrue(isString(x))) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun nested2(x: Any?) {
    myAssert(equalsTrue(isString(x)))
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun nested3(x: Any?) {
    myAssert(equalsTrue(notEqualsNull(nullWhenNotString(x))))
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
}

fun branchedAndNested(x: Any?, y: Any?) {
    myAssert(equalsTrue(notEqualsNull(nullWhenNotString(x))) && equalsTrue(isString(y)))
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
    <!DEBUG_INFO_SMARTCAST!>y<!>.length
}


fun br(y: Any?) {
    if (myAssert(y is Int) == Unit && myAssert(y is String) == Unit) {
        <!DEBUG_INFO_SMARTCAST!>y<!>.length
        <!DEBUG_INFO_SMARTCAST!>y<!>.inc()
    }
}

fun branchedAndNestedWithNativeOperators(x: Any?, y: Any?) {
    myAssert(
            equalsTrue(notEqualsNull(nullWhenNotString(x)))   // x is String
            &&
            (
                    (myAssert(y is Int) == Unit && myAssert(y is String) == Unit)  // y is Int, String
                    ||
                    equalsTrue(isInt(y) && isString(y))                          // y is Int, String
            )
            &&
            (1 == 2 || <!USELESS_IS_CHECK!>y is Int<!> || isString(y))
    )
    <!DEBUG_INFO_SMARTCAST!>x<!>.length
    <!DEBUG_INFO_SMARTCAST!>y<!>.length
    <!DEBUG_INFO_SMARTCAST!>y<!>.inc()
}

