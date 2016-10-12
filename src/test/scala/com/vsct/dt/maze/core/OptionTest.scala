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
