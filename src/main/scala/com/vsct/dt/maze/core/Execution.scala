package com.vsct.dt.maze.core

import scala.util.{Failure, Success, Try}

/**
  * Trait defining all the checks that can be done
  */
trait Execution[A] {
  self =>

  val label: String

  def execute(): Try[A]

  def map[B](f: (A) => B): Execution[B] = new Execution[B] {
    override val label: String = self.label

    override def execute(): Try[B] = self.execute().map(f)
  }

  def flatMap[B](f: (A) => Execution[B]): Execution[B] = new Execution[B] {
    override val label: String = self.label

    override def execute(): Try[B] = self.execute() match {
      case Success(a) => f(a).execute()
      case Failure(e) => Failure(e)
    }
  }

  def labeled(text: String): Execution[A] = {
    new Execution[A] {
      override val label: String = text

      override def execute(): Try[A] = self.execute()
    }
  }

}

object Execution {
  def apply[A](fn: () => A): Execution[A] = {
    new Execution[A] {
      override def execute(): Try[A] = Try(fn())

      override val label: String = "Unlabeled user defined execution (use 'labeled' to give a meaning to this execution)"
    }
  }
}
