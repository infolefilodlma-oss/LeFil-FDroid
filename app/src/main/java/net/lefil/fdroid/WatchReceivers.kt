package net.lefil.fdroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        if(intent.action==Intent.ACTION_BOOT_COMPLETED || intent.action==Intent.ACTION_MY_PACKAGE_REPLACED)
            MessageWatchService.start(context)
    }
}
class PollAlarmReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {MessageWatchService.pollNow(context)}
}
