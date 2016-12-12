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

import scala.util.{Failure, Success}

class PredicateTest extends FlatSpec with Matchers {

  val exception = new IllegalStateException("fake")
  val truePredicate = Predicate.`true`
  val falsePredicate = Predicate.`false`
  val errorPredicate = new Predicate {
    override val label: String = "exception thrower"

    override def get(): PredicateResult = Result.exception(exception)
  }

  "unary not" should "negate properly" in {

    (!truePredicate).get().result should be(Success(false))
    (!falsePredicate).get().result should be(Success(true))
    (!errorPredicate).get().result should be(Failure(exception))

  }

  "and" should "do boolean and properly" in {

    (truePredicate && truePredicate).get().result should be(Success(true))
    (truePredicate && falsePredicate).get().result should be(Success(false))
    (falsePredicate && truePredicate).get().result should be(Success(false))
    (falsePredicate && falsePredicate).get().result should be(Success(false))
    (falsePredicate && errorPredicate).get().result should be(Success(false))
    (truePredicate && errorPredicate).get().result should be(Failure(exception))
    (errorPredicate && truePredicate).get().result should be(Failure(exception))
    (errorPredicate && falsePredicate).get().result should be(Failure(exception))
    (errorPredicate && errorPredicate).get().result should be(Failure(exception))

  }


  "or" should "do boolean or properly" in {

    (truePredicate || truePredicate).get().result should be(Success(true))
    (truePredicate || falsePredicate).get().result should be(Success(true))
    (falsePredicate || truePredicate).get().result should be(Success(true))
    (falsePredicate || falsePredicate).get().result should be(Success(false))
    (falsePredicate || errorPredicate).get().result should be(Failure(exception))
    (truePredicate || errorPredicate).get().result should be(Success(true))
    (errorPredicate || truePredicate).get().result should be(Failure(exception))
    (errorPredicate || falsePredicate).get().result should be(Failure(exception))
    (errorPredicate || errorPredicate).get().result should be(Failure(exception))

  }



}
