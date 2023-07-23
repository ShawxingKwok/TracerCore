package pers.shawxingkwok.tracer.typesystem

import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import pers.shawxingkwok.ksputil.alsoRegister
import pers.shawxingkwok.ksputil.simpleName
import pers.shawxingkwok.ktutil.updateIf
import pers.shawxingkwok.tracer.Tracer
import pers.shawxingkwok.tracer.TracerGeneration
import pers.shawxingkwok.tracer.util.isAnnotatedRootOrNodes
import pers.shawxingkwok.tracer.util.isNativeKt

private val cache = mutableMapOf<Pair<KSClassDeclaration, Boolean>, List<Type.Specific>>().alsoRegister()

internal fun KSClassDeclaration.getSuperSpecificRawTypes(isSrc: Boolean): List<Type.Specific> =
    if (!isNativeKt()) emptyList()
    else cache.getOrPut(this to isSrc) {
        val currentSuperKlasses = mutableSetOf<KSClassDeclaration>()

        superTypes.filterNot { it.isAnnotationPresent(Tracer.Omit::class) }
            .map { typeRef ->
                // remove '?' since they may be converted from some alias types with '?'
                typeRef.toProto().convertAlias().convertStar().updateNullability(false) as Type.Specific
            }
            .filterNot {
                // for source root/nodes classes, super types of their super root/nodes classes
                // were implemented.
                isSrc && it.decl.isAnnotatedRootOrNodes()
                || it.decl.isAnnotationPresent(TracerGeneration.Interface::class)
                || it.decl == Type.`Any？`.decl
                || it.decl in currentSuperKlasses
            }
            .onEach { currentSuperKlasses += it.decl }
            .flatMap { basicSpecificSuperType ->
                val map = basicSpecificSuperType.args.associateBy { it.param.simpleName() }

                val upperUpdatedSuperTypes = basicSpecificSuperType.decl
                    .getSuperSpecificRawTypes(isSrc)
                    .filterNot { it.decl in currentSuperKlasses }
                    .onEach { currentSuperKlasses += it.decl }
                    .map { type -> type.updateIf({ map.any() }){ it.convertGeneric(map).first } }

                listOf(basicSpecificSuperType) + upperUpdatedSuperTypes
            }
            .toList()
    }