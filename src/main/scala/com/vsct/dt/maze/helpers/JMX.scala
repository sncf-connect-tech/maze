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

package com.vsct.dt.maze.helpers

import javax.management.ObjectName
import javax.management.remote.{JMXConnector, JMXConnectorFactory, JMXServiceURL}

import com.vsct.dt.maze.core.Execution

object JMX {

  private def jmxUrl(host: String, port: Int) = s"service:jmx:rmi:///jndi/rmi://$host:$port/jmxrmi"

  def retrieveProperty[A](host: String, port: Int, jmxObject: String, property: String, castTo: Class[A]): Execution[A] = Execution(() => {
    val url: JMXServiceURL = new JMXServiceURL(jmxUrl(host, port))
    val jmxConnector: JMXConnector = JMXConnectorFactory.connect(url)
    val result = castTo.cast(jmxConnector.getMBeanServerConnection.getAttribute(ObjectName.getInstance(jmxObject), property))
    jmxConnector.close()
    result
  }).labeled(s"the property $property of the JMX bean $jmxObject")

  def retrieveStringProperty(host: String, port: Int, jmxObject: String, property: String): Execution[String] =
    retrieveProperty(host, port, jmxObject, property, classOf[String])


}
