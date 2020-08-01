package co.netguru.android.chatandroll.feature.call

import android.os.Bundle
import co.netguru.android.chatandroll.R
import co.netguru.android.chatandroll.feature.base.BaseActivity


class CallActivity : BaseActivity() {

//    var videoFragment = VideoCallFragment

    override fun getLayoutId() = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = intent.getStringExtra("userId")
        val firsname = intent.getStringExtra("firstname")
        val surname = intent.getStringExtra("surname")
        val profileImageUrl = intent.getStringExtra("profileImageUrl")
        val isOffer = intent.getBooleanExtra("isOffer", true)
        val videoFragment = VideoCallFragment.newInstance(isOffer, userId, firsname, surname, profileImageUrl)
            getReplaceFragmentTransaction(R.id.fragmentContainer, videoFragment, VideoCallFragment.TAG).commit()
    }
}