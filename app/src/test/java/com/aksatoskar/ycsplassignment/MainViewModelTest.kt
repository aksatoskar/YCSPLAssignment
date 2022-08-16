package com.aksatoskar.ycsplassignment

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.aksatoskar.ycsplassignment.data.ISourceRepository
import com.aksatoskar.ycsplassignment.data.local.LocationDao
import com.aksatoskar.ycsplassignment.model.LocationDetails
import com.aksatoskar.ycsplassignment.model.Resource
import com.aksatoskar.ycsplassignment.ui.main.viewmodel.MainViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest

@ExperimentalCoroutinesApi
class MainViewModelTest {

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val testCoroutineRule = CoroutinesTestRule()

    @Mock
    private lateinit var sourceRepository: ISourceRepository

    @Mock
    private lateinit var dao: LocationDao

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun givenDetailsPresentInDB_whenFetch_shouldReturnSuccess() {
        val response = listOf(LocationDetails(latitude = 111.01, longitude = 222.02, propertyName = "test"))

        val flow = flow {
            emit(Resource.loading())
            emit(Resource.success(response))
        }

        testCoroutineRule.testDispatcher.runBlockingTest {
            `when`(sourceRepository.getAllLocations()).thenReturn(flow)
            `when`(dao.getAll()).thenReturn(emptyList())
        }

        testCoroutineRule.testDispatcher.pauseDispatcher()

        val viewModel = MainViewModel(sourceRepository)
        viewModel.fetchLocations()
        assertThat(viewModel.stateFlow.value, `is`(Resource.loading()))

        testCoroutineRule.testDispatcher.resumeDispatcher()

        assertThat(viewModel.stateFlow.value, `is`(Resource.success(response)))
    }

    @Test
    fun givenDBResponseError_whenFetch_shouldReturnError() {
        val errorMsg = Exception("errorMsg")
        val flow = flow<Resource<List<LocationDetails>>> {
            emit(Resource.loading())
            emit(Resource.error(errorMsg))
        }

        testCoroutineRule.testDispatcher.runBlockingTest {
            `when`(sourceRepository.getAllLocations()).thenReturn(flow)
            `when`(dao.getAll()).thenReturn(emptyList())
        }
        testCoroutineRule.testDispatcher.pauseDispatcher()
        val viewModel = MainViewModel(sourceRepository)
        viewModel.fetchLocations()
        assertThat(viewModel.stateFlow.value, `is`(Resource.loading()))

        testCoroutineRule.testDispatcher.resumeDispatcher()

        assertThat(viewModel.stateFlow.value, `is`(Resource.error(errorMsg, null)))
    }

}