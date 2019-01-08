package net.sansa_stack.ml.common.outliers.vandalismdetection.feature

import java.util.regex.{ Pattern
import org port org.apache.spark.sql._

object FacilitiesClass extends Serializable {

  def cleaner(str: String): String = {
    val cleaned_value1 = str.replace("{", "").trim()
    val cleaned_value2 = str.replace("}", "").trim()
    val cleaned_value3 = cleaned_value2.replace("\"", "");
    cleaned_value3
  }
  def splitBycomma(str: String): Array[String] = {
    val namesList: Array[String] = str.split(",")
    namesList
  }

  // ok --- Used for DF Triples
  def RDD_TO_DFR_RDFXML(rdd: RDD[String], sqlContext: org.apache.spark.sql.SQLContext): DataFrame = {
    // Create an Encoded Schema in a String Format:
    val schemaString = "Subject Predicate Object"
    // Generate schema:
    val schema = StructType(schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, true)))
    // Apply Transformation for Reading Data from Text File
    val rowRDD = rdd.map(_.split(" ")).map(e => Row(e(0), e(1), e(2)))
    // Apply RowRDD in Row Data based on Schema:
    val RDFTRIPLE = sqlContext.createDataFrame(rowRDD, schema)
    // Store DataFrame Data into Table
    RDFTRIPLE.registerTempTable("SPO")
    // Select Query on DataFrame
    val dfr = sqlContext.sql("SELECT * FROM SPO")
    dfr.show()

    dfr
  }

  // ok --- Used for DF Triples
  def RDD_TO_DFR_TRIX(rdd: RDD[String], sqlContext: org.apache.spark.sql.SQLContext): DataFrame = {
    // Create an Encoded Schema in a String Format:
    val schemaString = "Subject Predicate Object"
    // Generate schema:
    val schema = StructType(schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, true)))
    // Apply Transformation for Reading Data from Text File
    val rowRDD = rdd.map(_.split("><")).map(e => Row(e(0), e(1), e(2)))
    // Apply RowRDD in Row Data based on Schema:
    val RDFTRIPLE = sqlContext.createDataFrame(rowRDD, schema)
    // Store DataFrame Data into Table
    RDFTRIPLE.registerTempTable("SPO")
    // Select Query on DataFrame
    val dfr = sqlContext.sql("SELECT * FROM SPO")
    dfr.show()

    dfr
  }

  // ok --- Used for DF Triples
  def RDD_TO_DFR_JTriple(rdd: RDD[String], sqlContext: org.apache.spark.sql.SQLContext): DataFrame = {
    // Create an Encoded Schema in a String Format:
    val schemaString = "Subject Predicate Object"
    // Generate schema:
    val schema = StructType(schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, true)))
    // Apply Transformation for Reading Data from Text File
    val rowRDD = rdd.map(_.split(",")).map(e => Row(e(0), e(1), e(2)))
    // Apply RowRDD in Row Data based on Schema:
    val RDFTRIPLE = sqlContext.createDataFrame(rowRDD, schema)
    // Store DataFrame Data into Table
    RDFTRIPLE.registerTempTable("SPO")
    // Select Query on DataFrame
    val dfr = sqlContext.sql("SELECT * FROM SPO")
    dfr.show()

    dfr
  }

  def roundDouble(va: Double): Double =
    Math.round(va * 10000).toDouble / 10000

  def stringToInt(str: String): Integer =
    str.toInt

  def arrayString2VectorDouble(str: String): Vector = {

    val vector: Vector = Vectors.zeros(0)
    val str_recordList: Array[String] = str.split(",")
    val size: Integer = str_recordList.size
    var double_recordList: Array[Double] = new Array[Double](size)

    // val x= str_recordList.toVector

    for (record <- str_recordList) {

      if (record.nonEmpty) {

        val tem0: String = record.replace("[", "").trim()
        val tem1: String = tem0.replace("]", "").trim()

        val tem2 = tem1.toDouble
        double_recordList +:= tem2

      }

    }

    toVector(double_recordList)

  }

  def toVector(arra: Array[Double]): Vector =
    Vectors.dense(arra)

  def array2String(arra: Array[Double]): String = {

    var tem = ""

    for (i <- 0 until arra.length) {

      if (i == 0) {
        tem = arra(i).toString().trim()

      } else {
        tem = tem + "," + arra(i).toString()

      }

    }
    tem.trim()
  }

  // Character Features: ------ start calculation the Ratio for character:
  def characterRatio(str: String, pattern: Pattern): Double = {
    var charRatio: Double = -1.0;
    if (str != null) {
      val tem: String = pattern.matcher(str).replaceAll("")
      val digits: Double = str.length() - tem.length()
      charRatio = digits / str.length().toDouble
    }
    charRatio
  }

  def extractCharacterRatio(str: String, patternStr: String): Double = {
    val pattern: Pattern = Pattern.compile(patternStr)
    val result: Double = characterRatio(str, pattern)
    result
  }
}
