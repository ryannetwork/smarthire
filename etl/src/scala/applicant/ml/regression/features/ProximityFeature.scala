package applicant.ml.regression.features

import applicant.etl.ApplicantData
import applicant.etl.GeoUtils
import scala.collection.mutable.{ListBuffer, HashMap}

class ProximityFeature(cityFileLoc: String) extends BaseFeature {
  val name: String = "jobLocation"

  val locationMap: HashMap[(String, String), (Double, Double)] = {

    val result = HashMap[(String, String), (Double, Double)]()
    val lines = scala.io.Source.fromFile(cityFileLoc).getLines()

    for (line <- lines) {
      val splitVals = line.toLowerCase().split("#")//#split
      result += ((splitVals(0), splitVals(1)) -> (splitVals(2).toDouble, splitVals(3).toDouble))
    }
    result
  }
  
  /**
   * Will give a double from 0 - 1 based on distance
   *
   * @param meters the distance in meters
   * @return a double from 0 to 1
   *          0 signifies far away
   *          1 signifies close by
   */
  def scaleDistance(meters: Double): Double = {
    //f(x) = 2.0/(1 + 100^(x/4500000))
    val maxDistance = 4500000.0
    return if (meters >= maxDistance) 0.0 else (2.0/(1 + Math.pow(100, meters/4500000)))
  }

  /**
   * Will convert a location string into the city and state
   *
   * @param location The loaction string
   * @return A pair of the city name and state name
   */
  def locationToPair(location: String): (String, String) = {
    val tokens = location.split(",")
    if (tokens.length == 2) {
      val city = tokens(0).trim
      val state = tokens(1).trim

      return (city, state)
    }
    return ("", "")
  }

  /**
   *  Will return a score for the proximity of the applicant to the job location
   *
   * @param applicant The applicant whose feature is checked
   * @param values Any configurable options for the feature
   */
  def getFeatureScore(applicant: ApplicantData, values: ListBuffer[AnyRef]): Double = {
    val location1 = values(0).asInstanceOf[String]
    val location2 = applicant.recentLocation

    //Sanity check
    if (location1 == "" || location2 == "") {
      return 0.0
    }

    val loc1Key = locationToPair(location1.toLowerCase())
    val loc2Key = locationToPair(location2.toLowerCase())

    locationMap.get(loc1Key) match {
      case Some(loc1Coords) =>
        locationMap.get(loc2Key) match {
          case Some(loc2Coords) =>
            val rawResult = GeoUtils.haversineEarthDistance(loc1Coords, loc2Coords)
            return scaleDistance(rawResult)
          case None =>
            return 0.25
        }
      case None =>
        return 0.25
    }
  }
}
