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

import com.vsct.dt.maze.topology.{ClusterNode, ClusterNodeGroupBuilder, NodeGroup}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Commands {

  class UnexpectedResultException(message: String) extends RuntimeException(message)

  class TimeoutException(message: String) extends RuntimeException(message)

  def expectThat(predicate: Predicate): Unit = {
    predicate.get() match {
      case PredicateResult(Success(true), _) =>
      case PredicateResult(Success(_), s) => throw new UnexpectedResultException(s"Wrong expectation: ${predicate.label}: $s")
      case _ => throw new UnexpectedResultException(s"Wrong expectation: ${predicate.label}")
    }
  }

  def print(dsl: Execution[_]): Unit = {
    val str: String = dsl.execute() match {
      case Success(s) => stringRepresentation(s)
      case Failure(e) => s"An error occurred: ${e.getClass}: ${e.getMessage}"
    }
    println(s"${dsl.label} -> $str")
  }

  private def stringRepresentation(a: Any): String = a match {
    case s: String => s
    case map: Map[_, _] => map.toList.map(e => s"${e._1} -> ${e._2}").mkString("\n")
    case seq: Seq[_] => seq.mkString("[", ", ", "]")
    case array: Array[_] => array.mkString("[", ", ", "]")
    case other: Any => other.toString
  }

  def exec[A](execution: Execution[A]): A = execution.execute().get

  def exec[A](executions: Seq[Execution[A]]): Unit = executions.foreach(execution => exec(execution))

  def waitFor(duration: FiniteDuration): Unit = Thread.sleep(duration.toMillis)

  /* WaitUntil methods */
  def waitUntil(predicate: Predicate): WaitUntilBuilder = {
    new WaitUntilBuilder(predicate)
  }

  class WaitUntilBuilder(val predicate: Predicate) {
    def butNoLongerThan(duration: FiniteDuration): Duration = waitUntil(predicate, duration)
  }

  def waitUntil(predicate: Predicate, butNoLongerThan: FiniteDuration = 5 minutes): Duration =
    waitInternal(predicate, _.result != Success(true), butNoLongerThan)


  /* waitWhile methods */
  def waitWhile(predicate: Predicate): WaitWhileBuilder = {
    new WaitWhileBuilder(predicate)
  }

  class WaitWhileBuilder(val predicate: Predicate) {
    def butNoLongerThan(duration: FiniteDuration): Duration = waitWhile(predicate, duration)
  }

  def waitWhile(predicate: Predicate, butNoLongerThan: FiniteDuration = 5 minutes): Duration =
    waitInternal(predicate, _.result == Success(true), butNoLongerThan)


  /* repeatWhile methods */
  def repeat(doSomething: => Unit): RepeatWhileBuilder = {
    new RepeatWhileBuilder(doSomething)
  }

  class RepeatWhileBuilder(doSomething: => Unit) {
    def `while`(predicate: Predicate): RepeatWhileBuilder2 = new RepeatWhileBuilder2(doSomething, predicate)
  }

  class RepeatWhileBuilder2(doSomething: => Unit, predicate: Predicate) {
    def butNoLongerThan(duration: FiniteDuration): Unit = repeatWhile(predicate, duration)(doSomething)
  }

  def repeatWhile(predicate: Predicate, butNoLongerThan: FiniteDuration = 5 minutes)(doSomething: => Unit): Unit =
    repeatInternal(predicate, _.result == Success(true), butNoLongerThan)(doSomething)


  /* internals */
  private val waitInternal: (Predicate, PredicateResult => Boolean, FiniteDuration) => Duration = repeatInternal(_, _, _) {
    waitFor(20 milliseconds)
  }

  private def repeatInternal(predicate: Predicate,
                             equalityFunction: PredicateResult => Boolean,
                             butNoLongerThan: FiniteDuration = 5 minutes)
                            (doSomething: => Unit): Duration = {
    val deadline = Deadline.now + butNoLongerThan
    var result = predicate.get()
    while (equalityFunction(result)) {
      if (deadline.isOverdue()) {
        throw new TimeoutException(s"Condition '${predicate.label}' didn't occur within $butNoLongerThan: ${result.message}.")
      }
      doSomething
      result = predicate.get()
    }
    butNoLongerThan - deadline.timeLeft
  }

  /* Nodes group methods */
  def tag(group: NodeGroup): ClusterNodeGroupBuilder[ClusterNode] = {
    new ClusterNodeGroupBuilder(group.nodes)
  }

  /* doIf methods */
  def doIf(condition: Predicate)(code: => Unit): IfResult = {
    condition.get().result match {
      case Success(true) =>
        code
        IfResultConsumed
      case Success(false) => IfResultElse
      case Failure(e) if e.isInstanceOf[Exception] => new IfResultError(e.asInstanceOf[Exception])
      case Failure(e) => new IfResultError(new Exception(e))
    }
  }

  sealed trait ElseResult {
    def onError(code: Exception => Unit): Unit = {}
  }

  sealed trait IfResult extends ElseResult {
    def orElse(code: => Unit): ElseResult = this
  }

  case object IfResultConsumed extends IfResult

  case object IfResultElse extends IfResult {
    override def orElse(code: => Unit): ElseResult = {
      code
      this
    }
  }

  class IfResultError(e: Exception) extends IfResult {
    override def onError(code: (Exception) => Unit): Unit = code(e)
  }


}
