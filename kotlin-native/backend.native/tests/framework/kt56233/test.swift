import Kt56233

func threadRoutine(pointer: UnsafeMutableRawPointer) -> UnsafeMutableRawPointer? {
    autoreleasepool {
        let f = pointer.bindMemory(to: (() -> ()).self, capacity: 1).pointee
        f()
    }
    return nil
}

func launchThreads(
    _ f: @escaping () -> (),
    threadCount: Int = 4
) throws {
    var threads: [pthread_t] = []
    for _ in 0..<threadCount {
        let fPtr = UnsafeMutablePointer<() -> ()>.allocate(capacity: 1)
        fPtr.initialize(to: f)
        var thread: pthread_t? = nil
        let result = pthread_create(&thread, nil, threadRoutine, fPtr)
        try assertEquals(actual: result, expected: 0)
        threads.append(thread!)
    }
    for thread in threads {
        pthread_join(thread, nil)
    }
}

func enums() {
    // Stress testing for race conditions.
    for _ in 0..<50000000 {
        _ = Kt56233.SimpleEnum.two.ordinal
    }
}

class WeakX {
    weak var x: X?
    init(num: Int32) {
        self.x = Kt56233.KnlibraryKt.getX(num: num)
    }
}

func weaks(_ weakX: WeakX) {
    for i in 0..<50000000 {
        autoreleasepool {
            if let x = weakX.x {
                try! assertEquals(actual: x.num, expected: 42)
            } else {
                return
            }
        }
        if i % 100 == 0 {
            Kt56233.KnlibraryKt.scheduleGC()
        }
    }
}

// -------- Execution of the test --------

class TestTests : SimpleTestProvider {
    override init() {
        super.init()

        // test("Kt56233_enums", { try launchThreads(enums) })
        test("Kt56233_weaks", {
            let weakX = WeakX(num: 42)
            try launchThreads({
                weaks(weakX)
            })
        })
    }
}
