
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object wordcountkafka {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("socket source")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    val initDF =
      spark.readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", "localhost:9092")
        .option("subscribe", "test")
        .option("startingOffsets", "earliest")
        .load()
        .select(col("value").cast("string"))

    val wordCount = (
      initDF
        .select(explode(split(col("value"), " ")).alias("word"))
        .groupBy("word")
        .count()
    )

    wordCount
      .writeStream
      .outputMode("update")
      .option("truncate", value = false)
      .format("console")
      .start()
      .awaitTermination()
  }
}
