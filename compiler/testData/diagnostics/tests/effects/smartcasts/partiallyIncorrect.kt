// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

@kotlin.internal.Returns(kotlin.internal.ConstantValue.FALSE)
fun isString(@kotlin.internal.Not @kotlin.internal.IsInstance(String::class) x: Any?) = x is String

fun incorrectPartDoesntMatter(x: Any?) {
    if (isString(x) && <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}