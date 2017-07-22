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

package org.jetbrains.kotlin.effectsystem.factories

import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.impls.ESVariable
import org.jetbrains.kotlin.effectsystem.impls.EffectSchemaImpl
import org.jetbrains.kotlin.effectsystem.structure.*

object EffectSchemasFactory {
    fun pureReturns(value: ESValue): EffectSchema = clauses(listOf(ClausesFactory.always(ESReturns(value))), listOf())
    fun singleClause(premise: ESBooleanExpression, effects: List<ESEffect>, variables: List<ESVariable>) = clauses(listOf(ClausesFactory.create(premise, effects)), variables)
    fun clauses(clauses: List<ESClause>, params: List<ESVariable>): EffectSchema = EffectSchemaImpl(clauses, params)
}