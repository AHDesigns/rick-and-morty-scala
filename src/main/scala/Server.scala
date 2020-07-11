package server

import scala.util.{Left,Either,Right}
import spray.json._
import DefaultJsonProtocol._
import scala.concurrent._
import java.util.concurrent.Executors



object Server extends App {
  val characterOfInterest = "1"

  val url = "https://rickandmortyapi.com/api/"

  import RickAndMortyService._
  import Helpers.ec

  val res = getApis()
  // res foreach (value => println(value))
  res
    .map(value => value.episodes)
    .map(println)

  println("me first")

  import scala.concurrent.duration._
  Await.ready(res, 60.seconds)

  // response.flatMap { res =>
  //   println(res)
  //   // res.code match {
  //   //   case 200 => println(res.body.parseJson.prettyPrint)
  //   //   case _ => throw new Exception("response was not a 200: " + res)
  //   // }
  // }

  // implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  // getCharacter(characterOfInterest) flatMap { character =>
  //   println(character)
  // }

  // getCharacter(characterOfInterest)
  //   .map(_.episode.map(_.split("episode/").last))
  //   .flatMap(getEpisodes)
  //   .map(getUniqCharacterIdsIgnoring(characterOfInterest))
  //   .flatMap(getCharacters)
  //   .map(_.map(_.name))
  //   .map(println)

  /***** Helpers *****/

  // def getUniqCharacterIdsIgnoring(ignoredCharacterId: String)(episodes: List[Episode]) = 
  //   episodes
  //     .flatMap(_.characters.map(_.split("character/").last))
  //     .toSet
  //     .toList
  //     .filter(_ != ignoredCharacterId)
}
  object Helpers {
    import scalaj.http._

    implicit val ec: ExecutionContext = ExecutionContext.global
    // implicit val exec = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

    def get[R: JsonReader](url: String) = Future {
      println("running")
      val response = Http(url).asString
      println("have response")

      response.code match {
        case 200 => response.body.parseJson.convertTo[R]
        case _ => throw new Exception("response was not a 200: " + response)
      }
    }
  }


// object Helpers {
//   type Params = Option[Map[String, String]]
//   val cache = collection.mutable.Map.empty[(String, Params), Any]

//   import scalaj.http._
//   def get[R: JsonReader](url: String, params: Params = None): Future[R] = Future {
//     cache.getOrElseUpdate((url, params), {
//       try {
//         val response = params match {
//           case None => Http(url).asString
//           case Some(p) => Http(url).params(p).asString
//         }

//         response.code match {
//           case 200 => response.body.parseJson.convertTo[R]
//           case _ => throw new Exception("response was not a 200: " + response)
//         }
//       } catch {
//         case e: java.net.UnknownHostException => throw new Exception("domain is invalid: " + url)
//         case e: spray.json.DeserializationException => throw new Exception(
//           s"""DeserializationException for:\n${url} ${params}\ngot ${response.body}\n$e"""
//         )
//         case e: Throwable => throw new Exception("something went wrong: " + e)
//       }
//     }).asInstanceOf[R]
//   }
// }

object RickAndMortyService {
  import Helpers._

  val baseUrl = "https://rickandmortyapi.com/api/"

  /**** Apis ****/
  case class Apis(characters: String, episodes: String, locations: String)

  implicit val apiFormat: JsonFormat[Apis] = jsonFormat3(Apis)

  def getApis() = get[Apis](baseUrl)

  /**** Character ****/
  case class Character(id: Int, name: String, status: String, episode: List[String])

  implicit val characterFormat: JsonFormat[Character] = jsonFormat4(Character)

  // def getCharacter(id: String) = 
  //   getApis().flatMap(apis => get[Character](apis.characters + "/" + id))

  // def getCharacters(ids: List[String]) =
  //   getApis().flatMap(apis => get[List[Character]](apis.characters + "/" + ids.mkString(",")))

  // /**** Episode ****/
  // case class Episode(id: Int, name: String, characters: List[String])

  // implicit val episodeFormat: JsonFormat[Episode] = jsonFormat3(Episode)

  // def getEpisode(id: String): Either[String, Episode] =
  //   getApis().flatMap(apis => get[Episode](apis.episodes + "/" + id))

  // def getEpisodes(ids: List[String]): Either[String, List[Episode]] =
  //   getApis().flatMap(apis => get[List[Episode]](apis.episodes + "/" + ids.mkString(",")))

  // /**** Paginated Results ****/
  // case class Info(count: Int, pages: Int, next: Option[String], prev: Option[String])

  // implicit val infoFormat: JsonFormat[Info] = jsonFormat4(Info)
}
