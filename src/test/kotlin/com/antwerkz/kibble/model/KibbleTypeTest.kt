package com.antwerkz.kibble.model

import com.antwerkz.kibble.Kibble
import org.intellij.lang.annotations.Language
import org.testng.Assert
import org.testng.annotations.Test

@Test
class KibbleTypeTest {
    fun simpleType() {
        val file = Kibble.parseSource("val foo: com.foo.bar.Type")
        val type = file.properties[0].type

        Assert.assertEquals(type?.toString(), "com.foo.bar.Type")
        Assert.assertTrue(type?.typeParameters?.isEmpty() ?: false)
    }

    fun generics() {
        val string = "com.foo.bar.SomeType<kotlin.String, kotlin.Double>?"
        val type = Kibble.parseSource("val foo: $string")
                .properties[0]
                .type!!

        Assert.assertEquals(type.toString(), "com.foo.bar.SomeType<String, Double>?")
        Assert.assertEquals(type.className, "SomeType")
        Assert.assertEquals(type.pkgName, "com.foo.bar")
        Assert.assertTrue(type.nullable)
        Assert.assertEquals(type.typeParameters.size, 2)
        Assert.assertEquals(type.typeParameters[0].type.toString(), "String")
        Assert.assertEquals(type.typeParameters[1].type.toString(), "Double")
    }

    fun fullyQualified() {
        val qualified = KibbleType.from("java.math.BigDecimal")
        val decimal = KibbleType.from("BigDecimal")
        val integer = KibbleType.from("BigInteger")
        val dateTime = KibbleType.from("java.time.LocalDateTime")
        val list = KibbleType("java.util", "List", mutableListOf(TypeParameter(KibbleType.from("String"))))

        Assert.assertEquals(qualified.toString(), "java.math.BigDecimal")
        Assert.assertEquals(decimal.toString(), "BigDecimal")
        Assert.assertEquals(integer.toString(), "BigInteger")
        Assert.assertEquals(dateTime.toString(), "java.time.LocalDateTime")
        Assert.assertEquals(list.toString(), "java.util.List<String>")
    }

    fun components() {
        val dateTime = KibbleType.from("java.time.LocalDateTime")
        val entry = KibbleType.from("java.util.Map.Entry")
        val int = KibbleType.from("Int")

        Assert.assertEquals(dateTime.className, "LocalDateTime")
        Assert.assertEquals(dateTime.pkgName, "java.time")
        Assert.assertEquals(entry.className, "Map.Entry")
        Assert.assertEquals(entry.pkgName, "java.util")
        Assert.assertEquals(int.className, "Int")
        Assert.assertNull(int.pkgName)
    }

    fun values() {
        val file = KibbleFile(pkgName = "com.antwerkz.aliases")
        val type = file.resolve(KibbleType("this.is.the.package", "Class",
                mutableListOf(TypeParameter(KibbleType.from("K")),
                TypeParameter(KibbleType.from("V"))), true))

        Assert.assertEquals(type.fqcn, "this.is.the.package.Class")
        Assert.assertEquals(type.toString(), "Class<K, V>?")

        val list = file.resolve(KibbleType.from("java.util.List<com.foo.Bar, out K>"))
        Assert.assertEquals(list.fqcn, "java.util.List")
        Assert.assertEquals(list.toString(), "List<Bar, out K>")
    }

    fun autoImportedTypes() {
        @Language("kotlin")
        val source = """package com.antwerkz.testing
class Main {
    val b: Boolean
    val byte: Byte
    val short: Short
    val d: Double
    val f: Float
    val l: Long
    val i: Int
    val integer: Integer
    val s: String
}
""".trim()
        val file = Kibble.parseSource(source)
        val props = file.classes[0].properties.iterator()

        check(props.next(), "Boolean")
        check(props.next(), "Byte")
        check(props.next(), "Short")
        check(props.next(), "Double")
        check(props.next(), "Float")
        check(props.next(), "Long")
        check(props.next(), "Int")
        check(props.next(), "Integer")
        check(props.next(), "String")
    }

    private fun check(property: KibbleProperty, fqcn: String) {
        val type = property.type!!
        Assert.assertEquals(type.fqcn, fqcn)
    }
}