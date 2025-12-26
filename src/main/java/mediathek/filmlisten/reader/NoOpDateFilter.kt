package mediathek.filmlisten.reader

import mediathek.daten.DatenFilm
import mediathek.daten.ListeFilme

internal class NoOpDateFilter(private val listeFilme: ListeFilme) : IDateFilter {

    override fun filter(film: DatenFilm) {
        // just add the film objet to the list as we are not supposed to do any filtering
        listeFilme.add(film)
    }
}