declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        /** @deprecated */
        const bar: string;
        /** @deprecated */
        function foo(): void;
        /** @deprecated */
        class TestClass {
            constructor();
        }
        class AnotherClass {
            /** @deprecated */
            constructor(value: string);
            get value(): string;
            /** @deprecated */
            static fromNothing(): foo.AnotherClass;
            static fromInt(value: number): foo.AnotherClass;
            /** @deprecated */
            foo(): void;
            baz(): void;
            /** @deprecated */
            get bar(): string;
        }
        interface TestInterface {
            /** @deprecated */
            foo(): void;
            bar(): void;
            /** @deprecated */
            readonly baz: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.TestInterface": unique symbol;
            };
        }
        const TestObject: {
            /** @deprecated */
            foo(): void;
            bar(): void;
            /** @deprecated */
            get baz(): string;
        };
    }
}
