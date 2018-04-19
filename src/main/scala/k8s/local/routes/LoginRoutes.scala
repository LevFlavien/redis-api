package k8s.local.routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern.ask
import akka.stream.ActorMaterializer
import authentikat.jwt.JsonWebToken
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import k8s.local.registry.User
import k8s.local.registry.UserRegistryActor.GetUser
import k8s.local.tools.{ Authentication, JsonSupport, LoginRequest }
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.collection.immutable.Seq

trait LoginRoutes extends JsonSupport {
  private val tokenExpiryPeriodInDays = 1

  val headers: Seq[String] = Seq("Access-Token")
  val corsSettings: CorsSettings = CorsSettings.defaultSettings.withExposedHeaders(headers)

  def loginRoutes(userRegistryActor: ActorRef, askTimeout: FiniteDuration)(implicit mat: ActorMaterializer, ec: ExecutionContext, rs: RoutingSettings): Route =
    cors(corsSettings) {
      pathPrefix("login") {
        pathEnd {
          concat(
            post {
              entity(as[LoginRequest]) { lr =>
                val user = userRegistryActor.ask(GetUser(lr.username))(askTimeout).mapTo[Option[User]]
                onSuccess(user) { usrOpt =>
                  if (usrOpt.nonEmpty && usrOpt.get.username != "" && lr.username == usrOpt.get.username && BCrypt.checkpw(lr.password, usrOpt.get.password)) {
                    val claims = Authentication.setClaims(lr.username, tokenExpiryPeriodInDays)
                    respondWithHeader(RawHeader("Access-Token", JsonWebToken(Authentication.header, claims, Authentication.secretKey))) {
                      complete(StatusCodes.OK, lr.username)
                    }
                  } else {
                    complete(StatusCodes.Unauthorized)
                  }
                }
              }
            }, get {
              Authentication.authenticated { claims =>
                complete(claims.getOrElse("user", "").toString)
              }
            }
          )
        }
      }
    }
}
