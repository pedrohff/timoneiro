
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object wordcount {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("socket source")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    val host = "127.0.0.1"
    val port = "9999"

    val initDF = (
      spark.readStream
        .format("socket")
        .option("host", host)
        .option("port", port)
        .load()
    )

    val wordCount = (
      initDF
        .select(explode(split(col("value"), " ")).alias("word"))
        .groupBy("word")
        .count()
    )

    wordCount
      .writeStream
      .outputMode("complete")
      .option("truncate", value = false)
      .format("console")
      .start()
      .awaitTermination()
  }
}
