import NetGraphAlgebraDefs.NodeObject

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.conf.Configuration
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer
import java.io._
import scala.io.Source
import org.yaml.snakeyaml.Yaml

class OutputParser(size1: Int, size2: Int, originalNodes: List[NodeObject]) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  val nodeMatrix = Array.ofDim[Float](size1, size2)
  val valuableNodes = originalNodes.filter(node => node.valuableData).map(_.id)
  val nodesOG = originalNodes.map{node => node.id}

  val nodeAdded = ArrayBuffer[Int]()
  val nodeRemoved = ArrayBuffer[Int]()
  val nodeModified = ArrayBuffer[Int]()
  val nodeUnchanged = ArrayBuffer[Int]()

  val oNodeAdded = ArrayBuffer[Int]()
  val oNodeRemoved = ArrayBuffer[Int]()
  val oNodeModified = ArrayBuffer[Int]()

  def initializeNodeMatrix(): Unit = {
    (0 until size1).foreach(i => {
      (0 until size2).foreach(j => {
        if(i == j) nodeMatrix(i)(j) = 1.0f
      })
    })
  }

  def printMatrix(): Unit = {
    logger.info("PRINTING MATRIX")
    nodeMatrix.indices.foreach{ i => {
      val strArray = nodeMatrix(i).indices.map(j => s"${nodeMatrix(i)(j)}")
      logger.info(strArray.toString())
    }}
  }

  def parseOutput(output: List[String]): Unit = {
    logger.info("PARSING OUTPUT")
    output.foreach{ line => {
      val Array(first, second, scoreStr) = line.split("[\\-\\s]+")
      nodeMatrix(first.toInt)(second.toInt) = scoreStr.toFloat
    }}
  }

  def parseGoldenYAML(dir: String, fileName: String): Unit = {
    logger.info("Parsing the original yaml file")
    // TODO check if its working on AWS
    val conf = new Configuration()
//    val fileSystem = FileSystem.get(conf)
    val fileSystem = FileSystem.get(java.net.URI.create(dir), conf)
    val fsDataInputStream = fileSystem.open(new Path(dir.concat(fileName)))

    val source = Source.fromInputStream(fsDataInputStream)
    val lines = source.getLines().mkString(NGSConstants.NEW_LINE).replace(NGSConstants.TAB, " " * 4)

    val yaml = new Yaml()
    val data = yaml.load(lines).asInstanceOf[java.util.Map[String, Any]]
    val nodes = data.get(NGSConstants.NODES).asInstanceOf[java.util.Map[String, Any]]

    if(nodes.get(NGSConstants.MODIFIED) != null) nodes.get(NGSConstants.MODIFIED).asInstanceOf[java.util.ArrayList[Integer]].forEach { ele => oNodeModified += ele }
    if(nodes.get(NGSConstants.REMOVED) != null) nodes.get(NGSConstants.REMOVED).asInstanceOf[java.util.ArrayList[Integer]].forEach { ele => oNodeRemoved += ele }
    if(nodes.get(NGSConstants.ADDED) != null) nodes.get(NGSConstants.ADDED).asInstanceOf[java.util.Map[Integer, Integer]].values().forEach { ele => oNodeAdded += ele }
  }

  def computeDifferences(): Unit = {
    nodeRemoved ++= (nodeMatrix.zipWithIndex.filter {
      case (row, _) => row.forall(_ < 0.5f)
    }.map(_._2))
    logger.info("Removed Nodes")
    logger.info(nodeRemoved.toSet.toList.toString())

    // To calculate nodeAdded, we're looking for nodes in the perturbed graph that have 0 sim score in the matrix for all nodes in the original graph
    nodeAdded ++= (nodeMatrix.transpose.zipWithIndex.filter {
      case (column, _) => column.forall(_ < 0.5f)
    }.map(_._2).filterNot(nodeRemoved.contains))
    logger.info("Added Nodes")
    logger.info(nodeAdded.toSet.toList.toString())

    // To calculate node modified, we're looking for nodes in the original graph that have <0.75 sim score in the matrix for all nodes in the perturbed graph
    nodeModified ++= (nodeMatrix.zipWithIndex.filter {
      case (row, _) => row.forall(_ < 0.75f)
    }.map(_._2).filterNot(nodeRemoved.contains).filterNot(nodeAdded.contains)).distinct
    logger.info("Modified Nodes")
    logger.info(nodeModified.toSet.toList.toString())

    nodeUnchanged.clear()
    nodeUnchanged ++= nodesOG.diff((nodeAdded ++ nodeRemoved ++ nodeModified).toList)
  }

  def continueIterating(): Boolean = {
    if(nodeAdded.isEmpty || nodeRemoved.isEmpty || nodeModified.isEmpty) return true
    false
  }

  def computeScore(iter: Int): Unit = {
    val discardedTLNodes = nodeAdded ++ nodeRemoved ++ nodeModified
    val actualDiscardedTLNodes = oNodeAdded ++ oNodeRemoved ++ oNodeModified

    val matchedTLNodes = nodesOG.diff(discardedTLNodes)
    val actualTLNodes = nodesOG.diff(actualDiscardedTLNodes)
    val ctl = actualTLNodes.diff(matchedTLNodes).size
    val wtl = matchedTLNodes.diff(actualTLNodes).size
    val btl = ctl + wtl

    val dtl = (oNodeAdded.intersect(nodeAdded) ++ oNodeRemoved.intersect(nodeRemoved) ++ oNodeModified.intersect(nodeModified)).size
    val atl = actualTLNodes.intersect(matchedTLNodes).size
    val gtl = atl + dtl

    val rtl = gtl + btl
    val acc: Float = atl.toFloat / rtl.toFloat
    val bltr = wtl.toFloat / rtl.toFloat

    val fraction = (gtl - btl) / (2 * rtl).toFloat
    val vpr = fraction + 0.5f

    val successfulAttacks = nodeUnchanged.intersect(valuableNodes)
    val unsuccessfulAttacks = valuableNodes.filter { node => !nodeUnchanged.contains(node) }


    logger.info("GTL : " + gtl)
    logger.info("BTL : " + btl)
    logger.info("RTL : " + rtl)
    logger.info("BLTR : " + bltr)
    logger.info("Accuracy : " + acc)
    logger.info("Precision Ratio : " + vpr)

    logger.info("Attack Statistics")
    logger.info("Iterations : " + iter)
    logger.info("Successful Attacks : " + successfulAttacks.size)
    logger.info("Unsuccessful Attacks : " + unsuccessfulAttacks.size)
  }
}
