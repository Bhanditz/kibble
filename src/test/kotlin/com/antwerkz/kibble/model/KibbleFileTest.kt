package com.antwerkz.kibble.model

import com.antwerkz.kibble.Kibble
import org.intellij.lang.annotations.Language
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File
import java.sql.ResultSet

class KibbleFileTest {
    @Test(expectedExceptions = arrayOf(IllegalArgumentException::class))
    fun constructorProperties() {
        KibbleFile().addProperty("name", constructorParam = true)
    }

    @Test
    fun imports() {
        val file = KibbleFile()
        file.pkgName = "com.antwerkz.kibble"

        file.addImport(ResultSet::class.java, "aliasName")
        file.addImport(String::class.java, "anotherAlias")
        assertImport(file, "java.lang", "String", "anotherAlias")
        Assert.assertEquals(file.toSource().toString().trim(),
                """package com.antwerkz.kibble

import java.lang.String as anotherAlias
import java.sql.ResultSet as aliasName""")
    }

    @Test
    fun importOrdering() {
        val file = KibbleFile()

        file.addImport("java.util.ArrayList")
        file.addImport("javax.annotation.Generated")
        file.addImport("java.util.HashMap", "HMap")

        val iterator = file.imports.iterator()
        Assert.assertEquals(iterator.next().type.fqcn, "java.util.ArrayList")
        val next = iterator.next()
        Assert.assertEquals(next.type.fqcn, "java.util.HashMap")
        Assert.assertEquals(next.type.alias, "HMap")
        Assert.assertEquals(iterator.next().type.fqcn, "javax.annotation.Generated")
    }

    @Test
    fun outputFile() {
        val file = KibbleFile("Foo.kt")
        Assert.assertEquals(file.outputFile(File("/tmp/")), File("/tmp/Foo.kt"))

        file.pkgName = "com.antwerkz.kibble"
        Assert.assertEquals(file.outputFile(File("/tmp/")), File("/tmp/com/antwerkz/kibble/Foo.kt"))
    }

    @Test
    fun resolve() {
        @Language("kotlin")
        val source = """package com.antwerkz.testing

import com.foo.Bar
import com.zorg.Flur

class Main {
    val s: Second = Second()
    val t: Third = Third.HI
    val b: Bar = Bar()
    val f: com.zorg.Flur = com.zorg.Flur()
    val g: Generic<Int>
}

class Second""".trim()

        @Language("kotlin")
        val source2 = """package com.antwerkz.testing

enum class Third {
    HI
}

class Generic<T>"""

        val file = Kibble.parseSource(source)
        Kibble.parseSource(source2, file.context)
        val props = file.classes[0].properties.iterator()

        check(props.next(), "Second", "com.antwerkz.testing.Second")
        check(props.next(), "Third", "com.antwerkz.testing.Third")
        check(props.next(), "Bar", "com.foo.Bar")
        check(props.next(), "Flur", "com.zorg.Flur")
        check(props.next(), "Generic<Int>", "com.antwerkz.testing.Generic")

    }

    @Test
    fun resolveClassesInFile() {
        @Language("kotlin")
        val source = """package com.antwerkz.testing

class Main {
    val s: Second = Second()
}
class Second""".trim()

        val file = Kibble.parseSource(source)
        val props = file.classes[0].properties.iterator()

        check(props.next(), "Second", "com.antwerkz.testing.Second")
    }

    @Test
    fun resolveClassesInAnotherFile() {

        val sourceFile1 = createTempFile("source1", ".kt").also {
            it.writeText("""package com.antwerkz.testing

class Main {
    val s: Second = Second()
}""")
        }
        val sourceFile2 = createTempFile("source2", ".kt").also {
            it.writeText("""package com.antwerkz.testing

class Second""".trim())
        }

        val files = Kibble.parse(listOf(sourceFile1, sourceFile2))
        val props = files[0].classes[0].properties.iterator()

        check(props.next(), "Second", "com.antwerkz.testing.Second")
    }

    private fun check(property: KibbleProperty, expectedName: String, fqcn: String) {
        val type = property.type!!
        Assert.assertEquals(type.value, expectedName)
        Assert.assertEquals(type.fqcn, fqcn)
    }

    @Test
    fun normalize() {
        val file = KibbleFile("test.kt")

        Assert.assertEquals(file.normalize(KibbleType.from(file, "java.util.List")).value, "List")
        Assert.assertNotNull(file.imports.firstOrNull { "List" == it.type.alias || "List" == it.type.className })
        assertImport(file, "java.util", "List")

        Assert.assertEquals(file.normalize(KibbleType.from(file, "java.util.List")).value, "List")
        assertImport(file, "java.util", "List")

        Assert.assertEquals(file.normalize(KibbleType.from(file, "List")).value, "List")
        assertImport(file, "java.util", "List")

        Assert.assertEquals(file.normalize(KibbleType.from(file, "java.util.Set")).value, "Set")
        assertImport(file, "java.util", "Set")

        file.addImport(java.awt.List::class.java, "awtList")
        assertImport(file, "java.awt", "List", "awtList")

        Assert.assertEquals(file.normalize(KibbleType.from(file, "java.awt.List")).value, "awtList")
        Assert.assertEquals(file.normalize(KibbleType.from(file, "awtList")).value, "awtList")

        Assert.assertEquals(file.normalize(KibbleType.from(file, "Map.Entry")).value, "Map.Entry")
    }


    fun assertImport(file: KibbleFile, pkgName: String, className: String, alias: String? = null) {
        Assert.assertEquals(file.imports.filter {
            it.type.pkgName == pkgName && it.type.className == className
        }.size, 1)

        alias?.let {
            Assert.assertEquals(file.imports.filter {
                it.type.alias == alias
            }.size, 1)
        }
    }
}