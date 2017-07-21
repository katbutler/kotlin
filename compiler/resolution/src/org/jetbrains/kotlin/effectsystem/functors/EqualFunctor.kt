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

package org.jetbrains.kotlin.effectsystem.functors

import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.factories.ClausesFactory
import org.jetbrains.kotlin.effectsystem.impls.ESEqual
import org.jetbrains.kotlin.effectsystem.impls.and
import org.jetbrains.kotlin.effectsystem.factories.lift
import org.jetbrains.kotlin.effectsystem.structure.getOutcome
import org.jetbrains.kotlin.effectsystem.structure.withoutReturns

class EqualFunctor(val isNegated: Boolean) : AbstractSequentialBinaryFunctor() {
    override fun combineClauses(left: List<org.jetbrains.kotlin.effectsystem.structure.ESClause>, right: List<org.jetbrains.kotlin.effectsystem.structure.ESClause>): List<org.jetbrains.kotlin.effectsystem.structure.ESClause> {
        return left.flatMap { leftClause ->
             right.flatMap inner@ { rightClause ->
                val combinedPremise = leftClause.premise.and(rightClause.premise)

                val leftValue = (leftClause.conclusion.getOutcome() as ESReturns).value
                val rightValue = (rightClause.conclusion.getOutcome() as ESReturns).value

                val combinedRest = leftClause.conclusion.withoutReturns() + rightClause.conclusion.withoutReturns()

                val trueResult = ClausesFactory.create(
                        premise = combinedPremise.and(ESEqual(leftValue, rightValue, this)),
                        conclusion = combinedRest + ESReturns(true.lift())
                )
                val falseResult = ClausesFactory.create(
                        premise = combinedPremise.and(ESEqual(leftValue, rightValue, negated())),
                        conclusion = combinedRest + ESReturns(false.lift())
                )

                return@inner listOf(trueResult, falseResult)
            }
        }
    }

    fun negated(): EqualFunctor = EqualFunctor(isNegated.not())
}