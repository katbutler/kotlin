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

package org.jetbrains.kotlin.effectsystem.impls

import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.effects.ESThrows
import org.jetbrains.kotlin.effectsystem.factories.EffectSchemasFactory
import org.jetbrains.kotlin.effectsystem.structure.*
import org.jetbrains.kotlin.effectsystem.visitors.Substitutor

class EffectSchemaImpl(override val clauses: List<ESClause>, val parameters: List<ESVariable>) : EffectSchema {
    override fun apply(arguments: List<EffectSchema>): EffectSchema? {
        // Filter argument's clauses that will transparently lift through the application
        val irrelevantClauses = arguments.flatMap { schema ->
            schema.clauses.filter { clause ->
                clause.conclusion.all { it !is ESReturns && it !is ESThrows }
            }
        }

        // Build a pack of arguments schemas with only relevant clauses
        val filteredArgs = arguments.map { schema ->
            EffectSchemasFactory.clauses(schema.clauses.filter { it.conclusion.any { it is ESReturns || it is ESThrows } })
        }
        val substs = parameters.zip(filteredArgs).toMap()

        val combinedClauses = mutableListOf<ESClause>()
        for (clause in clauses) {
            // Recurse and join results
            val substitutedPremise = clause.premise.accept(Substitutor(substs)) ?: continue

            substitutedPremise.clauses.forEach {
                when {
                    it.conclusion.doesReturn(true) ->
                        // Left returns true, add clause without left Returns and with right-effects
                        combinedClauses += ESClause(it.premise, (it.conclusion.withoutReturns() + clause.conclusion))

                    it.conclusion.doesReturn(false) ->
                        // Left returns false, add clause without left Returns and without right-effects
                        combinedClauses += ESClause(it.premise, it.conclusion.withoutReturns())

                    it.conclusion.throws() -> combinedClauses += it
                }
            }
        }

        return EffectSchemasFactory.clauses(irrelevantClauses + combinedClauses)
    }
}