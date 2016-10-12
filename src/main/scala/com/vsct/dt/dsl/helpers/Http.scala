package com.vsct.dt.dsl.helpers

import com.vsct.dt.dsl.core.Execution
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods._
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager

import scala.language.implicitConversions


object Http {

  private def initHttpClient(): HttpClient = {
    val requestConfig: RequestConfig = RequestConfig.custom()
      .setConnectTimeout(5000)
      .setSocketTimeout(5000)
      .setConnectionRequestTimeout(200)
      .build()


    val connectionManager = new PoolingHttpClientConnectionManager()
    connectionManager.setDefaultMaxPerRoute(50)
    connectionManager.setMaxTotal(100)

    HttpClientBuilder.create()
      .useSystemProperties()
      .setConnectionManager(connectionManager)
      .setDefaultRequestConfig(requestConfig)
      .build()
  }

  // Use var to allow anyone to override it
  var client: HttpClient = initHttpClient()

  def get(url: String): Execution[HttpResponse] = {
    execute(new HttpGet(url))
  }

  def put(url: String, data: String, contentType: String): Execution[HttpResponse] = {
    val request = new HttpPut(url)
    request.setEntity(new StringEntity(data, ContentType.create(contentType)))
    execute(request)
  }

  def post(url: String, data: String, contentType: String): Execution[HttpResponse] = {
    val request = new HttpPost(url)
    request.setEntity(new StringEntity(data, ContentType.create(contentType)))
    execute(request)
  }

  def execute(request: HttpUriRequest): Execution[HttpResponse] = {
    Execution(() => client.execute(request)).labeled(request.toString)
  }

}
