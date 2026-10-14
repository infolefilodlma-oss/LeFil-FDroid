package net.lefil.fdroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Navigation à quatre onglets (Agora, Atrium, Messages, Notifications).
 * Agora est réellement câblé (AgoraFragment, WebView vers
 * social.le-fil.net) ; les trois autres restent des écrans de
 * remplacement honnêtes : voir STATUS.md pour l'état exact de chaque
 * connexion.
 */
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        if (savedInstanceState == null) {
            showFragment(AgoraFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_agora -> AgoraFragment()
                R.id.nav_atrium -> PlaceholderFragment.newInstance(R.string.placeholder_atrium)
                R.id.nav_messages -> PlaceholderFragment.newInstance(R.string.placeholder_messages)
                R.id.nav_notifications -> PlaceholderFragment.newInstance(R.string.placeholder_notifications)
                else -> return@setOnItemSelectedListener false
            }
            showFragment(fragment)
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
