//import com.typesafe.config.ConfigFactory
import NetGraphAlgebraDefs.{Action, NodeObject}
import org.apache.spark.sql.SparkSession
import org.apache.spark.{io, _}
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer
import org.slf4j.{Logger, LoggerFactory}

object Main {
  val logger: Logger = LoggerFactory.getLogger(Main.getClass)
  val conf: SparkConf = new SparkConf().setAppName("Random Walks")//å.setMaster("local[4]")
  val sc = new SparkContext(conf)
//  val spark = SparkSession.builder().appName("Random Walks").getOrCreate()
//  val sc = spark.sparkContext

  val (ogNodes, _) = Deserializer.loadGraph(NGSConstants.AWS, NGSConstants.ORIGINAL_GRAPH)
  val (perturbedNodes, perturbedEdges) = Deserializer.loadGraph(NGSConstants.AWS, NGSConstants.PERTURBED_GRAPH)

  val numIters = Array.range(1, 5)
  val iterCounter = ArrayBuffer[Int](0)

  val perturbedNodes_ : RDD[(VertexId, Any)] = sc.parallelize(perturbedNodes.map(node => {
    (node.id.asInstanceOf[Long], node)
  }).distinct)
  val perturbedEdges_ = sc.parallelize(perturbedEdges.map(action => {
    Edge(action.fromNode.id.asInstanceOf[Long], action.toNode.id.asInstanceOf[Long], action)
  }).distinct)

  val perturbedGraph = Graph(perturbedNodes_, perturbedEdges_, "default")
  perturbedGraph.cache()

  val outputParser = new OutputParser(ogNodes.maxBy(node => node.id).id + 1, (ogNodes.maxBy(node => node.id).id).max(perturbedNodes.maxBy(node => node.id).id) + 1, ogNodes)
  outputParser.initializeNodeMatrix()
  outputParser.parseGoldenYAML(NGSConstants.AWS, NGSConstants.GOLDEN_YAML)

  def runRandomWalks(): Unit = {
    numIters.foreach(_ => {
      iterCounter += iterCounter.last + 1
      val startingNodes = scala.util.Random.shuffle(perturbedGraph.vertices.collect().toList).take(4)
      val walksWithComparisons = sc.parallelize(startingNodes.map(node => {
        RandomWalker.performRandomWalk(perturbedGraph, node, ogNodes)
      })).collect()

      outputParser.parseOutput(walksWithComparisons.map(_._2).toList.flatten)
      outputParser.computeDifferences()
      if(!outputParser.continueIterating()) {
        return
      }
    })
  }

  def main(args: Array[String]): Unit = {
    runRandomWalks()
//    outputParser.printMatrix()
    outputParser.computeScore(iterCounter.last)
  }
}