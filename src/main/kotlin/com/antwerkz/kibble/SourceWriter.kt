package com.antwerkz.kibble

import com.antwerkz.kibble.model.KibbleArgument
import com.antwerkz.kibble.model.KibbleElement
import com.antwerkz.kibble.model.KibbleParameter
import com.antwerkz.kibble.model.KibbleProperty
import com.antwerkz.kibble.model.KibbleType
import com.antwerkz.kibble.model.Modality
import com.antwerkz.kibble.model.Modality.FINAL
import com.antwerkz.kibble.model.Mutability
import com.antwerkz.kibble.model.Mutability.VAL
import com.antwerkz.kibble.model.Mutability.VAR
import com.antwerkz.kibble.model.TypeParameter
import com.antwerkz.kibble.model.Visibility
import com.antwerkz.kibble.model.Visibility.NONE
import com.antwerkz.kibble.model.Visibility.PUBLIC
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * This is used to convert a Kibble model to its string form.
 *
 * @property
 */
class SourceWriter {
    private val stringWriter = StringWriter()
    private val writer = PrintWriter(stringWriter)

    private val indent = "    "

    internal fun write(content: String, level: Int = 0) {
        (1..level).forEach { writer.write(indent) }
        writer.print(content)
    }

    internal fun writeln(content: String = "", level: Int = 0) {
        (1..level).forEach { writer.write(indent) }
        writer.println(content)
    }

    /**
     * Dumps the content of this SourceWriter to a file
     *
     * @param file the file to use
     */
    fun toFile(file: File) {
        file.writeText(toString())
    }

    /**
     * Returns the source written to this SourceWriter
     *
     * @return the source
     */
    override fun toString(): String {
        return stringWriter.toString()
    }

    fun write(visibility: Visibility?) {
        when (visibility) {
            NONE, PUBLIC, null -> {
            }
            else -> {
                write(visibility.toString())
                write(" ")
            }
        }
    }

    fun write(modality: Modality?) {
        when (modality) {
            FINAL, null -> {
            }
            else -> {
                write(modality.toString())
                write(" ")
            }
        }
    }

    fun write(mutability: Mutability) {
        if (mutability == VAL || mutability == VAR) {
            write(mutability.toString())
            write(" ")
        }
    }

    operator fun invoke(func: SourceWriter.() -> Unit): SourceWriter {
        func()
        return this
    }

    fun writeParameters(properties: List<KibbleProperty>, parameters: List<KibbleParameter>, allowEmpty: Boolean = true) {
        val list: List<KibbleElement> = properties + parameters
        if (allowEmpty || list.isNotEmpty()) {
            write(list.joinToString(", ", "(", ")", transform = { it.toSource().toString() }))
        }
    }

    fun writeType(type: KibbleType?, useSeparator: Boolean = true) {
        if (type != null) {
            if (type.resolvedName != "Unit") {
                if (useSeparator) write(": ")
                write(type.toSource().toString())
            }
        }
    }

    fun writeTypeParameters(typeParameters: List<TypeParameter>, buffer: Boolean = false) {
        if (typeParameters.isNotEmpty()) {
            write(typeParameters.joinToString(prefix = "<", postfix = ">",
                    transform = { it.toSource().toString() }))
            if(buffer) {
                write(" ")
            }
        }
    }

    fun writeBlock(body: String?, level: Int) {
        body?.let {
            writeOpeningBrace()
            writeln(body.prependIndent(indent * (level + 1)))
            writeln("}", level)
        }
    }

    operator fun String.times(count: Int): String {
        var result = ""
        (1..count).forEach { result += this }

        return result
    }

    fun writeArguments(arguments: List<KibbleArgument>, allowEmpty: Boolean = true) {
        if (allowEmpty || arguments.isNotEmpty()) {
            write(arguments.joinToString(", ", prefix = "(", postfix = ")", transform = { it.toSource().toString() }))
        }
    }

    fun writeSuperCall(extends: KibbleType?, args: List<KibbleArgument>) {
        extends?.let {
            it.toSource(this)
            writeArguments(args)
        }
    }

    fun writeInitializer(initializer: String?) {
        initializer?.let { writer.write(" = $it") }
    }

    fun current(): Char {
        return stringWriter.buffer.last()
    }

    fun writeParentCalls(extends: KibbleType?, implements: List<KibbleType>, args: List<KibbleArgument>) {
        if (extends != null || !implements.isEmpty()) {
            write(": ")
            writeSuperCall(extends, args)
            if (extends != null) {
                write(", ")
            }
            write(implements.joinToString(", ", transform = { it.toSource().toString() }))
        }
    }

    fun writeOpeningBrace() {
        if (current() != ' ') {
            write(" ")
        }
        writeln("{")
    }

    fun writeIndent(level: Int) {
        write(indent * level)
    }

    fun writeCollections(previous: Boolean, level: Int, vararg blocks: Collection<KibbleElement>) {
        var previousWritten = previous
        blocks.forEach {
            previousWritten = writeCollection(previousWritten, it, level)
        }
    }

    fun writeCollection(previousWritten: Boolean, block: Collection<KibbleElement>, level: Int, spaceBetween: Boolean = true): Boolean {
        return if (block.isNotEmpty()) {
            if (previousWritten) {
                writeln()
            }
            block.forEachIndexed { i, it ->
                if (i != 0 && spaceBetween) {
                    writeln()
                }
                it.toSource(this, level)
            }
            true
        } else previousWritten
    }

    fun writePackage(name: String?) {
        name?.let {
            writeln("package $it")
        }
    }

    fun writeReceiver(receiver: KibbleType?) {
        receiver?.let {
            write("$receiver.")
        }
    }
}