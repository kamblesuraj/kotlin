val nullable: String? = null
val listOfStrings = listOf("no lists, sorry")
val arrayOfIntObjects = arrayOf(2 + 3, -1, Int.MAX_VALUE, Int.MIN_VALUE)
val arrayOfIntPrimitives = intArrayOf(2 + 3, -1, Int.MAX_VALUE, Int.MIN_VALUE)
val arrayOfLongObjects = arrayOf(2L + 3L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)
val arrayOfLongPrimitives = longArrayOf(2L + 3L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)
val arrayOfCharObjects = arrayOf('C', 0.toChar(), '\n', '\r', '\'', 0xff.toChar())
val arrayOfCharPrimitives = charArrayOf('C', 0.toChar(), '\n', '\r', '\'', 0xff.toChar())
val arrayOfDoubleObjects = arrayOf(2.0 + 3.0, -1.23E-7, Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0 / 0.0, 1.0 / 0.0, -1.0 / 0.0)
val arrayOfDoublePrimitives = doubleArrayOf(2.0 + 3.0, -1.23E-7, Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0 / 0.0, 1.0 / 0.0, -1.0 / 0.0)
val arrayOfFloatObjects = arrayOf(2f + 3f, -1.23E-7f, Float.MIN_VALUE, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f / 0.0f, 1.0f / 0.0f, -1.0f / 0.0f)
val arrayOfFloatPrimitives = floatArrayOf(2f + 3f, -1.23E-7f, Float.MIN_VALUE, Float.MAX_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f / 0.0f, 1.0f / 0.0f, -1.0f / 0.0f)
val arrayOfStrings = arrayOf("", "quotes \" ''quotes", "\r\n", "你好世界")
val arrayOfEnums = arrayOf(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
val arrayOfNAny = arrayOf(1, 2L, "abc", AnnotationTarget.CLASS, null)