package me.timschneeberger.onyxtweaks.mod_processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

class ModPackProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private var generatedFile: FileSpec? = null

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(TargetPackages::class.qualifiedName!!)
        val grouped = mutableMapOf<String, MutableList<KSClassDeclaration>>()

        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDecl ->
            val annotation = classDecl.getAnnotationsByType(TargetPackages::class).first()
            annotation.targets.forEach { pkg ->
                grouped.getOrPut(pkg) { mutableListOf() }.add(classDecl)
            }
        }

        // Include global mods in all packages
        grouped["#global"]?.let { globals ->
            grouped.forEach { key, _ ->
                globals.forEach { global ->
                    if (grouped[key]?.contains(global) == false)
                        grouped[key]?.add(global)
                }
            }
        }

        generatedFile = generateRegistry(grouped)
        return emptyList()
    }

    override fun finish() {
        generatedFile?.writeTo(environment.codeGenerator, false)
        super.finish()
    }

    private fun generateRegistry(grouped: Map<String, List<KSClassDeclaration>>): FileSpec =
        FileSpec.builder("me.timschneeberger.onyxtweaks.mods", "ModRegistry")
            .addType(
                TypeSpec.objectBuilder("ModRegistry")
                    .addProperty(
                        PropertySpec.builder("mods", MOD_LIST)
                            .initializer(
                                CodeBlock.builder()
                                    .add(buildListInitializer(grouped.values.flatten().distinct()))
                                    .build()
                            )
                            .build()

                    )
                    .addProperty(
                        PropertySpec.builder("modsWithZygoteInit", MOD_LIST)
                            .initializer(buildListInitializer(
                                grouped.values.flatten().filter { def ->
                                    def.getAllSuperTypes().any {
                                        type -> type.toClassName() == ClassName("me.timschneeberger.onyxtweaks.mods.base", "IEarlyZygoteHook")
                                    }
                                }
                            ))
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("modsByPackage", STRING_TO_MOD_MAP)
                            .initializer(buildMapInitializer(grouped))
                            .build()
                    )
                    .build()
            )
            .build()

    private fun buildMapInitializer(grouped: Map<String, List<KSClassDeclaration>>): CodeBlock {
        return CodeBlock.builder().apply {
            add("mapOf(\n")
            grouped.entries.forEachIndexed { index, (pkg, classes) ->
                // Add comma separator between entries (but not after last one)
                if (index > 0) add(",\n")

                add("%S to listOf(", pkg)
                classes.forEachIndexed { classIndex, className ->
                    if (classIndex > 0) add(", ")
                    add("%T::class", className.toClassName())
                }
                add(")")
            }
            add("\n)")
        }.build()
    }

    private fun buildListInitializer(list: List<KSClassDeclaration>): CodeBlock {
        return CodeBlock.builder().apply {
            add("listOf(\n")
            list.forEachIndexed { classIndex, className ->
                if (classIndex > 0) add(", \n")
                add("%T::class", className.toClassName())
            }
            add("\n)")
        }.build()
    }

    companion object {
        private val MOD_BASE_CLASS = ClassName("me.timschneeberger.onyxtweaks.mods.base", "ModPack")

        val STRING_TO_MOD_MAP = Map::class.asClassName()
            .parameterizedBy(
                String::class.asClassName(),
                List::class.asClassName()
                    .parameterizedBy(
                        KClass::class.asClassName()
                            .parameterizedBy(
                                WildcardTypeName.producerOf(MOD_BASE_CLASS)
                            )
                    )
            )

        val MOD_LIST = List::class.asClassName()
            .parameterizedBy(
                KClass::class.asClassName()
                    .parameterizedBy(
                        WildcardTypeName.producerOf(MOD_BASE_CLASS)
                    )
            )
    }
}