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

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.effectsystem.effects.ESCalls
import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.effects.ESThrows
import org.jetbrains.kotlin.effectsystem.factories.ClausesFactory
import org.jetbrains.kotlin.effectsystem.functors.EqualFunctor
import org.jetbrains.kotlin.effectsystem.structure.ESFunctor
import org.jetbrains.kotlin.effectsystem.functors.IsFunctor
import org.jetbrains.kotlin.effectsystem.functors.InPlaceCallFunctor
import org.jetbrains.kotlin.effectsystem.structure.ESBooleanExpression
import org.jetbrains.kotlin.effectsystem.structure.ESEffect
import org.jetbrains.kotlin.effectsystem.impls.and
import org.jetbrains.kotlin.effectsystem.functors.UnknownFunctor
import org.jetbrains.kotlin.effectsystem.impls.*
import org.jetbrains.kotlin.effectsystem.impls.not
import org.jetbrains.kotlin.effectsystem.impls.or
import org.jetbrains.kotlin.effectsystem.factories.EffectSchemasFactory
import org.jetbrains.kotlin.effectsystem.factories.ValuesFactory
import org.jetbrains.kotlin.effectsystem.factories.lift
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

typealias JoinerT = (List<ESBooleanExpression>) -> ESBooleanExpression

class FunctorsResolver {
    companion object {
        val RETURNS_EFFECT = FqName("kotlin.internal.Returns")
        val THROWS_EFFECT = FqName("kotlin.internal.Throws")
        val CALLS_EFFECT = FqName("kotlin.internal.CalledInPlace")
        val EFFECT_ANNOTATIONS: List<FqName> = listOf(RETURNS_EFFECT, THROWS_EFFECT)

        val CONDITION_JOINING_ANNOTATION = FqName("kotlin.internal.JoinConditions")
        val ANY = FqName("kotlin.internal.JoiningStrategy.ANY")
        val ALL = FqName("kotlin.internal.JoiningStrategy.ALL")
        val NONE = FqName("kotlin.internal.JoiningStrategy.NONE")

        val EQUALS_CONDITION = FqName("kotlin.internal.Equals")
        val IS_INSTANCE_CONDITION = FqName("kotlin.internal.IsInstance")
        val NOT_CONDITION = FqName("kotlin.internal.Not")
    }

    fun resolve(resolvedCall: ResolvedCall<*>): ESFunctor {
        val effect = getEffect(resolvedCall) ?: return getCallsFunctor(resolvedCall) ?: UnknownFunctor
        val condition = getCondition(resolvedCall) ?: return UnknownFunctor
        val parameters = getParameters(resolvedCall)
        return EffectSchemasFactory.clauses(
                listOf(ClausesFactory.create(condition, listOf(effect))),
                parameters
        )
    }

    private fun getCallsFunctor(resolvedCall: ResolvedCall<*>): ESFunctor? {
        val argsEffects = mutableListOf<AnnotationDescriptor>()
        val isRelevantArg = mutableListOf<Boolean>()
        // Effect with target=FUNCTION is not found, let's try to find something on arguments
        resolvedCall.resultingDescriptor.valueParameters.forEach { param ->
            val callsEffect = param.annotations.findAnnotation(CALLS_EFFECT)
            if (callsEffect != null) {
                argsEffects.add(callsEffect)
            }
            isRelevantArg.add(callsEffect != null)
        }

        assert(argsEffects.size <= 1) { "Multi-effect annotations are not supported yet" }

        if (argsEffects.isEmpty()) return null

        val invocationCount = argsEffects[0].allValueArguments.values.singleOrNull()?.toInvocationCountEnum() ?: return null
        return InPlaceCallFunctor(invocationCount, isRelevantArg)
    }

    private fun getEffect(resolvedCall: ResolvedCall<*>): ESEffect? {
        val annotations = resolvedCall.resultingDescriptor.annotations
        val effectAnnotations = annotations.filter { EFFECT_ANNOTATIONS.contains(it.annotationClass?.fqNameSafe) }

        if (effectAnnotations.isEmpty()) return null

        // Effects that target function
        assert(effectAnnotations.size == 1) { "Multi-effect annotations are not supported yet" }
        return when (effectAnnotations[0].annotationClass?.fqNameSafe) {
            RETURNS_EFFECT -> {
                ESReturns(effectAnnotations[0].allValueArguments.values.singleOrNull()?.toESConstant() ?: return null)
            }
            THROWS_EFFECT -> {
                ESThrows(null)
            }
            else -> null
        }
    }

    private fun getCondition(resolvedCall: ResolvedCall<*>): ESBooleanExpression? {
        val joiner = getJoiner(resolvedCall)

        val primitiveConditions = mutableListOf<ESBooleanExpression>()
        resolvedCall.resultingDescriptor.valueParameters.flatMapTo(primitiveConditions) { getConditionsOnArgument(it) }

        val conditionOnReceiver = resolvedCall.resultingDescriptor.extensionReceiverParameter?.let { getConditionsOnReceiver(it) } ?: emptyList()
        primitiveConditions.addAll(conditionOnReceiver)

        if (primitiveConditions.isEmpty()) return null

        return joiner(primitiveConditions)
    }

    private fun getParameters(resolvedCall: ResolvedCall<*>): List<ESVariable> {
        val allParameters = mutableListOf<ESVariable>()
        resolvedCall.resultingDescriptor.valueParameters.mapTo(allParameters) { it.toESVariable() }
        resolvedCall.resultingDescriptor.extensionReceiverParameter?.extensionReceiverToESVariable()?.let { allParameters += it }
        return allParameters
    }

    private fun getJoiner(resolvedCall: ResolvedCall<*>): JoinerT {
        val joiningStrategyName = resolvedCall.resultingDescriptor.annotations
                .findAnnotation(CONDITION_JOINING_ANNOTATION)?.allValueArguments?.values?.single()?.safeAs<EnumValue>()?.value?.name?.identifier
        return when (joiningStrategyName) {
            "ANY" -> Joiners.ANY
            "NONE" -> Joiners.NONE
            "ALL" -> Joiners.ALL
            else -> Joiners.DEFAULT
        }
    }

    private fun getConditionsOnArgument(parameterDescriptor: ValueParameterDescriptor): List<ESBooleanExpression> {
        val parameterVariable = parameterDescriptor.toESVariable()

        val isNegated = parameterDescriptor.annotations.findAnnotation(NOT_CONDITION) != null

        return parameterDescriptor.annotations.mapNotNull {
            when (it.annotationClass?.fqNameSafe) {
                EQUALS_CONDITION -> ESEqual(parameterVariable, it.allValueArguments.values.single().toESConstant(), EqualFunctor(isNegated))
                IS_INSTANCE_CONDITION -> ESIs(parameterVariable, IsFunctor(it.allValueArguments.values.single().value as KotlinType, isNegated))
                else -> null
            }
        }
    }

    private fun getConditionsOnReceiver(receiverParameter: ReceiverParameterDescriptor): List<ESBooleanExpression> {
        val variable = receiverParameter.extensionReceiverToESVariable()

        val isNegated = receiverParameter.type.annotations.getAllAnnotations().any { it.annotation.annotationClass?.fqNameSafe == NOT_CONDITION }

        return receiverParameter.type.annotations.getAllAnnotations().mapNotNull {
            when (it.annotation.annotationClass?.fqNameSafe) {
                EQUALS_CONDITION -> ESEqual(variable, it.annotation.allValueArguments.values.single().toESConstant(), EqualFunctor(isNegated))
                IS_INSTANCE_CONDITION -> ESIs(variable, IsFunctor(it.annotation.allValueArguments.values.single().value as KotlinType, isNegated))
                else -> null
            }
        }
    }

    private fun ValueParameterDescriptor.toESVariable(): ESVariable {
        val dfv = DataFlowValue(IdentifierInfo.Variable(this, DataFlowValue.Kind.STABLE_VALUE, null), type)
        return ESVariable(ValueIdsFactory.dfvBased(dfv), type)
    }

    private fun ReceiverParameterDescriptor.extensionReceiverToESVariable(): ESVariable {
        val dfv = DataFlowValue(IdentifierInfo.Receiver(value), type)
        return ESVariable(ValueIdsFactory.dfvBased(dfv), type)
    }

    private fun ConstantValue<*>.toInvocationCountEnum(): ESCalls.InvocationCount = when (this.safeAs<EnumValue>()?.value?.name?.identifier) {
        "AT_MOST_ONCE" -> ESCalls.InvocationCount.AT_MOST_ONCE
        "EXACTLY_ONCE" -> ESCalls.InvocationCount.EXACTLY_ONCE
        "AT_LEAST_ONCE" -> ESCalls.InvocationCount.AT_LEAST_ONCE
        "UNKNOWN" -> ESCalls.InvocationCount.UNKNOWN
        else -> throw IllegalStateException("Unknown invocation type enum: $this")
    }

    private fun ConstantValue<*>.toESConstant(): ESConstant = when (this.safeAs<EnumValue>()?.value?.name?.identifier) {
        "TRUE" -> true.lift()
        "FALSE" -> false.lift()
        "NULL" -> null.lift()
        "NOT_NULL" -> ValuesFactory.NOT_NULL_CONSTANT
        else -> throw IllegalStateException("Unknown annotation-constant: $this")
    }

    private object Joiners {
        val ALL: JoinerT = { it.reduce { acc, expr -> acc.and(expr) } }
        val NONE: JoinerT = { conditions -> conditions.map { it.not() }.reduce { acc, expr -> acc.and(expr) } }
        val ANY: JoinerT = { it.reduce { acc, expr -> acc.or(expr) } }
        val DEFAULT = ALL
    }
}