package co.netguru.android.chatandroll.feature.call

import androidx.coordinatorlayout.widget.CoordinatorLayout
import android.view.Gravity
import android.view.View


class MoveUpCallBehavior : CoordinatorLayout.Behavior<View>() {

    override fun onAttachedToLayoutParams(lp: CoordinatorLayout.LayoutParams) {
        if (lp.dodgeInsetEdges == Gravity.NO_GRAVITY) {
            lp.dodgeInsetEdges = Gravity.BOTTOM
        }
    }
}