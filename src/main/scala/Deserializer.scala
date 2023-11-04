import NetGraphAlgebraDefs.{Action, NodeObject}
import org.slf4j.{Logger, LoggerFactory}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import org.apache.commons.io.FileUtils
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.{ObjectWritable, SequenceFile, Text}

import java.io._
import scala.io.Source
import scala.util.{Failure, Success, Try}

object Deserializer {
  val logger: Logger = LoggerFactory.getLogger(Deserializer.getClass)

  def loadGraph(dir: String, fileName: String): (List[NodeObject], List[Action]) = {
    val conf = new Configuration()
    println("BEING CALLED")
//    AWS EMR
    val fileSystem = FileSystem.get(java.net.URI.create(dir), conf)
//    val fileSystem = FileSystem.get(conf)
    val fsDataInputStream = fileSystem.open(new Path(dir.concat(fileName)))
    val json = Source.fromInputStream(fsDataInputStream).mkString
    val arr = json.split("\n")
    val nodeArr = decode[Set[NodeObject]](arr.head).right.get
    val edgeArr = decode[Set[Action]](arr.last).right.get

    (nodeArr.toList.distinct, edgeArr.toList.distinct)

//    Try(new FileInputStream(s"$outputDirectory$fileName")).map { fis =>
//      val ois = new ObjectInputStream(fis)
//      val ng = ois.readObject.asInstanceOf[List[NetGraphComponent]]
//      logger.info(s"Deserialized the object $ng")
//      ois.close()
//      fis.close()
//      ng
//    } match {
//      case Success(lstOfNetComponents) =>
//        nodes ++= lstOfNetComponents.collect { case node: NodeObject => node }
//        edges ++= lstOfNetComponents.collect { case edge: Action => edge }
//        logger.info(s"Deserialized ${nodes.length} nodes and ${edges.length} edges")
//      case Failure(exception) => logger.error(s"Failed to deserialize due to this error : ${exception}")
//    }
//    (nodes.toList.distinct, edges.toList.distinct)
  }
}