package cm.momoshield.app.service

import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.TileService
import android.os.Build
import androidx.annotation.RequiresApi
import cm.momoshield.app.MainActivity

@RequiresApi(Build.VERSION_CODES.N)
class MoMoShieldTileService : TileService() {
    @Suppress("DEPRECATION", "NewApi")
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }
}
