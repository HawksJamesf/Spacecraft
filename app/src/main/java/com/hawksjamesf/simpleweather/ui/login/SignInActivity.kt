package com.hawksjamesf.simpleweather.ui.login

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.hawksjamesf.simpleweather.R
import com.hawksjamesf.simpleweather.data.bean.login.ClientState
import com.hawksjamesf.simpleweather.data.bean.login.SignInReq
import com.hawksjamesf.simpleweather.ui.BaseActivity
import com.hawksjamesf.simpleweather.ui.home.HomeActivity
import com.hawksjamesf.simpleweather.ui.mvp.AutoDisposable
import com.hawksjamesf.simpleweather.util.hideSoftInput
import com.hawksjamesf.simpleweather.util.openActivity
import com.hawksjamesf.simpleweather.util.subscribeBy
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.visibility
import com.jakewharton.rxbinding2.widget.editorActions
import com.jakewharton.rxbinding2.widget.text
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.rxkotlin.Observables
import kotlinx.android.synthetic.main.activity_signin.*

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Oct/23/2018  Tue
 */
class SignInActivity : BaseActivity() {

    override fun initComponent(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_signin)
    }

    override fun handleCallback(autoDisposable: AutoDisposable) {
        tv_signup.clicks()
                .doOnNext { tv_signup.isPressed = true }
                .subscribe({
                    openActivity<SignUpActivity>(false)
                }, { }, {}).autoDisposable()

        Observables.combineLatest(atv_mobile.textChanges(), et_password.textChanges())
                .map { isValidate(it.first, it.second) }
                .subscribe { bt_sign_in.isEnabled = it }
                .autoDisposable()

        et_password.editorActions()
                .filter { it == EditorInfo.IME_ACTION_DONE }
                .map { Unit }
                .mergeWith(bt_sign_in.clicks())
                .publish().apply {
                    subscribe { hideSoftInput() }
                    filter { isValidate(atv_mobile.text.toString(), et_password.text.toString()) }
                            .subscribe { signin(SignInReq(atv_mobile.text.toString(), et_password.text.toString())) }

                }.connect(autoDisposable)


        onBackPress.map { client.state == ClientState.SIGNING_IN }
                .publish().apply {
                    filter { it }.subscribe { client.signOut() }.autoDisposable()
                    filter { !it }.subscribe { navigateBack() }.autoDisposable()

                }.connect(autoDisposable)

    }

    private fun isValidate(mobile: CharSequence, password: CharSequence) = mobile.isNotBlank() && password.isNotBlank()

    fun signin(signInReq: SignInReq) {
        client.signIn(signInReq)
                .subscribeBy(
                        onSubscribe = { client.stateData = StateData(signinginDisposable = it) },
                        onSuccess = {
                            client.stateData = StateData(profile = it)
                            //todo:将新的Profile存入到数据库
                        },
                        onError = {
                            client.stateData = StateData()
                        }

                )

    }

    override fun loadData(autoDisposable: AutoDisposable) {
    }

    override fun onResume(autoDisposable: AutoDisposable) {
        super.onResume(autoDisposable)
        client.stateObservable.publish().apply {
            map { it == ClientState.SIGNING_IN }.subscribe { pb_signin_progress.visibility() }.autoDisposable()
            map { it == ClientState.SIGNED_OUT }.subscribe { ll_signin.visibility() }.autoDisposable()
            filter { it == ClientState.SIGNED_IN }.subscribe { openActivity<HomeActivity>() }.autoDisposable()
            filter { it == ClientState.SIGNING_UP }.subscribe { openActivity<SignUpActivity>(false) }.autoDisposable()

        }.connect(autoDisposable)

        client.signInFailedEventObservable.publish().apply {
            map { it.mobile }.subscribe { atv_mobile.text() }.autoDisposable()
            map { it.password }.subscribe { et_password.text() }.autoDisposable()
            map { it.excep }.subscribe { Toast.makeText(this@SignInActivity, "$it", Toast.LENGTH_SHORT).show() }
        }.connect(autoDisposable)
    }

}