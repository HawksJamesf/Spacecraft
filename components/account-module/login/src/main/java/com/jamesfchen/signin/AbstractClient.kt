package com.jamesfchen.signin

import com.jamesfchen.event.SignInFailedEvent
import com.jamesfchen.event.SignUpFailedEvent
import com.jamesfchen.modle.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * Copyright ® $ 2017
 * All right reserved.
 *
 * @author: hawks.jamesf
 * @since: Oct/24/2018  Wed
 */
@Deprecated("")
abstract class AbstractClient {

    //public API
    abstract val state: ClientState
    abstract val stateObservable: Observable<ClientState>
    abstract val signInFailedEventObservable: Observable<SignInFailedEvent>
    abstract val signUpFailedEventObservable: Observable<SignUpFailedEvent>

    abstract fun signUp(signUpReq: SignUpReqBody): Single<Profile>

    abstract fun signIn(signInReq: SignInReqBody): Single<Profile>

    abstract fun signOut()

    abstract fun sendCode(sendCodeReq: SendCodeReqBody): Single<SendCodeRespBody>
}


@Deprecated("")
abstract class ObservableClient : AbstractClient() {
    //private API
    private val stateDataSubject = BehaviorSubject.createDefault<StateData>(StateData())
    private val signInFailedEventSubject = PublishSubject.create<SignInFailedEvent>()
    private val signUpFailedEventSubject = PublishSubject.create<SignUpFailedEvent>()

    fun emitSignInFailedEvent(mobile: String, password: String, excep: ClientException) = signInFailedEventSubject.onNext(SignInFailedEvent(mobile, password, excep))
    fun emitSignUpFailedEvent(mobile: String, password: String, excep: ClientException) = signUpFailedEventSubject.onNext(SignUpFailedEvent(mobile, password, excep))

    //当登入/注册时，改变stateData的值时，通过stateDataSubject将登入/注册d的状态发给ui层从而更新ui
    var stateData: StateData
        get() = stateDataSubject.value!!
        set(value) = stateDataSubject.onNext(value)


    //public API
    override val stateObservable: Observable<ClientState>
        get() = stateDataSubject.map { it.state }
                .distinctUntilChanged()

    override val signInFailedEventObservable: Observable<SignInFailedEvent>
        get() = signInFailedEventSubject

    override val signUpFailedEventObservable: Observable<SignUpFailedEvent>
        get() = signUpFailedEventSubject

    override val state: ClientState
        get() = stateDataSubject.value?.state!!

}

