package operations

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Column, SparkSession}

object udfUsage {

  def getFileName: Column = {
    val file_name = reverse(split(input_file_name(), "/")).getItem(0)
    split(file_name, "_").getItem(0)
  }


  def up: (Double, Double) => String = (close: Double, open: Double) => {
    if ((close - open) > 0)
      "UP"
    else
      "Down"
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


    val upUdf = udf(up)

    val resultDF = initDF.withColumn("up_down_udf", upUdf($"Close", $"Open"))

    resultDF
      .writeStream
      .outputMode("append")
      .format("console")
      .start().awaitTermination()

  }
}
