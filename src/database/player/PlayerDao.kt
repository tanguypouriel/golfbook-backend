package com.mindeurfou.database.player

import com.mindeurfou.model.player.outgoing.Player
import com.mindeurfou.model.player.incoming.PostPlayerBody
import com.mindeurfou.model.player.incoming.PutPlayerBody

interface PlayerDao {
    fun getPlayerById(playerId: Int): Player?
    fun getPlayerPassword(playerId: Int): String?
    fun getPlayers(filters: Map<String, String>?, limit: Int, offset: Int): List<Player>
    fun insertPlayer(postPlayer: PostPlayerBody): Int
    fun updatePlayer(putPlayer: PutPlayerBody): Player
    fun deletePlayer(playerId: Int): Boolean
    fun getPlayerByUsername(username: String): Player?
}