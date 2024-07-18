import { selectAnvilWidget } from "layoutSystems/anvil/integrations/actions";
import { SELECT_ANVIL_WIDGET_CUSTOM_EVENT } from "layoutSystems/anvil/utils/constants";
import { useCallback, useEffect } from "react";
import { useDispatch } from "react-redux";
import { batchUpdateMultipleWidgetProperties } from "../../../../../actions/controlActions";

/**
 * This hook is used to select a widget in the Anvil Layout System
 * A custom event is dispatched by all widgets on click
 * This hook listens to that event and dispatches the select action
 *
 */
export function useSelectWidgetListener() {
  const dispatch = useDispatch();

  const handleClick = useCallback(
    function (e: any) {
      dispatch(selectAnvilWidget(e.detail.widgetId, e));
    },
    [selectAnvilWidget],
  );

  // TODO: Add TS types for event
  const handleWidgetEditText = useCallback(
    (event) => {
      dispatch(
        batchUpdateMultipleWidgetProperties([
          {
            widgetId: event.detail.widgetId,
            updates: {
              modify: {
                text: event.detail?.text,
              },
            },
          },
        ]),
      );
    },
    [dispatch],
  );

  // Register and unregister the listeners on the document.body
  useEffect(() => {
    document.body.addEventListener(
      SELECT_ANVIL_WIDGET_CUSTOM_EVENT,
      handleClick,
      true,
    );

    document.body.addEventListener(
      "WIDGET_EDIT_TEXT",
      handleWidgetEditText,
      true,
    );
    return () => {
      document.body.removeEventListener(
        SELECT_ANVIL_WIDGET_CUSTOM_EVENT,
        handleClick,
      );
    };
  }, [handleClick, handleWidgetEditText]);
}
