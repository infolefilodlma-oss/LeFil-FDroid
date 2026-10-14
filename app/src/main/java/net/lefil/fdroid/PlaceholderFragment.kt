package net.lefil.fdroid

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Écran de remplacement honnête : affiche l'onglet concerné sans simuler
 * aucune donnée réelle d'Agora/Atrium tant que les connexions ne sont pas
 * implémentées.
 */
class PlaceholderFragment : Fragment(R.layout.fragment_placeholder) {

    companion object {
        private const val ARG_TEXT_RES = "text_res"

        fun newInstance(textRes: Int): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_TEXT_RES, textRes)
            }
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textRes = arguments?.getInt(ARG_TEXT_RES) ?: 0
        if (textRes != 0) {
            view.findViewById<TextView>(R.id.placeholder_text).setText(textRes)
        }
    }
}
