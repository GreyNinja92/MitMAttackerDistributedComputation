import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SimilarityMeasureTest extends AnyFlatSpec with Matchers {
  behavior.of("Testing similarity between nodes")

  it should "output similarity 1.0 for identical nodes" in {
    val node = NetGraphAlgebraDefs.NodeObject(1, 2, 2, 2, 3, 3, 4, 5, 5.6)
    SimilarityMeasure.findSimilarityNodes(node, node) shouldBe 1.0f
  }

  it should "output similarity 0.5 for similar nodes with different storedValues" in {
    val node1 = NetGraphAlgebraDefs.NodeObject(1, 2, 2, 2, 3, 3, 4, 5, 5.6)
    val node2 = NetGraphAlgebraDefs.NodeObject(1, 2, 2, 2, 3, 3, 4, 5, 7.5)
    SimilarityMeasure.findSimilarityNodes(node1, node2) shouldBe 0.5f
  }

  it should "output similarity 0.75 for similar nodes with different children" in {
    val node1 = NetGraphAlgebraDefs.NodeObject(1, 2, 2, 2, 3, 3, 4, 5, 5.6)
    val node2 = NetGraphAlgebraDefs.NodeObject(1, 3, 2, 2, 3, 3, 4, 5, 5.6)
    SimilarityMeasure.findSimilarityNodes(node1, node2) shouldBe 0.75f
  }

  it should "output similarity 0.75 for similar nodes with different propValueRange" in {
    val node1 = NetGraphAlgebraDefs.NodeObject(1, 2, 2, 2, 3, 3, 4, 5, 5.6)
    val node2 = NetGraphAlgebraDefs.NodeObject(1, 2, 2, 2, 4, 3, 4, 5, 5.6)
    SimilarityMeasure.findSimilarityNodes(node1, node2) shouldBe 0.75f
  }

}
