package net.sansa_stack.ml.spark.amieSpark.mining

import net.sansa_stack.ml.spark.amieSpark.mining._


import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql._
import org.apache.spark.sql.types._

import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer
import KBObject.KB
import scala.collection.mutable.Map



object Rules {
  
  
  class RuleContainer () extends Serializable{
        val name: String =""
       
        var rule: ArrayBuffer[RDFTriple] = new ArrayBuffer
        
        var headKey = null;
        var support: Long = -1;
        var hashSupport = 0;
        var parent = null;
        var bodySize = -1.0;
        var highestVariable:Char = 'b';
        var pcaBodySize = 0.0;
        var _stdConfidenceUpperBound = 0.0;
        var _pcaConfidenceUpperBound = 0.0;
        var _pcaConfidenceEstimation = 0.0;
        var _belowMinimumSupport = false;
        var _containsQuasiBindings = false;
        var ancestors = new ArrayBuffer[(String,String,String)]()
        var highestVariableSuffix = 0; 
        var variableList:ArrayBuffer[String] = ArrayBuffer("?a","?b")
        var sizeHead: Double = 0.0
        
         
        
        def getRule(): ArrayBuffer[RDFTriple]={
          return this.rule
        }
        
        /**initializes rule, support, bodySize and sizeHead*/
        def initRule(x: ArrayBuffer[RDFTriple], k: KB, sc: SparkContext, sqlContext:SQLContext){
          this.rule = x
          calcSupport(k,sc,sqlContext)
          setBodySize(k,sc,sqlContext)
          setSizeHead(k)
           
         
        }
        
        def setRule(supp: Long, x: ArrayBuffer[RDFTriple], k: KB, sc: SparkContext, sqlContext:SQLContext){
          this.rule = x
          this.support = supp
          setBodySize(k,sc,sqlContext)
          setSizeHead(k)
        }
        
       
        
        
        
        /**returns ArrayBuffer with every triplePattern of the body as a RDFTriple*/
       
        
        def hc():Double={
          if (this.bodySize < 1){
            return -1.0
            
          }
          
          
          return ((this.support)/(this.sizeHead))
          
          
          
        }
         
      
         
         
         
       /**returns number of instantiations of the predicate of the head  
         * 
         * @param k knowledge base
         * 
         */
         def setSizeHead(k:KB){
          val headElem = this.rule(0)
           
          var rel = headElem.predicate
            
           var out = k.sizeOfRelation(rel)
           
           this.sizeHead = out
         }
         
       /**returns number of instantiations of the relations in the body  
         * 
         * @param k knowledge base
         * @param sc spark context 
         * 
         */
         
         def setBodySize(k: KB,sc: SparkContext, sqlContext: SQLContext){
           if ((this.rule.length-1) > 1){
           var body = this.rule.clone
           body.remove(0)
           var mapList =k.cardinality(body,sc,sqlContext)
           
           
           this.bodySize = mapList.count()
           }
           
           }
         
        
       /**returns the support of a rule, the number of instantiations of the rule 
         * 
         * @param k knowledge base
         * @param sc spark context 
         * 
         */
         
         def calcSupport(k: KB, sc:SparkContext, sqlContext: SQLContext){
           
           if (this.rule.length > 1){
          
             val mapList = k.cardinality(this.rule,sc,sqlContext)
           
           this.support = mapList.count()
           }
   }
      /**returns the length of the body*/   
  
     def bodyLength():Int={
       var x = this.rule.length -1
       return x
       
     }   
     
     def usePcaApprox(tparr: ArrayBuffer[RDFTriple]): Boolean = {
       
        var maptp: Map[String, Int] = Map()
        if (tparr.length <= 2){
          return false
        }
        
        for (x <- tparr){
          
                     if (!(maptp.contains(x._1))){ 
                       maptp += (x._1 -> 1)
                     }
                     else{
                       var counter:Int = maptp.remove(x._1).get + 1
                       if (counter > 2){return false}
       
                       maptp += (x._1 ->counter)
                     }
                  
                   /**checking map for placeholder for the object*/
                     if (!(maptp.contains(x._3))){
                       maptp += (x._3 -> 1)
                     }
                     else {
                       var counter:Int = maptp.remove(x._3).get + 1
                      if (counter > 2){return false}
       
                       maptp += (x._3 ->counter)
                     }
       
     }
     
       maptp.foreach{value => if (value._2 == 1) return false}
       return true
       
     }
     
     def getPcaConfidence(k: KB, sc:SparkContext, sqlContext:SQLContext): Double={
       
       var tparr = this.rule.clone
       val usePcaA = usePcaApprox(tparr)
        
       /**when it is expensive to compute pca approximate pca
        * */
       if (usePcaA){
           
         set_pcaConfidenceEstimation(k: KB)
         return this._pcaConfidenceEstimation
         
       }
       else{
       
         this.pcaBodySize = k.cardPlusnegativeExamplesLength(tparr,this.support,sc,sqlContext)
         
         return (support/pcaBodySize)
         
       }
       
       
       
     }
     
     def setPcaBodySize(k: KB, sc:SparkContext){
       val tparr = this.rule
       
       
       val out = k.cardPlusnegativeExamplesLength(tparr,sc)
      
       
       
       
       
      
       
       
       this.pcaBodySize = out
       
  
     }
         
       def set_pcaConfidenceEstimation(k: KB){
         var r = this.rule.clone
         var r0 = (k.overlap(r(1).predicate,r(0).predicate,0))/(k.functionality(r(1).predicate))
         var rrest: Double = 1.0
         
         for (i <- 2 to r.length-1 ){
           rrest= rrest*(((k.overlap(r(i-1).predicate, r(i).predicate, 2))/(k.getRngSize(r(i-1).predicate)))*((k.inverseFunctionality(r(i).predicate))/(k.functionality(r(i).predicate))))
           
           
         }
         
         this._pcaConfidenceEstimation = r0 * rrest
         
       }
         
    def parentsOfRule(outMap: Map[String, ArrayBuffer[(ArrayBuffer[RDFTriple], RuleContainer)]], sc: SparkContext):ArrayBuffer[RuleContainer] =  {
      // TODO: create new rules with body in alphabetical order     
      var parents = new ArrayBuffer[RuleContainer]
           val r = this.rule.clone
           
               
           
           if (outMap.get(r(0).predicate) == None){return parents}
           var rel = outMap.get(r(0).predicate).get
           
           var tp:ArrayBuffer[RDFTriple] =new ArrayBuffer
          
         
             var filtered = rel.filter(x => (x._1.length == r.length-1)) 
          
             
               
               for (l <- 1 to r.length-1){
                 if (!(filtered.isEmpty)){
                  
                   tp = r.clone()
                   tp.remove(l)
                   var x =partParents(tp, filtered, sc)
                   parents ++= x._1
                   filtered = x._2
                 }
               }
               
             
           
           return parents
           
       }
       
       def closed():Boolean = {
         
         var tparr = this.rule
         var maptp: Map[String, Int] = Map()
        if (tparr.length == 1 ){
          return false
        }
        
         
    
        for (x <- tparr){
          
                     if (!(maptp.contains(x._1))){
                       
                       maptp += (x._1 -> 1)
                     }
                     else{
                       
                       
                       maptp.put(x._1, (maptp.get(x._1).get + 1)).get
                     }
                  
                   /**checking map for placeholder for the object*/
                     if (!(maptp.contains(x._3))){
                                            
                       maptp += (x._3 -> 1)
                     }
                     else {
                       maptp.put(x._3, (maptp.get(x._3).get + 1)).get
                     }
       
     }
     
       maptp.foreach{value => if (value._2 == 1) return false}
       
       return true
       
       
       
         
       }
       
              
       def notClosed(): Option[ArrayBuffer[String]] = {
         var maxVar:Char = 'a'
         var varArBuff = new ArrayBuffer[String]
         
         var tparr = this.rule
         var maptp: Map[String, Int] = Map()
        if (tparr.length == 1 ){
          
          return Some(ArrayBuffer(tparr(0)._1,tparr(0)._3))
        }
        
         
    
        for (x <- tparr){
          
                     if (!(maptp.contains(x._1))){
                       varArBuff += x._1
                       if (x._1(1)>maxVar){
                         maxVar = x._1(1)
                       }
                       maptp += (x._1 -> 1)
                     }
                     else{
                       
                       
                       maptp.put(x._1, (maptp.get(x._1).get + 1)).get
                     }
                  
                   /**checking map for placeholder for the object*/
                     if (!(maptp.contains(x._3))){
                       varArBuff += x._3
                       
                       if (x._3(1)>maxVar){
                         maxVar = x._3(1)
                       }                       
                       maptp += (x._3 -> 1)
                     }
                     else {
                       maptp.put(x._3, (maptp.get(x._3).get + 1)).get
                     }
       
     }
       var out:ArrayBuffer[String] = new ArrayBuffer
       this.variableList = varArBuff
       maptp.foreach{value => if (value._2 == 1) out += value._1}
       
       this.highestVariable = maxVar
       if (out.isEmpty){
         return None
       }
       else{
         return Some(out)
       }
      
       
       
       
         
       }
       
       def getHighestVariable(): Char = {
         return this.highestVariable
       }
       
       def getVariableList(): ArrayBuffer[String] = {
         return this.variableList
       }
        
     
     
       
       def partParents(triples: ArrayBuffer[RDFTriple],arbuf: ArrayBuffer[(ArrayBuffer[RDFTriple], RuleContainer)], sc:SparkContext): (ArrayBuffer[RuleContainer], ArrayBuffer[(ArrayBuffer[RDFTriple], RuleContainer)])={
         var out1:ArrayBuffer[RuleContainer] = new ArrayBuffer
         var out2: ArrayBuffer[(ArrayBuffer[RDFTriple], RuleContainer)] = new ArrayBuffer 
         
         var out: (ArrayBuffer[RuleContainer], ArrayBuffer[(ArrayBuffer[RDFTriple], RuleContainer)]) = (out1, out2)
         if (triples.length <= 1){
           return out
         }
         var triplesCardcombis:ArrayBuffer[ArrayBuffer[RDFTriple]] = new ArrayBuffer
         
         
           //var rdd =sc.parallelize(arbuf.toSeq)
          //out ++= rdd.filter(x => (sameRule(triples, x._1))).map(y => y._2).collect
         
         for (x <-arbuf){
           if (sameRule(triples, x._1)){
             out1 += x._2 
             
           }
           else out2 += x
         }
        
        /* var rdd = sc.parallelize(arbuf.toSeq)
          var pq = rdd.map{ x=>
           if (sameRule(triples, x._1)){
             ("out1", x._2) 
             
           }
           else ("out2",x)
         }.groupByKey()
         * 
         */
         
         out = (out1, out2)
         return out
         
         
       }
      

       def rdTpEquals(a: RDFTriple, b: RDFTriple):Boolean={
          if ((a._1 == b._1)&&(a._2 == b._2)&&(a._3 == b._3)){
             return true
             
           }
          else {
            return false
          }
       }
       
       def rdfTpArrEquals(a: ArrayBuffer[RDFTriple], b: ArrayBuffer[RDFTriple]):Boolean={
         if (!( a.length ==  b.length)){
           return false
         }
         
         for(i <- 0 to a.length-1){
           if (!(rdTpEquals(a(i),b(i)))){
             return false
             
           }
         }
         return true
       }
 
       
      
       
         
         def sameRule(a: ArrayBuffer[RDFTriple], b: ArrayBuffer[RDFTriple]):Boolean={
           var varMap: Map[String,String] = Map()
           if(a.length != b.length){return false}
             for(i <- 0 to a.length-1){
               
               if (a(i)._2 != b(i)._2){return false}
               if (!(varMap.contains(a(i)._1))){ 
                       varMap += (a(i)._1 -> b(i)._1)
                     }
                  
                   if (!(varMap.contains(a(i)._3))){ 
                       varMap += (a(i)._3 -> b(i)._3)
                     }
                   
                    
                      if (!((varMap.get(a(i)._1)==Some(b(i)._1)) && (varMap.get(a(i)._3)==Some(b(i)._3)))){
                        return false 
                      } 
             
               
             }
           
           
           
           
           return true
         }
         
      
         
          def samePartialRule(x: ArrayBuffer[RDFTriple], y: ArrayBuffer[RDFTriple]):Boolean={
           var varMap: Map[String,String] = Map()
           
           var a:ArrayBuffer[RDFTriple] = new ArrayBuffer 
         var b:ArrayBuffer[RDFTriple] = new ArrayBuffer 
         
           if ( x.length <=  y.length){
            a = x
            b = y
         }
         else{
           a = y
           b = x
           
         }
           
           
             for(i <- 0 to a.length-1){
               if (a(i)._2 != b(i)._2){return false}
               
               if (!(varMap.contains(a(i)._1))){ 
                       varMap += (a(i)._1 -> b(i)._1)
                     }
                  
                   if (!(varMap.contains(a(i)._3))){ 
                       varMap += (a(i)._3 -> b(i)._3)
                     }
                   
                    
                      if (!((varMap.get(a(i)._1)==Some(b(i)._1)) && (varMap.get(a(i)._3)==Some(b(i)._3)))){
                        return false 
                      } 
             
               
             
           }
           
           
           
           
           return true
         }     
         
         
        
     
       //end
        
  }
  
 
  
}