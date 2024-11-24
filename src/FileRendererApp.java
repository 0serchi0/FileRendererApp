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

        // Programmpfad abhängig vom übergebenen Argument setzen
        String programPath;
        if (args.length > 0 && !args[0].isEmpty()) {
            programPath = args[0];
        } else {
            programPath = System.getProperty("user.dir") + File.separator + "nsr_render_log" + File.separator + "nsr_render_log.exe";
        }

        // Überprüfen, ob der Programmpfad existiert
        File programFile = new File(programPath);
        if (!programFile.exists()) {
            JOptionPane.showMessageDialog(null, "'nsr_render_log.exe' was not found, either place it in the subdirectory nsr_render_log\\nsr_render_log.exe or specify the path as argument.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
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
        JLabel pathLabel = new JLabel("path to .raw file:");
        JTextField pathField = new JTextField(40); // Breiteres Textfeld
        JButton browseButton = new JButton("browse"); // Button für die Auswahl
        JButton renderButton = new JButton("render");
        renderButton.setBackground(Color.PINK); // Färbt den renderButton grün

        pathInputPanel.add(pathLabel);
        pathInputPanel.add(pathField);
        pathInputPanel.add(browseButton);
        pathInputPanel.add(renderButton);

        pathPanel.add(pathInputPanel, BorderLayout.NORTH);

        // Panel für "From" und "To" Textfelder
        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Links ausgerichtetes Layout
        JLabel fromLabel = new JLabel("From:");
        JTextField fromField = new JTextField(15); // Feste Breite von 15 Spalten
        JLabel toLabel = new JLabel("To:");
        JTextField toField = new JTextField(15); // Feste Breite von 15 Spalten

        // Aktuelles Datum und Uhrzeit im Format "MM/dd/yyyy hh:mm:ss a" ermitteln
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
        String currentDateTime = dateFormat.format(new Date());
        toField.setText(currentDateTime);

        // Filter-Label, Textfeld und Button
        JLabel filterLabel = new JLabel("search:");
        JTextField filterField = new JTextField(15); // Feste Breite von 15 Spalten
        JButton applyFilterButton = new JButton("search");
        applyFilterButton.setBackground(Color.GREEN); // Färbt den applyFilterButton grün

        rangePanel.add(fromLabel);
        rangePanel.add(fromField);
        rangePanel.add(toLabel);
        rangePanel.add(toField);
        rangePanel.add(filterLabel); // Fügt das Filter-Label hinzu
        rangePanel.add(filterField); // Fügt das Filter-Textfeld hinzu
        rangePanel.add(applyFilterButton); // Fügt den Apply-Filter-Button hinzu

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(pathPanel, BorderLayout.NORTH);
        topPanel.add(rangePanel, BorderLayout.SOUTH);

        JTextPane outputPane = new JTextPane();
        outputPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputPane);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Event-Listener für den "Browse"-Button
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
                int result = fileChooser.showOpenDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
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
                String fromValue = fromField.getText().trim();
                String toValue = toField.getText().trim();

                if (filePath.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter a valid file path.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                outputPane.setText("");

                List<String> arguments = new ArrayList<>();
                arguments.add(programPath);

                if (!fromValue.isEmpty()) {
                    arguments.add("-S");
                    arguments.add("\"" + fromValue + "\"");
                }
                if (!toValue.isEmpty()) {
                    arguments.add("-E");
                    arguments.add("\"" + toValue + "\"");
                }

                arguments.add(filePath);

                StringBuilder commandBuilder = new StringBuilder();
                for (String arg : arguments) {
                    commandBuilder.append(arg).append(" ");
                }
                String command = commandBuilder.toString().trim();

                appendColoredText(outputPane, "command: " + command + "\n", Color.WHITE);

                try {
                    ProcessBuilder processBuilder = new ProcessBuilder(arguments);
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains("error") || line.toLowerCase().contains("critical") || line.toLowerCase().contains("fail")) {
                            appendColoredText(outputPane, line + "\n", Color.PINK);
                        } else if (line.toLowerCase().contains("warning")) {
                            appendColoredText(outputPane, line + "\n", Color.YELLOW);
                        } else {
                            appendColoredText(outputPane, line + "\n", Color.WHITE);
                        }
                    }

                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        appendColoredText(outputPane, "\nThe program terminated with an error code: " + exitCode, Color.RED);
                    }

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error while executing the program: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Event-Listener für den "Apply Filter"-Button
        applyFilterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filterText = filterField.getText().trim().toLowerCase(); // Case-insensitive Suche
                if (filterText.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "Please enter a search term.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    StyledDocument doc = outputPane.getStyledDocument();
                    String fullText = doc.getText(0, doc.getLength()); // Gesamten Text abrufen

                    // Falls der Text leer ist, abbrechen
                    if (fullText.isEmpty()) {
                        JOptionPane.showMessageDialog(frame, "The output pane is empty.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    // Standardstil definieren, falls nicht vorhanden
                    Style defaultStyle = outputPane.addStyle("Default", null);
                    StyleConstants.setBackground(defaultStyle, Color.WHITE); // Weißer Hintergrund als Standard

                    // Zurücksetzen aller Hervorhebungen
                    doc.setCharacterAttributes(0, fullText.length(), defaultStyle, true);

                    // Text in Zeilen aufteilen und durchsuchen
                    String[] lines = fullText.split("\n"); // Text in Zeilen aufteilen
                    int offset = 0; // Start-Offset für jede Zeile

                    for (String line : lines) {
                        if (line.toLowerCase().contains(filterText)) {
                            // Hervorhebung hinzufügen
                            int lineLength = line.length();
                            Style highlightStyle = outputPane.addStyle("Highlight", null);
                            StyleConstants.setBackground(highlightStyle, Color.GREEN); // Hellgrüner Hintergrund
                            doc.setCharacterAttributes(offset, lineLength, highlightStyle, true);
                        }
                        offset += line.length() + 1; // +1 für das '\n'
                    }
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });


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
