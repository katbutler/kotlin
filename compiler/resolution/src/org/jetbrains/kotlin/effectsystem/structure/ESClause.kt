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

/**
 * Description of some set of effects and corresponding to them conditions
 *
 * [condition] is some expression, which result-type is Boolean. Clause should be
 * interpreted as: "if [effect] took place then [condition]-expression is true"
 *
 * NB. [condition] and [effect] connected with implication in math logic sense,
 * which in particular means that:
 *  - if [condition] is false, we *can't* reason that we won't observe effects from [effect]
 *  - if [effect] is observed, we *can't* reason that [condition] was true
 */
class ESClause(val condition: ESBooleanExpression, val effect: ESEffect) {
    fun replaceEffect(newEffect: ESEffect) = ESClause(condition, effect)
}
