import NetGraphAlgebraDefs.{Action, NodeObject}
import org.apache.spark.graphx._

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object RandomWalker {
  // This function performs random walks starting from startingNode
  def performRandomWalk(graph: Graph[Any, Action], startingNode: (VertexId, Any), nodesOriginalGraph: List[NodeObject]) : (List[(VertexId, Any)], List[String]) = {
    // Maximum no. of steps is defined below. If there are no more neighbours, the walk can stop early
    val numSteps = Array.range(1, 11)
    // Storing the vertexes traversed in the walk & output of similarity computations
    val walk = ArrayBuffer[(VertexId, Any)](startingNode)
    val comparisons = ArrayBuffer[String]()

    numSteps.foreach(_ => {
      // Collecting all neighbours of the node
      val out_neighbours = graph.collectNeighbors(EdgeDirection.Out).lookup(walk.toList.last._1).head
      val in_neighbours = graph.collectNeighbors(EdgeDirection.In).lookup(walk.toList.last._1).head
      val neighbours = (out_neighbours ++ in_neighbours).distinct.filterNot(n => walk.toArray.contains(n))

      if(neighbours.nonEmpty) {
        // Selecting a successor at random from neighbours
        // Performing similarity computations between it and all nodes in original graph and saving it in the array
        val successor = neighbours(Random.nextInt(neighbours.length))
        nodesOriginalGraph.foreach(node => {
          val simMeasure = SimilarityMeasure.findSimilarityNodes(node, successor._2.asInstanceOf[NodeObject])
          comparisons += (s"${node.id} ${successor._2.asInstanceOf[NodeObject].id} - ${simMeasure}")
        })
        // Moving to the successor
        walk += successor
      } else {
        // No more neighbours, so early exit
        return (walk.toList, comparisons.toList)
      }
    })
    (walk.toList, comparisons.toList)
  }
}
