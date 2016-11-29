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

import fr.vsct.dt.maze.core.Predef._
import net.sf.saxon.s9api.XdmValue
import org.scalatest.FlatSpec

import scala.util.{Failure, Success}

class XpathTest extends FlatSpec {

  "an xpath resulting to an array" should "return it properly" in {

    val xml = """<?xml version="1.0" ?>
             |<test>
             |  <value>0</value>
             |  <value>1</value>
             |  <value>2</value>
             |</test>
              """.stripMargin

    val base: Execution[String] = Execution(() => xml)

    val xpath: Execution[XdmValue] = base.xpath("//test/value")
    xpath.length().execute() match {
      case Success(3) => // ok
      case Failure(e) => fail(e)
      case Success(number) => fail(s"Found $number results, expected 3")
    }

    xpath.first().stringValue().execute() match {
      case Success("0") => // cool
      case Failure(e) => fail(e)
      case Success(other) => fail(s"Expected to find 0 but got $other")
    }

    xpath.itemAt(2).stringValue().execute() match {
      case Success("2") => // cool
      case Failure(e) => fail(e)
      case Success(other) => fail(s"Expected to find 0 but got $other")
    }

  }


  "an xpath resulting to an empty array" should "handle it properly" in {

    val xml = """<?xml version="1.0" ?>
                |<test>
                |  <value>0</value>
                |  <value>1</value>
                |  <value>2</value>
                |</test>
              """.stripMargin

    val base: Execution[String] = Execution(() => xml)

    val xpath: Execution[XdmValue] = base.xpath("//some/path")
    xpath.length().execute() match {
      case Success(0) => // ok
      case Failure(e) => fail(e)
      case Success(number) => fail(s"Found $number results, expected 3")
    }

  }


  "an xpath from a non-valid xml" should "generate an error" in {

    val xml = "some non-xml value"

    val base: Execution[String] = Execution(() => xml)

    val xpath: Execution[XdmValue] = base.xpath("//test/value")
    xpath.execute() match {
      case Failure(e) => // was expected
      case Success(_) => fail("was expecting an error but didn't get one.")
    }

  }

}
