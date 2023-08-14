import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, SparkSession}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}

object filessink {

  def getFileName: Column = {
    val file_name = reverse(split(input_file_name(), "/")).getItem(0)
    split(file_name, "_").getItem(0)
  }

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("files source")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    val schema = StructType(List(
      StructField("Date", StringType, true),
      StructField("Open", DoubleType, true),
      StructField("High", DoubleType, true),
      StructField("Low", DoubleType, true),
      StructField("Close", DoubleType, true),
      StructField("Adjusted Close", DoubleType, true),
      StructField("Volume", DoubleType, true),
    ))

    val initDF = (
      spark.readStream
        .format("csv")
        .option("maxFilesPerTrigger", 2)
        .option("header", true)
        .option("path", "./resources/data/stream")
        .schema(schema)
        .load()
        .withColumn("Name", getFileName)
    )

    val stockDf = initDF
      .groupBy(col("Name"), year(col("Date")).as("Year"))
      .agg(max("High").as("Max"))

    stockDf.writeStream
      .outputMode("complete")
      .option("truncate", false)
      .option("numRows", 3)
      .format("console")
      .start()
      .awaitTermination()

  }
}
