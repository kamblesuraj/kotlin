function assert(condition: boolean) {
    if (!condition) {
        throw "Assertion failed";
    }
}

function box(): string {
    assert(JS_TESTS.foo.bar == "Test");
    assert(JS_TESTS.foo.foo() == undefined);

    return "OK";
}