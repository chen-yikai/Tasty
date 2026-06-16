package dev.eliaschen.tasty.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.eliaschen.tasty.core.Food
import dev.eliaschen.tasty.core.FoodType
import dev.eliaschen.tasty.core.NetworkClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds the Home screen's data so it survives navigation to other destinations (e.g. FoodDetail).
 *
 * The screen composable is disposed while it sits below another entry on the back stack, which
 * would otherwise discard any `remember`ed data. This ViewModel is retained for the Activity's
 * lifetime, so returning to Home shows the cached list instantly instead of reloading. The
 * LazyColumn's scroll position is preserved separately via `rememberLazyListState()` (retained by
 * the NavDisplay's saveable state holder) once this data is present again.
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {
    val foodTypes = mutableStateListOf<FoodType>()
    val foods = mutableStateListOf<Food>()

    var selectedTypeId by mutableStateOf(0)
        private set
    var loading by mutableStateOf(true)
        private set

    private var hasLoaded = false
    private var loadedToken: String? = null
    private var selectionJob: Job? = null

    /**
     * Loads types and foods the first time Home is shown. Safe to call on every (re)composition:
     * it no-ops while cached, but reloads when the signed-in user changes.
     */
    fun ensureLoaded(api: NetworkClient) {
        if (hasLoaded && loadedToken == api.token) return
        hasLoaded = true
        loadedToken = api.token
        loading = true
        viewModelScope.launch {
            loadTypesAndFoods(api)
            loading = false
        }
    }

    /** Re-fetches everything; triggered by realtime order updates. */
    fun refresh(api: NetworkClient) {
        viewModelScope.launch { loadTypesAndFoods(api) }
    }

    /** Switches the selected category and loads its foods, cancelling any in-flight switch. */
    fun selectType(api: NetworkClient, typeId: Int) {
        if (typeId == selectedTypeId) return
        selectedTypeId = typeId
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch { loadSelectedFoods(api) }
    }

    private suspend fun loadTypesAndFoods(api: NetworkClient) {
        val latestTypes = api.getFoodTypes()
        val resolvedTypeId = when {
            latestTypes.isEmpty() -> 0
            latestTypes.any { it.id == selectedTypeId } -> selectedTypeId
            else -> latestTypes.first().id
        }
        val latestFoods = if (resolvedTypeId != 0) api.getFoods(resolvedTypeId) else emptyList()

        foodTypes.clear()
        foodTypes.addAll(latestTypes)
        selectedTypeId = resolvedTypeId
        foods.clear()
        foods.addAll(latestFoods)
    }

    private suspend fun loadSelectedFoods(api: NetworkClient) {
        if (selectedTypeId == 0) {
            foods.clear()
            return
        }
        loading = true
        val newData = api.getFoods(selectedTypeId)
        loading = false
        foods.clear()
        foods.addAll(newData)
    }
}
