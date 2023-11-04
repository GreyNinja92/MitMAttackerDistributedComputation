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
    // Using Hadoop FileSystem and InputStream for both AWS & Local Machine
    val conf = new Configuration()
//    AWS EMR
//    val fileSystem = FileSystem.get(java.net.URI.create(dir), conf)
    val fileSystem = FileSystem.get(conf)
    val fsDataInputStream = fileSystem.open(new Path(dir.concat(fileName)))
    // Loading json and converting them to strings
    val json = Source.fromInputStream(fsDataInputStream).mkString
    val arr = json.split(NGSConstants.NEW_LINE)
    // After splitting the strings, we initialize node & edge arrays and return them
    val nodeArr = decode[Set[NodeObject]](arr.head).right.get
    val edgeArr = decode[Set[Action]](arr.last).right.get

    (nodeArr.toList.distinct, edgeArr.toList.distinct)
  }
}