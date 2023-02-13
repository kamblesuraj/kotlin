// FILE: StubElement.java

import org.jetbrains.annotations.NotNull;

public interface StubElement<T extends PsiElement> {
    <E extends PsiElement> E @NotNull [] getChildrenByType(@NotNull String filter, final E[] array);
}

// FILE: PsiElement.java

public interface PsiElement

// FILE: StubBasedPsiElement.java

public interface StubBasedBsiElement<Stub extends StubElement> extends PsiElement {
    Stub getStub();
}

// FILE: test.kt

private val STRING_TEMPLATE_EMPTY_ARRAY = emptyArray<KtStringTemplateExpression>()

open class KtStringTemplateExpression : PsiElement

fun StubBasedBsiElement<*>.foo(): KtStringTemplateExpression? {
    stub?.let {
        val expressions = it.getChildrenByType("", STRING_TEMPLATE_EMPTY_ARRAY)
        return <!RETURN_TYPE_MISMATCH!>expressions.firstOrNull()<!>
    }
    return null
}
