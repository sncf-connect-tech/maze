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

    predicate.get().result should be (Success(true))
    predicate.get().result should be (Success(false))
    predicate.get().result should be (Failure(exception))

  }

  "the is function" should "test equality as needed even with predicate result" in {

    val execution = Execution {
      "ok"
    }

    val predicate = execution is execution

    (0 to 20).foreach{_ => Commands.expectThat(predicate)}

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

    predicate.get().result should be (Success(true))
    predicate.get().result should be (Success(false))
    predicate.get().result should be (Failure(exception))

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

    predicate.get().result should be (Success(false))
    predicate.get().result should be (Success(true))

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

    predicate.get().result should be (Success(true))
    predicate.get().result should be (Success(false))

  }

  "the snapshots" should "always return the same value" in {
    val counter = new AtomicInteger()
    val execution = Execution {
      counter.incrementAndGet()
    }

    execution.withSnapshot { snap =>
      (0 to 100).foreach {_ =>
        Commands.expectThat(snap is 1)
      }
    }
  }

}
