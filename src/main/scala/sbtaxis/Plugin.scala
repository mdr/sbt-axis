package sbtaxis

import sbt._
import sbt.Keys._

import org.apache.axis.utils._
import org.apache.axis.wsdl._
import org.apache.axis.wsdl.gen._

import java.lang.reflect.Field
import java.util.List

import scala.collection.JavaConverters._

object Plugin extends sbt.Plugin {

  val SbtAxis = config("sbtaxis")

  object SbtAxisKeys {

    val wsdl2java = TaskKey[Seq[File]]("wsdl2java", "Runs WSDL2Java")
    val wsdlFiles = SettingKey[Seq[File]]("axis-wsdl-files")
    val packageSpace = SettingKey[Option[String]]("axis-package-space", "Package to create Java files under, corresponds to -p / --package option in WSDL2Java")
    val otherArgs = SettingKey[Seq[String]]("axis-other-args", "Other arguments to pass to WSDL2Java")

  }

  import SbtAxisKeys._

  val sbtAxisSettings: Seq[Setting[_]] =
    Seq(
      javaSource in SbtAxis <<= sourceManaged in Compile,
      wsdlFiles := Nil,
      packageSpace := None,
      otherArgs := Nil,
      wsdl2java <<= (streams, wsdlFiles, javaSource in SbtAxis, packageSpace, otherArgs) map { runWsdlToJavas },
      sourceGenerators in Compile <+= wsdl2java,
      managedSourceDirectories in Compile <+= (javaSource in SbtAxis),
      cleanFiles <+= (javaSource in SbtAxis))

  private case class WSDL2JavaSettings(dest: File, packageSpace: Option[String], otherArgs: Seq[String])

  private def runWsdlToJavas(
    streams: TaskStreams,
    wsdlFiles: Seq[File],
    dest: File,
    packageSpace: Option[String],
    otherArgs: Seq[String]): Seq[File] =
    wsdlFiles.flatMap(wsdl =>
      runWsImport(streams, wsdl, WSDL2JavaSettings(dest, packageSpace, otherArgs)))

  private def makeArgs(wsdlFile: File, settings: WSDL2JavaSettings): Seq[String] =
    settings.packageSpace.toSeq.flatMap(p => Seq("--package", p)) ++
      Seq("--output", settings.dest.getAbsolutePath) ++
      settings.otherArgs ++
      Seq(wsdlFile.getAbsolutePath)

  private def runWsImport(streams: TaskStreams, wsdlFile: File, settings: WSDL2JavaSettings): Seq[File] = {
    streams.log.info("Generating Java from " + wsdlFile)

    streams.log.debug("Creating dir " + settings.dest)
    settings.dest.mkdirs()

    val args = makeArgs(wsdlFile, settings)
    streams.log.debug("wsimport " + args.mkString(" "))
    try
      new WSDL2JavaWrapper().execute(args.toArray)
    catch {
      case t =>
        streams.log.error("Problem running WSDL2Java " + args.mkString(" "))
        throw t
    }
    (settings.dest ** "*.java").get
  }

}

class WSDL2JavaWrapper extends WSDL2Java {

  def execute(args: Array[String]) {
    // Extremely ugly hack because the "options" static field in WSDL2Java
    // shadows the "options" instance field in WSDL2. It is the field
    // in WSDL2 that we need because the command line options
    // defined in subclasses get copied to it.
    // The result is that options defined in WSDL2 ( timeout, Debug )
    // are not available otherwise.  (MOJO-318)
    val field = classOf[WSDL2].getDeclaredField("options")

    val options = field.get(this).asInstanceOf[Array[CLOptionDescriptor]]

    for (option <- new CLArgsParser(args, options).getArguments.asScala)
      parseOption(option.asInstanceOf[CLOption])

    validateOptions()

    parser.run(wsdlURI)

  }

}
