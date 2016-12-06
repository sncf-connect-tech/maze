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

import java.io.StringReader
import javax.xml.transform.stream.StreamSource

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import fr.vsct.dt.maze.helpers.Http.HttpResponse
import fr.vsct.dt.maze.topology._
import net.sf.saxon.s9api.{Processor, XdmItem, XdmValue}

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object Predef {

  private val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)

  private val xpathProcessor = new Processor(false)

  implicit class HttpExecution(val self: Execution[HttpResponse]) extends AnyVal {
    def status: Execution[Int] = self.map(_.responseCode).labeled(s"status code of ${self.label}")

    def response: Execution[String] = self.map(_.entity).labeled(s"Response to ${self.label}")

    def responseAs[A](c: Class[A]): Execution[A] = self.map { r =>
      objectMapper.readValue(r.entity, c)
    }.labeled(s"Response to ${self.label} as instance of ${c.getName}")

    def isOk: Predicate = {
      val statusOk = 200
      val maxOk = 299
      self.status.between(statusOk, maxOk).labeled(self.label + " is ok?")
    }
  }

  implicit class RichExecution[A](val self: Execution[A]) extends AnyVal {
    private def toExecutionWrappingExecuted: Execution[A] = {
      val returnValue = self.execute()
      new Execution[A] {
        override def execute(): Try[A] = returnValue

        override val label: String = self.label
      }
    }

    def withSnapshot[B](fn: (Execution[A]) => B): B = fn(toExecutionWrappingExecuted)

    def untilSuccess: Execution[A] = new Execution[A] {
      override val label: String = self.label

      override def execute(): Try[A] = {
        val result: Try[A] = self.execute()
        result match {
          case Success(_) => result
          case Failure(_) => this.execute()
        }
      }
    }

    def is(other: Execution[A]): Predicate = self is other.execute().get

    def is(other: A): Predicate = self.toPredicate(s"${self.label} is '$other'?") {
      case a if a == other => Result.success
      case a => Result.failure(s"Expected '$a' to be '$other'")
    }

    def isNot(other: A): Predicate = self.toPredicate(s"${self.label} isn't '$other'?") {
      case a if a != other => Result.success
      case a => Result.failure(s"Expected '$a' to be different from '$other'")
    }

    def isError: Predicate = new Predicate {
      override def get(): PredicateResult = self.execute() match {
        case Failure(_) => Result.success
        case _ => Result.failure("Expected execution to be in error, but it is not.")
      }


      override val label: String = self.label + " is in error?"
    }

    def isSuccess: Predicate = new Predicate {

      override def get(): PredicateResult = self.execute() match {
        case Success(_) => Result.success
        case _ => Result.failure("Expected execution to be in success, but it is not.")
      }

      override val label: String = self.label + " is in success?"
    }


    def toPredicate(predicateLabel: String)(fn: PartialFunction[A, PredicateResult]): Predicate = new Predicate {
      override val label: String = predicateLabel

      override def get(): PredicateResult = {
        self.execute() match {
          case Success(r) => fn(r)
          case Failure(e) => Result.exception(e)
        }
      }
    }

  }

  implicit class OptionExecution[A](val self: Execution[Option[A]]) extends AnyVal {

    def exists(): Predicate = {
      self.toPredicate(s"Does ${self.label} exists ?") {
        case Some(_) => Result.success
        case _ => Result.failure(s"No value found for ${self.label}")
      }
    }

    def get(): Execution[A] = self.map(_.get)

    def getOrElse(defaultValue: A): Execution[A] = self.map(_.getOrElse(defaultValue))

  }

  implicit class ArrayExecution[A: ClassTag](val self: Execution[Array[A]]) {

    def contains(condition: (A) => Boolean, functionText: String): Predicate =
      self.map(_.contains(condition)).toPredicate(s"${self.label} contains $functionText ?") {
        case a if a => Result.success
        case _ => Result.failure(s"Expected array to contain '$functionText'")
      }

    def length: Execution[Int] = self.map(_.length).labeled(s"the length of ${self.label}")

    def isEmpty: Predicate = (self.length is 0).labeled(s"${self.label} is empty?")

    def first: Execution[A] = self.map(_.head).labeled(s"the first element of ${self.label}")

    def append(other: Execution[Array[A]]): Execution[Array[A]] = {
      for {
        first <- self
        second <- other
      } yield Array.concat[A](first, second)
    }

    def forall(fn: (A) => Boolean): Predicate = self.map(_.forall(fn)).toPredicate(s"all elements of ${self.label} verify condition?") {
      case a if a => Result.success
      case _ => Result.failure(s"Expected all elements of ${self.label} verify condition?")
    }

    def removeDuplicates(): Execution[Array[A]] = self.map(_.toSet.toArray)

    def find(condition: (A) => Boolean): Execution[Option[A]] = {
      self.map(_.find(condition)).labeled(s"find first element of ${self.label} verifying condition")
    }

  }

  implicit class MapExecution[KEY, A](val self: Execution[Map[KEY, A]]) extends AnyVal {
    def hasKey(key: KEY): Predicate = self.toPredicate(s"${self.label} contains $key?") {
      case m if m.contains(key) => Result.success
      case m => Result.failure(s"Expected map to contain '$key', but it is: ${m.map(entry => s"${entry._1} -> ${entry._2}").mkString("\n")}")
    }

    def get(key: KEY): Execution[A] = self.map(_ (key)).labeled(s"the key '$key' of ${self.label}")

    def getOrElse(key: KEY, default: A): Execution[A] = self.map(_.getOrElse(key, default)).labeled(s"the key '$key' of ${self.label}")

    def isEmpty: Predicate = self.toPredicate(s"${self.label} is empty?") {
      case m if m.isEmpty => Result.success
      case m => Result.failure(s"Expected map to be empty, but it is: ${m.map(entry => s"${entry._1} -> ${entry._2}").mkString("\n")}")
    }

    def isNotEmpty: Predicate = self.toPredicate(s"${self.label} is not empty?") {
      case m if m.nonEmpty => Result.success
      case _ => Result.failure(s"Expected map to be not empty, but it is empty")
    }
  }

  implicit class StringExecution(val self: Execution[String]) extends AnyVal {
    def contains(fragment: String): Predicate = self.toPredicate(s"${self.label} contains '$fragment'?") {
      case s if s.contains(fragment) => Result.success
      case s => Result.failure(s"Expected '$s' to contain '$fragment'")
    }

    def length: Execution[Int] = self.map(_.length).labeled(s"the length of ${self.label}")

    def xpath(expression: String, namespaces: Map[String, String] = Map()): Execution[XdmValue] = self.map { content =>
      val compiler = xpathProcessor.newXPathCompiler()
      namespaces.foreach { v =>
        compiler.declareNamespace(v._1, v._2)
      }
      val selector = compiler.compile(expression).load()
      val node = xpathProcessor.newDocumentBuilder().build(new StreamSource(new StringReader(content)))
      selector.setContextItem(node)
      selector.evaluate()
    }.labeled(s"the result of the xpath expression $expression on ${self.label}")
  }

  implicit class XdmValueExecution(val self: Execution[XdmValue]) extends AnyVal {

    def length(): Execution[Int] = self.map(_.size()).labeled(s"the number of elements matching ${self.label}")

    def first(): Execution[XdmItem] = self.map(_.itemAt(0)).labeled(s"the first element matching ${self.label}")

    def itemAt(index: Int): Execution[XdmItem] = self.map(_.itemAt(index)).labeled(s"the #$index element matching ${self.label}")

  }

  implicit class XdmItemExecution(val self: Execution[XdmItem]) extends AnyVal {

    def stringValue(): Execution[String] = self.map(_.getStringValue)

  }

  implicit class IntExecution(val self: Execution[Int]) extends AnyVal {
    def >=(other: Execution[Int]): Predicate = new Predicate {
      override def get(): PredicateResult = {
        val firstTry = self.execute()
        val otherTry = other.execute()

        val execution = for {
          first <- firstTry
          second <- otherTry
        } yield first >= second

        execution match {
          case Success(true) => Result.success
          case Success(false) => Result.failure(s"Expected ${firstTry.get} > ${otherTry.get}")
          case Failure(e) => Result.exception(e)
        }

      }

      override val label: String = s"${self.label} >= $other?"
    }


    def >(other: Int): Predicate = self.toPredicate(s"${self.label} > $other?") {
      case a if a > other => Result.success
      case a => Result.failure(s"Expected $a > $other")
    }

    def >=(other: Int): Predicate = self.toPredicate(s"${self.label} >= $other?") {
      case a if a >= other => Result.success
      case a => Result.failure(s"Expected $a >= $other")
    }

    def <(other: Int): Predicate = self.toPredicate(s"${self.label} < $other?") {
      case a if a < other => Result.success
      case a => Result.failure(s"Expected $a < $other")
    }

    def <=(other: Int): Predicate = self.toPredicate(s"${self.label} <= $other?") {
      case a if a > other => Result.success
      case a => Result.failure(s"Expected $a <= $other")
    }

    def +(other: Execution[Int]): Execution[Int] = {
      for {
        i <- self
        j <- other
      } yield i + j
    }.labeled(s"${self.label} + ${other.label}")

    def +(increment: Int): Execution[Int] = self.map(_ + increment).labeled(s"${self.label} + $increment")

    def between(a: Int, b: Int): Predicate = self.toPredicate(self.label + s" between $a and $b ?") {
      case i if i >= a && i <= b => Result.success
      case i => Result.failure(s"Expected result to be between $a and $b but is $i")
    }
  }

  implicit class LongExecution(val self: Execution[Long]) extends AnyVal {
    def >=(other: Execution[Long]): Predicate = new Predicate {
      override def get(): PredicateResult = {
        val firstTry = self.execute()
        val otherTry = other.execute()

        val execution = for {
          first <- firstTry
          second <- otherTry
        } yield first >= second

        execution match {
          case Success(true) => Result.success
          case Success(false) => Result.failure(s"Expected ${firstTry.get} >= ${otherTry.get}")
          case Failure(e) => Result.exception(e)
        }

      }

      override val label: String = s"${self.label} > $other?"
    }

    def <(other: Execution[Long]): Predicate = new Predicate {
      override def get(): PredicateResult = {
        val firstTry = self.execute()
        val otherTry = other.execute()

        val execution = for {
          first <- firstTry
          second <- otherTry
        } yield first < second

        execution match {
          case Success(true) => Result.success
          case Success(false) => Result.failure(s"Expected ${firstTry.get} < ${otherTry.get}")
          case Failure(e) => Result.exception(e)
        }

      }

      override val label: String = s"${self.label} > $other?"
    }


    def >(other: Long): Predicate = self.toPredicate(s"${self.label} > $other?") {
      case a if a > other => Result.success
      case a => Result.failure(s"Expected $a > $other")
    }

    def >=(other: Long): Predicate = self.toPredicate(s"${self.label} >= $other?") {
      case a if a >= other => Result.success
      case a => Result.failure(s"Expected $a >= $other")
    }

    def <(other: Long): Predicate = self.toPredicate(s"${self.label} < $other?") {
      case a if a < other => Result.success
      case a => Result.failure(s"Expected $a < $other")
    }

    def <=(other: Int): Predicate = self.toPredicate(s"${self.label} <= $other?") {
      case a if a > other => Result.success
      case a => Result.failure(s"Expected $a <= $other")
    }

    def +(other: Execution[Long]): Execution[Long] = {
      for {
        i <- self
        j <- other
      } yield i + j
    }.labeled(s"${self.label} + ${other.label}")

    def +(increment: Long): Execution[Long] = self.map(_ + increment).labeled(s"${self.label} + $increment")

    def between(a: Long, b: Long): Predicate = self.toPredicate(self.label + s" between $a and $b ?") {
      case i if i >= a && i <= b => Result.success
      case i => Result.failure(s"Expected result to be between $a and $b but is $i")
    }
  }

  implicit class OrderedExecution[A](val self: Execution[Ordered[A]]) extends AnyVal {
    def >(other: A): Predicate = self.toPredicate(s"${self.label} > $other?") {
      case a if a > other => Result.success
      case a => Result.failure(s"Expected $a > $other")
    }

    def >=(other: A): Predicate = self.toPredicate(s"${self.label} >= $other?") {
      case a if a >= other => Result.success
      case a => Result.failure(s"Expected $a >= $other")
    }

    def <(other: A): Predicate = self.toPredicate(s"${self.label} < $other?") {
      case a if a < other => Result.success
      case a => Result.failure(s"Expected $a < $other")
    }

    def <=(other: A): Predicate = self.toPredicate(s"${self.label} <= $other?") {
      case a if a <= other => Result.success
      case a => Result.failure(s"Expected $a <= $other")
    }
  }

  def emptyArray[A: ClassTag](): Execution[Array[A]] = Execution{Array[A]()}.labeled("Empty list")

  /* Implicit related to ClusterNode */
  implicit class IntToClusterNodeBuilder(val n: Int) extends AnyVal {
    def nodes[T <: ClusterNode]: MultipleClusterNodeBuilderStepOne = {
      new MultipleClusterNodeBuilderStepOne(n)
    }

    def node[T <: ClusterNode]: SingleClusterNodeBuilderStepOne = {
      if(n != 1) {
        throw new IllegalArgumentException("the 'node' method can only be used if the number of nodes is 1, else use 'nodes'.")
      }
      new SingleClusterNodeBuilderStepOne
    }
  }

  implicit def clusterNodeToNodeGroup(node: DockerClusterNode): NodeGroup = new NodeGroup(Seq(node))

  implicit def clusterNodesToNodeGroup(nodes: Seq[DockerClusterNode]): NodeGroup = new NodeGroup(nodes)

}
