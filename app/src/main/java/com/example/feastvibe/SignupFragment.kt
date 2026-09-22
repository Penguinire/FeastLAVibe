package com.example.feastvibe

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch

class SignupFragment : Fragment(R.layout.activity_signup) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private lateinit var inputFullName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputConfirmPassword: EditText
    private lateinit var buttonSignUp: MaterialButton
    private lateinit var progress: CircularProgressIndicator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        inputFullName = view.findViewById(R.id.input_full_name)
        inputEmail = view.findViewById(R.id.input_email)
        inputPassword = view.findViewById(R.id.input_password)
        inputConfirmPassword = view.findViewById(R.id.input_confirm_password)
        buttonSignUp = view.findViewById(R.id.button_sign_up)
        progress = view.findViewById(R.id.progress_signup)

        buttonSignUp.setOnClickListener { attemptSignup() }
        view.findViewById<TextView>(R.id.text_go_to_login).setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun attemptSignup() {
        val fullName = inputFullName.text.toString().trim()
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString()
        val confirmPassword = inputConfirmPassword.text.toString()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(context, "Passwords don't match.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val registerResult = authRepository.register(email, password)
            registerResult.onSuccess { firebaseUser ->
                val profile = UserProfile(uid = firebaseUser.uid, fullName = fullName, email = email)
                val profileResult = userRepository.createProfile(profile)
                setLoading(false)
                profileResult.onSuccess {
                    val navOptions = androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.exploreFragment, false)
                        .build()
                    findNavController().navigate(R.id.profileFragment, null, navOptions)
                }.onFailure { e ->
                    Toast.makeText(context, "Account created, but saving your profile failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }.onFailure { e ->
                setLoading(false)
                Toast.makeText(context, e.message ?: "Sign up failed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        buttonSignUp.isEnabled = !loading
    }
}