package com.boodschappen.app.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.boodschappen.app.MainActivity
import com.boodschappen.app.data.local.ShoppingDatabase

class BoodschappenWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = ShoppingDatabase.getDatabase(context)
        val items = db.shoppingDao().getUncheckedItemsForWidget()

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1040))
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "🛒 Boodschappen",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(6.dp))
                if (items.isEmpty()) {
                    Text(
                        text = "Je lijst is leeg",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF9CA3AF)),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    items.forEach { item ->
                        Text(
                            text = "• ${item.name}",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 12.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
