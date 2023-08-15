package sinks
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{Column, Dataset, Row, SparkSession}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}

object ForEachBatch {

  def getFileName: Column = {
    val file_name = reverse(split(input_file_name(), "/")).getItem(0)
    split(file_name, "_").getItem(0)
  }

  def saveToMySql = (df: Dataset[Row], batchId: Long) => {
    val url = """jdbc:mysql://localhost:3306/training"""


    df.withColumn("batchId", lit(batchId))
      .write.format("jdbc")
      .option("url", url)
      .option("user", "root")
      .option("password", "mysqlpass")
      .option("dbtable", "training")
      .mode("append")
      .save()
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

    val resultDf = initDF.select("Name", "Date", "Open")

    resultDf
      .writeStream
      .outputMode("append")
      .foreachBatch(saveToMySql)
      .start()
      .awaitTermination()
  }
}
