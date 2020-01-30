package de.iteconomics.async_kotlin.network.cold

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import java.lang.IllegalArgumentException
import java.util.concurrent.Executors

class TalkDataSourceTest {

    private val client = mockk<TalkClient>()


    val dataSource = TalkDataSource(talkClient = client)

    @Test
    fun successRx() {
        every { client.getTalks() } returns emptyList()

        dataSource.Rx().getSingleTalks().test()
            .assertValue { result -> result.isEmpty() }
            .assertComplete()
    }

    @Test
    fun errorRx() {
        every { client.getTalks() } throws IllegalArgumentException("Something is wrong")

        dataSource.Rx().getSingleTalks().test()
            .assertError { error -> error is IllegalArgumentException }
    }


    // Da suspending functions einen Coroutine Context benötigen, müssen wir entweder mit runBlocking
    // oder runBlockingTest einen Context starten, auf dem wir die Methoden aufrufen können
    @ExperimentalCoroutinesApi
    @Test
    fun successCoroutines() = runBlockingTest {
        every { client.getTalks() } returns emptyList()

        val result = dataSource.Coroutines().getTalks()

        assertTrue(result.isEmpty())
    }

    @ExperimentalCoroutinesApi
    @Test(expected = IllegalArgumentException::class)
    fun errorCoroutines() = runBlockingTest {
        every { client.getTalks() } throws IllegalArgumentException("Something is wrong")

        dataSource.Coroutines().getTalks()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun successCoroutinesOnMain() = runBlocking {
        // Da wir den MainScope nutzen, um einen Coroutine Context zu starten, dieser allerdings nur
        // auf einem echten Gerät zur Verfügung steht, müssen wir ihn mocken. Nutzt man den AndroidScheduler
        // von RxJava, muss man es ähnlich handhaben.
        val mainReplacement = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        Dispatchers.setMain(mainReplacement)

        every { client.getTalks() } returns emptyList()

        val deferredTalks = dataSource.Coroutines().getDeferredTalks()

        val result = deferredTalks.await()
        assertTrue(result.isEmpty())

        Dispatchers.resetMain()
        Unit
    }

    @Test
    fun successFlow() = runBlocking {
        every { client.getTalks() } returns emptyList()

        val results = dataSource.Flows().getTalks().toList()

        assertTrue(results.size == 1)
        assertTrue(results.first().isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun errorFlows() = runBlocking {
        every { client.getTalks() } throws IllegalArgumentException("Something is wrong")

        dataSource.Flows().getTalksWithFlowOf().toList()
        Unit
    }
}