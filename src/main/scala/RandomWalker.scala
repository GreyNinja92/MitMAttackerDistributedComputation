import NetGraphAlgebraDefs.{Action, NodeObject}
import org.apache.spark.graphx._

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object RandomWalker {
  def performRandomWalk(graph: Graph[Any, Action], startingNode: (VertexId, Any), nodesOriginalGraph: List[NodeObject]) : (List[(VertexId, Any)], List[String]) = {
    val numSteps = Array.range(1, 11)
    val walk = ArrayBuffer[(VertexId, Any)](startingNode)
    val comparisons = ArrayBuffer[String]()

    // TODO: Add comparison for starting node

    numSteps.foreach(_ => {
      val out_neighbours = graph.collectNeighbors(EdgeDirection.Out).lookup(walk.toList.last._1).head
      val in_neighbours = graph.collectNeighbors(EdgeDirection.In).lookup(walk.toList.last._1).head
      val neighbours = (out_neighbours ++ in_neighbours).distinct.filterNot(n => walk.toArray.contains(n))

      if(neighbours.nonEmpty) {
        val successor = neighbours(Random.nextInt(neighbours.length))
        nodesOriginalGraph.foreach(node => {
          val simMeasure = SimilarityMeasure.findSimilarityNodes(node, successor._2.asInstanceOf[NodeObject])
          comparisons += (s"${node.id} ${successor._2.asInstanceOf[NodeObject].id} - ${simMeasure}")
        })
        walk += successor
      } else {
        return (walk.toList, comparisons.toList)
      }
    })
    (walk.toList, comparisons.toList)
  }
}
