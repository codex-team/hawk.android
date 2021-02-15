package so.codex.hawk.ui.main

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.squareup.picasso.Picasso
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import so.codex.hawk.HawkApp
import so.codex.hawk.R
import so.codex.hawk.custom.views.SquircleDrawable
import so.codex.hawk.custom.views.badge.UiBadgeViewModel
import so.codex.hawk.custom.views.search.HawkSearchUiViewModel
import so.codex.hawk.domain.FetchProjectsInteractor
import so.codex.hawk.domain.FetchWorkspacesInteractor
import so.codex.hawk.entity.Project
import so.codex.hawk.extensions.domain.Utils
import so.codex.hawk.notification.domain.NotificationManager
import so.codex.hawk.notification.model.NotificationModel
import so.codex.hawk.notification.model.NotificationType
import so.codex.hawk.ui.data.UiMainViewModel
import so.codex.hawk.ui.data.UiProject
import so.codex.hawk.ui.data.UiWorkspace
import so.codex.hawk.utils.ShortNumberUtils
import javax.inject.Inject

/**
 * The ViewModel class for MainActivity.
 * Designed to handle various processes associated with the specified activity.
 */
class MainViewModel : ViewModel() {
    /**
     * @see Context
     */
    @Inject
    lateinit var context: Context

    /**
     * @property fetchWorkspaceInteractor for getting workspaces
     */
    @Inject
    lateinit var fetchWorkspaceInteractor: FetchWorkspacesInteractor

    /**
     * @property fetchProjectInteractor for getting projects
     */
    @Inject
    lateinit var fetchProjectInteractor: FetchProjectsInteractor

    /**
     * Show notification if an error occurred
     */
    @Inject
    lateinit var notificationManager: NotificationManager

    /**
     * A LiveData with list of [UiProject] that should be inserted to the view
     */
    private val uiProjects: MutableLiveData<List<UiProject>> = MutableLiveData()

    /**
     * A LiveData of [UiMainViewModel] that should be inserted to the view
     */
    private val uiModels: MutableLiveData<UiMainViewModel> = MutableLiveData()

    /**
     * Subject of ui event for notify a component of a new event
     */
    private val eventSubject = PublishSubject.create<UiEvent>()

    /**
     * Subject of change text in search view
     */
    private val searchSubject = BehaviorSubject.createDefault("")

    /**
     * Contain all disposable of sources
     */
    private val disposable = CompositeDisposable()

    /**
     * Initialization block. Called on creation.
     */
    init {
        HawkApp.mainComponent.inject(this)
        disposable.addAll(
            subscribeOnData(),
            subscribeOnEvent(),
            subscribeOnProjects()
        )

        eventSubject.onNext(UiEvent.Refresh)
    }

    /**
     * Subscribe on events from ui and handle it
     *
     * @return [Disposable] for dispose of observer from source
     */
    private fun subscribeOnEvent(): Disposable {
        return eventSubject.subscribe { event ->
            when (event) {
                is UiEvent.Refresh -> {
                    fetchWorkspaceInteractor.update()
                }
            }
        }
    }

    /**
     * Subscribe on project list and subject of search text, filter of projects by text with ignoring
     * @return [Disposable] for dispose of observer from source
     */
    private fun subscribeOnProjects(): Disposable {
        return Observables.combineLatest(
            fetchProjectInteractor.fetchProjects().distinctUntilChanged(),
            searchSubject.observeOn(Schedulers.io())
        )
            .subscribeOn(Schedulers.io())
            .map { (projectList, searchText) ->
                projectList.filter {
                    it.name.contains(searchText)
                }
            }
            .distinctUntilChanged()
            .map { projectList ->
                projectList.map {
                    it.toUiProject()
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    uiProjects.value = it
                },
                {
                    it.printStackTrace()
                    notificationManager.showNotification(
                        NotificationModel(
                            text = it.message ?: "Unknown error in while getting projects",
                            type = NotificationType.ERROR
                        )
                    )
                }
            )
    }

    /**
     * Subscribe on events from ui like as [UiEvent.Refresh] and data from the source of workspace.
     * Map pair of ui event and workspace to ui model for showing on activity
     *
     * @return [Disposable] for dispose of observer from source
     */
    private fun subscribeOnData(): Disposable {
        return Observables.combineLatest(
            getRefreshedSubject(),
            fetchWorkspaceInteractor.fetchWorkspaces()
                .map {
                    it.map { workspace ->
                        UiWorkspace(workspace.name)
                    }
                }
                .doAfterNext {
                    eventSubject.onNext(UiEvent.CompleteRefresh)
                }
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { (isFetched, workspaceList) ->
                    val title = context.getString(R.string.project_list_default_title)
                    uiModels.value =
                        UiMainViewModel(
                            title = title,
                            workspaces = workspaceList,
                            searchUiViewModel = HawkSearchUiViewModel(
                                hint = context.getString(R.string.search_hint),
                                text = "",
                                listener = {
                                    searchSubject.onNext(it)
                                }
                            ),
                            showLoading = isFetched
                        )
                },
                {
                    it.printStackTrace()
                    notificationManager.showNotification(
                        NotificationModel(
                            text = it.message ?: "Unknown error in while getting workspace",
                            type = NotificationType.ERROR
                        )
                    )
                }
            )
    }

    /**
     * Convert from [Project] to [UiProject] to display in the list
     * @return Ui model to display in the list
     */
    private fun Project.toUiProject(): UiProject {
        return UiProject(
            id,
            name,
            description,
            image,
            if (image.isBlank()) {
                SquircleDrawable(
                    Utils.createDefaultLogo(
                        context,
                        id,
                        name,
                        R.dimen.project_icon_side
                    )
                )
            } else {
                SquircleDrawable(Picasso.get().load(image).get())
            },
            unreadCount.toBadge()
        )
    }

    /**
     * Extension for converting from number to [UiBadgeViewModel]
     * @return Badge view model for current number
     */
    private fun Int.toBadge(): UiBadgeViewModel {
        if (this == 0) {
            return UiBadgeViewModel()
        }
        return UiBadgeViewModel(
            ShortNumberUtils.convert(this.toLong()),
            this.toLong()
        )
    }

    /**
     * Get source of ui event and map if ui events is [UiEvent.Refresh]
     *
     * @return Observable of Boolean, if we receive ui event for refresh data then source contains
     * true else false
     */
    private fun getRefreshedSubject(): Observable<Boolean> {
        return eventSubject
            .filter { event ->
                event is UiEvent.Refresh || event is UiEvent.CompleteRefresh
            }.map { refreshEvent ->
                refreshEvent is UiEvent.Refresh
            }
    }

    /**
     * Submit ui event to event source
     *
     * @param event is [UiEvent] for handle some of the action
     */
    fun submitEvent(event: UiEvent) {
        eventSubject.onNext(event)
    }

    /**
     * Dispose of all sources
     */
    override fun onCleared() {
        super.onCleared()
        disposable.dispose()
    }

    /**
     * Provide source of model
     */
    fun observeUiModels(): LiveData<UiMainViewModel> {
        return uiModels
    }

    /**
     * Provide source of ui project model
     */
    fun observeUiProjects(): LiveData<List<UiProject>> {
        return uiProjects
    }

    /**
     * Common class for ui events
     */
    sealed class UiEvent {
        /**
         * Ui event for refresh data (like as swipe to refresh)
         */
        object Refresh : UiEvent()

        /**
         * An ui event that indicates an refresh has been completed
         */
        object CompleteRefresh : UiEvent()
    }
}