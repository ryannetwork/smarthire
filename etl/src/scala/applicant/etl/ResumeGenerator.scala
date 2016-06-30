package applicant.etl

import applicant.ml.rnn.TextNet

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.Random

import scopt.OptionParser

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font

/**
 * Generates resume(s) from attribute files.
 */
class ResumeGenerator(attributeDir: String, json: String, coefficients: String) {
    // Attribute values
    private val firstNames = readValues(attributeDir + "/firstnames.txt")
    private val lastNames = readValues(attributeDir + "/lastnames.txt")
    private val titles = readValues(attributeDir + "/titles.txt")
    private val organizations = readValues(attributeDir + "/organizations.txt")
    private val locations = readValues(attributeDir + "/locations.txt")
    private val degrees = readValues(attributeDir + "/degrees.txt")
    private val schools = readValues(attributeDir + "/schools.txt")

    // Random index generator
    private val generator = if (json != null) {
        new Random()
    }
    else {
        new Random(1024)
    }

    // Job text generator
    private val textNet: TextNet = if (json != null) {
        new TextNet(json, coefficients)
    }
    else {
        null
    }

    /**
     * Generates a resume as a sequence of Strings (lines).
     *
     * @return Seq[String]
     */
    def generate(): Seq[String] = {
        val lines = new ListBuffer[String]()

        // Generate name
        lines += getValue(firstNames) + " " + getValue(lastNames)
        lines += "\n"

        // Generate work experience
        lines += "WORK EXPERIENCE"
        for (x <- 0 until generator.nextInt(5) + 1) {
            val title = getValue(titles)

            lines += title
            lines += getValue(organizations) + " - " + getValue(locations)

            // Generate position text if a textnet is present
            if (textNet != null) {
                var text = textNet.getText(title.toUpperCase(), 300, 1)(0)
                lines += text.replaceAll("(?m)[A-Z\\s]{10,}\\s+?", "")
            }

            lines += "\n"
        }

        lines += "EDUCATION"
        lines += getValue(degrees)
        lines += getValue(schools)
        lines += "\n"

        return lines
    }

    /**
     * Gets a random value out of an Attribute sequence.
     *
     * @param values Seq[String] of all attribute values
     * @return String
     */
    private def getValue(values: Seq[String]): String = {
        return values(generator.nextInt(values.length))
    }

    /**
     * Reads all attributes in from a file. Each attribute is on a single line.
     *
     * @param file input file
     * @return Seq[String]
     */
    private def readValues(file: String): Seq[String] = {
        return Source.fromFile(file).getLines.toSeq
    }
}

/** 
  * Generates a PDF file that contains the strings from the generated resume
  */

class PDFGenerator() {
    // Create a new empty document
    val document = new PDDocument()
    var nameGet = false
    var name = "" 

    // Create a new blank page and add it to the document
    val page = new PDPage()
    document.addPage(page)

    // Create a new font object selecting one of the PDF base fonts
    val font = PDType1Font.HELVETICA

    // Start a new content stream which will "hold" the to be created content
    val contentStream = new PDPageContentStream(document, page)

    contentStream.beginText();
    contentStream.setFont( font, 12 )

    //sets offset for each new line in PDF
    contentStream.setLeading(14.5f)

    //initially moves text to upper left of page
    contentStream.moveTextPositionByAmount( 50, 750 )

    /**
      * adds a line to the PDF document. each string has been trimmed to remove space characters, as 
      * PDFBox does not take their encoding. also checks to see if a string is longer than 80 characters, 
      * which then separates the string into substrings to avoid running off the page
      *
      * @param trimLine - string to add to PDF
      */

    def addLine(trimLine: String) : Unit = {
        if (trimLine.length() > 80) {
                                                   
            var count = 0
            var index = 0
            var track = 80

            while (count < trimLine.length()) {
                count = trimLine.indexOf(' ', track)
                contentStream.showText(trimLine.substring(index, count))
                contentStream.newLine()
                index = count
                count += 80
                track += 80

            }
            contentStream.showText(trimLine.substring((index + 1), trimLine.length()))
            contentStream.newLine()
        }
        
        else {
            contentStream.showText(trimLine)
            contentStream.newLine()
        }


    }

    /**
      * closes the stream to PDF and saves it to the desired directory
      *
      * @param name - first string of the PDF doc (the name of the applicant), used for naming the PDF file
      */

    def end(name: String) : Unit = {
        contentStream.endText();

        // Make sure that the content stream is closed:
        contentStream.close();
         // Save the newly created document
        document.save("data/generatedresumes/" + name + ".pdf")

        // finally make sure that the document is properly
        // closed.
        document.close()

    }
}

object ResumeGenerator {
    // Command line arguments
    case class Command(attributeDir: String = "", json: String = "", coefficients: String = "")

    /**
     * Main method
     */
    def main(args: Array[String]) {

        var nameGet = false
        var name = "" 
        val pdfGenerator = new PDFGenerator()
    
        val parser = new OptionParser[Command]("ResumeGenerator") {
            opt[String]('a', "attributeDir") required() valueName("<attributes dir>") action { (x, c) =>
                c.copy(attributeDir = x)
            } text("Attribute Directory")
            opt[String]('j', "json") required() valueName("<json>") action { (x, c) =>
                c.copy(json = x)
            } text("RNN Model JSON Configuration")
            opt[String]('c', "coefficients") required() valueName("<coefficients>") action { (x, c) =>
                c.copy(coefficients = x)
            } text("RNN Model Coefficients")

            note("Generates a Random Resume")
            help("help") text("Prints this usage text")
        }

        // Parses command line arguments and passes them to the search
        parser.parse(args, Command()) match {
            case Some(options) =>
                val generator = new ResumeGenerator(options.attributeDir, options.json, options.coefficients)
                generator.generate().foreach { line =>
                    if (line.endsWith("\n")) {
                        print(line)
                        val trimLine = line.replace('\n', ' ')
                        pdfGenerator.addLine(trimLine)
                    }
                    else {
                        println(line)
                        val trimLine2 = line.replace('\n', ' ')
                        pdfGenerator.addLine(trimLine2)

                        if (nameGet == false) {
                            name = trimLine2
                            nameGet = true 
                        }
                    }
                }
            case None =>
        }

        pdfGenerator.end(name)

    }
}
