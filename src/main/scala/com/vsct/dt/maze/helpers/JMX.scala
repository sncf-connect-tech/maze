package com.vsct.dt.maze.helpers

import javax.management.ObjectName
import javax.management.remote.{JMXConnector, JMXConnectorFactory, JMXServiceURL}

import com.vsct.dt.maze.core.Execution

object JMX {

  private def jmxUrl(host: String, port: Int) = s"service:jmx:rmi:///jndi/rmi://$host:$port/jmxrmi"

  def retrieveProperty[A](host: String, port: Int, jmxObject: String, property: String, castTo: Class[A]): Execution[A] = Execution(() => {
    val url: JMXServiceURL = new JMXServiceURL(jmxUrl(host, port))
    val jmxConnector: JMXConnector = JMXConnectorFactory.connect(url, null)
    val result = castTo.cast(jmxConnector.getMBeanServerConnection.getAttribute(ObjectName.getInstance(jmxObject), property))
    jmxConnector.close()
    result
  }).labeled(s"the property $property of the JMX bean $jmxObject")

  def retrieveStringProperty(host: String, port: Int, jmxObject: String, property: String): Execution[String] =
    retrieveProperty(host, port, jmxObject, property, classOf[String])


}
