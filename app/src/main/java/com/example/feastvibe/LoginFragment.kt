package com.example.feastvibe

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.activity_login) {

    private val authRepository = AuthRepository()
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var buttonSignIn: MaterialButton
    private lateinit var progress: CircularProgressIndicator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        inputEmail = view.findViewById(R.id.input_email)
        inputPassword = view.findViewById(R.id.input_password)
        buttonSignIn = view.findViewById(R.id.button_sign_in)
        progress = view.findViewById(R.id.progress_login)

        buttonSignIn.setOnClickListener { attemptLogin() }
        view.findViewById<TextView>(R.id.text_go_to_signup).setOnClickListener {
            findNavController().navigate(R.id.signupFragment)
        }
        view.findViewById<TextView>(R.id.text_forgot_password).setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        if (authRepository.isLoggedIn) {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun attemptLogin() {
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            val result = authRepository.login(email, password)
            setLoading(false)
            result.onSuccess {
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.exploreFragment, false)
                    .build()
                findNavController().navigate(R.id.profileFragment, null, navOptions)
            }.onFailure { e ->
                Toast.makeText(context, e.message ?: "Login failed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val emailField = EditText(requireContext()).apply {
            hint = "Email address"
            setText(inputEmail.text.toString())
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Reset Password")
            .setMessage("We'll email you a link to reset your password.")
            .setView(emailField)
            .setPositiveButton("Send") { _, _ ->
                val email = emailField.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(context, "Enter your email first.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val result = authRepository.sendPasswordResetEmail(email)
                    result.onSuccess {
                        Toast.makeText(context, "Reset email sent. Check your inbox.", Toast.LENGTH_LONG).show()
                    }.onFailure { e ->
                        Toast.makeText(context, e.message ?: "Couldn't send reset email.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        buttonSignIn.isEnabled = !loading
    }
}