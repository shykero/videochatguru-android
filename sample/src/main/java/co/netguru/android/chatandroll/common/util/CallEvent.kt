package co.netguru.android.chatandroll.common.util

sealed class CallEvent {
    object HangUpClickedEvent: CallEvent()
    object HangDownClickedEvent: CallEvent()
}