import kotlin.native.concurrent.Worker
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@BeforeClass
fun testTopBeforeClass() {
    println("Top level BEFORE CLASS")
}

@AfterClass
fun testTopAfterClass() {
    println("Top level AFTER CLASS")
}

@BeforeTest
fun testTopLevelBefore() {
    println("Top level BEFORE")
}

@AfterTest
fun testTopLevelAfter() {
    println("Top level AFTER")
}

@Test
fun testTopOne() {
    println("Top level ONE")
}

@Test
fun testTopTwo() {
    println("Top level TWO")
}

class MyTest {
    companion object {
        @BeforeClass
        fun beforeClass() {
            println("Setup @BeforeClass")
        }

        @AfterClass
        fun afterClass() {
            println("After @AfterClass")
        }
    }

    @BeforeTest
    fun beforeTest() {
        println("Setup @BeforeTest")
    }

    @Test
    fun testABC() {
        println("Test @Test ABC")
        // Wait a sec to see that test is actually working in the suite, not during creation
        Worker.current.park(1.seconds.toLong(DurationUnit.MICROSECONDS))
    }

    @Test
    fun testOther() {
        println("Test @Test Other")
    }

    @Test
    fun testFailed() {
        println("Failed started")
        assertTrue(false, "Kotlin assertion failed")
        println("Failed ended")
    }

    @AfterTest
    fun afterTest() {
        println("After @AfterTest")
    }
}