package com.antwerkz.kibble.model

import com.antwerkz.kibble.KibbleContext
import com.antwerkz.kibble.SourceWriter
import java.io.File

/**
 * Defines a kotlin source file model
 *
 * @property name the name of the physical kotlin file
 * @property pkgName the package name
 * @property importDirectives the imports defined in the file
 */
class KibbleFile(val name: String? = null, var pkgName: String? = null,
                 val context: KibbleContext = KibbleContext()) :
        KibbleElement, ClassOrObjectHolder, PropertyHolder, FunctionHolder {

    private val importDirectives = sortedMapOf<String, KibbleImport>()
    val imports
        get() = importDirectives.values.toSet()


    override val classes = mutableListOf<KibbleClass>()
    override val objects = mutableListOf<KibbleObject>()
    override val functions = mutableListOf<KibbleFunction>()
    override val properties = mutableListOf<KibbleProperty>()

    var sourceTimestamp: Long = 0

    init {
        context.register(this)
    }

    override fun addClass(name: String): KibbleClass {
        return KibbleClass(name).also {
            classes += it
        }
    }

    override fun addObject(name: String, isCompanion: Boolean): KibbleObject {
        return KibbleObject(name, isCompanion).also {
            objects += it
        }

    }

    override fun addFunction(name: String?, type: String, body: String): KibbleFunction {
        return KibbleFunction(name, type = KibbleType.from(type), body = body).also {
            functions += it
        }
    }

    /**
     * Adds an import to this file
     *
     * @param name the type to import
     * @param alias the alias of the import
     *
     * @return the new import
     */
    fun addImport(name: String, alias: String? = null) {
        addImport(KibbleType.from(name), alias)
    }

    /**
     * Adds an import to this file
     *
     * @param type the type to import
     * @param alias the alias of the import
     *
     * @return the new import
     */
    fun addImport(type: Class<*>, alias: String? = null) {
        addImport(KibbleType.from(type.name), alias)
    }

    fun addImport(type: KibbleType, alias: String? = null) {
        if (type.pkgName != null || type.className.endsWith(".*")) {
            addImport(KibbleImport(type, alias))
        }
    }

    fun addImport(kibbleImport: KibbleImport) {
        importDirectives[kibbleImport.type.fqcn()] = kibbleImport
    }

    /**
     * Creates a File reference for this KibbleFile based on the directory given.
     *
     * @param directory the output directory
     *
     * @return the new File
     */
    fun outputFile(directory: File): File {
        var fileName = name
        pkgName?.let {
            fileName = it.replace('.', '/') + "/" + fileName
        }
        return File(directory, fileName)
    }

    override fun toSource(writer: SourceWriter, level: Int): SourceWriter {
        pkgName?.let {
            writer.writeln("package $it")
            writer.writeln()
        }

        properties.forEach { it.collectImports(this) }
        objects.forEach { it.collectImports(this) }
        classes.forEach { it.collectImports(this) }
        functions.forEach { it.collectImports(this) }

        writeBlock(writer, level, false, imports)
        writeBlock(writer, level, false, properties)
        writeBlock(writer, level, true, classes.filter { it.isInterface })
        writeBlock(writer, level, true, classes.filter { !it.isInterface })
        writeBlock(writer, level, true, objects)
        writeBlock(writer, level, true, functions)

        return writer
    }

    private fun writeBlock(writer: SourceWriter, level: Int, inBetween: Boolean, block: Collection<KibbleElement>) {
        if (!block.isEmpty()) {
            writer.writeln()
        }

        block.forEachIndexed { i, it ->
            if (inBetween && i != 0) {
                writer.writeln()
            }
            it.toSource(writer, level)
        }
    }

    override fun collectImports(file: KibbleFile) {
        properties.forEach { it.collectImports(file) }
        classes.forEach { it.collectImports(file) }
        objects.forEach { it.collectImports(file) }
        functions.forEach { it.collectImports(file) }
    }

    /**
     * @return the string form of this class
     */
    override fun toString(): String {
        return outputFile(File(".")).toString()
    }

    fun resolve(type: KibbleType): KibbleType {
//        val namespace = imports.groupBy { it.alias ?: it.type.className }
//                .mapValues { it.value[0] }
//
        val imported = importDirectives[type.fqcn()] ?: importDirectives
                .filterValues { it.alias == type.className || it.type.className == type.className }
                .map { it.value }
                .firstOrNull()
//        importDirectives[type.resolvedName]
        when {
            imported != null -> {
                if (type.pkgName == null) {
                    type.pkgName = imported.type.pkgName
                }
                type.resolvedName = imported.alias ?: imported.type.resolvedName
            }

            imported == null -> when {
                type.pkgName == null /*&& classes.any { it.name == type.className }*/ -> {
                    type.pkgName = pkgName
                    type.resolvedName = type.className
                }
                type.pkgName != pkgName -> addImport(type)
                else -> type.resolvedName = type.className
            }

            else -> throw RuntimeException("how did I get here?  type = $type, file = $this") // type.resolvedName = type.fqcn()
        }

        type.typeParameters.forEach {
            it.type?.let { resolve(it) }
        }
        return type
    }
}
