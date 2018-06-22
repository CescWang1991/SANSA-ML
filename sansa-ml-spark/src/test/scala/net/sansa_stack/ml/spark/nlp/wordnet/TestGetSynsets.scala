package net.sansa_stack.ml.spark.nlp.wordnet

import com.holdenkarau.spark.testing.DataFrameSuiteBase
import org.scalatest.FunSuite
import net.sf.extjwnl.data._

class TestGetSynsets  extends FunSuite with DataFrameSuiteBase{

  test("If The WordNEt dictionary is correctly installed synsets must not be null ") {

    val wn = new WordNet

    // getting a synset by a word and index

    val thing1 = wn.getSynset("thing", POS.NOUN, 1)

    // getting a list of synsets by a word

    val thing2 = wn.getSynsets("thing", POS.NOUN).head

    assert(thing1 != null)
    assert(thing2 != null)
  }
}
