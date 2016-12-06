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

package fr.vsct.dt.maze.helpers

import com.typesafe.scalalogging.StrictLogging
import fr.vsct.dt.maze.core.Commands.expectThat
import fr.vsct.dt.maze.core.Predef._
import fr.vsct.dt.maze.core.{Predicate, Result}
import org.apache.http._
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.message.{BasicHttpResponse, BasicStatusLine}
import org.apache.http.protocol.HttpContext
import org.scalatest.FlatSpec

import scala.beans.BeanProperty


class HttpExecutionTest extends FlatSpec {

  /**
    * A mock class for http client helping to ensure that calls are not done before we want them done
    *
    * @param response the text response that will be returned when doing a mock call
    */
  class MockHttpClient(val response: String) extends CloseableHttpClient with StrictLogging {
    var init = false

    override def doExecute(target: HttpHost, request: HttpRequest, context: HttpContext): CloseableHttpResponse = {
      if (!init) throw new IllegalStateException("Client is not initialized")
      logger.info("Doing actual http call")
      val r = if(request.getRequestLine.getUri == "http://some-url.com") {
        val t = new BasicCloseableHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK"))
        t.setEntity(new StringEntity(response, "UTF-8"))
        t
      } else {
        val t = new BasicCloseableHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "KO"))
        t.setEntity(new StringEntity("""{"status": "ko"}""", ContentType.APPLICATION_JSON))
        t
      }
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

    val requestOk = new HttpGet("http://some-url.com")

    val check1: Predicate = Http.execute(requestOk).status is 200
    val check2: Predicate = Http.execute(requestOk).response is "Youppy !"

    val check3 = check1 || check2

    val check4 = !check3

    Http.client.asInstanceOf[MockHttpClient].init = true

    assert(check1.get() == Result.success)
    assert(check2.get() == Result.success)
    assert(check3.get() == Result.success)
    assert(check4.get() == Result.failure(s"Expected ${check3.label} to be false"))
    expectThat(Http.get("http://some-error-url.com").status is 400)
    expectThat(Http.get("http://some-url.com").isOk)
    expectThat(!Http.get("http://some-error-url.com").isOk)
    expectThat(Http.get("http://some-error-url.com").responseAs(classOf[Stupid]) is Stupid(status = "ko"))

  }

}

case class Stupid(@BeanProperty status: String)