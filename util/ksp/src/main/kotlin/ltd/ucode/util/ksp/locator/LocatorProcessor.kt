package ltd.ucode.util.ksp.locator

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import kotlin.reflect.KClass

class LocatorProcessor(private val environment: SymbolProcessorEnvironment): SymbolProcessor {
    private fun KClass<*>.getTarget(): KClass<*> =
        members.single { it.returnType == KClass::class }.call() as KClass<*>

    private fun Resolver.handleLocator(target: KClass<*>, classes: List<KSClassDeclaration>): List<KSAnnotated> {
        if (classes.isEmpty()) return emptyList()

        val packageName = target.java.getPackage().name
        val objectName = "${target.simpleName!!}ImplList"

        val sourceFiles = classes.mapNotNull { it.containingFile }
        val fileText = buildString {
            append("package ")
            append(packageName)
            appendLine()
            appendLine()
            append("import ")
            append(target.qualifiedName!!)
            appendLine()
            append("import java.util.LinkedList")
            appendLine()
            classes.forEach { implementation ->
                append("import ")
                append(implementation.qualifiedName!!.asString())
                appendLine()
            }
            appendLine()
            append("object ")
            append(objectName)
            append(" : LinkedList<")
            append(target.simpleName)
            append("> { init {")
            classes.forEach { implementation ->
                append("  add(")
                append(implementation.simpleName.asString())
                append("())")
            }
            append("} }")
        }
        val file = environment.codeGenerator.createNewFile(
            Dependencies(false, *sourceFiles.toList().toTypedArray()),
            packageName, objectName
        )

        file.write(fileText.toByteArray())
        return classes.toList()
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classes: Sequence<KSClassDeclaration> =
            resolver.getSymbolsWithAnnotation(Locator::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
        val targets = classes.groupBy {
            it.getAnnotationsByType(Locator::class).single().kClass
        }

        return targets.flatMap { (target, classList) ->
            resolver.handleLocator(target, classList)
        }
    }
}

