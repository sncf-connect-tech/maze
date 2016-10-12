package com.vsct.dt.maze.helpers

import com.vsct.dt.maze.core.Predef._
import com.vsct.dt.maze.core.{Predicate, Result}
import org.apache.http._
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.{BasicHttpResponse, BasicStatusLine}
import org.apache.http.protocol.HttpContext
import org.scalatest.FlatSpec

import scala.Predef._

class HttpExecutionTest extends FlatSpec {

  /**
    * A mock class for http client helping to ensure that calls are not done before we want them done
    *
    * @param response the text response that will be returned when doing a mock call
    */
  class MockHttpClient(val response: String) extends CloseableHttpClient {
    var init = false

    override def doExecute(target: HttpHost, request: HttpRequest, context: HttpContext): CloseableHttpResponse = {
      if (!init) throw new IllegalStateException("Client is not initialized")
      println("Doing actual http call")
      val r = new BasicCloseableHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 200, "OK"))
      r.setEntity(new StringEntity(response, "UTF-8"))
      r
    }

    @Deprecated
    override def getConnectionManager = null

    @Deprecated
    override def getParams = null

    override def close(): Unit = {}

    class BasicCloseableHttpResponse(statusLine: StatusLine) extends BasicHttpResponse(statusLine) with CloseableHttpResponse {
      override def close(): Unit = {}
    }

  }

  "a http check" should "not do an effective call until apply is effectively called" in {

    Http.client = new MockHttpClient("Youppy !")

    val request = new HttpGet("http://some-url.com")

    val check1: Predicate = Http.execute(request).status is 200
    val check2: Predicate = Http.execute(request).response is "Youppy !"

    val check3 = check1 || check2

    val check4 = !check3

    Http.client.asInstanceOf[MockHttpClient].init = true

    assert(check1.get() == Result.success)
    assert(check2.get() == Result.success)
    assert(check3.get() == Result.success)
    assert(check4.get() == Result.failure(s"Expected ${check3.label} to be false"))

  }

}
