import React, { useCallback } from "react";
import type { HighlightInfo } from "./HighlightingCanvas";
import { HighlightingCanvas } from "./HighlightingCanvas";
import { getClosestHighlight } from "./utils";

type AnvilCanvasDraggingArenaProps = {
  canvasId: string;
  layoutId: string;
  deriveAllHighlightsFn: () => HighlightInfo[];
};

export const AnvilCanvasDraggingArena = (
  props: AnvilCanvasDraggingArenaProps,
) => {
  const { canvasId, deriveAllHighlightsFn, layoutId } = props;
  const allHighLights = deriveAllHighlightsFn();

  const onDrop = (renderedBlock: HighlightInfo) => {
    return renderedBlock;
    // dispatch appropriate action to update the widgets
    // if (isNewWidget) addNewWidgetToAnvilLayout(dropPayload, drawingBlocks);
    // else
    //   dispatch({
    //     type: ReduxActionTypes.ANVILLAYOUT_REORDER_WIDGETS,
    //     payload: {
    //       dropPayload,
    //       movedWidgets: selectedWidgets,
    //       parentId: widgetId,
    //       direction,
    //     },
    //   });
  };
  const renderOnMouseMove = useCallback(
    (e: MouseEvent) => {
      return getClosestHighlight(e, allHighLights);
    },
    [allHighLights],
  );
  return (
    <HighlightingCanvas
      canvasId={canvasId}
      layoutId={layoutId}
      onDrop={onDrop}
      renderOnMouseMove={renderOnMouseMove}
    />
  );
};
