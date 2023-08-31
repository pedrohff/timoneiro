package operations
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.Column
import org.apache.spark.sql.types._
object groupby {

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

    val companyDF = spark.read.option("header", true)
      .csv("./resources/data/company.csv")


    val resultDF = initDF.groupBy($"Name", year($"Date").as("Year"))
      .agg(max("High").as("Max"))

    val joinDF = resultDF.join(companyDF, Seq("Name"), "inner")

    joinDF
      .writeStream
      .outputMode("complete")
      .format("console")
      .start().awaitTermination()

  }
}
