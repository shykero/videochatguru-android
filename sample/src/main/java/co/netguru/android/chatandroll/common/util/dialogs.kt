package co.netguru.android.chatandroll.common.util

import android.app.Activity
import android.app.Dialog
import co.netguru.android.chatandroll.R
import kotlinx.android.synthetic.main.activity_hang_up.view.*


fun createHangUpCallDialog (activityContext: Activity, callback: (selectedValue: CallEvent?) -> Unit
): Dialog {
    val dialog = Dialog(activityContext, R.style.CallDialogTheme)
    val view = activityContext.layoutInflater.inflate(R.layout.activity_hang_up, null)

    view.hangup_call_button.setOnClickListener {
        callback(CallEvent.HangUpClickedEvent)
    }

    //    view.btn_subscribe_monthly.text = premiumInAppSkuDetails.price
    view.cancel_call_button.setOnClickListener {
        callback(CallEvent.HangDownClickedEvent)
    }

    dialog.setContentView(view)
    return dialog
}

