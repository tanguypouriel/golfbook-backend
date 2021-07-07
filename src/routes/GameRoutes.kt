package com.mindeurfou.routes

import com.mindeurfou.model.game.incoming.PostGameBody
import com.mindeurfou.model.game.incoming.PutGameBody
import com.mindeurfou.service.GameService
import com.mindeurfou.utils.GBException
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.koin.ktor.ext.inject

fun Route.gameRouting() {

    val gameService: GameService by inject()

    route("/game") {

        route("{id}") {

            get {
                val gameId = call.parameters["id"]?.toInt() ?: return@get call.respond(HttpStatusCode.BadRequest)
                try {
                    val gameDetails = gameService.getGame(gameId)
                    call.respond(gameDetails)
                } catch (gBException: GBException) {
                    call.respondText(gBException.message, status = HttpStatusCode.NotFound)
                }
            }

            put {
                try {
                    val putGameBody = call.receive<PutGameBody>()
                    val updatedGame = gameService.updateGame(putGameBody)
                    call.respond(updatedGame)
                } catch (e: ContentTransformationException) {
                    return@put call.respond(HttpStatusCode.BadRequest)
                } catch (e: GBException) {
                    call.respondText(e.message, status = HttpStatusCode.NotFound)
                }
            }

            delete {
                val gameId = call.parameters["id"]?.toInt() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                try {
                    val deleted = gameService.deleteGame(gameId)
                    call.respond(deleted)
                } catch (e: GBException) {
                    call.respondText(e.message, status = HttpStatusCode.NotFound)
                }
            }

            scoreBookRouting(gameService)
        }

        post {
            try {
                val postGameBody = call.receive<PostGameBody>()
                val gameDetails = gameService.addNewGame(postGameBody)
                call.respond(gameDetails)
            } catch (e: ContentTransformationException) {
                return@post call.respond(HttpStatusCode.BadRequest)
            } catch (e: GBException) {
                call.respondText(e.message, status = HttpStatusCode.NotFound)
            }
        }

        get {
            val tournamentId = call.parameters["tournamentId"]?.toInt() ?: return@get call.respond(HttpStatusCode.BadRequest)
            val games = gameService.getGameByTournamentId(tournamentId)
            call.respond(games)
        }

    }
}


private fun Route.scoreBookRouting(gameService: GameService) {

    route("scorebook") {
        // TODO
    }
}