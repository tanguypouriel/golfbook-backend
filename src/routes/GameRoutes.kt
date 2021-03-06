package com.mindeurfou.routes

import com.mindeurfou.model.game.incoming.PatchGameBody
import com.mindeurfou.model.game.incoming.PostGameBody
import com.mindeurfou.model.game.incoming.PutGameBody
import com.mindeurfou.model.game.local.ScoreBook
import com.mindeurfou.model.player.outgoing.Player
import com.mindeurfou.service.GameService
import com.mindeurfou.utils.GBException
import com.mindeurfou.utils.GBHttpStatusCode
import com.mindeurfou.utils.addCacheHeader
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.SerializationException
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
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            put {
                try {
                    // TODO work here ?
                    val putGameBody = call.receive<PutGameBody>()
                    val updatedGame = gameService.updateGame(putGameBody)
                    call.respond(updatedGame)
                } catch (e: SerializationException) {
                    return@put call.respond(HttpStatusCode.BadRequest)
                } catch (e: GBException) {
                    if (e.message == GBException.GAME_NOT_FIND_MESSAGE)
                        call.respond(HttpStatusCode.NotFound)
                    else // invalid operation message
                        call.respondText(e.message, status = GBHttpStatusCode.valueA)
                }
            }

            patch {
                val gameId = call.parameters["id"]?.toInt() ?: return@patch call.respond(HttpStatusCode.BadRequest)
                try {
                    val patchGameBody = call.receive<PatchGameBody>()
                    if (patchGameBody.playing)
                        gameService.addGamePlayer(gameId, patchGameBody.playerId)
                    else
                        gameService.deleteGamePlayer(gameId, patchGameBody.playerId)
                    call.respond(HttpStatusCode.OK)
                } catch (e: SerializationException) {
                    return@patch call.respond(HttpStatusCode.BadRequest)
                }
            }

            delete {
                val gameId = call.parameters["id"]?.toInt() ?: return@delete call.respond(HttpStatusCode.BadRequest)
                try {
                    val deleted = gameService.deleteGame(gameId)
                    call.respond(deleted)
                } catch (e: GBException) {
                    call.respondText(e.message, status = GBHttpStatusCode.valueA)
                }
            }

            scoreBookRouting(gameService)
        }

        post {
            try {
                val postGameBody = call.receive<PostGameBody>()
                var gameDetails = gameService.addNewGame(postGameBody)
                call.principal<Player>()?.let {
                    gameDetails = gameService.addGamePlayer(gameDetails.id, it.id)
                }
                call.respond(gameDetails)
            } catch (e: SerializationException) {
                return@post call.respond(HttpStatusCode.BadRequest)
            } catch (e: GBException) {
                if (e.message == GBException.TOURNAMENT_NOT_FIND_MESSAGE)
                    call.respond(HttpStatusCode.NotFound)
                else
                    call.respond(GBHttpStatusCode.valueA) // tournament done
            }
        }

        get {
            val playerId = call.parameters["playerId"]?.toInt()
            val state = call.parameters["state"]

            if ((playerId != null && state != null) || (playerId == null && state == null))
                return@get call.respond(HttpStatusCode.BadRequest)

            val games =  if (playerId != null)
                gameService.getGamesByPlayerId(playerId)
            else
                gameService.getGamesByState(state!!)

            games?.let {
                with(call) {
                    addCacheHeader()
                    call.respond(it)
                }
            } ?: return@get call.respond(HttpStatusCode.NoContent)
        }

    }
}


private fun Route.scoreBookRouting(gameService: GameService) {

    route("scorebook") {

        put {
            val gameId = call.parameters["id"]?.toInt() ?: return@put call.respond(HttpStatusCode.BadRequest)
            try {
                val scoreBook = call.receive<ScoreBook>()
                val updatedScorebook = gameService.updateScoreBook(gameId, scoreBook)
                call.respond(updatedScorebook)
            } catch (e: SerializationException) {
                call.respond(HttpStatusCode.BadRequest)
            } catch (e: GBException) {
                if (e.message == GBException.GAME_NOT_FIND_MESSAGE)
                    call.respond(HttpStatusCode.NotFound)
                else // invalid operation
                    call.respondText(e.message, status = GBHttpStatusCode.valueA)
            }
        }

        get {
            val gameId = call.parameters["id"]?.toInt() ?: return@get call.respond(HttpStatusCode.BadRequest)
            try {
                val scoreBook = gameService.getScoreBookByGameId(gameId)
                call.respond(scoreBook)
            } catch (e: GBException) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}