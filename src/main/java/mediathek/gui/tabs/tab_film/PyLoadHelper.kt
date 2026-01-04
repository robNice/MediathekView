package mediathek.gui.tabs.tab_film

import mediathek.config.Konstanten
import mediathek.config.MVConfig
import mediathek.controller.history.SeenHistoryController
import mediathek.daten.DatenFilm
import mediathek.daten.FilmResolution
import mediathek.mainwindow.MediathekGui
import mediathek.tool.SwingErrorDialog
import mediathek.tool.http.MVHttpClient
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Credentials
import org.apache.logging.log4j.LogManager
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPopupMenu

class PyLoadHelper {
    private val historyController = SeenHistoryController()

    private fun downloadUrl(url: HttpUrl, film: DatenFilm) {
        val baseUrl = MVConfig.get(MVConfig.Configs.SYSTEM_PYLOAD_URL).trimEnd('/')
        val user = MVConfig.get(MVConfig.Configs.SYSTEM_PYLOAD_USER)
        val pass = MVConfig.get(MVConfig.Configs.SYSTEM_PYLOAD_PASSWORD)

        val apiUrl = try {
            "$baseUrl/api/add_package".toHttpUrl()
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid pyLoad URL in config: {}", baseUrl)
            SwingErrorDialog.showExceptionMessage(
                MediathekGui.ui(),
                "<html>Die konfigurierte pyLoad-URL ist ungültig:<br><br><b>$baseUrl</b></html>",
                e
            )
            return
        }

        val jsonBody = """
        {
          "name": "${film.getTitle()}",
          "dest": 1,
          "links": ["$url"]
        }
    """.trimIndent()

        val requestBody = RequestBody.create(
            "application/json".toMediaType(),
            jsonBody
        )

        val request = Request.Builder()
            .url(apiUrl)
            .header(
                "Authorization",
                Credentials.basic(user, pass)
            )
            .post(requestBody)
            .build()

        try {
            val client = MVHttpClient.getInstance()
                .httpClient
                .newBuilder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .build()

            client.newCall(request).execute().use {
                if (it.isSuccessful) {
                    historyController.markSeen(film)
                } else {
                    throw RuntimeException("pyLoad HTTP ${it.code}")
                }
            }
        }
        catch (e: Exception) {
            logger.error("pyLoad downloadUrl", e)
            showErrorMessage()
        }
    }

    private fun showErrorMessage() {
        JOptionPane.showMessageDialog(
            MediathekGui.ui(),
            "Verbindung mit pyLoad nicht möglich.\n" +
                    "Bitte stellen Sie sicher, dass pyLoad gestartet wurde und die in den Einstellungen hinterlegten Daten (URL, Benutzer und Passwort) korrekt sind.",
            Konstanten.PROGRAMMNAME,
            JOptionPane.ERROR_MESSAGE)
    }

    fun installContextMenu(film: DatenFilm, jPopupMenu: JPopupMenu) {
        jPopupMenu.addSeparator()

        val pyLoadUrl = MVConfig.get(MVConfig.Configs.SYSTEM_PYLOAD_URL)
        val pyLoadConfigured = pyLoadUrl.isNotBlank()

        val mJD = JMenu("Mit pyLoad herunterladen")
        mJD.isEnabled = pyLoadConfigured
        val uNormal = film.urlNormalQuality.toHttpUrl()
        val uHq = film.getUrlFuerAufloesung(FilmResolution.Enum.HIGH_QUALITY).toHttpUrl()
        val uLow = film.getUrlFuerAufloesung(FilmResolution.Enum.LOW).toHttpUrl()

        if (film.isHighQuality) {
            val miHq = JMenuItem("in bester Qualität")
            miHq.addActionListener { downloadUrl(uHq, film) }
            mJD.add(miHq)
        }
        val miNormal = JMenuItem("in normaler Qualität")
        miNormal.addActionListener { downloadUrl(uNormal, film) }
        mJD.add(miNormal)

        if (uLow !== uNormal) {
            val miLow = JMenuItem("in niedriger Qualität")
            miLow.addActionListener { downloadUrl(uLow, film) }
            mJD.add(miLow)
        }
        jPopupMenu.add(mJD)

        val miWebsiteToJd = JMenuItem("Webseiten-URL an pyLoad übergeben")
        miWebsiteToJd.isEnabled = pyLoadConfigured
        miWebsiteToJd.addActionListener {
            try {
                val webSiteUrl = film.websiteUrl.toHttpUrl()
                downloadUrl(webSiteUrl, film)
            }
            catch (e: IllegalArgumentException) {
                logger.error("Illegal Website URL found: {}", film.websiteUrl)
            }
        }
        jPopupMenu.add(miWebsiteToJd)
        if (film.websiteUrl.isBlank()) {
            miWebsiteToJd.isEnabled = false
        }
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}