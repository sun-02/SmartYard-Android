package com.sesameware.smartyard_oem.ui.main.burger.cityCameras

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import org.osmdroid.util.BoundingBox
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import com.sesameware.domain.interactors.CCTVInteractor
import com.sesameware.domain.interactors.GeoInteractor
import com.sesameware.domain.interactors.IssueInteractor
import com.sesameware.domain.model.request.CreateIssuesRequest
import com.sesameware.domain.model.response.CCTVCityCameraData
import com.sesameware.domain.model.response.CCTVYoutubeData
import com.sesameware.domain.utils.listenerEmpty
import com.sesameware.smartyard_oem.ui.main.BaseIssueViewModel

class CityCamerasViewModel(
    private val state: SavedStateHandle,
    private val cctvInteractor: CCTVInteractor,
    geoInteractor: GeoInteractor,
    issueInteractor: IssueInteractor
) : BaseIssueViewModel(geoInteractor, issueInteractor) {
    var cityCameraList = listOf<CCTVCityCameraData>()
    val chosenCamera = state.getLiveData<CCTVCityCameraData?>(chosenCityCamera_Key, null)
    var eventList = mutableListOf<CCTVYoutubeData>()
    var isFullscreen = false
    var camerasBoundingBox: BoundingBox? = null

    fun getCityCameras(onComplete: listenerEmpty) {
        viewModelScope.withProgress {
            cctvInteractor.getCCTVOverview()?.let {
                cityCameraList = it.toList().filter {cityCamera ->
                    cityCamera.latitude != null && cityCamera.longitude != null
                }
                onComplete()
            }
        }
    }

    fun chooseCityCamera(index: Int) {
        if (0 <= index && index < cityCameraList.size) {
            state.set(chosenCityCamera_Key, cityCameraList[index])
        }
    }

    fun getEvents(id: Int?, onComplete: listenerEmpty) {
        viewModelScope.withProgress {
            eventList.clear()
            eventList = cctvInteractor.getCCTVYoutube(id)?.toMutableList() ?: mutableListOf()
            eventList.sortByDescending {
                it.eventTime
            }

            onComplete()
        }
    }

    fun createIssue(recordDate: LocalDate, recordTime: LocalTime, duration: Int, comments: String) {
        val summary = "Авто: Запрос на получение видеофрагмента с архива"
        val description =
            "Обработать запрос на добавление видеофрагмента из архива видовой видеокамеры ${chosenCamera.value?.name} (id = ${chosenCamera.value?.id}) по парамертам: дата: ${recordDate.format(
                DateTimeFormatter.ofPattern("d.MM.yyyy"))}, время: ${recordTime.format(
                DateTimeFormatter.ofPattern("HH-mm"))}, продолжительность фрагмента: $duration минут. Комментарий пользователя: $comments."
        val x10011 = "-5"
        val x12440 = "Приложение"
        super.createIssue(
            summary,
            description,
            null,
            CreateIssuesRequest.CustomFields(x10011 = x10011, x12440 = x12440),
            CreateIssuesRequest.TypeAction.ACTION3
        )
    }

    companion object {
        private const val chosenCityCamera_Key = "chosenCityCamera_Key"
        private const val eventList_Key = "eventList_Key"

        //количество подгружаемых происшествий
        const val CHUNK_ITEM_COUNT = 8

        //глубина записи архива в днях
        const val RECORD_DEPTH_DAYS = 7L
    }
}
