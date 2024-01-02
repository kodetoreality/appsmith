import { fetchMockDatasources } from "actions/datasourceActions";
import {
  fetchGitRemoteStatusInit,
  fetchGitProtectedBranchesInit,
  fetchGitStatusInit,
  remoteUrlInputValue,
  resetPullMergeStatus,
  fetchBranchesInit,
  startAutocommitProgressPolling,
  getGitMetadataInitAction,
} from "actions/gitSyncActions";
import { restoreRecentEntitiesRequest } from "actions/globalSearchActions";
import { resetEditorSuccess } from "actions/initActions";
import { loadGuidedTourInit } from "actions/onboardingActions";
import { fetchAllPageEntityCompletion, setupPage } from "actions/pageActions";
import {
  executePageLoadActions,
  fetchActions,
} from "actions/pluginActionActions";
import { fetchPluginFormConfigs } from "actions/pluginActions";
import type { ApplicationPayload } from "@appsmith/constants/ReduxActionConstants";
import {
  ReduxActionErrorTypes,
  ReduxActionTypes,
} from "@appsmith/constants/ReduxActionConstants";
import { addBranchParam } from "constants/routes";
import type { APP_MODE } from "entities/App";
import { call, fork, put, select, spawn } from "redux-saga/effects";
import type { EditConsolidatedApi } from "sagas/InitSagas";
import {
  failFastApiCalls,
  reportSWStatus,
  waitForWidgetConfigBuild,
} from "sagas/InitSagas";
import { getCurrentApplication } from "selectors/editorSelectors";
import {
  getCurrentGitBranch,
  getIsGitStatusLiteEnabled,
} from "selectors/gitSyncSelectors";
import AnalyticsUtil from "utils/AnalyticsUtil";
import history from "utils/history";
import PerformanceTracker, {
  PerformanceTransactionName,
} from "utils/PerformanceTracker";
import type { AppEnginePayload } from ".";
import AppEngine, {
  ActionsNotFoundError,
  PluginFormConfigsNotFoundError,
  PluginsNotFoundError,
} from ".";
import { fetchJSLibraries } from "actions/JSLibraryActions";
import CodemirrorTernService from "utils/autocomplete/CodemirrorTernService";
import {
  waitForSegmentInit,
  waitForFetchUserSuccess,
} from "@appsmith/sagas/userSagas";
import { getFirstTimeUserOnboardingComplete } from "selectors/onboardingSelectors";
import { isAirgapped } from "@appsmith/utils/airgapHelpers";
import { getAIPromptTriggered } from "utils/storage";
import { trackOpenEditorTabs } from "../../utils/editor/browserTabsTracking";
import { EditorModes } from "components/editorComponents/CodeEditor/EditorConfig";
import { waitForFetchEnvironments } from "@appsmith/sagas/EnvironmentSagas";
import { getPageDependencyActions } from "@appsmith/entities/Engine/actionHelpers";
import { fetchJSCollections } from "actions/jsActionActions";
import {
  fetchAppThemesAction,
  fetchSelectedAppThemeAction,
} from "actions/appThemingActions";

export default class AppEditorEngine extends AppEngine {
  constructor(mode: APP_MODE) {
    super(mode);
    this.setupEngine = this.setupEngine.bind(this);
    this.loadAppData = this.loadAppData.bind(this);
    this.loadAppURL = this.loadAppURL.bind(this);
    this.loadAppEntities = this.loadAppEntities.bind(this);
    this.loadGit = this.loadGit.bind(this);
    this.completeChore = this.completeChore.bind(this);
    this.loadPageThemesAndActions = this.loadPageThemesAndActions.bind(this);
    this.loadPluginsAndDatasources = this.loadPluginsAndDatasources.bind(this);
  }

  /**
   * this saga is called once then application is loaded.
   * It will hold the editor in uninitialized till all the apis/actions are completed
   *
   * @param AppEnginePayload
   * @returns
   */
  public *setupEngine(payload: AppEnginePayload): any {
    yield* super.setupEngine.call(this, payload);
    yield put(resetEditorSuccess());
    CodemirrorTernService.resetServer();
  }

  public startPerformanceTracking() {
    PerformanceTracker.startAsyncTracking(
      PerformanceTransactionName.INIT_EDIT_APP,
    );
  }

  public stopPerformanceTracking() {
    PerformanceTracker.stopAsyncTracking(
      PerformanceTransactionName.INIT_EDIT_APP,
    );
  }

  private *loadPageThemesAndActions(
    toLoadPageId: string,
    applicationId: string,
    allResponses: EditConsolidatedApi,
  ) {
    const {
      v1ActionsResp,
      v1CollectionsActionsResp,
      v1LibrariesApplicationResp,
      v1PageResp,
      v1ThemesApplicationCurrentModeResp,
      v1ThemesResp,
    } = allResponses;
    const initActionsCalls = [
      // check from the feature flags response for release_server_dsl_migrations_enabled
      // if it is true or truthy set the migrateDSL parameter to true
      // v1/page/:pageId?migrateDSL=!!release_server_dsl_migrations_enabled
      // tie response to v1PageResp
      setupPage(toLoadPageId, true, v1PageResp),
      // params applicationId
      // v1/actions?applicationId=someApplicationId
      // tie response to v1ActionsResp
      fetchActions({ applicationId, v1ActionsResp }, []),
      // params applicationId
      // v1/collections/actions?applicationId=someApplicationId
      // tie response to v1CollectionsActionsResp
      fetchJSCollections({ applicationId, v1CollectionsActionsResp }),
      // pathVariable applicationId
      // v1/themes/applications/:applicationId/current?mode=EDIT
      // tie response to v1ThemesApplicationCurrentModeResp
      fetchSelectedAppThemeAction(
        applicationId,
        v1ThemesApplicationCurrentModeResp,
      ),
      // pathVariable applicationId
      // v1/themes/applications/:applicationId
      // tie response to v1ThemesApplicationsResp
      fetchAppThemesAction(applicationId, v1ThemesResp),
    ];

    const successActionEffects = [
      ReduxActionTypes.FETCH_JS_ACTIONS_SUCCESS,
      ReduxActionTypes.FETCH_ACTIONS_SUCCESS,
      ReduxActionTypes.FETCH_APP_THEMES_SUCCESS,
      ReduxActionTypes.FETCH_SELECTED_APP_THEME_SUCCESS,
      ReduxActionTypes.SETUP_PAGE_SUCCESS,
    ];

    const failureActionEffects = [
      ReduxActionErrorTypes.FETCH_JS_ACTIONS_ERROR,
      ReduxActionErrorTypes.FETCH_ACTIONS_ERROR,
      ReduxActionErrorTypes.FETCH_APP_THEMES_ERROR,
      ReduxActionErrorTypes.FETCH_SELECTED_APP_THEME_ERROR,
      ReduxActionErrorTypes.SETUP_PAGE_ERROR,
    ];

    initActionsCalls.push(
      // pathVariable applicationId
      // "v1/libraries/:applicationId
      // tie response to v1LibrariesApplicationResp
      fetchJSLibraries(applicationId, v1LibrariesApplicationResp),
    );
    successActionEffects.push(ReduxActionTypes.FETCH_JS_LIBRARIES_SUCCESS);

    const allActionCalls: boolean = yield call(
      failFastApiCalls,
      initActionsCalls,
      successActionEffects,
      failureActionEffects,
    );

    if (!allActionCalls)
      throw new ActionsNotFoundError(
        `Unable to fetch actions for the application: ${applicationId}`,
      );

    yield call(waitForFetchUserSuccess);
    yield call(waitForSegmentInit, true);
    yield call(waitForFetchEnvironments);
    yield put(fetchAllPageEntityCompletion([executePageLoadActions()]));
  }

  private *loadPluginsAndDatasources(allResponses: EditConsolidatedApi) {
    const { v1DatasourcesMockResp, v1PluginFormConfigsResp } =
      allResponses || {};
    const isAirgappedInstance = isAirgapped();

    const { errorActions, initActions, successActions } =
      getPageDependencyActions(allResponses);

    if (!isAirgappedInstance) {
      // v1/datasources/mocks
      // tie response to v1DatasourcesMockResp
      initActions.push(fetchMockDatasources(v1DatasourcesMockResp));

      successActions.push(ReduxActionTypes.FETCH_MOCK_DATASOURCES_SUCCESS);
      errorActions.push(ReduxActionErrorTypes.FETCH_MOCK_DATASOURCES_ERROR);
    }

    const initActionCalls: boolean = yield call(
      failFastApiCalls,
      initActions,
      successActions,
      errorActions,
    );

    if (!initActionCalls)
      throw new PluginsNotFoundError("Unable to fetch plugins");

    const pluginFormCall: boolean = yield call(
      failFastApiCalls,
      //  pluginIds are the combination of the following
      //  1.are all unique plugins returned by the datasources call made earlier
      //  2.if an API plugin is present in the plugins call made earlier
      //  3.if a graphQl plugin is present in the plugins call made earlier
      //  4.are all plugins returned by the actions calls made earlier
      // for the above pluginIds make the fetch calls with the following api
      // v1/plugins/:pluginId/form
      // please attach a plugin id to each element in the collection
      // tie response to v1PluginFormConfigsResp
      [fetchPluginFormConfigs(v1PluginFormConfigsResp)],
      [ReduxActionTypes.FETCH_PLUGIN_FORM_CONFIGS_SUCCESS],
      [ReduxActionErrorTypes.FETCH_PLUGIN_FORM_CONFIGS_ERROR],
    );
    if (!pluginFormCall)
      throw new PluginFormConfigsNotFoundError(
        "Unable to fetch plugin form configs",
      );
  }

  public *loadAppEntities(
    toLoadPageId: string,
    applicationId: string,
    allResponses: EditConsolidatedApi,
  ): any {
    yield call(
      this.loadPageThemesAndActions,
      toLoadPageId,
      applicationId,
      allResponses,
    );
    yield call(this.loadPluginsAndDatasources, allResponses);
  }

  public *completeChore() {
    const isFirstTimeUserOnboardingComplete: boolean = yield select(
      getFirstTimeUserOnboardingComplete,
    );
    const currentApplication: ApplicationPayload = yield select(
      getCurrentApplication,
    );

    const [isAnotherEditorTabOpen, currentTabs] = yield call(
      trackOpenEditorTabs,
      currentApplication.id,
    );

    if (currentApplication) {
      AnalyticsUtil.logEvent("EDITOR_OPEN", {
        appId: currentApplication.id,
        appName: currentApplication.name,
        isAnotherEditorTabOpen,
        currentTabs,
      });
    }
    yield put(loadGuidedTourInit());
    if (isFirstTimeUserOnboardingComplete) {
      yield put({
        type: ReduxActionTypes.SET_FIRST_TIME_USER_ONBOARDING_APPLICATION_IDS,
        payload: [],
      });
    }

    const noOfTimesAIPromptTriggered: number = yield getAIPromptTriggered(
      EditorModes.TEXT_WITH_BINDING,
    );

    yield put({
      type: ReduxActionTypes.UPDATE_AI_TRIGGERED,
      payload: {
        value: noOfTimesAIPromptTriggered,
        mode: EditorModes.TEXT_WITH_BINDING,
      },
    });

    const noOfTimesAIPromptTriggeredForQuery: number =
      yield getAIPromptTriggered(EditorModes.POSTGRESQL_WITH_BINDING);

    yield put({
      type: ReduxActionTypes.UPDATE_AI_TRIGGERED,
      payload: {
        value: noOfTimesAIPromptTriggeredForQuery,
        mode: EditorModes.POSTGRESQL_WITH_BINDING,
      },
    });

    yield call(waitForWidgetConfigBuild);
    yield spawn(reportSWStatus);

    yield put({
      type: ReduxActionTypes.INITIALIZE_EDITOR_SUCCESS,
    });
  }

  public *loadGit(applicationId: string) {
    const branchInStore: string = yield select(getCurrentGitBranch);
    yield put(
      restoreRecentEntitiesRequest({
        applicationId,
        branch: branchInStore,
      }),
    );
    // init of temporary remote url from old application
    yield put(remoteUrlInputValue({ tempRemoteUrl: "" }));
    // add branch query to path and fetch status
    if (branchInStore) {
      history.replace(addBranchParam(branchInStore));
      yield fork(this.loadGitInBackground);
    }
  }

  private *loadGitInBackground() {
    const isGitStatusLiteEnabled: boolean = yield select(
      getIsGitStatusLiteEnabled,
    );

    yield put(fetchBranchesInit());
    yield put(fetchGitProtectedBranchesInit());
    yield put(fetchGitProtectedBranchesInit());
    yield put(getGitMetadataInitAction());

    if (isGitStatusLiteEnabled) {
      yield put(fetchGitRemoteStatusInit());
      yield put(fetchGitStatusInit({ compareRemote: false }));
    } else {
      yield put(fetchGitStatusInit({ compareRemote: true }));
    }

    yield put(startAutocommitProgressPolling());
    yield put(resetPullMergeStatus());
  }
}
