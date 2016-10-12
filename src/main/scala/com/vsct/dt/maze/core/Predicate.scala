package com.vsct.dt.maze.core

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
  def apply(a: Boolean, message: String = ""): PredicateResult = PredicateResult(Try(a), message)

  def apply(e: Throwable, message: String): PredicateResult = PredicateResult(Failure(e), message)

  val success: PredicateResult = PredicateResult(Try(true), "")

  def failure(message: String): PredicateResult = PredicateResult(Try(false), message)

  def exception(e: Throwable): PredicateResult = PredicateResult(Failure(e), s"Predicate failed: ${e.getMessage}")
}
