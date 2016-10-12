
# Le module DSL
==========

Ceci est le coeur du projet.

Il définit différentes couches d'abstraction au dessus de docker et des outils à tester.

## La philosophie
----------

Les différents tests sont des tests unitaires, lancés avec scalatest. Ils se trouvent dans src/test/scala.

Ces tests contiennent plusieurs phases :

  - En début de test, création d'un network dédié
  - Avant chaque test, lancement des clusters, dans l'ordre permettant une bonne initialisation
  - A la fin de chaque test, extinction et nettoyage des différents clusters
  - A la fin des tests, suppression du network


Afin d'utiliser les différents outils, les tests sont configurés en étendant un trait de configuration.
Ce trait a pour but de mettre dans le contexte des tests tous les différents paramètres implicites.

Ainsi, la configuration de tel ou tel composant peut être facilement différents d'un test à un autre.

## Les objets de type DSL et les opérations supportées
----------

Les objets de type DSL permettent de répéter des opérations et avoir des assertions sur les résultats dans un langage qui se veut lisible.

Actuellement, les cas d'usage de ces objets sont :

  - Attendre qu'une condition se réalise (par exemple attendre qu'un serveur finisse de se lancer, qu'une élection master / slave se produise, etc..)
  - Avoir des assertions sur certaines conditions. Ce point est mitigé par le fait que scalatest fournisse également des assertions riches.

Le trait DSL (l'implémentation des méthodes a été supprimée pour plus de clarté) :

```scala
trait DSL[A] { self =>

  val stringFragment: String

  def get(): ExecutionResult[A]

  def map[B](f: (A) => B, fragment: String = self.stringFragment): DSL[B] = ...

  def flatMap[B](f: (A) => DSL[B]): DSL[B] = ...

  def value(): A = get().result.get

  def withText(text: String): DSL[A] = ...

}
```

Dans sa forme la plus simple, un objet de type DSL contient :

  - une description : stringFragment
  - une méthode pour calculer un résultat : get()

Ensuite, d'autres méthodes plus utilitaires viennent l'enrichir :

  - La méthode withText permet de changer la dercription d'un objet DSL. Cette méthode est principalement utile après l'utilisation de map ou flatmap
  - La méthode value() permet de récupérer la valeur calculée sans enrobage, mais lance une exception si le calcule de la valeur a lancé une exception
  - Les méthodes map et flatmap permettent des transformations analogues à la manipulation d'objets de type Option ou Try, et permettent l'utilisation de patterns scala, tels que le for-comprehension.


Enrichir les objets DSL selon leur type :

Afin d'enrichir les objets de type DSL sans polluer le trait de base, des méthodes sont ajoutées aux objets, selon le type qu'ils enveloppent.

Ainsi, un objet de type DSL[String] aura des méthodes telles que length ou contains.
De même, pour tout objet de type DSL (DSL[Any]), certaines méthodes sont ajoutées, telles que is ou isNot.

Cet enrichissement est fait par le mécanisme de conversions implicites.

Ainsi, la classe com.vsct.dt.dsl.RichDsl va ajouter des méthodes à tous les objets de type DSL,
la classe com.vsct.dt.dsl.ArrayDsl va ajouter des méthodes à tous les objets DSL mappant des tableaux, etc.

Afin que les conversions puissent être utilisées ensuite, il suffit de les déclarer dans le trait com.vsct.dt.dsl.RicherDsl

## Les objets bas niveau
----------

### L'objet Docker

L'objet docker encapsule tous les appels à l'api docker-java.

Afin de fonctionner correctement, l'objet docker a besoin, en implicite, d'une instance de com.github.dockerjava.api.DockerClient.
Le plus simple est d'utiliser le trait com.vsct.dt.configuration.DockerDefaultConfiguration,
également contenu dans com.vsct.dt.configuration.DockerDefaultConfiguration.
Cette configuration permet l'utilisation de variables d'environnement pour spécifier le DOCKER_HOST, etc..


L'objet Docker ne devrait pas être utilisé directement dans les classes de test unitaire, mais être utilisés dans les modules fonctionnels.


### L'objet Network

L'objet Network permet de jouer sur le réseau. Il supporte pour le moment les points suivants :

  - Gestion du réseau docker
  - Ajout de latence à un conteneur
  - Possibilité de splitter créer des partitions réseau puis de rétablir la situation

Cet objet peut être utilisé dans les tests unitaires.

### L'objet Http

Cet objet sert à effecter des requêtes http et retourne des DSL. Il est conseillé d'encapsuler les appels à l'objet Http dans les modules fonctionnels.

Attention : afin de pouvoir effectuer les appels, les ports doivent être "bindés" sur le host,
sinon, il est possible que le réseau créé précédemment crée des problèmes de routage (gateway à ajouter sur le poste local)


## Les objets fonctionnels
----------

Les objets fonctionnels sont des objets utilitaires spécialisés pour l'utilisation d'un outil donné.
Ces objets doivent savoir gérer des clusters et leurs noeuds.

### Gestion des clusters

L'unité de base est le noeud du cluster.
Pour les créer, il faut hériter de la classe com.vsct.dt.dsl.ClusterNode.

Cette class abstraite prend en paramètre de constructeur une spécification de conteneur Docker, décrivant comment lancer le noeud. Cette dernière peut être crée à partir de l'objet docker.
Ainsi, lors de la création du cluster, les différent noeuds sont créés depuis ces définitions.

Ensuite, pour contrôler ces noeuds, il suffit d'étendre de la classe com.vsct.dt.dsl.Cluster.
Cette classe prend en paramètre de constructeur un ensemble de noeuds.

Ainsi, les opérations de base (start, stop, kill, etc.) sont implémentées

### Opérations spécifiques

Les objets fonctionnels permettent des opérations riches basées sur l'outil à tester.
Il est conseillé d'y mettre les méthodes utilitaires de création de cluster.

Par exemple, dans com.vsct.dt.components.Zookeeper :

```scala
  def cluster(nodes: Int)(implicit dockerClient: DockerClient) = {

      val range = 1 to nodes

      val servers = range.map(s => s"zk$s").mkString(",")

      new ZookeeperCluster(range map { i =>
        new ZookeeperNode(Docker.prepareCreateContainer("dockerregistrydev.socrate.vsct.fr/resilience/zookeeper:3.4.6")
          .withEnv(s"SERVERS=$servers", s"MYID=$i")
          .withHostName(s"zk$i")
          .withName(s"zk$i")
          .withNetworkMode("technical-tests") // Docker 1.10 : specify a network other than default network will give you a free dns.
          , i)
      }:_*)
    }
```

Ainsi la création d'un cluster zookeeper se fait ainsi :

```scala
  val zookeeperCluster = Zookeeper.cluster(nodes = 3)
```

Le paramètre implicite dockerClient est fourni par la configuration.


