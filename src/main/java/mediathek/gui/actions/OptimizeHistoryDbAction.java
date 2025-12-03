package mediathek.gui.actions;

import mediathek.config.Konstanten;
import mediathek.controller.history.SeenHistoryController;
import mediathek.mainwindow.MediathekGui;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

public class OptimizeHistoryDbAction extends AbstractAction {
    private final MediathekGui mediathekGui;
    private static final Logger logger = LogManager.getLogger();

    public OptimizeHistoryDbAction(MediathekGui mediathekGui) {
        this.mediathekGui = mediathekGui;
        putValue(NAME, "History-Datenbank optimieren...");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try (SeenHistoryController controller = new SeenHistoryController()) {
            var dupes = controller.checkDuplicates();
            logger.trace("{} duplicates found in history", dupes.total() - dupes.distinct());
            controller.removeDuplicates();
            dupes = controller.checkDuplicates();
            logger.trace("{} duplicates found in history after cleanup", dupes.total() - dupes.distinct());

            controller.performDatabaseCompact();
            JOptionPane.showMessageDialog(mediathekGui, "Datenbankoptimierung abgeschlossen.", Konstanten.PROGRAMMNAME, JOptionPane.INFORMATION_MESSAGE);
        }
        catch (SQLException ex) {
            JOptionPane.showMessageDialog(mediathekGui, ex.getMessage(), Konstanten.PROGRAMMNAME, JOptionPane.ERROR_MESSAGE);
        }
    }
}
