package com.antwerkz.kibble.model

import com.antwerkz.kibble.SourceWriter
import org.jetbrains.kotlin.psi.KtImportDirective

data class Import(val name: String, val alias: String? = null): KotlinElement {
    internal constructor(kt: KtImportDirective): this(kt.importedFqName!!.asString(), kt.aliasName)

    override fun toSource(writer: SourceWriter, indentationLevel: Int) {
        writer.write("import $name")
        alias?.let { writer.write(" as $alias")}
        writer.writeln()
    }
}