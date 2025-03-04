/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.junit.jupiter.api.Tag;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/binaryCompatibility/klibEvolution")
@TestDataPath("$PROJECT_ROOT")
@Tag("k1KlibCompatibility")
public class KlibBinaryCompatibilityTestGenerated extends AbstractNativeKlibBinaryCompatibilityTest {
    @Test
    @TestMetadata("addAbstractMemberBody.kt")
    public void testAddAbstractMemberBody() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addAbstractMemberBody.kt");
    }

    @Test
    @TestMetadata("addCompanionObject.kt")
    public void testAddCompanionObject() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addCompanionObject.kt");
    }

    @Test
    @TestMetadata("addDefaultImplementations.kt")
    public void testAddDefaultImplementations() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addDefaultImplementations.kt");
    }

    @Test
    @TestMetadata("addEnumClassMember.kt")
    public void testAddEnumClassMember() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addEnumClassMember.kt");
    }

    @Test
    @TestMetadata("addLateinitToVar.kt")
    public void testAddLateinitToVar() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addLateinitToVar.kt");
    }

    @Test
    @TestMetadata("addOpenToClass.kt")
    public void testAddOpenToClass() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addOpenToClass.kt");
    }

    @Test
    @TestMetadata("addOpenToMember.kt")
    public void testAddOpenToMember() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addOpenToMember.kt");
    }

    @Test
    @TestMetadata("addOrRemoveConst.kt")
    public void testAddOrRemoveConst() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addOrRemoveConst.kt");
    }

    @Test
    @TestMetadata("addOrRemoveInitBlock.kt")
    public void testAddOrRemoveInitBlock() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addOrRemoveInitBlock.kt");
    }

    @Test
    @TestMetadata("addOverloads.kt")
    public void testAddOverloads() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addOverloads.kt");
    }

    @Test
    @TestMetadata("addParameterDefaulValue.kt")
    public void testAddParameterDefaulValue() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addParameterDefaulValue.kt");
    }

    @Test
    @TestMetadata("addPropertyAccessor.kt")
    public void testAddPropertyAccessor() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addPropertyAccessor.kt");
    }

    @Test
    @TestMetadata("addingSealedClassMember.kt")
    public void testAddingSealedClassMember() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/addingSealedClassMember.kt");
    }

    @Test
    public void testAllFilesPresentInKlibEvolution() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/binaryCompatibility/klibEvolution"), Pattern.compile("^(.+)\\.kt$"), null, false);
    }

    @Test
    @TestMetadata("changeBaseClassOrder.kt")
    public void testChangeBaseClassOrder() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/changeBaseClassOrder.kt");
    }

    @Test
    @TestMetadata("changeCompanionToNestedObject.kt")
    public void testChangeCompanionToNestedObject() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/changeCompanionToNestedObject.kt");
    }

    @Test
    @TestMetadata("changeConstInitialization.kt")
    public void testChangeConstInitialization() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/changeConstInitialization.kt");
    }

    @Test
    @TestMetadata("changeNamesOfTypeParameters.kt")
    public void testChangeNamesOfTypeParameters() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/changeNamesOfTypeParameters.kt");
    }

    @Test
    @TestMetadata("changeObjectToCompanion.kt")
    public void testChangeObjectToCompanion() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/changeObjectToCompanion.kt");
    }

    @Test
    @TestMetadata("changeParameterDefaultValue.kt")
    public void testChangeParameterDefaultValue() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/changeParameterDefaultValue.kt");
    }

    @Test
    @TestMetadata("changePropertyFromValToVar.kt")
    public void testChangePropertyFromValToVar() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/changePropertyFromValToVar.kt");
    }

    @Test
    @TestMetadata("changePropertyInitialization.kt")
    public void testChangePropertyInitialization() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/changePropertyInitialization.kt");
    }

    @Test
    @TestMetadata("constructorParameterMarkValVar.kt")
    public void testConstructorParameterMarkValVar() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/constructorParameterMarkValVar.kt");
    }

    @Test
    @TestMetadata("deleteOverrideMember.kt")
    public void testDeleteOverrideMember() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/deleteOverrideMember.kt");
    }

    @Test
    @TestMetadata("deletePrivateMembers.kt")
    public void testDeletePrivateMembers() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/deletePrivateMembers.kt");
    }

    @Test
    @TestMetadata("inlineBodyChange.kt")
    public void testInlineBodyChange() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/inlineBodyChange.kt");
    }

    @Test
    @TestMetadata("inlineFunction.kt")
    public void testInlineFunction() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/inlineFunction.kt");
    }

    @Test
    @TestMetadata("makeFunctionInfixOrTailrec.kt")
    public void testMakeFunctionInfixOrTailrec() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/makeFunctionInfixOrTailrec.kt");
    }

    @Test
    @TestMetadata("moreSpecificBaseClass.kt")
    public void testMoreSpecificBaseClass() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/moreSpecificBaseClass.kt");
    }

    @Test
    @TestMetadata("moveMemberUpInHierarchy.kt")
    public void testMoveMemberUpInHierarchy() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/moveMemberUpInHierarchy.kt");
    }

    @Test
    @TestMetadata("newFakeOverride.kt")
    public void testNewFakeOverride() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/newFakeOverride.kt");
    }

    @Test
    @TestMetadata("newOverrideMember.kt")
    public void testNewOverrideMember() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/newOverrideMember.kt");
    }

    @Test
    @TestMetadata("removeAbstractFromClass.kt")
    public void testRemoveAbstractFromClass() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/removeAbstractFromClass.kt");
    }

    @Test
    @TestMetadata("removeInfixOrTailrecFromFunction.kt")
    public void testRemoveInfixOrTailrecFromFunction() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/removeInfixOrTailrecFromFunction.kt");
    }

    @Test
    @TestMetadata("removeLateinitFromVar.kt")
    public void testRemoveLateinitFromVar() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/removeLateinitFromVar.kt");
    }

    @Test
    @TestMetadata("removePropertyAccessor.kt")
    public void testRemovePropertyAccessor() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/removePropertyAccessor.kt");
    }

    @Test
    @TestMetadata("renameArguments.kt")
    public void testRenameArguments() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/renameArguments.kt");
    }

    @Test
    @TestMetadata("reorderClassConstructors.kt")
    public void testReorderClassConstructors() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/reorderClassConstructors.kt");
    }

    @Test
    @TestMetadata("turnClassIntoDataClass.kt")
    public void testTurnClassIntoDataClass() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/turnClassIntoDataClass.kt");
    }

    @Test
    @TestMetadata("widenSuperMemberVisibility.kt")
    public void testWidenSuperMemberVisibility() throws Exception {
        runTest("compiler/testData/binaryCompatibility/klibEvolution/widenSuperMemberVisibility.kt");
    }
}
