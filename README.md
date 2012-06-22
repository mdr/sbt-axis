An sbt version of the [Axis Tools Maven Plugin][1]. Supports calling `WSDL2Java` only, to generate Java from WSDL. 

You should also consider using [scalaxb][2].

Usage
-----

Put this in your `project/plugins.sbt`:

    addSbtPlugin("com.github.mdr" % "sbt-axis" % "0.0.1-SNAPSHOT")

And in `build.sbt`:

    seq(sbtAxisSettings : _*)

    SbtAxisKeys.wsdlFiles <+= baseDirectory(_ / "service.wsdl")

    SbtAxisKeys.packageSpace := Some("com.example")

There is an `SbtAxisKeys.otherArgs` for other `WSDL2Java` arguments -- if you use this, 
please consider adding a new setting to the plug-in and sending a pull request ;-)

  [1]: http://mojo.codehaus.org/axistools-maven-plugin/
  [2]: http://scalaxb.org/sbt-scalaxb
