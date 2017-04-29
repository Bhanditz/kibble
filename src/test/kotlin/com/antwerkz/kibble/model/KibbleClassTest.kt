package com.antwerkz.kibble.model

import com.antwerkz.kibble.Kibble
import org.intellij.lang.annotations.Language
import org.testng.Assert
import org.testng.annotations.Test

class KibbleClassTest {
    @Language("kotlin")
    val source = """class Temp {
    companion object {
        val prop = 42
    }
    object temp
    class Nested(val foo: Bob) : Foo("bar"), Interface {
        constructor() : this(blarg, "nargle")

        val property: String

        fun something(): Int {
            return 4
        }
    }
}""".trim()

    @Test
    fun nested() {
        var kibbleClass = Kibble.parseSource(source).classes[0]

        Assert.assertEquals(kibbleClass.classes.size, 1)
        Assert.assertEquals(kibbleClass.classes[0].name, "Nested")

        val kibbleFile = KibbleFile()
        kibbleClass = kibbleFile.addClass("Temp")
        val nested = kibbleClass.addClass("Nested")

        nested.superType = KibbleType.from("Foo")
        nested.superCallArgs = listOf("\"bar\"")
        nested.superTypes += KibbleType.from("Interface")

        nested.addSecondaryConstructor().delegationArguments += listOf("blarg", "\"nargle\"")

        nested.addProperty("foo", "Bob", constructorParam = true)
        nested.addProperty("property", "String")
        nested.addFunction("something", "Int", "return 4")

        val companion = kibbleClass.addCompanionObject()
        companion.addProperty("prop", initializer = "42")
        kibbleClass.addObject("temp")

        Assert.assertEquals(kibbleClass.classes.size, 1)
        Assert.assertEquals(kibbleClass.classes[0].name, "Nested")

        Assert.assertTrue(kibbleClass.objects[0].companion)
        Assert.assertEquals(kibbleClass.objects[1].name, "temp")
        Assert.assertEquals(kibbleFile.toSource().toString().trim(), source.trim())
    }

    @Test
    fun members() {
        val kibbleClass = Kibble.parseSource(source).classes[0]

        var obj = kibbleClass.companion()
        Assert.assertNotNull(obj, "Should find a companion object")
        Assert.assertNotNull(obj?.getProperty("prop"), "Should find a property named 'prop'")

        obj = kibbleClass.getObject("temp")
        Assert.assertNotNull(obj, "Should find an object named 'temp'")

        val kibble = kibbleClass.getClass("Nested")
        Assert.assertNotNull(kibble, "Should find an class named 'Nested'")
        Assert.assertNotNull(kibble?.getProperty("property"), "Should find a property named 'property'")
    }

    @Test
    fun parent() {
        val file = Kibble.parseSource("""
package critter.test.source

import org.bson.types.ObjectId
import org.mongodb.morphia.annotations.Entity
import org.mongodb.morphia.annotations.Id

open class AbstractKotlinPerson {
    var age: Long? = null
}

@Entity
open class Person : AbstractKotlinPerson {
    @Id
    var id: ObjectId? = null

    var first: String? = null

    var last: String? = null
} """)
        Assert.assertNull(file.classes[0].superType)
        Assert.assertTrue(file.classes[0].superTypes.isEmpty())
        Assert.assertNull(file.classes[1].superType)
        Assert.assertEquals(file.classes[1].superTypes.size, 1)
        Assert.assertEquals(file.classes[1].superTypes[0], KibbleType(name = "AbstractKotlinPerson"))
    }
}