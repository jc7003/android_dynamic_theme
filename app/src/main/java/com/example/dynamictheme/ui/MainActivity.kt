package com.example.dynamictheme.ui

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.dynamictheme.R
import com.example.dynamictheme.ServiceLocator
import com.example.dynamictheme.databinding.ActivityMainBinding
import com.example.dynamictheme.theme.AppTheme
import com.example.dynamictheme.theme.DefaultTheme
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : BaseThemedActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Chip → theme name passed to GET /theme?name=… */
    private val chipThemes by lazy {
        mapOf(
            binding.chipOcean to "ocean",
            binding.chipForest to "forest",
            binding.chipSunset to "sunset",
            binding.chipBroken to "broken",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        chipThemes.forEach { (chip, name) -> chip.setOnClickListener { loadTheme(name) } }

        val mock = ServiceLocator.mockInterceptor
        binding.switchFail.isChecked = mock.simulateFailure
        binding.switchFail.setOnCheckedChangeListener { _, checked -> mock.simulateFailure = checked }

        binding.fabRefresh.setOnClickListener { loadTheme(currentThemeName()) }
        binding.buttonSecond.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }
        binding.buttonDialog.setOnClickListener { showDialog() }

        // On a cold start, refresh in the background. The cached theme is already on screen.
        if (savedInstanceState == null) loadTheme(currentThemeName())
    }

    override fun onThemeApplied(theme: AppTheme) {
        binding.themeInfo.text = getString(
            R.string.theme_info, theme.id, theme.version, theme.cornerRadiusDp.toInt(),
        )
        binding.statusBadge.text = theme.id
        syncChips(theme)
    }

    private fun loadTheme(name: String?) {
        binding.progress.isVisible = true
        lifecycleScope.launch {
            val result = themeRepository.refresh(name)
            binding.progress.isVisible = false
            // A changed theme recreates this Activity; otherwise (same theme or failure)
            // undo the chip the user tapped.
            syncChips(currentTheme)
            result.onFailure { error ->
                Snackbar.make(
                    binding.root,
                    getString(R.string.theme_error, error.message ?: error.javaClass.simpleName),
                    Snackbar.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun currentThemeName(): String? = currentTheme.id.takeIf { it != DefaultTheme.ID }

    private fun syncChips(theme: AppTheme) {
        chipThemes.forEach { (chip: Chip, name) -> chip.isChecked = name == theme.id }
    }

    private fun showDialog() {
        // Inflates with the overridden theme attrs, so it is themed with no extra code.
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title)
            .setMessage(getString(R.string.dialog_message, currentTheme.id))
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
