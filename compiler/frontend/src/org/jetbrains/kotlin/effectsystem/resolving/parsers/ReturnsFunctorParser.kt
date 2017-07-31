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

package org.jetbrains.kotlin.effectsystem.resolving.parsers

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.effectsystem.adapters.ValueIdsFactory
import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.factories.EffectSchemasFactory
import org.jetbrains.kotlin.effectsystem.factories.ValuesFactory
import org.jetbrains.kotlin.effectsystem.impls.ESVariable
import org.jetbrains.kotlin.effectsystem.resolving.FunctorParser
import org.jetbrains.kotlin.effectsystem.resolving.utility.ConditionParser
import org.jetbrains.kotlin.effectsystem.resolving.utility.ConstantsParser
import org.jetbrains.kotlin.effectsystem.resolving.utility.UtilityParsers
import org.jetbrains.kotlin.effectsystem.structure.ESFunctor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo

class ReturnsFunctorParser : FunctorParser {
    private val RETURNS_EFFECT = FqName("kotlin.internal.Returns")

    override fun tryParseFunctor(resolvedCall: ResolvedCall<*>): ESFunctor? {
        val condition = UtilityParsers.conditionParser.parseCondition(resolvedCall) ?: return null

        val returnsAnnotation = resolvedCall.resultingDescriptor.annotations.findAnnotation(RETURNS_EFFECT) ?: return null
        val returnsArg = UtilityParsers.constantsParser.parseConstantValue(returnsAnnotation.allValueArguments.values.singleOrNull()) ?: ValuesFactory.UNKNOWN_CONSTANT

        return EffectSchemasFactory.singleClause(condition, ESReturns(returnsArg), getParameters(resolvedCall))
    }

    private fun getParameters(resolvedCall: ResolvedCall<*>): List<ESVariable> {
        val allParameters = mutableListOf<ESVariable>()
        resolvedCall.resultingDescriptor.valueParameters.mapTo(allParameters) { it.toESVariable() }
        resolvedCall.resultingDescriptor.extensionReceiverParameter?.extensionReceiverToESVariable()?.let { allParameters += it }
        return allParameters
    }

    private fun ValueParameterDescriptor.toESVariable(): ESVariable {
        val dfv = DataFlowValue(IdentifierInfo.Variable(this, DataFlowValue.Kind.STABLE_VALUE, null), type)
        return ESVariable(ValueIdsFactory.dfvBased(dfv), type)
    }

    private fun ReceiverParameterDescriptor.extensionReceiverToESVariable(): ESVariable {
        val dfv = DataFlowValue(IdentifierInfo.Receiver(value), type)
        return ESVariable(ValueIdsFactory.dfvBased(dfv), type)
    }
}