import NetGraphAlgebraDefs.NodeObject

object SimilarityMeasure {
  def findSimilarityNodes(node1: NodeObject, node2: NodeObject): Float = {
    val equalNoOfChildren: Int = if (node1.children == node2.children) 1 else 0
    val equalStoredValue: Int = if (node1.storedValue == node2.storedValue) 1 else 0
    val equalPropValueRange: Int = if (node1.propValueRange == node2.propValueRange) 1 else 0

    val simMeasure = equalNoOfChildren * 0.25f + equalStoredValue * 0.5f + equalPropValueRange * 0.25f
    simMeasure
  }
}
