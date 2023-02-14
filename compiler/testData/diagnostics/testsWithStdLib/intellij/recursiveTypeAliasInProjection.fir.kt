private typealias Bar = <!RECURSIVE_TYPEALIAS_EXPANSION!>Foo<Gau><!>

internal class Gau : <!CYCLIC_INHERITANCE_HIERARCHY!>Bar<!>

interface Foo<T>
