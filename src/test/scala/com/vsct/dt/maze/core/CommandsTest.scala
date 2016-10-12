package com.vsct.dt.maze.core

import org.scalatest.{FlatSpec, Matchers}
import com.vsct.dt.maze.core.Commands._

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
