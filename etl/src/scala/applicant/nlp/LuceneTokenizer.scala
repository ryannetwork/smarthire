package applicant.nlp

import java.io.StringReader
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import scala.collection.mutable.ListBuffer

/**
 * Tokenizes text using Lucene Analyzers.
 */
class LuceneTokenizer(analyzer: String = null) {
    /**
     * Tokenizes a String.
     *
     * @param string input string
     * @return Seq[String]
     */
    def tokenize(string: String): Seq[String] = {
        var result = new ListBuffer[String]()
        var stream = getAnalyzer().tokenStream(null, new StringReader(string))
        stream.reset()
        while (stream.incrementToken()) {
            result += stream.getAttribute(classOf[CharTermAttribute]).toString()
        }

        return result
    }

    /**
     * Sets the analyzer to english if the string "english" is used in the constructor
     */
    private def getAnalyzer(): Analyzer = {
        return if (analyzer == "english") new EnglishAnalyzer() else new StandardAnalyzer()
    }
}

/**
 * Some utilities surrounding Lucene Tokenizing
 */
object LuceneTokenizer {
  /**
   * Tokenizes the string as english words and returns and Iterator
   *  that groups the tokens in sections of 10
   *
   * @param tokenString The string to be tokenized
   * @return The grouped iterator of tokens
   */
  def getTokens(tokenString: String): Iterator[Seq[String]] = {
    return tokenize(tokenString).grouped(10)
  }

  /**
   * Will tokenize a string as english words
   *
   * @param tokenString The string to be tokenized
   * @return A sequence of tokens
   */
  def tokenize(tokenString: String): Seq[String] = {
    val tokenizer = new LuceneTokenizer("english")
    return tokenizer.tokenize(tokenString).filter { term =>
        term.length() > 2 && !term.matches("\\d+")
    }
  }
}
