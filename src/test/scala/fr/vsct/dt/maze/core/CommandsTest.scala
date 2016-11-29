/*
 * Copyright (C) 2016 VSCT
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

package fr.vsct.dt.maze.core

import org.scalatest.{FlatSpec, Matchers}
import fr.vsct.dt.maze.core.Commands._

class CommandsTest extends FlatSpec with Matchers {

  val truePredicate: Predicate = Predicate.`true`
  val falsePredicate: Predicate = Predicate.`false`
  val errorPredicate: Predicate = new Predicate {
    override def get(): PredicateResult = Result(new Exception("simulated exception"), "")
    override val label: String = "predicate throwing exception"
  }

  "a if-else with a predicate always true" should "execute the first block" in {
    var block: Int = -1

    doIf(truePredicate) {
      block = 0
    } orElse {
      block = 1
    } onError{ e =>
      block = 2
    }

    block shouldBe 0

  }

  "a if-else with a predicate always false" should "execute the else block" in {
    var block: Int = -1

    doIf(falsePredicate) {
      block = 0
    } orElse {
      block = 1
    } onError{ e =>
      block = 2
    }

    block shouldBe 1

  }
  "a if-else with a predicate throwing exceptions" should "execute the exception block" in {
    var block: Int = -1

    doIf(errorPredicate) {
      block = 0
    } orElse {
      block = 1
    } onError{ e =>
      block = 2
    }

    block shouldBe 2

  }

}
