package io.kvarto.mock

import io.github.classgraph.ClassGraph
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.TreeSet
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.internal.impl.renderer.DescriptorRenderer
import kotlin.reflect.jvm.jvmErasure


fun generateMocks(packageName: String) {
    val scanResult = ClassGraph().enableAllInfo().whitelistPackages(packageName).scan()
    scanResult.use {
        scanResult.allInterfaces.forEach { elem ->
            generateMock(elem.loadClass().kotlin)
        }
    }
}

private fun generateMock(klass: KClass<out Any>) {
    val imports = HashSet<String>()
    val paramLines = ArrayList<String>()
    val methodLines = ArrayList<String>()

    imports += klass.qualifiedName!!
    fun addToImports(t: KType?) {
        t?.jvmErasure?.qualifiedName?.let(imports::add)
        t?.arguments?.forEach { addToImports(it.type) }
    }

    val classNameLine = "class Mock${klass.simpleName} ("
    val paramNames = TreeSet<String>()
    klass.declaredMemberFunctions.forEach { member ->
        val name = findName(member.name, paramNames)
        paramNames += name
        val parameters = member.parameters.drop(1)
        parameters.forEach { addToImports(it.type) }
        addToImports(member.returnType)
        val params = parameters.joinToString { it.type.toString() }
        paramLines += "  val $name: ($params) -> ${member.returnType}"
        val methodParams = parameters.joinToString { "${it.name}: ${it.type}" }
        val argNames = parameters.joinToString { it.name.toString() }
        methodLines += "  override fun ${member.name}($methodParams): ${member.returnType} =\n    $name($argNames)"
    }

    val signatureLastLine = "): ${klass.simpleName} {"

    val importLines = imports.joinToString("\n") { "import $it" }
    println("")
    println(importLines)
    println(classNameLine)
    println(paramLines.joinToString(",\n"))
    println(signatureLastLine)
    println(methodLines.joinToString("\n"))
    println("}\n")
}

fun findName(originalName: String, existingNames: Set<String>): String =
    (0..Int.MAX_VALUE).asSequence().map { originalName + if (it == 0) "" else it }.first { it !in existingNames }

fun main() {
    hackRenderer()

//    StringMultiMap::class.declaredMemberFunctions.forEach {
//        println(it.returnType.toString())
//    }

    generateMocks("io.kvarto")
}

private fun hackRenderer() {
    val clazz = Class.forName("kotlin.reflect.jvm.internal.ReflectionObjectRenderer")
    val field = clazz.getDeclaredField("renderer").apply {
        isAccessible = true
    }
    Field::class.java.getDeclaredField("modifiers").apply {
        isAccessible = true
        setInt(field, field.modifiers and Modifier.FINAL.inv())
    }
    field.set(null, DescriptorRenderer.COMPACT_WITH_SHORT_TYPES)
}





