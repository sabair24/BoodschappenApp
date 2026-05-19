package com.boodschappen.app.ui.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class BoodschappenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BoodschappenWidget()
}
