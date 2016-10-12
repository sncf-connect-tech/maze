package com.vsct.dt.dsl.core

import com.vsct.dt.dsl.core.Predef._
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
