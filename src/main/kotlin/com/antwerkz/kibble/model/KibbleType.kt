package com.antwerkz.kibble.model

import com.antwerkz.kibble.Kibble
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType

/**
 * Specifies the type information of a property or parameter
 *
 * @property className the class name of this type
 * @property pkgName the package name of this type
 * @property typeParameters the type parameters of this type
 * @property nullable does this type support null values?
 */
open class KibbleType internal constructor(val className: String, val pkgName: String? = null ,
                                           override val typeParameters: List<TypeParameter> = listOf<TypeParameter>(),
                                           val nullable: Boolean = false, private val imported: Boolean = false) : GenericCapable {

    internal constructor(type: KibbleType, imported: Boolean) : this(type.className, type.pkgName, type.typeParameters,
            imported = imported)

    /**
     * Gives the full expression of this type complete with type parameters
     */
    val value: String by lazy {
        val base = if (!imported && pkgName != null) {
            pkgName.let { "$pkgName.$className" }
        } else {
            className
        }
        base + (if (typeParameters.isNotEmpty()) typeParameters.joinToString(prefix = "<", postfix = ">") else "") + if (nullable) "?" else ""
    }


    companion object {
        /**
         * Creates a KibbleType from ths string
         *
         * @return the new KibbleType
         */
        fun from(type: String): KibbleType {
            return Kibble.parseSource("val temp: $type").properties[0].type!!
        }

/*
        fun resolve(type: KibbleType, pkgName: String?): KibbleType {
            return KibbleType(type.className, pkgName, type.typeParameters, type.nullable)
        }
*/

        internal fun from(kt: KtTypeReference?): KibbleType? {
            return kt?.typeElement.let {
                when (it) {
                    is KtUserType -> extractType(it)
                    is KtNullableType -> extractType(it.innerType as KtUserType, true)
                    is KtFunctionType -> KibbleFunctionType(it)
                    else -> it?.let { throw IllegalArgumentException("unknown type $it") }
                }
            }
        }

        internal fun extractType(typeElement: KtUserType, nullable: Boolean = false): KibbleType {
            val value = (typeElement.qualifier?.text?.let { "$it." } ?: "") +
                    (typeElement.referencedName ?: "")
            val parameters = GenericCapable.extractFromTypeProjections(typeElement.typeArguments)
            val raw = value.substringBefore("<")
            val pkg = raw.split(".")
                    .dropLastWhile { it != "" && it[0].isUpperCase() }
                    .joinToString(".")
            val pkgName = if (pkg == "") null else pkg

            val className = raw.split(".")
                    .dropWhile { it != "" && !it[0].isUpperCase() }
                    .joinToString(".")

            return KibbleType(className, pkgName, parameters, nullable)
        }
    }

    /**
     * @return the string/source form of this type
     */
    override fun toString() = value

    /**
     * @return true if `other` is equal to this
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as KibbleType

        if (value != other.value) return false
        if (typeParameters != other.typeParameters) return false
        if (nullable != other.nullable) return false

        return true
    }

    /**
     * @return the hashcode for this type
     */
    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + nullable.hashCode()
        return result
    }

}