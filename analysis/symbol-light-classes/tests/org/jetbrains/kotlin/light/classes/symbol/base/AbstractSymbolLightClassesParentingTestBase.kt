/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AssertionsService
import java.nio.file.Path

open class AbstractSymbolLightClassesParentingTestBase(
    configurator: AnalysisApiTestConfigurator,
    override val currentExtension: String,
    override val stopIfCompilationErrorDirectivePresent: Boolean
) : AbstractSymbolLightClassesTestBase(configurator) {
    override fun getRenderResult(ktFile: KtFile, ktFiles: List<KtFile>, testDataFile: Path, module: TestModule, project: Project): String {
        throw IllegalStateException("This test is not rendering light elements")
    }

    protected fun createLightElementsVisitor(assertions: AssertionsService) = object : JavaElementVisitor() {
        private val declarationStack = ArrayDeque<PsiElement>()

        private fun <T : PsiElement> checkParentAndVisitChildren(declaration: T?, action: T.(visitor: JavaElementVisitor) -> Unit = {}) {
            if (declaration == null) return
            checkDeclarationParent(declaration)

            declarationStack.addLast(declaration)
            try {
                if (declaration is PsiModifierListOwner) {
                    declaration.modifierList?.accept(this)
                }

                if (declaration is PsiParameterListOwner) {
                    declaration.parameterList.accept(this)
                }

                if (declaration is PsiTypeParameterListOwner) {
                    declaration.typeParameterList?.accept(this)
                }

                declaration.action(this)
            } finally {
                val removed = declarationStack.removeLast()
                assertions.assertEquals(declaration, removed)
            }
        }

        override fun visitModifierList(list: PsiModifierList?) {
            checkParentAndVisitChildren(list) { visitor ->
                annotations.forEach { it.accept(visitor) }
            }
        }

        override fun visitParameterList(list: PsiParameterList?) {
            checkParentAndVisitChildren(list) { visitor ->
                parameters.forEach { it.accept(visitor) }
            }
        }

        override fun visitTypeParameterList(list: PsiTypeParameterList?) {
            checkParentAndVisitChildren(list) { visitor ->
                typeParameters.forEach { it.accept(visitor) }
            }
        }

        override fun visitClass(aClass: PsiClass?) {
            checkParentAndVisitChildren(aClass) { visitor ->
                annotations.forEach { it.accept(visitor) }

                fields.forEach { it.accept(visitor) }
                methods.forEach { it.accept(visitor) }
                innerClasses.forEach { it.accept(visitor) }
            }
        }

        override fun visitField(field: PsiField?) {
            checkParentAndVisitChildren(field) { visitor ->
                annotations.forEach { it.accept(visitor) }

                type.annotations.forEach { it.accept(visitor) }
            }
        }

        override fun visitMethod(method: PsiMethod?) {
            checkParentAndVisitChildren(method) { visitor ->
                annotations.forEach { it.accept(visitor) }

                returnType?.annotations?.forEach { it.accept(visitor) }
            }
        }

        override fun visitParameter(parameter: PsiParameter?) {
            checkParentAndVisitChildren(parameter) { visitor ->
                annotations.forEach { it.accept(visitor) }
            }
        }

        override fun visitTypeParameter(classParameter: PsiTypeParameter?) {
            checkParentAndVisitChildren(classParameter) { visitor ->
                annotations.forEach { it.accept(visitor) }
            }
        }

        private fun checkDeclarationParent(declaration: PsiElement) {
            val expectedParent = declarationStack.lastOrNull() ?: return
            val parent = declaration.parent
            assertions.assertNotNull(parent) { "Parent should not be null for ${declaration::class} with text ${declaration.text}" }
            assertions.assertEquals(expectedParent, parent) {
                "Unexpected parent for ${declaration::class} with text ${declaration.text}"
            }
        }

        override fun visitAnnotation(annotation: PsiAnnotation?) {
            if (annotation == null) return

            val owner = annotation.owner
            assertions.assertNotNull(owner)

            val lastDeclaration = declarationStack.last()
            val psiModifierListOwner = if (lastDeclaration is PsiModifierListOwner) {
                assertions.assertEquals(lastDeclaration.modifierList, owner)
                lastDeclaration
            } else {
                (lastDeclaration as PsiModifierList).parent
            } as PsiModifierListOwner

            when (psiModifierListOwner) {
                is PsiClass,
                is PsiParameter ->
                    assertions.assertTrue(owner is SymbolLightClassModifierList<*>)

                is PsiField,
                is PsiMethod ->
                    assertions.assertTrue(owner is SymbolLightMemberModifierList<*>)

                else ->
                    throw IllegalStateException("Unexpected annotation owner kind: ${lastDeclaration::class.java}")
            }

            val modifierList = psiModifierListOwner.modifierList!!
            assertions.assertTrue(modifierList.annotations.any { it == annotation })

            val qualifiedName = annotation.qualifiedName!!
            assertions.assertTrue(modifierList.hasAnnotation(qualifiedName))

            val anno = modifierList.findAnnotation(qualifiedName)
            assertions.assertNotNull(anno)
            assertions.assertTrue(annotation == anno || modifierList.annotations.count { it.qualifiedName == qualifiedName } > 1)
        }
    }
}