package operations

import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Column, SparkSession}

object windowOp {
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

    val sliding = true

    if (sliding) {
      val resultDF = initDF.select("Name", "Date", "Open", "High", "Low")
        .groupBy(window($"Date", "10 days", "5 days"), $"Name", year($"Date").as("Year"))
        .agg(max("High").as("Max"))
        .orderBy($"window.start")
        .select("window.start", "window.end", "Name", "Max")

      resultDF.writeStream
        .outputMode("complete")
        .option("truncate", false)
        .format("console")
        .start()
        .awaitTermination()

    } else {
      val resultDF = initDF.select("Name", "Date", "Open", "High", "Low")
        .groupBy(window($"Date", "10 days"), $"Name")
        .agg(max("High").as("Max"))
        .orderBy($"window.start")
        .select("window.start", "window.end", "Name", "Max")

      resultDF.writeStream
        .outputMode("complete")
        .option("truncate", false)
        .format("console")
        .start()
        .awaitTermination()

    }

  }
}
