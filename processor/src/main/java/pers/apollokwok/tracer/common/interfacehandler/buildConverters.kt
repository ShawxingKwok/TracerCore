package pers.apollokwok.tracer.common.interfacehandler

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSClassDeclaration
import pers.apollokwok.ksputil.Environment
import pers.apollokwok.ksputil.createFile
import pers.apollokwok.ksputil.noPackageName
import pers.apollokwok.ksputil.packageName
import pers.apollokwok.tracer.common.shared.*
import pers.apollokwok.tracer.common.util.SUPPRESSING
import pers.apollokwok.tracer.common.util.isAnnotatedRootOrNodes
import pers.apollokwok.tracer.common.util.moduleVisibility
import pers.apollokwok.tracer.common.util.trimMarginAndRepeatedBlankLines

internal fun buildConverters(){
    getRootNodesKlasses().forEach(::buildConverter)
}

private fun buildConverter(klass: KSClassDeclaration){
    val (interfaceName, outerInterfaceName) = getInterfaceNames(klass)

    val namePairs = klass.tracerInterfaces.toList()
        .map {
            it.getAllProperties()
                .toList()
                .sortedBy { prop -> prop.type.resolve().declaration.toString() }
        }
        .run { get(0).zip(get(1)) }

    val v = if (Tags.AllInternal) "internal" else klass.moduleVisibility()!!.name.lowercase()

    val decls = namePairs.joinToString("\n        ") {
        (name, _name) -> "override val `$_name` get() = this@$interfaceName.`$name`"
    }

    val outerDecls = namePairs.joinToString("\n        ") {
        (name, _name) -> "override val `$name` get() = this@$outerInterfaceName.`$_name`"
    }

    val content =
        """
        |$SUPPRESSING
        |
        |${if (klass.packageName().any()) "package ${klass.packageName()}" else "" }
        |
        |$v val $interfaceName.`_$outerInterfaceName`: $outerInterfaceName inline get() = 
        |    object : $outerInterfaceName{
        |        $decls
        |    }
        |   
        |$v val $outerInterfaceName.`__$interfaceName`: $interfaceName inline get() = 
        |    object : $interfaceName{
        |        $outerDecls
        |    }
        """.trimMarginAndRepeatedBlankLines()

    Environment.codeGenerator.createFile(
        packageName = klass.packageName(),
        fileName = klass.noPackageName() + "Converters",
        dependencies = Dependencies(
            aggregating = false,
            sources = klass
                .getAllSuperTypes()
                .map { it.declaration }
                .filter { it.isAnnotatedRootOrNodes() }
                .mapNotNull { it.containingFile }
                .toList()
                .toTypedArray()
        ),
        content = content
    )
}