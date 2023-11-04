import com.typesafe.config.ConfigFactory
import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer
import org.slf4j.{Logger, LoggerFactory}

object Main {
  // Initializing Spark & Logger
  val logger: Logger = LoggerFactory.getLogger(Main.getClass)
  val conf: SparkConf = new SparkConf().setAppName(NGSConstants.SPARK_JOB_NAME).setMaster(NGSConstants.RUNNING_LOCALLY)
//  AWS EMR
//  val conf: SparkConf = new SparkConf().setAppName("Random Walks")

  val sc = new SparkContext(conf)

  // Deserializing graphs from json
  val (ogNodes, _) = Deserializer.loadGraph(NGSConstants.outputDirectory, NGSConstants.ORIGINAL_GRAPH)
  val (perturbedNodes, perturbedEdges) = Deserializer.loadGraph(NGSConstants.outputDirectory, NGSConstants.PERTURBED_GRAPH)
//  AWS EMR
//  val (ogNodes, _) = Deserializer.loadGraph(NGSConstants.AWS, NGSConstants.ORIGINAL_GRAPH)
//  val (perturbedNodes, perturbedEdges) = Deserializer.loadGraph(NGSConstants.AWS, NGSConstants.PERTURBED_GRAPH)

  // numIters stores the maximum number of iterations of random walks
  val numIters = Array.range(1, 5)
  // iterCounter stores the number of iterations performed
  // this will be used while outputting scores later
  val iterCounter = ArrayBuffer[Int](0)

  // Converting perturbedNodes & perturbedEdges to RDD format
  // so that we can create a graphx implementation of the perturbed graph
  val perturbedNodes_ : RDD[(VertexId, Any)] = sc.parallelize(perturbedNodes.map(node => {
    (node.id.asInstanceOf[Long], node)
  }).distinct)
  val perturbedEdges_ = sc.parallelize(perturbedEdges.map(action => {
    Edge(action.fromNode.id.asInstanceOf[Long], action.toNode.id.asInstanceOf[Long], action)
  }).distinct)

  // Creating a graphx implementation of the perturbed graph
  val perturbedGraph = Graph(perturbedNodes_, perturbedEdges_, NGSConstants.DEFAULT)
  perturbedGraph.cache()

  // This class houses all score computation metrics
  val outputParser = new OutputParser(ogNodes.maxBy(node => node.id).id + 1, (ogNodes.maxBy(node => node.id).id).max(perturbedNodes.maxBy(node => node.id).id) + 1, ogNodes)
  outputParser.initializeNodeMatrix()

  // Let's parse the difference.yaml file which contains the actual differences between the graphs
  outputParser.parseGoldenYAML(NGSConstants.outputDirectory, NGSConstants.GOLDEN_YAML)
//    AWS EMR
//  outputParser.parseGoldenYAML(NGSConstants.AWS, NGSConstants.GOLDEN_YAML)

  // This function executes random walks in parallel
  // outputParser decides whether to continue iterating or not
  // based on the similarity values collected
  // Maximum no of iterations is defined above in numIters
  def runRandomWalks(): Unit = {
    numIters.foreach(_ => {
      iterCounter += iterCounter.last + 1
      // Randomly selecting starting nodes from the graph
      val startingNodes = scala.util.Random.shuffle(perturbedGraph.vertices.collect().toList).take(4)
      // Performing random walks on those starting nodes in parallel
      val walksWithComparisons = sc.parallelize(startingNodes.map(node => {
        RandomWalker.performRandomWalk(perturbedGraph, node, ogNodes)
      })).collect()

      // Parsing the output of those random walks
      outputParser.parseOutput(walksWithComparisons.map(_._2).toList.flatten)
      // Computing differences between two graphs based on parsed output
      outputParser.computeDifferences()
      // Deciding whether to continue iterating or exit early
      if(!outputParser.continueIterating()) {
        return
      }
    })
  }

  def main(args: Array[String]): Unit = {
    logger.info("Starting Random Walks")
    runRandomWalks()
    // Outputting accuracy & other metrics
    outputParser.computeScore(iterCounter.last)
  }
}