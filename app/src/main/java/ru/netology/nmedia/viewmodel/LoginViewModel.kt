package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.netology.nmedia.model.LoginState

class LoginViewModel : ViewModel() {
    private val _dataLogin = MutableLiveData(LoginState())
    val dataLogin: LiveData<LoginState>
        get() = _dataLogin

    fun singIn(login: String, pass: String) {
        _dataLogin.value = LoginState(login, pass)
        //TODO
    }
}