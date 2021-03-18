package ua.andrii.andrushchenko.gimmepictures.ui.auth

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.SupportRSBlurTransformation
import ua.andrii.andrushchenko.gimmepictures.GlideApp
import ua.andrii.andrushchenko.gimmepictures.R
import ua.andrii.andrushchenko.gimmepictures.data.auth.AuthRepository.Companion.unsplashAuthCallback
import ua.andrii.andrushchenko.gimmepictures.databinding.ActivityAuthBinding
import ua.andrii.andrushchenko.gimmepictures.util.ApiCallResult
import ua.andrii.andrushchenko.gimmepictures.util.customtabs.CustomTabsHelper

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        with(binding) {
            toolbar.setNavigationOnClickListener { finish() }
            viewModel.backgroundPhoto.observe(this@AuthActivity) { photo ->
                GlideApp.with(this@AuthActivity)
                    .load(photo.urls.small)
                    .placeholder(ColorDrawable(Color.parseColor(photo.color)))
                    .transition(DrawableTransitionOptions.withCrossFade(350))
                    .apply(RequestOptions.bitmapTransform(SupportRSBlurTransformation()))
                    .into(bgImage)
                    .clearOnDetach()
            }

            btnLogin.setOnClickListener {
                openUnsplashLoginTab()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.data?.let { uri ->
            if (uri.authority.equals(unsplashAuthCallback)) {
                uri.getQueryParameter("code")?.let { code ->
                    viewModel.getAccessToken(code).observe(this) {
                        when (it) {
                            is ApiCallResult.Loading -> {
                                binding.authProgress.visibility = View.VISIBLE
                            }
                            is ApiCallResult.Success -> {
                                binding.authProgress.visibility = View.GONE
                                Toast.makeText(this,
                                    getString(R.string.login_successful),
                                    Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            is ApiCallResult.Error, ApiCallResult.NetworkError -> {
                                binding.authProgress.visibility = View.GONE
                                Toast.makeText(this,
                                    getString(R.string.login_failed),
                                    Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openUnsplashLoginTab() = openCustomTab(viewModel.loginUrl)

    private fun openCustomTab(url: String) {
        CustomTabsHelper.openCustomTab(this, Uri.parse(url))
    }
}
