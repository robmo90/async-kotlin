package de.iteconomics.async_kotlin.viewmodel.errorHandling

import androidx.lifecycle.*
import de.iteconomics.async_kotlin.data.Talk
import de.iteconomics.async_kotlin.network.cold.TalkDataSource
import de.iteconomics.async_kotlin.viewmodel.doHeavyWork
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

// Zustand des Views ist in ein Objekt gekapselt - aktuell gibt es nur Erfolg und Fehler
data class TalkViewState(
    val talks: List<Talk>? = null,
    val error: String? = null
)


class RxTalkViewModel(
    private val dataSource: TalkDataSource.Rx
) : ViewModel() {

    private var disposable: Disposable? = null
    private val talkLiveData = MutableLiveData<TalkViewState>()

    private fun start() {
        disposable = dataSource.getSingleTalks()
            .subscribeOn(Schedulers.io())
            .onErrorResumeNext { throwable ->
                // Fehler beim Netzwerkaufruf (z.B. kein Netz)
                throwable.log()
                getLocalTalks()
            }

            .observeOn(Schedulers.computation())
            .map { talks ->
                doHeavyWork(talks)
            }
            .observeOn(AndroidSchedulers.mainThread())

            .subscribeBy(
                onSuccess = { talks ->
                    talkLiveData.postValue(TalkViewState(talks = talks))
                },
                // Fehler in den restlichen Operationen werden abgefangen
                onError = { throwable ->
                    throwable.log()
                    talkLiveData.postValue(TalkViewState(error = throwable.message!!))
                }
            )
    }

    // Hole Talks aus dem Cache, sofern ein Fehler auftritt
    private fun getLocalTalks(): Single<List<Talk>> {
        return Single.just(emptyList())
    }

    override fun onCleared() {
        super.onCleared()
        disposable?.dispose()
    }
}


class CoroutineTalkViewModel(
    private val dataSource: TalkDataSource.Coroutines
) : ViewModel() {

    private val talkLiveData = MutableLiveData<TalkViewState>()

    // Exception Handler kann an jeden Job angehängt werden,
    // um Fehler handzuhaben
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.log()
        talkLiveData.postValue(TalkViewState(error = throwable.message!!))
    }

    fun start() {
        // Kurzform, um direkt auf den IO Thread Pool zu wechseln und den Exception Handler zu nutzen
        viewModelScope.launch(Dispatchers.IO + exceptionHandler) {
            val talks = try {
                dataSource.getTalks()
            } catch (throwable: Throwable) {
                // Fehler beim Netzwerkaufruf (z.B. kein Netz)
                throwable.log()
                getLocalTalks()
            }

            withContext(Dispatchers.Default) {
                doHeavyWork(talks)
            }

            talkLiveData.postValue(TalkViewState(talks = talks))
        }
    }

    private suspend fun getLocalTalks(): List<Talk> {
        return emptyList()
    }

}


@ExperimentalCoroutinesApi
class FlowTalkViewModel(
    private val dataSource: TalkDataSource.Flows
) : ViewModel() {

    // Exception Handler kann an jeden Job angehängt werden,
    // um Fehler handzuhaben
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.log()
    }

    val talkLiveData = dataSource.getTalks()
        .flowOn(Dispatchers.IO + exceptionHandler)
        .catch {
            // Fängt Fehler im vorherigen Flow, z.B. bei Netzwerkfehlern
            emit(getLocalTalks())
        }
        .map { talks ->
            doHeavyWork(talks)
        }
        .flowOn(Dispatchers.Default + exceptionHandler)
        .map { talks -> TalkViewState(talks) }
        .catch { throwable ->
            emit(TalkViewState(error = throwable.message!!))
        }
        .asLiveData()

    private suspend fun getLocalTalks(): List<Talk> {
        return emptyList()
    }


}

fun Throwable.log() {
    println("Fehler: ${this.message}")
}