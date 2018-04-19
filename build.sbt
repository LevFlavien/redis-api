import com.typesafe.sbt.packager.docker._

lazy val akkaHttpVersion = "10.0.11"
lazy val akkaVersion = "2.5.11"

enablePlugins(JavaServerAppPackaging)

resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo)


lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "redis-k8s",
      scalaVersion := "2.12.4"
    )),
    name := "redis-k8s",
    dockerEntrypoint ++= Seq(
      """-Dakka.actor.provider=cluster""",
      """-Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")"""",
      """-Dakka.remote.netty.tcp.port="$AKKA_REMOTING_BIND_PORT"""",
      """$(IFS=','; I=0; for NODE in $AKKA_SEED_NODES; do echo "-Dakka.cluster.seed-nodes.$I=akka.tcp://$AKKA_ACTOR_SYSTEM_NAME@$NODE"; I=$(expr $I + 1); done)""",
      "-Dakka.io.dns.resolver=async-dns",
      "-Dakka.io.dns.async-dns.resolve-srv=true",
      "-Dakka.io.dns.async-dns.resolv-conf=on"
    ),
    dockerCommands :=
      dockerCommands.value.flatMap {
        case ExecCmd("ENTRYPOINT", args@_*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
        case v => Seq(v)
      },
    dockerRepository := Some("knox.hurrycane.fr"),
    dockerUpdateLatest := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      "com.jason-goodwin" %% "authentikat-jwt" % "0.4.5",
      "ch.megard" %% "akka-http-cors" % "0.3.0",
      "org.mindrot" % "jbcrypt" % "0.3m",
      "net.debasishg" %% "redisclient" % "3.5"
    )
  )


