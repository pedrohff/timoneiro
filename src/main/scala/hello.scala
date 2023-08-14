package main.scala

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._


object hello {
  def main(args: Array[String]): Unit = {


    // Create Spark Session
    val spark = SparkSession
      .builder()
      .master("local")
      .appName("Rate Source")
      .getOrCreate()

    // Set Spark logging level to ERROR to avoid various other logs on console.
    spark.sparkContext.setLogLevel("ERROR")
    val initDF = spark
      .readStream
      .format("rate")
      .option("rowsPerSecond", 1)
      .load()


    println("Streaming DataFrame : " + initDF.isStreaming)
    var resultDF = initDF.withColumn("result", col("value") + lit(1))
    resultDF.writeStream
      .outputMode("append")
      .option("truncate", value = false)
      .format("console")
      .start()
      .awaitTermination()
  }
}
