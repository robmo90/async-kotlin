package de.iteconomics.async_kotlin.viewmodel

import androidx.lifecycle.*
import de.iteconomics.async_kotlin.data.Talk
import de.iteconomics.async_kotlin.network.cold.TalkDataSource
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn


class RxTalkViewModel(
    val dataSource: TalkDataSource.Rx
) : ViewModel() {


    // LiveData werden genutzt, um Daten an den View zu geben. Der View observed LiveData, solange er selbst lebt
    private val talkLiveData = MutableLiveData<List<Talk>>()

    // Sammelt beobachtete Event Streams
    private var compositeDisposable = CompositeDisposable()

    // Muss beim Start des Views aufgerufen werden
    fun start() {
        val disposable = dataSource.getSingleTalks()
            .subscribe { talks ->
                talkLiveData.postValue(talks)
            }
        compositeDisposable.add(disposable)
    }

    fun markTalkAsFavorite(talk: Talk) {
        val disposable = dataSource.markTalkAsFavorite(talk)
            .subscribe()
        compositeDisposable.add(disposable)
    }

    // Wird aufgerufen, sobald das ViewModel nicht mehr benötigt wird
    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }

}


class CoroutineTalkViewModel(
    private val dataSource: TalkDataSource.Coroutines
) : ViewModel() {

    // Sammelt gestartete Jobs
    private var supervisorJob = SupervisorJob()

    private val talkLiveData = MutableLiveData<List<Talk>>()

    fun start() {
        val talksJob = MainScope().launch {
            val talks = dataSource.getTalks()
            talkLiveData.postValue(talks)
        }
        // TODO: Add talks job to supervisor job
        talksJob.cancel()
    }

    fun markTalkAsFavorite(talk: Talk) {
        MainScope().launch(supervisorJob) {
            dataSource.markTalkAsFavorite(talk)
        }
    }

    override fun onCleared() {
        super.onCleared()
        supervisorJob.cancel()
    }


}


@ExperimentalCoroutinesApi
class FlowTalkViewModel(
    private val dataSource: TalkDataSource.Flows
) : ViewModel() {

    private val talkLiveData = MutableLiveData<List<Talk>>()

    private var supervisorJob = SupervisorJob()

    fun start() {
        MainScope().launch(supervisorJob) {
            dataSource.getTalks().collect { talks ->
                talkLiveData.postValue(talks)
            }
        }
    }

    fun markTalkAsFavorite(talk: Talk) {
        MainScope().launch(supervisorJob) {
            dataSource.markTalkAsFavorite(talk).launchIn(this)
        }
    }

    override fun onCleared() {
        super.onCleared()
        supervisorJob.cancel()
    }


}


fun doHeavyWork(talks: List<Talk>): List<Talk> {
    // We're doing something that takes a really long time
    return talks
}

