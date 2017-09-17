package com.antwerkz.kibble.model

import com.antwerkz.kibble.SourceWriter
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtUserType

class KibbleFunctionType internal constructor(name: String, var parameters: List<KibbleFunctionTypeParameter>,
                                              val type: KibbleType?) : KibbleType(name) {

    internal constructor(file: KibbleFile, kt: KtFunctionType) : this(kt.name ?: "",
            kt.parameters.map { KibbleFunctionTypeParameter(file, it) },
            kt.returnTypeReference?.typeElement?.let { extractType(file, it as KtUserType) })

    override fun toString(): String {
        var string = parameters.joinToString(", ", prefix = "(", postfix = ")")
        type?.let {
            if (type.toString() != "Unit") {
                string += " -> $type"
            }
        }
        return string
    }
}

internal class KibbleFunctionTypeParameter(val type: KibbleType?) : KibbleElement {
    var typeParameters = listOf<TypeParameter>()

    internal constructor(file: KibbleFile, kt: KtParameter) : this(KibbleType.from(file, kt.typeReference)) {
        typeParameters += GenericCapable.extractFromTypeParameters(file, kt.typeParameters)
    }

    /**
     * @return the string/source form of this type
     */
    override fun toString(): String {
        return toSource().toString()
    }

    /**
     * @return the string/source form of this type
     */
    override fun toSource(writer: SourceWriter, level: Int): SourceWriter {
        writer.write("$type")
        return writer
    }

}