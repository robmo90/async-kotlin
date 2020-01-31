package de.iteconomics.async_kotlin.network.cold

import de.iteconomics.async_kotlin.data.Talk
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class TalkDataSource(
    private val talkClient: TalkClient
) {

    inner class Rx {

        fun getObservableTalks(): Observable<List<Talk>> {
            return Observable.fromCallable {
                talkClient.getTalks()
            }
        }

        /*
            Erzeugt ein spezifisches Observable, das nur einen Wert zurückliefert
         */
        fun getSingleTalks(): Single<List<Talk>> {
            // TODO: Werte lazy zurückgeben
            return Single.just(
                talkClient.getTalks()
            )
        }

        /*
            Erzeugt ein spezifisches Observable, das gar keinen Wert zurückliefert
         */
        fun markTalkAsFavorite(talk: Talk): Completable {
            // TODO: Exception in Event Stream einfügen
            return Completable.fromAction {
                talkClient.markTalkAsFavorite(talk)
            }
        }

    }


    inner class Coroutines {

        suspend fun markTalkAsFavorite(talk: Talk) {
            talkClient.markTalkAsFavorite(talk)
        }

        suspend fun getTalks(): List<Talk> {
            return talkClient.getTalks()
        }

        /*
            Gibt einen Job zurück, aber nicht die Objekte -> Coroutinen sollten auf höherer Ebene
            wie dem ViewModel oder Presenter gestartet werden
         */
        fun getTalksWithMainScope(): Job {
            val job = MainScope().launch {
                talkClient.getTalks()
            }

            return job
        }

        // Gibt ein Deferred zurück, was grob einem Future entspricht
        fun getDeferredTalks(): Deferred<List<Talk>> {
            val deferred = MainScope().async {
                talkClient.getTalks()
            }
            return deferred
        }

    }


    inner class Flows {

        // Stellt einen Lazy Flow ohne Ergebnis bereit
        fun markTalkAsFavorite(talk: Talk): Flow<Unit> {
            return flow {
                talkClient.markTalkAsFavorite(talk)
                emit(Unit)
            }
        }

        // Stellt einen lazy Flow der Talks bereit
        fun getTalks(): Flow<List<Talk>> {
            return flow {
                val talks = talkClient.getTalks()
                // Übergibt Talks an den Flow
                emit(talks)
            }
        }

        // Kurzschreibweise der oberen Funktion
        fun getTalksWithFlowOf(): Flow<List<Talk>> {
            return flowOf(talkClient.getTalks())
        }

    }


}