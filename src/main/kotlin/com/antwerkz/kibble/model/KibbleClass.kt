package com.antwerkz.kibble.model

import com.antwerkz.kibble.SourceWriter
import com.antwerkz.kibble.model.Modality.FINAL
import com.antwerkz.kibble.model.Visibility.PUBLIC
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.modalityModifier
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

/**
 * Represents an annotation in kotlin source code.
 *
 * @property name the class name
 * @property constructor the primary constructor for this class
 * @property secondaries the secondary constructors this class
 */
class KibbleClass internal constructor(override var file: KibbleFile,
                                       var name: String = "",
                                       override var modality: Modality = FINAL,
                                       override var visibility: Visibility = PUBLIC) : KibbleElement, FunctionHolder,
        Visible, Modal<KibbleClass>, Annotatable, PropertyHolder, Extendable, ClassOrObjectHolder {

    override var superTypes = listOf<KibbleType>()
    override var superType: KibbleType? = null
    override var superCallArgs = listOf<String>()

    override var annotations = mutableListOf<KibbleAnnotation>()
    override val classes: MutableList<KibbleClass> = mutableListOf()
    override val objects: MutableList<KibbleObject> = mutableListOf()
    override val functions = mutableListOf<KibbleFunction>()
    override val properties = mutableListOf<KibbleProperty>()

    var constructor = Constructor()
        private set
    val secondaries: MutableList<SecondaryConstructor> = mutableListOf()

    internal constructor(file: KibbleFile, kt: KtClass) : this(file, kt.name ?: "") {
        Extendable.extractSuperInformation(this, kt)

        modality = Modal.apply(kt.modalityModifier())
        visibility = Visible.apply(kt.visibilityModifier())

        kt.primaryConstructor?.let {
            constructor = Constructor(this, it)
        }
        kt.secondaryConstructors.forEach {
            secondaries += SecondaryConstructor(this, it)
        }
        extractAnnotations(file, kt.annotationEntries)
        kt.getBody()?.let {
            extractClassesObjects(file, it.declarations)
            extractFunctions(file, it.declarations)
            extractProperties(file, it.declarations, this)
        }
    }

    /**
     * Adds a secondary constructor to this class
     *
     * @return the new constructor
     */
    fun addSecondaryConstructor(): SecondaryConstructor {
        return SecondaryConstructor().also {
            secondaries += it
        }
    }

    override fun addClass(name: String): KibbleClass {
        return KibbleClass(file, name).also {
            classes += it
        }
    }

    override fun addObject(name: String, isCompanion: Boolean): KibbleObject {
        return KibbleObject(file, name, isCompanion).also {
            objects += it
        }
    }

    override fun addFunction(name: String?, type: String, body: String): KibbleFunction {
        return KibbleFunction(name = name, type = type, body = body).also {
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
                constructor.parameters += it
            }
            properties += it
        }
    }

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

        constructor.toSource(writer, level)
        superType?.let {
            writer.write(" : $it")
            writer.write(superCallArgs.joinToString(prefix = "(", postfix = ")"))
        }
        if (!superTypes.isEmpty()) {
            writer.write(superTypes.joinToString(prefix = ", "))
        }
        val nonParamProps = properties.filter { !it.constructorParam }
        if (!nonParamProps.isEmpty() || !functions.isEmpty() || !classes.isEmpty() || !objects.isEmpty()
                || !secondaries.isEmpty()) {
            writer.writeln(" {")
            secondaries.forEach { it.toSource(writer, level + 1) }
            nonParamProps.forEach { it.toSource(writer, level + 1) }

            functions.forEach { it.toSource(writer, level + 1) }
            objects.forEach { it.toSource(writer, level + 1) }
            classes.forEach { it.toSource(writer, level + 1) }

            writer.write("}", level)
        }
        writer.writeln()
        return writer
    }
}
