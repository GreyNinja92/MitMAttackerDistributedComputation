import NetGraphAlgebraDefs.NodeObject
import org.mockito.Mockito.{mock, verify, when}
import org.scalatest.PrivateMethodTester
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.Logger
import org.yaml.snakeyaml.Yaml

import java.io.FileWriter
import scala.collection.mutable.ArrayBuffer

class OutputParserTest extends AnyFlatSpec with Matchers with MockitoSugar{
  val node1: NodeObject = NodeObject(id = 1, children = 5, props = 10, propValueRange = 20, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, storedValue = 1)
  val node2: NodeObject = NodeObject(id = 2, children = 5, props = 10, propValueRange = 20, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, storedValue = 1)
  val node3: NodeObject = NodeObject(id = 3, children = 5, props = 10, propValueRange = 20, maxDepth = 5, maxBranchingFactor = 5, maxProperties = 10, storedValue = 1)
  val outputParser = new OutputParser(1, 1, List(node1, node2, node3))
  behavior.of("Testing output parsing")

  it should "read YAML" in {
    val yaml = mock[Yaml]
    val fileWriter = mock[FileWriter]
    outputParser.parseGoldenYAML(NGSConstants.GOLDEN_YAML)

    verify(yaml).load(_: String)
  }

  it should "log scores" in {
    val log = mock[Logger]

    outputParser.computeDifferences()
    verify(log).info(_: String)
  }
}
