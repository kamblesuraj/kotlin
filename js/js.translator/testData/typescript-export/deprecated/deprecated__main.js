"use strict";
function assert(condition) {
    if (!condition) {
        throw "Assertion failed";
    }
}
function box() {
    assert(JS_TESTS.foo.bar == "Test");
    assert(JS_TESTS.foo.foo() == undefined);
    return "OK";
}
