scalaVersion := "2.12.4"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
    "org.pcollections" % "pcollections" % "2.1.2",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.2.0",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.0",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.2.0",
    "org.scalaz" %% "scalaz-core" % "7.2.22" % "test",
    "junit" % "junit-dep" % "4.11" % "test",
    "org.hamcrest" % "hamcrest-core" % "1.3" % "test",
    "org.hamcrest" % "hamcrest-library" % "1.3" % "test",
    "org.specs2" %% "specs2-core" % "4.1.0" % "test",
    "org.specs2" %% "specs2-matcher-extra" % "4.1.0" % "test",
    "org.specs2" %% "specs2-scalacheck" % "4.1.0" % "test",
    //"org.scalacheck" %% "scalacheck" % "1.14.0" % "test"
)

initialCommands :=
  """
    import unitard._;
  """

initialCommands in Test :=
  """
    import unitard.JavaInterop._;
    import unitard._;

    val stuff = Stuff.of(JMap(
      "a" -> JMap("aa" -> JList(1,2,3,4,5)), 
      "n" -> null, 
      "el" -> JList(), 
      (null).asInstanceOf[AnyRef] -> "frab",
      "nl" -> JList(null, null)));
  """
