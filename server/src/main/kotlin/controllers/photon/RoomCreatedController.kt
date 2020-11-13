package controllers.photon

import io.ktor.application.*
import io.ktor.request.*
import repository.GameStateStore
import schema.requests.photon.CreateOptions
import schema.requests.photon.RoomCreatedRequest
import util.logger
import kotlin.math.ceil
import kotlin.math.roundToInt

class RoomCreatedController(private val gameStateStore: GameStateStore) {
    suspend fun roomCreated(ctx: ApplicationCall) {
        val request = ctx.receive<RoomCreatedRequest>()
        logger.info { "Room created $request" }

        val createOptions = request.createOptions
        if (createOptions.lobbyId != GameLobbyId) {
            logger.warn { "Ignoring room created outside of Game Lobby ${createOptions.lobbyId}}" }
            return
        }
        val imposterCount = getImposterCountForGame(createOptions)

        val (_, error) = gameStateStore.initGameState(request.gameId, createOptions.maxPlayers, imposterCount)
        if (error != null) {
            throw Error("Failed to initialize game state", error)
        }

        processPlayerJoined(request.gameId, request.playerId, gameStateStore)
    }

    private fun getImposterCountForGame(createOptions: CreateOptions): Int {
        var imposterCount = createOptions.customProps.imposterCount
        if (imposterCount < 1) {
            //TODO: Get real imposter count
            imposterCount = ceil(0.2f * createOptions.maxPlayers).roundToInt()
        }
        return imposterCount
    }
}