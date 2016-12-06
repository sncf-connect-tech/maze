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
import scala.util.{Failure, Success, Try}

class CommandsTest extends FlatSpec with Matchers {

  val truePredicate: Predicate = Predicate.`true`
  val falsePredicate: Predicate = Predicate.`false`
  val errorPredicate: Predicate = new Predicate {
    override def get(): PredicateResult = Result(new Exception("simulated exception"), "")

    override val label: String = "predicate throwing exception"
  }

  val throwablePredicate: Predicate = new Predicate {
    override def get(): PredicateResult = Result(new Throwable("simulated exception"), "")

    override val label: String = "predicate throwing exception"
  }

  "a if-else with a predicate always true" should "execute the first block" in {
    var block: Int = -1

    doIf(truePredicate) {
      block = 0
    } orElse {
      block = 1
    } onError { e =>
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
    } onError { e =>
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
    } onError { e =>
      block = 2
    }

    block shouldBe 2

  }

  "a if-else with a predicate throwing a throwable" should "execute the exception block" in {
    var block: Int = -1

    doIf(throwablePredicate) {
      block = 0
    } orElse {
      block = 1
    } onError { e =>
      block = 2
    }

    block shouldBe 2

  }


  "a waitUntil block" should "continue while exceptions are thrown" in {

    val counter = new AtomicInteger()
    val execution: Execution[String] = Execution {
      if (counter.incrementAndGet() < 10) {
        throw new IllegalArgumentException("fake")
      }
      "ok"
    }

    waitUntil(execution is "ok") butNoLongerThan (10 seconds)

    counter.get() should be(10)

  }

  "a waitUntil block" should "continue while predicate returns false" in {

    val counter = new AtomicInteger()
    val execution: Execution[String] = Execution {
      if (counter.incrementAndGet() < 10) {
        "ko"
      } else {
        "ok"
      }
    }

    waitUntil(execution is "ok") butNoLongerThan (10 seconds)

    counter.get() should be(10)

  }

  "a waitUntil block" should "throw an exception if deadline occurrs" in {

    val execution: Execution[String] = Execution {
      waitFor(1 seconds)
      "ko"
    }

    val result = Try {
      waitUntil(execution is "ok") butNoLongerThan (100 milliseconds)
    }

    result.isFailure should be(true)
  }

  "a waitWhile block" should "leave right away if an exception is thrown" in {

    val counter = new AtomicInteger()
    val execution: Execution[String] = Execution {
      if (counter.incrementAndGet() < 10) {
        "ok"
      } else {
        throw new IllegalArgumentException("fake")
      }

    }

    waitWhile(execution is "ok") butNoLongerThan (10 seconds)

    counter.get() should be(10)

  }

  "a waitWhile block" should "continue while predicate returns true" in {

    val counter = new AtomicInteger()
    val execution: Execution[String] = Execution {
      if (counter.incrementAndGet() < 10) {
        "ok"
      } else {
        "ko"
      }
    }

    waitWhile(execution is "ok") butNoLongerThan (10 seconds)

    counter.get() should be(10)

  }

  "a waitWhile block" should "throw an exception if deadline occurrs" in {

    val execution: Execution[String] = Execution {
      waitFor(1 seconds)
      "ok"
    }

    val result = Try {
      waitWhile(execution is "ok") butNoLongerThan (100 milliseconds)
    }

    result.isFailure should be(true)
  }

  "a repeatWhile block" should "leave right away if an exception is thrown" in {

    val counter = new AtomicInteger()
    val execution: Execution[String] = Execution {
      if (counter.get() < 10) {
        "ok"
      } else {
        throw new IllegalArgumentException("fake")
      }

    }

    repeat {
      counter.incrementAndGet()
    } `while` (execution is "ok") butNoLongerThan (10 seconds)

    counter.get() should be(10)

  }

  "a repeatWhile block" should "continue while predicate returns true" in {

    val counter = new AtomicInteger()
    val execution: Execution[String] = Execution {
      if (counter.get() < 10) {
        "ok"
      } else {
        "ko"
      }
    }

    repeat {
      counter.incrementAndGet()
    } `while` (execution is "ok") butNoLongerThan (10 seconds)

    counter.get() should be(10)

  }

  "a repeatWhile block" should "throw an exception if deadline occurrs" in {

    val execution: Execution[String] = Execution {
      "ok"
    }.labeled("just wait, for fun")

    val result = Try {
      repeat {
        print(execution)
      } `while` (execution is "ok") butNoLongerThan (100 milliseconds)
    }

    result.isFailure should be(true)
  }

  "expectations" should "conserve original cause" in {

    val exception = new IllegalArgumentException("fake")

    val predicate = new Predicate {
      override def get() = PredicateResult(Failure(exception), null)

      override val label: String = "exception launcher"
    }

    val result = Try {
      expectThat(predicate)
    }

    result match {
      case Success(_) => fail("An exception should have been launched")
      case Failure(e) => e.getCause should be(exception)
    }

  }

  "expectations" should "be transparent on Success(true)" in {

    val predicate = new Predicate {
      override def get() = PredicateResult(Success(true), null)

      override val label: String = "ok predicate"
    }

    expectThat(predicate)
    // No exception sjoulkd be thrown
  }

  "expectations" should "throw exceptions on Success(false)" in {

    val predicate = new Predicate {
      override def get() = PredicateResult(Success(false), "cause")

      override val label: String = "ko predicate"
    }

    Try(expectThat(predicate)) match {
      case Failure(e) =>
      case _ => fail("Expected an exception to be thrown")
    }

  }

}
