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

package com.vsct.dt.maze.core

import com.vsct.dt.maze.core.Predef._
import org.scalatest.FlatSpec

import scala.util.{Failure, Success}

class OptionTest extends FlatSpec {

  "a valued option" should "execute without errors" in {

    val execution: Execution[Option[String]] = Execution(() => Some("it works"))

    execution.get().execute() match {
      case Success("it works") => // ok
      case Success(somethingElse) => fail(s"Unexpected value : $somethingElse")
      case Failure(e) => fail(e)
    }

    execution.getOrElse("something else").execute() match {
      case Success("it works") => // ok
      case Success(somethingElse) => fail(s"Unexpected value : $somethingElse")
      case Failure(e) => fail(e)
    }

    execution.exists().get() match {
      case PredicateResult(Success(true), _) => // ok
      case PredicateResult(Success(false), message) => fail(s"Unexpected value : false, message : $message")
      case PredicateResult(Failure(e), _) => fail(e)
    }
  }

  "an empty option" should "execute without errors" in {

    val execution: Execution[Option[String]] = Execution(() => None)

    execution.get().execute() match {
      case Success(_) => fail("empty option not seen as empty")
      case Failure(e) =>
    }

    execution.getOrElse("something else").execute() match {
      case Success("something else") => // ok
      case Success(somethingElse) => fail(s"Unexpected value : $somethingElse")
      case Failure(e) => fail(e)
    }

    execution.exists().get() match {
      case PredicateResult(Success(false), _) => // ok
      case PredicateResult(Success(true), message) => fail(s"Unexpected value : true, message : $message")
      case PredicateResult(Failure(e), _) => fail(e)
    }
  }
}
