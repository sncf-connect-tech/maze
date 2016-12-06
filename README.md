
# Maze, automate your technical tests

[![Build Status](https://travis-ci.org/voyages-sncf-technologies/maze.svg?branch=master)](https://travis-ci.org/voyages-sncf-technologies/maze)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[<img src="https://img.shields.io/maven-central/v/fr.vsct.dt/maze_2.12.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22maze_2.12%22)
[![Coverage Status](https://coveralls.io/repos/github/voyages-sncf-technologies/maze/badge.svg?branch=master)](https://coveralls.io/github/voyages-sncf-technologies/maze?branch=master)


Maze is a tool to automate technical tests. You might want them in some of the following cases :

- You have a new product (kafka, redis, ...) you need to deploy for your application,
  and want to understand how it behaves to configure it as needed and understand better what could happen in production
- You need to connect to some external system and want to make sure your application will be robust when perturbations happen
- You have micro-services architecture with some scenarios including different components
  and want to make sure you will always have consistent behaviour when perturbations happen
- You need to run these tests frequently to ensure you don't have resilience regressions

## How it works


Maze is a unit test library, made for scalatest, that will have the following lifecycle :

- In the beginning of a test, create a dedicated docker network
- Before each test, start the configured clusters
- execute the test
- Stop the clusters
- In the end of all the tests, remove the network


All the tests will then consist in communications with your applications / tools, deployed as docker images and the unit test, orchestrating them.

## Get it in your project


### Using sbt (preferred)

```scala
scalaVersion := "2.12.0"

libraryDependencies += "fr.vsct.dt" %% "maze" % "1.0.6"
```

### Using maven

```xml
...
<dependency>
  <groupId>fr.vsct.dt</groupId>
  <artifactId>maze_2.12</artifactId>
  <version>1.0.6</version>
</dependency>
...
<plugin>
    <groupId>net.alchim31.maven</groupId>
    <artifactId>scala-maven-plugin</artifactId>
    <version>3.2.2</version>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
                <goal>testCompile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.scalatest</groupId>
    <artifactId>scalatest-maven-plugin</artifactId>
    <version>1.0</version>
    <configuration>
        <reportsDirectory>${project.build.directory}/surefire-reports</reportsDirectory>
        <junitxml>.</junitxml>
        <filereports>TestSuite.txt</filereports>
    </configuration>
</plugin>
```

## Developing a test using maze

Step 1: Dockerize the application you want to test
Step 2: Configure nodes for it 

```scala

...
import fr.vsct.dt.maze.core.Predef._
...

class MyAwesomeApplicationNode extends SingleContainerClusterNode {
  override val servicePort: Int = 9000
  override val serviceContainer: CreateContainerCmd = "some/image:1.2.3".withEnv("SOME_VARIABLE=some-value")
} 
```

Maze uses docker-java to interact with docker, more information on container creation there.

Step 3: Create your cluster

```scala

class MyAwesomeApplicationCluster extends Cluster[MyAwesomeApplicationNode]

```

Step 4: Create a unit test

```scala
...
import fr.vsct.dt.maze.core.Predef._
import fr.vsct.dt.maze.core.Commands._
...

class MyAwesomeApplicationTest extends TechnicalTest {

  var myAwesomeCluster: MyAwesomeApplicationCluster = _
  
  override protected def beforeEach(): Unit = {
    myAwesomeCluster = new MyAwesomeApplicationCluster
    myAwesomeCluster.add(3.nodes named "my-awesome-node" constructedLike(new MyAwesomeApplicationNode))
    myAwesomeCluster.start()
  }
  
  "my awesome applications" should "response to http requests" in {
    // Do all your testing logic here, for instance:
    val someNode = myAwesomeCluster.nodes.head
    expectThat(Http.get(s"http://${someNode.externalIp}:${someNode.mappedPort.get}/healthcheck").status is 200)
  }
  
  override protected def afterEach(): Unit = {
    myAwesomeCluster.stop()
  }
  
}

```

## Execution and predicates

Technical tests often require observation: wait for a cluster to recover, for some messages to be consumed, and so on. 
That's why we need to create repeatable commands and checks. These commands are subclasses of the Execution trait :

```scala
trait Execution[A] {

  def execute(): Try[A]

  def map[B](f: (A) => B): Execution[B] = ...

  def flatMap[B](f: (A) => Execution[B]): Execution[B] = ...

  def value(): A = get().result.get

  val label: String
  def labelled(text: String): Execution[A] = ...

}
```

This trait describes commands that can be executed, returning a new result with each execution.

- the execute method will do the actual method call, which will certainly include side effects, and would have many reasons to be in error.
- The map / flaMap method allows to compose the execution the same way as it can be dome with any other monad. 
  FlatMap also allows the use of for-comprehensions.
- the value method returns the result of an execution, throwing an exception if an error occurred
- The field / methods label / labelled allow to describe the execution in a human readable way. This allows better error messages.


A special case of these executions are the boolean executions: when we have one, there cases when you'd want to apply a function
Boolean => A is quite rare (or maybe you wanted to use something more meaningful then a boolean?), and in most of case,
you'll have at most boolean operations (and, or, not). 

Since these Execution[Boolean] can be seen as leaves, and that the 3 states (true, false, error) are important, maze defines a Predicate class.
A predicate class is like an Execution[Boolean], but less generic.


```scala
abstract class Predicate {
  self =>

  val label: String

  def get(): PredicateResult

  def execute(): Boolean = ...

  def labeled(text: String): Predicate = ...

  def unary_! : Predicate = ...

  def &&(other: Predicate): Predicate = ...

  def ||(other: Predicate): Predicate = ...

}

case class PredicateResult(result: Try[Boolean], message: String)

```

The results of the predicates include the boolean result, but also a message. This message is meant to be a human readable message to display to the user.

This makes error message readable.

In order to create a Predicate from an execution, the most simple way is to use the toPredicate method:

```scala
def is(other: A): Predicate = self.toPredicate(s"${self.label} is '$other'?") {
  case a if a == other => Result.success
  case a => Result.failure(s"Expected '$a' to be '$other'")
}
```

This example is taken from the core of maze. 



These executions and predicate are then to be manipulated with the commands.

## The commands

```scala
object commands {
 
  def expectThat(predicate: Predicate): Unit = ...
 
  def print(dsl: Execution[_]): Unit = ...
 
  def exec[A](execution: Execution[A]): A = ...
 
  def waitFor(duration: FiniteDuration): Unit = ...
 
  def waitUntil(predicate: Predicate, butNoLongerThan: FiniteDuration = 5 minutes): Unit = ...
 
  def waitWhile(predicate: Predicate, butNoLongerThan: FiniteDuration = 5 minutes): Unit = ...
 
  def repeatWhile(predicate: Predicate, butNoLongerThan: FiniteDuration = 5 minutes)(doSomething: => Unit): Unit = ...
 
  def doIf(condition: Predicate)(code: => Unit): IfResult = ...
 
  sealed trait ElseResult {
    def onError(code: Exception => Unit) = {}
  }
 
  sealed trait IfResult extends ElseResult {
    def orElse(code: => Unit): ElseResult = this
  }
 
}
```



```scala
def expectThat(predicate: Predicate): Unit = ...

// For instance:

expectThat(Http.get("http://www.google.fr").status is 200)
```

Expects that a given Predicate will return Success(true). If not, throws an exception using the label of the predicate and the message of the PredicateResult.


```scala
def print(dsl: Execution[_]): Unit = ...
```

Prints the label and result of an Execution


```scala
def exec[A](execution: Execution[A]): A = ...
```

Executes an Execution right away, throwing an Exception if an error occurs


```scala
def waitFor(duration: FiniteDuration): Unit = ...

// for instance
waitFor(1 minute)
```

A more expressive way to wait than Thread.sleep.


```scala
def waitUntil(predicate: Predicate, butNoLongerThan: FiniteDuration = 5 minutes): Duration = ...
 
def waitWhile(predicate: Predicate, butNoLongerThan: FiniteDuration = 5 minutes): Duration = ...
 
def repeatWhile(predicate: Predicate, butNoLongerThan: FiniteDuration = 5 minutes)(doSomething: => Unit): Duration = ...
```

Loop on predicate conditions and returns the time taken for the condition to happen. For Instance:


```scala
waitUntil(Http.get("http://some-url").status is 200, butNoLongerThan: 30 seconds)

// Or you can also write:

waitUntil(Http.get("http://some-url").status is 200) butNoLongerThan(30 seconds)
```



```scala
def doIf(condition: Predicate)(code: => Unit): IfResult = ...

// For instance
doIf(somePredicate) {
  expectThat(...)
} orElse {
  expectThat(...)
} onError { e =>
    ...
}
```

## Introducing perturbations in your test
 
Maze can introduce some system / network perturbations in your nodes, in order to test the resilience of the system.
 
To be able to introduce these perturbations, maze will need:

- Some rights on container (containers are run as privileged with a few capabilities added)
- Some tools available in the container, for instance iptables or tc.


### Stopping nodes

```scala
// Stops the node using docker stop
node.stop()
  
// Stops the node by sending a SIGTERM signal to the process
node.kill()
  
// Stops the node by sending a KILL signal to the process
node.crash()
```

### Network isolation / partition

```scala
tag(injector + master) as "Isolated injector and master"
DockerNetwork.isolate("Isolated injector and master")
  
...
  
DockerNetwork.cancelIsolation()
```

```scala
DockerNetwork.split(node1 + node2, node3)
  
...
  
DockerNetwork.cancelIsolation()
```

### Network lag

```scala
// Adds network lag on node to anything
node.lag(2 seconds)
  
  
// Adds lag towards another node
val sourceNode = ...
val destinationNode = ...
  
DockerNetwork.setLag(from = sourceNode.containerId, to = destinationNode.containerId, lag = 2 seconds)
  
  
// Adds lag to an external host
DockerNetwork.setLagToExternalHost(on = node.containerId, externalHost = "10.11.12.13", lag = 2 seconds)
  
  
  
// Remove lag
DockerNetwork.removeLag(on = node.containerId)
```

## Making executions richer

The execution trait is simple, map / flatMap allow almost anything, but don't make user friendly code (someone not too much used to scala will need time to read it well).
 
 
In order to make them richer, methods are added to Executions, according to the type of it. These are added using implicits.
In order to use the basinc implicits, you must import the maze Predef package.

```scala
import fr.vsct.dt.maze.core.Predef._
```
 
 
For instance, the following implicits are available on any execution:

```scala
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
```

Using this mechanism, it's easy to create your own rich executions.
