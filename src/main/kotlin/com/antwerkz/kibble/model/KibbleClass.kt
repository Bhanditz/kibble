package com.antwerkz.kibble.model

import com.antwerkz.kibble.SourceWriter
import com.antwerkz.kibble.model.KibbleExtractor.extractAnnotations
import com.antwerkz.kibble.model.KibbleExtractor.extractClasses
import com.antwerkz.kibble.model.KibbleExtractor.extractFunctions
import com.antwerkz.kibble.model.KibbleExtractor.extractObjects
import com.antwerkz.kibble.model.KibbleExtractor.extractProperties
import com.antwerkz.kibble.model.KibbleExtractor.extractSuperCallArgs
import com.antwerkz.kibble.model.KibbleExtractor.extractSuperType
import com.antwerkz.kibble.model.KibbleExtractor.extractSuperTypes
import com.antwerkz.kibble.model.Modality.FINAL
import com.antwerkz.kibble.model.Visibility.PUBLIC
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

/**
 * Represents an annotation in kotlin source code.
 *
 * @property name the class name
 * @property initBlock any custom init block for this class
 * @property constructor the primary constructor for this class
 * @property secondaries the secondary constructors this class
 */
class KibbleClass internal constructor(val file: KibbleFile, var name: String = "",
                                       override var modality: Modality = FINAL,
                                       override var visibility: Visibility = PUBLIC) : KibbleElement, FunctionHolder, GenericCapable,
        Visible, Modal<KibbleClass>, AnnotationHolder, PropertyHolder, ClassOrObjectHolder {

    var superCallArgs = listOf<String>()
        private set

    override var typeParameters = mutableListOf<TypeParameter>()
    override val annotations = mutableListOf<KibbleAnnotation>()
    override val interfaces = mutableListOf<KibbleInterface>()
    override val classes = mutableListOf<KibbleClass>()
    override val objects = mutableListOf<KibbleObject>()
    override val functions = mutableListOf<KibbleFunction>()
    override val properties = mutableListOf<KibbleProperty>()
    var parentClass: KibbleType? = null
        private set
    val parentInterfaces: MutableList<KibbleType> = mutableListOf()

    var initBlock: String? = null

    var constructor = Constructor()
    val secondaries: MutableList<SecondaryConstructor> = mutableListOf()

    var enum = false

    internal constructor(file: KibbleFile, kt: KtClass) : this(file, kt.name ?: "") {
        modality = Modal.apply(kt.modalityModifier())
        visibility = Visible.apply(kt.visibilityModifier())
        typeParameters = GenericCapable.extractFromTypeParameters(kt.typeParameters)
        enum = kt.isEnum()

        kt.primaryConstructor?.let {
            constructor = Constructor(this, it)
        }
        kt.secondaryConstructors.forEach {
            secondaries += SecondaryConstructor(it)
        }

        parentClass = extractSuperType(kt.superTypeListEntries)
        parentInterfaces += extractSuperTypes(kt.superTypeListEntries)
        superCallArgs += extractSuperCallArgs(kt.superTypeListEntries)
        annotations += extractAnnotations(kt.annotationEntries)

        classes += extractClasses(file, kt.declarations)
        objects += extractObjects(file, kt.declarations)
        functions += extractFunctions(kt.declarations)
        properties += extractProperties(kt.declarations)

    }

    fun extend(type: String, vararg arguments: String) {
        extend(KibbleType.from(type), *arguments)
    }

    fun extend(type: KibbleType, vararg arguments: String) {
        parentClass = type
        superCallArgs = listOf(*arguments)
    }

    fun implement(type: String) {
        parentInterfaces += KibbleType.from(type)
    }

    /**
     * Adds a secondary constructor to this class
     *
     * @return the new constructor
     */
    fun addSecondaryConstructor(vararg arguments: String): SecondaryConstructor {
        return SecondaryConstructor(*arguments).also {
            secondaries += it
        }
    }

    override fun addClass(name: String): KibbleClass {
        return KibbleClass(file, name).also {
            classes += it
        }
    }

    override fun addInterface(name: String): KibbleInterface {
        return KibbleInterface(file, name).also {
            interfaces += it
        }
    }

    /**
     * Adds (or gets if it already exists) a companion object to this class
     *
     * @return the companion object
     */
    fun addCompanionObject(): KibbleObject {
        return companion() ?: KibbleObject(file, companion = true).also {
            objects.add(it)
        }
    }

    override fun addObject(name: String, isCompanion: Boolean): KibbleObject {
        return KibbleObject(file, name, isCompanion).also {
            objects += it
        }
    }

    override fun addFunction(name: String?, type: String, body: String): KibbleFunction {
        return KibbleFunction(name = name, type = KibbleType.from(type), body = body).also {
            functions += it
        }
    }

    override fun addProperty(name: String, type: String?, initializer: String?, modality: Modality, overriding: Boolean,
                             visibility: Visibility, mutability: Mutability, lateInit: Boolean, constructorParam: Boolean): KibbleProperty {
        return KibbleProperty(name, type?.let { KibbleType.from(type) }, initializer, modality, overriding, lateInit).also {
            it.visibility = visibility
            it.mutability = mutability
            it.constructorParam = constructorParam
            if (constructorParam) {
                constructor.parameters.add(it)
            }
            properties += it
        }
    }

    fun isEnum() = enum

    /**
     * @return the string form of this class
     */
    override fun toString(): String {
        return "class $name"
    }

    override fun toSource(writer: SourceWriter, level: Int): SourceWriter {
        annotations.forEach { writer.writeln(it.toString(), level) }
        writer.write("$visibility${modality}class ", level)
        writer.write(name)
        if (!typeParameters.isEmpty()) {
            writer.write(typeParameters.joinToString(", ", prefix = "<", postfix = ">"))
        }

        if (constructor.parameters.isNotEmpty()) {
            constructor.toSource(writer, level)
        }
        val extends = mutableListOf<String>()
        parentClass?.let {
            extends += "$it${superCallArgs.joinToString(prefix = "(", postfix = ")")}"
        }
        if (!parentInterfaces.isEmpty()) {
            extends += parentInterfaces.joinToString(", ")
        }
        if (extends.isNotEmpty()) {
            writer.write(": ")
            writer.write(extends.joinToString(", "))
        }
        val nonParamProps = properties.filter { !it.constructorParam }

        writer.writeln(" {")

        objects.filter { it.companion }
                .forEach { it.toSource(writer, level + 1) }

        secondaries.forEach { it.toSource(writer, level + 1) }
        initBlock?.let {
            writer.writeln("init {", level + 1)
            it.trimIndent().split("\n").forEach {
                writer.writeln(it, level + 2)
            }
            writer.writeln("}", level + 1)
            writer.writeln()
        }
        nonParamProps.forEach { it.toSource(writer, level + 1) }

        objects.filter { !it.companion }
                .forEach { it.toSource(writer, level + 1) }
        interfaces.forEach { it.toSource(writer, level + 1) }
        classes.forEach { it.toSource(writer, level + 1) }
        functions.forEach { it.toSource(writer, level + 1) }

        writer.write("}", level)

        writer.writeln()
        return writer
    }

    /**
     * Gets the companion object if it exists
     *
     * @return the companion object
     */
    fun companion(): KibbleObject? {
        return objects.firstOrNull { it.companion }
    }

    override fun collectImports(file: KibbleFile) {
        properties.forEach { it.collectImports(file) }
        interfaces.forEach { it.collectImports(file) }
        classes.forEach { it.collectImports(file) }
        objects.forEach { it.collectImports(file) }
        functions.forEach { it.collectImports(file) }
        secondaries.forEach { it.collectImports(file) }
        parentClass?.let { file.resolve(it) }
        parentInterfaces.forEach { file.resolve(it) }
    }

}
