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
import fr.vsct.dt.maze.core.Predef._

import scala.util.{Failure, Success}

class PredefTest extends FlatSpec with Matchers {

  "the is function" should "test equality as needed" in {

    val counter = new AtomicInteger()
    val exception = new IllegalArgumentException("fake")

    val predicate = Execution {
      counter.incrementAndGet() match {
        case 1 => "ok"
        case 2 => "ko"
        case _ => throw exception
      }
    } is "ok"

    predicate.get().result should be(Success(true))
    val predicateResult = predicate.get()
    predicateResult.result should be(Success(false))
    predicateResult.message should be("Expected 'ko' to be 'ok'")
    predicate.get().result should be(Failure(exception))

  }

  "the is function" should "test equality as needed even with predicate result" in {

    val execution = Execution {
      "ok"
    }

    val predicate = execution is execution

    (0 to 20).foreach { _ => Commands.expectThat(predicate) }

    val koExecution = Execution {
      "ko"
    }

    val predicateResult = (execution is koExecution).get()
    predicateResult.message should be("Expected 'ko' to be 'ok'")
    predicateResult.result should be(Success(false))
  }

  "the isNot function" should "test equality as needed" in {

    val counter = new AtomicInteger()
    val exception = new IllegalArgumentException("fake")

    val predicate = Execution {
      counter.incrementAndGet() match {
        case 1 => "ok"
        case 2 => "ko"
        case _ => throw exception
      }
    } isNot "ko"

    predicate.get().result should be(Success(true))
    predicate.get().result should be(Success(false))
    predicate.get().result should be(Failure(exception))

  }

  "the isError function" should "test errors" in {

    val counter = new AtomicInteger()
    val exception = new IllegalArgumentException("fake")

    val predicate = Execution {
      counter.incrementAndGet() match {
        case 1 => "ok"
        case _ => throw exception
      }
    }.isError

    predicate.get().result should be(Success(false))
    predicate.get().result should be(Success(true))

  }

  "the isSuccess function" should "test errors" in {

    val counter = new AtomicInteger()
    val exception = new IllegalArgumentException("fake")

    val predicate = Execution {
      counter.incrementAndGet() match {
        case 1 => "ok"
        case _ => throw exception
      }
    }.isSuccess

    predicate.get().result should be(Success(true))
    predicate.get().result should be(Success(false))

  }

  "the snapshots" should "always return the same value" in {
    val counter = new AtomicInteger()
    val execution = Execution {
      counter.incrementAndGet()
    }

    execution.withSnapshot { snap =>
      (0 to 100).foreach { _ =>
        Commands.expectThat(snap is 1)
      }
    }
  }

  "recovery functions" should "provide default values" in {
    val execution: Execution[String] = Execution {
      throw new IllegalStateException("simulated")
    }
    Commands.exec(execution.recoverWith("ok")) should be("ok")
  }

  "recovery functions" should "return normal value if no exception occurs" in {
    val execution: Execution[String] = Execution {
      "original"
    }
    Commands.exec(execution.recoverWith("ok")) should be("original")
  }

  "array executions" should "implement exists correctly" in {

    val empty: Execution[Array[String]] = emptyArray()
    val abcd = Execution {
      Array("a", "b", "c", "d", "")
    }
    val defg = Execution {
      Array("d", "e", "f", "g")
    }

    empty.exists(_.length == 0, "some empty string").execute() should be(false)
    abcd.exists(_.length == 0, "some empty string").execute() should be(true)
    defg.exists(_.length == 0, "some empty string").execute() should be(false)
  }

  "array executions" should "implement cotains correctly" in {

    val empty: Execution[Array[String]] = emptyArray()
    val abcd = Execution {
      Array("a", "b", "c", "d", "")
    }
    val defg = Execution {
      Array("d", "e", "f", "g")
    }

    empty.contains("c", "c").execute() should be(false)
    abcd.contains("c", "c").execute() should be(true)
    defg.contains("c", "c").execute() should be(false)
  }

  "array executions" should "implement length correctly" in {

    val empty: Execution[Array[String]] = emptyArray()
    val abcd = Execution {
      Array("a", "b", "c", "d", "")
    }
    val defg = Execution {
      Array("d", "e", "f", "g")
    }

    empty.length.execute().get should be(0)
    abcd.length.execute().get should be(5)
    defg.length.execute().get should be(4)
  }

  "array executions" should "implement isEmpty correctly" in {

    val empty: Execution[Array[String]] = emptyArray()
    val abcd = Execution {
      Array("a", "b", "c", "d", "")
    }

    empty.isEmpty.execute() should be(true)
    abcd.isEmpty.execute() should be(false)
  }

  "array executions" should "implement first correctly" in {

    val empty: Execution[Array[String]] = emptyArray()
    val abcd = Execution {
      Array("a", "b", "c", "d", "")
    }

    empty.first.execute().isFailure should be(true)
    abcd.first.execute().get should be("a")
  }

  "array executions" should "implement forall correctly" in {

    val empty: Execution[Array[String]] = emptyArray()
    val abcd = Execution {
      Array("a", "b", "c", "d", "")
    }
    val defg = Execution {
      Array("d", "e", "f", "g")
    }

    empty.forall(_.length == 1).execute() should be(true)
    abcd.forall(_.length == 1).execute() should be(false)
    defg.forall(_.length == 1).execute() should be(true)
  }

  "map executions" should "be implemented properly" in {

    val mapExecution = Execution[Map[String, String]] {
      Map("key" -> "value")
    }
    val emptyMapExecution = Execution[Map[String, String]] {
      Map[String, String]()
    }

    mapExecution.hasKey("key").execute() should be(true)
    mapExecution.hasKey("key2").execute() should be(false)
    emptyMapExecution.hasKey("key").execute() should be(false)

    mapExecution.get("key").execute().get should be("value")
    mapExecution.get("key2").execute().isFailure should be(true)

    mapExecution.getOrElse("key", "default").execute().get should be("value")
    mapExecution.getOrElse("key2", "default").execute().get should be("default")

    mapExecution.isEmpty.execute() should be(false)
    emptyMapExecution.isEmpty.execute() should be(true)

    mapExecution.isNotEmpty.execute() should be(true)
    emptyMapExecution.isNotEmpty.execute() should be(false)

  }

  "string executions" should "work as expected" in {

    Execution {
      "string"
    }.length.execute().get should be("string".length)
    Execution {
      "string"
    }.contains("tri").execute() should be(true)
    Execution {
      "string"
    }.contains("test").execute() should be(false)

  }

  "Int executions" should "compare each other normally" in {

    val first: Execution[Int] = Execution(5)
    val other: Execution[Int] = Execution(2)

    (first > 2).execute() should be(true)
    (first > 10).execute() should be(false)
    (first > 5).execute() should be(false)
    (first > other).execute() should be(true)
    (other > first).execute() should be(false)
    (first > first).execute() should be(false)

    (first >= 2).execute() should be(true)
    (first >= 10).execute() should be(false)
    (first >= 5).execute() should be(true)
    (first >= other).execute() should be(true)
    (other >= first).execute() should be(false)
    (first >= first).execute() should be(true)


    (first < 2).execute() should be(false)
    (first < 10).execute() should be(true)
    (first < 5).execute() should be(false)
    (first < other).execute() should be(false)
    (other < first).execute() should be(true)
    (first < first).execute() should be(false)

    (first <= 2).execute() should be(false)
    (first <= 10).execute() should be(true)
    (first <= 5).execute() should be(true)
    (first <= other).execute() should be(false)
    (other <= first).execute() should be(true)
    (first <= first).execute() should be(true)
  }

  "Int executions" should "use operators normally" in {
    val first: Execution[Int] = Execution(5)
    val other: Execution[Int] = Execution(2)

    (first + other).execute().get should be(7)
    (other + first).execute().get should be(7)
    (first + 2).execute().get should be(7)
  }

  "array executions" should "implement append correctly" in {
    val abcd = Execution {
      Array("a", "b", "c", "d")
    }
    val defg = Execution {
      Array("e", "f", "g")
    }
    val result = abcd.append(defg).execute().get

    result should not be empty
    result should equal(Array("a", "b", "c", "d", "e", "f", "g"))
  }

  "array executions" should "implement removeDuplicates correctly" in {
    val duplicateList = Execution {
      Array("a", "b", "a", "c", "d", "c")
    }

    val result = duplicateList.removeDuplicates().execute().get
    result should not be empty
    result should have size 4
  }

  "array executions" should "implement find correctly" in {
    val listContains = Execution {
      Array(1, 2, 3, 4, 5)
    }
    val listNotContains = Execution {
      Array(1, 3)
    }
    listContains.find(_ % 2 == 0).exists().execute() should be(true)
    listNotContains.find(_ % 2 == 0).exists().execute() should be(false)
  }
}
