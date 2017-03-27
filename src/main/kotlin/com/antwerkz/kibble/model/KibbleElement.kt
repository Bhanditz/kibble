package com.antwerkz.kibble.model

import com.antwerkz.kibble.SourceWriter
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface KibbleElement {
    fun addModifier(modifier: String?) {
        when (modifier) {
            null, " " -> {}
            "public", "protected", "private", "internal" -> (this as Visible).visibility = Visibility.valueOf(modifier.toUpperCase())
            "final", "abstract", "open" -> (this as Modal<*>).modality = Modality.valueOf(modifier.toUpperCase())
            "var", "val" -> (this as Mutable).mutability = Mutability.valueOf(modifier.toUpperCase())
            "lateinit" -> (this as KibbleProperty).lateInit = true
            "override" -> (this as Overridable).overriding = true
            else -> throw IllegalStateException("Unknown modifier: '$modifier' on $this")
        }
    }

    fun toSource(writer: SourceWriter, indentationLevel: Int = 0) {
        writer.writeln("toSource not implemented for ${javaClass.name}")
    }
}