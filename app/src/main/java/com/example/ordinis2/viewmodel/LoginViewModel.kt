package com.example.ordinis2.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ordinis2.data.local.AppDatabase
import com.example.ordinis2.data.local.User
import com.example.ordinis2.data.local.UserDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt
import java.lang.IllegalArgumentException

class LoginViewModel(private val userDao: UserDao) : ViewModel() { // Removed Application, use DAO
    private val _loginResult = MutableStateFlow<String?>(null)
    val loginResult: StateFlow<String?> = _loginResult

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = "Loading" // Set a loading state
            try {
                val user = userDao.getUserByUsername(username)
                if (user != null && BCrypt.checkpw(password, user.passwordHash)) {
                    _loginResult.value = "Login successful"
                    // Potentially store user session information here (e.g., in a SavedStateHandle)
                } else {
                    _loginResult.value = "Invalid username or password"
                }
            } catch (e: Exception) {
                _loginResult.value = "Error: ${e.message}" // Handle database errors
            }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _loginResult.value = "Loading"  // Set a loading state
            try {
                if (userDao.getUserByUsername(username) == null) {
                    val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
                    val newUser = User(username = username, passwordHash = hashedPassword)
                    userDao.insert(newUser)
                    _loginResult.value = "Registration successful. You can now log in."
                } else {
                    _loginResult.value = "Username already exists"
                }
            } catch (e: Exception) {
                _loginResult.value = "Error: ${e.message}" // Handle database errors
            }
        }
    }

    fun clearLoginResult() {
        _loginResult.value = null
    }

    // Factory for creating LoginViewModel with dependencies
    class LoginViewModelFactory(private val userDao: UserDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(userDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
