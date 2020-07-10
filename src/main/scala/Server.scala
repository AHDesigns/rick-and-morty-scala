package server

import scala.util.{Left,Either,Right}
import spray.json._
import DefaultJsonProtocol._


object Server extends App {
  import RickAndMortyService._

  val characterOfInterest = "1"

  getCharacter(characterOfInterest)
    .map(_.episode.map(_.split("episode/").last))
    .flatMap(getEpisodes)
    .map(getUniqCharacterIdsIgnoring(characterOfInterest))
    .flatMap(getCharacters)
    .map(_.map(_.name))
    .map(println)

  /***** Helpers *****/

  def getUniqCharacterIdsIgnoring(ignoredCharacterId: String)(episodes: List[Episode]) = 
    episodes
      .flatMap(_.characters.map(_.split("character/").last))
      .toSet
      .toList
      .filter(_ != ignoredCharacterId)
}

object Helpers {
  type Params = Option[Map[String, String]]
  val cache = collection.mutable.Map.empty[(String, Params), Either[String, Any]]

  import scalaj.http._

  def get[R: JsonReader](url: String, params: Params = None): Either[String, R] = {
    val res = cache.getOrElseUpdate((url, params), {
      try {
        val response = params match {
          case None => Http(url).asString
          case Some(p) => Http(url).params(p).asString
        }

        response.code match {
          case 200 => try {
            Right(response.body.parseJson.convertTo[R])
          } catch {
            case e: spray.json.DeserializationException => Left(
              s"""DeserializationException for:\n${url} ${params}\ngot ${response.body}\n$e"""
            )
            case e: Throwable => throw(e)
          }
          case _ => Left("response was not a 200: " + response)
        }
      } catch {
        case e: java.net.UnknownHostException => Left("domain is invalid: " + url)
        case e: Throwable => Left("something went wrong: " + e)
      }
    })

    res.map(_.asInstanceOf[R])
  }
}


object RickAndMortyService {
  import Helpers.{get}

  val baseUrl = "https://rickandmortyapi.com/api/"

  /**** Apis ****/
  case class Apis(characters: String, episodes: String, locations: String)

  implicit val apiFormat: JsonFormat[Apis] = jsonFormat3(Apis)

  def getApis(): Either[String, Apis] = get[Apis](baseUrl)

  /**** Character ****/
  case class Character(id: Int, name: String, status: String, episode: List[String])

  implicit val characterFormat: JsonFormat[Character] = jsonFormat4(Character)

  def getCharacter(id: String): Either[String, Character] =
    getApis().flatMap(apis => get[Character](apis.characters + "/" + id))

  def getCharacters(ids: List[String]): Either[String, List[Character]] =
    getApis().flatMap(apis => get[List[Character]](apis.characters + "/" + ids.mkString(",")))

  /**** Episode ****/
  case class Episode(id: Int, name: String, characters: List[String])

  implicit val episodeFormat: JsonFormat[Episode] = jsonFormat3(Episode)

  def getEpisode(id: String): Either[String, Episode] =
    getApis().flatMap(apis => get[Episode](apis.episodes + "/" + id))

  def getEpisodes(ids: List[String]): Either[String, List[Episode]] =
    getApis().flatMap(apis => get[List[Episode]](apis.episodes + "/" + ids.mkString(",")))

  /**** Paginated Results ****/
  case class Info(count: Int, pages: Int, next: Option[String], prev: Option[String])

  implicit val infoFormat: JsonFormat[Info] = jsonFormat4(Info)
}
