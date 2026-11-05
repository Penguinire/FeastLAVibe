package com.example.feastvibe

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.InputStream

class ProfileFragment : Fragment(R.layout.activity_profile) {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private var currentProfile: UserProfile? = null

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                saveProfilePhoto(uri)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setMenu(view, R.id.menu_reviews, "My Reviews")
        setMenu(view, R.id.menu_settings, "Settings")
        setMenu(view, R.id.menu_help, "Help & Support")

        val user = authRepository.currentUser

        if (user == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // Show basic Firebase Auth details immediately.
        view.findViewById<TextView>(R.id.tv_profile_name).text =
            user.displayName ?: (user.email?.substringBefore("@") ?: "Feast & Vibe user")

        view.findViewById<TextView>(R.id.tv_profile_email).text =
            user.email ?: ""

        // Load the detailed Firestore profile.
        viewLifecycleOwner.lifecycleScope.launch {
            val result = userRepository.getProfile(user.uid)

            if (result.isSuccess) {
                val profile = result.getOrNull()

                if (profile != null) {
                    currentProfile = profile
                    updateUI(view, profile)
                }
            } else {
                // Use basic account details if no Firestore profile is available.
                currentProfile = UserProfile(
                    uid = user.uid,
                    fullName = user.displayName
                        ?: (user.email?.substringBefore("@") ?: "Feast & Vibe user"),
                    email = user.email ?: ""
                )
            }
        }

        // Change profile photo.
        view.findViewById<MaterialButton>(R.id.btn_change_photo)
            .setOnClickListener {
                imagePicker.launch("image/*")
            }

        // Edit profile details.
        view.findViewById<MaterialButton>(R.id.btn_edit_profile)
            .setOnClickListener {
                showEditProfileDialog()
            }

        // Log out.
        view.findViewById<TextView>(R.id.tv_log_out)
            .setOnClickListener {
                authRepository.logout()

                val navController = findNavController()

                if (!navController.popBackStack(R.id.exploreFragment, false)) {
                    navController.navigate(R.id.exploreFragment)
                }
            }
    }

    private fun updateUI(view: View, profile: UserProfile) {
        view.findViewById<TextView>(R.id.tv_profile_name).text =
            profile.fullName

        view.findViewById<TextView>(R.id.tv_profile_email).text =
            profile.email

        val date = SimpleDateFormat(
            "yyyy",
            Locale.getDefault()
        ).format(Date(profile.memberSince))

        val meta = if (profile.location.isNotEmpty()) {
            "${profile.location} · Member since $date"
        } else {
            "Member since $date"
        }

        view.findViewById<TextView>(R.id.tv_profile_meta).text = meta

        view.findViewById<TextView>(R.id.tv_reviews_count).text =
            profile.reviewCount.toString()

        view.findViewById<TextView>(R.id.tv_favourites_count).text =
            profile.favouritesCount.toString()

        view.findViewById<TextView>(R.id.tv_visited_count).text =
            profile.visitedCount.toString()

        // Load saved profile photo.
        if (profile.photoBase64.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(
                    profile.photoBase64,
                    Base64.DEFAULT
                )

                val bitmap = BitmapFactory.decodeByteArray(
                    imageBytes,
                    0,
                    imageBytes.size
                )

                if (bitmap != null) {
                    view.findViewById<ShapeableImageView>(
                        R.id.iv_profile_photo
                    ).setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // Keep the default profile image if decoding fails.
            }
        }
    }

    private fun saveProfilePhoto(uri: Uri) {
        val user = authRepository.currentUser ?: return
        val profile = currentProfile ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Toast.makeText(
                    requireContext(),
                    "Processing image...",
                    Toast.LENGTH_SHORT
                ).show()

                val imageBytes = withContext(Dispatchers.IO) {
                    compressImage(uri)
                }

                val encodedImage = Base64.encodeToString(
                    imageBytes,
                    Base64.NO_WRAP
                )

                profile.photoBase64 = encodedImage
                profile.uid = user.uid

                val result = userRepository.updateProfile(profile)

                if (result.isSuccess) {
                    currentProfile = profile
                    updateUI(requireView(), profile)

                    Toast.makeText(
                        requireContext(),
                        "Profile photo updated!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"

                    Toast.makeText(
                        requireContext(),
                        "Photo save failed: $error",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Image error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun compressImage(uri: Uri): ByteArray {
        val resolver = requireContext().contentResolver

        // First read image dimensions without loading the full image.
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        openImageStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: throw Exception("Android could not access this image.")

        // Reduce large images before decoding them.
        var sampleSize = 1

        while (
            bounds.outWidth / sampleSize > 1024 ||
            bounds.outHeight / sampleSize > 1024
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        val bitmap = openImageStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw Exception("Could not decode the selected image.")

        // Resize to a maximum dimension of 512 pixels.
        val maxDimension = 512

        val scale = minOf(
            1f,
            maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
        )

        val resizedBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }

        // Compress until the image is small enough for Firestore.
        var quality = 85
        var imageBytes: ByteArray

        do {
            val output = ByteArrayOutputStream()

            resizedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                output
            )

            imageBytes = output.toByteArray()
            quality -= 10

        } while (imageBytes.size > 450_000 && quality >= 35)

        if (bitmap !== resizedBitmap) {
            bitmap.recycle()
        }

        if (imageBytes.size > 450_000) {
            resizedBitmap.recycle()
            throw Exception("Image is too large. Please choose a smaller image.")
        }

        resizedBitmap.recycle()

        return imageBytes
    }

    private fun showEditProfileDialog() {
        val profile = currentProfile ?: return

        val context = requireContext()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
        }

        val nameInput = EditText(context).apply {
            hint = "Full name"
            setText(profile.fullName)
        }

        val locationInput = EditText(context).apply {
            hint = "Location (e.g. Pretoria)"
            setText(profile.location)
        }

        layout.addView(nameInput)
        layout.addView(locationInput)

        AlertDialog.Builder(context)
            .setTitle("Edit Profile")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->

                val newName = nameInput.text.toString().trim()
                val newLocation = locationInput.text.toString().trim()

                if (newName.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Please enter your name.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setPositiveButton
                }

                profile.fullName = newName
                profile.location = newLocation

                viewLifecycleOwner.lifecycleScope.launch {
                    val result = userRepository.updateProfile(profile)

                    if (result.isSuccess) {
                        currentProfile = profile
                        updateUI(requireView(), profile)

                        Toast.makeText(
                            requireContext(),
                            "Profile updated!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"

                        Toast.makeText(
                            requireContext(),
                            "Profile update failed: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun setMenu(root: View, id: Int, label: String) {
        root.findViewById<View>(id)
            .findViewById<TextView>(R.id.tv_menu_label)
            .text = label
    }

    private fun openImageStream(uri: Uri): InputStream? {
        val resolver = requireContext().contentResolver

        return resolver.openInputStream(uri)
            ?: resolver.openAssetFileDescriptor(uri, "r")?.createInputStream()
    }
}