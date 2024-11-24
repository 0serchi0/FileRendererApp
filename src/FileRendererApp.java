import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileRendererApp {

    public static void main(String[] args) {

        // Setze das Look-and-Feel auf Metal
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Erstelle das Hauptfenster
        JFrame frame = new JFrame("NSR File Renderer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        frame.setLayout(new BorderLayout());

        // Oben: Panel für die Pfadangabe
        JPanel pathPanel = new JPanel(new BorderLayout());

        // Label, Textfeld und Button für den Dateipfad
        JPanel pathInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Links ausgerichtetes Layout
        JLabel pathLabel = new JLabel("path to daemon.raw:");
        JTextField pathField = new JTextField(40); // Breiteres Textfeld
        JButton browseButton = new JButton("browse"); // Button für die Auswahl
        JButton renderButton = new JButton("render");
        renderButton.setBackground(Color.GREEN); // Färbt den renderButton grün

        // Füge Label, Textfeld und Browse-Button in dasselbe Panel ein
        pathInputPanel.add(pathLabel);
        pathInputPanel.add(pathField); // Textfeld für Pfadangabe
        pathInputPanel.add(browseButton); // Button direkt hinter dem Textfeld
        pathInputPanel.add(renderButton);

        // Füge das pathInputPanel ins Hauptpanel ein
        pathPanel.add(pathInputPanel, BorderLayout.NORTH);

        // Panel für den Render-Button
        //JPanel renderButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Separates Panel für Render-Button
        //renderButtonPanel.add(renderButton);
        //pathPanel.add(renderButtonPanel, BorderLayout.SOUTH);

        // Panel für "From" und "To" Textfelder
        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Links ausgerichtetes Layout
        JLabel fromLabel = new JLabel("From:");
        JTextField fromField = new JTextField(15); // Feste Breite von 15 Spalten
        JLabel toLabel = new JLabel("To:");
        JTextField toField = new JTextField(15); // Feste Breite von 15 Spalten

        // Aktuelles Datum und Uhrzeit im Format "MM/dd/yyyy hh:mm:ss a" ermitteln
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
        String currentDateTime = dateFormat.format(new Date());
        toField.setText(currentDateTime); // Setze das aktuelle Datum und die Uhrzeit in das To-Feld

        rangePanel.add(fromLabel);
        rangePanel.add(fromField);
        rangePanel.add(toLabel);
        rangePanel.add(toField);
        rangePanel.add(renderButton); // Fügt den Render-Button direkt hinter das To-Feld hinzu

        // Panel für Pfad und Range kombinieren
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(pathPanel, BorderLayout.NORTH);
        topPanel.add(rangePanel, BorderLayout.SOUTH);

        // Mitte: Textbereich für die Programmausgabe
        JTextPane outputPane = new JTextPane();
        outputPane.setEditable(false); // Nur lesbar
        JScrollPane scrollPane = new JScrollPane(outputPane);

        // Fügt die Komponenten ins Hauptfenster ein
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Event-Listener für den "Browse"-Button
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                // Setze das aktuelle Verzeichnis als Ausgangspunkt
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
                int result = fileChooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    // Datei ausgewählt, Pfad in das Textfeld setzen
                    String selectedFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                    pathField.setText(selectedFilePath);
                }
            }
        });

        // Event-Listener für den "Render"-Button
        renderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filePath = pathField.getText();
                String fromValue = fromField.getText().trim(); // Trim für saubere Werte
                String toValue = toField.getText().trim();     // Trim für saubere Werte

                if (filePath.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter a valid file path.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Bereinige das outputPane bei jedem Render
                outputPane.setText(""); // Setzt das Textfeld zurück

                // Pfad des Programms "nsr_render_log.exe" im Unterverzeichnis "nsr_render_log"
                String programPath = System.getProperty("user.dir") + File.separator + "nsr_render_log" + File.separator + "nsr_render_log.exe";

                File programFile = new File(programPath);
                if (!programFile.exists()) {
                    JOptionPane.showMessageDialog(frame, "'nsr_render_log.exe' was not found in the directory: " + programPath, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Argumentliste für den ProcessBuilder
                List<String> arguments = new ArrayList<>();
                arguments.add(programPath); // Programm-Pfad

                // "From" und "To" hinzufügen, falls nicht leer
                if (!fromValue.isEmpty()) {
                    arguments.add("-S");
                    arguments.add("\"" + fromValue + "\"");
                }
                if (!toValue.isEmpty()) {
                    arguments.add("-E");
                    arguments.add("\"" + toValue + "\"");
                }

                arguments.add(filePath);   // Datei-Pfad

                // Befehl als String darstellen
                StringBuilder commandBuilder = new StringBuilder();
                for (String arg : arguments) {
                    commandBuilder.append(arg).append(" ");
                }
                String command = commandBuilder.toString().trim();

                // Kommando in der ersten Zeile des Textfeldes ausgeben
                appendColoredText(outputPane, "command: " + command + "\n", Color.WHITE);

                // Programm ausführen mit den angegebenen Argumenten
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder(arguments);
                    processBuilder.redirectErrorStream(true); // Fehlerausgabe mit normaler Ausgabe zusammenführen
                    Process process = processBuilder.start();

                    // Ausgabe des Programms lesen
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains("error") || line.toLowerCase().contains("critical")) {
                            appendColoredText(outputPane, line + "\n", Color.PINK);
                        } else if (line.toLowerCase().contains("warning")) {
                            appendColoredText(outputPane, line + "\n", Color.YELLOW);
                        } else {
                            appendColoredText(outputPane, line + "\n", Color.WHITE);
                        }
                    }

                    // Warte, bis der Prozess beendet ist
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        appendColoredText(outputPane, "\nThe program terminated with an error code: " + exitCode, Color.RED);
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error while executing the program: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });


        // Zeige das Fenster an
        frame.setVisible(true);
    }

    private static void appendColoredText(JTextPane textPane, String text, Color bgColor) {
        StyledDocument doc = textPane.getStyledDocument();
        Style style = textPane.addStyle("CustomStyle", null);
        StyleConstants.setBackground(style, bgColor);

        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
