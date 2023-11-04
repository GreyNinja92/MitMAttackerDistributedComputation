import com.typesafe.config.{Config, ConfigFactory}
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import scala.util.Failure
import org.slf4j.{Logger, LoggerFactory}

// Constants file to store all strings used for finding files, creating graphs, etc.
object NGSConstants {
  private val config: Config = ConfigFactory.load()
  val logger: Logger = LoggerFactory.getLogger(NGSConstants.getClass)
  val SPARK_JOB_NAME: String = "Random Walks"

  val RUNNING_LOCALLY: String = "local[4]"

  val ORIGINAL_GRAPH: String = if(config.getString("ORIGINAL_GRAPH") != null) config.getString("ORIGINAL_GRAPH") else "original.json"
  val PERTURBED_GRAPH: String = if(config.getString("PERTURBED_GRAPH") != null) config.getString("PERTURBED_GRAPH") else "perturbed.json"
  val GOLDEN_YAML: String = if(config.getString("GOLDEN_YAML") != null) config.getString("GOLDEN_YAML") else "difference.yaml"

  val AWS: String = if(config.getString("AWS") != null) config.getString("AWS") else "s3://cc-p2/"

  val DEFAULT: String = "default"

  val NEW_LINE: String = "\n"
  val TAB: String = "\t"

  val NODES: String = "Nodes"

  val ADDED: String = "Added"
  val MODIFIED: String = "Modified"
  val REMOVED: String = "Removed"

  val outputDirectory: String = {
    val defDir = new java.io.File(".").getCanonicalPath
    logger.info(s"Default output directory: $defDir")
    defDir + "/"
  }
}




