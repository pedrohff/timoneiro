package operations

import org.apache.spark.sql.{Column, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object streamjoins {

  def getFileName: Column = {
    val file_name = reverse(split(input_file_name(), "/")).getItem(0)
    split(file_name, "_").getItem(0)
  }

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("File Source")
      .getOrCreate()

    import spark.implicits._

    // Set Spark logging level to ERROR.
    spark.sparkContext.setLogLevel("ERROR")

    val schema = StructType(List(
      StructField("Date", StringType, true),
      StructField("Open", DoubleType, true),
      StructField("High", DoubleType, true),
      StructField("Low", DoubleType, true),
      StructField("Close", DoubleType, true),
      StructField("Adjusted Close", DoubleType, true),
      StructField("Volume", DoubleType, true)
    ))

    // Create Streaming DataFrame by reading data from socket.
    val initDF = (spark
      .readStream
      .option("maxFilesPerTrigger", 2)
      .option("header", true)
      .schema(schema)
      .csv("./resources/data/stream")
      .withColumn("Name", getFileName)
      )

    val streamDf1 = initDF.select("Name", "Date", "High", "Low")
    val streamDf2 = initDF.select("Name", "Date", "Open", "Close")

    val joinDf = streamDf1.join(streamDf2, Seq("Name", "Date"))
    joinDf
      .writeStream
      .outputMode("append")
      .format("console")
      .start().awaitTermination()

  }
}
