package com.madt.lab6.weathermap.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.madt.lab6.weathermap.R
import com.madt.lab6.weathermap.databinding.ActivityAuthBinding
import com.madt.lab6.weathermap.ui.map.MapActivity

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    private var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            handleAuthAction()
        }

        binding.btnSwitchMode.setOnClickListener {
            toggleMode(!isRegisterMode)
        }
    }

    private fun toggleMode(register: Boolean) {
        isRegisterMode = register
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        if (isRegisterMode) {
            binding.tvFormHeader.setText(R.string.tab_register)
            binding.tilConfirmPassword.visibility = View.VISIBLE
            binding.btnSubmit.setText(R.string.btn_register)
            binding.btnSwitchMode.setText(R.string.msg_switch_to_login)
        } else {
            binding.tvFormHeader.setText(R.string.tab_login)
            binding.tilConfirmPassword.visibility = View.GONE
            binding.btnSubmit.setText(R.string.btn_login)
            binding.btnSwitchMode.setText(R.string.msg_switch_to_register)
        }
    }

    private fun handleAuthAction() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.err_empty_email)
            binding.etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.err_invalid_email)
            binding.etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.err_empty_password)
            binding.etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.err_short_password)
            binding.etPassword.requestFocus()
            return
        }

        if (isRegisterMode) {
            if (password != confirmPassword) {
                binding.tilConfirmPassword.error = getString(R.string.err_passwords_mismatch)
                binding.etConfirmPassword.requestFocus()
                return
            }
            viewModel.signUp(email, password)
        } else {
            viewModel.signIn(email, password)
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.progressBarAuth.visibility = View.VISIBLE
                    binding.btnSubmit.isEnabled = false
                    binding.btnSwitchMode.isEnabled = false
                }
                is AuthState.Authenticated -> {
                    binding.progressBarAuth.visibility = View.GONE
                    navigateToMap()
                }
                is AuthState.Error -> {
                    binding.progressBarAuth.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    binding.btnSwitchMode.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                is AuthState.SuccessMessage -> {
                    binding.progressBarAuth.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    binding.btnSwitchMode.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_SHORT).show()
                }
                is AuthState.Idle -> {
                    binding.progressBarAuth.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    binding.btnSwitchMode.isEnabled = true
                }
            }
        }
    }

    private fun navigateToMap() {
        val intent = Intent(this, MapActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
