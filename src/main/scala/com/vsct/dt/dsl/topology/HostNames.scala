package com.vsct.dt.dsl.topology

object HostNames {

  /* Stores current indexes for hostname. Useful to avoid hostname collision when the hostname is user predefined */
  var hostnameIndexes: Map[String, Int] = Map()

  def getNextIndex(hostName: String): Int = {
    if (hostnameIndexes contains hostName) {
      val index = hostnameIndexes get hostName
      hostnameIndexes += (hostName -> (index.get + 1))
      index.get + 1
    } else {
      hostnameIndexes += (hostName -> 0)
      0
    }
  }

}
