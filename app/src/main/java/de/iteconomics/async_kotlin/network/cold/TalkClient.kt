package de.iteconomics.async_kotlin.network.cold

import de.iteconomics.async_kotlin.data.Talk
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow

interface TalkClient {

    fun getTalks() : List<Talk>

    fun markTalkAsFavorite(talk: Talk)

}