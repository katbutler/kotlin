/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.effectsystem.adapters

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.effectsystem.structure.ESEffect
import org.jetbrains.kotlin.effectsystem.structure.EffectSchema
import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.effects.ESThrows
import org.jetbrains.kotlin.effectsystem.factories.ValuesFactory
import org.jetbrains.kotlin.effectsystem.factories.lift
import org.jetbrains.kotlin.effectsystem.resolving.FunctorResolver
import org.jetbrains.kotlin.effectsystem.visitors.Reducer
import org.jetbrains.kotlin.effectsystem.visitors.SchemaBuilder
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo

class EffectSystem {
    private val functorResolver = FunctorResolver()

    fun getResultDFI(
            resolvedCall: ResolvedCall<*>,
            bindingTrace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
            moduleDescriptor: ModuleDescriptor
    ): DataFlowInfo {
        val callExpression = resolvedCall.call.callElement as? KtExpression ?: return DataFlowInfo.EMPTY
        if (callExpression is KtDeclaration) return DataFlowInfo.EMPTY
        return getDFIWhen(ESReturns(ValuesFactory.UNKNOWN_CONSTANT), callExpression,
                          bindingTrace, languageVersionSettings, moduleDescriptor)
    }

    fun getConditionalInfoForThenBranch(
            condition: KtExpression?,
            bindingTrace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
            moduleDescriptor: ModuleDescriptor
    ): DataFlowInfo {
        if (condition == null) return DataFlowInfo.EMPTY
        return getDFIWhen(ESReturns(true.lift()), condition, bindingTrace, languageVersionSettings, moduleDescriptor)
    }

    fun getConditionalInfoForElseBranch(
            condition: KtExpression?,
            bindingTrace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
            moduleDescriptor: ModuleDescriptor
    ): DataFlowInfo {
        if (condition == null) return DataFlowInfo.EMPTY
        return getDFIWhen(ESReturns(false.lift()), condition, bindingTrace, languageVersionSettings, moduleDescriptor)
    }

    private fun getDFIWhen(
            observedEffect: ESEffect,
            expression: KtExpression,
            bindingTrace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
            moduleDescriptor: ModuleDescriptor
    ): DataFlowInfo {
        val schema = getSchema(expression, bindingTrace, moduleDescriptor) ?: return DataFlowInfo.EMPTY

        val extractedContextInfo = InfoCollector(observedEffect).collectFromSchema(schema)

        return extractedContextInfo.toDataFlowInfo(languageVersionSettings)
    }

    private fun getSchema(expression: KtExpression, bindingTrace: BindingTrace, moduleDescriptor: ModuleDescriptor): EffectSchema? {
        if (bindingTrace[BindingContext.EXPRESSION_EFFECTS, expression] == null) {
            val evaluatedSchema = evaluateSchema(expression, bindingTrace.bindingContext, moduleDescriptor) ?: return null
            bindingTrace.record(BindingContext.EXPRESSION_EFFECTS, expression, evaluatedSchema)
        }

        return bindingTrace[BindingContext.EXPRESSION_EFFECTS, expression]
    }

    private fun evaluateSchema(expression: KtExpression, bindingContext: BindingContext, moduleDescriptor: ModuleDescriptor): EffectSchema? {
        val ctBuilder = CallTreeBuilder(bindingContext, moduleDescriptor, functorResolver)
        val callTree = expression.accept(ctBuilder, Unit)

        val esBuilder = SchemaBuilder()
        val schema = callTree.accept(esBuilder) ?: return null

        val reducedSchema = Reducer().reduceSchema(schema)

        return reducedSchema
    }
}