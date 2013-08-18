package de.panschk.gpxmerge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

public class GPXMerge {

    public static void main(String[] args) throws IOException {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return "GPX files (*.gpx)";
            }

            @Override
            public boolean accept(File f) {
                return (f.isDirectory() || f.getName().toLowerCase()
                        .endsWith(".gpx"));
            }
        });
        int returnVal = fc.showDialog(null, "Select input files");
        if (returnVal != 0) {
            return;
        }
        File[] inputFiles = fc.getSelectedFiles();

        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return "GPX files (*.gpx)";
            }

            @Override
            public boolean accept(File f) {
                return (f.isDirectory() || f.getName().toLowerCase()
                        .endsWith(".gpx"));
            }
        });
        returnVal = fc.showDialog(null, "Output file");
        if (returnVal != 0) {
            return;
        }
        File outputFile = fc.getSelectedFile();
        Integer[] choices = { 1, 2, 3, 5, 10, 20, 35, 50, 100, 200 };
        Integer skipRatio = (Integer) JOptionPane.showInputDialog(null,
                "Keep one coordinate out of..", "Skip points",
                JOptionPane.PLAIN_MESSAGE, null, choices, Integer.valueOf(5));
        String nameContains = (String) JOptionPane
                .showInputDialog("Name of activity must contain: (leave empty to use all data sets)");

        mergeFiles(inputFiles, outputFile, skipRatio, nameContains);

    }

    private static void mergeFiles(File[] inputFiles, File outputFile,
            Integer skipRatio, String nameContains) throws IOException {
        boolean isFirst = true;
        FileWriter fw = new FileWriter(outputFile);
        for (File file : inputFiles) {
            String gpxContent = handleFile(file, skipRatio, isFirst,
                    nameContains);
            fw.write(gpxContent);
            isFirst = false;
        }
        fw.write("</gpx>");
        fw.close();

    }

    private static String handleFile(File file, Integer skipRatio,
            boolean isFirst, String nameContains) throws IOException {
        int trkPtCount = 0;
        InputStream in = new FileInputStream(file);
        Reader reader = new InputStreamReader(in, "UTF-8");
        // buffer for efficiency
        Reader buffer = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        StringBuilder cache = new StringBuilder();
        String lastSegment = null;
        int r;
        while ((r = buffer.read()) != -1) {
            char ch = (char) r;
            cache.append(ch);
            String cacheValue = cache.toString();
            if (cacheValue.endsWith("<trk>")) {
               
                if (isFirst) {
                    sb.append(cache);
                    cache = new StringBuilder();
                } else {
                    cache = new StringBuilder("<trk>");
                }
            } else if (cacheValue.endsWith("</name>")) {
                if (nameContains != null && nameContains.length() > 0) {
                    if (!cacheValue.toLowerCase().contains(
                            nameContains.toLowerCase())) {
                        // skip file
                        buffer.close();
                        if (isFirst) {
                            int prefixEnd = sb.toString().indexOf("<trk>");
                            return sb.toString().substring(0, prefixEnd);
                        } else {
                            return "";
                        }
                    }

                }  
            } else if (cache.toString().endsWith("</trkpt>")) {

                if (trkPtCount % skipRatio == 0) {
                    sb.append(cache);
                    lastSegment = null;
                } else {
                    lastSegment = cache.toString();
                }
                cache = new StringBuilder();
                trkPtCount++;
            } else if (cache.toString().endsWith("</trkseg>")) {
                if (lastSegment != null) {
                    sb.append(lastSegment);
                    lastSegment = null;
                }
                sb.append("\n</trkseg>");
                cache = new StringBuilder();
            } else if (cache.toString().endsWith("</trk>")) {
                sb.append("\n</trk>");
                cache = new StringBuilder();
                trkPtCount = 0;
            }
        }
        buffer.close();
        return sb.toString();
    }

    private enum ParseState {
        BEGINNING, TRK, TRKPT
    }

}
