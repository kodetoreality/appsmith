import React, { useEffect, useMemo, useState } from "react";
import { AnimatedGridLayout, LayoutArea } from "components/AnimatedGridLayout";
import Sidebar from "./Sidebar";
import LeftPane from "./LeftPane";
import MainPane from "./MainPane";
import RightPane from "./RightPane";
import {
  useCurrentAppState,
  useCurrentEditorState,
  useEditorPaneWidth,
} from "./hooks";
import { useSelector } from "react-redux";
import { getPropertyPaneWidth } from "selectors/propertyPaneSelectors";
import { previewModeSelector } from "selectors/editorSelectors";
import { getIDEViewMode } from "selectors/ideSelectors";
import {
  EditorEntityTab,
  EditorState,
  EditorViewMode,
} from "@appsmith/entities/IDE/constants";
import {
  APP_SETTINGS_PANE_WIDTH,
  SPLIT_SCREEN_RATIO,
} from "constants/AppConstants";
import EditorWrapperContainer from "../commons/EditorWrapperContainer";
import BottomBar from "components/BottomBar";
import useWindowDimensions from "../../../utils/hooks/useWindowDimensions";

const Areas = {
  Sidebar: "Sidebar",
  Explorer: "Explorer",
  CodeEditor: "CodeEditor",
  WidgetEditor: "WidgetEditor",
  PropertyPane: "PropertyPane",
  BottomBar: "BottomBar",
};

const SIDEBAR_WIDTH = "50px";

function useAppIDEAnimated(): [string[], string[], string[][]] {
  const areas = useMemo(
    () => [
      [Areas.Sidebar, Areas.Explorer, Areas.WidgetEditor, Areas.PropertyPane],
    ],
    [],
  );
  const [columns, setColumns] = useState<string[]>([]);
  const [rows] = useState<string[]>(["1fr"]);
  const LeftPaneWidth = useEditorPaneWidth();
  const [windowWidth] = useWindowDimensions();
  const PropertyPaneWidth = useSelector(getPropertyPaneWidth);
  const { segment } = useCurrentEditorState();
  const appState = useCurrentAppState();
  const isPreviewMode = useSelector(previewModeSelector);
  const editorMode = useSelector(getIDEViewMode);

  useEffect(() => {
    switch (appState) {
      case EditorState.DATA:
        setColumns([
          SIDEBAR_WIDTH,
          "300px",
          windowWidth - 50 - 300 + "px",
          "0px",
        ]);
        break;
      case EditorState.SETTINGS:
        setColumns([
          SIDEBAR_WIDTH,
          APP_SETTINGS_PANE_WIDTH + "px",
          windowWidth - 50 - APP_SETTINGS_PANE_WIDTH + "px",
          "0px",
        ]);
        break;
      case EditorState.LIBRARIES:
        setColumns([
          SIDEBAR_WIDTH,
          "250px",
          windowWidth - 50 - 250 + "px",
          "0px",
        ]);
        break;
      case EditorState.EDITOR:
        if (isPreviewMode) {
          setColumns(["0px", "0px", windowWidth + "px", "0px"]);
        } else if (segment !== EditorEntityTab.UI) {
          if (editorMode === EditorViewMode.SplitScreen) {
            setColumns([
              SIDEBAR_WIDTH,
              LeftPaneWidth,
              `${windowWidth - 50 - windowWidth * SPLIT_SCREEN_RATIO}px`,
              "0px",
            ]);
          } else {
            setColumns([SIDEBAR_WIDTH, windowWidth - 50 + "px", "0px", "0px"]);
          }
        } else {
          setColumns([
            SIDEBAR_WIDTH,
            "255px",
            windowWidth - 50 - 256 - PropertyPaneWidth + "px",
            PropertyPaneWidth + "px",
          ]);
        }
    }
  }, [
    appState,
    isPreviewMode,
    LeftPaneWidth,
    PropertyPaneWidth,
    segment,
    editorMode,
    windowWidth,
  ]);

  return [rows, columns, areas];
}

function AnimatedGridIDE() {
  const [rows, columns, areas] = useAppIDEAnimated();
  const isPreviewMode = useSelector(previewModeSelector);
  return (
    <>
      <EditorWrapperContainer>
        <AnimatedGridLayout
          areas={areas}
          columns={columns}
          height="100%"
          rows={rows}
          width="100vw"
        >
          <LayoutArea name={Areas.Sidebar}>
            <Sidebar />
          </LayoutArea>
          <LayoutArea name={Areas.Explorer}>
            <LeftPane />
          </LayoutArea>
          <LayoutArea name={Areas.WidgetEditor}>
            <MainPane id="app-body" />
          </LayoutArea>
          <LayoutArea name={Areas.PropertyPane}>
            <RightPane />
          </LayoutArea>
        </AnimatedGridLayout>
      </EditorWrapperContainer>
      <BottomBar viewMode={isPreviewMode} />
    </>
  );
}

export default AnimatedGridIDE;