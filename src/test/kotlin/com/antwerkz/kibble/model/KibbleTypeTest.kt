package com.antwerkz.kibble.model

import com.antwerkz.kibble.Kibble
import org.testng.Assert
import org.testng.annotations.Test

@Test
class KibbleTypeTest {
    fun simpleType() {
        val file = Kibble.parseSource("val foo: com.foo.bar.Type")
        val type = file.properties[0].type

        Assert.assertEquals(type.fullName, "com.foo.bar.Type")
        Assert.assertEquals(type.packageName, "com.foo.bar")
        Assert.assertEquals(type.name, "Type")
        Assert.assertTrue(type.parameters.isEmpty())
    }

    fun generics() {
        val string = "com.foo.bar.Type<kotlin.String, kotlin.Double>?"
        val file = Kibble.parseSource("val foo: $string")
        val type = file.properties[0].type

        Assert.assertEquals(type.fullName, string)
        Assert.assertEquals(type.packageName, "com.foo.bar")
        Assert.assertEquals(type.name, "Type")
        Assert.assertTrue(type.nullable)
        Assert.assertEquals(type.parameters.size, 2)
        Assert.assertEquals(type.parameters[0].name, "String")
        Assert.assertEquals(type.parameters[0].packageName, "kotlin")
        Assert.assertEquals(type.parameters[1].name, "Double")
        Assert.assertEquals(type.parameters[1].packageName, "kotlin")
    }

    fun fullyQualified() {
        val file = Kibble.parseSource("import java.math.BigDecimal")

        val qualified = KibbleType.from(file, "java.math.BigDecimal")
        val decimal = KibbleType.from(file, "BigDecimal")
        val integer = KibbleType.from(file, "BigInteger")
        val dateTime = KibbleType.from(file, "java.time.LocalDateTime")

        Assert.assertEquals(qualified.fullName, "java.math.BigDecimal")
        Assert.assertEquals(decimal.fullName, "java.math.BigDecimal")
        Assert.assertEquals(integer.fullName, "BigInteger")
//        Assert.assertEquals(dateTime.name, "java.time.LocalDateTime")
//        Assert.assertEquals(dateTime.fullName, "java.time.LocalDateTime")
    }
}