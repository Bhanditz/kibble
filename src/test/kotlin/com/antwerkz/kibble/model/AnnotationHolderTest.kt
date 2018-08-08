package com.antwerkz.kibble.model

import com.antwerkz.kibble.Kibble
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File
import java.lang.annotation.Retention


class AnnotationHolderTest {
    @Test
    fun properties() {
        val property = Kibble.parseSource("""
        import java.lang.annotation.Retention

        @Retention
        val name: String""")
                .properties[0]
        Assert.assertTrue(property.hasAnnotation(Retention::class.java))
    }

    @Test
    fun classes() {
        val file = Kibble.parseSource("""
        @SuppressWarnings("deprecation", count=10, foo=@Foo(42))
        class Foo """)

        val klass = file
                .classes.first()
        Assert.assertTrue(klass.hasAnnotation(SuppressWarnings::class.java))
        verify(klass.getAnnotation(SuppressWarnings::class.java)!!)
    }

    @Test
    fun functions() {
        val function = Kibble.parseSource("""
        @SuppressWarnings("deprecation", count=10, foo=@Foo(42))
        fun foo()
        """)
                .functions.first()
        Assert.assertTrue(function.hasAnnotation(SuppressWarnings::class.java))
        verify(function.getAnnotation(SuppressWarnings::class.java)!!)

        val file = KibbleFile(File("temp.kt"))
        val foo = file.addFunction("foo")
        foo.addAnnotation("Bob", listOf(KibbleArgument("name", "Feller")))
        foo.addAnnotation(Retention::class.java)
        val source = file.toSource().toString()
        //language=kotlin
        Assert.assertEquals(source, """
            import java.lang.annotation.Retention

            @Bob(name = Feller)
            @Retention
            fun foo()

""".trimIndent())
    }

    private fun verify(annotation: KibbleAnnotation) {
        Assert.assertEquals(annotation["value"], "\"deprecation\"")
        Assert.assertEquals(annotation.getValue(), "\"deprecation\"")
        Assert.assertEquals(annotation["count"], "10")
        val annotationValue = annotation.getAnnotationValue("foo")
        Assert.assertEquals(annotationValue?.getValue(), "42")
    }
}