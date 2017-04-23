package com.antwerkz.kibble

import com.antwerkz.kibble.model.KibbleFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer.PLAIN_FULL_PATHS
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import java.io.File

/**
 * This is the primary entry point for parsing existing Kotlin code
 */
class Kibble {
    companion object {
        /**
         * Parses source found at the given path.
         *
         * @return the KibbleFile
         */
        @JvmStatic
        fun parseFile(file: String): KibbleFile {
            return parse(File(file))[0]
        }

        /**
         * Parses source found at the given path.
         *
         * @return the KibbleFile
         */
        @JvmStatic
        fun parseFile(file: File): KibbleFile {
            return parse(file.absoluteFile)[0]
        }

        /**
         * Parses sources found at the given path. This String can represent a source file or a directory to scan for sources
         *
         * @return the list of KibbleFiles from sources found at the given path
         */
        @JvmStatic
        fun parse(path: File): List<KibbleFile> {
            val configuration = CompilerConfiguration()
            configuration.put(CompilerConfigurationKey.create<File>("output directory"), File(""))
            configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                    PrintingMessageCollector(System.err, PLAIN_FULL_PATHS, false))
            configuration.addKotlinSourceRoot(path.absolutePath)

            return KotlinCoreEnvironment
                    .createForProduction(Disposable { }, configuration, listOf())
                    .getSourceFiles()
                    .map(::KibbleFile)
        }

        /**
         * Parses a code snippet in to a Kibble model
         *
         * @return a KibbleFile holding the results of the parsed snippet
         */
        @JvmStatic
        fun parseSource(source: String): KibbleFile {
            val tempFile = File.createTempFile("kibble-", ".kt")
            tempFile.deleteOnExit()

            try {
                tempFile.writeText(source)
                return parse(tempFile.absoluteFile)[0]
            } finally {
                tempFile.delete()
            }
        }

    }
}
