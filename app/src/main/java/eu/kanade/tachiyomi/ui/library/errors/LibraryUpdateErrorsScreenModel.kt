package eu.kanade.tachiyomi.ui.library.errors

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.libraryUpdateError.interactor.DeleteLibraryUpdateErrors
import tachiyomi.domain.libraryUpdateError.interactor.GetLibraryUpdateErrorWithRelations
import tachiyomi.domain.libraryUpdateError.model.LibraryUpdateErrorWithRelations
import tachiyomi.domain.libraryUpdateErrorMessage.interactor.GetLibraryUpdateErrorMessages
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class LibraryUpdateErrorUiItem(
    val error: LibraryUpdateErrorWithRelations,
    val message: String,
)

class LibraryUpdateErrorsScreenModel(
    private val getLibraryUpdateErrorWithRelations: GetLibraryUpdateErrorWithRelations = Injekt.get(),
    private val getLibraryUpdateErrorMessages: GetLibraryUpdateErrorMessages = Injekt.get(),
    private val deleteLibraryUpdateErrors: DeleteLibraryUpdateErrors = Injekt.get(),
) : StateScreenModel<List<LibraryUpdateErrorUiItem>>(emptyList()) {

    init {
        screenModelScope.launchIO {
            combine(
                getLibraryUpdateErrorWithRelations.subscribeAll(),
                getLibraryUpdateErrorMessages.subscribe(),
            ) { errors, messages ->
                errors.map { error ->
                    LibraryUpdateErrorUiItem(
                        error = error,
                        message = messages.find { it.id == error.messageId }?.message ?: "Unknown error",
                    )
                }
            }.collectLatest { mutableState.value = it }
        }
    }

    fun deleteErrors() {
        screenModelScope.launchIO {
            deleteLibraryUpdateErrors.await()
        }
    }
}