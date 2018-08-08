package com.antwerkz.kibble.model

import com.antwerkz.kibble.Kibble
import org.testng.Assert
import org.testng.annotations.Test

class OverridableTest {
    @Test
    fun overridable() {
        val file = Kibble.parseSource("""
    override fun temp() {
    }
""")
        Assert.assertTrue(file.functions.first().isOverride())
    }
}