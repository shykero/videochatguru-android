package co.netguru.android.chatandroll.feature.call

import co.netguru.android.chatandroll.common.di.FragmentScope
import dagger.Subcomponent

@FragmentScope
@Subcomponent
interface VideoCallFragmentComponent {
    fun inject(videoCallFragment: VideoCallFragment)

    fun videoCallFragmentPresenter(): VideoCallFragmentPresenter
}