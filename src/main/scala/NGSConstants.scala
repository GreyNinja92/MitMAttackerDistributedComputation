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

  val ORIGINAL_GRAPH: String = "original.json"
  val PERTURBED_GRAPH: String = "perturbed.json"
  val GOLDEN_YAML: String = "difference.yaml"

  val AWS: String = "s3://cc-p2/"

  val DEFAULT: String = "default"

  val NEW_LINE: String = "\n"
  val TAB: String = "\t"

  val NODES: String = "Nodes"
  val EDGES: String = "Edges"

  val ADDED: String = "Added"
  val MODIFIED: String = "Modified"
  val REMOVED: String = "Removed"
  val PERTURBED = ".perturbed"
  val OUTPUTDIRECTORY = "output"

  val outputDirectory: String = {
    val defDir = new java.io.File(".").getCanonicalPath
    logger.info(s"Default output directory: $defDir")
    val dir: String = getConfigEntry(config, NGSConstants.OUTPUTDIRECTORY, defDir)
    val dir: String = defDir
    val ref = new File(dir)
    if(ref.exists() && ref.isDirectory) {
      logger.info(s"Using output directory: $dir")
      if(dir.endsWith("/")) dir else dir + "/"
    }
    else {
      logger.error(s"Output directory $dir does not exist or is not a directory, using current directory instead: $defDir")
      defDir + "/"
    }
  }
}




