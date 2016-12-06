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

import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.{FlatSpec, Matchers}
import fr.vsct.dt.maze.core.Commands._
import fr.vsct.dt.maze.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps

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


  "a waitUntil block" should "continue while exceptions are thrown" in {

    val counter = new AtomicInteger()
    val execution: Execution[String] = Execution {
      if(counter.incrementAndGet() < 10) {
        throw new IllegalArgumentException("fake")
      }
      "ok"
    }

    waitUntil(execution is "ok") butNoLongerThan(10 seconds)

    counter.get() should be(10)

  }

  "a waitUntil block" should "continue while predicate returns false" in {

    val counter = new AtomicInteger()
    val execution: Execution[String] = Execution {
      if(counter.incrementAndGet() < 10) {
        "ko"
      } else {
        "ok"
      }
    }

    waitUntil(execution is "ok") butNoLongerThan(10 seconds)

    counter.get() should be(10)

  }

  "a waitWhile block" should "leave right away if an exception is thrown" in {

    val counter = new AtomicInteger()
    val execution: Execution[String] = Execution {
      if(counter.incrementAndGet() < 10) {
        "ok"
      } else {
        throw new IllegalArgumentException("fake")
      }

    }

    waitWhile(execution is "ok") butNoLongerThan(10 seconds)

    counter.get() should be(10)

  }

  "a waitWhile block" should "continue while predicate returns true" in {

    val counter = new AtomicInteger()
    val execution: Execution[String] = Execution {
      if(counter.incrementAndGet() < 10) {
        "ok"
      } else {
        "ko"
      }
    }

    waitWhile(execution is "ok") butNoLongerThan(10 seconds)

    counter.get() should be(10)

  }

}
