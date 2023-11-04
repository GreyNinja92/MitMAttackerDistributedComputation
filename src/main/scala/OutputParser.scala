import NetGraphAlgebraDefs.NodeObject

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.conf.Configuration
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import org.yaml.snakeyaml.Yaml

// This class parses difference.yaml file and computes traceability links and scores
class OutputParser(size1: Int, size2: Int, originalNodes: List[NodeObject]) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)
  // This is the node matrix which stores node similarity information.
  val nodeMatrix = Array.ofDim[Float](size1, size2)
  val valuableNodes = originalNodes.filter(node => node.valuableData).map(_.id)
  val nodesOG = originalNodes.map{node => node.id}

  val nodeAdded = ArrayBuffer[Int]()
  val nodeRemoved = ArrayBuffer[Int]()
  val nodeModified = ArrayBuffer[Int]()
  val nodeUnchanged = ArrayBuffer[Int]()

  // o stands for original graph. These array buffers will be used later for score computation.
  val oNodeAdded = ArrayBuffer[Int]()
  val oNodeRemoved = ArrayBuffer[Int]()
  val oNodeModified = ArrayBuffer[Int]()

  // Helper function to initialize node matrix while assuming the graph is unchanged
  // This information will be updated as we get data from random walks
  def initializeNodeMatrix(): Unit = {
    (0 until size1).foreach(i => {
      (0 until size2).foreach(j => {
        if(i == j) nodeMatrix(i)(j) = 1.0f
      })
    })
  }

  // Helper function to print matrix
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

  // This function parses the golden yaml file i.e. the original yaml file containing all the perturbations.
  // I'm using snakeyaml for parsing and separating data. I split the yaml file into added nodes, modified nodes & removed nodes.
  // I then convert them into the same data structures as the ones we'll use for computing traceability links for our random walk output.
  // In the end, I store all information in the file to all the 'o' data structures initialized above.
  // I keep the format the same for all data structures so that later on when we're computing scores, we can directly use intersect and diff
  // functions from scala collection.
  def parseGoldenYAML(dir: String, fileName: String): Unit = {
    logger.info("Parsing the original yaml file")
    val conf = new Configuration()
    val fileSystem = FileSystem.get(conf)
//    AWS EMR
//    val fileSystem = FileSystem.get(java.net.URI.create(dir), conf)
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

  // This function is used to compute traceability links between nodes in both graphs
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

  // This function tells us to continue iterating or not based on the differences computed above
  // Since we know the graphs are different, we can iterate one more time if the algorithm can't find differences between the two graphs
  def continueIterating(): Boolean = {
    if(nodeAdded.isEmpty || nodeRemoved.isEmpty || nodeModified.isEmpty) return true
    false
  }

  // This function is used to compute scores. Since, all data structures for nodes follow the same
  // structure, computing differences and intersections can be done using scala collections.
  // Any node that is not present in added, modified or removed arrays is considered matched i.e. the algorithm managed to compute a traceability link.
  // Successful Attacks are determined by computing correct traceability links for valuable nodes
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

    val successfulAttacks = nodeUnchanged.intersect(valuableNodes) ++ nodeModified.intersect(valuableNodes)
    val unsuccessfulAttacks = valuableNodes.filter { node => !nodeUnchanged.contains(node) || !nodeModified.contains(node) }

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
