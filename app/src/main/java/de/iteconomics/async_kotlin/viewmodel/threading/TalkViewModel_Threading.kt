package de.iteconomics.async_kotlin.viewmodel.threading

import androidx.lifecycle.*
import de.iteconomics.async_kotlin.data.Talk
import de.iteconomics.async_kotlin.network.cold.TalkDataSource
import de.iteconomics.async_kotlin.viewmodel.doHeavyWork
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map


class RxTalkViewModel(
    private val dataSource: TalkDataSource.Rx
) : ViewModel() {

    private var disposable: Disposable? = null

    private val talkLiveData = MutableLiveData<List<Talk>>()


    private fun start() {
        disposable = dataSource.getSingleTalks() // --> IO Thread Pool
            .subscribeOn(Schedulers.io()) // Alle Operationen laufen auf dem IO Thread Pool
            .observeOn(Schedulers.computation()) // Alle folgenden Operationen laufen auf dem Computation Thread Pool
            .map { talks ->
                doHeavyWork(talks) // --> Computation Thread Pool
            }
            .observeOn(AndroidSchedulers.mainThread()) // Alle folgenden Operationen laufen auf dem UI Thread
            .subscribe { talks ->
                talkLiveData.postValue(talks) // --> UI Thread
            }
    }

    // Wird automatisch aufgerufen, sobald der Halter des ViewModels nicht mehr existiert
    override fun onCleared() {
        super.onCleared()
        disposable?.dispose()
    }
}


class CoroutineTalkViewModel(
    private val dataSource: TalkDataSource.Coroutines
) : ViewModel() {

    /*
     Operationen im Live Data Coroutine Scope werden gestartet,
     sobald die Live Data observed werden und stoppen,
     sobald der beobachtende View zerstört wird.
     Sie starten auf dem dem UI Thread.
     */
    val talkLiveData: LiveData<List<Talk>> = liveData {
        val talks = withContext(Dispatchers.IO) {
            // Alle Operationen im Lambda laufen auf dem IO Thread Pool
            dataSource.getTalks() // --> IO Thread Pool
        }
        val workedTalks = withContext(Dispatchers.Default) {
            // Alle Operationen im Lambda laufen auf dem Computation Thread Pool
            doHeavyWork(talks) // --> Computation Thread Pool
        }
        emit(workedTalks) // --> UI Thread
    }

    /*
     Operationen im View Model Coroutine Scope werden gestoppt,
     sobald das ViewModel gecleared wird. Sie starten auf dem UI Thread.
     */
    fun markTalkAsFavorite(talk: Talk) {
        viewModelScope.launch {
            // --> UI Thread
            withContext(Dispatchers.IO) {
                // --> IO Thread Pool
                dataSource.markTalkAsFavorite(talk)
            }
        }
    }

}


@ExperimentalCoroutinesApi
class FlowTalkViewModel(
    private val dataSource: TalkDataSource.Flows
) : ViewModel() {

    val talkLiveData = dataSource.getTalks() // --> IO Thread Pool
        .flowOn(Dispatchers.IO) // Alle vorhergehenden Operationen laufen auf dem IO Thread Pool
        .map { talks -> doHeavyWork(talks) } // --> Computation Thread Pool
        .flowOn(Dispatchers.Default) // Alle vorhergehenden Operationen laufen auf dem Computation Thread Pool
        .asLiveData()

    fun markTalkAsFavorite(talk: Talk) {
        viewModelScope.launch(Dispatchers.IO) {
            dataSource.markTalkAsFavorite(talk)
                .launchIn(this)
        }
    }

}