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

import scala.util.{Failure, Success, Try}

abstract class Predicate {
  self =>

  val label: String

  def get(): PredicateResult

  def execute(): Boolean = self.get().result.getOrElse(false)

  def labeled(text: String): Predicate = {
    new Predicate {
      override val label: String = text

      override def get(): PredicateResult = self.get()
    }
  }

  def unary_! : Predicate = new Predicate {
    override val label: String = "!" + self.label

    override def get(): PredicateResult = self.get() match {
      case PredicateResult(Success(true), _) => Result.failure(s"Expected ${self.label} to be false")
      case PredicateResult(Failure(e), m) => Result(e, m)
      case _ => Result.success
    }
  }

  def &&(other: Predicate): Predicate = {
    new Predicate {
      override val label: String = self.label + " AND " + other.label

      override def get(): PredicateResult = self.get() match {
        case PredicateResult(Success(true), _) => other.get()
        case PredicateResult(Failure(e), m) => Result(e, m)
        case _ => Result.failure(s"${this.label} returned false")
      }
    }
  }

  def ||(other: Predicate): Predicate = new Predicate {
    override def get(): PredicateResult = self.get() match {
      case PredicateResult(Success(true), _) => Result.success
      case PredicateResult(Failure(e), m) => Result(e, m)
      case _ => other.get()
    }

    override val label: String = self.label + " OR " + other.label
  }

}

object Predicate {
  val `true` = new Predicate {
    override def get(): PredicateResult = Result.success

    override val label: String = "True predicate"
  }

  val `false` = new Predicate {
    override def get(): PredicateResult = Result.failure("")

    override val label: String = "False predicate"
  }
}

case class PredicateResult(result: Try[Boolean], message: String)

object Result {
  def apply(value: Boolean, message: String = ""): PredicateResult = PredicateResult(Success(value), message)

  def apply(e: Throwable, message: String): PredicateResult = PredicateResult(Failure(e), message)

  val success: PredicateResult = Result(value = true, "")

  def failure(message: String): PredicateResult = Result(value = false, message)

  def exception(e: Throwable): PredicateResult = PredicateResult(Failure(e), s"Predicate failed: ${e.getMessage}")
}
