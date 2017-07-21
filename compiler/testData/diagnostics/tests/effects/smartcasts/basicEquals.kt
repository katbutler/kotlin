// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

@kotlin.internal.Returns(kotlin.internal.ConstantValue.TRUE)
fun myEqualsNull(@kotlin.internal.Equals(kotlin.internal.ConstantValue.NULL) x: Int?) = x == null

@kotlin.internal.Returns(kotlin.internal.ConstantValue.FALSE)
fun myEqualsNotNull(@kotlin.internal.Equals(kotlin.internal.ConstantValue.NULL) x: Int?) = x != null

fun testBasicEquals(x: Int?) {
    x<!UNSAFE_CALL!>.<!>inc()

    if (myEqualsNull(x)) {
        x<!UNSAFE_CALL!>.<!>inc()
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }

    x<!UNSAFE_CALL!>.<!>inc()
}

fun testBasicNotEquals(x: Int?) {
    x<!UNSAFE_CALL!>.<!>inc()

    if (myEqualsNotNull(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    } else {
        x<!UNSAFE_CALL!>.<!>inc()
    }

    x<!UNSAFE_CALL!>.<!>inc()
}