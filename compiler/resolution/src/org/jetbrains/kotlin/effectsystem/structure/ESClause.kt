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

package org.jetbrains.kotlin.effectsystem.structure

import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.effects.ESThrows
import org.jetbrains.kotlin.effectsystem.factories.lift

/**
 * Description of some set of effects and corresponding to them conditions
 *
 * [premise] is some expression, which result-type is Boolean. Clause should be
 * interpreted as: "if [premise] is true then effects of [conclusion] are fired"
 *
 * NB. [premise] and [conclusion] connected with implication in math logic sense,
 * which in particular means that:
 *  - if [premise] is false, we *can't* reason that we won't observe effects from [conclusion]
 *  - if [conclusion] is observed, we *can't* reason that [premise] was true
 */
class ESClause(val premise: ESBooleanExpression, val conclusion: List<ESEffect>)

// Clause extensions
fun ESClause.withoutReturns(): ESClause = ESClause(premise, conclusion.withoutReturns())

fun ESClause.withEffect(effect: ESEffect): ESClause = ESClause(premise, conclusion + effect)


// Conclusion extensions
fun List<ESEffect>.getOutcome(): ESEffect? = find { it is ESReturns || it is ESThrows }

fun List<ESEffect>.throws(): Boolean = any { it is ESThrows }

fun List<ESEffect>.returns(): Boolean = any { it is ESReturns }

fun List<ESEffect>.withoutReturns(): List<ESEffect> = filterNot { it is ESReturns }

fun List<ESEffect>.doesReturn(booleanValue: Boolean) = contains(ESReturns(booleanValue.lift()))

fun List<ESEffect>.extractOutcome(): Pair<ESEffect?, List<ESEffect>> =
        find { it is ESReturns || it is ESThrows } to filterNot { it is ESReturns || it is ESThrows }