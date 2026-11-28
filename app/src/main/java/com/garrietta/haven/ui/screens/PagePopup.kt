package com.garrietta.haven.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.garrietta.haven.ui.components.DialogTitleText
import com.garrietta.haven.ui.components.LocalHeroTarget
import com.garrietta.haven.ui.components.LocalPopupDragHandle
import com.garrietta.haven.ui.components.LocalPopupDragRangePx
import com.garrietta.haven.ui.components.popupDragDetector

/**
 * The header of a page opened as a popup (Climate, Security): its title,
 * which the finger can drag down to close the popup, as a device popup's
 * header can. A page popup has no badge, so nothing travels to it.
 */
@Composable
fun PagePopupHeader(title: String) {
    val dragHandle = LocalPopupDragHandle.current
    val dragRangePx = LocalPopupDragRangePx.current
    val heroTarget = LocalHeroTarget.current
    Row(
        Modifier.fillMaxWidth()
            .onGloballyPositioned { heroTarget?.invoke(it.boundsInRoot()) }
            .then(
                if (dragHandle != null) {
                    Modifier.popupDragDetector(
                        dragHandle.state,
                        { dy -> dragHandle.onDelta(dy, dragRangePx()) },
                        { v -> dragHandle.onRelease(v, dragRangePx()) },
                    )
                } else {
                    Modifier
                },
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DialogTitleText(title, Modifier.fillMaxWidth())
    }
}
