package net.lefil.fdroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Navigation initiale à quatre onglets (Agora, Atrium, Messages,
 * Notifications). Aucun onglet n'appelle de service réel : voir
 * STATUS.md pour l'état exact de chaque connexion.
 */
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        if (savedInstanceState == null) {
            showFragment(PlaceholderFragment.newInstance(R.string.placeholder_agora))
        }

        bottomNav.setOnItemSelectedListener { item ->
            val textRes = when (item.itemId) {
                R.id.nav_agora -> R.string.placeholder_agora
                R.id.nav_atrium -> R.string.placeholder_atrium
                R.id.nav_messages -> R.string.placeholder_messages
                R.id.nav_notifications -> R.string.placeholder_notifications
                else -> return@setOnItemSelectedListener false
            }
            showFragment(PlaceholderFragment.newInstance(textRes))
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
