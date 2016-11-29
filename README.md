
# Maze, automate your technical tests


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
scalaVersion := 2.12

libraryDependencies += "fr.vsct.dt" %% "maze" % "1.0.4"
```

### Using maven

```xml
...
<dependency>
  <groupId>fr.vsct.dt</groupId>
  <artifactId>maze_2.12</artifactId>
  <version>1.0.4</version>
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
trait Execution[A] {

  val label: String

  def execute(): Try[A]

  def map[B](f: (A) => B): Execution[B] = ...

  def flatMap[B](f: (A) => Execution[B]): Execution[B] = ...

  def value(): A = get().result.get

  def labelled(text: String): Execution[A] = ...

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

Ainsi, un objet de type Execution[String] aura des méthodes telles que length ou contains.
De même, pour tout objet de type Execution (Execution[Any]), certaines méthodes sont ajoutées, telles que is ou isNot.

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


