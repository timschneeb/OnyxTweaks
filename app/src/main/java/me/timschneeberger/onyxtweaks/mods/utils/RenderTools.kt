package me.timschneeberger.onyxtweaks.mods.utils

import com.github.kyuubiran.ezxhelper.Log
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.IdentityHashMap
import kotlin.math.max

private const val MAX_DEPTH = 4

/**
 * Recursively renders a complex object to the Xposed log.
 */
@Suppress("unused")
fun Any?.renderToXLog(prefix: String? = null, multiline: Boolean = true, maxDepth: Int = MAX_DEPTH) {
    renderToString(multiline, maxDepth)
        .lines()
        .forEach { line ->
            Log.ex(
                if (prefix.isNullOrBlank()) line else "$prefix: $line"
            )
        }
}

/**
 * Recursively renders a complex object to the logcat log.
 */
@Suppress("unused")
fun Any?.renderToLog(prefix: String? = null, multiline: Boolean = true, maxDepth: Int = MAX_DEPTH) {
    renderToString(multiline, maxDepth)
        .lines()
        .forEach { line -> Log.e(if (prefix.isNullOrBlank()) line else "$prefix: $line") }
}

fun Any?.renderToString(multiline: Boolean = false, maxDepth: Int = MAX_DEPTH): String {
    return renderInternal(this, IdentityHashMap(), multiline, 0, maxDepth)
}

private fun renderInternal(
    obj: Any?,
    visited: IdentityHashMap<Any, Unit>,
    multiline: Boolean,
    indentLevel: Int,
    maxDepth: Int
): String {
    if(indentLevel > maxDepth) return "... (max depth reached)"

    try {
        return when {
            obj == null -> "null"
            obj is String -> "\"$obj\""
            obj is Char -> "'$obj'"
            obj is Number || obj is Boolean -> obj.toString()
            obj is Enum<*> -> obj.name
            obj is Map<*, *> -> renderMap(obj, visited, multiline, indentLevel, maxDepth)
            obj is Collection<*> -> renderCollection(obj, visited, multiline, indentLevel, maxDepth)
            obj.javaClass.isArray -> renderArray(obj, visited, multiline, indentLevel, maxDepth)
            else -> renderObject(obj, visited, multiline, indentLevel, maxDepth)
        }
    }
    catch (e: Exception) {
        Log.ex(e, "Failed to render object: $obj")
        return "<${e.javaClass.simpleName}> (Failed to render '${e.message}')"
    }
}

private fun renderMap(
    map: Map<*, *>,
    visited: IdentityHashMap<Any, Unit>,
    multiline: Boolean,
    indentLevel: Int,
    maxDepth: Int
): String {
    return if (multiline) {
        val indent = getIndent(indentLevel)
        val entryIndent = getIndent(indentLevel + 1)
        map.entries.joinToString(
            prefix = "${indent}{\n$entryIndent",
            separator = ",\n$entryIndent",
            postfix = "\n$indent}",
            transform = { (k, v) ->
                "${renderInternal(k, visited, true, indentLevel + 1, maxDepth)}=${
                    renderInternal(v, visited, true, indentLevel + 1, maxDepth)
                }"
            }
        )
    } else {
        map.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
            "${renderInternal(k, visited, false, 0, maxDepth)}=${renderInternal(v, visited, false, 0, maxDepth)}"
        }
    }
}

private fun renderCollection(
    collection: Collection<*>,
    visited: IdentityHashMap<Any, Unit>,
    multiline: Boolean,
    indentLevel: Int,
    maxDepth: Int
): String {
    return if (multiline) {
        val indent = getIndent(indentLevel)
        val elementIndent = getIndent(indentLevel + 1)
        collection.joinToString(
            prefix = "${indent}[\n$elementIndent",
            separator = ",\n$elementIndent",
            postfix = "\n$indent]",
            transform = { renderInternal(it, visited, true, indentLevel + 1, maxDepth) }
        )
    } else {
        collection.joinToString(prefix = "[", postfix = "]") {
            renderInternal(it, visited, false, 0, maxDepth)
        }
    }
}

private fun renderArray(
    array: Any,
    visited: IdentityHashMap<Any, Unit>,
    multiline: Boolean,
    indentLevel: Int,
    maxDepth: Int
): String {
    val length = Array.getLength(array)
    return if (multiline) {
        val indent = getIndent(indentLevel)
        val elementIndent = getIndent(indentLevel + 1)
        (0 until length).joinToString(
            prefix = "${indent}[\n$elementIndent",
            separator = ",\n$elementIndent",
            postfix = "\n$indent]"
        ) { i ->
            renderInternal(Array.get(array, i), visited, true, indentLevel + 1, maxDepth)
        }
    } else {
        (0 until length).joinToString(prefix = "[", postfix = "]") { i ->
            renderInternal(Array.get(array, i), visited, false, 0, maxDepth)
        }
    }
}

private fun renderObject(
    obj: Any,
    visited: IdentityHashMap<Any, Unit>,
    multiline: Boolean,
    indentLevel: Int,
    maxDepth: Int
): String {
    if (visited.containsKey(obj)) return "..."
    visited[obj] = Unit

    val fields = getFields(obj.javaClass)
    val fieldStrings = fields.map { field ->
        field.isAccessible = true
        val value = try {
            field.get(obj)
        } catch (e: Exception) {
            "<${e.javaClass.simpleName}>"
        }
        "${field.name}=${renderInternal(value, visited, multiline, indentLevel + 1, maxDepth)}"
    }

    return if (multiline) {
        val prefixIndent = getIndent(indentLevel - 1)
        val indent = getIndent(indentLevel)
        val fieldIndent = getIndent(indentLevel + 1)
        fieldStrings.joinToString(
            prefix = "${prefixIndent}${obj.javaClass.simpleName}(\n$fieldIndent",
            separator = ",\n$fieldIndent",
            postfix = "\n$indent)"
        )
    } else {
        "${obj.javaClass.simpleName}(${fieldStrings.joinToString(", ")})"
    }
}

private fun getFields(clazz: Class<*>): List<Field> {
    val fields = mutableListOf<Field>()
    try {
        var currentClass: Class<*>? = clazz
        while (currentClass != null && currentClass != Any::class.java) {
            currentClass.declaredFields
                .filter { !Modifier.isStatic(it.modifiers) }
                .let(fields::addAll)
            currentClass = currentClass.superclass
        }
    }
    catch (e: Exception) {
        Log.ex("Error getting fields for class ${clazz.simpleName}: $e")
    }
    return fields
}

private fun getIndent(level: Int): String = "    ".repeat(max(level, 0))