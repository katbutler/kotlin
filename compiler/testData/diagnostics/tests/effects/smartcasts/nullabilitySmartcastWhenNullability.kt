// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

@Returns(ConstantValue.NULL)
fun nullWhenNull(@Equals(ConstantValue.NULL) x: Int?) = x?.inc()

fun testNullWhenNull(x: Int?) {
    if (nullWhenNull(x) == null) {
        x<!UNSAFE_CALL!>.<!>dec()
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.dec()
    }

    if (nullWhenNull(x) != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.dec()
    } else {
        x<!UNSAFE_CALL!>.<!>dec()
    }

    x<!UNSAFE_CALL!>.<!>dec()
}

// NB. it is the same function as `nullWhenNull`, but annotations specifies other facet of the function behaviour
@Returns(ConstantValue.NOT_NULL)
fun notNullWhenNotNull (@Equals(ConstantValue.NOT_NULL) x: Int?) = x?.inc()

fun testNotNullWhenNotNull (x: Int?) {
    if (notNullWhenNotNull(x) == null) {
        x == null
    } else {
        // Note that we don't get a smartcast here because we get schema:
        // x != null -> Returns(false)
        // so we can't reason that x is definitely not null here
        x<!UNSAFE_CALL!>.<!>dec()
    }

    if (notNullWhenNotNull(x) != null) {
        // Note that we don't get a smartcast here because we get schema:
        // x != null -> Returns(true)
        // so we can't reason that x is definitely not null here
        x<!UNSAFE_CALL!>.<!>dec()
    } else {
        x == null
    }

    x<!UNSAFE_CALL!>.<!>dec()
}