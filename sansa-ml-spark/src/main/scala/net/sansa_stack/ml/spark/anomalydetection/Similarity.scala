package net.sansa_stack.ml.spark.anomalydetection

import org.apache.spark.sql.SparkSession
import org.apache.jena.graph.Node

object Similarity {
  def sim(seq1: Seq[Node], seq2: Seq[Node]): Double = {
    
    val sparkSession = SparkSession.builder
      .master("local[*]")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .appName("Jacard Similarity")
      .getOrCreate()
      
    val broadcastSeq1 = sparkSession.sparkContext.broadcast(seq1)
    val broadcastSeq2 = sparkSession.sparkContext.broadcast(seq2)

    val broadcastSet1 = broadcastSeq1.value.toSet
    val broadcastSet2 = broadcastSeq2.value.toSet

    val intersect_cnt = broadcastSet1.intersect(broadcastSet2).size

    val union_count = broadcastSet1.union(broadcastSet2).size
    val jSimilarity = intersect_cnt / (union_count).toDouble
   
    jSimilarity
  }
}
