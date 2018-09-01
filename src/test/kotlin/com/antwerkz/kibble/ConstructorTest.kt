package com.antwerkz.kibble

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec.Companion.classBuilder
import org.testng.Assert
import org.testng.annotations.Test
import com.squareup.kotlinpoet.FileSpec.Companion.builder as file
import com.squareup.kotlinpoet.FunSpec.Companion.constructorBuilder as ctor
import com.squareup.kotlinpoet.ParameterSpec.Companion.builder as parameter
import com.squareup.kotlinpoet.PropertySpec.Companion.builder as property

class ConstructorTest {
    @Test
    fun secondaries() {
        val classes = Kibble.parseSource("""
class Factory(val type: String) {
    constructor(): this("car")
}
        """).classes.iterator()

        val klass = classes.next()
        Assert.assertEquals(klass.primaryConstructor?.parameters?.size, 1)
        Assert.assertEquals("type", klass.propertySpecs[0].name)

        val secondaries = klass.secondaries.iterator()
        val secondary = secondaries.next()
        Assert.assertNotNull(secondary)
        Assert.assertTrue(secondary.parameters.isEmpty())
    }
    @Test
    fun constructors() {
        val fileSpec = Kibble.parseSource(
                """
class Factory(vararg val type: String = "red")
        """
        )
        val classes = fileSpec.classes.iterator()

        val klass = classes.next()
        val primaryConstructor = klass.primaryConstructor!!
        Assert.assertEquals(primaryConstructor.parameters.size, 1)

        val parameterSpec = primaryConstructor.parameters[0]
        Assert.assertEquals("type", parameterSpec.name)
        Assert.assertEquals(CodeBlock.of("\"red\""), parameterSpec.defaultValue)
        Assert.assertTrue(KModifier.VARARG in parameterSpec.modifiers)
        Assert.assertEquals("type", klass.propertySpecs[0].name)
    }
}
